package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


/**
 * 酒店详情实体
 * 用途：存放采集到的酒店原文与基础解析字段（名称等），作为后续清洗与入库的中间层。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hotels",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hotels_source_hotel", columnNames = {"source", "hotel_id"})
        })
@DynamicUpdate
@Comment("酒店详情实体（保留原文与基础字段）")
public class Hotels {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 数据源：Elong=艺龙 ；Agoda=安可達
     */
    @Column(name = "source", nullable = false, length = 32)
    private String source;


    /**
     * 标签源：CN=国内；INTL= 国际； HMT = 港澳台
     */
    @Column(name = "tag_source", nullable = false, length = 32)
    private String tagSource;

    /**
     * 酒店ID（供应商体系内的 hotelId）
     */
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    /**
     * 酒店原文压缩后存储（Base64 编码的 GZIP 压缩字符串）
     */
    @Column(name = "raw_compressed", nullable = false, columnDefinition = "text")
    private String rawCompressed;

    /**
     * 中文名（从 origContent 解析，或后续翻译得到）
     */
    @Column(name = "hotel_name_cn")
    private String hotelNameCn;

    /**
     * 英文名（从 origContent 解析）
     */
    @Column(name = "hotel_name_en")
    private String hotelNameEn;

    /**
     * 国家（中文）
     */
    @Column(name = "country_cn")
    private String countryCn;

    /**
     * 国家（英文）
     */
    @Column(name = "country_en")
    private String countryEn;

    /**
     * 国家代码（如 CN/HK/MO/TW/US 等）
     */
    @Column(name = "country_code")
    private String countryCode;

    /**
     * 城市名称
     */
    @Column(name = "city_cn")
    private String cityCn;


    /**
     * 城市名称（英文）
     */
    @Column(name = "city_en")
    private String cityEn;

    /**
     * 区域/区县
     */
    @Column(name = "region_cn")
    private String regionCn;

    /**
     * 区域/区县（英文）
     */
    @Column(name = "region_en")
    private String regionEn;

    /**
     * 洲区域（如 亚洲）
     */
    @Column(name = "continent_cn")
    private String continentCn;

    /**
     * 洲区域（如 Asia）（英文）
     */
    @Column(name = "continent_en")
    private String continentEn;

    /**
     * 地址原文
     */
    @Column(name = "address_cn")
    private String addressCn;


    /**
     * 地址原文（英文）
     */
    @Column(name = "address_en")
    private String addressEn;

    /**
     * 经度 （Google 经度）
     */
    @Column(name = "longitude")
    private BigDecimal longitude;

    /**
     * 纬度  （Google 维度）
     */
    @Column(name = "latitude")
    private BigDecimal latitude;

    /**
     * 酒店集团
     */
    @Column(name = "hotel_group_cn")
    private String hotelGroupCn;

    /**
     * 酒店集团（英文）
     */
    @Column(name = "hotel_group_en")
    private String hotelGroupEn;

    /**
     * 酒店品牌
     */
    @Column(name = "hotel_brand_cn")
    private String hotelBrandCn;

    /**
     * 酒店品牌（英文））
     */
    @Column(name = "hotel_brand_en")
    private String hotelBrandEn;


    /**
     * 酒店描述 （中文）
     */
    @Column(name = "description_cn")
    private String descriptionCn;

    /**
     * 酒店描述 （英文）
     */
    @Column(name = "description_en")
    private String descriptionEn;


    /**
     * 新中文名（标准酒店库 人工修正字段）
     */
    @Column(name = "new_hotel_name_cn")
    private String newHotelNameCn;

    /**
     * 新英文名（标准酒店库 人工修正字段）
     */
    @Column(name = "new_hotel_name_en")
    private String newHotelNameEn;

    /**
     * 酒店类型  （标准酒店库 人工修正字段）
     */
    @Column(name = "accommodation_type")
    private String accommodationType;

    /**
     * 是否可搜索  1=可搜索  0=不可搜索(前端搜索需要过滤掉) （标准酒店库 人工修正字段）
     */
    @Column(name = "search_enable")
    private String searchEnable;

    /**
     * 电话 （标准酒店库 人工修正字段）
     */
    @Column(name = "tel")
    private String tel;

    /**
     * 分数 （标准酒店库 人工修正字段）
     */
    @Column(name = "score")
    private String score;

    /**
     * 抓取时间（入库时间戳）
     */
    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;



    /**
     * 最后更新时间
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


    /**
     * 保存前自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.fetchedAt = now;
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
