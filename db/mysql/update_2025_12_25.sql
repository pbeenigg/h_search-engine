
-- 2025-12-25 更新酒店表，新增字段：酒店类型、是否可搜索、电话、分数
-- 步骤1：使用INSTANT算法快速添加字段（几乎零停机时间）
ALTER TABLE `hotels`
    ADD COLUMN `accommodation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '酒店类型 （标准酒店库 人工修正字段）',
    ADD COLUMN `search_enable` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '1' COMMENT '是否可搜索  1=可搜索  0=不可搜索(前端搜索需要过滤掉) （标准酒店库 人工修正字段）',
    ADD COLUMN `tel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话 （标准酒店库 人工修正字段）',
    ADD COLUMN `score` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分数 （标准酒店库 人工修正字段）',
    ALGORITHM=INSTANT;

-- 步骤2：使用INPLACE算法添加索引（允许并发读写，但需要时间）
ALTER TABLE `hotels`
    ADD INDEX `idx_search_enable` (`search_enable`),
    ADD INDEX `idx_accommodation_type` (`accommodation_type`),
    ALGORITHM=INPLACE,
    LOCK=NONE;