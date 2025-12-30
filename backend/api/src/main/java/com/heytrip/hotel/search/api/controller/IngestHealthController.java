package com.heytrip.hotel.search.api.controller;

import com.heytrip.hotel.search.domain.entity.SyncLog;
import com.heytrip.hotel.search.domain.repository.SyncLogRepository;
import com.heytrip.hotel.search.ingest.service.JobScheduleCacheService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集任务健康检查端点
 *
 *
 * 提供实时状态查询、进度监控、缓存管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/ingest/health")
@RequiredArgsConstructor
public class IngestHealthController {

    private final SyncLogRepository syncLogRepository;
    private final JobScheduleCacheService jobScheduleCacheService;

    private static final String JOB_CODE = "HOTEL_FULL_SYNC_ALL";

    /**
     * 任务状态详情 DTO
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TaskStatusDto {
        private String jobCode;
        private String status;
        private String traceId;
        private Long syncLogId;
        private OffsetDateTime startedAt;
        private Long elapsedSeconds;
        private Long totalIds;
        private Long totalDetails;
        private Long successCount;
        private Long failCount;
        private String message;
        private Double estimatedProgress;
    }

    /**
     * 健康检查响应 DTO
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class HealthResponse {
        private String status;          // HEALTHY / RUNNING / FAILED
        private Integer runningTaskCount;
        private List<TaskStatusDto> runningTasks;
        private TaskStatusDto lastCompletedTask;
        private String cacheStats;
        private Long timestamp;
    }

    /**
     * 主健康检查端点
     *
     * GET /ingest/health
     */
    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        try {
            // 查询正在运行的任务
            List<SyncLog> runningSyncs = syncLogRepository.findByJobCodeAndStatus(JOB_CODE, "RUNNING");
            
            // 查询最后完成的任务
            List<SyncLog> recentCompleted = syncLogRepository.findAll().stream()
                    .filter(s -> s.getJobCode().equals(JOB_CODE))
                    .filter(s -> !s.getStatus().equals("RUNNING"))
                    .sorted((a, b) -> b.getFinishedAt().compareTo(a.getFinishedAt()))
                    .limit(1)
                    .toList();

            // 转换为 DTO
            List<TaskStatusDto> runningTaskDtos = runningSyncs.stream()
                    .map(this::convertToTaskStatusDto)
                    .toList();

            TaskStatusDto lastCompletedDto = recentCompleted.isEmpty() 
                    ? null 
                    : convertToTaskStatusDto(recentCompleted.get(0));

            // 判断整体状态
            String overallStatus = "HEALTHY";
            if (!runningSyncs.isEmpty()) {
                overallStatus = "RUNNING";
            } else if (lastCompletedDto != null && "FAILED".equals(lastCompletedDto.getStatus())) {
                overallStatus = "FAILED";
            }

            HealthResponse response = HealthResponse.builder()
                    .status(overallStatus)
                    .runningTaskCount(runningSyncs.size())
                    .runningTasks(runningTaskDtos)
                    .lastCompletedTask(lastCompletedDto)
                    .cacheStats(jobScheduleCacheService.getCacheStats())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[HealthCheck] 健康检查失败", e);
            return ResponseEntity.status(500).body(
                    HealthResponse.builder()
                            .status("ERROR")
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
        }
    }

    /**
     * 查询指定任务详情
     *
     * GET /ingest/health/task/{syncLogId}
     */
    @GetMapping("/task/{syncLogId}")
    public ResponseEntity<TaskStatusDto> getTaskStatus(@PathVariable Long syncLogId) {
        return syncLogRepository.findById(syncLogId)
                .map(this::convertToTaskStatusDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 查询正在运行的任务列表
     *
     * GET /ingest/health/running
     */
    @GetMapping("/running")
    public ResponseEntity<List<TaskStatusDto>> getRunningTasks() {
        List<SyncLog> runningSyncs = syncLogRepository.findByJobCodeAndStatus(JOB_CODE, "RUNNING");
        List<TaskStatusDto> dtos = runningSyncs.stream()
                .map(this::convertToTaskStatusDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * 清除配置缓存
     *
     * POST /ingest/health/cache/invalidate
     */
    @PostMapping("/cache/invalidate")
    public ResponseEntity<Map<String, String>> invalidateCache(
            @RequestParam(required = false) String jobCode) {
        try {
            if (jobCode != null && !jobCode.isBlank()) {
                jobScheduleCacheService.invalidate(jobCode);
                log.info("[HealthCheck] 缓存已清除 jobCode={}", jobCode);
            } else {
                jobScheduleCacheService.invalidateAll();
                log.info("[HealthCheck] 所有缓存已清除");
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "缓存已清除");
            result.put("cacheStats", jobScheduleCacheService.getCacheStats());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[HealthCheck] 清除缓存失败", e);
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @Dest GET /ingest/health/cache/stats
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, String>> getCacheStats() {
        Map<String, String> stats = new HashMap<>();
        stats.put("cacheStats", jobScheduleCacheService.getCacheStats());
        stats.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(stats);
    }

    /**
     * 转换 SyncLog 为 TaskStatusDto
     */
    private TaskStatusDto convertToTaskStatusDto(SyncLog syncLog) {
        Long elapsedSeconds = null;
        if (syncLog.getStartedAt() != null) {
            OffsetDateTime endTime = syncLog.getFinishedAt() != null 
                    ? syncLog.getFinishedAt() 
                    : OffsetDateTime.now();
            elapsedSeconds = Duration.between(syncLog.getStartedAt(), endTime).getSeconds();
        }

        // 估算进度（如果有总数信息）
        Double estimatedProgress = null;
        if (syncLog.getTotalIds() != null && syncLog.getTotalIds() > 0) {
            long processed = (syncLog.getSuccessCount() != null ? syncLog.getSuccessCount() : 0)
                    + (syncLog.getFailCount() != null ? syncLog.getFailCount() : 0);
            estimatedProgress = (double) processed / syncLog.getTotalIds() * 100;
        }

        return TaskStatusDto.builder()
                .jobCode(syncLog.getJobCode())
                .status(syncLog.getStatus())
                .traceId(syncLog.getTraceId())
                .syncLogId(syncLog.getId())
                .startedAt(syncLog.getStartedAt())
                .elapsedSeconds(elapsedSeconds)
                .totalIds(syncLog.getTotalIds())
                .totalDetails(syncLog.getTotalDetails())
                .successCount(syncLog.getSuccessCount())
                .failCount(syncLog.getFailCount())
                .message(syncLog.getMessage())
                .estimatedProgress(estimatedProgress)
                .build();
    }
}
