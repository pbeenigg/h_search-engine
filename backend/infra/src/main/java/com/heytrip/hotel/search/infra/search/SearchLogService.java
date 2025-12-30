package com.heytrip.hotel.search.infra.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.domain.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索日志服务
 * 职责：通过 Redis Stream 异步收集搜索日志，不影响主搜索流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchLogRepository searchLogRepository;
    private final ObjectMapper objectMapper;
    
    private static final String SEARCH_LOG_STREAM = "search:logs:stream";

    /**
     * 通过 Redis Stream 记录搜索日志（非阻塞）
     * 
     * @param query 查询词
     * @param tagSource 业务域
     * @param resultCount 结果数量
     * @param durationMs 查询耗时
     * @param userId 用户ID（可选）
     * @param userIp 用户IP（可选）
     * @param country 国家（可选）
     * @param city 城市（可选）
     * @param latitude 纬度（可选）
     * @param longitude 经度（可选）
     */
    public void logSearch(String query, String tagSource, int resultCount, long durationMs, 
                         Long userId, String userIp, String country, String city, 
                         Double latitude, Double longitude) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("type", "search");
            logData.put("query", query);
            logData.put("tagSource", tagSource);
            logData.put("resultCount", resultCount);
            logData.put("durationMs", durationMs);
            logData.put("userId", userId);
            logData.put("userIp", userIp);
            logData.put("country", country);
            logData.put("city", city);
            logData.put("latitude", latitude);
            logData.put("longitude", longitude);
            logData.put("createdAt", OffsetDateTime.now().toString());
            
            // 发送到 Redis Stream（非阻塞）
            ObjectRecord<String, Map<String, Object>> record = StreamRecords
                    .newRecord()
                    .ofObject(logData)
                    .withStreamKey(SEARCH_LOG_STREAM);
            
            redisTemplate.opsForStream().add(record);
            
            log.debug("[SEARCH_LOG] 发送搜索日志到 Stream query='{}' ip='{}' country='{}' city='{}' resultCount={} durationMs={}", 
                    query, userIp, country, city, resultCount, durationMs);
        } catch (Exception e) {
            log.error("[SEARCH_LOG] 发送搜索日志失败 query='{}' err={}", query, e.getMessage(), e);
        }
    }
    
    /**
     * 通过 Redis Stream 记录搜索日志（非阻塞）- 简化版本（向后兼容）
     * 
     * @param query 查询词
     * @param tagSource 业务域
     * @param resultCount 结果数量
     * @param durationMs 查询耗时
     * @param userId 用户ID（可选）
     * @param userIp 用户IP（可选）
     */
    public void logSearch(String query, String tagSource, int resultCount, long durationMs, Long userId, String userIp) {
        logSearch(query, tagSource, resultCount, durationMs, userId, userIp, null, null, null, null);
    }

    /**
     * 通过 Redis Stream 记录用户点击（非阻塞）
     * 
     * @param query 原始查询词
     * @param hotelId 点击的酒店ID
     * @param position 点击位置
     */
    public void logClick(String query, Long hotelId, int position) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("type", "click");
            logData.put("query", query);
            logData.put("clickedHotelId", hotelId);
            logData.put("clickPosition", position);
            logData.put("createdAt", OffsetDateTime.now().toString());
            
            // 发送到 Redis Stream（非阻塞）
            ObjectRecord<String, Map<String, Object>> record = StreamRecords
                    .newRecord()
                    .ofObject(logData)
                    .withStreamKey(SEARCH_LOG_STREAM);
            
            redisTemplate.opsForStream().add(record);
            
            log.debug("[SEARCH_LOG] 发送点击日志到 Stream query='{}' hotelId={} position={}", 
                    query, hotelId, position);
        } catch (Exception e) {
            log.error("[SEARCH_LOG] 发送点击日志失败 query='{}' hotelId={} err={}", 
                    query, hotelId, e.getMessage(), e);
        }
    }

    /**
     * 获取搜索质量指标
     */
    public SearchMetrics getMetrics(OffsetDateTime start, OffsetDateTime end) {
        try {
            long totalQueries = searchLogRepository.countByCreatedAtBetween(start, end);
            long zeroResults = searchLogRepository.countZeroResultsByCreatedAtBetween(start, end);
            long withClicks = searchLogRepository.countWithClicksByCreatedAtBetween(start, end);
            
            double zeroResultRate = totalQueries > 0 ? (double) zeroResults / totalQueries : 0;
            double clickThroughRate = totalQueries > 0 ? (double) withClicks / totalQueries : 0;
            
            return SearchMetrics.builder()
                    .totalQueries(totalQueries)
                    .zeroResultCount(zeroResults)
                    .zeroResultRate(zeroResultRate)
                    .clickThroughRate(clickThroughRate)
                    .build();
        } catch (Exception e) {
            log.error("[SEARCH_LOG] 获取指标失败 err={}", e.getMessage(), e);
            return SearchMetrics.builder().build();
        }
    }

    /**
     * 搜索质量指标
     */
    @lombok.Data
    @lombok.Builder
    public static class SearchMetrics {
        private Long totalQueries;
        private Long zeroResultCount;
        private Double zeroResultRate;
        private Double clickThroughRate;
    }
}
