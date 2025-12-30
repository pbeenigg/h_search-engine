-- 酒店搜索引擎 - 用户认证授权模块
-- 数据库初始化脚本


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- ================== 应用授权表 ==================
-- ----------------------------
-- Table structure for app
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app` (
                       `app_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用ID（主键）',
                       `secret_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密钥',
                       `encryption_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密密钥',
                       `rate_limit` int NOT NULL DEFAULT '1000' COMMENT '限流阈值（次/分钟）',
                       `timeout` int NOT NULL DEFAULT '-1' COMMENT '过期时间（小时）-1=永不过期，大于-1=过期时长',
                       `update_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                       `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
                       `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
                       PRIMARY KEY (`app_id`),
                       KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用授权表';
-- ================== 应用授权表结束 ==================



-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
                        `user_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                        `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密）',
                        `user_nick` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
                        `sex` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'U' COMMENT '性别：M=男，F=女，U=未知',
                        `timeout` int NOT NULL DEFAULT '-1' COMMENT '过期时间（小时）-1=永不过期，大于-1=过期时长',
                        `app_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联应用ID',
                        `update_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
                        `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
                        PRIMARY KEY (`user_id`),
                        UNIQUE KEY `idx_user_name` (`user_name`),
                        KEY `idx_user_app_id` (`app_id`),
                        CONSTRAINT `fk_user_app_id` FOREIGN KEY (`app_id`) REFERENCES `app` (`app_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ================== 初始数据 ==================

-- 初始化应用数据
INSERT INTO `app` (`app_id`, `secret_key`, `encryption_key`, `rate_limit`, `timeout`, `create_by`, `update_by`)
VALUES ('heytrip_hotel_search_admin', 'HeyTrip@Admin#HotelSearch!2025', '427ae41e4649b934ca495991b7852b855', 1000, 720, 'admin', 'admin')
    ON DUPLICATE KEY UPDATE
                         `secret_key` = VALUES(`secret_key`),
                         `encryption_key` = VALUES(`encryption_key`),
                         `rate_limit` = VALUES(`rate_limit`),
                         `timeout` = VALUES(`timeout`),
                         `update_by` = VALUES(`update_by`),
                         `update_at` = CURRENT_TIMESTAMP;

-- 初始化管理员账号（密码：admin123）
-- 密码已使用BCrypt加密，hash值如下
INSERT INTO `user` (`user_id`, `user_name`, `password`, `user_nick`, `sex`, `timeout`, `app_id`, `create_by`, `update_by`)
VALUES (1, 'admin', '$2a$10$qapyfP0pJbMb59K4gcluT.DrInAeNR8LF3h3cl1i80mCtFmKWzA.K', '系统管理员', 'U', -1, 'heytrip_hotel_search_admin', 'admin', 'admin')
    ON DUPLICATE KEY UPDATE
                         `user_name` = VALUES(`user_name`),
                         `password` = VALUES(`password`),
                         `user_nick` = VALUES(`user_nick`),
                         `sex` = VALUES(`sex`),
                         `timeout` = VALUES(`timeout`),
                         `app_id` = VALUES(`app_id`),
                         `update_by` = VALUES(`update_by`),
                         `update_at` = CURRENT_TIMESTAMP;

-- ================== 数据验证 ==================
-- 验证表数据记录数
SELECT 'app表记录' as table_name, COUNT(*) as record_count FROM `app`
UNION ALL
SELECT 'user表记录' as table_name, COUNT(*) as record_count FROM `user`;

-- 验证关联关系
SELECT
    a.app_id,
    a.rate_limit,
    a.timeout as app_timeout,
    u.user_id,
    u.user_name,
    u.user_nick,
    u.timeout as user_timeout
FROM `app` a
         LEFT JOIN `user` u ON a.app_id = u.app_id
WHERE a.app_id = 'heytrip_hotel_search_admin';
