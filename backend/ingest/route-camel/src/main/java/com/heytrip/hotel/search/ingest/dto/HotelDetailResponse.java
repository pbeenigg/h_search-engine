package com.heytrip.hotel.search.ingest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelDetailResponse {
    private List<Item> data;
    private Boolean hasData;
    private Integer code;
    private Integer bizCode;
    private String message;
    private Boolean isOk;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Long hotelId;
        private String origContent; // 原始内容（JSON字符串）



        ///////////  标准酒店库 人工修正字段  ///////////
        /// 如果newHotelNameCn，newHotelNameEn不为空，说明人工修改过酒店名称 ;
        /// 酒店名称（中文）newHotelNameCn
        /// 酒店名称（英文）newHotelNameEn
        /// 酒店类型 AccommodationType
        /// 可搜索 SearchEnable   1=可搜索  0=不可搜索(需要过滤掉)
        /// 国家二字码 CountryIso2
        /// 国家名称英文 CountryNameEn
        /// 城市名称英文 CityNameEn
        /// 城市名称 CityName
        /// 地址 Address
        /// 地址英文 AddressEn
        /// 电话 Tel
        /// 分数 Score
        private String newHotelNameCn;
        private String newHotelNameEn;
        private String accommodationType;
        private String searchEnable;
        private String countryIso2;
        private String countryNameEn;
        private String cityNameEn;
        private String cityName;
        private String address;
        private String addressEn;
        private String tel;
        private String score;
        ////////////////////////////////////////////////

    }
}
