package com.heytrip.hotel.search.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Redis Stream 消费工具
 * - 负责创建消费组、读取、ACK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamConsumer {

    private final RedissonClient redissonClient;

    public void ensureGroup(String streamName, String groupName) {
        RStream<String, String> stream = redissonClient.getStream(streamName, StringCodec.INSTANCE);
        try {
            // 创建消费组，若 Stream 不存在则 makeStream()
            stream.createGroup(StreamCreateGroupArgs.name(groupName)
                    .id(StreamMessageId.NEWEST)
                    .makeStream());
            log.info("[REDIS-STREAM] create group ok stream={} group= {}", streamName, groupName);
        } catch (Exception e) {
            // 已存在时会报错，忽略
        }
    }

    /**
     * 读取一批消息（非阻塞），仅拉取未投递消息（相当于 XREADGROUP >）
     */
    public Map<StreamMessageId,Map<String, String>> readBatch(String streamName, String groupName, String consumerName, int count) {
        try {
            RStream<String, String> stream = redissonClient.getStream(streamName,StringCodec.INSTANCE);
            return stream.readGroup(groupName, consumerName,
                    StreamReadGroupArgs.neverDelivered().count(count));
        } catch (Exception e) {
            log.error("[REDIS-STREAM] readGroup failed stream={} group={} consumer={} err={}", streamName, groupName, consumerName, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public void ack(String streamName, String groupName, StreamMessageId id) {
        try {
            RStream<String, String> stream = redissonClient.getStream(streamName,StringCodec.INSTANCE);
            stream.ack(groupName, id);
        } catch (Exception e) {
            log.warn("[REDIS-STREAM] ack failed stream={} group={} id={} err={}", streamName, groupName, id, e.getMessage());
        }
    }
}
