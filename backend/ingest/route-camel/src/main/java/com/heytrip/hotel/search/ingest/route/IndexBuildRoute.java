package com.heytrip.hotel.search.ingest.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.common.event.HotelEvent;
import com.heytrip.hotel.search.common.parser.HotelParser;
import com.heytrip.hotel.search.common.parser.HotelParserSelector;
import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import com.heytrip.hotel.search.infra.redis.RedisStreamConsumer;
import com.heytrip.hotel.search.ingest.service.IndexBackfillService;
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

/**
 * 索引构建消费者路由
 * 阶段目标：
 * - 先使用 SEDA 端点模拟事件消费，验证解析器链路
 * - 后续接入 Redisson XREADGROUP 读取 Redis Stream
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexBuildRoute extends RouteBuilder {

    private final ObjectMapper objectMapper;
    private final HotelParserSelector parserSelector;
    private final RedisStreamConsumer redisStreamConsumer;
    private final IndexBackfillService indexBackfillService;

    private static final String STREAM = "hotel:events";
    private static final String GROUP  = "hotel-indexer";
    private static final String CONSUMER = "indexer-1";
    private static final int BATCH = 500;

    @Override
    public void configure() {
        // 启动时确保消费组存在（启动一次）
        from("timer:index-init?repeatCount=1")
                .routeId("route-index-init")
                .process(e -> redisStreamConsumer.ensureGroup(STREAM, GROUP))
                .log(LoggingLevel.INFO,"[INDEX-BUILD] init group ensured: stream=" + STREAM + ", group=" + GROUP);

        // 定时轮询 Redis Stream（每秒一次），批量读取并处理
        from("timer:index-poll?period=1000")
                .routeId("route-index-poll")
                .process(e -> processBatch())
                .log(LoggingLevel.DEBUG,"[INDEX-BUILD] poll done");

        // 本地测试入口：保持 SEDA 路径
        from("seda:index:consume")
                .routeId("route-index-build-skeleton")
                .process(this::handleEvent)
                .log(LoggingLevel.INFO,"[INDEX-BUILD] skeleton handled event for hotelId=${exchangeProperty.hotelId}");
    }

    /**
     * 处理单个事件
     */
    private void handleEvent(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        HotelEvent evt = objectMapper.readValue(body, HotelEvent.class);
        exchange.setProperty("hotelId", evt.getHotelId());
        // 简化：根据 providerSource 选择解析器
        HotelParser parser = parserSelector.select(evt.getProviderSource(), evt.getTagSource());
        if (parser == null) {
            log.warn("[INDEX-BUILD] no parser for providerSource={} tagSource={} hotelId={}", evt.getProviderSource(), evt.getTagSource(), evt.getHotelId());
            return;
        }
        // 目前 skeleton 未拉取 raw，这里只演示解析器选择；后续接入 DB 查 raw
        HotelStructuredExtractor.Result parsed = parser.parse("{}");
        log.info("[INDEX-BUILD] skeleton parsed for hotelId={} provider={} tag={} result(nameCn={}, nameEn={}, country={}, city={}, lat={}, lon={})",
                evt.getHotelId(), evt.getProviderSource(), evt.getTagSource(),
                parsed.getNameCn(), parsed.getNameEn(), parsed.getCountryEn(), parsed.getCityEn(), parsed.getLatitude(), parsed.getLongitude());
    }

    /**
     * 批量处理 Redis Stream 消息
     */
    private void processBatch() {
        Map<StreamMessageId, Map<String, String>> batch = redisStreamConsumer.readBatch(STREAM, GROUP, CONSUMER, BATCH);
        if (batch == null || batch.isEmpty()) return;
        List<HotelEvent> events = new ArrayList<>(batch.size());
        List<StreamMessageId> ids = new ArrayList<>(batch.size());
        for (Map.Entry<StreamMessageId, Map<String, String>> e : batch.entrySet()) {
            StreamMessageId msgId = e.getKey();
            try {
                Map<String, String> fields = e.getValue();
                HotelEvent evt = mapToEvent(fields);
                events.add(evt);
                ids.add(msgId);
            } catch (Exception ex) {
                log.error("[INDEX-BUILD] parse message failed id={} err={}", msgId, ex.getMessage(), ex);
                redisStreamConsumer.ack(STREAM, GROUP, msgId);
            }
        }
        if (events.isEmpty()) return;
        // 批处理：内部包含回填与 ES bulk 写入与错误处理
        indexBackfillService.processEventsBatch(events);
        // 全部尝试处理后统一 ACK（即便失败会写入 DLQ）
        ids.forEach(id -> redisStreamConsumer.ack(STREAM, GROUP, id));
    }

    private HotelEvent mapToEvent(Map<String, String> m) {
        HotelEvent evt = new HotelEvent();
        evt.setEventType(m.getOrDefault("eventType", "UPSERT"));
        evt.setRowId(parseLong(m.get("rowId")));
        evt.setHotelId(parseLong(m.get("hotelId")));
        evt.setProviderSource(m.get("providerSource"));
        evt.setTagSource(m.get("tagSource"));
        evt.setTraceId(m.get("traceId"));
        evt.setSyncLogId(parseLong(m.get("syncLogId")));
        evt.setFetchedAt(m.get("fetchedAt"));
        return evt;
    }

    private Long parseLong(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.parseLong(s); } catch (Exception e) { return null; }
    }
}
