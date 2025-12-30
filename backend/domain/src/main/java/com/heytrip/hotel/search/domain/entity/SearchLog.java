package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 搜索日志实体
 * 用途：记录用户搜索行为，用于搜索质量分析和优化
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "search_logs", indexes = {
        @Index(name = "idx_search_logs_query", columnList = "query"),
        @Index(name = "idx_search_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_search_logs_result_count", columnList = "result_count"),
        @Index(name = "idx_search_logs_tag_source", columnList = "tag_source")
})
@Comment("搜索日志（用于搜索质量分析）")
public class SearchLog {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户查询词
     */
    @Column(name = "query", nullable = false, length = 500)
    @Comment("用户查询词")
    private String query;

    /**
     * 业务域（CN/INTL/HMT）
     */
    @Column(name = "tag_source", length = 32)
    @Comment("业务域")
    private String tagSource;

    /**
     * 搜索结果数量
     */
    @Column(name = "result_count")
    @Comment("搜索结果数量")
    private Integer resultCount;

    /**
     * 用户点击的酒店ID
     */
    @Column(name = "clicked_hotel_id")
    @Comment("点击的酒店ID")
    private Long clickedHotelId;

    /**
     * 点击位置（排名）
     */
    @Column(name = "click_position")
    @Comment("点击位置")
    private Integer clickPosition;

    /**
     * 查询耗时（毫秒）
     */
    @Column(name = "duration_ms")
    @Comment("查询耗时（毫秒）")
    private Long durationMs;

    /**
     * 用户ID（可选）
     */
    @Column(name = "user_id")
    @Comment("用户ID")
    private Long userId;

    /**
     * 用户IP地址
     */
    @Column(name = "user_ip", length = 64)
    @Comment("用户IP")
    private String userIp;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    @Comment("创建时间")
    private OffsetDateTime createdAt;
}
