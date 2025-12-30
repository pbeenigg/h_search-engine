package com.heytrip.hotel.search.ingest.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 消费服务
 * 职责：封装 Redis Stream 的消费逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamConsumerService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STREAM_KEY = "search:logs:stream";
    private static final String CONSUMER_GROUP = "search-log-consumer-group";
    private static final String CONSUMER_NAME = "search-log-consumer-1";

    /**
     * 初始化消费者组
     */
    @PostConstruct
    public void initConsumerGroup() {
        try {
            // 尝试创建消费者组
            redisTemplate.opsForStream().createGroup(STREAM_KEY, CONSUMER_GROUP);
            log.info("[REDIS_STREAM] 消费者组创建成功: {}", CONSUMER_GROUP);
        } catch (Exception e) {
            // 消费者组已存在（BUSYGROUP）是正常情况
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("BUSYGROUP")) {
                log.info("[REDIS_STREAM] 消费者组已存在，跳过创建: {}", CONSUMER_GROUP);
            } else {
                log.warn("[REDIS_STREAM] 消费者组创建失败: {}", errorMsg, e);
            }
        }
    }

    /**
     * 从 Stream 批量读取消息
     * 
     * @param batchSize 批量大小
     * @param blockMs 阻塞时间（毫秒）
     * @return 消息列表
     */
    public List<Map<String, Object>> consumeBatch(int batchSize, long blockMs) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try {
            // 读取消息
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(
                        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty()
                                .count(batchSize)
                                .block(Duration.ofMillis(blockMs)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                    );

            if (records != null && !records.isEmpty()) {
                for (MapRecord<String, Object, Object> record : records) {
                    // 转换 Map<Object, Object> 为 Map<String, Object>
                    Map<String, Object> message = new java.util.HashMap<>();
                    record.getValue().forEach((key, value) -> {
                        if (key != null) {
                            message.put(key.toString(), value);
                        }
                    });
                    messages.add(message);
                    
                    // ACK 消息
                    try {
                        redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());
                    } catch (Exception e) {
                        log.error("[REDIS_STREAM] ACK 失败 recordId={}", record.getId(), e);
                    }
                }
                
                log.debug("[REDIS_STREAM] 消费 {} 条消息", messages.size());
            }
            
        } catch (RedisConnectionFailureException e) {
            // Redis连接失败，降低日志级别避免刷屏
            log.warn("[REDIS_STREAM] Redis连接失败，跳过本次消费: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[REDIS_STREAM] 消费失败", e);
        }
        
        return messages;
    }

    /**
     * 获取待处理消息数量
     */
    public long getPendingCount() {
        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                    .pending(STREAM_KEY, CONSUMER_GROUP);
            return summary != null ? summary.getTotalPendingMessages() : 0;
        } catch (Exception e) {
            log.error("[REDIS_STREAM] 获取待处理消息数量失败", e);
            return 0;
        }
    }

    /**
     * 获取 Stream 长度
     */
    public long getStreamLength() {
        try {
            Long size = redisTemplate.opsForStream().size(STREAM_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("[REDIS_STREAM] 获取 Stream 长度失败", e);
            return 0;
        }
    }
}
