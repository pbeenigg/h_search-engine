package com.heytrip.hotel.search.ingest.route;

import com.heytrip.hotel.search.domain.entity.SearchLog;
import com.heytrip.hotel.search.domain.repository.SearchLogRepository;
import com.heytrip.hotel.search.ingest.service.RedisStreamConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索日志消费路由
 * 职责：从 Redis Stream 批量消费搜索日志并入库
 * 
 * 配置：
 * - 每2秒轮询一次
 * - 每次最多读取500条
 * - 批量插入数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchLogConsumerRoute extends RouteBuilder {

    private final SearchLogRepository searchLogRepository;
    private final RedisStreamConsumerService redisStreamConsumerService;

    private static final String STREAM_KEY = "search:logs:stream";
    private static final String CONSUMER_GROUP = "search-log-consumer-group";
    private static final String CONSUMER_NAME = "search-log-consumer-1";
    private static final int BATCH_SIZE = 500;
    private static final int POLL_INTERVAL_MS = 5000;

    @Override
    public void configure() throws Exception {
        
        // 定时消费 Redis Stream（每2秒一次）
        from("timer:search-log-consumer?period=" + POLL_INTERVAL_MS)
                .routeId("search-log-consumer")
                .bean(redisStreamConsumerService, "consumeBatch(" + BATCH_SIZE + ", 100)")
                .choice()
                    .when(simple("${body.size} > 0"))
                        .log(LoggingLevel.DEBUG,"[SEARCH_LOG_ROUTE] 消费到 ${body.size} 条搜索日志")
                        .bean(this, "batchInsert")
                        .log(LoggingLevel.DEBUG,"[SEARCH_LOG_ROUTE] 批量插入完成: ${body} 条")
                    .otherwise()
                        .log(LoggingLevel.DEBUG,"[SEARCH_LOG_ROUTE] 无新日志")
                .end();
    }

    /**
     * 批量插入搜索日志
     */
    public int batchInsert(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        try {
            List<SearchLog> logs = new ArrayList<>();
            
            for (Map<String, Object> msg : messages) {
                String type = (String) msg.get("type");
                
                if ("search".equals(type)) {
                    // 搜索日志
                    SearchLog log = SearchLog.builder()
                            .query((String) msg.get("query"))
                            .tagSource((String) msg.get("tagSource"))
                            .resultCount(getIntValue(msg, "resultCount"))
                            .durationMs(getLongValue(msg, "durationMs"))
                            .userId(getLongValue(msg, "userId"))
                            .userIp((String) msg.get("userIp"))
                            .createdAt(parseDateTime((String) msg.get("createdAt")))
                            .build();
                    logs.add(log);
                    
                } else if ("click".equals(type)) {
                    // 点击日志 - 更新已有记录或创建新记录
                    String query = (String) msg.get("query");
                    Long hotelId = getLongValue(msg, "clickedHotelId");
                    Integer position = getIntValue(msg, "clickPosition");
                    
                    // 简化处理：创建新的点击记录
                    SearchLog log = SearchLog.builder()
                            .query(query)
                            .clickedHotelId(hotelId)
                            .clickPosition(position)
                            .createdAt(parseDateTime((String) msg.get("createdAt")))
                            .build();
                    logs.add(log);
                }
            }
            
            if (!logs.isEmpty()) {
                searchLogRepository.saveAll(logs);
                log.info("[SEARCH_LOG_ROUTE] 批量插入 {} 条搜索日志", logs.size());
                return logs.size();
            }
            
        } catch (Exception e) {
            log.error("[SEARCH_LOG_ROUTE] 批量插入失败", e);
        }
        
        return 0;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return OffsetDateTime.now();
        try {
            return OffsetDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }
}
