package com.heytrip.hotel.search.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.heytrip.hotel.search.common.api.R;
import com.heytrip.hotel.search.domain.entity.JobSchedule;
import com.heytrip.hotel.search.domain.entity.SyncLog;
import com.heytrip.hotel.search.domain.entity.SyncLogDetail;
import com.heytrip.hotel.search.domain.repository.JobScheduleRepository;
import com.heytrip.hotel.search.domain.repository.SyncLogDetailRepository;
import com.heytrip.hotel.search.domain.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 任务管理与查询接口
 *
 * - 触发全量同步
 * - 查询同步日志与明细
 */
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
@SaCheckLogin
@SaCheckPermission("job:manage")
@Slf4j
public class JobController {

    private final SyncLogRepository syncLogRepository;
    private final SyncLogDetailRepository syncLogDetailRepository;
    private final ProducerTemplate producerTemplate;
    private final JobScheduleRepository jobScheduleRepository;



    /**
     * 立即执行一次（单页模式）
     *
     * 通过 direct 路由触发，复用与 Quartz 相同的处理链
     */
    @PostMapping("/trigger/runOnce")
    public R<Map<String, Object>> triggerFullSyncNow() {

        final Long syncLogId = 12L;
        final String traceId = "trace-" + System.currentTimeMillis();
        try {

            producerTemplate.send("direct:ingest:hotelIds:runOnce", exchange ->
                {
                    exchange.getIn().setHeader("syncLogId", syncLogId);
                    exchange.getIn().setHeader("traceId", traceId);
                }
            );
        } catch (Exception e) {
            log.error("[JOB] 立即触发执行失败", e);
            return R.fail( 2001, "触发失败: " + e.getMessage());
        }
        return R.ok(Map.of("syncLogId", syncLogId, "traceId", traceId));
    }

    /**
     * 触发持续全量采集（不间断模式）
     *
     * 会循环拉取所有页直到无更多数据
     * 注意：此接口为异步执行，立即返回，通过syncLogId查询进度
     */
    @PostMapping("/trigger/runContinuous")
    public R<Map<String, Object>> triggerContinuousSync() {
        try {
            log.info("[JOB] 开始触发持续全量采集");
            
            // 异步触发，不等待返回
            producerTemplate.asyncSend("direct:ingest:hotelIds:runContinuous", exchange -> {
                // 持续模式不需要预先传递syncLogId，由路由内部创建
                log.info("[JOB] 持续采集路由已触发");
            });
            
            return R.ok(Map.of(
                "message", "持续全量采集已启动，将循环采集直到无更多数据",
                "mode", "CONTINUOUS",
                "tip", "请通过 /job/sync-log/page 接口查询最新的 traceId 以continuous-开头的记录来追踪进度"
            ));
        } catch (Exception e) {
            log.error("[JOB] 触发持续采集失败", e);
            return R.fail(2002, "触发失败: " + e.getMessage());
        }
    }

    /**
     * 停止正在运行的持续全量采集
     *
     * 通过禁用任务配置，让持续采集在下一页处理时自动停止
     */
    @PostMapping("/trigger/stopContinuousSync")
    public R<Map<String, Object>> stopContinuousSync() {
        try {
            log.info("[JOB] 开始停止持续全量采集");
            
            // 查找任务配置
            var jsOpt = jobScheduleRepository.findByJobCode("HOTEL_FULL_SYNC_ALL");
            if (jsOpt.isEmpty()) {
                return R.fail(2003, "任务配置不存在");
            }
            
            JobSchedule js = jsOpt.get();
            if (!js.isEnabled()) {
                return R.ok(Map.of(
                    "message", "任务已经是禁用状态",
                    "status", "ALREADY_DISABLED"
                ));
            }
            
            // 禁用任务，持续采集会在下一页检查时停止
            js.setEnabled(false);
            js.setRemark(js.getRemark() + " [手动停止 " + OffsetDateTime.now() + "]");
            jobScheduleRepository.save(js);
            
            log.info("[JOB] 任务已禁用，持续采集将在处理完当前页后停止");
            
            return R.ok(Map.of(
                "message", "停止指令已发送，持续采集将在处理完当前页后停止（预计几秒到几分钟）",
                "action", "DISABLED_JOB",
                "tip", "请通过 /job/sync-log/page 查询最新的 continuous- 开头的记录确认停止状态"
            ));
        } catch (Exception e) {
            log.error("[JOB] 停止持续采集失败", e);
            return R.fail(2004, "停止失败: " + e.getMessage());
        }
    }

    /**
     * 重新启用任务（用于停止后恢复）
     */
    @PostMapping("/trigger/enableSync")
    public R<Map<String, Object>> enableSync() {
        try {
            var jsOpt = jobScheduleRepository.findByJobCode("HOTEL_FULL_SYNC_ALL");
            if (jsOpt.isEmpty()) {
                return R.fail(2003, "任务配置不存在");
            }
            
            JobSchedule js = jsOpt.get();
            js.setEnabled(true);
            jobScheduleRepository.save(js);
            
            log.info("[JOB] 任务已重新启用");
            
            return R.ok(Map.of(
                "message", "任务已重新启用，可以再次触发采集",
                "status", "ENABLED"
            ));
        } catch (Exception e) {
            log.error("[JOB] 启用任务失败", e);
            return R.fail(2005, "启用失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询同步日志列表
     */
    @GetMapping("/sync-log/page")
    public R<Page<SyncLog>> pageSyncLog(@RequestParam(name = "page", defaultValue = "0") int page,
                                        @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable p = PageRequest.of(page, size);
        // 简化：使用 findAll(Pageable)；如需条件可扩展 Spec
        Page<SyncLog> data = syncLogRepository.findAll(p);
        return R.ok(data);
    }

    /**
     * 根据同步日志ID分页查询日志明细
     */
    @GetMapping("/sync-log/{id}/detail")
    public R<Page<SyncLogDetail>> pageSyncLogDetail(@PathVariable("id") Long syncLogId,
                                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable p = PageRequest.of(page, size);
        Page<SyncLogDetail> data = syncLogDetailRepository.findBySyncLogId(syncLogId, p);
        return R.ok(data);
    }
}
