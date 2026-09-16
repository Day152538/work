-- ============================================================
-- V5__private_message.sql —— 买家↔卖家下单后的私信
-- 一条订单下两人之间的沟通记录；只允许该订单的买卖双方查看/发送。
-- 会话按 order_id 组织（同一次交易一个窗口），item_id 冗余便于按商品维度扩展。
-- ============================================================

DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `from_user`   BIGINT      NOT NULL COMMENT '发送者',
    `to_user`     BIGINT      NOT NULL COMMENT '接收者',
    `order_id`    BIGINT      NULL COMMENT '关联订单（可选：下单前咨询为空，成交后同窗继续）',
    `item_id`     BIGINT      NOT NULL COMMENT '关联商品（会话以商品为线索）',
    `content`     VARCHAR(1000) NOT NULL COMMENT '消息内容',
    `read_flag`   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time` DATETIME    NOT NULL COMMENT '发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_order_time` (`order_id`, `id`),
    INDEX `idx_from` (`from_user`, `id`),
    INDEX `idx_to_read` (`to_user`, `read_flag`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单买卖双方私信';
