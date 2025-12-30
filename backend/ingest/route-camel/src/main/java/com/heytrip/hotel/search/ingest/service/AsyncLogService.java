package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.domain.entity.ApiRequestLog;
import com.heytrip.hotel.search.domain.entity.SyncLogDetail;
import com.heytrip.hotel.search.domain.repository.ApiRequestLogRepository;
import com.heytrip.hotel.search.domain.repository.SyncLogDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步日志服务
 * 使用异步方式写入 API 请求日志和同步失败详情，避免阻塞主流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncLogService {

    private final ApiRequestLogRepository apiRequestLogRepository;
    private final SyncLogDetailRepository syncLogDetailRepository;

    /**
     * 异步保存单条 API 日志
     * @param apiLog API 日志对象
     * @return CompletableFuture
     */
    @Async("asyncLogExecutor")
    @Transactional
    public CompletableFuture<Void> saveApiRequestLogAsync(ApiRequestLog apiLog) {
        try {
            apiRequestLogRepository.save(apiLog);
            log.debug("[AsyncApiRequestLog] 异步保存成功 method={} uri={} status={}",
                    apiLog.getHttpMethod(), apiLog.getUrl(), apiLog.getResponseStatus());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[AsyncApiRequestLog] 异步保存失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步批量保存 API 日志
     * @param apiLogs API 日志列表
     * @return CompletableFuture
     */
    @Async("asyncLogExecutor")
    @Transactional
    public CompletableFuture<Void> saveAllApiRequestLogAsync(List<ApiRequestLog> apiLogs) {
        try {
            apiRequestLogRepository.saveAll(apiLogs);
            log.debug("[AsyncApiLog] 异步批量保存成功 count={}", apiLogs.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[AsyncApiLog] 异步批量保存失败 count={}", apiLogs.size(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步保存同步失败详情
     * @param syncLogDetail 同步失败详情对象
     * @return CompletableFuture
     */
    @Async("asyncLogExecutor")
    @Transactional
    public CompletableFuture<Void> saveSyncLogDetailAsync(SyncLogDetail syncLogDetail) {
        try {
            syncLogDetailRepository.save(syncLogDetail);
            log.debug("[AsyncSyncLogDetail] 异步保存成功 syncLogId={} stage={} errorCode={}", 
                    syncLogDetail.getSyncLogId(), syncLogDetail.getStage(), syncLogDetail.getErrorCode());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[AsyncSyncLogDetail] 异步保存失败 syncLogId={} stage={}", 
                    syncLogDetail.getSyncLogId(), syncLogDetail.getStage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步批量保存同步失败详情
     * @param syncLogDetails 同步失败详情列表
     * @return CompletableFuture
     */
    @Async("asyncLogExecutor")
    @Transactional
    public CompletableFuture<Void> saveSyncLogDetailsAsync(List<SyncLogDetail> syncLogDetails) {
        try {
            syncLogDetailRepository.saveAll(syncLogDetails);
            log.debug("[AsyncSyncLogDetail] 异步批量保存成功 count={}", syncLogDetails.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("[AsyncSyncLogDetail] 异步批量保存失败 count={}", syncLogDetails.size(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
