package com.heytrip.hotel.search.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.heytrip.hotel.search.ingest.service.AmapCachedDataService;
import com.heytrip.hotel.search.ingest.service.AmapCollectMetricService;
import com.heytrip.hotel.search.ingest.service.AmapCollectTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 高德POI采集控制器
 * 提供POI采集任务的管理接口
 */
@RequiredArgsConstructor
@Slf4j
@SaCheckLogin
@RestController
@RequestMapping("/amap/poi")
public class AmapPoiCollectController {

    private final ProducerTemplate producerTemplate;
    private final AmapCollectTaskManager taskManager;
    private final AmapCachedDataService cachedDataService;
    private final AmapCollectMetricService metricService;

    /**
     * 手动触发POI采集
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> startCollect() {
        log.info("[AmapPoiCollectController] 接收到手动触发POI采集请求");
        
        // 检查任务状态
        AmapCollectTaskManager.TaskStatusInfo status = taskManager.getTaskStatus();
        if (status.getStatus() == AmapCollectTaskManager.TaskStatus.RUNNING) {
            log.warn("[AmapPoiCollectController] 已有采集任务正在运行中，拒绝新的采集请求");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "已有采集任务正在运行中",
                    "batchId", status.getBatchId()
            ));
        }
        
        try {
            // 发送到Camel路由
            producerTemplate.sendBody("direct:amap:poi:collect:manual", "");
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "POI采集任务已启动"
            ));
        } catch (Exception e) {
            log.error("[AmapPoiCollectController] 启动POI采集失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "启动失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 暂停采集任务
     */
    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pauseTask() {
        log.info("[AmapPoiCollectController] 接收到暂停采集任务请求");
        
        boolean success = taskManager.pauseTask();
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "采集任务已暂停"
            ));
        } else {
            AmapCollectTaskManager.TaskStatusInfo status = taskManager.getTaskStatus();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无法暂停任务，当前状态：" + status.getStatus()
            ));
        }
    }

    /**
     * 恢复采集任务
     */
    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> resumeTask() {
        log.info("[AmapPoiCollectController] 接收到恢复采集任务请求");
        
        boolean success = taskManager.resumeTask();
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "采集任务已恢复"
            ));
        } else {
            AmapCollectTaskManager.TaskStatusInfo status = taskManager.getTaskStatus();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无法恢复任务，当前状态：" + status.getStatus()
            ));
        }
    }

    /**
     * 停止采集任务
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopTask() {
        log.info("[AmapPoiCollectController] 接收到停止采集任务请求");
        
        boolean success = taskManager.stopTask("用户手动停止");
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "采集任务已停止"
            ));
        } else {
            AmapCollectTaskManager.TaskStatusInfo status = taskManager.getTaskStatus();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无法停止任务，当前状态：" + status.getStatus()
            ));
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTaskStatus() {
        AmapCollectTaskManager.TaskStatusInfo status = taskManager.getTaskStatus();
        double progress = taskManager.getProgressPercentage();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", status.getStatus());
        result.put("batchId", status.getBatchId());
        result.put("traceId", status.getTraceId());
        result.put("startTime", status.getStartTime());
        result.put("totalMetrics", status.getTotalMetrics());
        result.put("completedMetrics", status.getCompletedMetrics());
        result.put("totalCollected", status.getTotalCollected());
        result.put("currentCity", status.getCurrentCityCode());
        result.put("currentType", status.getCurrentTypeCode());
        result.put("progress", String.format("%.2f%%", progress));
        result.put("message", status.getMessage());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取采集指标统计
     */
    @GetMapping("/metrics/stats")
    public ResponseEntity<Map<String, Object>> getMetricsStats() {
        AmapCollectTaskManager.TaskStatusInfo status = taskManager.getTaskStatus();
        
        if (status.getBatchId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无正在进行的任务"
            ));
        }
        
        try {
            Map<String, Long> stats = metricService.getMetricStats(status.getBatchId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("batchId", status.getBatchId());
            result.put("total", stats.get("total"));
            result.put("pending", stats.get("pending"));
            result.put("processing", stats.get("processing"));
            result.put("completed", stats.get("completed"));
            result.put("failed", stats.get("failed"));
            
            // 计算完成率
            long total = stats.get("total");
            long completed = stats.get("completed");
            double completionRate = total > 0 ? (double) completed / total * 100 : 0.0;
            result.put("completionRate", String.format("%.2f%%", completionRate));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[AmapPoiCollectController] 获取指标统计失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 清理采集指标（慎用）
     */
    @PostMapping("/metrics/clear")
    public ResponseEntity<Map<String, Object>> clearMetrics(
            @RequestParam String batchId) {
        log.warn("[AmapPoiCollectController] 接收到清理指标请求: batchId={}", batchId);
        
        try {
            metricService.clearMetrics(batchId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "指标已清理"
            ));
        } catch (Exception e) {
            log.error("[AmapPoiCollectController] 清理指标失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "清理失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 触发城市代码同步
     */
    @PostMapping("/sync/citycode")
    public ResponseEntity<Map<String, Object>> syncCitycode() {
        log.info("[AmapPoiCollectController] 接收到城市代码同步请求");
        
        try {
            producerTemplate.sendBody("direct:amap:citycode:sync:manual", "");
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "城市代码同步任务已启动"
            ));
        } catch (Exception e) {
            log.error("[AmapPoiCollectController] 城市代码同步失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "同步失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 触发POI类型同步
     */
    @PostMapping("/sync/poitype")
    public ResponseEntity<Map<String, Object>> syncPoitype() {
        log.info("[AmapPoiCollectController] 接收到POI类型同步请求");
        
        try {
            producerTemplate.sendBody("direct:amap:poitype:sync:manual", "");
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "POI类型同步任务已启动"
            ));
        } catch (Exception e) {
            log.error("[AmapPoiCollectController] POI类型同步失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "同步失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 清除缓存
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache(
            @RequestParam(required = false) String type) {
        log.info("[AmapPoiCollectController] 接收到清除缓存请求: type={}", type);
        
        try {
            if ("citycode".equals(type)) {
                cachedDataService.evictAllCitycodeCaches();
            } else if ("poitype".equals(type)) {
                cachedDataService.evictAllPoitypeCaches();
            } else {
                cachedDataService.evictAllCaches();
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "缓存已清除"
            ));
        } catch (Exception e) {
            log.error("[AmapPoiCollectController] 清除缓存失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "清除失败：" + e.getMessage()
            ));
        }
    }
}
