package com.heytrip.hotel.search.ingest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 高德POI项目DTO
 */
@Data
public class AmapPoiItem {

    /**
     * POI唯一标识
     */
    private String id;

    /**
     * POI名称
     */
    private String name;

    /**
     * POI类型（如：科教文化服务;学校;高等院校）
     */
    private String type;

    /**
     * POI类型代码
     */
    private String typecode;

    /**
     * 地址
     */
    private String address;

    /**
     * 经纬度坐标（格式：lng,lat）
     */
    private String location;

    /**
     * 省份代码
     */
    private String pcode;

    /**
     * 省份名称
     */
    private String pname;

    /**
     * 城市代码
     */
    private String citycode;

    /**
     * 城市名称
     */
    private String cityname;

    /**
     * 区县代码
     */
    private String adcode;

    /**
     * 区县名称
     */
    private String adname;

    /**
     * 上级行政区划
     */
    private String parent;

    /**
     * 距离（可选）
     */
    private String distance;

    /**
     * 子POI信息列表
     */
    private List<ChildPoi> children;

    /**
     * 商业信息
     */
    private Business business;

    /**
     * 子POI信息
     */
    @Data
    public static class ChildPoi {
        /**
         * 子POI唯一标识
         */
        private String id;

        /**
         * 子POI名称
         */
        private String name;

        /**
         * 子POI所属类型
         */
        private String subtype;

        /**
         * 子POI分类编码
         */
        private String typecode;

        /**
         * 子POI详细地址
         */
        private String address;

        /**
         * 子POI经纬度
         */
        private String location;

        /**
         * 子POI分类信息
         */
        private String sname;
    }

    /**
     * POI商业信息
     */
    @Data
    public static class Business {
        /**
         * POI所属商圈
         */
        @JsonProperty("business_area")
        private String businessArea;

        /**
         * POI评分
         */
        private String rating;

        /**
         * POI人均消费
         */
        private String cost;

        /**
         * POI标识，用于确认POI信息类型
         */
        private String keytag;

        /**
         * 用于再次确认信息类型
         */
        private String rectag;
    }
}
