package com.xuyan.fm.common.task;

import com.xuyan.fm.constants.Constants;
import com.xuyan.fm.dao.OrderDao;
import com.xuyan.fm.model.OrderModel;
import com.xuyan.fm.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 订单超时自动取消任务。
 *
 * 整改点（对应分析报告 P1-4）：
 * 原实现是“内存 DelayQueue + 手动起消费线程”：
 *   1. OrderTaskHandler.run() 从未被任何代码调用，消费线程根本不存在，
 *      导致“30 分钟未支付自动取消”功能完全失效（前端还配合做了假倒计时）；
 *   2. 任务只存于内存，服务重启即全部丢失，已支付边界也存在竞态。
 * 现改为 Spring @Scheduled 定时扫描数据库：
 *   - 每分钟扫描一次超时未支付订单（create_time 早于 now-30min 且未支付）；
 *   - 取消走 OrderService.updateOrder 的条件更新分支，与用户支付动作天然互斥；
 *   - 任务持久化在数据库中，服务重启后仍能补取消，幂等可重试。
 */
@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    @Resource
    private OrderDao orderDao;

    @Resource
    private OrderService orderService;

    /**
     * 每分钟扫描一次超时未支付订单并自动取消
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelTimeoutOrders() {
        Date deadline = new Date(System.currentTimeMillis() - Constants.ORDER_TIMEOUT_MINUTES * 60_000L);
        List<OrderModel> timeoutOrders = orderDao.selectTimeoutUnpaidOrders(deadline, Constants.TIMEOUT_SCAN_BATCH);
        if (timeoutOrders.isEmpty()) {
            return;
        }
        log.info("订单超时扫描：发现 {} 笔超时未支付订单，开始自动取消", timeoutOrders.size());
        int canceled = 0;
        for (OrderModel order : timeoutOrders) {
            OrderModel cancel = new OrderModel();
            cancel.setId(order.getId());
            cancel.setOrderStatus((byte) 4);
            try {
                if (orderService.updateOrder(cancel)) {
                    canceled++;
                    log.info("订单 {} 超时未支付，已自动取消并重新上架商品", order.getOrderNumber());
                }
            } catch (Exception e) {
                // 单笔失败不影响后续订单处理
                log.error("自动取消订单 {} 失败", order.getOrderNumber(), e);
            }
        }
        log.info("订单超时扫描完成：成功取消 {}/{} 笔", canceled, timeoutOrders.size());
    }
}
