package com.heytrip.hotel.search.ingest.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 对应 job_schedule.params 的配置对象（JSON）
 * 仅服务于 HotelIdsIngestRoute，按需扩展。
 */
@Data
public class JobScheduleParams {
    private Integer hotelDetailBatchSize;   // 详情请求子批大小，默认 20
    private Integer hotelIdsBatchSize;      // 单页ID拉取大小，默认 10000
    private Integer retryTimes;             // 重试次数，默认 3
    private Integer retryDelayMs;           // 基础重试延迟（非429），默认 3000ms
    private Integer retryJitterMs;          // 抖动范围，默认 2000ms
    private Integer retryBaseDelayMs429;    // 429 基础延迟，默认 1000ms
    private Integer batchSize;              // 提交批次大小（入库每批），默认 1000
    private Integer maxPages;               // 持续采集最大页数限制（熔断保护），默认 100
    private Integer maxRuntimeMinutes;      // 持续采集最大运行时间（分钟），默认 360（6小时）
    private Integer semaphoreWaitRetries;   // 信号量等待重试次数，默认 3
    private Integer pageDelayMs;            // 页间延迟（毫秒），默认 1000

    public static JobScheduleParams parse(ObjectMapper om, String json) {
        if (json == null || json.isBlank()) return new JobScheduleParams();
        try {
            return om.readValue(json, JobScheduleParams.class);
        } catch (Exception ignored) {
            return new JobScheduleParams();
        }
    }

    // 提供带默认值的读取方法，避免调用端散落默认
    public int detailBatchOrDefault(int def) { return hotelDetailBatchSize != null ? hotelDetailBatchSize : def; }
    public int idsBatchOrDefault(int def) { return hotelIdsBatchSize != null ? hotelIdsBatchSize : def; }
    public int retryTimesOrDefault(int def) { return retryTimes != null ? retryTimes : def; }
    public int retryDelayOrDefault(int def) { return retryDelayMs != null ? retryDelayMs : def; }
    public int retryJitterOrDefault(int def) { return retryJitterMs != null ? retryJitterMs : def; }
    public int retryBase429OrDefault(int def) { return retryBaseDelayMs429 != null ? retryBaseDelayMs429 : def; }
    public int batchSizeOrDefault(int def) { return batchSize != null ? batchSize : def; }
    public int maxPagesOrDefault(int def) { return maxPages != null ? maxPages : def; }
    public int maxRuntimeMinutesOrDefault(int def) { return maxRuntimeMinutes != null ? maxRuntimeMinutes : def; }
    public int semaphoreWaitRetriesOrDefault(int def) { return semaphoreWaitRetries != null ? semaphoreWaitRetries : def; }
    public int pageDelayOrDefault(int def) { return pageDelayMs != null ? pageDelayMs : def; }
}
