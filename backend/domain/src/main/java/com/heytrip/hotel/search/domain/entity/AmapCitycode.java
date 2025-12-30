package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * 高德城市代码实体
 * 用途：存储高德地图的城市、区县行政区划代码
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "amap_citycode", indexes = {
        @Index(name = "idx_citycode", columnList = "citycode"),
        @Index(name = "idx_adcode", columnList = "adcode"),
        @Index(name = "idx_name", columnList = "name")
})
@DynamicUpdate
@Comment("高德城市代码表")
public class AmapCitycode {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    /**
     * 名称（如：北京市、东城区）
     */
    @Column(name = "name", nullable = false, length = 100)
    @Comment("名称")
    private String name;

    /**
     * 区县代码（如：110101）
     */
    @Column(name = "adcode", nullable = false, length = 10)
    @Comment("区县代码")
    private String adcode;

    /**
     * 城市代码（如：010），注意：某些地区可能为NULL
     */
    @Column(name = "citycode", length = 10)
    @Comment("城市代码")
    private String citycode;

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
    }

    /**
     * 更新前自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
