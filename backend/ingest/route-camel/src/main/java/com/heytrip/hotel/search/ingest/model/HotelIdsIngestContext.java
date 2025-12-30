package com.heytrip.hotel.search.ingest.model;

import com.heytrip.hotel.search.common.util.Md5Signer;
import lombok.Builder;
import lombok.Data;

/**
 * HotelIds 路由专用运行上下文
 * 仅服务于酒店ID分页采集与详情入库线路，避免与其他业务路由耦合。
 */
@Data
@Builder
public class HotelIdsIngestContext {
    // 运行态
    private String traceId;
    private Long syncLogId;
    private long watermark;
    private long reqStartNs;

    // 并发/信号量
    private int maxConcurrency;
    private String semKey;
    private boolean semAcquired;
    private int permits;

    // 业务参数
    private int submitBatchSize;
    private int hotelDetailBatchSize;
    private int hotelIdsBatchSize;
    private int retryTimes;
    private int retryDelayMs;
    private int retryJitterMs;
    private int retryBaseDelayMs429;
    private int httpTimeoutMs;

    // 供应商
    private String baseUrl;
    private String app;
    private String secret;

    public String sign(long tsSec) {
        return Md5Signer.buildSign(app, secret, tsSec);
    }
}
