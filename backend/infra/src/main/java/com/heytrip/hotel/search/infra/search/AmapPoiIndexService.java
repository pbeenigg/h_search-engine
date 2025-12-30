package com.heytrip.hotel.search.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.heytrip.hotel.search.common.util.HanlpUtils;
import com.heytrip.hotel.search.domain.entity.AmapPoi;
import com.heytrip.hotel.search.infra.nlp.NlpEnrichmentService;
import com.heytrip.hotel.search.infra.search.doc.AmapPoiIndexDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 高德POI ES索引服务
 * 用途：地标识别、附近酒店定位
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapPoiIndexService {
    
    private final ElasticsearchClient elasticsearchClient;
    private final NlpEnrichmentService nlpEnrichmentService;
    
    // POI索引配置
    private static final String INDEX_NAME = "amap_poi";           // 实际索引名
    private static final String READ_ALIAS = "amap_poi_read";     // 读取别名
    private static final String WRITE_ALIAS = "amap_poi_write";   // 写入别名
    
    /**
     * 批量索引POI数据
     *
     * @param pois POI列表
     * @return [成功数, 失败数]
     */
    public long[] bulkIndex(List<AmapPoi> pois) {
        if (pois == null || pois.isEmpty()) {
            return new long[]{0, 0};
        }
        
        try {
            // 构建批量索引请求
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            
            for (AmapPoi poi : pois) {
                AmapPoiIndexDoc doc = convertToDoc(poi);
                bulkBuilder.operations(BulkOperation.of(op -> op
                        .index(idx -> idx
                                .index(WRITE_ALIAS)  // 使用写别名进行索引
                                .id(doc.getId())
                                .document(doc)
                        )
                ));
            }
            
            // 执行批量索引
            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
            
            // 统计结果
            long successCount = 0;
            long failureCount = 0;
            
            if (response.items() != null) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        failureCount++;
                        log.warn("[AmapPoiIndexService] 索引失败: id={}, error={}", 
                                item.id(), item.error().reason());
                    } else {
                        successCount++;
                    }
                }
            }
            
            log.info("[AmapPoiIndexService] 批量索引完成: 成功={}, 失败={}", successCount, failureCount);
            return new long[]{successCount, failureCount};
            
        } catch (Exception e) {
            log.error("[AmapPoiIndexService] 批量索引异常", e);
            return new long[]{0, pois.size()};
        }
    }
    
    /**
     * 转换POI实体为ES文档
     */
    private AmapPoiIndexDoc convertToDoc(AmapPoi poi) {
        AmapPoiIndexDoc doc = AmapPoiIndexDoc.builder()
                .id(poi.getId())
                .name(poi.getName())
                .type(poi.getType())
                .typecode(poi.getTypecode())
                .address(poi.getAddress())
                .citycode(poi.getCitycode())
                .cityname(poi.getCityname())
                .adcode(poi.getAdcode())
                .adname(poi.getAdname())
                .pcode(poi.getPcode())
                .pname(poi.getPname())
                .sourceBatch(poi.getSourceBatch())
                .build();
        
        // 设置经纬度
        if (poi.getLongitude() != null && poi.getLatitude() != null) {
            double lat = poi.getLatitude().doubleValue();
            double lon = poi.getLongitude().doubleValue();
            
            // 验证坐标有效性
            if (isValidCoordinate(lat, lon)) {
                doc.setLat(lat);
                doc.setLon(lon);
                
                // ES geo_point格式
                Map<String, Object> location = new HashMap<>();
                location.put("lat", lat);
                location.put("lon", lon);
                doc.setLocation(location);
            }
        }
        
        // ===== 分词增强：生成nameTokens和typeTokens =====
        try {
            // 1. POI名称分词（使用精细分词）
            if (poi.getName() != null && !poi.getName().isEmpty()) {
                List<String> rawNameTokens = nlpEnrichmentService.tokenizeFine(poi.getName());
                List<String> filteredNameTokens = HanlpUtils.filterValidTokens(rawNameTokens);
                doc.setNameTokens(filteredNameTokens);
                //log.debug("[POI分词] name='{}' -> tokens={}", poi.getName(), filteredNameTokens);
            }
            
            // 2. POI类型分词（使用标准分词）
            if (poi.getType() != null && !poi.getType().isEmpty()) {
                // POI类型通常是"餐饮服务;中餐厅;川菜馆"格式，先按分隔符切分
                List<String> typeTokens = new ArrayList<>();
                String[] typeParts = poi.getType().split("[/,，、|]+");
                for (String part : typeParts) {
                    if (part != null && !part.trim().isEmpty()) {
                        // 对每个部分进行分词
                        List<String> partTokens = nlpEnrichmentService.tokenizeFine(part.trim());
                        List<String> filteredPartTokens = HanlpUtils.filterValidTokens(partTokens);
                        typeTokens.addAll(filteredPartTokens);
                    }
                }
                // 去重
                List<String> uniqueTypeTokens = typeTokens.stream().distinct().toList();
                doc.setTypeTokens(uniqueTypeTokens);
                //log.debug("[POI分词] type='{}' -> tokens={}", poi.getType(), uniqueTypeTokens);
            }
        } catch (Exception e) {
            //log.warn("[POI分词] 分词失败，使用原始数据 poiId={}, error={}", poi.getId(), e.getMessage());
            // 分词失败不影响索引，继续使用原始数据
        }
        
        return doc;
    }
    
    /**
     * 验证坐标有效性
     */
    private boolean isValidCoordinate(double lat, double lon) {
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }
}
