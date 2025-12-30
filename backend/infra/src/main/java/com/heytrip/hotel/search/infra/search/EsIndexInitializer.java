package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.infra.config.HotelSearchWeights;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 索引与别名初始化（适配动态索引名模式）：
 * - 如果别名不存在，创建初始索引（带时间戳）并设置别名
 * - 如果别名已存在，跳过初始化（由重建脚本管理）
 * - 索引命名规则：hotels_YYYYMMDD_HHMMSS
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer {

    private final HotelSearchWeights hotelSearchWeights;

    private final ElasticsearchOperations operations;

    @PostConstruct
    public void init() {
        try {
            String readAlias = hotelSearchWeights.getReadAlias();
            String writeAlias = hotelSearchWeights.getWriteAlias();
            
            // 检查读别名是否已存在
            boolean readAliasExists = checkAliasExists(readAlias);
            boolean writeAliasExists = checkAliasExists(writeAlias);
            
            if (readAliasExists && writeAliasExists) {
                log.info("[ES-INIT] 别名已存在，跳过初始化 (readAlias={}, writeAlias={})", readAlias, writeAlias);
                log.info("[ES-INIT] 当前别名指向: {} -> {}", readAlias, getAliasTarget(readAlias));
                log.info("[ES-INIT] 当前别名指向: {} -> {}", writeAlias, getAliasTarget(writeAlias));
                return;
            }
            
            // 别名不存在，创建初始索引
            log.info("[ES-INIT] 别名不存在，开始创建初始索引和别名...");
            
            // 生成带时间戳的索引名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String initialIndexName = "hotels_" + timestamp;
            
            log.info("[ES-INIT] 创建初始索引: {}", initialIndexName);
            
            // 1) 创建索引（基于实体映射创建 settings + mappings）
            IndexOperations entityOps = operations.indexOps(HotelIndexDoc.class);
            IndexCoordinates indexCoordinates = IndexCoordinates.of(initialIndexName);
            IndexOperations indexOps = operations.indexOps(indexCoordinates);
            
            if (!indexOps.exists()) {
                // 创建索引（同时应用 settings 和 mappings）
                // 从 HotelIndexDoc 类的 @Setting 注解获取 settings
                var settings = entityOps.createSettings();
                var mapping = entityOps.createMapping();
                
                log.info("[ES-INIT] 准备创建索引，包含 settings 和 mappings...");
                boolean created = indexOps.create(settings, mapping);
                log.info("[ES-INIT] 索引创建成功: {} (created={})", initialIndexName, created);
            } else {
                log.warn("[ES-INIT] 索引已存在: {}", initialIndexName);
            }
            
            // 2) 设置别名
            AliasActions actions = new AliasActions();
            
            // 添加读别名
            if (!readAliasExists) {
                actions.add(new AliasAction.Add(AliasActionParameters.builder()
                        .withIndices(initialIndexName)
                        .withAliases(readAlias)
                        .build()));
                log.info("[ES-INIT] 添加读别名: {} -> {}", readAlias, initialIndexName);
            }
            
            // 添加写别名
            if (!writeAliasExists) {
                actions.add(new AliasAction.Add(AliasActionParameters.builder()
                        .withIndices(initialIndexName)
                        .withAliases(writeAlias)
                        .build()));
                log.info("[ES-INIT] 添加写别名: {} -> {}", writeAlias, initialIndexName);
            }
            
            if (!actions.getActions().isEmpty()) {
                indexOps.alias(actions);
                log.info("[ES-INIT] ✅ 别名设置成功");
            }
            
            log.info("[ES-INIT] ========================================");
            log.info("[ES-INIT] 初始化完成！");
            log.info("[ES-INIT] 索引名称: {}", initialIndexName);
            log.info("[ES-INIT] 读别名: {} -> {}", readAlias, initialIndexName);
            log.info("[ES-INIT] 写别名: {} -> {}", writeAlias, initialIndexName);
            log.info("[ES-INIT] ========================================");
            
        } catch (Exception e) {
            log.error("[ES-INIT] ❌ 索引/别名初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Elasticsearch 索引初始化失败", e);
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
