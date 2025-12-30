/*
 MySQL Dump SQL
 
 Source: PostgreSQL hotel_search database
 Target: MySQL 8.0+
 Converted: 2024-12-05
*/

-- 设置字符集和存储引擎
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for api_request_log
-- ----------------------------
DROP TABLE IF EXISTS `api_request_log`;
CREATE TABLE `api_request_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `trace_id` VARCHAR(64) NOT NULL COMMENT '与同步日志相关联的追踪ID',
  `job_code` VARCHAR(64) NOT NULL COMMENT '任务编码（对应 job_schedule.job_code）',
  `source` VARCHAR(32) NOT NULL COMMENT '来源标识（CN/INTL 等）',
  `http_method` VARCHAR(8) NOT NULL COMMENT 'HTTP 方法（GET/POST 等）',
  `url` TEXT NOT NULL COMMENT '请求URL',
  `request_headers` JSON NOT NULL COMMENT '请求头（JSON）',
  `response_status` INT DEFAULT NULL COMMENT '响应状态码',
  `duration_ms` INT DEFAULT NULL COMMENT '请求耗时（毫秒）',
  `request_body_compressed` LONGBLOB COMMENT '压缩后的请求体字节',
  `response_body_compressed` LONGBLOB COMMENT '压缩后的响应体字节',
  `request_size_bytes` INT DEFAULT NULL COMMENT '请求体大小（字节）',
  `response_size_bytes` INT DEFAULT NULL COMMENT '响应体大小（字节）',
  `compression` ENUM('none','gzip','zstd','lz4') NOT NULL DEFAULT 'gzip' COMMENT '压缩算法',
  `app` VARCHAR(64) DEFAULT NULL COMMENT '供应商应用标识',
  `timestamp_utc` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '请求发生时间（UTC）',
  `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_api_log_trace` (`trace_id`),
  KEY `idx_api_log_job` (`job_code`),
  KEY `idx_api_log_status` (`response_status`),
  KEY `idx_api_log_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部API请求日志（请求/响应原文采用压缩字节存储）';

