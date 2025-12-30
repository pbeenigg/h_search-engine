package com.heytrip.hotel.search.ingest.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 高德POI采集任务管理器
 * <p>
 * 职责：
 * - 管理采集任务的状态（运行、暂停、停止、完成）
 * - 支持任务暂停和恢复
 * - 基于Redis存储任务状态，支持分布式环境
 * <p>
 * 注意：
 * - 断点续采由AmapCollectMetricService管理，基于Redis指标状态
 * - 本类主要负责任务的执行控制（启动/暂停/停止/恢复）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapCollectTaskManager {

    private final RedissonClient redissonClient;
    
    private static final String TASK_STATUS_KEY = "amap:poi:collect:status";
    private static final long STATUS_TTL_HOURS = 48; // 状态保留48小时

    /**
     * 任务状态
     */
    public enum TaskStatus {
        IDLE,      // 空闲
        RUNNING,   // 运行中
        PAUSED,    // 已暂停
        STOPPED,   // 已停止
        COMPLETED, // 已完成
        FAILED     // 失败
    }

    /**
     * 任务状态信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatusInfo {
        private TaskStatus status;
        private String batchId;
        private String traceId;
        private OffsetDateTime startTime;
        private OffsetDateTime pauseTime;
        private OffsetDateTime resumeTime;
        private Long totalMetrics;       // 总采集指标数
        private Long completedMetrics;   // 已完成指标数
        private Long totalCollected;     // 已采集POI数
        private String currentCityCode;
        private String currentTypeCode;
        private String message;
    }



    /**
     * 获取任务状态
     *
     * @return 任务状态信息
     */
    public TaskStatusInfo getTaskStatus() {
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        TaskStatusInfo status = bucket.get();
        
        if (status == null) {
            status = TaskStatusInfo.builder()
                    .status(TaskStatus.IDLE)
                    .message("无正在进行的任务")
                    .build();
        }
        
        return status;
    }

    /**
     * 开始任务
     *
     * @param batchId     批次ID
     * @param traceId     追踪ID
     * @param totalMetrics 总指标数
     */
    public void startTask(String batchId, String traceId, long totalMetrics) {
        TaskStatusInfo statusInfo = TaskStatusInfo.builder()
                .status(TaskStatus.RUNNING)
                .batchId(batchId)
                .traceId(traceId)
                .startTime(OffsetDateTime.now())
                .totalMetrics(totalMetrics)
                .completedMetrics(0L)
                .totalCollected(0L)
                .message("任务启动")
                .build();
        
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.set(statusInfo, STATUS_TTL_HOURS, TimeUnit.HOURS);
        
        log.info("[AmapCollectTaskManager] 任务已启动: batchId={}, traceId={}, totalMetrics={}", 
                batchId, traceId, totalMetrics);
    }

    /**
     * 暂停任务（无进度保存）
     *
     * @return 是否成功暂停
     */
    public boolean pauseTask() {
        TaskStatusInfo currentStatus = getTaskStatus();
        
        if (currentStatus.getStatus() != TaskStatus.RUNNING) {
            log.warn("[AmapCollectTaskManager] 无法暂停任务，当前状态：{}", currentStatus.getStatus());
            return false;
        }
        
        currentStatus.setStatus(TaskStatus.PAUSED);
        currentStatus.setPauseTime(OffsetDateTime.now());
        currentStatus.setMessage("任务已暂停");
        
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.set(currentStatus, STATUS_TTL_HOURS, TimeUnit.HOURS);
        
        log.info("[AmapCollectTaskManager] 任务已暂停: batchId={}", currentStatus.getBatchId());
        return true;
    }
    

    /**
     * 恢复任务
     *
     * @return 是否成功恢复
     */
    public boolean resumeTask() {
        TaskStatusInfo currentStatus = getTaskStatus();
        
        if (currentStatus.getStatus() != TaskStatus.PAUSED) {
            log.warn("[AmapCollectTaskManager] 无法恢复任务，当前状态：{}", currentStatus.getStatus());
            return false;
        }
        
        currentStatus.setStatus(TaskStatus.RUNNING);
        currentStatus.setResumeTime(OffsetDateTime.now());
        currentStatus.setMessage("任务已恢复");
        
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.set(currentStatus, STATUS_TTL_HOURS, TimeUnit.HOURS);
        
        log.info("[AmapCollectTaskManager] 任务已恢复: batchId={}", currentStatus.getBatchId());
        return true;
    }

    /**
     * 停止任务
     *
     * @param message 停止原因
     * @return 是否成功停止
     */
    public boolean stopTask(String message) {
        TaskStatusInfo currentStatus = getTaskStatus();
        
        // 如果是IDLE状态，说明没有任务在运行
        if (currentStatus.getStatus() == TaskStatus.IDLE) {
            log.warn("[AmapCollectTaskManager] 无法停止任务，当前状态：IDLE（无任务运行）");
            return false;
        }
        
        // 允许在任何其他状态（包括COMPLETED）下强制停止
        TaskStatus previousStatus = currentStatus.getStatus();
        currentStatus.setStatus(TaskStatus.STOPPED);
        currentStatus.setMessage(message != null ? message : "任务已停止");
        
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.set(currentStatus, STATUS_TTL_HOURS, TimeUnit.HOURS);
        
        log.info("[AmapCollectTaskManager] 任务已停止: batchId={}, previousStatus={}, reason={}", 
                currentStatus.getBatchId(), previousStatus, message);
        return true;
    }

    /**
     * 完成任务
     */
    public void completeTask() {
        TaskStatusInfo currentStatus = getTaskStatus();
        
        currentStatus.setStatus(TaskStatus.COMPLETED);
        currentStatus.setMessage("任务已完成");
        
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.set(currentStatus, STATUS_TTL_HOURS, TimeUnit.HOURS);
        
        log.info("[AmapCollectTaskManager] 任务已完成: batchId={}", currentStatus.getBatchId());
    }

    /**
     * 标记任务失败
     *
     * @param errorMessage 错误信息
     */
    public void failTask(String errorMessage) {
        TaskStatusInfo currentStatus = getTaskStatus();
        
        currentStatus.setStatus(TaskStatus.FAILED);
        currentStatus.setMessage("任务失败：" + errorMessage);
        
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.set(currentStatus, STATUS_TTL_HOURS, TimeUnit.HOURS);
        
        log.error("[AmapCollectTaskManager] 任务失败: batchId={}, error={}", 
                currentStatus.getBatchId(), errorMessage);
    }

    /**
     * 更新任务进度
     *
     * @param completedMetrics 已完成指标数
     * @param totalCollected   已采集POI总数
     * @param currentCityCode  当前城市代码
     * @param currentTypeCode  当前类型代码
     */
    public void updateProgress(long completedMetrics, long totalCollected, 
                              String currentCityCode, String currentTypeCode) {
        TaskStatusInfo currentStatus = getTaskStatus();
        
        if (currentStatus.getStatus() == TaskStatus.RUNNING) {
            currentStatus.setCompletedMetrics(completedMetrics);
            currentStatus.setTotalCollected(totalCollected);
            currentStatus.setCurrentCityCode(currentCityCode);
            currentStatus.setCurrentTypeCode(currentTypeCode);
            
            RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
            bucket.set(currentStatus, STATUS_TTL_HOURS, TimeUnit.HOURS);
            
            log.debug("[AmapCollectTaskManager] 进度更新: {}/{}, 已采集={}", 
                    completedMetrics, currentStatus.getTotalMetrics(), totalCollected);
        }
    }

    /**
     * 检查任务是否应该暂停
     *
     * @return true:应暂停, false:继续运行
     */
    public boolean shouldPause() {
        TaskStatusInfo status = getTaskStatus();
        return status.getStatus() == TaskStatus.PAUSED;
    }

    /**
     * 检查任务是否应该停止
     *
     * @return true:应停止, false:继续运行
     */
    public boolean shouldStop() {
        TaskStatusInfo status = getTaskStatus();
        return status.getStatus() == TaskStatus.STOPPED;
    }


    /**
     * 清理任务状态
     */
    public void clearTaskStatus() {
        RBucket<TaskStatusInfo> bucket = redissonClient.getBucket(TASK_STATUS_KEY);
        bucket.delete();
        log.info("[AmapCollectTaskManager] 任务状态已清理");
    }

    /**
     * 获取任务进度百分比
     *
     * @return 进度百分比（0-100）
     */
    public double getProgressPercentage() {
        TaskStatusInfo status = getTaskStatus();
        if (status.getTotalMetrics() == null || status.getTotalMetrics() == 0) {
            return 0.0;
        }
        long completed = status.getCompletedMetrics() != null ? status.getCompletedMetrics() : 0;
        return (double) completed / status.getTotalMetrics() * 100;
    }
}
