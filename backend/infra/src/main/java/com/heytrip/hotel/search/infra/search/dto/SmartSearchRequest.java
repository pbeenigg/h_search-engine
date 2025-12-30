package com.heytrip.hotel.search.infra.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能搜索请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartSearchRequest {
    
    /**
     * 搜索关键词（可选）
     */
    private String keyword;

    /**
     * 业务域（可选：CN=中国大陆/INTL=国际/HMT=港澳台）
     */
    private String tag;
    
    /**
     * 用户IP地址（用于定位）
     */
    private String userIp;
    
    /**
     * 用户指定的纬度（优先级高于IP定位）
     */
    private Double userLat;
    
    /**
     * 用户指定的经度（优先级高于IP定位）
     */
    private Double userLon;
    
    /**
     * 用户指定的国家（可选）
     */
    private String country;
    
    /**
     * 用户指定的城市（可选）
     */
    private String city;
    
    /**
     * 返回数量（默认5条）
     */
    private Integer size;
    
    /**
     * 分页起始位置（默认0）
     */
    private Integer from;
    
    /**
     * 用户ID（可选，用于日志记录）
     */
    private Long userId;
}
