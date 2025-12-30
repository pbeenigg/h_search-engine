package com.heytrip.hotel.search.ingest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 高德POI采集完成事件
 * 通过Redis Stream发送到下游进行ES索引创建和数据清洗
 */
@Data
@Builder
public class AmapPoiCollectedEvent {

    /**
     * 事件类型
     */
    @JsonProperty("event_type")
    private String eventType;

    /**
     * 批次ID
     */
    @JsonProperty("batch_id")
    private String batchId;

    /**
     * ES索引名称
     */
    @JsonProperty("index_name")
    private String indexName;

    /**
     * 时间戳（ISO 8601格式）
     */
    private String timestamp;

    /**
     * 统计信息
     */
    private Statistics statistics;

    /**
     * 数据位置信息
     */
    @JsonProperty("data_location")
    private DataLocation dataLocation;

    /**
     * 追踪ID
     */
    @JsonProperty("trace_id")
    private String traceId;

    /**
     * 统计信息
     */
    @Data
    @Builder
    public static class Statistics {
        /**
         * 总记录数
         */
        @JsonProperty("total_count")
        private Long totalCount;

        /**
         * 成功数
         */
        @JsonProperty("success_count")
        private Long successCount;

        /**
         * 失败数
         */
        @JsonProperty("fail_count")
        private Long failCount;

        /**
         * 采集的城市列表
         */
        private List<String> cities;

        /**
         * 采集的POI类型列表
         */
        private List<String> types;
    }

    /**
     * 数据位置信息
     */
    @Data
    @Builder
    public static class DataLocation {
        /**
         * 数据库名称
         */
        private String database;

        /**
         * 表名
         */
        private String table;

        /**
         * 批次字段名
         */
        @JsonProperty("batch_column")
        private String batchColumn;

        /**
         * 批次值
         */
        @JsonProperty("batch_value")
        private String batchValue;
    }
}
