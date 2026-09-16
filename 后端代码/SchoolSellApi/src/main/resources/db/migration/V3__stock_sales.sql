-- ============================================================
-- V3__stock_sales.sql
-- 商品库存 + 销量（在 itsource_18 库执行）
--
-- 背景：IdleItemModel 早已声明 stock/salesCount 两个字段并给出语义
--   （stock：多件可售，下单不扣、付款时 -1，付款后取消回退 +1；
--    salesCount：销量，付款成功 +1），但一直只有注释没有落地：
--   表无列、Mapper 不映射、订单状态机也不维护。本次把该功能补齐。
--
-- 1. 新增列（老数据单件默认库存 1、销量 0）
-- 2. 历史数据回填：老系统一件商品只卖一次，按已付款订单回填销量；
--    已有付款记录的商品按“已售罄”处理——仍挂着在售(idle_status=1)的
--    一并自动下架(idle_status=2)，与“库存为 0 自动下架”语义保持一致。
-- ============================================================

ALTER TABLE sh_idle_item
    ADD COLUMN stock INT NOT NULL DEFAULT 1 COMMENT '库存数量（多件可售；付款时 -1，售罄自动下架）' AFTER idle_status,
    ADD COLUMN sales_count INT NOT NULL DEFAULT 0 COMMENT '销量（付款成功 +1）' AFTER stock;

-- 回填销量，并修正“有付款记录却仍在售”的历史脏数据
UPDATE sh_idle_item i
LEFT JOIN (
    SELECT idle_id, COUNT(*) AS paid_cnt
    FROM sh_order
    WHERE payment_status = 1
    GROUP BY idle_id
) t ON t.idle_id = i.id
SET i.sales_count = COALESCE(t.paid_cnt, 0),
    i.stock       = CASE WHEN COALESCE(t.paid_cnt, 0) > 0 THEN 0 ELSE 1 END,
    i.idle_status = CASE WHEN COALESCE(t.paid_cnt, 0) > 0 AND i.idle_status = 1 THEN 2 ELSE i.idle_status END;
