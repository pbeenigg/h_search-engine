package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;


/**
 * 任务调度配置实体
 * 用于描述基于数据库驱动的 Cron 调度计划与限流/批量等参数
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_schedule")
@DynamicUpdate
@Comment("任务调度配置（DB驱动Cron），含业务参数与同步范围")
public class JobSchedule {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务编码（唯一），如：HOTEL_FULL_SYNC_ALL */
    @Column(name = "job_code", nullable = false, unique = true, length = 64)
    private String jobCode;

    /** Cron 表达式（Quartz规范），例如：0 0 2 1 * ? 表示每月1日02:00 */
    @Column(name = "cron_expr", nullable = false, length = 64)
    private String cronExpr;

    /** 是否启用本任务 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 最大并发（全局许可数），接口约束 ≤ 10 */
    @Column(name = "max_concurrency", nullable = false)
    @ColumnDefault("10")
    private Integer maxConcurrency = 10;

    /** 入库提交批次大小，默认 1000 */
    @Column(name = "batch_size", nullable = false)
    @ColumnDefault("1000")
    private Integer batchSize = 1000;

    /** HTTP 调用超时时间（秒），默认 20 */
    @Column(name = "http_timeout_sec", nullable = false)
    @ColumnDefault("20")
    private Integer httpTimeoutSec = 20;

    /** 任务运行时业务参数（JSON），例如：{"syncHotelDetail":true,"syncHotelName":true} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", nullable = false, columnDefinition = "JSON")
    private String params = "{}";

    /** 同步模式：FULL=全量；LIMIT=限定条数 */
    @Column(name = "sync_mode", nullable = false, length = 16)
    private String syncMode = "FULL"; // FULL / LIMIT

    /** 当 syncMode=LIMIT 时的条数上限（0 表示不限制） */
    @Column(name = "sync_limit", nullable = false)
    private Long syncLimit = 0L;

    /** 备注说明 */
    @Column(name = "remark", length = 255)
    private String remark;

    /** 更新时间 */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


    /** 创建时间 */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
