package com.xuyan.fm.service;

import com.xuyan.fm.model.OrderAddressModel;

public interface OrderAddressService {

    /**
     * 为订单添加地址信息（校验订单归属）
     * @param userId 当前登录用户
     * @param orderAddressModel 地址信息
     * @return 是否成功
     */
    boolean addOrderAddress(Long userId, OrderAddressModel orderAddressModel);

    /**
     * 更新订单的地址信息（校验订单归属）
     * @param userId 当前登录用户
     * @param orderAddressModel 地址信息
     * @return 是否成功
     */
    boolean updateOrderAddress(Long userId, OrderAddressModel orderAddressModel);

    /**
     * 获取订单的地址信息（仅买家本人或该商品卖家可查）
     * @param userId 当前登录用户
     * @param orderId 订单 ID
     * @return 地址信息
     */
    OrderAddressModel getOrderAddress(Long userId, Long orderId);
}
