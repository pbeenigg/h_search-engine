package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * 同步失败明细实体
 * 用途：记录采集流程中各阶段的单条失败记录，便于排查与按批次重放。
 * 阶段（stage）建议值：
 * - IDS_FETCH    在线ID拉取失败
 * - DETAIL_FETCH 详情拉取或解析失败
 * - TRANSFORM    转换/清洗失败
 * - SINK         入库失败
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sync_log_detail")
@DynamicUpdate
@Comment("同步失败明细（支持按 sync_log_id 重跑补偿）")
public class SyncLogDetail {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 汇总日志ID（外键，关联 sync_log.id） */
    @Column(name = "sync_log_id", nullable = false)
    private Long syncLogId;

    /** 失败记录对应的酒店ID（可为空） */
    @Column(name = "hotel_id")
    private Long hotelId;

    /** 失败发生阶段（IDS_FETCH/DETAIL_FETCH/TRANSFORM/SINK） */
    @Column(name = "stage", length = 32)
    private String stage; // IDS_FETCH / DETAIL_FETCH / TRANSFORM / SINK

    /** 错误码（如：HTTP_429、PARSE_ERROR、DB_SAVE_ERROR 等） */
    @Column(name = "error_code", length = 64)
    private String errorCode;

    /** 错误信息摘要（必要时可截断） */
    @Column(name = "error_message")
    private String errorMessage;

    /** 记录时间 */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
