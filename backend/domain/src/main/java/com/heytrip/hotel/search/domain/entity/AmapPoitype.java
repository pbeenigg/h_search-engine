package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * 高德POI类型编码实体
 * 用途：存储高德地图的POI分类编码信息（大类、中类、小类）
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "amap_poitype", indexes = {
        @Index(name = "idx_typecode", columnList = "typecode")
})
@DynamicUpdate
@Comment("高德POI类型代码表")
public class AmapPoitype {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    /**
     * POI类型代码（如：141201）
     */
    @Column(name = "typecode", nullable = false, length = 20)
    @Comment("POI类型代码")
    private String typecode;

    /**
     * 大类中文名称（如：科教文化服务）
     */
    @Column(name = "big_category_cn", length = 100)
    @Comment("大类中文名称")
    private String bigCategoryCn;

    /**
     * 中类中文名称（如：学校）
     */
    @Column(name = "mid_category_cn", length = 100)
    @Comment("中类中文名称")
    private String midCategoryCn;

    /**
     * 小类中文名称（如：高等院校）
     */
    @Column(name = "sub_category_cn", length = 100)
    @Comment("小类中文名称")
    private String subCategoryCn;

    /**
     * 大类英文名称（如：Science/Culture & Education）
     */
    @Column(name = "big_category_en", length = 100)
    @Comment("大类英文名称")
    private String bigCategoryEn;

    /**
     * 中类英文名称（如：School）
     */
    @Column(name = "mid_category_en", length = 100)
    @Comment("中类英文名称")
    private String midCategoryEn;

    /**
     * 小类英文名称（如：Higher Education）
     */
    @Column(name = "sub_category_en", length = 100)
    @Comment("小类英文名称")
    private String subCategoryEn;

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
