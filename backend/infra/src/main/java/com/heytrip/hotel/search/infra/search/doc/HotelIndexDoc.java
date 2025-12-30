package com.heytrip.hotel.search.infra.search.doc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ES 酒店索引文档
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Setting(useServerConfiguration = false, shards = 1, replicas = 1, refreshInterval = "5s", 
        settingPath = "es/elasticsearch-settings.json")
@Document(indexName = "hotels_read") // 默认读别名，实际运行时通过配置动态指定
public class HotelIndexDoc extends  HotelSearchResultDoc {

    /**
     * 文档ID，格式：{tagSource}_{hotelId}
     */
    @Id
    private String id;

    /**
     * 业务域（CN|INTL|HMT）
     */
    @Field(type = FieldType.Keyword)
    private String tagSource;

    /**
     * 数据提供商（Elong/Agoda/Booking 等）
     */
    @Field(type = FieldType.Keyword)
    private String providerSource;

    /**
     * 酒店ID
     */
    @Field(type = FieldType.Long)
    private Long hotelId;


    /**
     * 酒店名称（中文）
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "cn_ik_max_syn", searchAnalyzer = "cn_ik_smart_syn"),
            otherFields = {
                    @InnerField(suffix = "pinyin", type = FieldType.Text, analyzer = "pinyin_analyzer", searchAnalyzer = "pinyin_analyzer"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String nameCn;


    /**
     * 酒店名称（英文）
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "std_lc", searchAnalyzer = "std_lc"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String nameEn;

    /**
     * 国家名称（中文）
     */
    @Field(type = FieldType.Keyword)
    private String countryCn;

    /**
     * 国家名称（英文）
     */
    @Field(type = FieldType.Keyword)
    private String countryEn;

    /**
     * 国家代码（ISO Alpha-2）
     */
    @Field(type = FieldType.Keyword)
    private String countryCode;

    /**
     * 城市名称 （中文）
     */
    @Field(type = FieldType.Keyword)
    private String cityCn;

    /**
     * 城市名称（英文）
     */
    @Field(type = FieldType.Keyword)
    private String cityEn;

    /**
     * 省/州名称（中文）
     */
    @Field(type = FieldType.Keyword)
    private String regionCn;

    /**
     * 省/州名称（英文）
     */
    @Field(type = FieldType.Keyword)
    private String regionEn;

    /**
     * 大陆/洲名称（中文）
     */
    @Field(type = FieldType.Keyword)
    private String continentCn;

    /**
     * 大陆/洲名称（英文）
     */
    @Field(type = FieldType.Keyword)
    private String continentEn;

    /**
     * 地址（中文）
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "cn_ik_max_syn", searchAnalyzer = "cn_ik_smart_syn"),
            otherFields = {
                    @InnerField(suffix = "pinyin", type = FieldType.Text, analyzer = "pinyin_analyzer", searchAnalyzer = "pinyin_analyzer")
            }
    )
    private String addressCn;

    /**
     * 地址（英文）
     */
    @Field(type = FieldType.Text, analyzer = "std_lc", searchAnalyzer = "std_lc")
    private String addressEn;

    /**
     * 纬度（Google Lat）
     */
    @Field(type = FieldType.Double)
    private Double lat;

    /**
     * 经度 （Google Lon）
     */
    @Field(type = FieldType.Double)
    private Double lon;

    /**
     * 地理位置（geo_point）：用于距离过滤/排序；由 lat/lon 组合
     * ES 可能返回多种格式：
     * - 对象格式：{"lat": 31.24, "lon": 121.23}
     * - 字符串格式："31.24,121.23"
     * - 数组格式：[121.23, 31.24]
     * 使用 Object 类型兼容所有格式
     */
    @GeoPointField
    private Map<String,Object> location;

    /**
     * 酒店集团名称（中文）
     */
    @Field(type = FieldType.Keyword)
    private String groupCn;

    /**
     * 酒店集团名称（英文）
     */
    @Field(type = FieldType.Keyword)
    private String groupEn;

    /**
     * 品牌名称（中文）
     */
    @Field(type = FieldType.Keyword)
    private String brandCn;


    /**
     * 品牌名称（英文）
     */
    @Field(type = FieldType.Keyword)
    private String brandEn;


    /**
     * 酒店类型
     */
    @Field(type = FieldType.Keyword)
    private String accommodationType;

    /**
     * 是否可搜索  1=可搜索  0=不可搜索(前端搜索需要过滤掉)
     */
    @Field(type = FieldType.Keyword)
    private String searchEnable;

    /**
     * 电话
     */
    @Field(type = FieldType.Keyword)
    private String tel;

    /**
     * 分数
     */
    @Field(type = FieldType.Double)
    private Double score;

    /**
     * 更新时间（用于排序和时间范围查询）
     */
    @Field(type = FieldType.Date)
    private Date updatedAt;


    // ===== 以下为 衍生字段（索引时生成，查询时可用于过滤/召回/加权） =====

    /**
     * 酒店名称精细分词（用于 should 子句召回）
     */
    @Field(type = FieldType.Keyword)
    private List<String> nameTokens;

    /**
     * 地址精细分词（用于地名召回与匹配）
     */
    @Field(type = FieldType.Keyword)
    private List<String> addressTokens;

    /**
     * 名称关键词（TopK），用于权重更高的匹配
     */
    @Field(type = FieldType.Keyword)
    private List<String> nameKeywords;


    /**
     * 地点实体（NER）
     */
    @Field(type = FieldType.Keyword)
    private List<String> nerPlaces;

    /**
     * 品牌实体（NER）
     */
    @Field(type = FieldType.Keyword)
    private List<String> nerBrands;


    // ===== 增强字段 =====

    /**
     * 酒店名称繁体（简繁体转换）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String nameTraditional;

    /**
     * 地址繁体（简繁体转换）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String addressTraditional;

    /**
     * 品牌名称（基于自定义品牌词典识别）
     */
    @Field(type = FieldType.Keyword)
    private List<String> brandNames;

    /**
     * 地理层级路径（从大到小）
     * 示例：["亚洲", "中国", "上海", "浦东新区"]
     * 用于地理层级过滤和聚合
     */
    @Field(type = FieldType.Keyword)
    private List<String> geoHierarchy;



}
