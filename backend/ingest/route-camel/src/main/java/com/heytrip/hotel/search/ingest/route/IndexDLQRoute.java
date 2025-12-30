package com.heytrip.hotel.search.ingest.route;

import com.heytrip.hotel.search.infra.notify.MailNotifier;
import com.heytrip.hotel.search.infra.redis.RedisStreamConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.redisson.api.StreamMessageId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * DLQ 消费与告警路由
 * - 读取 hotel:events:dlq
 * - 聚合错误并发送邮件通知（Hutool Mail）
 * - 处理成功后 ACK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexDLQRoute extends RouteBuilder {

    private static final String STREAM = "hotel:events:dlq";
    private static final String GROUP = "hotel-indexer-dlq";
    private static final String CONSUMER = "dlq-worker-1";
    private static final int BATCH = 1000; // 每批最多读取条数

    private final RedisStreamConsumer redisStreamConsumer;
    private final MailNotifier mailNotifier;

    @Override
    public void configure() {
        // 初始化消费组
        from("timer:dlq-init?repeatCount=1")
                .routeId("route-dlq-init")
                .process(e -> redisStreamConsumer.ensureGroup(STREAM, GROUP))
                .log(LoggingLevel.INFO, "[DLQ] group ensured: stream=" + STREAM + ", group=" + GROUP);

        // 定时轮询 DLQ（每60秒一次），批量读取、聚合并邮件告警
        from("timer:dlq-poll?period=60000")
                .routeId("route-dlq-poll")
                .process(this::processBatch)
                .log(LoggingLevel.DEBUG, "[DLQ] poll done");
    }

    /**
     * 处理一批 DLQ 消息
     */
    private void processBatch(Exchange exchange) {
        Map<StreamMessageId, Map<String, String>> batch = redisStreamConsumer.readBatch(STREAM, GROUP, CONSUMER, BATCH);
        if (batch == null || batch.isEmpty()) return;
        List<StreamMessageId> ids = new ArrayList<>(batch.size());
        StringJoiner sj = new StringJoiner("\n");
        sj.add("[DLQ] 批量错误摘要，共 " + batch.size() + " 条：");
        for (Map.Entry<StreamMessageId, Map<String, String>> e : batch.entrySet()) {
            StreamMessageId msgId = e.getKey();
            Map<String, String> f = e.getValue();
            ids.add(msgId);
            String hotelId = f.getOrDefault("hotelId", "");
            String provider = f.getOrDefault("providerSource", "");
            String tag = f.getOrDefault("tagSource", "");
            String errCode = f.getOrDefault("errorCode", "");
            String errMsg = f.getOrDefault("errorMsg", "");
            String traceId = f.getOrDefault("traceId", "");
            sj.add(String.format("hotelId=%s provider=%s tag=%s errorCode=%s errorMsg=%s traceId=%s id=%s",
                    hotelId, provider, tag, errCode, errMsg, traceId, msgId));
        }
        try {
            mailNotifier.sendText("[DLQ告警] 酒店索引处理失败批次(" + batch.size() + ")", sj.toString());
        } catch (Exception ex) {
            log.error("[DLQ] 邮件发送失败 err={}", ex.getMessage(), ex);
        } finally {
            // 最终统一 ACK，避免重复告警
            ids.forEach(id -> redisStreamConsumer.ack(STREAM, GROUP, id));
        }
    }
}
