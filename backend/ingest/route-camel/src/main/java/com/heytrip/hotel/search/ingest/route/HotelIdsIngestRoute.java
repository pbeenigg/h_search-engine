package com.heytrip.hotel.search.ingest.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.common.util.GzipCompressor;
import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import com.heytrip.hotel.search.common.util.Md5Signer;
import com.heytrip.hotel.search.domain.entity.*;
import com.heytrip.hotel.search.domain.repository.*;
import com.heytrip.hotel.search.infra.config.SupplierConfig;
import com.heytrip.hotel.search.infra.redis.RedisStreamPublisher;
import com.heytrip.hotel.search.infra.redis.RedissonSemaphore;
import com.heytrip.hotel.search.ingest.dto.HotelDetailResponse;
import com.heytrip.hotel.search.ingest.dto.HotelIdsResponse;
import com.heytrip.hotel.search.ingest.model.HotelIdsIngestContext;
import com.heytrip.hotel.search.ingest.model.JobScheduleParams;
import com.heytrip.hotel.search.ingest.service.AsyncLogService;
import com.heytrip.hotel.search.ingest.service.HotelsSinkService;
import com.heytrip.hotel.search.ingest.service.JobScheduleCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 酒店ID分页采集路由
 * - DB驱动Cron：从 job_schedule 读取 cron_expr 与 enabled
 * - 签名： MD5("app"+app+"secret"+secret+"timestamp"+ts)
 * - 并发控制：Redisson 全局信号量，许可数来源 job_schedule.max_concurrency（≤10）
 * - 详情采集：按20/批调用详情接口，解析并批量入库 hotels（每1000条提交一次）
 * - API日志：请求/响应写入 api_request_log（正文GZIP压缩）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelIdsIngestRoute extends RouteBuilder {

    private static final String JOB_CODE = "HOTEL_FULL_SYNC_ALL";

    private static final String SEMAPHORE_KEY ="hotel:semaphore:ingest:";
    private static final String REDIS_STREAM = "hotel:events";

    private final SupplierConfig supplierConfig;
    private final JobScheduleRepository jobScheduleRepository;
    private final JobRuntimeStateRepository jobRuntimeStateRepository;
    private final ApiRequestLogRepository apiRequestLogRepository;
    private final ObjectMapper objectMapper;
    private final HotelsSinkService hotelsSinkService; // 带事务批量入库服务
    private final RedissonSemaphore semaphoreManager;
    private final SyncLogRepository syncLogRepository;
    private final SyncLogDetailRepository syncLogDetailRepository;
    private final RedisStreamPublisher redisStreamPublisher;
    private final AsyncLogService asyncLogService;
    private final JobScheduleCacheService jobScheduleCacheService;

    @Override
    public void configure() {
        // 基础异常处理：记录错误并继续下一次触发
        onException(Exception.class)
                .handled(true)
                .logExhausted(true)
                .process(exchange -> {
                    // 同步日志失败收尾（使用 ctx）
                    HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    if (ctx != null && ctx.getSyncLogId() != null) {
                        syncLogRepository.findById(ctx.getSyncLogId()).ifPresent(sl -> {
                            OffsetDateTime finishedAt = OffsetDateTime.now();
                            sl.setFinishedAt(finishedAt);
                            if (sl.getStartedAt() != null) {
                                // 使用 Duration 计算时间差，避免时区问题
                                long secs = Duration.between(sl.getStartedAt(), finishedAt).getSeconds();
                                sl.setCostSeconds((int) secs);
                            }
                            sl.setStatus("FAILED");
                            String msg = ex == null ? "unknown error" : ex.getMessage();
                            sl.setMessage(msg);
                            syncLogRepository.save(sl);
                            
                            // 异步记录失败详情到 sync_log_detail
                            try {
                                SyncLogDetail detail = SyncLogDetail.builder()
                                        .syncLogId(ctx.getSyncLogId())
                                        .stage(determineFailureStage(exchange))
                                        .errorCode(ex != null ? ex.getClass().getSimpleName() : "UNKNOWN_ERROR")
                                        .errorMessage(truncateMessage(msg, 1000))
                                        .createdAt(OffsetDateTime.now())
                                        .build();
                                // 异步保存，不阻塞主流程
                                asyncLogService.saveSyncLogDetailAsync(detail);
                                log.warn("[INGEST] 异步记录失败详情 syncLogId={} stage={} error={}", 
                                        ctx.getSyncLogId(), detail.getStage(), detail.getErrorCode());
                            } catch (Exception logEx) {
                                log.error("[INGEST] 异步记录失败详情时出错", logEx);
                            }
                        });
                    }
                    // 释放信号量（防止许可被占用）
                    try {
                        if (ctx != null && ctx.isSemAcquired() && ctx.getSemKey() != null) {
                            var semaphore = semaphoreManager.getSemaphore(ctx.getSemKey(), ctx.getPermits());
                            semaphore.release();
                        }
                    } catch (Exception ignore) {
                    }
                })
                .log("[INGEST] 异常: ${exception.message}");

        // 读取 DB 中的 cron 表达式，作为触发计划（默认为每2小时触发一次）
        // Quartz Cron: 秒 分 时 日 月 星期
        // "0 0 */2 * * ?" = 每2小时的整点触发（0:00, 2:00, 4:00, ...）
        String cron = jobScheduleCacheService.getJobSchedule(JOB_CODE)
                .map(JobSchedule::getCronExpr)
                .orElse("0 0 */2 * * ?");

        from("quartz://ingest/hotelIds?cron=" + cron)
                .routeId("route-hotel-ids-ingest")
                .process(exchange -> {
                    // 1) 检查任务配置与开关（使用缓存）
                    Optional<JobSchedule> jsOpt = jobScheduleCacheService.getJobSchedule(JOB_CODE);
                    if (jsOpt.isEmpty() || !jsOpt.get().isEnabled()) {
                        exchange.setProperty("skip", true);
                        log.debug("[INGEST] 任务未开启或不存在: {}", JOB_CODE);
                        return;
                    }
                    
                    // 2) 检查是否有正在运行的任务（防止重复执行）
                    List<SyncLog> runningSyncs = syncLogRepository.findByJobCodeAndStatus(JOB_CODE, "RUNNING");
                    if (!runningSyncs.isEmpty()) {
                        exchange.setProperty("skip", true);
                        log.warn("[INGEST] 检测到正在运行的任务，跳过本次定时触发 jobCode={} runningCount={}", 
                                JOB_CODE, runningSyncs.size());
                        return;
                    }
                    
                    JobSchedule js = jsOpt.get();
                    // 参数接入（按用户约定的 keys）
                    // - hotelDetailBatchSize: 详情请求子批大小
                    // - hotelIdsBatchSize:    单页ID拉取大小（服务端控制，此处保留备用）
                    // - retryTimes:           重试次数
                    // - retryDelayMs:         基础重试延迟（非429）
                    // - retryJitterMs:        抖动范围（0~N）
                    // - retryBaseDelayMs429:  429 的基础延迟
                    JobScheduleParams pz = JobScheduleParams.parse(objectMapper, js.getParams());
                    int detailBatchSize = pz.detailBatchOrDefault(20);
                    int hotelIdsBatchSize = pz.idsBatchOrDefault(10_000);
                    int retryTimes = pz.retryTimesOrDefault(3);
                    int retryDelayMs = pz.retryDelayOrDefault(3000);
                    int retryJitterMs = pz.retryJitterOrDefault(2000);
                    int retryBaseDelayMs429 = pz.retryBase429OrDefault(1000);
                    int httpTimeoutMs = (js.getHttpTimeoutSec() != null ? js.getHttpTimeoutSec() : 60) * 1000;

                    // 1.1) 创建本次运行的汇总日志（RUNNING）
                    SyncLog sync = SyncLog.builder()
                            .jobCode(JOB_CODE)
                            .traceId("trace-" + System.currentTimeMillis())
                            .startedAt(OffsetDateTime.now())
                            .status("RUNNING")
                            .successCount(0L)
                            .failCount(0L)
                            .totalIds(0L)
                            .totalDetails(0L)
                            .build();
                    sync = syncLogRepository.save(sync);
                    log.info("[INGEST] 任务启动 Quartz traceId={} syncLogId={} detailBatch={} idsBatch={}", sync.getTraceId(), sync.getId(), detailBatchSize, hotelIdsBatchSize);

                    // 2) 读取断点水位
                    long watermark = jobRuntimeStateRepository.findByJobCode(JOB_CODE)
                            .map(JobRuntimeState::getWatermarkMaxHotelId)
                            .orElse(0L);

                    // 3) 组装 ctx
                    HotelIdsIngestContext ctx = HotelIdsIngestContext.builder()
                            .traceId(sync.getTraceId())
                            .syncLogId(sync.getId())
                            .watermark(watermark)
                            .maxConcurrency(js.getMaxConcurrency())
                            .submitBatchSize(pz.batchSizeOrDefault(js.getBatchSize() == null ? 1000 : js.getBatchSize()))
                            .hotelDetailBatchSize(detailBatchSize)
                            .hotelIdsBatchSize(hotelIdsBatchSize)
                            .retryTimes(retryTimes)
                            .retryDelayMs(retryDelayMs)
                            .retryJitterMs(retryJitterMs)
                            .retryBaseDelayMs429(retryBaseDelayMs429)
                            .httpTimeoutMs(httpTimeoutMs)
                            .baseUrl(supplierConfig.getBaseUrl())
                            .app(supplierConfig.getApp())
                            .secret(supplierConfig.getSecret())
                            .reqStartNs(System.nanoTime())
                            .build();
                    exchange.setProperty("ingest", ctx);
                    // 4) 构造签名头（使用 ctx）
                    long ts = System.currentTimeMillis() / 1000;
                    String sign = ctx.sign(ts);
                    exchange.getIn().setHeader("app", ctx.getApp());
                    exchange.getIn().setHeader("timestamp", ts);
                    exchange.getIn().setHeader("sign", sign);

                    // 5) 设置 HTTP 方法与超时
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
                    // Camel 4: 超时可在组件级配置；此处留占位

                    // 6) Redisson 全局信号量（确保并发≤maxConcurrency）
                    int permits = ctx.getMaxConcurrency();
                    String semKey = SEMAPHORE_KEY + JOB_CODE;
                    // 非阻塞尝试获取一个许可，获取失败则跳过本次（避免堆积）
                    var semaphore = semaphoreManager.getSemaphore(semKey, permits);
                    boolean acquired = semaphore.tryAcquire(1);
                    ctx.setSemKey(semKey);
                    ctx.setPermits(permits);
                    ctx.setSemAcquired(acquired);
                    if (!acquired) {
                        exchange.setProperty("skip", true);
                        log.warn("[INGEST] ===============================并发受限，跳过本次触发 ===============================");
                    }
                })
                .to("direct:ingest:hotelIds:runCore");

        // 持续全量采集入口（不间断循环直到无数据）
        from("direct:ingest:hotelIds:runContinuous")
                .routeId("route-hotel-ids-ingest-continuous")
                .process(exchange -> {
                    log.info("[INGEST-CONTINUOUS] ========== 开始持续全量采集 ==========");
                    
                    // 读取 JobSchedule 配置（使用缓存）
                    Optional<JobSchedule> jsOpt = jobScheduleCacheService.getJobSchedule(JOB_CODE);
                    if (jsOpt.isEmpty() || !jsOpt.get().isEnabled()) {
                        exchange.setProperty("skip", true);
                        log.warn("[INGEST-CONTINUOUS] 任务未开启或不存在: {}", JOB_CODE);
                        return;
                    }
                    
                    // 检查是否有正在运行的任务（防止重复执行）
                    List<SyncLog> runningSyncs = syncLogRepository.findByJobCodeAndStatus(JOB_CODE, "RUNNING");
                    if (!runningSyncs.isEmpty()) {
                        exchange.setProperty("skip", true);
                        log.warn("[INGEST-CONTINUOUS] 检测到正在运行的任务，拒绝手动触发 jobCode={} runningCount={}", 
                                JOB_CODE, runningSyncs.size());
                        throw new IllegalStateException("已有任务正在运行，请等待当前任务完成后再试。当前运行任务数: " + runningSyncs.size());
                    }
                    
                    JobSchedule js = jsOpt.get();
                    
                    // 创建汇总日志
                    SyncLog sync = SyncLog.builder()
                            .jobCode(JOB_CODE)
                            .traceId("continuous-" + System.currentTimeMillis())
                            .startedAt(OffsetDateTime.now())
                            .status("RUNNING")
                            .successCount(0L)
                            .failCount(0L)
                            .totalIds(0L)
                            .totalDetails(0L)
                            .build();
                    sync = syncLogRepository.save(sync);
                    
                    exchange.setProperty("syncLogId", sync.getId());
                    exchange.setProperty("traceId", sync.getTraceId());
                    exchange.setProperty("continuousMode", true);
                    
                    log.info("[INGEST-CONTINUOUS] 创建同步日志 traceId={} syncLogId={}", sync.getTraceId(), sync.getId());
                })
                .to("direct:ingest:hotelIds:continuousLoop");
        
        // 持续循环处理核心
        from("direct:ingest:hotelIds:continuousLoop")
                .routeId("route-hotel-ids-continuous-loop")
                .filter(exchangeProperty("skip").isNull())
                .process(exchange -> {
                    Long syncLogId = exchange.getProperty("syncLogId", Long.class);
                    String traceId = exchange.getProperty("traceId", String.class);
                    
                    // 读取配置并检查任务是否被禁用（支持动态停止，使用缓存）
                    Optional<JobSchedule> jsOpt = jobScheduleCacheService.getJobSchedule(JOB_CODE);
                    if (jsOpt.isEmpty() || !jsOpt.get().isEnabled()) {
                        log.warn("[INGEST-CONTINUOUS] 任务已被禁用，停止采集 jobCode={}", JOB_CODE);
                        exchange.setProperty("shouldContinue", false);
                        exchange.setProperty("stopReason", "JOB_DISABLED");
                        return;
                    }
                    JobSchedule js = jsOpt.get();
                    JobScheduleParams pz = JobScheduleParams.parse(objectMapper, js.getParams());
                    
                    // 读取当前水位
                    long watermark = jobRuntimeStateRepository.findByJobCode(JOB_CODE)
                            .map(JobRuntimeState::getWatermarkMaxHotelId)
                            .orElse(0L);
                    
                    // 初始化页计数器（如果不存在）
                    Integer pageCount = exchange.getProperty("pageCount", Integer.class);
                    if (pageCount == null) {
                        pageCount = 0;
                        exchange.setProperty("pageCount", pageCount);
                        exchange.setProperty("totalIdsCollected", 0L);
                        exchange.setProperty("continuousStartTime", System.currentTimeMillis());
                    }
                    
                    // 熔断保护 1：最大页数限制（防止无限循环）
                    int maxPages = pz.maxPagesOrDefault(100);
                    if (pageCount >= maxPages) {
                        log.warn("[INGEST-CONTINUOUS] 达到最大页数限制 pageCount={} maxPages={}", pageCount, maxPages);
                        exchange.setProperty("shouldContinue", false);
                        exchange.setProperty("stopReason", "MAX_PAGES_REACHED");
                        return;
                    }
                    
                    // 熔断保护 2：最大运行时间限制（防止长时间占用资源）
                   /* Long startTime = exchange.getProperty("continuousStartTime", Long.class);
                    if (startTime != null) {
                        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000;
                        int maxRuntimeMinutes = pz.maxRuntimeMinutesOrDefault(360); // 默认6小时
                        if (elapsedMinutes >= maxRuntimeMinutes) {
                            log.warn("[INGEST-CONTINUOUS] 达到最大运行时间限制 elapsedMinutes={} maxRuntimeMinutes={}", 
                                    elapsedMinutes, maxRuntimeMinutes);
                            exchange.setProperty("shouldContinue", false);
                            exchange.setProperty("stopReason", "MAX_RUNTIME_REACHED");
                            return;
                        }
                    }*/
                    
                    pageCount++;
                    exchange.setProperty("pageCount", pageCount);
                    
                    log.info("[INGEST-CONTINUOUS] ========== 第 {} 页开始 watermark={} ==========", pageCount, watermark);
                    
                    // 构建上下文
                    HotelIdsIngestContext ctx = HotelIdsIngestContext.builder()
                            .traceId(traceId)
                            .syncLogId(syncLogId)
                            .watermark(watermark)
                            .maxConcurrency(js.getMaxConcurrency())
                            .submitBatchSize(pz.batchSizeOrDefault(js.getBatchSize() == null ? 1000 : js.getBatchSize()))
                            .hotelDetailBatchSize(pz.detailBatchOrDefault(20))
                            .hotelIdsBatchSize(pz.idsBatchOrDefault(10_000))
                            .retryTimes(pz.retryTimesOrDefault(3))
                            .retryDelayMs(pz.retryDelayOrDefault(3000))
                            .retryJitterMs(pz.retryJitterOrDefault(2000))
                            .retryBaseDelayMs429(pz.retryBase429OrDefault(1000))
                            .httpTimeoutMs((js.getHttpTimeoutSec() != null ? js.getHttpTimeoutSec() : 60) * 1000)
                            .baseUrl(supplierConfig.getBaseUrl())
                            .app(supplierConfig.getApp())
                            .secret(supplierConfig.getSecret())
                            .reqStartNs(System.nanoTime())
                            .build();
                    
                    exchange.setProperty("ingest", ctx);
                    
                    // 签名头
                    long ts = System.currentTimeMillis() / 1000;
                    String sign = ctx.sign(ts);
                    exchange.getIn().setHeader("app", ctx.getApp());
                    exchange.getIn().setHeader("timestamp", ts);
                    exchange.getIn().setHeader("sign", sign);
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
                    
                    // 信号量控制（持续模式使用独立的信号量key）
                    String semKey = SEMAPHORE_KEY + JOB_CODE + ":continuous";
                    int permits = ctx.getMaxConcurrency();
                    var semaphore = semaphoreManager.getSemaphore(semKey, permits);
                    boolean acquired = semaphore.tryAcquire(1);
                    
                    ctx.setSemKey(semKey);
                    ctx.setPermits(permits);
                    ctx.setSemAcquired(acquired);
                    
                    if (!acquired) {
                        // 使用重试次数限制，避免无限等待
                        Integer semRetries = exchange.getProperty("semRetries", Integer.class);
                        if (semRetries == null) semRetries = 0;
                        
                        int maxRetries = pz.semaphoreWaitRetriesOrDefault(3);
                        if (semRetries >= maxRetries) {
                            log.error("[INGEST-CONTINUOUS] 信号量获取失败，超过最大重试次数 retries={} maxRetries={}", 
                                    semRetries, maxRetries);
                            exchange.setProperty("shouldContinue", false);
                            exchange.setProperty("stopReason", "SEMAPHORE_ACQUISITION_FAILED");
                            return;
                        }
                        
                        semRetries++;
                        exchange.setProperty("semRetries", semRetries);
                        log.warn("[INGEST-CONTINUOUS] 并发受限，等待10秒后重试 ({}/{})", semRetries, maxRetries);
                        Thread.sleep(10000);
                        // 重新尝试
                        exchange.setProperty("shouldRetry", true);
                        return;
                    }
                    
                    // 获取成功，重置重试计数器
                    exchange.setProperty("semRetries", null);
                })
                .to("direct:ingest:hotelIds:runCoreContinuous")
                .process(exchange -> {
                    // 检查是否需要重试
                    Boolean shouldRetry = exchange.getProperty("shouldRetry", Boolean.class);
                    if (shouldRetry != null && shouldRetry) {
                        exchange.setProperty("shouldRetry", null);
                        // 递归调用继续循环
                        exchange.getContext().createFluentProducerTemplate()
                                .withExchange(exchange)
                                .to("direct:ingest:hotelIds:continuousLoop")
                                .send();
                        return;
                    }
                    
                    // 判断是否继续下一页
                    Boolean hasMoreData = exchange.getProperty("hasMoreData", Boolean.class);
                    Boolean shouldContinue = exchange.getProperty("shouldContinue", Boolean.class);
                    
                    if (shouldContinue == Boolean.FALSE) {
                        String stopReason = exchange.getProperty("stopReason", String.class);
                        log.info("[INGEST-CONTINUOUS] ========== 达到停止条件，结束采集 reason={} ==========", stopReason);
                        finalizeContinuousSync(exchange);
                        return;
                    }
                    
                    if (hasMoreData == null || !hasMoreData) {
                        log.info("[INGEST-CONTINUOUS] ========== 无更多数据，完成全量采集 ==========");
                        exchange.setProperty("stopReason", "NO_MORE_DATA");
                        finalizeContinuousSync(exchange);
                        return;
                    }
                    
                    // 打印进度
                    Integer pageCount = exchange.getProperty("pageCount", Integer.class);
                    Long totalIds = exchange.getProperty("totalIdsCollected", Long.class);
                    Long startTime = exchange.getProperty("continuousStartTime", Long.class);
                    long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                    
                    log.info("[INGEST-CONTINUOUS] ========== 进度: 已处理 {} 页，共 {} 个ID，耗时 {}秒 ==========",
                            pageCount, totalIds, elapsedSeconds);
                    
                    // 页间延迟（避免对API造成过大压力，使用缓存）
                    JobScheduleParams currentPz = jobScheduleCacheService.getJobScheduleParams(JOB_CODE);
                    //延长 1s
                    int delayMs = currentPz.pageDelayOrDefault(1000);
                    if (delayMs > 0) {
                        log.debug("[INGEST-CONTINUOUS] 页间延迟 {}ms", delayMs);
                        Thread.sleep(delayMs);
                    }
                    
                    // 递归调用继续下一页
                    exchange.getContext().createFluentProducerTemplate()
                            .withExchange(exchange)
                            .to("direct:ingest:hotelIds:continuousLoop")
                            .send();
                });
        
        // 立即执行入口（手动触发）：与 Quartz 路由共享同一核心处理链
        from("direct:ingest:hotelIds:runOnce")
                .routeId("route-hotel-ids-ingest-runOnce")
                .process(exchange -> {
                    // 若外部未传 syncLogId/traceId，则创建 RUNNING 的 SyncLog
                    Long syncLogId = (Long) exchange.getProperty("syncLogId");
                    String traceId = (String) exchange.getProperty("traceId");

                    // 读取 JobSchedule 配置（使用缓存）
                    Optional<JobSchedule> jsOpt = jobScheduleCacheService.getJobSchedule(JOB_CODE);
                    if (jsOpt.isEmpty() || !jsOpt.get().isEnabled()) {
                        exchange.setProperty("skip", true);
                        log.debug("[INGEST-runOnce] 任务未开启或不存在: {}", JOB_CODE);
                        return;
                    }
                    
                    // 检查是否有正在运行的任务（防止重复执行）
                    List<SyncLog> runningSyncs = syncLogRepository.findByJobCodeAndStatus(JOB_CODE, "RUNNING");
                    if (!runningSyncs.isEmpty()) {
                        exchange.setProperty("skip", true);
                        log.warn("[INGEST-runOnce] 检测到正在运行的任务，拒绝手动触发 jobCode={} runningCount={}", 
                                JOB_CODE, runningSyncs.size());
                        throw new IllegalStateException("已有任务正在运行，请等待当前任务完成后再试。当前运行任务数: " + runningSyncs.size());
                    }
                    
                    JobSchedule js = jsOpt.get();

                    if (syncLogId == null || traceId == null) {
                        SyncLog sync = SyncLog.builder()
                                .jobCode(JOB_CODE)
                                .traceId("trace-" + System.currentTimeMillis())
                                .startedAt(OffsetDateTime.now())
                                .status("RUNNING")
                                .successCount(0L)
                                .failCount(0L)
                                .totalIds(0L)
                                .totalDetails(0L)
                                .build();
                        sync = syncLogRepository.save(sync);
                        exchange.setProperty("syncLogId", sync.getId());
                        exchange.setProperty("traceId", sync.getTraceId());
                    }
                    log.info("[INGEST-runOnce] 手动触发启动 traceId={} syncLogId={} watermark(预读中)=?", exchange.getProperty("traceId"), exchange.getProperty("syncLogId"));

                    exchange.setProperty("httpTimeoutSec", js.getHttpTimeoutSec());
                    exchange.setProperty("maxConcurrency", js.getMaxConcurrency());
                    int submitBatch = js.getBatchSize() == null ? 1000 : js.getBatchSize();
                    exchange.setProperty("submitBatchSize", submitBatch);

                    // 参数接入（解析 JSON params 到对象）
                    JobScheduleParams pz2 = JobScheduleParams.parse(objectMapper, js.getParams());
                    int detailBatchSize = pz2.detailBatchOrDefault(20);
                    int hotelIdsBatchSize = pz2.idsBatchOrDefault(10_000);
                    int retryTimes = pz2.retryTimesOrDefault(3);
                    int retryDelayMs = pz2.retryDelayOrDefault(3000);
                    int retryJitterMs = pz2.retryJitterOrDefault(2000);
                    int retryBaseDelayMs429 = pz2.retryBase429OrDefault(1000);
                    exchange.setProperty("hotelDetailBatchSize", detailBatchSize);
                    exchange.setProperty("hotelIdsBatchSize", hotelIdsBatchSize);
                    exchange.setProperty("retryTimes", retryTimes);
                    exchange.setProperty("retryDelayMs", retryDelayMs);
                    exchange.setProperty("retryJitterMs", retryJitterMs);
                    exchange.setProperty("retryBaseDelayMs429", retryBaseDelayMs429);

                    // 水位读取
                    long watermark = jobRuntimeStateRepository.findByJobCode(JOB_CODE)
                            .map(JobRuntimeState::getWatermarkMaxHotelId)
                            .orElse(0L);
                    exchange.setProperty("watermark", watermark);

                    // 供应商参数与 httpTimeoutMs 计算
                    exchange.setProperty("baseUrl", supplierConfig.getBaseUrl());
                    exchange.setProperty("app", supplierConfig.getApp());
                    exchange.setProperty("secret", supplierConfig.getSecret());
                    Integer jsTimeout = js.getHttpTimeoutSec();
                    Integer spTimeout = supplierConfig.getHttpTimeoutSec();
                    int httpTimeoutMs = (jsTimeout != null ? jsTimeout : (spTimeout != null ? spTimeout : 30)) * 1000;
                    exchange.setProperty("httpTimeoutMs", httpTimeoutMs);

                    // 构造签名头
                    long ts = System.currentTimeMillis() / 1000;
                    String sign = Md5Signer.buildSign(supplierConfig.getApp(), supplierConfig.getSecret(), ts);
                    exchange.getIn().setHeader("app", supplierConfig.getApp());
                    exchange.getIn().setHeader("timestamp", ts);
                    exchange.getIn().setHeader("sign", sign);

                    // 并发控制
                    int permits = (int) exchange.getProperty("maxConcurrency");
                    String semKey = SEMAPHORE_KEY + JOB_CODE;
                    exchange.setProperty("semKey", semKey);
                    exchange.setProperty("permits", permits);
                    var semaphore = semaphoreManager.getSemaphore(semKey, permits);
                    boolean acquired = semaphore.tryAcquire(1);
                    exchange.setProperty("semAcquired", acquired);
                    if (!acquired) {
                        exchange.setProperty("skip", true);
                        log.warn("[INGEST-runOnce] ===============================并发受限，跳过本次触发 ===============================");
                    }
                    exchange.setProperty("reqStartNs", System.nanoTime());
                    log.info("[INGEST-runOnce] 参数加载完成 traceId={} watermark={} detailBatch={} idsBatch={} permits={}",
                            exchange.getProperty("traceId"), exchange.getProperty("watermark"),
                            exchange.getProperty("hotelDetailBatchSize"), exchange.getProperty("hotelIdsBatchSize"), exchange.getProperty("maxConcurrency"));

                    // 组装并挂载路由专用上下文对象（保留原属性，便于过渡期兼容）
                    HotelIdsIngestContext ctx = HotelIdsIngestContext.builder()
                            .traceId((String) exchange.getProperty("traceId"))
                            .syncLogId((Long) exchange.getProperty("syncLogId"))
                            .watermark((long) exchange.getProperty("watermark"))
                            .maxConcurrency((int) exchange.getProperty("maxConcurrency"))
                            .submitBatchSize((int) exchange.getProperty("submitBatchSize"))
                            .hotelDetailBatchSize((int) exchange.getProperty("hotelDetailBatchSize"))
                            .hotelIdsBatchSize((int) exchange.getProperty("hotelIdsBatchSize"))
                            .retryTimes((int) exchange.getProperty("retryTimes"))
                            .retryDelayMs((int) exchange.getProperty("retryDelayMs"))
                            .retryJitterMs((int) exchange.getProperty("retryJitterMs"))
                            .retryBaseDelayMs429((int) exchange.getProperty("retryBaseDelayMs429"))
                            .httpTimeoutMs((int) exchange.getProperty("httpTimeoutMs"))
                            .baseUrl(supplierConfig.getBaseUrl())
                            .app(supplierConfig.getApp())
                            .secret(supplierConfig.getSecret())
                            .reqStartNs(System.nanoTime())
                            .build();
                    exchange.setProperty("ingest", ctx);


                })
                .to("direct:ingest:hotelIds:runCore");

        // 持续模式的核心处理（不释放信号量，由循环控制）
        from("direct:ingest:hotelIds:runCoreContinuous")
                .routeId("route-hotel-ids-ingest-core-continuous")
                .to("direct:ingest:hotelIds:fetchAndProcess")
                .process(exchange -> {
                    // 持续模式不在这里释放信号量，由外层循环控制
                    HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    if (ctx != null && ctx.isSemAcquired() && ctx.getSemKey() != null) {
                        var semaphore = semaphoreManager.getSemaphore(ctx.getSemKey(), ctx.getPermits());
                        semaphore.release();
                        ctx.setSemAcquired(false);
                    }
                });
        
        // 核心处理链：ID 拉取 -> 推进水位 -> API日志 -> 详情拉取与入库 -> 汇总收尾 -> 释放信号量
        from("direct:ingest:hotelIds:runCore")
                .routeId("route-hotel-ids-ingest-core")
                .to("direct:ingest:hotelIds:fetchAndProcess")
                .process(exchange -> {
                    // 单次模式：完成后释放信号量并更新同步日志
                    HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    finalizeSingleSync(ctx);
                });
        
        // 实际的数据拉取和处理逻辑（共用）
        from("direct:ingest:hotelIds:fetchAndProcess")
                .routeId("route-hotel-ids-fetch-process")
                .filter(exchangeProperty("skip").isNull())
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                // 确保请求前设置签名头（部分入口未显式设置到当前 message）
                .process(exchange -> {
                    long tsNow = System.currentTimeMillis() / 1000;
                    HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    //设置签名头
                    applyAuthHeaders(exchange, ctx, tsNow);

                    log.info("[INGEST] 准备拉取ID页 traceId={} watermark={} pageSize={}", ctx.getTraceId(), ctx.getWatermark(), ctx.getHotelIdsBatchSize());
                })
                .process(exchange -> {
                    // 使用 ctx 构建 URL 并放入 Header，降低 toD 表达式复杂度
                    HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    String url = buildIdsUrl(ctx);
                    exchange.getMessage().setHeader("targetUrl", url);
                })
                .toD("${header.targetUrl}")
                .process(exchange -> {
                    log.debug("[INGEST] 请求URL: {}", exchange.getFromEndpoint().getEndpointUri());
                    // 以下内容拷贝自原 Quartz 路由的核心处理逻辑
                    String body = exchange.getMessage().getBody(String.class);
                    Integer status = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                    HotelIdsIngestContext ctx0 = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    long startNs = ctx0 != null ? ctx0.getReqStartNs() : Optional.ofNullable((Long) exchange.getProperty("reqStartNs")).orElse(0L);
                    int durationMs = startNs == 0 ? 0 : (int) ((System.nanoTime() - startNs) / 1_000_000);

                    HotelIdsResponse resp = null;
                    try {
                        resp = objectMapper.readValue(body, HotelIdsResponse.class);
                    } catch (Exception e) {
                        log.error("[INGEST] 解析在线酒店ID响应失败", e);
                    }

                    HotelIdsIngestContext ctxPage = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                    long oldWatermark = ctxPage.getWatermark();
                    long pageMax = oldWatermark;
                    if (resp != null && resp.getData() != null) {
                        if (resp.getData().getMaxHotelId() != null) {
                            pageMax = Math.max(pageMax, resp.getData().getMaxHotelId());
                        }
                        if (resp.getData().getHotelIds() != null && !resp.getData().getHotelIds().isEmpty()) {
                            for (Long id : resp.getData().getHotelIds()) {
                                if (id != null) pageMax = Math.max(pageMax, id);
                            }
                        }
                    }

                    if (pageMax > oldWatermark) {
                        JobRuntimeState state = jobRuntimeStateRepository.findByJobCode(JOB_CODE)
                                .orElseGet(() -> {
                                    JobRuntimeState s = new JobRuntimeState();
                                    s.setJobCode(JOB_CODE);
                                    return s;
                                });
                        state.setWatermarkMaxHotelId(pageMax);
                        state.setUpdatedAt(OffsetDateTime.now());
                        jobRuntimeStateRepository.save(state);
                        // 更新 ctx 水位
                        if (pageMax > oldWatermark) {
                            ctxPage.setWatermark(pageMax);
                        }
                        log.info("[INGEST] 推进水位: {} -> {}", oldWatermark, pageMax);
                    } else {
                        log.info("[INGEST] 水位无变化: {}", oldWatermark);
                    }

                    String url = exchange.getFromEndpoint().getEndpointUri();
                    String headersJson;
                    try {
                        headersJson = objectMapper.writeValueAsString(exchange.getIn().getHeaders());
                    } catch (Exception e) {
                        headersJson = "{}";
                    }
                    byte[] respCompressed = body == null ? null : GzipCompressor.compress(body.getBytes(StandardCharsets.UTF_8));

                    ApiRequestLog logRow = ApiRequestLog.builder()
                            .traceId(ctxPage.getTraceId())
                            .jobCode(JOB_CODE)
                            .source("ALL")
                            .httpMethod("GET")
                            .url(url)
                            .requestHeaders(headersJson)
                            .responseStatus(status)
                            .durationMs(durationMs)
                            .responseBodyCompressed(respCompressed)
                            .responseSizeBytes(body == null ? 0 : body.length())
                            .compression(CompressionCodec.gzip)
                            .timestampUtc(OffsetDateTime.now())
                            .createdAt(OffsetDateTime.now())
                            .build();
                    // 异步保存 API 日志，不阻塞主流程
                    asyncLogService.saveApiRequestLogAsync(logRow);

                    log.info("[INGEST] 拉取ID页完成 watermark(old)={} status={} durationMs={} bodySize={} hasData={} code={}",
                            oldWatermark,
                            status,
                            durationMs,
                            body == null ? 0 : body.length(),
                            resp == null ? null : resp.getHasData(),
                            resp == null ? null : resp.getCode());

                    Long syncLogId = ctxPage.getSyncLogId();
                    String traceId = ctxPage.getTraceId();
                    if (resp != null && resp.getData() != null && resp.getData().getHotelIds() != null) {
                        List<Long> ids = resp.getData().getHotelIds();
                        log.info("[INGEST] 本页获取ID数量={} maxHotelId={} hasData={} code={}", ids.size(), resp.getData().getMaxHotelId(), resp.getHasData(), resp.getCode());
                        
                        // 保存 hasData 标志供持续模式使用
                        exchange.setProperty("hasMoreDataFlag", resp.getHasData());
                        exchange.setProperty("currentPageIdsCount", ids.size());
                        if (syncLogId != null) {
                            syncLogRepository.findById(syncLogId).ifPresent(sl -> {
                                sl.setTotalIds((sl.getTotalIds() == null ? 0 : sl.getTotalIds()) + ids.size());
                                syncLogRepository.save(sl);
                            });
                        }
                        List<Hotels> batch = new ArrayList<>();
                        HotelIdsIngestContext detailCtx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                        int submitBatchSize = detailCtx.getSubmitBatchSize();
                        int detailBatchSize = detailCtx.getHotelDetailBatchSize();
                        int retryTimes = detailCtx.getRetryTimes();
                        int retryDelayMs = detailCtx.getRetryDelayMs();
                        int retryJitterMs = detailCtx.getRetryJitterMs();
                        int retryBaseDelayMs429 = detailCtx.getRetryBaseDelayMs429();
                        for (int i = 0; i < ids.size(); i += detailBatchSize) {
                            List<Long> sub = ids.subList(i, Math.min(i + detailBatchSize, ids.size()));
                            log.info("[INGEST] 详情批次开始 traceId={} batchIndex={} batchSize={} hotelIds={}",
                                    detailCtx.getTraceId(), (i / detailBatchSize) + 1, sub.size(), sub);

                            String detailUrl = buildDetailUrl(detailCtx, sub);
                            String detailBody = null;
                            Integer detailStatus = null;
                            Exception lastEx = null;
                            int attemptsUsed = 0;
                            int hit429 = 0;
                            long batchStartNs = System.nanoTime();
                            for (int attempt = 1; attempt <= retryTimes; attempt++) {
                                try {
                                    // 每次重试重新计算时间戳与签名，避免跨时间窗导致认证失败
                                    long tsAttempt = System.currentTimeMillis() / 1000;
                                    var detEx = exchange.getContext().createFluentProducerTemplate()
                                            .withHeader(Exchange.HTTP_METHOD, "GET")
                                            // 注意：FluentProducerTemplate 默认不会继承当前 Exchange 的 headers，这里需要显式传入
                                            .withHeader("app", detailCtx.getApp())
                                            .withHeader("timestamp", tsAttempt)
                                            .withHeader("sign", detailCtx.sign(tsAttempt))
                                            .to(detailUrl)
                                            .request(org.apache.camel.Exchange.class);
                                    detailStatus = detEx.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                                    detailBody = detEx.getMessage().getBody(String.class);
                                    attemptsUsed = attempt;
                                    if (detailStatus != null && detailStatus == 429) hit429++;
                                    if (detailStatus != null && (detailStatus == 429 || detailStatus >= 500)) {
                                        lastEx = new RuntimeException("HTTP status " + detailStatus);
                                    } else {
                                        lastEx = null;
                                        int durMs = (int) ((System.nanoTime() - batchStartNs) / 1_000_000);
                                        log.info("[INGEST] 详情批次成功 status={} bodySize={} batchSize={}", detailStatus, detailBody == null ? 0 : detailBody.length(), sub.size());
                                        log.info("[METRIC][DETAIL] success batchSize={} attemptsUsed={} status={} durationMs={} hit429={}",
                                                sub.size(), attemptsUsed, detailStatus, durMs, hit429);
                                        break;
                                    }
                                } catch (Exception ex) {
                                    lastEx = ex;
                                    if (attempt > 1) {
                                        log.warn("[INGEST] 详情批次重试 attempt={} status={} err={}", attempt, detailStatus, ex.getMessage());
                                    }
                                }
                                long base = (detailStatus != null && detailStatus == 429) ? retryBaseDelayMs429 : retryDelayMs;
                                long backoff = base * (1L << (attempt - 1));
                                long jitter = retryJitterMs <= 0 ? 0 : java.util.concurrent.ThreadLocalRandom.current().nextLong(0, retryJitterMs + 1L);
                                long sleepMs = Math.max(0, backoff + jitter);
                                try {
                                    Thread.sleep(sleepMs);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            if ((detailBody == null && lastEx != null) || (detailStatus != null && (detailStatus == 429 || detailStatus >= 400))) {
                                log.error("[INGEST] 详情批次失败 status={} err={} hotelIds={}", detailStatus, lastEx == null ? null : lastEx.getMessage(), sub);
                                int durMs = (int) ((System.nanoTime() - batchStartNs) / 1_000_000);
                                log.warn("[METRIC][DETAIL] failed batchSize={} attemptsUsed={} status={} durationMs={} hit429={} err={}",
                                        sub.size(), attemptsUsed == 0 ? retryTimes : attemptsUsed, detailStatus, durMs, hit429, lastEx == null ? null : lastEx.getMessage());
                                if (syncLogId != null) {
                                    final String msg = lastEx.getMessage();
                                    String err;
                                    if (detailStatus != null) {
                                        if (detailStatus == 429) err = "HTTP_429";
                                        else if (detailStatus >= 500) err = "HTTP_5XX";
                                        else if (detailStatus >= 400) err = "HTTP_4XX";
                                        else err = "HTTP_ERROR";
                                    } else if (msg != null && msg.toLowerCase().contains("timeout")) {
                                        err = "TIMEOUT";
                                    } else {
                                        err = "HTTP_ERROR";
                                    }
                                    // 批量收集失败详情，异步保存
                                    List<SyncLogDetail> failureDetails = new ArrayList<>();
                                    for (Long hid : sub) {
                                        SyncLogDetail d = SyncLogDetail.builder()
                                                .syncLogId(syncLogId)
                                                .hotelId(hid)
                                                .stage("DETAIL_FETCH")
                                                .errorCode(err)
                                                .errorMessage(msg)
                                                .createdAt(OffsetDateTime.now())
                                                .build();
                                        failureDetails.add(d);
                                    }
                                    if (!failureDetails.isEmpty()) {
                                        asyncLogService.saveSyncLogDetailsAsync(failureDetails);
                                    }
                                }
                                continue; // 跳过后续解析和入库，继续下一个批次
                            }

                            HotelDetailResponse detailResp = null;
                            try {
                                detailResp = objectMapper.readValue(detailBody, HotelDetailResponse.class);
                            } catch (Exception e) {
                                log.error("[INGEST] 解析酒店详情响应失败", e);
                            }

                            if (detailResp != null && detailResp.getData() != null) {
                                for (HotelDetailResponse.Item it : detailResp.getData()) {
                                    Hotels row = new Hotels();
                                    row.setHotelId(it.getHotelId());


                                    /// 先以旧规则计算标签源（CN/INTL），实际数据源映射为 Elong/Agoda
                                    String provider = (it.getHotelId() != null && it.getHotelId() >= 20_000_000L) ? "Elong" : "Agoda";
                                    row.setSource(provider);

                                    ///解析提取： 中国大陆  = CN  | 港澳台 = HMT | 国际其他国家 = INTL
                                    String tagSrc = HotelStructuredExtractor.extractTagSource(it.getOrigContent(),provider);
                                    row.setTagSource(tagSrc);


                                    ///provider = Agoda , 且国家代码是CN的，应该排除掉（数据异常）
                                    if("CN".equals(tagSrc)  && provider.equalsIgnoreCase("Agoda")) {
                                        log.debug("[INGEST] 异常数据过滤，酒店ID={} 国家代码={} 数据来源={} ,已过滤", it.getHotelId(), tagSrc,provider);
                                        continue;
                                    }

                                    // 结构化字段解析并填充
                                    HotelStructuredExtractor.Result s = HotelStructuredExtractor.extract(it.getOrigContent(), provider);

                                    ///  以下字段，从 API 原文解析有中英混合的情况，需要通过判断识别中文英文 赋值到对应的字段
                                    ///  HotelNameCn （中文） ，HotelNameEn （英文）
                                    ///  CountryCn（国家中文），CountryEn（国家英文）
                                    ///  CityCn（城市中文），CityEn（城市英文）
                                    ///  RegionCn（地区中文），RegionEn（地区英文）
                                    ///  AddressCn（地址中文），AddressEn（地址英文）

                                    row.setHotelNameCn(s.getNameCn());
                                    row.setHotelNameEn(s.getNameEn());

                                    row.setCountryCn(s.getCountryCn());
                                    row.setCountryEn(s.getCountryEn());
                                    row.setCountryCode(s.getCountryCode());

                                    row.setCityCn(s.getCityCn());
                                    row.setCityEn(s.getCityEn());

                                    row.setRegionCn(s.getRegionCn());
                                    row.setRegionEn(s.getRegionEn());

                                    row.setContinentCn(s.getContinentCn());
                                    row.setContinentEn(s.getContinentEn());

                                    row.setAddressCn(s.getAddressCn());
                                    row.setAddressEn(s.getAddressEn());

                                    row.setLongitude(s.getLongitude());
                                    row.setLatitude(s.getLatitude());

                                    row.setHotelGroupCn(s.getHotelGroupCn());
                                    row.setHotelGroupEn(s.getHotelGroupEn());

                                    row.setHotelBrandCn(s.getHotelBrandCn());
                                    row.setHotelBrandEn(s.getHotelBrandEn());
                                    row.setFetchedAt(OffsetDateTime.now());

                                    // ========== 人工修正字段优先覆盖策略 ==========
                                    row.setNewHotelNameCn(it.getNewHotelNameCn());
                                    row.setNewHotelNameEn(it.getNewHotelNameEn());
                                    row.setAccommodationType(it.getAccommodationType());
                                    row.setSearchEnable(it.getSearchEnable());
                                    row.setTel(it.getTel());
                                    row.setScore(it.getScore());
                                    // 如果存在人工修正字段，则覆盖对应的业务字段（保留两套字段但内容一致）
                                    if (it.getNewHotelNameCn() != null && !it.getNewHotelNameCn().trim().isEmpty()) {
                                        row.setHotelNameCn(it.getNewHotelNameCn());
                                    }
                                    if (it.getNewHotelNameEn() != null && !it.getNewHotelNameEn().trim().isEmpty()) {
                                        row.setHotelNameEn(it.getNewHotelNameEn());
                                    }
                                    if (it.getCountryIso2() != null && !it.getCountryIso2().trim().isEmpty()) {
                                        row.setCountryCode(it.getCountryIso2());
                                    }
                                    if (it.getAddress() != null && !it.getAddress().trim().isEmpty()) {
                                        row.setAddressCn(it.getAddress());
                                    }
                                    if (it.getAddressEn() != null && !it.getAddressEn().trim().isEmpty()) {
                                        row.setAddressEn(it.getAddressEn());
                                    }
                                    // ========== 人工修正字段覆盖结束 ==========

                                    // 原始内容 GZIP 压缩后入库（原文 -> GZIP -> Base64）
                                    String origContent = it.getOrigContent();
                                    byte[] compressedRaw = GzipCompressor.compressString(origContent);
                                    String base64Compressed = Base64.getEncoder().encodeToString(compressedRaw);
                                    row.setRawCompressed(base64Compressed);


                                    batch.add(row);
                                    if (batch.size() >= submitBatchSize) {
                                        log.info("[INGEST] 批次入库提交 size={} submitBatchSize={}", batch.size(), submitBatchSize);
                                        hotelsSinkService.saveInBatches(batch, submitBatchSize, syncLogId);
                                        // 入库成功后，逐条写入 Redis Stream 事件
                                        try {
                                            String trace = traceId;
                                            writeXaddEvents(batch, trace, syncLogId);
                                        } catch (Exception ex) {
                                            log.error("[INGEST] 生产 Redis Stream 事件失败", ex);
                                        }
                                        batch.clear();
                                    }
                                }
                            } else {
                                if (syncLogId != null) {
                                    // 批量收集失败详情，异步保存
                                    List<SyncLogDetail> parseErrorDetails = new ArrayList<>();
                                    for (Long hid : sub) {
                                        SyncLogDetail d = SyncLogDetail.builder()
                                                .syncLogId(syncLogId)
                                                .hotelId(hid)
                                                .stage("DETAIL_FETCH")
                                                .errorCode("PARSE_ERROR")
                                                .errorMessage("detail response parse failed")
                                                .createdAt(OffsetDateTime.now())
                                                .build();
                                        parseErrorDetails.add(d);
                                    }
                                    if (!parseErrorDetails.isEmpty()) {
                                        asyncLogService.saveSyncLogDetailsAsync(parseErrorDetails);
                                    }
                                }
                            }

                            ApiRequestLog dlog = ApiRequestLog.builder()
                                    .traceId(traceId == null ? ("trace-" + System.currentTimeMillis()) : traceId)
                                    .jobCode(JOB_CODE)
                                    .source("ALL")
                                    .httpMethod("GET")
                                    .url(detailUrl)
                                    .requestHeaders("{}")
                                    .responseStatus(detailStatus)
                                    .durationMs(null)
                                    .responseBodyCompressed(detailBody == null ? null : GzipCompressor.compress(detailBody.getBytes(StandardCharsets.UTF_8)))
                                    .responseSizeBytes(detailBody == null ? 0 : detailBody.length())
                                    .compression(CompressionCodec.gzip)
                                    .timestampUtc(OffsetDateTime.now())
                                    .createdAt(OffsetDateTime.now())
                                    .build();
                            // 异步保存 API 日志，不阻塞主流程
                            asyncLogService.saveApiRequestLogAsync(dlog);
                        }
                        if (!batch.isEmpty()) {
                            log.info("[INGEST] 末尾批次入库提交 size={}", batch.size());
                            hotelsSinkService.saveInBatches(batch, submitBatchSize, ctxPage.getSyncLogId());
                            try {
                                String trace = traceId;
                                writeXaddEvents(batch, trace, ctxPage.getSyncLogId());
                            } catch (Exception ex) {
                                log.error("[INGEST] 生产 Redis Stream 事件失败(末尾批次)", ex);
                            }
                            batch.clear();
                        }
                    }
                    // 检查是否有更多数据（用于持续模式）
                    Boolean continuousMode = exchange.getProperty("continuousMode", Boolean.class);
                    if (continuousMode != null && continuousMode) {
                        // 持续模式：从响应中提取 hasData 标志
                        Boolean hasMoreData = exchange.getProperty("hasMoreDataFlag", Boolean.class);
                        exchange.setProperty("hasMoreData", hasMoreData);
                        
                        // 累加本页ID数量
                        Integer currentPageIds = exchange.getProperty("currentPageIdsCount", Integer.class);
                        if (currentPageIds != null && currentPageIds > 0) {
                            Long totalIds = exchange.getProperty("totalIdsCollected", Long.class);
                            totalIds = (totalIds == null ? 0L : totalIds) + currentPageIds;
                            exchange.setProperty("totalIdsCollected", totalIds);
                        }
                        
                        log.info("[INGEST-CONTINUOUS] 本页处理完成 hasMoreData={}", hasMoreData);
                    } else {
                        // 单次模式：正常结束
                        HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
                        // 不在这里处理，移到外层
                    }
                });
    }

    /**
     * 设置签名认证头
     * @param exchange
     * @param ctx
     * @param tsSec
     */
    private void applyAuthHeaders(Exchange exchange, HotelIdsIngestContext ctx, long tsSec) {
        String sign = ctx.sign(tsSec);
        exchange.getIn().setHeader("app", ctx.getApp());
        exchange.getIn().setHeader("timestamp", tsSec);
        exchange.getIn().setHeader("sign", sign);
    }

    /**
     * 私有辅助方法：构建分页ID URL
     * @param ctx
     * @return
     */
    private String buildIdsUrl(HotelIdsIngestContext ctx) {
        return ctx.getBaseUrl()
            + "/api/StandardHotel/GetOnlineHotelIds?maxHotelId=" + ctx.getWatermark()
            + "&pageSize=" + ctx.getHotelIdsBatchSize()
            + "&timestamp=0";
    }

    /**
     *  私有辅助方法：构建详情URL（hotelIds 逗号分隔）
     * @param ctx
     * @param ids
     * @return
     */
    private String buildDetailUrl(HotelIdsIngestContext ctx, List<Long> ids) {
        String idParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return ctx.getBaseUrl() + "/api/StandardHotel/GetHotelOrigContent?hotelIds=" + idParam;
    }

    /**
     * 写入 Redis Stream 事件
     * @param rows
     * @param traceId
     * @param syncLogId
     */
    private void writeXaddEvents(List<Hotels> rows, String traceId, Long syncLogId) {
        if (rows == null || rows.isEmpty()) return;
        for (Hotels r : rows) {
            Long hotelId = r.getHotelId();
            if (hotelId == null) continue;
            String providerSource = r.getSource(); // 期望为 Elong/Agoda
            String tagSource = r.getTagSource();   // 期望为 CN/INTL/HMT

            // 事件字段
            Map<String, String> msg = new HashMap<>();
            msg.put("eventType", "UPSERT");
            msg.put("rowId", r.getId() == null ? "" : String.valueOf(r.getId()));
            msg.put("hotelId", String.valueOf(hotelId));
            msg.put("providerSource", providerSource == null ? "" : providerSource);
            msg.put("tagSource", tagSource);
            msg.put("traceId", traceId == null ? "" : traceId);
            msg.put("syncLogId", syncLogId == null ? "" : String.valueOf(syncLogId));
            msg.put("fetchedAt", r.getFetchedAt() == null ? "" : r.getFetchedAt().toString());
            redisStreamPublisher.xadd(REDIS_STREAM, msg);
        }
    }

    /**
     * 完成持续采集的收尾工作
     * @param exchange
     */
    private void finalizeContinuousSync(Exchange exchange) {
        Long syncLogId = exchange.getProperty("syncLogId", Long.class);
        String stopReason = exchange.getProperty("stopReason", String.class);
        Integer pageCount = exchange.getProperty("pageCount", Integer.class);
        Long totalIds = exchange.getProperty("totalIdsCollected", Long.class);
        
        if (syncLogId != null) {
            syncLogRepository.findById(syncLogId).ifPresent(sl -> {
                OffsetDateTime finishedAt = OffsetDateTime.now();
                sl.setFinishedAt(finishedAt);
                if (sl.getStartedAt() != null) {
                    // 使用 Duration 计算时间差，避免时区问题
                    long secs = Duration.between(sl.getStartedAt(), finishedAt).getSeconds();
                    sl.setCostSeconds((int) secs);
                }
                if (sl.getFailCount() != null && sl.getFailCount() > 0) {
                    sl.setStatus("PARTIAL");
                } else if (sl.getStatus() == null || sl.getStatus().equals("RUNNING")) {
                    sl.setStatus("SUCCESS");
                }
                
                // 记录停止原因到 message 字段
                String message = String.format("停止原因: %s | 总页数: %d | 总ID数: %d", 
                    stopReason != null ? stopReason : "UNKNOWN", 
                    pageCount != null ? pageCount : 0,
                    totalIds != null ? totalIds : 0);
                sl.setMessage(message);
                
                syncLogRepository.save(sl);
                
                log.info("[INGEST-CONTINUOUS] 任务完成 traceId={} syncLogId={} status={} costSeconds={} stopReason={} totalPages={} totalIds={}", 
                        sl.getTraceId(), sl.getId(), sl.getStatus(), sl.getCostSeconds(), stopReason, pageCount, totalIds);
            });
        }
        
        // 释放信号量（如果还持有）
        HotelIdsIngestContext ctx = exchange.getProperty("ingest", HotelIdsIngestContext.class);
        if (ctx != null && ctx.isSemAcquired() && ctx.getSemKey() != null) {
            try {
                var semaphore = semaphoreManager.getSemaphore(ctx.getSemKey(), ctx.getPermits());
                semaphore.release();
                log.debug("[INGEST-CONTINUOUS] 信号量已释放 key={} permits={}", ctx.getSemKey(), ctx.getPermits());
            } catch (Exception e) {
                log.warn("[INGEST-CONTINUOUS] 释放信号量失败", e);
            }
        }
    }

    /**
     * 完成单次采集的收尾工作
     * @param ctx
     */
    private void finalizeSingleSync(HotelIdsIngestContext ctx) {
        if (ctx == null) return;
        
        // 更新同步日志
        if (ctx.getSyncLogId() != null) {
            syncLogRepository.findById(ctx.getSyncLogId()).ifPresent(sl -> {
                log.info("[INGEST] 任务收尾 traceId={} syncLogId={} totalIds={} totalDetails={} status(before)={}",
                        ctx.getTraceId(), sl.getId(), sl.getTotalIds(), sl.getTotalDetails(), sl.getStatus());
                OffsetDateTime finishedAt = OffsetDateTime.now();
                sl.setFinishedAt(finishedAt);
                if (sl.getStartedAt() != null) {
                    // 使用 Duration 计算时间差，避免时区问题
                    long secs = Duration.between(sl.getStartedAt(), finishedAt).getSeconds();
                    sl.setCostSeconds((int) secs);
                }
                if (sl.getFailCount() != null && sl.getFailCount() > 0) {
                    sl.setStatus("PARTIAL");
                } else if (sl.getStatus() == null || sl.getStatus().equals("RUNNING")) {
                    sl.setStatus("SUCCESS");
                }
                syncLogRepository.save(sl);
                log.info("[INGEST] 任务完成 traceId={} syncLogId={} status={} costSeconds={}", 
                        sl.getTraceId(), sl.getId(), sl.getStatus(), sl.getCostSeconds());
            });
        }

        // 释放信号量
        if (ctx.isSemAcquired() && ctx.getSemKey() != null) {
            try {
                var semaphore = semaphoreManager.getSemaphore(ctx.getSemKey(), ctx.getPermits());
                semaphore.release();
                log.debug("[INGEST] 信号量已释放 key={} permits={}", ctx.getSemKey(), ctx.getPermits());
            } catch (Exception e) {
                log.warn("[INGEST] 释放信号量失败", e);
            }
        }
    }

    /**
     * 根据 Exchange 属性判断失败阶段
     */
    private String determineFailureStage(Exchange exchange) {
        // 根据上下文判断失败阶段
        Boolean isFetchingIds = exchange.getProperty("isFetchingIds", Boolean.class);
        Boolean isFetchingDetails = exchange.getProperty("isFetchingDetails", Boolean.class);
        Boolean isSinking = exchange.getProperty("isSinking", Boolean.class);
        
        if (Boolean.TRUE.equals(isSinking)) {
            return "SINK";
        } else if (Boolean.TRUE.equals(isFetchingDetails)) {
            return "DETAIL_FETCH";
        } else if (Boolean.TRUE.equals(isFetchingIds)) {
            return "IDS_FETCH";
        }
        return "UNKNOWN";
    }

    /**
     * 截断错误消息，避免过长
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) return null;
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength - 3) + "...";
    }

}
