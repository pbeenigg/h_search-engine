package com.heytrip.hotel.search.ingest.service;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.search.common.event.HotelEvent;
import com.heytrip.hotel.search.common.parser.HotelParser;
import com.heytrip.hotel.search.common.parser.HotelParserSelector;
import com.heytrip.hotel.search.common.util.GzipCompressor;
import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import com.heytrip.hotel.search.domain.entity.Hotels;
import com.heytrip.hotel.search.domain.entity.SyncLogDetail;
import com.heytrip.hotel.search.domain.repository.HotelsRepository;
import com.heytrip.hotel.search.domain.repository.SyncLogDetailRepository;
import com.heytrip.hotel.search.infra.redis.RedisStreamPublisher;
import com.heytrip.hotel.search.infra.search.EsHotelIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * 索引前置：基于消费者解析结果幂等回写 Hotels
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexBackfillService {

    private final HotelsRepository hotelsRepository;
    private final SyncLogDetailRepository syncLogDetailRepository;
    private final HotelParserSelector parserSelector;
    private final RedisStreamPublisher redisStreamPublisher;
    private final EsHotelIndexService esHotelIndexService;

    // Redis Stream 死信队列
    private static final String DLQ = "hotel:events:dlq";
    // Redis Stream 空解析结果队列
    private static final String DLQ_EMPTY = "hotel:events:dlq:empty";

    @Transactional
    public void processEvent(HotelEvent evt) {
        try {
            if (evt.getHotelId() == null) return;
            Hotels hotels = hotelsRepository.findByHotelId(evt.getHotelId()).orElse(null);
            if (hotels == null) {
                emitDlq(evt, "NOT_FOUND", "Hotel row not found");
                return;
            }
            
            // 解压 GZIP 压缩的原始数据（Base64 -> GZIP -> 原文）
            String rawCompressed = hotels.getRawCompressed();
            String raw;
            try {
                byte[] compressedBytes = Base64.getDecoder().decode(rawCompressed);
                raw = GzipCompressor.decompressToString(compressedBytes);
            } catch (Exception e) {
                log.warn("[IndexBackfill] 解压失败，尝试直接使用原文 hotelId={} err={}", evt.getHotelId(), e.getMessage());
                raw = rawCompressed; // 降级：如果解压失败，尝试直接使用（兼容旧数据）
            }
            
            HotelParser parser = parserSelector.select(evt.getProviderSource(), evt.getTagSource());
            if (parser == null) {
                emitDlq(evt, "NO_PARSER", "no parser for provider=" + evt.getProviderSource());
                return;
            }
            HotelStructuredExtractor.Result parsed = parser.parse(raw);
            if((evt.getProviderSource().equalsIgnoreCase("Agoda") && StrUtil.isBlank(parsed.getNameEn())) ||
              (evt.getProviderSource().equalsIgnoreCase("Elong") && StrUtil.isBlank(parsed.getNameCn()))){
                emitDlqEmpty(evt, "EMPTY_PARSER", "Hotel basic information is empty=" + evt.getProviderSource());
                return;
            }
            // 幂等更新：仅在值变化或为空时更新
            boolean changed = false;
            
            // 人工修正字段优先：如果存在人工修正字段，则跳过对应业务字段的回填更新
            boolean hasManualNameCn = hotels.getNewHotelNameCn() != null && !hotels.getNewHotelNameCn().trim().isEmpty();
            boolean hasManualNameEn = hotels.getNewHotelNameEn() != null && !hotels.getNewHotelNameEn().trim().isEmpty();
            boolean hasManualCountryCode = hotels.getCountryCode() != null && hotels.getNewHotelNameCn() != null;
            
            if (!hasManualNameCn) {
                changed |= setIfDiff(() -> hotels.getHotelNameCn(), hotels::setHotelNameCn, parsed.getNameCn());
            }
            if (!hasManualNameEn) {
                changed |= setIfDiff(() -> hotels.getHotelNameEn(), hotels::setHotelNameEn, parsed.getNameEn());
            }

            changed |= setIfDiff(() -> hotels.getCountryCn(), hotels::setCountryCn, parsed.getCountryCn());
            changed |= setIfDiff(() -> hotels.getCountryEn(), hotels::setCountryEn, parsed.getCountryEn());
            if (!hasManualCountryCode) {
                changed |= setIfDiff(() -> hotels.getCountryCode(), hotels::setCountryCode, parsed.getCountryCode());
            }

            changed |= setIfDiff(() -> hotels.getCityCn(), hotels::setCityCn, parsed.getCityCn());
            changed |= setIfDiff(() -> hotels.getCityEn(), hotels::setCityEn, parsed.getCityEn());

            changed |= setIfDiff(() -> hotels.getRegionCn(), hotels::setRegionCn, parsed.getRegionCn());
            changed |= setIfDiff(() -> hotels.getRegionEn(), hotels::setRegionEn, parsed.getRegionEn());

            changed |= setIfDiff(() -> hotels.getContinentCn(), hotels::setContinentCn, parsed.getContinentCn());
            changed |= setIfDiff(() -> hotels.getContinentEn(), hotels::setContinentEn, parsed.getContinentEn());

            changed |= setIfDiff(() -> hotels.getAddressCn(), hotels::setAddressCn, parsed.getAddressCn());
            changed |= setIfDiff(() -> hotels.getAddressEn(), hotels::setAddressEn, parsed.getAddressEn());

            changed |= setIfDiff(() -> hotels.getLongitude(), hotels::setLongitude, parsed.getLongitude());
            changed |= setIfDiff(() -> hotels.getLatitude(), hotels::setLatitude, parsed.getLatitude());

            changed |= setIfDiff(() -> hotels.getHotelGroupCn(), hotels::setHotelGroupCn, parsed.getHotelGroupCn());
            changed |= setIfDiff(() -> hotels.getHotelGroupEn(), hotels::setHotelGroupEn, parsed.getHotelGroupEn());

            changed |= setIfDiff(() -> hotels.getHotelBrandCn(), hotels::setHotelBrandCn, parsed.getHotelBrandCn());
            changed |= setIfDiff(() -> hotels.getHotelBrandEn(), hotels::setHotelBrandEn, parsed.getHotelBrandEn());

            changed |= setIfDiff(() -> hotels.getDescriptionCn(), hotels::setDescriptionCn, parsed.getDescriptionCn());
            changed |= setIfDiff(() -> hotels.getDescriptionEn(), hotels::setDescriptionEn, parsed.getDescriptionEn());
            if (changed) {
                hotels.setUpdatedAt(OffsetDateTime.now());
                hotelsRepository.save(hotels);
            }

        } catch (Exception e) {
            log.error("[IndexBackfill] backfill failed hotelId={} err= {}", evt.getHotelId(), e.getMessage(), e);
            emitDlq(evt, "BACKFILL_ERROR", e.getMessage());
            if (evt.getSyncLogId() != null) {
                SyncLogDetail d = SyncLogDetail.builder()
                        .syncLogId(evt.getSyncLogId())
                        .hotelId(evt.getHotelId())
                        .stage("INDEX_BACKFILL")
                        .errorCode("BACKFILL_ERROR")
                        .errorMessage(e.getMessage())
                        .build();
                syncLogDetailRepository.save(d);
            }
        }
    }

    /**
     * 批量处理：解析、幂等回填、ES 批量写入（refresh=false），失败重试并写 DLQ
     */
    @Transactional
    public void processEventsBatch(List<HotelEvent> events) {
        if (events == null || events.isEmpty()) return;
        List<Hotels> toIndex = new ArrayList<>(events.size());
        Map<Long, HotelStructuredExtractor.Result> parsedMap = new HashMap<>();

        // 统计指标
        int cntParsedOk = 0;
        int cntNotFound = 0;
        int cntNoParser = 0;
        int cntBackfillErr = 0;
        int cntChanged = 0;

        // 逐条解析与回填（以消费者为准）
        for (HotelEvent evt : events) {
            try {
                if (evt.getHotelId() == null) continue;
                Hotels hotels = hotelsRepository.findByHotelId(evt.getHotelId()).orElse(null);
                if (hotels == null) {
                    emitDlq(evt, "NOT_FOUND", "Hotel row not found");
                    cntNotFound++;
                    continue;
                }
                // 解压 GZIP 压缩的原始数据（Base64 -> GZIP -> 原文）
                String rawCompressed = hotels.getRawCompressed();
                String raw;
                try {
                    byte[] compressedBytes =Base64.getDecoder().decode(rawCompressed);
                    raw = GzipCompressor.decompressToString(compressedBytes);
                } catch (Exception e) {
                    log.warn("[IndexBackfill] 批量解压失败，尝试直接使用原文 hotelId={} err={}", evt.getHotelId(), e.getMessage());
                    raw = rawCompressed; // 降级：如果解压失败，尝试直接使用（兼容旧数据）
                }
                
                HotelParser parser = parserSelector.select(evt.getProviderSource(), evt.getTagSource());
                if (parser == null) {
                    emitDlq(evt, "NO_PARSER", "no parser for provider=" + evt.getProviderSource());
                    cntNoParser++;
                    continue;
                }
                // 解析结构化酒店信息（已解压）
                HotelStructuredExtractor.Result parsed = parser.parse(raw);

                if((evt.getProviderSource().equalsIgnoreCase("Agoda") && StrUtil.isBlank(parsed.getNameEn())) ||
                        (evt.getProviderSource().equalsIgnoreCase("Elong") && StrUtil.isBlank(parsed.getNameCn()))){
                    log.warn("[IndexBackfill] 结构化字段解析异常，缺失酒店名称 hotelId={} provider={}  跳过", evt.getHotelId(), evt.getProviderSource());
                    emitDlqEmpty(evt, "EMPTY_PARSER", "Hotel basic information is empty=" + evt.getProviderSource());
                    continue;
                }

                boolean changed = false;
                
                // 人工修正字段优先：如果存在人工修正字段，则跳过对应业务字段的回填更新
                boolean hasManualNameCn = hotels.getNewHotelNameCn() != null && !hotels.getNewHotelNameCn().trim().isEmpty();
                boolean hasManualNameEn = hotels.getNewHotelNameEn() != null && !hotels.getNewHotelNameEn().trim().isEmpty();
                boolean hasManualCountryCode = hotels.getCountryCode() != null && hotels.getNewHotelNameCn() != null; // 如果有人工修正名称，认为国家代码也是人工修正过的
                // 注意：address的人工修正字段在Hotels表中没有对应的new字段，所以不需要特殊处理
                
                if (!hasManualNameCn) {
                    changed |= setIfDiff(() -> hotels.getHotelNameCn(), hotels::setHotelNameCn, parsed.getNameCn());
                }
                if (!hasManualNameEn) {
                    changed |= setIfDiff(() -> hotels.getHotelNameEn(), hotels::setHotelNameEn, parsed.getNameEn());
                }

                changed |= setIfDiff(() -> hotels.getCountryCn(), hotels::setCountryCn, parsed.getCountryCn());
                changed |= setIfDiff(() -> hotels.getCountryEn(), hotels::setCountryEn, parsed.getCountryEn());
                if (!hasManualCountryCode) {
                    changed |= setIfDiff(() -> hotels.getCountryCode(), hotels::setCountryCode, parsed.getCountryCode());
                }

                changed |= setIfDiff(() -> hotels.getCityCn(), hotels::setCityCn, parsed.getCityCn());
                changed |= setIfDiff(() -> hotels.getCityEn(), hotels::setCityEn, parsed.getCityEn());

                changed |= setIfDiff(() -> hotels.getRegionCn(), hotels::setRegionCn, parsed.getRegionCn());
                changed |= setIfDiff(() -> hotels.getRegionEn(), hotels::setRegionEn, parsed.getRegionEn());

                changed |= setIfDiff(() -> hotels.getContinentCn(), hotels::setContinentCn, parsed.getContinentCn());
                changed |= setIfDiff(() -> hotels.getContinentEn(), hotels::setContinentEn, parsed.getContinentEn());

                changed |= setIfDiff(() -> hotels.getAddressCn(), hotels::setAddressCn, parsed.getAddressCn());
                changed |= setIfDiff(() -> hotels.getAddressEn(), hotels::setAddressEn, parsed.getAddressEn());

                changed |= setIfDiff(() -> hotels.getLongitude(), hotels::setLongitude, parsed.getLongitude());
                changed |= setIfDiff(() -> hotels.getLatitude(), hotels::setLatitude, parsed.getLatitude());

                changed |= setIfDiff(() -> hotels.getHotelGroupCn(), hotels::setHotelGroupCn, parsed.getHotelGroupCn());
                changed |= setIfDiff(() -> hotels.getHotelGroupEn(), hotels::setHotelGroupEn, parsed.getHotelGroupEn());

                changed |= setIfDiff(() -> hotels.getHotelBrandCn(), hotels::setHotelBrandCn, parsed.getHotelBrandCn());
                changed |= setIfDiff(() -> hotels.getHotelBrandEn(), hotels::setHotelBrandEn, parsed.getHotelBrandEn());

                changed |= setIfDiff(() -> hotels.getDescriptionCn(), hotels::setDescriptionCn, parsed.getDescriptionCn());
                changed |= setIfDiff(() -> hotels.getDescriptionEn(), hotels::setDescriptionEn, parsed.getDescriptionEn());
                if (changed) {
                    hotels.setUpdatedAt(OffsetDateTime.now());
                    hotelsRepository.save(hotels);
                    cntChanged++;
                }
                toIndex.add(hotels);
                parsedMap.put(hotels.getHotelId(), parsed);
                cntParsedOk++;
            } catch (Exception ex) {
                log.error("[IndexBackfill] batch backfill single failed hotelId={} err={}", evt.getHotelId(), ex.getMessage(), ex);
                emitDlq(evt, "BACKFILL_ERROR", ex.getMessage());
                cntBackfillErr++;
                if (evt.getSyncLogId() != null) {
                    SyncLogDetail d = SyncLogDetail.builder()
                            .syncLogId(evt.getSyncLogId())
                            .hotelId(evt.getHotelId())
                            .stage("INDEX_BACKFILL")
                            .errorCode("BACKFILL_ERROR")
                            .errorMessage(ex.getMessage())
                            .createdAt(OffsetDateTime.now())
                            .build();
                    syncLogDetailRepository.save(d);
                }
            }
        }

        // 解析与回填阶段指标
        log.info("[METRIC][IndexBackfill] batch_parse summary size={} parsedOk={} notFound={} noParser={} backfillErr={} changed={}",
                events.size(), cntParsedOk, cntNotFound, cntNoParser, cntBackfillErr, cntChanged);

        // 构建 hotelId -> event 映射，便于后续逐条回退
        Map<Long, HotelEvent> evtById = new HashMap<>();
        for (HotelEvent e : events) {
            if (e.getHotelId() != null) evtById.put(e.getHotelId(), e);
        }

        // 构建便捷映射：hotelId -> staging
        Map<Long, Hotels> stagingById = new HashMap<>();
        for (Hotels s : toIndex) {
            if (s.getHotelId() != null) stagingById.put(s.getHotelId(), s);
        }

        // 批量写 ES：1 次指数退避（200ms, 800ms, 1600ms）；按 per-item 失败仅重试失败项
        int attempts = 0;
        long esDurationMs = 0L;
        List<Hotels> pending = new ArrayList<>(toIndex);
        EsHotelIndexService.BulkResult lastResult = null;
        while (attempts < 3 && !pending.isEmpty()) {
            try {
                long esStart = System.nanoTime();
                log.info("[IndexBackfill] bulk upsert attempt={} size={}", attempts + 1, pending.size());
                lastResult = esHotelIndexService.bulkUpsert(
                        pending,
                        s -> parsedMap.getOrDefault(s.getHotelId(), new HotelStructuredExtractor.Result()),
                        Hotels::getTagSource,
                        Hotels::getSource
                );
                esDurationMs = (System.nanoTime() - esStart) / 1_000_000;
                int succ = lastResult == null ? pending.size() : lastResult.successCount();
                int fail = lastResult == null ? 0 : lastResult.failCount();
                log.info("[METRIC][IndexBackfill] es_bulk attempt={} durationMs={} success={} fail={}", attempts + 1, esDurationMs, succ, fail);
                if (fail <= 0) {
                    return; // 全部成功
                }
                // 仅对失败项重试
                Map<Long, String> failed = lastResult.getFailed();
                List<Hotels> next = new ArrayList<>(failed.size());
                for (Long fid : failed.keySet()) {
                    Hotels s = stagingById.get(fid);
                    if (s != null) next.add(s);
                }
                pending = next;
            } catch (Exception ex) {
                attempts++;
                log.warn("[IndexBackfill] bulk upsert failed attempt={} size={} err={}", attempts, pending.size(), ex.getMessage());
                try { Thread.sleep((long) (200 * Math.pow(4, attempts - 1))); } catch (InterruptedException ignored) {}
                // 发生异常由下一轮重试处理；到达上限再执行 DLQ
            }
            // 正常返回但有失败项，也需要递增 attempts 与退避
            if (lastResult != null && lastResult.failCount() > 0) {
                attempts++;
                try { Thread.sleep((long) (200 * Math.pow(4, attempts - 1))); } catch (InterruptedException ignored) {}
            }
        }
        // 到达重试上限或 pending 为空：将剩余失败项（pending）写入 DLQ
        int perFail = 0;
        int perOk = 0;
        for (Hotels s : pending) {
            HotelEvent origin = evtById.get(s.getHotelId());
            perFail++;
            if (origin != null) {
                emitDlq(origin, "ES_UPSERT_FAIL", "exhausted after retries");


                //错误的列表
                Map<Long, String> failedMsg =  lastResult.getFailed();

                if (origin.getSyncLogId() != null) {
                    SyncLogDetail d = SyncLogDetail.builder()
                            .syncLogId(origin.getSyncLogId())
                            .hotelId(origin.getHotelId())
                            .stage("INDEX_BULK")
                            .errorCode("ES_UPSERT_FAIL")
                            .errorMessage("重试后最终失败:"+ failedMsg.getOrDefault(origin.getHotelId(),"unknown error"))
                            .createdAt(OffsetDateTime.now())
                            .build();
                    syncLogDetailRepository.save(d);
                }
            }
        }
        log.warn("[METRIC][IndexBackfill] es_bulk final_failed size={} attemptsUsed={} perItemOk={} perItemFail={}",
                toIndex.size(), attempts, perOk, perFail);
    }

    /**
     * 写入死信队列
     * @param evt
     * @param code
     * @param msg
     */
    private void emitDlq(HotelEvent evt, String code, String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("eventType", evt.getEventType());
        m.put("rowId", evt.getRowId() == null ? "" : String.valueOf(evt.getRowId()));
        m.put("hotelId", evt.getHotelId() == null ? "" : String.valueOf(evt.getHotelId()));
        m.put("providerSource", evt.getProviderSource());
        m.put("tagSource", evt.getTagSource());
        m.put("traceId", evt.getTraceId());
        m.put("syncLogId", evt.getSyncLogId() == null ? "" : String.valueOf(evt.getSyncLogId()));
        m.put("fetchedAt", evt.getFetchedAt());
        m.put("errorCode", code);
        m.put("errorMsg", msg);
        redisStreamPublisher.xadd(DLQ, m);
    }

    /**
     * 写入空解析结果队列
     * @param evt
     * @param code
     * @param msg
     */
    private void emitDlqEmpty(HotelEvent evt, String code, String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("eventType", evt.getEventType());
        m.put("rowId", evt.getRowId() == null ? "" : String.valueOf(evt.getRowId()));
        m.put("hotelId", evt.getHotelId() == null ? "" : String.valueOf(evt.getHotelId()));
        m.put("providerSource", evt.getProviderSource());
        m.put("tagSource", evt.getTagSource());
        m.put("traceId", evt.getTraceId());
        m.put("syncLogId", evt.getSyncLogId() == null ? "" : String.valueOf(evt.getSyncLogId()));
        m.put("fetchedAt", evt.getFetchedAt());
        m.put("errorCode", code);
        m.put("errorMsg", msg);
        redisStreamPublisher.xadd(DLQ_EMPTY, m);
    }

    private <T> boolean setIfDiff(SupplierLike<T> getter, SetterLike<T> setter, T newVal) {
        T old = getter.get();
        if (!Objects.equals(old, newVal)) {
            setter.set(newVal);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    private interface SupplierLike<T> { T get(); }
    @FunctionalInterface
    private interface SetterLike<T> { void set(T v); }
}
