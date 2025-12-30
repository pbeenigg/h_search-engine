package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * 任务运行时状态实体
 * 用途：记录采集任务的水位线断点、最近一次运行状态与时间点
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_runtime_state")
@DynamicUpdate
@Comment("任务运行时状态（水位线断点等）")
public class JobRuntimeState {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务编码（唯一），如：HOTEL_FULL_SYNC_ALL */
    @Column(name = "job_code", nullable = false, unique = true, length = 64)
    private String jobCode;

    /** 水位线：已处理的最大 hotel_id（下一次从该值之后继续） */
    @Column(name = "watermark_max_hotel_id", nullable = false)
    private Long watermarkMaxHotelId;

    /** 最近一次任务开始时间 */
    @Column(name = "last_started_at")
    private OffsetDateTime lastStartedAt;

    /** 最近一次任务结束时间 */
    @Column(name = "last_finished_at")
    private OffsetDateTime lastFinishedAt;

    /** 最近一次运行状态：SUCCESS/FAILED/RUNNING */
    @Column(name = "last_status", length = 20)
    private String lastStatus; // SUCCESS/FAILED/RUNNING

    /** 更新时间 */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
