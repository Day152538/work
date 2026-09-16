package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.dao.OrderDao;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.model.OrderModel;
import com.xuyan.fm.service.OrderService;
import com.xuyan.fm.vo.PageVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现。
 *
 * 整改点（对应分析报告 P1-1/P1-2/P1-3/P1-4）：
 * 1. 事务失效修复（两处）：
 *    a) 原代码事务回滚分支写的是 `new RuntimeException();`（创建了异常对象但没有 throw），
 *       插入订单失败时商品照样被下架且不回滚 —— 改为 throw 抛出，rollbackFor=Exception 生效；
 *    b) 原同类内部调用 addOrder(...) -> addOrderHelp(...)，事务基于代理实现，
 *       自调用不经过代理，@Transactional 完全不生效 —— 现把事务方法收敛为单一 public 方法，
 *       并发控制改由数据库条件更新承担，不再依赖自调用。
 * 2. 并发下单竞态修复：原方案“查状态 + 分段锁 + 再查一次”仍存在窗口期。
 *    改为条件更新（update ... set idle_status=2 where idle_status=1），
 *    数据库行锁保证同一商品只有一个请求能锁定成功，天然防超卖，分段锁代码全部删除。
 * 3. 订单超时取消：内存延迟队列（从未启动 + 重启丢任务）改为数据库扫描 + 定时任务
 *    （见 OrderTimeoutScheduler），任务可持久、可幂等。
 * 4. 支付与取消的竞态：取消订单/标记支付都改为条件更新（仅未支付状态可流转），
 *    杜绝“已取消的订单被补单支付”。
 * 5. 多件库存（拍下预占）：下单原子扣 1 件库存（reserveStock）防超卖，但不再把整件商品锁成下架，
 *    只要还有库存就继续在首页供其他买家购买；最后一库存件被预占后自动下架（downIfNoStock）。
 *    取消/超时释放库存（releaseStock + relistIfInStock），支付仅销量 +1（increaseSalesCount）。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /** 订单状态：0=待支付 1=已支付 4=已取消 */
    private static final byte ORDER_UNPAID = 0;
    private static final byte ORDER_CANCELED = 4;

    @Resource
    private OrderDao orderDao;

    @Resource
    private IdleItemDao idleItemDao;

    /**
     * 新增订单：条件更新锁定商品 + 插入订单，同一事务内完成。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOrder(OrderModel orderModel) {
        IdleItemModel item = idleItemDao.selectByPrimaryKey(orderModel.getIdleId());
        if (item == null || item.getIdleStatus() == null) {
            return false;
        }
        // 不允许购买自己发布的商品
        if (item.getUserId() != null && item.getUserId().equals(orderModel.getUserId())) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        // 库存为 0（已售罄）的商品不能下单
        if (item.getStock() == null || item.getStock() <= 0) {
            throw new BusinessException(ErrorMsg.SOLD_OUT_ERROR);
        }
        // 下单即预占 1 件库存（原子扣减，防并发超卖）。商品不随下单下架，
        // 只要还有剩余库存就继续在首页供其他买家购买；仅剩库存被预占/售罄时才自动下架。
        int reserved = idleItemDao.reserveStock(orderModel.getIdleId());
        if (reserved != 1) {
            // 预占失败：已被并发抢空或商品已不在售
            throw new BusinessException(ErrorMsg.SOLD_OUT_ERROR);
        }
        // 若这是最后一库存件，整件自动下架（不再出现在首页）
        idleItemDao.downIfNoStock(orderModel.getIdleId());
        if (orderDao.insert(orderModel) != 1) {
            // 抛出异常而非 return false：保证上面的库存预占一起回滚
            throw new BusinessException(ErrorMsg.COMMIT_FAIL_ERROR);
        }
        return true;
    }

    /**
     * 获取订单信息（附带商品信息）
     */
    @Override
    public OrderModel getOrder(Long id) {
        OrderModel orderModel = orderDao.selectByPrimaryKey(id);
        if (orderModel != null) {
            orderModel.setIdleItem(idleItemDao.selectByPrimaryKey(orderModel.getIdleId()));
        }
        return orderModel;
    }

    /**
     * 更新订单：支付 / 取消两类关键流转均使用条件更新保证原子性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrder(OrderModel orderModel) {
        // 不可被篡改的字段一律置空
        orderModel.setOrderNumber(null);
        orderModel.setUserId(null);
        orderModel.setIdleId(null);
        orderModel.setCreateTime(null);

        // —— 取消订单 ——
        if (orderModel.getOrderStatus() != null && orderModel.getOrderStatus() == ORDER_CANCELED) {
            OrderModel dbOrder = orderDao.selectByPrimaryKey(orderModel.getId());
            if (dbOrder == null || dbOrder.getOrderStatus() == null
                    || dbOrder.getOrderStatus() != ORDER_UNPAID) {
                return false;
            }
            // 条件更新：仅当订单仍为待支付时才取消（与支付动作互斥）
            int canceled = orderDao.updateOrderStatusIfCurrent(orderModel.getId(), ORDER_UNPAID, ORDER_CANCELED);
            if (canceled != 1) {
                return false;
            }
            // 释放该订单预占的 1 件库存；若释放后又有在售库存且商品此前因售罄下架，则自动重新上架。
            // （下单不再锁定整件商品，这里不再做“整件上架”判断以外的状态流转）
            if (dbOrder.getIdleId() != null) {
                idleItemDao.releaseStock(dbOrder.getIdleId());
                idleItemDao.relistIfInStock(dbOrder.getIdleId());
            }
            return true;
        }

        // —— 支付 ——
        if (orderModel.getPaymentStatus() != null && orderModel.getPaymentStatus() == 1) {
            // 条件更新：仅未支付订单可支付，已取消订单无法补单支付
            if (orderDao.markPaidIfUnpaid(orderModel.getId(),
                    orderModel.getPaymentWay(), new Date()) != 1) {
                return false;
            }
            // 支付成功：销量 +1。库存已在下单时预占，这里不再扣减；
            // 预占但未支付的库存由取消订单 / 超时任务释放（releaseStock）。
            OrderModel paidOrder = orderDao.selectByPrimaryKey(orderModel.getId());
            if (paidOrder != null && paidOrder.getIdleId() != null) {
                idleItemDao.increaseSalesCount(paidOrder.getIdleId());
            }
            return true;
        }

        return orderDao.updateByPrimaryKeySelective(orderModel) == 1;
    }

    /**
     * 我的订单（附带商品信息）
     */
    @Override
    public List<OrderModel> getMyOrder(Long userId) {
        List<OrderModel> list = orderDao.getMyOrder(userId);
        fillIdleItems(list);
        return list;
    }

    /**
     * 我卖出的订单
     */
    @Override
    public List<OrderModel> getMySoldIdle(Long userId) {
        List<IdleItemModel> list = idleItemDao.getAllIdleItem(userId);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> idleIdList = new ArrayList<>();
        for (IdleItemModel i : list) {
            idleIdList.add(i.getId());
        }
        List<OrderModel> orderList = orderDao.findOrderByIdleIdList(idleIdList);
        if (orderList == null) {
            return new ArrayList<>();
        }
        Map<Long, IdleItemModel> map = new HashMap<>();
        for (IdleItemModel idle : list) {
            map.put(idle.getId(), idle);
        }
        for (OrderModel o : orderList) {
            o.setIdleItem(map.get(o.getIdleId()));
        }
        return orderList;
    }

    /**
     * 全部订单（管理端）
     */
    @Override
    public PageVo<OrderModel> getAllOrder(int page, int nums) {
        List<OrderModel> list = orderDao.getAllOrder((page - 1) * nums, nums);
        fillIdleItems(list);
        int count = orderDao.countAllOrder();
        return new PageVo<>(list, count);
    }

    @Override
    public boolean deleteOrder(long id) {
        return orderDao.deleteByPrimaryKey(id) == 1;
    }

    /**
     * 批量填充订单关联的商品信息（一次 in 查询，避免 N+1）
     */
    private void fillIdleItems(List<OrderModel> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> idleIdList = new ArrayList<>();
        for (OrderModel i : list) {
            idleIdList.add(i.getIdleId());
        }
        List<IdleItemModel> idleItemModelList = idleItemDao.findIdleByList(idleIdList);
        Map<Long, IdleItemModel> map = new HashMap<>();
        for (IdleItemModel idle : idleItemModelList) {
            map.put(idle.getId(), idle);
        }
        for (OrderModel i : list) {
            i.setIdleItem(map.get(i.getIdleId()));
        }
    }
}
