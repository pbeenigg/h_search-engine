package com.heytrip.hotel.search.infra.search.doc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;
import java.util.Map;

/**
 * 高德POI ES索引文档
 * 用途：地标识别、附近酒店定位
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "amap_poi")
@Setting(shards = 3, replicas = 1, refreshInterval = "5s", settingPath = "es/amap-poi-setting.json")
public class AmapPoiIndexDoc {
    
    /**
     * POI唯一标识
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String id;
    
    /**
     * 地标名称
     * 使用ik_smart分词器，支持中文全文搜索
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String name;
    
    /**
     * POI类型（如：科教文化服务;学校;高等院校）
     * 使用ik_smart分词器
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String type;
    
    /**
     * POI类型代码
     */
    @Field(type = FieldType.Keyword)
    private String typecode;
    
    /**
     * 地址
     * 使用ik_smart分词器，支持中文地址搜索
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String address;
    
    /**
     * 经纬度坐标 (geo_point格式)
     * 格式: { "lat": 23.158017, "lon": 113.357553 }
     * 用于地理位置搜索（如：附近5km的POI）
     */
    @GeoPointField
    private Map<String, Object> location;
    
    /**
     * 经度（用于显示）
     */
    @Field(type = FieldType.Double)
    private Double lon;
    
    /**
     * 纬度（用于显示）
     */
    @Field(type = FieldType.Double)
    private Double lat;
    
    /**
     * 城市代码
     */
    @Field(type = FieldType.Keyword)
    private String citycode;
    
    /**
     * 城市名称
     */
    @Field(type = FieldType.Keyword)
    private String cityname;
    
    /**
     * 区县代码
     */
    @Field(type = FieldType.Keyword)
    private String adcode;
    
    /**
     * 区县名称
     */
    @Field(type = FieldType.Keyword)
    private String adname;
    
    /**
     * 省份代码
     */
    @Field(type = FieldType.Keyword)
    private String pcode;
    
    /**
     * 省份名称
     */
    @Field(type = FieldType.Keyword)
    private String pname;
    
    /**
     * 采集批次
     */
    @Field(type = FieldType.Keyword)
    private String sourceBatch;

    // ===== 以下为 衍生字段（索引时生成，查询时可用于过滤/召回/加权） =====
    /**
     * POI名称精细分词（用于 should 子句召回）
     */
    @Field(type = FieldType.Keyword)
    private List<String> nameTokens;

    /**
     * POI类型 精细分词（用于POI类型召回与匹配）
     */
    @Field(type = FieldType.Keyword)
    private List<String> typeTokens;
}
