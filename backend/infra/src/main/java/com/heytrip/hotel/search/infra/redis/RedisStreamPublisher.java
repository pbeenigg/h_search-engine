package com.heytrip.hotel.search.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Stream 发布工具（基于 Redisson）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamPublisher {
    private final RedissonClient redissonClient;

    // 裁剪阈值与触发频率
    private static final long DEFAULT_MAXLEN = 100_000L; // 最近保留10万条
    private static final long TRIM_EVERY = 1_000L;       // 每发布1000条触发一次裁剪
    private final AtomicLong publishCounter = new AtomicLong(0);

    /**
     * 向指定 Stream 追加一条消息（字符串 KV）
     * @param streamName Stream 名称
     * @param fields     字段键值（不得为 null）
     * @return 条目 ID
     */
    public String xadd(String streamName, Map<String, String> fields) {
        try {
            RStream<String, String> stream = redissonClient.getStream(streamName, StringCodec.INSTANCE);
            StreamMessageId id = stream.add(StreamAddArgs.entries(fields));
            if (log.isDebugEnabled()) {
                log.debug("[REDIS-STREAM] XADD stream={} id={} fieldsKeys={}", streamName, id, fields.keySet());
            }
            // 发布计数，按频率触发近似裁剪（XTRIM MAXLEN ~ N）
            long cnt = publishCounter.incrementAndGet();
            if (cnt % TRIM_EVERY == 0) {
                try {
                    long trimmed = xtrimMaxLenApprox(streamName, DEFAULT_MAXLEN);
                    if (log.isDebugEnabled()) {
                        log.debug("[REDIS-STREAM] XTRIM (approx) stream={} maxLen={} trimmed={} afterCount={}",
                                streamName, DEFAULT_MAXLEN, trimmed, cnt);
                    }
                } catch (Exception te) {
                    log.warn("[REDIS-STREAM] XTRIM failed stream={} err={}", streamName, te.getMessage());
                }
            }
            return id == null ? null : id.toString();
        } catch (Exception e) {
            log.error("[REDIS-STREAM] XADD 失败 stream={} err={}", streamName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 裁剪 Stream 长度（XTRIM MAXLEN ~ N）
     * @param streamName Stream 名称
     * @param maxLen     最大长度
     * @return 被裁剪的条目数
     */
    private long xtrimMaxLenApprox(String streamName, long maxLen) {
        String script = "return redis.call('XTRIM', KEYS[1], 'MAXLEN', '~', ARGV[1])";
        Object ret = redissonClient.getScript(StringCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                        Collections.singletonList(streamName), String.valueOf(maxLen));
        if (ret instanceof Number) {
            return ((Number) ret).longValue();
        }
        return 0L;
    }
}
