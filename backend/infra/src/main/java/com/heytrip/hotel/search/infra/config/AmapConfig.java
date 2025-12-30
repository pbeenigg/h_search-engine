package com.heytrip.hotel.search.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 高德地图POI采集配置
 * prefix: amap.poi
 */
@Data
@Component
@ConfigurationProperties(prefix = "amap.poi")
public class AmapConfig {

    /**
     * API配置
     */
    private ApiConfig api = new ApiConfig();

    /**
     * 采集配置
     */
    private CollectConfig collect = new CollectConfig();

    /**
     * 性能配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 数据校验配置
     */
    private ValidationConfig validation = new ValidationConfig();

    /**
     * 定时任务配置
     */
    private ScheduleConfig schedule = new ScheduleConfig();

    /**
     * API配置
     */
    @Data
    public static class ApiConfig {
        /**
         * 高德API基础地址
         */
        private String baseUrl = "https://restapi.amap.com";

        /**
         * API Key列表（支持多个Key轮询使用）
         */
        private List<String> keys = new ArrayList<>();

        /**
         * Key轮询策略（ROUND_ROBIN:轮询, RANDOM:随机）
         */
        private String keyRotationStrategy = "ROUND_ROBIN";

        /**
         * 故障Key黑名单时长（秒），默认1小时
         */
        private Integer keyBlacklistDuration = 3600;

        /**
         * 每个Key的最大重试次数
         */
        private Integer maxRetryPerKey = 3;

        /**
         * 每个Key每天的配额限制
         */
        private Integer dailyQuotaPerKey = 1000;

        /**
         * HTTP状态码重试列表
         */
        private List<Integer> httpStatusRetry = List.of(429, 500, 502, 503, 504);
    }

    /**
     * 采集配置
     */
    @Data
    public static class CollectConfig {
        /**
         * 是否启用POI采集
         */
        private Boolean enabled = true;

        /**
         * 采集城市白名单（为空表示采集全部城市）
         * 示例：["110000", "310000"]
         */
        private List<String> enabledCities = new ArrayList<>();

        /**
         * 采集POI类型白名单（为空表示采集全部类型）
         * 示例：["141201", "010100"]
         */
        private List<String> enabledTypes = new ArrayList<>();

        /**
         * API单页大小（1-25）
         */
        private Integer pageSize = 25;

        /**
         * 数据库批量提交大小
         */
        private Integer dbCommitSize = 1000;

        /**
         * 详情请求批次大小
         */
        private Integer detailBatchSize = 20;

        /**
         * 是否严格限制城市范围（对应API的city_limit参数）
         */
        private Boolean cityLimit = true;
    }

    /**
     * 性能配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 最大并发请求数
         */
        private Integer maxConcurrentRequests = 10;

        /**
         * 请求间隔（毫秒）
         */
        private Integer requestDelayMs = 200;

        /**
         * HTTP超时时间（毫秒）
         */
        private Integer timeoutMs = 30000;

        /**
         * 页间延迟（毫秒），避免对API造成过大压力
         */
        private Integer pageDelayMs = 1000;
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 最大重试次数
         */
        private Integer maxAttempts = 3;

        /**
         * 基础延迟时间（毫秒）
         */
        private Integer baseDelayMs = 1000;

        /**
         * 最大延迟时间（毫秒）
         */
        private Integer maxDelayMs = 10000;

        /**
         * 退避乘数
         */
        private Double backoffMultiplier = 2.0;

        /**
         * 429状态码的基础延迟（毫秒）
         */
        private Integer base429DelayMs = 5000;
    }

    /**
     * 数据校验配置
     */
    @Data
    public static class ValidationConfig {
        /**
         * 是否校验经纬度合法性
         */
        private Boolean checkLocation = true;

        /**
         * 是否校验必填字段
         */
        private Boolean checkRequiredFields = true;

        /**
         * 中国境内经纬度范围
         */
        private LocationBounds locationBounds = new LocationBounds();
    }

    /**
     * 经纬度边界配置
     */
    @Data
    public static class LocationBounds {
        /**
         * 最小经度
         */
        private Double minLng = 73.0;

        /**
         * 最大经度
         */
        private Double maxLng = 136.0;

        /**
         * 最小纬度
         */
        private Double minLat = 3.0;

        /**
         * 最大纬度
         */
        private Double maxLat = 54.0;
    }

    /**
     * 定时任务配置
     */
    @Data
    public static class ScheduleConfig {
        /**
         * POI采集Cron表达式（默认每月1号执行）
         */
        private String poiCollectCron = "0 0 0 1 * ?";

        /**
         * 城市代码同步Cron表达式（默认每3个月1号执行）
         */
        private String cityCodeCron = "0 0 0 1 */3 ?";

        /**
         * POI类型同步Cron表达式（默认每3个月1号执行）
         */
        private String poiTypeCron = "0 0 0 1 */3 ?";
    }
}
