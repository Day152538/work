package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.dao.OrderAddressDao;
import com.xuyan.fm.dao.OrderDao;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.model.OrderAddressModel;
import com.xuyan.fm.model.OrderModel;
import com.xuyan.fm.service.OrderAddressService;
import org.springframework.stereotype.Service;

/**
 * 订单收货地址服务实现。
 * 整改点（修复水平越权 / IDOR）：
 * 原实现任何登录用户都能读/改任意订单的收货地址（含收件人姓名、电话、详细地址等 PII），
 * 现在所有操作前都校验订单归属：仅订单买家本人或该商品卖家可操作。
 */
@Service
public class OrderAddressServiceImpl implements OrderAddressService {

    private final OrderAddressDao orderAddressDao;
    private final OrderDao orderDao;
    private final IdleItemDao idleItemDao;

    public OrderAddressServiceImpl(OrderAddressDao orderAddressDao,
                                   OrderDao orderDao,
                                   IdleItemDao idleItemDao) {
        this.orderAddressDao = orderAddressDao;
        this.orderDao = orderDao;
        this.idleItemDao = idleItemDao;
    }

    /**
     * 校验当前用户是否为该订单的买家或卖家
     */
    private void checkOrderOwnership(Long userId, Long orderId) {
        OrderModel order = orderDao.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new BusinessException(ErrorMsg.ORDER_NOT_EXIST);
        }
        if (userId.equals(order.getUserId())) {
            return; // 买家本人
        }
        IdleItemModel idleItem = idleItemDao.selectByPrimaryKey(order.getIdleId());
        if (idleItem != null && userId.equals(idleItem.getUserId())) {
            return; // 商品卖家（发货方需要查看买家收货地址）
        }
        throw new BusinessException(ErrorMsg.NO_PERMISSION);
    }

    @Override
    public boolean addOrderAddress(Long userId, OrderAddressModel orderAddressModel) {
        checkOrderOwnership(userId, orderAddressModel.getOrderId());
        return orderAddressDao.insert(orderAddressModel) == 1;
    }

    @Override
    public boolean updateOrderAddress(Long userId, OrderAddressModel orderAddressModel) {
        // 先按地址主键反查所属订单校验归属，再执行更新
        OrderAddressModel existing = orderAddressDao.selectByPrimaryKey(orderAddressModel.getId());
        if (existing == null) {
            return false;
        }
        checkOrderOwnership(userId, existing.getOrderId());
        // 订单 ID 不允许被修改
        orderAddressModel.setOrderId(null);
        return orderAddressDao.updateByPrimaryKeySelective(orderAddressModel) == 1;
    }

    @Override
    public OrderAddressModel getOrderAddress(Long userId, Long orderId) {
        checkOrderOwnership(userId, orderId);
        return orderAddressDao.selectByOrderId(orderId);
    }
}
