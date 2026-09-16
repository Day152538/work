package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.utils.IdFactoryUtil;
import com.xuyan.fm.common.utils.RedisUtil;
import com.xuyan.fm.model.OrderModel;
import com.xuyan.fm.service.OrderService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单接口。
 *
 * 整改点（对应分析报告 P0-2-4 / P1 系列）：
 * 1. 身份来源：从“信任明文 Cookie shUserId”改为 JWT 上下文 UserContext.getUserId()。
 * 2. updateOrder 增加属主校验：原接口任何登录用户可改任意订单（改支付状态=白嫖），
 *    现仅订单买家或商品卖家本人可操作。
 * 3. 移除向静态字段塞 OrderService 的 hack 写法（OrderTaskHandler.orderService=...）。
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final RedisUtil redisUtil;

    /** 内存幂等降级：Redis 不可用时使用，key=requestId, value=过期时间戳 */
    private final Map<String, Long> memoryIdem = new ConcurrentHashMap<>();
    /** 幂等键过期时间：5 分钟 */
    private static final long IDEM_TTL_MS = 5 * 60 * 1000L;

    public OrderController(OrderService orderService, RedisUtil redisUtil) {
        this.orderService = orderService;
        this.redisUtil = redisUtil;
    }

    /**
     * 下单（同时锁定商品，事务保证原子性）。
     * 幂等：前端传 requestId（UUID），后端用 Redis SETNX 防重复提交；
     * Redis 不可用时降级为内存 ConcurrentHashMap，保证单机环境也能防重。
     */
    @PostMapping("/add")
    public ResultVo addOrder(@RequestBody OrderModel orderModel,
                             @RequestParam(value = "requestId", required = false) String requestId) {
        // —— 幂等校验 ——
        if (requestId != null && !requestId.isBlank()) {
            String idemKey = "order:idem:" + requestId.trim();
            boolean redisOk = redisUtil.setIfAbsent(idemKey, "1", 5, java.util.concurrent.TimeUnit.MINUTES);
            if (!redisOk) {
                // Redis SETNX 失败：可能是重复提交，也可能是 Redis 不可用
                if (redisUtil.isAvailable()) {
                    // Redis 可用但 SETNX 失败 → 确实是重复提交
                    return ResultVo.fail(ErrorMsg.REPEAT_COMMIT_ERROR);
                }
                // Redis 不可用 → 降级内存幂等
                long now = System.currentTimeMillis();
                memoryIdem.entrySet().removeIf(e -> e.getValue() < now); // 清理过期
                if (memoryIdem.putIfAbsent(idemKey, now + IDEM_TTL_MS) != null) {
                    return ResultVo.fail(ErrorMsg.REPEAT_COMMIT_ERROR);
                }
            }
        }
        orderModel.setOrderNumber(IdFactoryUtil.getOrderId());
        orderModel.setCreateTime(new Date());
        orderModel.setUserId(UserContext.getUserId());
        orderModel.setOrderStatus((byte) 0);
        orderModel.setPaymentStatus((byte) 0);
        if (orderService.addOrder(orderModel)) {
            return ResultVo.success(orderModel);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 订单详情（仅买家或卖家可见）
     */
    @GetMapping("/info")
    public ResultVo getOrderInfo(@RequestParam Long id) {
        OrderModel orderModel = orderService.getOrder(id);
        if (orderModel == null) {
            return ResultVo.fail(ErrorMsg.PARAM_ERROR);
        }
        if (!isOrderParticipant(orderModel)) {
            return ResultVo.fail(ErrorMsg.NO_PERMISSION);
        }
        return ResultVo.success(orderModel);
    }

    /**
     * 更新订单（支付/取消），仅买家或卖家可操作
     */
    @PostMapping("/update")
    public ResultVo updateOrder(@RequestBody OrderModel orderModel) {
        if (orderModel.getId() == null) {
            return ResultVo.fail(ErrorMsg.PARAM_ERROR);
        }
        OrderModel dbOrder = orderService.getOrder(orderModel.getId());
        if (dbOrder == null) {
            return ResultVo.fail(ErrorMsg.PARAM_ERROR);
        }
        if (!isOrderParticipant(dbOrder)) {
            return ResultVo.fail(ErrorMsg.NO_PERMISSION);
        }
        if (orderService.updateOrder(orderModel)) {
            return ResultVo.success(orderModel);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 我买入的订单
     */
    @GetMapping("/my")
    public ResultVo getMyOrder() {
        return ResultVo.success(orderService.getMyOrder(UserContext.getUserId()));
    }

    /**
     * 我卖出的订单
     */
    @GetMapping("/my-sold")
    public ResultVo getMySoldIdle() {
        return ResultVo.success(orderService.getMySoldIdle(UserContext.getUserId()));
    }

    /**
     * 判断当前登录用户是否为该订单的参与者（买家或卖家）
     */
    private boolean isOrderParticipant(OrderModel orderModel) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return false;
        }
        boolean isBuyer = currentUserId.equals(orderModel.getUserId());
        boolean isSeller = orderModel.getIdleItem() != null
                && currentUserId.equals(orderModel.getIdleItem().getUserId());
        return isBuyer || isSeller;
    }
}
