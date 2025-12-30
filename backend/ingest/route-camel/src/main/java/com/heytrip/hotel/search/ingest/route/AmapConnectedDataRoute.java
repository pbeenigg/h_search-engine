package com.heytrip.hotel.search.ingest.route;

import com.heytrip.hotel.search.domain.entity.AmapCitycode;
import com.heytrip.hotel.search.domain.entity.AmapPoitype;
import com.heytrip.hotel.search.domain.entity.JobSchedule;
import com.heytrip.hotel.search.domain.entity.MapDataSyncLog;
import com.heytrip.hotel.search.domain.repository.AmapCitycodeRepository;
import com.heytrip.hotel.search.domain.repository.AmapPoitypeRepository;
import com.heytrip.hotel.search.domain.repository.JobScheduleRepository;
import com.heytrip.hotel.search.domain.repository.MapDataSyncLogRepository;
import com.heytrip.hotel.search.infra.config.AmapConfig;
import com.heytrip.hotel.search.ingest.service.AmapCachedDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德关联数据路由
 * 职责：
 * - 读取城市代码CSV文件并导入数据库
 * - 读取POI类型CSV文件并导入数据库
 * - 支持定时触发和手动触发
 * - 记录详细的同步日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmapConnectedDataRoute extends RouteBuilder {

    private static final String JOB_CODE_CITYCODE = "AMAP_CITYCODE_SYNC";
    private static final String JOB_CODE_POITYPE = "AMAP_POITYPE_SYNC";
    
    private static final String CITYCODE_CSV_PATH = "poi/amap_adcode_citycode.csv";
    private static final String POITYPE_CSV_PATH = "poi/amap_poi_type.csv";

    private final AmapConfig amapConfig;
    private final AmapCitycodeRepository citycodeRepository;
    private final AmapPoitypeRepository poitypeRepository;
    private final MapDataSyncLogRepository syncLogRepository;
    private final AmapCachedDataService cachedDataService;
    private final JobScheduleRepository jobScheduleRepository;

    @Override
    public void configure() {
        
        // ========== 异常处理 ==========
        onException(Exception.class)
                .handled(true)
                .logExhausted(true)
                .process(exchange -> {
                    Long syncLogId = exchange.getProperty("syncLogId", Long.class);
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    
                    if (syncLogId != null) {
                        finalizeSyncLog(syncLogId, "FAILED", ex != null ? ex.getMessage() : "未知错误");
                    }
                    
                    log.error("[AmapConnectedData] 路由异常: {}", ex != null ? ex.getMessage() : "未知错误", ex);
                });

        // ========== 城市代码同步路由（定时触发，从JobSchedule表读取cron配置） ==========
        String citycodeCron = jobScheduleRepository.findByJobCode(JOB_CODE_CITYCODE)
                .filter(JobSchedule::isEnabled)
                .map(JobSchedule::getCronExpr)
                .orElse("0 0 0 1 */3 ?"); // 默认：每3个月1号0点执行
        
        from("quartz://amap/citycodeSync?cron=" + citycodeCron)
                .routeId("route-amap-citycode-sync-scheduled")
                .log("[AmapConnectedData] 定时触发城市代码同步")
                .to("direct:amap:citycode:sync");

        // ========== 城市代码同步路由（手动触发） ==========
        from("direct:amap:citycode:sync:manual")
                .routeId("route-amap-citycode-sync-manual")
                .log("[AmapConnectedData] 手动触发城市代码同步")
                .to("direct:amap:citycode:sync");

        // ========== 城市代码同步核心路由 ==========
        from("direct:amap:citycode:sync")
                .routeId("route-amap-citycode-sync-core")
                .process(exchange -> {
                    OffsetDateTime startTime = OffsetDateTime.now();
                    
                    // 创建同步日志
                    MapDataSyncLog syncLog = MapDataSyncLog.builder()
                            .source("amap")
                            .dataType("citycode")
                            .startTime(startTime)
                            .status("RUNNING")
                            .totalCount(0L)
                            .successCount(0L)
                            .failureCount(0L)
                            .traceId("citycode-" + System.currentTimeMillis())
                            .build();
                    syncLog = syncLogRepository.save(syncLog);
                    
                    exchange.setProperty("syncLogId", syncLog.getId());
                    exchange.setProperty("startTime", startTime);
                    
                    log.info("[AmapConnectedData] 开始同步城市代码数据: syncLogId={}, traceId={}", 
                            syncLog.getId(), syncLog.getTraceId());
                })
                .process(exchange -> {
                    Long syncLogId = exchange.getProperty("syncLogId", Long.class);
                    
                    try {
                        // 读取并解析CSV文件
                        List<AmapCitycode> citycodes = parseCitycodeCSV();
                        
                        // 批量保存（支持增量更新）
                        long[] result = saveCitycodesInBatches(citycodes, syncLogId);
                        
                        exchange.setProperty("successCount", result[0]);
                        exchange.setProperty("failureCount", result[1]);
                        exchange.setProperty("totalCount", result[0] + result[1]);
                        
                    } catch (Exception e) {
                        log.error("[AmapConnectedData] 城市代码同步失败", e);
                        throw e;
                    }
                })
                .process(exchange -> {
                    // 完成同步，更新日志
                    Long syncLogId = exchange.getProperty("syncLogId", Long.class);
                    Long successCount = exchange.getProperty("successCount", Long.class);
                    Long failureCount = exchange.getProperty("failureCount", Long.class);
                    Long totalCount = exchange.getProperty("totalCount", Long.class);
                    
                    finalizeSyncLog(syncLogId, "SUCCESS", 
                            String.format("同步完成，总数：%d，成功：%d，失败：%d", 
                                    totalCount, successCount, failureCount));
                    
                    // 清除城市代码缓存
                    cachedDataService.evictAllCitycodeCaches();
                    log.info("[AmapConnectedData] 已清除城市代码缓存");
                    
                    log.info("[AmapConnectedData] 城市代码同步完成: syncLogId={}, success={}, failure={}", 
                            syncLogId, successCount, failureCount);
                });

        // ========== POI类型同步路由（定时触发，从JobSchedule表读取cron配置） ==========
        String poitypeCron = jobScheduleRepository.findByJobCode(JOB_CODE_POITYPE)
                .filter(JobSchedule::isEnabled)
                .map(JobSchedule::getCronExpr)
                .orElse("0 0 0 1 */3 ?"); // 默认：每3个月1号0点执行
        
        from("quartz://amap/poitypeSync?cron=" + poitypeCron)
                .routeId("route-amap-poitype-sync-scheduled")
                .log("[AmapConnectedData] 定时触发POI类型同步")
                .to("direct:amap:poitype:sync");

        // ========== POI类型同步路由（手动触发） ==========
        from("direct:amap:poitype:sync:manual")
                .routeId("route-amap-poitype-sync-manual")
                .log("[AmapConnectedData] 手动触发POI类型同步")
                .to("direct:amap:poitype:sync");

        // ========== POI类型同步核心路由 ==========
        from("direct:amap:poitype:sync")
                .routeId("route-amap-poitype-sync-core")
                .process(exchange -> {
                    OffsetDateTime startTime = OffsetDateTime.now();
                    
                    // 创建同步日志
                    MapDataSyncLog syncLog = MapDataSyncLog.builder()
                            .source("amap")
                            .dataType("poitype")
                            .startTime(startTime)
                            .status("RUNNING")
                            .totalCount(0L)
                            .successCount(0L)
                            .failureCount(0L)
                            .traceId("poitype-" + System.currentTimeMillis())
                            .build();
                    syncLog = syncLogRepository.save(syncLog);
                    
                    exchange.setProperty("syncLogId", syncLog.getId());
                    exchange.setProperty("startTime", startTime);
                    
                    log.info("[AmapConnectedData] 开始同步POI类型数据: syncLogId={}, traceId={}", 
                            syncLog.getId(), syncLog.getTraceId());
                })
                .process(exchange -> {
                    Long syncLogId = exchange.getProperty("syncLogId", Long.class);
                    
                    try {
                        // 读取并解析CSV文件
                        List<AmapPoitype> poitypes = parsePoitypeCSV();
                        
                        // 批量保存（支持增量更新）
                        long[] result = savePoitypesInBatches(poitypes, syncLogId);
                        
                        exchange.setProperty("successCount", result[0]);
                        exchange.setProperty("failureCount", result[1]);
                        exchange.setProperty("totalCount", result[0] + result[1]);
                        
                    } catch (Exception e) {
                        log.error("[AmapConnectedData] POI类型同步失败", e);
                        throw e;
                    }
                })
                .process(exchange -> {
                    // 完成同步，更新日志
                    Long syncLogId = exchange.getProperty("syncLogId", Long.class);
                    Long successCount = exchange.getProperty("successCount", Long.class);
                    Long failureCount = exchange.getProperty("failureCount", Long.class);
                    Long totalCount = exchange.getProperty("totalCount", Long.class);
                    
                    finalizeSyncLog(syncLogId, "SUCCESS", 
                            String.format("同步完成，总数：%d，成功：%d，失败：%d", 
                                    totalCount, successCount, failureCount));
                    
                    // 清除POI类型缓存
                    cachedDataService.evictAllPoitypeCaches();
                    log.info("[AmapConnectedData] 已清除POI类型缓存");
                    
                    log.info("[AmapConnectedData] POI类型同步完成: syncLogId={}, success={}, failure={}", 
                            syncLogId, successCount, failureCount);
                });
    }

    /**
     * 解析城市代码CSV文件
     *
     * @return 城市代码列表
     */
    private List<AmapCitycode> parseCitycodeCSV() throws Exception {
        List<AmapCitycode> citycodes = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(CITYCODE_CSV_PATH);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            // 跳过表头
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("城市代码CSV文件为空");
            }
            
            log.debug("[AmapConnectedData] CSV表头: {}", headerLine);
            
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    AmapCitycode citycode = parseCitycodeLine(line);
                    if (citycode != null) {
                        citycodes.add(citycode);
                    }
                } catch (Exception e) {
                    log.warn("[AmapConnectedData] 解析城市代码CSV第{}行失败: line={}, error={}", 
                            lineNumber, line, e.getMessage());
                }
            }
        }
        
        log.info("[AmapConnectedData] 解析城市代码CSV完成，共{}条记录", citycodes.size());
        return citycodes;
    }

    /**
     * 解析城市代码CSV行
     * 格式：中文名,adcode,citycode
     *
     * @param line CSV行
     * @return 城市代码实体
     */
    private AmapCitycode parseCitycodeLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = line.split(",");
        if (parts.length < 2) {
            throw new RuntimeException("CSV格式错误，字段数不足: " + line);
        }
        
        String name = parts[0].trim();
        String adcode = parts[1].trim();
        String citycode = parts.length > 2 ? parts[2].trim() : null;
        
        // 处理citycode为"\N"的情况（表示NULL）
        if ("\\N".equals(citycode) || "NULL".equalsIgnoreCase(citycode)) {
            citycode = null;
        }
        
        return AmapCitycode.builder()
                .name(name)
                .adcode(adcode)
                .citycode(citycode)
                .build();
    }

    /**
     * 批量保存城市代码（支持增量更新）
     *
     * @param citycodes 城市代码列表
     * @param syncLogId 同步日志ID
     * @return [成功数, 失败数]
     */
    @Transactional
    protected long[] saveCitycodesInBatches(List<AmapCitycode> citycodes, Long syncLogId) {
        long success = 0;
        long failure = 0;
        
        for (AmapCitycode citycode : citycodes) {
            try {
                // 检查是否已存在（根据adcode）
                citycodeRepository.findByAdcode(citycode.getAdcode())
                        .ifPresentOrElse(
                                existing -> {
                                    // 更新现有记录
                                    existing.setName(citycode.getName());
                                    existing.setCitycode(citycode.getCitycode());
                                    citycodeRepository.save(existing);
                                },
                                () -> {
                                    // 新增记录
                                    citycodeRepository.save(citycode);
                                }
                        );
                success++;
            } catch (Exception e) {
                failure++;
                log.warn("[AmapConnectedData] 保存城市代码失败: adcode={}, error={}", 
                        citycode.getAdcode(), e.getMessage());
            }
        }
        
        return new long[]{success, failure};
    }

    /**
     * 解析POI类型CSV文件
     *
     * @return POI类型列表
     */
    private List<AmapPoitype> parsePoitypeCSV() throws Exception {
        List<AmapPoitype> poitypes = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(POITYPE_CSV_PATH);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            // 跳过表头
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("POI类型CSV文件为空");
            }
            
            log.debug("[AmapConnectedData] CSV表头: {}", headerLine);
            
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    AmapPoitype poitype = parsePoitypeLine(line);
                    if (poitype != null) {
                        poitypes.add(poitype);
                    }
                } catch (Exception e) {
                    log.warn("[AmapConnectedData] 解析POI类型CSV第{}行失败: line={}, error={}", 
                            lineNumber, line, e.getMessage());
                }
            }
        }
        
        log.info("[AmapConnectedData] 解析POI类型CSV完成，共{}条记录", poitypes.size());
        return poitypes;
    }

    /**
     * 解析POI类型CSV行
     * 格式：序号,NEW_TYPE,大类,中类,小类,Big Category,Mid Category,Sub Category
     *
     * @param line CSV行
     * @return POI类型实体
     */
    private AmapPoitype parsePoitypeLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = line.split(",");
        if (parts.length < 8) {
            throw new RuntimeException("CSV格式错误，字段数不足: " + line);
        }
        
        String typecode = parts[1].trim();
        String bigCategoryCn = parts[2].trim();
        String midCategoryCn = parts[3].trim();
        String subCategoryCn = parts[4].trim();
        String bigCategoryEn = parts[5].trim();
        String midCategoryEn = parts[6].trim();
        String subCategoryEn = parts[7].trim();
        
        return AmapPoitype.builder()
                .typecode(typecode)
                .bigCategoryCn(bigCategoryCn)
                .midCategoryCn(midCategoryCn)
                .subCategoryCn(subCategoryCn)
                .bigCategoryEn(bigCategoryEn)
                .midCategoryEn(midCategoryEn)
                .subCategoryEn(subCategoryEn)
                .build();
    }

    /**
     * 批量保存POI类型（支持增量更新）
     *
     * @param poitypes  POI类型列表
     * @param syncLogId 同步日志ID
     * @return [成功数, 失败数]
     */
    @Transactional
    protected long[] savePoitypesInBatches(List<AmapPoitype> poitypes, Long syncLogId) {
        long success = 0;
        long failure = 0;
        
        for (AmapPoitype poitype : poitypes) {
            try {
                // 检查是否已存在（根据typecode）
                poitypeRepository.findByTypecode(poitype.getTypecode())
                        .ifPresentOrElse(
                                existing -> {
                                    // 更新现有记录
                                    existing.setBigCategoryCn(poitype.getBigCategoryCn());
                                    existing.setMidCategoryCn(poitype.getMidCategoryCn());
                                    existing.setSubCategoryCn(poitype.getSubCategoryCn());
                                    existing.setBigCategoryEn(poitype.getBigCategoryEn());
                                    existing.setMidCategoryEn(poitype.getMidCategoryEn());
                                    existing.setSubCategoryEn(poitype.getSubCategoryEn());
                                    poitypeRepository.save(existing);
                                },
                                () -> {
                                    // 新增记录
                                    poitypeRepository.save(poitype);
                                }
                        );
                success++;
            } catch (Exception e) {
                failure++;
                log.warn("[AmapConnectedData] 保存POI类型失败: typecode={}, error={}", 
                        poitype.getTypecode(), e.getMessage());
            }
        }
        
        return new long[]{success, failure};
    }

    /**
     * 完成同步日志
     *
     * @param syncLogId 同步日志ID
     * @param status    状态
     * @param message   消息
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
            log.error("[AmapConnectedData] 更新同步日志失败: syncLogId={}", syncLogId, e);
        }
    }
}
