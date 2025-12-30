package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * 同步日志汇总实体
 * 用途：记录每次采集/补偿任务的总体数据与状态，便于统计与追踪
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sync_log",
       uniqueConstraints = {@UniqueConstraint(name = "uk_sync_log_code_trace", columnNames = {"job_code", "trace_id"})})
@DynamicUpdate
@Comment("同步日志汇总（全局计数器）")
public class SyncLog {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务编码（如：HOTEL_FULL_SYNC_ALL） */
    @Column(name = "job_code", nullable = false, length = 64)
    private String jobCode;

    /** 本次运行的唯一追踪ID（用于跨表关联） */
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    /** 开始时间 */
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    /** 结束时间 */
    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    /** 总耗时（秒） */
    @Column(name = "cost_seconds")
    private Integer costSeconds;

    /** 拉取到的酒店ID总数 */
    @Column(name = "total_ids")
    private Long totalIds;

    /** 成功解析并落地的酒店详情数 */
    @Column(name = "total_details")
    private Long totalDetails;

    /** 成功记录数（可与 total_details 同步，或用于其他阶段成功数） */
    @Column(name = "success_count")
    private Long successCount;

    /** 失败记录数（详情拉取/解析/入库失败等） */
    @Column(name = "fail_count")
    private Long failCount;

    /** 状态：RUNNING / SUCCESS / FAILED / PARTIAL */
    @Column(name = "status", length = 20)
    private String status; // RUNNING/SUCCESS/FAILED/PARTIAL

    /** 附加信息（错误摘要/备注） */
    @Column(name = "message")
    private String message;
}
