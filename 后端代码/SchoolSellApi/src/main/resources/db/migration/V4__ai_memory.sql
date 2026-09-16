-- ============================================================
-- V4__ai_memory.sql —— AI 对话持久化 + 用户长期记忆
-- M0：ai_chat_message 存每轮对话，按 conversation_id 可按用户取回，刷新不丢。
-- M1：ai_user_memory 存该用户的“长期画像摘要 + 已摘要到哪条消息”，开场自动注入。
-- ============================================================

DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话id（一次对话的标识）',
    `user_id`         BIGINT      NOT NULL COMMENT '归属用户（只能查自己）',
    `role`            VARCHAR(16) NOT NULL COMMENT 'user / assistant',
    `content`         TEXT        NOT NULL COMMENT '消息内容',
    `create_time`     DATETIME    NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conv_user` (`conversation_id`, `user_id`, `id`),
    INDEX `idx_user_id` (`user_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 对话消息（持久化，刷新不丢）';

DROP TABLE IF EXISTS `ai_user_memory`;
CREATE TABLE `ai_user_memory` (
    `user_id`           BIGINT   NOT NULL COMMENT '用户主键',
    `summary`           TEXT     NULL COMMENT '该用户的长期画像/偏好摘要（开场自动注入）',
    `summarized_upto_id` BIGINT   NOT NULL DEFAULT 0 COMMENT '已摘要到 ai_chat_message 的哪条 id（用于增量摘要）',
    `update_time`       DATETIME NOT NULL,
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 用户长期记忆';
