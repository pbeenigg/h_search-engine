package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * 地图数据同步日志实体
 * 用途：记录高德POI、城市代码、POI类型等数据的采集日志
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "map_data_sync_log", indexes = {
        @Index(name = "idx_source", columnList = "source"),
        @Index(name = "idx_data_type", columnList = "data_type"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_start_time", columnList = "start_time")
})
@DynamicUpdate
@Comment("地图数据同步日志表")
public class MapDataSyncLog {

    /**
     * 日志唯一标识（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("日志唯一标识")
    private Long id;

    /**
     * 数据来源（amap:高德地图, openStreetMap:开放街图）
     */
    @Column(name = "source", nullable = false, length = 50)
    @Comment("数据来源")
    private String source;

    /**
     * 数据类型（citycode:城市代码, poitype:POI类型, poi:POI数据）
     */
    @Column(name = "data_type", nullable = false, length = 50)
    @Comment("数据类型")
    private String dataType;

    /**
     * 总记录数
     */
    @Column(name = "total_count")
    @Comment("总记录数")
    private Long totalCount = 0L;

    /**
     * 成功记录数
     */
    @Column(name = "success_count")
    @Comment("成功记录数")
    private Long successCount = 0L;

    /**
     * 失败记录数
     */
    @Column(name = "failure_count")
    @Comment("失败记录数")
    private Long failureCount = 0L;

    /**
     * 开始时间
     */
    @Column(name = "start_time", nullable = false)
    @Comment("开始时间")
    private OffsetDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    @Comment("结束时间")
    private OffsetDateTime endTime;

    /**
     * 耗时（秒）
     */
    @Column(name = "duration")
    @Comment("耗时（秒）")
    private Integer duration;

    /**
     * 状态（RUNNING:运行中, SUCCESS:成功, FAILED:失败）
     */
    @Column(name = "status", nullable = false, length = 20)
    @Comment("状态")
    private String status;

    /**
     * 错误信息（如果有的话）
     */
    @Column(name = "error_message", columnDefinition = "text")
    @Comment("错误信息")
    private String errorMessage;

    /**
     * 追踪ID（用于关联同一批次的多个操作）
     */
    @Column(name = "trace_id", length = 100)
    @Comment("追踪ID")
    private String traceId;

    /**
     * 附加信息（JSON格式，存储额外的上下文信息）
     */
    @Column(name = "extra_info", columnDefinition = "text")
    @Comment("附加信息")
    private String extraInfo;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("创建时间")
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    @Comment("更新时间")
    private OffsetDateTime updatedAt;

    /**
     * 保存前自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.startTime == null) {
            this.startTime = now;
        }
    }

    /**
     * 更新前自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
