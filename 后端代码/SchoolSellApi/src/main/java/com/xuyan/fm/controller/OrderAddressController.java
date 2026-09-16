package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.model.OrderAddressModel;
import com.xuyan.fm.service.OrderAddressService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

/**
 * 订单收货地址控制器。
 * 整改点：
 * 1. 鉴权方式由「明文 Cookie shUserId」改为 JWT + ThreadLocal（UserContext）。
 * 2. 修复 IDOR：原实现任何登录用户可读/改任意订单的收货地址（PII 泄露），
 *    现由 Service 层校验订单归属（买家本人或商品卖家）。
 */
@RestController
@RequestMapping("/order-address")
public class OrderAddressController {

    private final OrderAddressService orderAddressService;

    public OrderAddressController(OrderAddressService orderAddressService) {
        this.orderAddressService = orderAddressService;
    }

    @PostMapping("/add")
    public ResultVo addOrderAddress(@RequestBody OrderAddressModel orderAddressModel) {
        return ResultVo.success(orderAddressService.addOrderAddress(UserContext.getUserId(), orderAddressModel));
    }

    @PostMapping("/update")
    public ResultVo updateOrderAddress(@RequestBody OrderAddressModel orderAddressModel) {
        if (orderAddressService.updateOrderAddress(UserContext.getUserId(), orderAddressModel)) {
            return ResultVo.success(orderAddressModel);
        }
        return ResultVo.fail(com.xuyan.fm.common.enums.ErrorMsg.SYSTEM_ERROR);
    }

    @GetMapping("/info")
    public ResultVo getOrderAddress(@RequestParam Long orderId) {
        return ResultVo.success(orderAddressService.getOrderAddress(UserContext.getUserId(), orderId));
    }
}
