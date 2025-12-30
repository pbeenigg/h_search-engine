-- =============================================
-- 高德POI数据采集系统 - 数据库初始化脚本
-- =============================================

-- 1. 创建高德POI数据表
CREATE TABLE IF NOT EXISTS `amap_poi` (
                                          `id` VARCHAR(50) PRIMARY KEY COMMENT 'POI唯一标识',
    `name` VARCHAR(255) NOT NULL COMMENT '地标名称',
    `type` VARCHAR(255) COMMENT 'POI类型',
    `typecode` VARCHAR(100) COMMENT 'POI类型代码',
    `address` VARCHAR(500) COMMENT '地址',
    `location` VARCHAR(50) COMMENT '经纬度坐标 格式:lng,lat',
    `longitude` DECIMAL(10, 6) COMMENT '经度',
    `latitude` DECIMAL(9, 6) COMMENT '纬度',
    `pcode` VARCHAR(30) COMMENT '省份代码',
    `pname` VARCHAR(50) COMMENT '省份名称',
    `citycode` VARCHAR(30) COMMENT '城市代码',
    `cityname` VARCHAR(50) COMMENT '城市名称',
    `adcode` VARCHAR(30) COMMENT '区县代码',
    `adname` VARCHAR(50) COMMENT '区县名称',
    `parent` VARCHAR(50) COMMENT '上级行政区划',
    `distance` VARCHAR(50) COMMENT '距离',
    `data_version` VARCHAR(20) COMMENT '数据版本号',
    `source_batch` VARCHAR(50) COMMENT '采集批次标识',
    `data_hash` VARCHAR(200) COMMENT '数据哈希值（MD5，用于快速变更检测）',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_citycode` (`citycode`),
    INDEX `idx_adcode` (`adcode`),
    INDEX `idx_typecode` (`typecode`),
    INDEX `idx_location` (`longitude`, `latitude`),
    INDEX `idx_name` (`name`),
    INDEX `idx_data_hash` (data_hash),
    INDEX `idx_source_batch` (`source_batch`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高德POI数据表';

-- 2. 创建高德城市代码表
CREATE TABLE IF NOT EXISTS `amap_citycode` (
                                               `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                               `name` VARCHAR(100) NOT NULL COMMENT '名称',
    `adcode` VARCHAR(30) NOT NULL COMMENT '区县代码',
    `citycode` VARCHAR(30) COMMENT '城市代码',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE INDEX `uk_adcode` (`adcode`),
    INDEX `idx_citycode` (`citycode`),
    INDEX `idx_name` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高德城市代码表';

-- 3. 创建高德POI类型代码表
CREATE TABLE IF NOT EXISTS `amap_poitype` (
                                              `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                              `typecode` VARCHAR(20) NOT NULL COMMENT 'POI类型代码',
    `big_category_cn` VARCHAR(100) COMMENT '大类中文名称',
    `mid_category_cn` VARCHAR(100) COMMENT '中类中文名称',
    `sub_category_cn` VARCHAR(100) COMMENT '小类中文名称',
    `big_category_en` VARCHAR(100) COMMENT '大类英文名称',
    `mid_category_en` VARCHAR(100) COMMENT '中类英文名称',
    `sub_category_en` VARCHAR(100) COMMENT '小类英文名称',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE INDEX `uk_typecode` (`typecode`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高德POI类型代码表';

-- 4. 创建地图数据同步日志表
CREATE TABLE IF NOT EXISTS `map_data_sync_log` (
                                                   `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志唯一标识',
                                                   `source` VARCHAR(50) NOT NULL COMMENT '数据来源(amap/openStreetMap)',
    `data_type` VARCHAR(50) NOT NULL COMMENT '数据类型(citycode/poitype/poi)',
    `total_count` BIGINT DEFAULT 0 COMMENT '总记录数',
    `success_count` BIGINT DEFAULT 0 COMMENT '成功记录数',
    `failure_count` BIGINT DEFAULT 0 COMMENT '失败记录数',
    `start_time` TIMESTAMP NOT NULL COMMENT '开始时间',
    `end_time` TIMESTAMP COMMENT '结束时间',
    `duration` INT COMMENT '耗时（秒）',
    `status` VARCHAR(20) NOT NULL COMMENT '状态(RUNNING/SUCCESS/FAILED)',
    `error_message` TEXT COMMENT '错误信息',
    `trace_id` VARCHAR(100) COMMENT '追踪ID',
    `extra_info` TEXT COMMENT '附加信息',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_source` (`source`),
    INDEX `idx_data_type` (`data_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_trace_id` (`trace_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地图数据同步日志表';

-- =============================================
-- 插入job_schedule定时任务配置
-- =============================================

-- 插入POI采集定时任务（每月1号执行）
INSERT INTO `job_schedule` (
    `job_code`,
    `cron_expr`,
    `enabled`,
    `max_concurrency`,
    `batch_size`,
    `http_timeout_sec`,
    `params`,
    `remark`,
    `created_at`,
    `updated_at`
) VALUES (
             'AMAP_POI_COLLECT',
             '0 0 0 1 * ?',  -- 每月1号0点执行
             TRUE,
             10,
             1000,
             60,
             '{"pageSize":25,"dbCommitSize":1000,"cityLimit":true}',
             '高德POI数据采集任务（每月执行1次）',
             NOW(),
             NOW()
         ) ON DUPLICATE KEY UPDATE
    `cron_expr` = VALUES(`cron_expr`),
    `enabled` = VALUES(`enabled`),
    `max_concurrency` = VALUES(`max_concurrency`),
    `batch_size` = VALUES(`batch_size`),
    `http_timeout_sec` = VALUES(`http_timeout_sec`),
    `params` = VALUES(`params`),
    `remark` = VALUES(`remark`),
    `updated_at` = NOW();

-- 插入城市代码同步定时任务（每3个月1号执行）
INSERT INTO `job_schedule` (
    `job_code`,
    `cron_expr`,
    `enabled`,
    `max_concurrency`,
    `batch_size`,
    `http_timeout_sec`,
    `params`,
    `remark`,
    `created_at`,
    `updated_at`
) VALUES (
             'AMAP_CITYCODE_SYNC',
             '0 0 0 1 */3 ?',  -- 每3个月1号0点执行
             TRUE,
             1,
             1000,
             30,
             '{}',
             '高德城市代码同步任务（每3个月执行1次）',
             NOW(),
             NOW()
         ) ON DUPLICATE KEY UPDATE
    `cron_expr` = VALUES(`cron_expr`),
    `enabled` = VALUES(`enabled`),
    `max_concurrency` = VALUES(`max_concurrency`),
    `batch_size` = VALUES(`batch_size`),
    `http_timeout_sec` = VALUES(`http_timeout_sec`),
    `params` = VALUES(`params`),
    `remark` = VALUES(`remark`),
    `updated_at` = NOW();

-- 插入POI类型同步定时任务（每3个月1号执行）
INSERT INTO `job_schedule` (
    `job_code`,
    `cron_expr`,
    `enabled`,
    `max_concurrency`,
    `batch_size`,
    `http_timeout_sec`,
    `params`,
    `remark`,
    `created_at`,
    `updated_at`
) VALUES (
             'AMAP_POITYPE_SYNC',
             '0 0 0 1 */3 ?',  -- 每3个月1号0点执行
             TRUE,
             1,
             1000,
             30,
             '{}',
             '高德POI类型代码同步任务（每3个月执行1次）',
             NOW(),
             NOW()
         ) ON DUPLICATE KEY UPDATE
    `cron_expr` = VALUES(`cron_expr`),
    `enabled` = VALUES(`enabled`),
    `max_concurrency` = VALUES(`max_concurrency`),
    `batch_size` = VALUES(`batch_size`),
    `http_timeout_sec` = VALUES(`http_timeout_sec`),
    `params` = VALUES(`params`),
    `remark` = VALUES(`remark`),
    `updated_at` = NOW();

-- =============================================
-- 验证查询
-- =============================================

-- 查看所有高德相关的定时任务
SELECT
    job_code,
    cron_expr,
    enabled,
    max_concurrency,
    batch_size,
    remark,
    created_at
FROM job_schedule
WHERE job_code LIKE 'AMAP_%'
ORDER BY job_code;

-- 查看表结构
SHOW CREATE TABLE amap_poi;
SHOW CREATE TABLE amap_citycode;
SHOW CREATE TABLE amap_poitype;
SHOW CREATE TABLE map_data_sync_log;
