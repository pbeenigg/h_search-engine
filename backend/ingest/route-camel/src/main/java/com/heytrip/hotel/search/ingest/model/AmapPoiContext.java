package com.heytrip.hotel.search.ingest.model;

import lombok.Builder;
import lombok.Data;

/**
 * 高德POI采集上下文
 * 用于在Camel路由之间传递采集状态和配置信息
 */
@Data
@Builder
public class AmapPoiContext {

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 同步日志ID
     */
    private Long syncLogId;

    /**
     * 当前使用的API Key
     */
    private String currentApiKey;

    /**
     * 当前采集的城市代码
     */
    private String currentCityCode;

    /**
     * 当前采集的城市名称
     */
    private String currentCityName;

    /**
     * 当前采集的POI类型代码
     */
    private String currentTypeCode;

    /**
     * 当前页码
     */
    private Integer currentPage;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 是否严格限制城市范围
     */
    private Boolean cityLimit;


    /**
     * 需要返回的字段列表
     */
    private String showFields ="children,business";

    /**
     * 本次采集批次标识
     */
    private String batchId;

    /**
     * 已采集POI总数（累计）
     */
    private Long totalCollected;

    /**
     * 已成功入库POI数量（累计）
     */
    private Long successCount;

    /**
     * 失败数量（累计）
     */
    private Long failureCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryAttempts;

    /**
     * 当前重试次数
     */
    private Integer currentRetryCount;

    /**
     * HTTP超时时间（毫秒）
     */
    private Integer httpTimeoutMs;

    /**
     * 数据库批量提交大小
     */
    private Integer dbCommitSize;

    /**
     * 请求开始时间（纳秒）
     */
    private Long reqStartNs;

    /**
     * 是否已获取信号量
     */
    private boolean semAcquired;

    /**
     * 信号量Key
     */
    private String semKey;

    /**
     * 信号量许可数
     */
    private Integer permits;

    /**
     * 采集指标标识（城市+类型）
     */
    private String metricKey;

    /**
     * 当前指标已采集POI数量（单个指标）
     */
    private Long currentMetricCollected;

    /**
     * ES索引名称
     */
    private String esIndexName;
}
