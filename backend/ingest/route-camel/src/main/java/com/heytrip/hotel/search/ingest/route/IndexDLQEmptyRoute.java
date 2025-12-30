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

import java.util.*;

/**
 * DLQ Empty 消费与告警路由
 * - 读取 hotel:events:dlq:empty
 * - 聚合错误并发送邮件通知（Hutool Mail）
 * - 处理成功后 ACK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexDLQEmptyRoute extends RouteBuilder {

    private static final String STREAM = "hotel:events:dlq:empty";
    private static final String GROUP = "hotel-indexer-dlq-empty";
    private static final String CONSUMER = "dlq-empty-worker-1";
    private static final int BATCH = 1000; // 每批最多读取条数

    private final RedisStreamConsumer redisStreamConsumer;
    private final MailNotifier mailNotifier;

    @Override
    public void configure() {
        // 初始化消费组
        from("timer:dlq-empty-init?repeatCount=1")
                .routeId("route-dlq-empty-init")
                .process(e -> redisStreamConsumer.ensureGroup(STREAM, GROUP))
                .log(LoggingLevel.INFO, "[DLQ Empty] group ensured: stream=" + STREAM + ", group=" + GROUP);

        // 定时轮询 DLQ Empty（每60秒一次），批量读取、聚合并邮件告警
        from("timer:dlq-empty-poll?period=60000")
                .routeId("route-dlq-empty-poll")
                .process(this::processBatch)
                .log(LoggingLevel.DEBUG, "[DLQ Empty] poll done");
    }

    /**
     * 处理一批 DLQ Empty 消息
     */
    private void processBatch(Exchange exchange) {
        Map<StreamMessageId, Map<String, String>> batch = redisStreamConsumer.readBatch(STREAM, GROUP, CONSUMER, BATCH);
        if (batch == null || batch.isEmpty()) return;
        
        List<StreamMessageId> ids = new ArrayList<>(batch.size());
        
        // 按 providerSource 分组
        Map<String, List<String>> providerErrors = new HashMap<>();
        
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
            
            String errorLine = String.format("hotelId=%s provider=%s tag=%s errorCode=%s errorMsg=%s traceId=%s id=%s",
                    hotelId, provider, tag, errCode, errMsg, traceId, msgId);
            
            providerErrors.computeIfAbsent(provider, k -> new ArrayList<>()).add(errorLine);
        }
        
        // 按 provider 分别发送邮件
        for (Map.Entry<String, List<String>> entry : providerErrors.entrySet()) {
            String provider = entry.getKey();
            List<String> errors = entry.getValue();
            
            // 根据 provider 确定收件人
            String recipient = getRecipientByProvider(provider);
            
            // 构建邮件内容
            StringJoiner sj = new StringJoiner("\n");
            sj.add("[DLQ Empty] 批量错误摘要 [" + provider + "]，共 " + errors.size() + " 条：");
            errors.forEach(sj::add);
            
            try {
                mailNotifier.sendText(
                        "[DLQ Empty告警] 酒店索引处理失败 [" + provider + "] (" + errors.size() + "条)  |  未解析到酒店名称",
                        sj.toString(),
                        recipient
                );
                log.info("[DLQ Empty] 已发送邮件通知 provider={} count={} recipient={}", provider, errors.size(), recipient);
            } catch (Exception ex) {
                log.error("[DLQ Empty] 邮件发送失败 provider={} err={}", provider, ex.getMessage(), ex);
            }
        }
        
        // 最终统一 ACK，避免重复告警
        ids.forEach(id -> redisStreamConsumer.ack(STREAM, GROUP, id));
    }
    
    /**
     * 根据 provider 获取对应的收件人
     * @param provider 数据源（Elong | Agoda）
     * @return 收件人邮箱地址
     */
    private String getRecipientByProvider(String provider) {
        if (provider == null || provider.isEmpty()) {
            return "pax@heytripgo.com"; // 默认收件人
        }
        
        return switch (provider) {
            case "Elong" -> "seeu@heytripgo.com,pax@heytripgo.com";
            case "Agoda" -> "stone@heytripgo.com,pax@heytripgo.com";
            default -> "pax@heytripgo.com"; // 未知 provider 发送给默认收件人
        };
    }
}
