package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.infra.search.doc.AmapPoiIndexDoc;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

/**
 * 高德POI索引与别名初始化
 * - 检测物理索引 amap_poi 是否存在，不存在则创建
 * - 检测读写别名是否存在，不存在则创建并绑定到物理索引
 * - 索引命名规则：amap_poi（固定名称）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmapPoiIndexInitializer {

    private final ElasticsearchOperations operations;

    // POI索引配置
    private static final String INDEX_NAME = "amap_poi";           // 物理索引名（固定）
    private static final String READ_ALIAS = "amap_poi_read";     // 读取别名
    private static final String WRITE_ALIAS = "amap_poi_write";   // 写入别名

    @PostConstruct
    public void init() {
        try {
            log.info("[AMAP-POI-INIT] ========================================");
            log.info("[AMAP-POI-INIT] 开始初始化POI索引和别名...");
            
            // 1. 检查物理索引是否存在
            IndexOperations indexOps = operations.indexOps(IndexCoordinates.of(INDEX_NAME));
            boolean indexExists = indexOps.exists();
            
            if (!indexExists) {
                log.info("[AMAP-POI-INIT] 物理索引不存在，开始创建: {}", INDEX_NAME);
                createIndex(indexOps);
            } else {
                log.info("[AMAP-POI-INIT] 物理索引已存在: {}", INDEX_NAME);
            }
            
            // 2. 检查读写别名是否存在
            boolean readAliasExists = checkAliasExists(READ_ALIAS);
            boolean writeAliasExists = checkAliasExists(WRITE_ALIAS);
            
            log.info("[AMAP-POI-INIT] 别名检查: readAlias={} (exists={}), writeAlias={} (exists={})", 
                    READ_ALIAS, readAliasExists, WRITE_ALIAS, writeAliasExists);
            
            // 3. 创建缺失的别名
            if (!readAliasExists || !writeAliasExists) {
                createAliases(indexOps, readAliasExists, writeAliasExists);
            } else {
                log.info("[AMAP-POI-INIT] 所有别名已存在，跳过创建");
                log.info("[AMAP-POI-INIT] 当前别名指向: {} -> {}", READ_ALIAS, getAliasTarget(READ_ALIAS));
                log.info("[AMAP-POI-INIT] 当前别名指向: {} -> {}", WRITE_ALIAS, getAliasTarget(WRITE_ALIAS));
            }
            
            log.info("[AMAP-POI-INIT] ========================================");
            log.info("[AMAP-POI-INIT] ✅ 初始化完成！");
            log.info("[AMAP-POI-INIT] 物理索引: {}", INDEX_NAME);
            log.info("[AMAP-POI-INIT] 读别名: {} -> {}", READ_ALIAS, INDEX_NAME);
            log.info("[AMAP-POI-INIT] 写别名: {} -> {}", WRITE_ALIAS, INDEX_NAME);
            log.info("[AMAP-POI-INIT] ========================================");
            
        } catch (Exception e) {
            log.error("[AMAP-POI-INIT] ❌ 索引/别名初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("POI索引初始化失败", e);
        }
    }

    /**
     * 创建物理索引（包含settings和mappings）
     */
    private void createIndex(IndexOperations indexOps) {
        try {
            // 从 AmapPoiIndexDoc 类的注解获取 settings 和 mappings
            IndexOperations entityOps = operations.indexOps(AmapPoiIndexDoc.class);
            var settings = entityOps.createSettings();
            var mapping = entityOps.createMapping();
            
            log.info("[AMAP-POI-INIT] 准备创建索引，包含 settings 和 mappings...");
            boolean created = indexOps.create(settings, mapping);
            
            if (created) {
                log.info("[AMAP-POI-INIT] ✅ 索引创建成功: {}", INDEX_NAME);
            } else {
                log.warn("[AMAP-POI-INIT] 索引创建返回false（可能已存在）: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("[AMAP-POI-INIT] 索引创建失败: {}", e.getMessage(), e);
            throw new RuntimeException("POI索引创建失败", e);
        }
    }

    /**
     * 创建别名
     */
    private void createAliases(IndexOperations indexOps, boolean readAliasExists, boolean writeAliasExists) {
        try {
            AliasActions actions = new AliasActions();
            
            // 添加读别名
            if (!readAliasExists) {
                actions.add(new AliasAction.Add(AliasActionParameters.builder()
                        .withIndices(INDEX_NAME)
                        .withAliases(READ_ALIAS)
                        .build()));
                log.info("[AMAP-POI-INIT] 准备添加读别名: {} -> {}", READ_ALIAS, INDEX_NAME);
            }
            
            // 添加写别名
            if (!writeAliasExists) {
                actions.add(new AliasAction.Add(AliasActionParameters.builder()
                        .withIndices(INDEX_NAME)
                        .withAliases(WRITE_ALIAS)
                        .build()));
                log.info("[AMAP-POI-INIT] 准备添加写别名: {} -> {}", WRITE_ALIAS, INDEX_NAME);
            }
            
            if (!actions.getActions().isEmpty()) {
                indexOps.alias(actions);
                log.info("[AMAP-POI-INIT] ✅ 别名创建成功");
            }
        } catch (Exception e) {
            log.error("[AMAP-POI-INIT] 别名创建失败: {}", e.getMessage(), e);
            throw new RuntimeException("POI别名创建失败", e);
        }
    }

    /**
     * 检查别名是否存在
     */
    private boolean checkAliasExists(String aliasName) {
        try {
            IndexOperations ops = operations.indexOps(IndexCoordinates.of(aliasName));
            return ops.exists();
        } catch (Exception e) {
            log.debug("[AMAP-POI-INIT] 检查别名存在性失败: {}", aliasName, e);
            return false;
        }
    }

    /**
     * 获取别名指向的索引名称
     */
    private String getAliasTarget(String aliasName) {
        try {
            IndexOperations ops = operations.indexOps(IndexCoordinates.of(aliasName));
            var aliasData = ops.getAliases();
            if (aliasData != null && !aliasData.isEmpty()) {
                return String.join(", ", aliasData.keySet());
            }
            return "未知";
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }
}
