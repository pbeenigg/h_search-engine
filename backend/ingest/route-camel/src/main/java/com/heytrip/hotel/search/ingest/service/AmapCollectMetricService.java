package com.heytrip.hotel.search.ingest.service;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.search.domain.entity.AmapCitycode;
import com.heytrip.hotel.search.domain.entity.AmapPoitype;
import com.heytrip.hotel.search.ingest.model.AmapCollectMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 高德POI采集指标服务
 * <p>
 * 职责：
 * - 管理采集指标的生命周期（生成、查询、更新、清理）
 * - 基于Redis存储，支持分布式环境和断点续采
 * - 每个指标代表一个"城市+POI类型"的采集任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapCollectMetricService {

    private final RedissonClient redissonClient;
    
    private static final String METRIC_MAP_KEY = "amap:poi:collect:metrics:";
    private static final long METRIC_TTL_HOURS = 720; // 指标保留720小时
    
    /**
     * 初始化采集指标
     * <p>
     * 根据城市列表和POI类型列表生成全量采集指标
     * 如果已存在指标，则跳过初始化
     * 
     * @param batchId 批次ID
     * @param cities 城市列表
     * @param types POI类型列表
     * @return 是否执行了初始化（true-新建，false-已存在）
     */
    public boolean initializeMetrics(String batchId, List<AmapCitycode> cities, List<AmapPoitype> types) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        
        // 如果已存在指标，不重复初始化
        if (!metricsMap.isEmpty()) {
            log.info("[AmapCollectMetricService] 发现已存在的采集指标，跳过初始化: batchId={}, 指标数={}", 
                     batchId, metricsMap.size());
            return false;
        }
        
        log.info("[AmapCollectMetricService] 开始初始化采集指标: batchId={}, 城市数={}, 类型数={}", 
                 batchId, cities.size(), types.size());
        
        int totalMetrics = 0;
        
        // 遍历所有城市和类型，生成指标
        for (AmapCitycode city : cities) {
            for (AmapPoitype type : types) {
                String metricKey = AmapCollectMetric.generateMetricKey(city.getCitycode(), type.getTypecode());
                
                AmapCollectMetric metric = AmapCollectMetric.builder()
                        .cityCode(city.getCitycode())
                        .cityName(city.getName())
                        .typeCode(type.getTypecode())
                        .typeName(StrUtil.format("{}-{}-{}", type.getBigCategoryCn(), type.getMidCategoryCn(),type.getSubCategoryCn()))
                        .metricKey(metricKey)
                        .status("PENDING")  // 初始状态：待采集
                        .collectedCount(0L)
                        .build();
                
                metricsMap.put(metricKey, metric);
                totalMetrics++;
            }
        }
        
        // 设置过期时间
        metricsMap.expire(METRIC_TTL_HOURS, TimeUnit.HOURS);
        
        log.info("[AmapCollectMetricService] 采集指标初始化完成: batchId={}, 总指标数={}", 
                 batchId, totalMetrics);
        
        return true;
    }
    
    /**
     * 获取待采集的指标列表（分页）
     * <p>
     * 只返回状态为PENDING的指标
     * 
     * @param batchId 批次ID
     * @param pageSize 每页大小
     * @return 待采集指标列表
     */
    public List<AmapCollectMetric> getPendingMetrics(String batchId, int pageSize) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        
        if (metricsMap.isEmpty()) {
            log.warn("[AmapCollectMetricService] 指标列表为空，无法获取待采集指标: batchId={}", batchId);
            return new ArrayList<>();
        }
        
        // 筛选出PENDING状态的指标
        List<AmapCollectMetric> pendingMetrics = metricsMap.values().stream()
                .filter(metric -> "PENDING".equals(metric.getStatus()))
                .limit(pageSize)
                .collect(Collectors.toList());
        
        log.debug("[AmapCollectMetricService] 获取待采集指标: batchId={}, 请求数={}, 实际返回={}", 
                  batchId, pageSize, pendingMetrics.size());
        
        return pendingMetrics;
    }
    
    /**
     * 标记指标为采集中
     * 
     * @param batchId 批次ID
     * @param metricKey 指标Key
     */
    public void markAsProcessing(String batchId, String metricKey) {
        updateMetricStatus(batchId, metricKey, "PROCESSING", null);
    }
    
    /**
     * 标记指标为已完成
     * 
     * @param batchId 批次ID
     * @param metricKey 指标Key
     * @param collectedCount 采集到的POI数量
     */
    public void markAsCompleted(String batchId, String metricKey, long collectedCount) {
        updateMetricStatus(batchId, metricKey, "COMPLETED", collectedCount);
        log.info("[AmapCollectMetricService] 指标采集完成: metricKey={}, 采集数量={}", metricKey, collectedCount);
    }
    
    /**
     * 标记指标为失败
     * 
     * @param batchId 批次ID
     * @param metricKey 指标Key
     */
    public void markAsFailed(String batchId, String metricKey) {
        updateMetricStatus(batchId, metricKey, "FAILED", null);
        log.warn("[AmapCollectMetricService] 指标采集失败: metricKey={}", metricKey);
    }
    
    /**
     * 更新指标状态
     * 
     * @param batchId 批次ID
     * @param metricKey 指标Key
     * @param status 新状态
     * @param collectedCount 采集数量（可选）
     */
    private void updateMetricStatus(String batchId, String metricKey, String status, Long collectedCount) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        
        AmapCollectMetric metric = metricsMap.get(metricKey);
        if (metric == null) {
            log.warn("[AmapCollectMetricService] 指标不存在，无法更新状态: metricKey={}", metricKey);
            return;
        }
        
        metric.setStatus(status);
        if (collectedCount != null) {
            metric.setCollectedCount(collectedCount);
        }
        
        metricsMap.put(metricKey, metric);
    }
    
    /**
     * 获取指标统计信息
     * 
     * @param batchId 批次ID
     * @return 统计信息Map
     */
    public Map<String, Long> getMetricStats(String batchId) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        
        long total = metricsMap.size();
        long pending = metricsMap.values().stream().filter(m -> "PENDING".equals(m.getStatus())).count();
        long processing = metricsMap.values().stream().filter(m -> "PROCESSING".equals(m.getStatus())).count();
        long completed = metricsMap.values().stream().filter(m -> "COMPLETED".equals(m.getStatus())).count();
        long failed = metricsMap.values().stream().filter(m -> "FAILED".equals(m.getStatus())).count();
        
        return Map.of(
                "total", total,
                "pending", pending,
                "processing", processing,
                "completed", completed,
                "failed", failed
        );
    }
    
    /**
     * 清除所有指标
     * 
     * @param batchId 批次ID
     */
    public void clearMetrics(String batchId) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        metricsMap.delete();
        log.info("[AmapCollectMetricService] 已清除采集指标: batchId={}", batchId);
    }
    
    /**
     * 检查是否有待采集的指标
     * 
     * @param batchId 批次ID
     * @return true-有待采集指标，false-无
     */
    public boolean hasPendingMetrics(String batchId) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        
        return metricsMap.values().stream()
                .anyMatch(metric -> "PENDING".equals(metric.getStatus()));
    }
    
    /**
     * 获取总指标数
     * 
     * @param batchId 批次ID
     * @return 总指标数
     */
    public long getTotalMetrics(String batchId) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        return metricsMap.size();
    }
    
    /**
     * 获取已完成指标数
     * 
     * @param batchId 批次ID
     * @return 已完成指标数
     */
    public long getCompletedMetrics(String batchId) {
        String key = METRIC_MAP_KEY + batchId;
        RMap<String, AmapCollectMetric> metricsMap = redissonClient.getMap(key);
        
        return metricsMap.values().stream()
                .filter(metric -> "COMPLETED".equals(metric.getStatus()))
                .count();
    }
}
