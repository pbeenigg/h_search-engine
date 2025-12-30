package com.heytrip.hotel.search.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 搜索日志参数配置（可通过 application.yml 覆盖）
 * heytrip.search-log.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "heytrip.search-log")
public class HotelSearchLogs {

    /**
     *   # 搜索日志配置
     *   search-log:
     *     enabled: true
     *     # Redis Stream 配置
     *     stream-key: search:logs:stream
     *     consumer-group: search-log-consumer-group
     *     consumer-name: search-log-consumer-1
     *
     *     # 批量消费配置
     *     batch-size: 500           # 每次最多读取500条
     *     poll-interval-ms: 2000    # 每2秒轮询一次
     *     block-ms: 100             # 阻塞等待100ms（避免空轮询）
     *
     *     # 告警配置
     *     zero-result-threshold: 0.15      # 零结果率阈值 15%
     *     min-queries-for-alert: 100       # 最小查询数（避免低流量误报）
     *
     *     # Redis Stream 维护配置
     *     max-stream-length: 100000        # Stream 最大长度（超过后自动清理）
     *     message-ttl-hours: 72
     */


    private boolean enabled = true;

    // Redis Stream 配置
    private String streamKey = "search:logs:stream";
    private String consumerGroup = "search-log-consumer-group";
    private String consumerName = "search-log-consumer-1";
    // 批量消费配置
    private int batchSize = 500;
    private int pollIntervalMs = 2000;
    private int blockMs = 100;
    // 告警配置
    private double zeroResultThreshold = 0.15;
    private int minQueriesForAlert = 100;

    // Redis Stream 维护配置
    private int maxStreamLength = 100000;
    private int messageTtlHours = 72;




}
