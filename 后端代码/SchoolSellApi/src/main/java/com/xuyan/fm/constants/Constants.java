package com.xuyan.fm.constants;

/**
 * 全局常量。
 * 整改点：新增订单超时时长常量。原代码后端写 1 分钟、前端倒计时 30 分钟/10 分钟三处矛盾，
 * 现统一为 30 分钟，前后端共用同一口径。
 */
public class Constants {

    /** 订单支付超时时间（分钟）：超时未支付的订单由定时任务自动取消并重新上架商品 */
    public static final int ORDER_TIMEOUT_MINUTES = 30;

    /** 超时扫描单次处理上限，避免大事务 */
    public static final int TIMEOUT_SCAN_BATCH = 100;
}
