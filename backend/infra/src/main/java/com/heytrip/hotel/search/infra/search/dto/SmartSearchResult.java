package com.heytrip.hotel.search.infra.search.dto;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartSearchResult {
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 总结果数
     */
    private Long total;
    
    /**
     * 酒店列表
     */
    private List<HotelIndexDoc> hotels;
    
    /**
     * 实际搜索使用的纬度
     */
    private Double searchLat;
    
    /**
     * 实际搜索使用的经度
     */
    private Double searchLon;
    
    /**
     * 实际搜索使用的国家
     */
    private String searchCountry;
    
    /**
     * 实际搜索使用的城市
     */
    private String searchCity;
    
    /**
     * 搜索耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 识别出的国家（基于最高分结果）
     */
    private String detectedCountry;
    
    /**
     * 识别出的区域/省份（基于最高分结果）
     */
    private String detectedRegion;
    
    /**
     * 识别出的城市（基于最高分结果）
     */
    private String detectedCity;
}
