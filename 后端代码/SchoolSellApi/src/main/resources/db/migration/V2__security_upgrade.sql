-- ============================================================
-- V2__security_upgrade.sql
-- 安全与功能整改的数据库结构变更（在 itsource_18 库执行）
--
-- 1. 密码列扩容：BCrypt 哈希固定 60 字符，原 varchar(16) 只能存明文
-- 2. 新增 vip_expire_time：会员到期时间。
--    原设计把“会员”塞进 user_status=3，与封禁状态(1)复用同一字段，
--    导致“开通会员”实际上是把封禁标记字段改掉，语义混乱且可被前端任意篡改。
--    现拆分为独立字段，由后端 /user/vip/subscribe 接口写入。
-- ============================================================

ALTER TABLE sh_user
    MODIFY COLUMN user_password VARCHAR(72) NOT NULL COMMENT 'BCrypt 密码哈希';

ALTER TABLE sh_user
    ADD COLUMN vip_expire_time DATETIME NULL DEFAULT NULL COMMENT '会员到期时间，NULL 表示非会员';

ALTER TABLE sh_admin
    MODIFY COLUMN admin_password VARCHAR(72) NOT NULL COMMENT 'BCrypt 密码哈希';
