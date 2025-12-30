package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 高德POI数据实体
 * 用途：存储从高德地图API采集的POI地标数据
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "amap_poi", indexes = {
        @Index(name = "idx_citycode", columnList = "citycode"),
        @Index(name = "idx_adcode", columnList = "adcode"),
        @Index(name = "idx_typecode", columnList = "typecode"),
        @Index(name = "idx_location", columnList = "longitude, latitude"),
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_source_batch", columnList = "source_batch")
})
@DynamicUpdate
@Comment("高德POI数据表")
public class AmapPoi {

    /**
     * POI唯一标识（高德API返回的id，如：B00140TVEV）
     */
    @Id
    @Column(name = "id", length = 50)
    @Comment("POI唯一标识")
    private String id;

    /**
     * 地标名称
     */
    @Column(name = "name", nullable = false)
    @Comment("地标名称")
    private String name;

    /**
     * POI类型（如：科教文化服务;学校;高等院校）
     */
    @Column(name = "type")
    @Comment("POI类型")
    private String type;

    /**
     * POI类型代码（如：141201）
     */
    @Column(name = "typecode", length = 20)
    @Comment("POI类型代码")
    private String typecode;

    /**
     * 地址
     */
    @Column(name = "address", length = 500)
    @Comment("地址")
    private String address;

    /**
     * 经纬度坐标 格式:lng,lat（如：113.357553,23.158017）
     */
    @Column(name = "location", length = 50)
    @Comment("经纬度坐标")
    private String location;

    /**
     * 经度（拆分便于空间查询）
     */
    @Column(name = "longitude", precision = 10, scale = 6)
    @Comment("经度")
    private BigDecimal longitude;

    /**
     * 纬度（拆分便于空间查询）
     */
    @Column(name = "latitude", precision = 9, scale = 6)
    @Comment("纬度")
    private BigDecimal latitude;

    /**
     * 省份代码（如：440000）
     */
    @Column(name = "pcode", length = 10)
    @Comment("省份代码")
    private String pcode;

    /**
     * 省份名称（如：广东省）
     */
    @Column(name = "pname", length = 50)
    @Comment("省份名称")
    private String pname;

    /**
     * 城市代码（如：020）
     */
    @Column(name = "citycode", length = 10)
    @Comment("城市代码")
    private String citycode;

    /**
     * 城市名称（如：广州市）
     */
    @Column(name = "cityname", length = 50)
    @Comment("城市名称")
    private String cityname;

    /**
     * 区县代码（如：440106）
     */
    @Column(name = "adcode", length = 10)
    @Comment("区县代码")
    private String adcode;

    /**
     * 区县名称（如：天河区）
     */
    @Column(name = "adname", length = 50)
    @Comment("区县名称")
    private String adname;

    /**
     * 上级行政区划
     */
    @Column(name = "parent", length = 50)
    @Comment("上级行政区划")
    private String parent;

    /**
     * 距离（可选，某些查询场景返回）
     */
    @Column(name = "distance", length = 50)
    @Comment("距离")
    private String distance;

    /**
     * 数据版本号（用于标识数据的版本）
     */
    @Column(name = "data_version", length = 20)
    @Comment("数据版本号")
    private String dataVersion;

    /**
     * 采集批次标识（格式：batch_yyyyMMdd_HHmmss）
     */
    @Column(name = "source_batch", length = 50)
    @Comment("采集批次标识")
    private String sourceBatch;

    /**
     * 数据哈希值（MD5，用于快速变更检测）
     * 计算字段：name + type + address + location + citycode + adcode
     */
    @Column(name = "data_hash", length = 200)
    @Comment("数据哈希值MD5")
    private String dataHash;

    /**
     * 逻辑删除标识（false:未删除, true:已删除）
     */
    @Column(name = "is_deleted", columnDefinition = "TINYINT(1)")
    @Comment("逻辑删除标识")
    private Boolean isDeleted = false;

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