-- ----------------------------
-- Table structure for hotel_poi
-- ----------------------------
DROP TABLE IF EXISTS `hotel_poi`;
CREATE TABLE `hotel_poi` (
  `id` VARCHAR(50) NOT NULL COMMENT 'POI ID',
  `name` TEXT COMMENT 'POI名称',
  `type` TEXT COMMENT 'POI类型',
  `typeCode` TEXT COMMENT 'POI类型编码',
  `address` TEXT COMMENT 'POI地址',
  `location` VARCHAR(100) COMMENT 'POI坐标（经度,纬度）',
  `tel` TEXT COMMENT 'POI电话',
  `pcode` VARCHAR(20) COMMENT '省份编码',
  `pname` TEXT COMMENT '省份名称',
  `cityname` TEXT COMMENT '城市名称',
  `adname` TEXT COMMENT '区县名称',
  `adcode` VARCHAR(20) COMMENT '区县编码',
  `citycode` VARCHAR(20) COMMENT '城市编码',
  `parent` VARCHAR(50) COMMENT '父级POI ID',
  `distance` VARCHAR(50) COMMENT '距离',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='高德POI数据表';

-- ----------------------------
-- Table structure for hotels
-- ----------------------------
DROP TABLE IF EXISTS `hotels`;
CREATE TABLE `hotels` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
  `source` VARCHAR(32) NOT NULL COMMENT '数据源：Elong=艺龙；Agoda=安可达',
  `tag_source` VARCHAR(32) NOT NULL COMMENT '标签源：CN=国内；INTL=国际；GAT=港澳台',
  `hotel_id` BIGINT NOT NULL COMMENT '酒店ID（供应商体系内的酒店标识）',
  `hotel_name_cn` TEXT COMMENT '酒店中文名（从原文解析或翻译得到）',
  `hotel_name_en` TEXT COMMENT '酒店英文名（从原文解析）',
  `new_hotel_name_cn` TEXT COMMENT '新中文名（数据源返回的标准化名称或内部清洗后的结果）',
  `new_hotel_name_en` TEXT COMMENT '新英文名（数据源返回的标准化名称或内部清洗后的结果）',
  `country_cn` VARCHAR(100) COMMENT '国家中文名',
  `country_en` VARCHAR(100) COMMENT '国家英文名',
  `country_code` VARCHAR(10) COMMENT '国家代码（ISO 3166-1 alpha-2，如 CN/HK/MO/TW/US 等）',
  `city_cn` VARCHAR(100) COMMENT '城市中文名',
  `city_en` VARCHAR(100) COMMENT '城市英文名',
  `region_cn` VARCHAR(100) COMMENT '区域/区县中文名',
  `region_en` VARCHAR(100) COMMENT '区域/区县英文名',
  `continent_cn` VARCHAR(50) COMMENT '洲中文名（如：亚洲、欧洲、北美洲等）',
  `continent_en` VARCHAR(50) COMMENT '洲英文名（如：Asia、Europe、North America 等）',
  `address_cn` TEXT COMMENT '酒店地址中文原文',
  `address_en` TEXT COMMENT '酒店地址英文原文',
  `longitude` DECIMAL(10,7) COMMENT '经度（-180 ~ 180）',
  `latitude` DECIMAL(10,7) COMMENT '纬度（-90 ~ 90）',
  `hotel_group_cn` VARCHAR(200) COMMENT '酒店集团中文名（如：万豪、希尔顿等）',
  `hotel_group_en` VARCHAR(200) COMMENT '酒店集团英文名（如：Marriott、Hilton 等）',
  `hotel_brand_cn` VARCHAR(200) COMMENT '酒店品牌中文名（如：JW万豪、康莱德等）',
  `hotel_brand_en` VARCHAR(200) COMMENT '酒店品牌英文名（如：JW Marriott、Conrad 等）',
  `fetched_at` TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT '数据抓取时间（入库时间戳）',
  `description_cn` TEXT COMMENT '酒店描述（中文）',
  `description_en` TEXT COMMENT '酒店描述（英文）',
  `raw_compressed` LONGTEXT COMMENT '酒店原文（GZIP 压缩）',
  `updated_at` TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hotels_source_hotel` (`source`, `hotel_id`),
  KEY `idx_hotels_hotel_id` (`hotel_id`),
  KEY `idx_hotels_tag_source` (`tag_source`),
  KEY `idx_hotels_city` (`city_cn`(100), `city_en`(100)),
  KEY `idx_hotels_country` (`country_cn`(100), `country_en`(100), `country_code`),
  KEY `idx_hotels_continent` (`continent_cn`, `continent_en`),
  KEY `idx_hotels_region` (`region_cn`(100), `region_en`(100)),
  KEY `idx_hotels_brand` (`hotel_brand_cn`(100), `hotel_brand_en`(100)),
  KEY `idx_hotels_hotel_group` (`hotel_group_cn`(100), `hotel_group_en`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='酒店详情 Staging 表：存放采集到的酒店原文与基础解析字段';

-- ----------------------------
-- Table structure for job_runtime_state
-- ----------------------------
DROP TABLE IF EXISTS `job_runtime_state`;
CREATE TABLE `job_runtime_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `job_code` VARCHAR(64) NOT NULL COMMENT '任务编码',
  `watermark_max_hotel_id` BIGINT NOT NULL DEFAULT 0 COMMENT '基于供应商API的 maxHotelId 断点续跑水位',
  `last_started_at` TIMESTAMP(6)  COMMENT '上次开始时间',
  `last_finished_at` TIMESTAMP(6)  COMMENT '上次结束时间',
  `last_status` VARCHAR(20) DEFAULT NULL COMMENT '上次运行状态（SUCCESS/FAILED/RUNNING）',
  `updated_at` TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_runtime_state_job_code` (`job_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务运行时状态（水位线断点等）';

-- ----------------------------
-- Table structure for job_schedule
-- ----------------------------
DROP TABLE IF EXISTS `job_schedule`;
CREATE TABLE `job_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `job_code` VARCHAR(64) NOT NULL COMMENT '任务编码（唯一）',
  `cron_expr` VARCHAR(64) NOT NULL COMMENT 'Cron 表达式',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '任务开关',
  `max_concurrency` INT NOT NULL DEFAULT 10 COMMENT '并发上限',
  `batch_size` INT NOT NULL DEFAULT 1000 COMMENT '批量提交大小',
  `http_timeout_sec` INT NOT NULL DEFAULT 20 COMMENT 'HTTP 超时（秒）',
  `params` JSON NOT NULL DEFAULT ('{}') COMMENT '自定义业务参数（JSON）',
  `sync_mode` VARCHAR(16) NOT NULL DEFAULT 'FULL' COMMENT '同步范围：FULL=全量；LIMIT=仅同步指定条数',
  `sync_limit` BIGINT NOT NULL DEFAULT 0 COMMENT '当 sync_mode=LIMIT 时的最大同步条数（0表示不限制）',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `updated_at` TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_schedule_job_code` (`job_code`),
  KEY `idx_job_schedule_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务调度配置（DB驱动Cron），含业务参数与同步范围';

-- ----------------------------
-- Records of job_schedule
-- ----------------------------
INSERT INTO `job_schedule` VALUES (1, 'HOTEL_FULL_SYNC_ALL', '0 0 */2 * * ?', 1, 50, 1000, 60, '{"batchSize": 1000, "retryTimes": 3, "maxPages": 100, "retryDelayMs": 3000, "retryJitterMs": 2000, "hotelIdsBatchSize": 10000, "retryBaseDelayMs429": 1000, "hotelDetailBatchSize": 20}', 'FULL', 0, '酒店全量同步（按 maxHotelId 遍历）', CURRENT_TIMESTAMP(6));
INSERT INTO `job_schedule` VALUES (2, 'HOTEL_RETRY_BY_SYNC_LOG', '0 0 0 1 1 ? 2099', 0, 50, 1000, 60, '{"retry": {"multiplier": 2, "maxAttempts": 3, "initialBackoffMs": 1000}, "maxPages": 100, "batchSize": 1000, "syncLogId": 0}', 'LIMIT', 0, '失败明细补偿：按 sync_log_id 重放', CURRENT_TIMESTAMP(6));

-- ----------------------------
-- Table structure for search_logs
-- ----------------------------
DROP TABLE IF EXISTS `search_logs`;
CREATE TABLE `search_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `query` VARCHAR(500) NOT NULL COMMENT '用户查询词',
  `tag_source` VARCHAR(32) DEFAULT NULL COMMENT '业务域（CN/INTL/HMT）',
  `result_count` INT DEFAULT NULL COMMENT '搜索结果数量',
  `clicked_hotel_id` BIGINT DEFAULT NULL COMMENT '点击的酒店ID',
  `click_position` INT DEFAULT NULL COMMENT '点击位置（排名）',
  `duration_ms` BIGINT DEFAULT NULL COMMENT '查询耗时（毫秒）',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（可选）',
  `user_ip` VARCHAR(64) DEFAULT NULL COMMENT '用户IP地址',
  `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_logs_query` (`query`(255)),
  KEY `idx_search_logs_tag_source` (`tag_source`),
  KEY `idx_search_logs_result_count` (`result_count`),
  KEY `idx_search_logs_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索日志（用于搜索质量分析）';

-- ----------------------------
-- Table structure for sync_log
-- ----------------------------
DROP TABLE IF EXISTS `sync_log`;
CREATE TABLE `sync_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `job_code` VARCHAR(64) NOT NULL COMMENT '任务编码',
  `trace_id` VARCHAR(64) NOT NULL COMMENT '运行追踪ID（与 api_request_log 关联）',
  `started_at` TIMESTAMP(6) NOT NULL COMMENT '开始时间',
  `finished_at` TIMESTAMP(6)  COMMENT '结束时间',
  `cost_seconds` INT DEFAULT NULL COMMENT '总耗时（秒）',
  `total_ids` BIGINT DEFAULT 0 COMMENT '本次运行期间拉取到的酒店ID数量',
  `total_details` BIGINT DEFAULT 0 COMMENT '成功解析并落地的酒店详情记录数',
  `success_count` BIGINT DEFAULT 0 COMMENT '成功计数',
  `fail_count` BIGINT DEFAULT 0 COMMENT '失败计数',
  `status` VARCHAR(20) DEFAULT 'RUNNING' COMMENT '运行状态（RUNNING/SUCCESS/FAILED/PARTIAL）',
  `message` TEXT COMMENT '附加信息/备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_log_job_code_trace_id` (`job_code`, `trace_id`),
  KEY `idx_sync_log_job_code` (`job_code`),
  KEY `idx_sync_log_started_at` (`started_at`),
  KEY `idx_sync_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步日志汇总（全局计数器）';

-- ----------------------------
-- Table structure for sync_log_detail
-- ----------------------------
DROP TABLE IF EXISTS `sync_log_detail`;
CREATE TABLE `sync_log_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `sync_log_id` BIGINT NOT NULL COMMENT '关联的同步日志ID',
  `hotel_id` BIGINT DEFAULT NULL COMMENT '酒店ID（可为空）',
  `stage` VARCHAR(32) DEFAULT NULL COMMENT '阶段（IDS_FETCH/DETAIL_FETCH/TRANSFORM/SINK/INDEX_*）',
  `error_code` VARCHAR(64) DEFAULT NULL COMMENT '错误码',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_log_detail_log_id` (`sync_log_id`),
  KEY `idx_sync_log_detail_hotel_id` (`hotel_id`),
  KEY `idx_sync_log_detail_stage` (`stage`),
  CONSTRAINT `fk_sync_log_detail_sync_log` FOREIGN KEY (`sync_log_id`) REFERENCES `sync_log` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步失败明细（支持按 sync_log_id 重跑补偿）';

-- ----------------------------
-- Enable foreign key checks
-- ----------------------------
SET FOREIGN_KEY_CHECKS = 1;
