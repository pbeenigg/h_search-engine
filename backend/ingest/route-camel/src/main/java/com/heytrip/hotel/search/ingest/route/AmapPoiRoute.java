package com.heytrip.hotel.search.ingest.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.domain.entity.AmapCitycode;
import com.heytrip.hotel.search.domain.entity.AmapPoi;
import com.heytrip.hotel.search.domain.entity.AmapPoitype;
import com.heytrip.hotel.search.domain.entity.JobSchedule;
import com.heytrip.hotel.search.domain.entity.MapDataSyncLog;
import com.heytrip.hotel.search.domain.repository.*;
import com.heytrip.hotel.search.infra.config.AmapConfig;
import com.heytrip.hotel.search.infra.redis.RedisStreamPublisher;
import com.heytrip.hotel.search.infra.search.AmapPoiIndexService;
import com.heytrip.hotel.search.ingest.dto.*;
import com.heytrip.hotel.search.ingest.exception.TaskStoppedException;
import com.heytrip.hotel.search.ingest.model.AmapCollectMetric;
import com.heytrip.hotel.search.ingest.model.AmapPoiContext;
import com.heytrip.hotel.search.ingest.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 高德POI采集主路由
 * 架构：
 * - AmapPoiRoute (主采集路由)
 *   ├── AmapApiRoute (API调用路由，负责HTTP请求和重试)
 *   ├── AmapDataCleanRoute (数据清洗路由，负责数据校验和转换)
 *   ├── AmapDataPersistRoute (数据持久化路由，负责入库)
 *   └── AmapEsIndexRoute (ES索引路由，负责索引创建)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmapPoiRoute extends RouteBuilder {

    private static final String JOB_CODE = "AMAP_POI_COLLECT";
    private static final String REDIS_STREAM = "amap:poi:events";
    
    private final AmapConfig amapConfig;
    private final AmapApiKeyManager apiKeyManager;
    private final AmapDataValidator dataValidator;
    private final AmapPoiSinkService poiSinkService;
    private final AmapCitycodeRepository citycodeRepository;
    private final AmapPoitypeRepository poitypeRepository;
    private final MapDataSyncLogRepository syncLogRepository;
    private final RedisStreamPublisher redisStreamPublisher;
    private final ObjectMapper objectMapper;
    private final AmapCachedDataService cachedDataService;
    private final AmapCollectTaskManager taskManager;
    private final JobScheduleRepository jobScheduleRepository;
    private final AmapPoiRepository amapPoiRepository;
    private final AmapPoiIndexService amapPoiIndexService;
    private final AmapCollectMetricService metricService;

    @Override
    public void configure() {

        /**
         * 采集流程（基于Redis指标）：
         * 
         * 1. 检查Redis中是否已有指标
         *    ├─ 不存在 → 初始化全量指标（城市×POI类型）
         *    └─ 已存在 → 断点续采，只处理PENDING状态
         * 
         * 2. 循环批量获取待采集指标（每批10个）
         *    └─ 如果没有待采集指标 → 结束采集
         * 
         * 3. 处理每个指标
         *    ├─ 标记为PROCESSING
         *    ├─ 分页采集POI数据
         *    │  ├─ API调用
         *    │  ├─ 数据清洗
         *    │  └─ 数据持久化
         *    └─ 标记为COMPLETED（记录采集数量）
         * 
         * 4. 所有指标完成 → 任务结束
         * 
         * 示例：北京市 (110000) - 餐饮服务 (050000)
         * metricKey: "110000_050000"
         * status: PENDING → PROCESSING → COMPLETED
         * collectedCount: 1234
         */

        // ========== 任务停止异常处理（优先级最高）==========
        onException(TaskStoppedException.class)
                .handled(true)
                .stop()  // 立即停止路由，不继续处理后续元素
                .process(exchange -> {
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String errorMsg = ex != null ? ex.getMessage() : "任务已停止";
                    
                    log.warn("[AmapPoiRoute] 任务停止异常: {}", errorMsg);
                    
                    // 基于Redis指标的断点续采，不需要额外保存索引进度
                    // 每个指标的状态已实时保存在Redis中
                    
                    // 完成同步日志
                    if (ctx != null && ctx.getSyncLogId() != null) {
                        finalizeSyncLog(ctx.getSyncLogId(), "STOPPED", errorMsg);
                    }
                });
        
        // ========== 全局异常处理 ==========
        onException(Exception.class)
                .handled(true)
                .logExhausted(true)
                .process(exchange -> {
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String errorMsg = ex != null ? ex.getMessage() : "未知错误";
                    
                    log.error("[AmapPoiRoute] 路由异常: {}", errorMsg, ex);
                    
                    // 基于Redis指标的断点续采，不需要额外保存索引进度
                    // 每个指标的状态已实时保存在Redis中
                    
                    // 检查是否是API Key耗尽导致的异常
                    if (errorMsg != null && errorMsg.contains("没有可用的API Key")) {
                        log.warn("[AmapPoiRoute] 所有API Key已耗尽，停止采集任务");
                        
                        // 停止任务
                        try {
                            taskManager.stopTask("API Key配额耗尽，自动停止");
                            log.info("[AmapPoiRoute] 已自动停止采集任务");
                        } catch (Exception stopEx) {
                            log.error("[AmapPoiRoute] 停止任务失败", stopEx);
                        }
                    }
                    
                    // 完成同步日志
                    if (ctx != null && ctx.getSyncLogId() != null) {
                        finalizeSyncLog(ctx.getSyncLogId(), "FAILED", errorMsg);
                    }
                });

        // ========== 主路由：定时触发（从JobSchedule表读取cron配置） ==========
        String poiCollectCron = jobScheduleRepository.findByJobCode(JOB_CODE)
                .filter(JobSchedule::isEnabled)
                .map(JobSchedule::getCronExpr)
                .orElse("0 0 0 1 * ?"); // 默认：每月1号0点执行
        
        from("quartz://amap/poiCollect?cron=" + poiCollectCron)
                .routeId("route-amap-poi-collect-scheduled")
                .log("[AmapPoiRoute] 定时触发POI采集")
                .to("direct:amap:poi:collect");

        // ========== 主路由：手动触发 ==========
        from("direct:amap:poi:collect:manual")
                .routeId("route-amap-poi-collect-manual")
                .log("[AmapPoiRoute] 手动触发POI采集")
                .to("direct:amap:poi:collect");

        // ========== 主路由：核心采集流程 ==========
        from("direct:amap:poi:collect")
                .routeId("route-amap-poi-collect-core")
                .process(exchange -> {
                    // 1. 初始化上下文
                    AmapPoiContext ctx = initializeContext();
                    exchange.setProperty("poiContext", ctx);
                    
                    log.info("[AmapPoiRoute] ========== POI采集开始 ==========");
                    log.info("[AmapPoiRoute] traceId={}, batchId={}, syncLogId={}", 
                            ctx.getTraceId(), ctx.getBatchId(), ctx.getSyncLogId());
                })
                .process(exchange -> {
                    // 2. 初始化采集指标（如果不存在）
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    String batchId = ctx.getBatchId();
                    
                    // 检查是否已存在指标
                    long existingMetrics = metricService.getTotalMetrics(batchId);
                    
                    if (existingMetrics == 0) {
                        // 首次采集，需要初始化指标
                        List<AmapCitycode> cities = getCitiesToCollect();
                        List<AmapPoitype> types = getPoiTypesToCollect();
                        
                        log.info("[AmapPoiRoute] ===== 首次采集，初始化指标 =====");
                        log.info("[AmapPoiRoute] 城市数: {}, 类型数: {}", cities.size(), types.size());
                        
                        metricService.initializeMetrics(batchId, cities, types);
                        existingMetrics = metricService.getTotalMetrics(batchId);
                    } else {
                        // 断点续采
                        long completedMetrics = metricService.getCompletedMetrics(batchId);
                        log.info("[AmapPoiRoute] ===== 发现未完成的采集指标，断点续采 =====");
                        log.info("[AmapPoiRoute] 已完成进度: {}/{} ({:.2f}%)", 
                                completedMetrics, existingMetrics, 
                                (double) completedMetrics / existingMetrics * 100);
                    }
                    
                    // 启动任务管理
                    taskManager.startTask(batchId, ctx.getTraceId(), existingMetrics);
                    
                    exchange.setProperty("totalMetrics", existingMetrics);
                    exchange.setProperty("completedMetrics", metricService.getCompletedMetrics(batchId));
                    
                    log.info("[AmapPoiRoute] 总指标数: {}", existingMetrics);
                })
                .loopDoWhile(simple("true"))
                    .process(exchange -> {
                        // 3. 批量获取待采集指标
                        AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                        
                        // 每次获取一批待采集的指标
                        List<AmapCollectMetric> pendingMetrics = metricService.getPendingMetrics(
                                ctx.getBatchId(), 
                                50  // 每次批量处理50个指标
                        );
                        
                        if (pendingMetrics.isEmpty()) {
                            // 没有待采集指标了，结束循环
                            log.info("[AmapPoiRoute] ===== 所有指标采集完成 =====");
                            exchange.setProperty("continueLoop", false);
                            exchange.setProperty("pendingMetrics", new ArrayList<>());
                        } else {
                            exchange.setProperty("continueLoop", true);
                            exchange.setProperty("pendingMetrics", pendingMetrics);
                            log.info("[AmapPoiRoute] 获取到{}个待采集指标", pendingMetrics.size());
                        }
                    })
                    .choice()
                        .when(simple("${exchangeProperty.continueLoop} == false"))
                            .stop()
                        .otherwise()
                            .split(simple("${exchangeProperty.pendingMetrics}"))
                        .stopOnException()
                        .process(exchange -> {
                            // 4. 检查任务状态（暂停/停止）
                            if (taskManager.shouldPause()) {
                                // 等待恢复
                                while (taskManager.shouldPause()) {
                                    log.info("[AmapPoiRoute] 任务已暂停，等待恢复...");
                                    Thread.sleep(5000);
                                }
                                log.info("[AmapPoiRoute] 任务已恢复，继续采集");
                            }
                            
                            if (taskManager.shouldStop()) {
                                log.warn("[AmapPoiRoute] 检测到停止信号，立即退出采集循环");
                                throw new TaskStoppedException("任务已停止");
                            }
                            
                            // 5. 处理当前指标
                            AmapCollectMetric metric = exchange.getIn().getBody(AmapCollectMetric.class);
                            AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                            
                            // 标记指标为处理中
                            metricService.markAsProcessing(ctx.getBatchId(), metric.getMetricKey());
                            
                            // 更新上下文
                            ctx.setCurrentCityCode(metric.getCityCode());
                            ctx.setCurrentCityName(metric.getCityName());
                            ctx.setCurrentTypeCode(metric.getTypeCode());
                            ctx.setMetricKey(metric.getMetricKey());
                            ctx.setCurrentPage(1);
                            ctx.setCurrentMetricCollected(0L);  // 初始化当前指标采集数为0
                            
                            // 初始化分页标识
                            exchange.setProperty("hasMoreData", true);
                            exchange.setProperty("currentMetric", metric);
                            
                            log.info("[AmapPoiRoute] ===== 开始采集：{} ({}) - {} {} =====", 
                                    metric.getCityName(), metric.getCityCode(), 
                                    metric.getTypeCode(), metric.getTypeName());
                        })
                    .loopDoWhile(simple("${exchangeProperty.hasMoreData} == true"))
                        .process(exchange -> {
                            // 在分页循环内检查任务状态
                            while (taskManager.shouldPause()) {
                                log.info("[AmapPoiRoute] 任务已暂停，等待恢复...");
                                Thread.sleep(5000);
                            }
                            
                            if (taskManager.shouldStop()) {
                                log.warn("[AmapPoiRoute] 任务已停止，退出分页采集");
                                exchange.setProperty("hasMoreData", false); // 强制退出循环
                                throw new RuntimeException("任务已停止");
                            }
                        })
                        .to("direct:amap:poi:api:call")        // 子路由1：API调用
                        .to("direct:amap:poi:data:clean")      // 子路由2：数据清洗
                        .to("direct:amap:poi:data:persist")    // 子路由3：数据持久化
                        .process(exchange -> {
                            // 判断是否还有下一页
                            AmapPoiResponse response = exchange.getIn().getBody(AmapPoiResponse.class);
                            AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                            Boolean qpsRetry = exchange.getProperty("qpsRetry", Boolean.class);
                            
                            // 如果是QPS重试，保持hasMoreData=true，重试当前页
                            if (Boolean.TRUE.equals(qpsRetry)) {
                                exchange.setProperty("hasMoreData", true);
                                exchange.removeProperty("qpsRetry");
                                log.debug("[AmapPoiRoute] QPS重试完成，继续请求当前页: currentPage={}", ctx.getCurrentPage());
                                return;
                            }
                            
                            boolean hasMore = response != null && response.hasData() && 
                                    response.getPois().size() >= ctx.getPageSize();
                            exchange.setProperty("hasMoreData", hasMore);
                            
                            if (hasMore) {
                                ctx.setCurrentPage(ctx.getCurrentPage() + 1);
                                // 页间延迟
                                Thread.sleep(amapConfig.getPerformance().getPageDelayMs());
                            }
                            
                            log.debug("[AmapPoiRoute] 分页检查: hasMore={}, currentPage={}, size={}", 
                                    hasMore, ctx.getCurrentPage(), 
                                    response != null && response.hasData() ? response.getPois().size() : 0);
                        })
                    .end()
                        .process(exchange -> {
                            // 7. 标记指标为已完成
                            AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                            AmapCollectMetric metric = exchange.getProperty("currentMetric", AmapCollectMetric.class);
                            
                            // 获取当前指标采集的POI数量
                            Long poiCount = ctx.getCurrentMetricCollected() != null ? ctx.getCurrentMetricCollected() : 0L;
                            
                            // 标记指标为已完成
                            metricService.markAsCompleted(ctx.getBatchId(), metric.getMetricKey(), poiCount);
                            
                            // 更新任务进度
                            Long completedMetrics = metricService.getCompletedMetrics(ctx.getBatchId());
                            Long totalMetrics = exchange.getProperty("totalMetrics", Long.class);
                            
                            // 更新任务管理器的进度信息
                            taskManager.updateProgress(
                                    completedMetrics,
                                    ctx.getTotalCollected() != null ? ctx.getTotalCollected() : 0L,
                                    metric.getCityCode(),
                                    metric.getTypeCode()
                            );
                            
                            log.info("[AmapPoiRoute] 指标采集完成: {} ({}) - {}, 本次采集: {}条, 总进度: {}/{}", 
                                    metric.getCityName(), metric.getCityCode(), metric.getTypeCode(),
                                    poiCount, completedMetrics, totalMetrics);
                        })
                    .end()  // 关闭分页loopDoWhile
                    .end()  // 关闭split
                    .end()  // 关闭choice
                .end()  // 关闭外层loopDoWhile
                .process(exchange -> {
                    // 8. 最终完成处理
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    
                    // 获取指标统计
                    Map<String, Long> stats = metricService.getMetricStats(ctx.getBatchId());
                    
                    log.info("[AmapPoiRoute] ========== POI采集结束 ==========");
                    log.info("[AmapPoiRoute] 指标统计: 总数={}, 已完成={}, 失败={}, 处理中={}, 待采集={}",
                            stats.get("total"), stats.get("completed"), stats.get("failed"),
                            stats.get("processing"), stats.get("pending"));
                    
                    // 如果所有指标都完成了，清理进度和指标
                    if (stats.get("pending") == 0 && stats.get("processing") == 0) {
                        taskManager.completeTask();
                        // 可选：保留指标一段时间用于查询统计，或立即清理
                        // metricService.clearMetrics(ctx.getBatchId());
                    }
                    
                    // 完成同步日志
                    finalizeSyncLog(ctx.getSyncLogId(), "SUCCESS", "采集完成");
                });

        // ========== 子路由1：API调用路由 ==========
        from("direct:amap:poi:api:call")
                .routeId("route-amap-poi-api-call")
                .process(exchange -> {
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    
                    // 获取可用的API Key
                    String apiKey = apiKeyManager.getNextAvailableKey();
                    if (apiKey == null) {
                        throw new RuntimeException("没有可用的API Key");
                    }
                    ctx.setCurrentApiKey(apiKey);
                    
                    // 构建API请求URL
                    String url = buildApiUrl(ctx, apiKey);
                    exchange.getIn().setHeader(Exchange.HTTP_URI, url);
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
                    
                    log.debug("[AmapPoiRoute-API] 调用API: city={}, type={}, page={}", 
                            ctx.getCurrentCityName(), ctx.getCurrentTypeCode(), ctx.getCurrentPage());
                })
                .to("http://dummy")  // 使用Camel HTTP组件发起请求
                .process(exchange -> {
                    // 解析API响应
                    String responseBody = exchange.getIn().getBody(String.class);
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    
                    AmapPoiResponse response = objectMapper.readValue(responseBody, AmapPoiResponse.class);
                    
                    if (!response.isSuccess()) {
                        // API调用失败
                        String errorMsg = String.format("API返回错误: status=%s, info=%s, infocode=%s", 
                                response.getStatus(), response.getInfo(), response.getInfocode());
                        
                        // 10021: 并发QPS超限 - 等待3秒后重试
                        if ("10021".equals(response.getInfocode())) {
                            log.warn("[AmapPoiRoute-API] QPS超限，等待3秒后重试: {}", errorMsg);
                            Thread.sleep(3000);
                            
                            // 设置空响应，让loopDoWhile继续重试当前页
                            AmapPoiResponse retryResponse = new AmapPoiResponse();
                            retryResponse.setStatus("0");
                            retryResponse.setInfo("QPS_RETRY");
                            retryResponse.setInfocode("QPS_RETRY");
                            retryResponse.setPois(new ArrayList<>());
                            exchange.getIn().setBody(retryResponse);
                            exchange.setProperty("qpsRetry", true);
                            return;
                        }
                        
                        // 如果是配额相关错误，标记Key失败
                        // 10003: 访问已超出日访问量
                        // 10004: 单位时间内访问过于频繁
                        // 10044: 用户每日查询次数超限
                        if ("10003".equals(response.getInfocode()) || 
                            "10004".equals(response.getInfocode()) ||
                            "10044".equals(response.getInfocode())) {
                            apiKeyManager.markFailure(ctx.getCurrentApiKey(), "配额不足");
                        }
                        
                        // 停止任务
                        try {
                            taskManager.stopTask("API异常停止: " + errorMsg);
                            log.warn("[AmapPoiRoute-API] 因API异常已停止采集任务: {}", errorMsg);
                        } catch (Exception stopEx) {
                            log.error("[AmapPoiRoute-API] 停止任务失败", stopEx);
                        }
                        
                        throw new RuntimeException(errorMsg);
                    }
                    
                    // 标记Key使用成功
                    apiKeyManager.markSuccess(ctx.getCurrentApiKey());
                    
                    exchange.getIn().setBody(response);
                    log.debug("[AmapPoiRoute-API] API调用成功，返回{}条POI", 
                            response.hasData() ? response.getPois().size() : 0);
                });

        // ========== 子路由2：数据清洗路由 ==========
        from("direct:amap:poi:data:clean")
                .routeId("route-amap-poi-data-clean")
                .process(exchange -> {
                    AmapPoiResponse response = exchange.getIn().getBody(AmapPoiResponse.class);
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);
                    
                    if (!response.hasData()) {
                        exchange.setProperty("cleanedPois", new ArrayList<AmapPoi>());
                        return;
                    }
                    
                    List<AmapPoiItem> validItems = dataValidator.filterValidPois(response.getPois());
                    List<AmapPoi> cleanedPois = convertToEntities(validItems, ctx);
                    
                    exchange.setProperty("cleanedPois", cleanedPois);
                    log.debug("[AmapPoiRoute-Clean] 数据清洗完成，有效POI：{}", cleanedPois.size());
                });

        // ========== 子路由3：数据持久化路由 ==========
        from("direct:amap:poi:data:persist")
                .routeId("route-amap-poi-data-persist")
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    List<AmapPoi> pois = exchange.getProperty("cleanedPois", List.class);
                    AmapPoiContext ctx = exchange.getProperty("poiContext", AmapPoiContext.class);

                    log.info("[AmapPoiRoute-Persist] 准备入库POI数量：{}", pois.size());
                    
                    if (pois == null || pois.isEmpty()) {
                        return;
                    }
                    
                    // 批量入库（UPSERT）
                    long[] result = poiSinkService.upsertInBatches(pois, ctx.getDbCommitSize(), ctx.getSyncLogId());
                    
                    // 更新统计
                    ctx.setTotalCollected(ctx.getTotalCollected() + pois.size());
                    ctx.setSuccessCount(ctx.getSuccessCount() + result[0]);
                    ctx.setFailureCount(ctx.getFailureCount() + result[1]);
                    // 累加当前指标采集数
                    ctx.setCurrentMetricCollected(ctx.getCurrentMetricCollected() + pois.size());
                    
                    log.info("[AmapPoiRoute-Persist] 批量入库完成，成功：{}，失败：{}", result[0], result[1]);
                    
                    // ⚠️ 关键：立即索引到ES，确保数据库和ES实时同步
                    if (result[0] > 0) {
                        try {
                            long[] esResult = amapPoiIndexService.bulkIndex(pois);
                            log.info("[AmapPoiRoute-Persist] ES索引完成，成功：{}，失败：{}", esResult[0], esResult[1]);
                        } catch (Exception e) {
                            log.error("[AmapPoiRoute-Persist] ES索引失败，但数据库已保存: {}", e.getMessage(), e);
                            // 不抛异常，允许继续采集
                        }
                    }
                });

    }

    /**
     * 初始化采集上下文
     */
    private AmapPoiContext initializeContext() {
        // 使用年月作为batchId，确保同一个月内的多次触发都使用相同的batchId
        // 这样可以实现断点续采功能
        String yearMonth = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String batchId = "batch_" + yearMonth;
        
        // ES索引名保留精确时间戳，避免索引冲突
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String esIndexName = "hotels_amap_poi_" + timestamp;
        
        // 创建同步日志
        MapDataSyncLog syncLog = MapDataSyncLog.builder()
                .source("amap")
                .dataType("poi")
                .startTime(OffsetDateTime.now())
                .status("RUNNING")
                .totalCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .traceId("poi-" + System.currentTimeMillis())
                .build();
        syncLog = syncLogRepository.save(syncLog);
        
        return AmapPoiContext.builder()
                .traceId(syncLog.getTraceId())
                .syncLogId(syncLog.getId())
                .batchId(batchId)
                .esIndexName(esIndexName)
                .pageSize(amapConfig.getCollect().getPageSize())
                .cityLimit(amapConfig.getCollect().getCityLimit())
                .dbCommitSize(amapConfig.getCollect().getDbCommitSize())
                .httpTimeoutMs(amapConfig.getPerformance().getTimeoutMs())
                .totalCollected(0L)
                .successCount(0L)
                .failureCount(0L)
                .currentRetryCount(0)
                .maxRetryAttempts(amapConfig.getRetry().getMaxAttempts())
                .build();
    }


    /**
     * 获取需要采集的城市列表（使用缓存）
     * 只查询地级市及以上城市（排除国家级和省级）
     */
    private List<AmapCitycode> getCitiesToCollect() {
        List<String> enabledCities = amapConfig.getCollect().getEnabledCities();
        
        if (enabledCities == null || enabledCities.isEmpty()) {
            // 采集所有地级市及以上城市（从缓存获取）
            return cachedDataService.findAllPrefectureLevelCities();
        } else {
            // 采集指定城市（从缓存获取）
            return enabledCities.stream()
                    .map(cachedDataService::findCitycodesByCitycode)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取需要采集的POI类型列表（使用缓存）
     */
    private List<AmapPoitype> getPoiTypesToCollect() {
        List<String> enabledTypes = amapConfig.getCollect().getEnabledTypes();
        
        if (enabledTypes == null || enabledTypes.isEmpty()) {
            // 采集所有类型（从缓存获取）
            return cachedDataService.findAllPoitypes();
        } else {
            // 采集指定类型（从缓存获取）
            return enabledTypes.stream()
                    .map(cachedDataService::findPoitypeByTypecode)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 构建API请求URL
     */
    private String buildApiUrl(AmapPoiContext ctx, String apiKey) {
        StringBuilder url = new StringBuilder(amapConfig.getApi().getBaseUrl());
        url.append("/v5/place/text");
        url.append("?key=").append(apiKey);
        url.append("&types=").append(ctx.getCurrentTypeCode());
        url.append("&region=").append(ctx.getCurrentCityCode());
        url.append("&city_limit=").append(ctx.getCityLimit());
        url.append("&page_size=").append(ctx.getPageSize());
        url.append("&page_num=").append(ctx.getCurrentPage());
        url.append("&show_fields=").append(ctx.getShowFields());
        
        return url.toString();
    }

    /**
     * 转换API响应为实体对象（包含children扁平化）
     */
    private List<AmapPoi> convertToEntities(List<AmapPoiItem> items, AmapPoiContext ctx) {
        List<AmapPoi> entities = new ArrayList<>();
        
        for (AmapPoiItem item : items) {
            // 主POI
            AmapPoi poi = convertToEntity(item, ctx);
            entities.add(poi);
            
            // 子POI（扁平化处理）
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                for (AmapPoiItem.ChildPoi child : item.getChildren()) {
                    AmapPoi childPoi = convertChildToEntity(child, item, ctx);
                    entities.add(childPoi);
                }
            }
        }
        
        return entities;
    }

    /**
     * 转换单个POI项为实体
     */
    private AmapPoi convertToEntity(AmapPoiItem item, AmapPoiContext ctx) {
        BigDecimal[] coords = dataValidator.parseLocation(item.getLocation());
        
        AmapPoi poi = AmapPoi.builder()
                .id(item.getId())
                .name(item.getName())
                .type(item.getType())
                .typecode(item.getTypecode())
                .address(item.getAddress())
                .location(item.getLocation())
                .longitude(coords != null ? coords[0] : null)
                .latitude(coords != null ? coords[1] : null)
                .pcode(item.getPcode())
                .pname(item.getPname())
                .citycode(item.getCitycode())
                .cityname(item.getCityname())
                .adcode(item.getAdcode())
                .adname(item.getAdname())
                .parent(item.getParent())
                .distance(item.getDistance())
                .dataVersion("1.0")
                .sourceBatch(ctx.getBatchId())
                .isDeleted(false)
                .build();
        
        // 计算并设置MD5哈希值（用于变更检测）
        poi.setDataHash(AmapPoiChangeDetector.calculatePoiHash(poi));
        
        return poi;
    }

    /**
     * 转换子POI为实体
     */
    private AmapPoi convertChildToEntity(AmapPoiItem.ChildPoi child, AmapPoiItem parent, AmapPoiContext ctx) {
        BigDecimal[] coords = dataValidator.parseLocation(child.getLocation());
        
        AmapPoi poi = AmapPoi.builder()
                .id(child.getId())
                .name(child.getName())
                .type(child.getSubtype())
                .typecode(child.getTypecode())
                .address(child.getAddress())
                .location(child.getLocation())
                .longitude(coords != null ? coords[0] : null)
                .latitude(coords != null ? coords[1] : null)
                .pcode(parent.getPcode())
                .pname(parent.getPname())
                .citycode(parent.getCitycode())
                .cityname(parent.getCityname())
                .adcode(parent.getAdcode())
                .adname(parent.getAdname())
                .parent(parent.getId())  // 父POI的ID
                .dataVersion("1.0")
                .sourceBatch(ctx.getBatchId())
                .isDeleted(false)
                .build();
        
        // 计算并设置MD5哈希值（用于变更检测）
        poi.setDataHash(AmapPoiChangeDetector.calculatePoiHash(poi));
        
        return poi;
    }

    /**
     * 构建POI采集完成事件
     */
    private AmapPoiCollectedEvent buildCollectedEvent(AmapPoiContext ctx) {
        return AmapPoiCollectedEvent.builder()
                .eventType("AMAP_POI_COLLECTED")
                .batchId(ctx.getBatchId())
                .indexName(ctx.getEsIndexName())
                .timestamp(OffsetDateTime.now().toString())
                .traceId(ctx.getTraceId())
                .statistics(AmapPoiCollectedEvent.Statistics.builder()
                        .totalCount(ctx.getTotalCollected())
                        .successCount(ctx.getSuccessCount())
                        .failCount(ctx.getFailureCount())
                        .build())
                .dataLocation(AmapPoiCollectedEvent.DataLocation.builder()
                        .database("hotel_search")
                        .table("amap_poi")
                        .batchColumn("source_batch")
                        .batchValue(ctx.getBatchId())
                        .build())
                .build();
    }

    /**
     * 完成同步日志
     */
    private void finalizeSyncLog(Long syncLogId, String status, String message) {
        try {
            syncLogRepository.findById(syncLogId).ifPresent(log -> {
                OffsetDateTime endTime = OffsetDateTime.now();
                log.setEndTime(endTime);
                log.setStatus(status);
                
                if (log.getStartTime() != null) {
                    long seconds = Duration.between(log.getStartTime(), endTime).getSeconds();
                    log.setDuration((int) seconds);
                }
                
                if ("FAILED".equals(status)) {
                    log.setErrorMessage(message);
                }
                
                syncLogRepository.save(log);
            });
        } catch (Exception e) {
            log.error("[AmapPoiRoute] 更新同步日志失败: syncLogId={}", syncLogId, e);
        }
    }
}
