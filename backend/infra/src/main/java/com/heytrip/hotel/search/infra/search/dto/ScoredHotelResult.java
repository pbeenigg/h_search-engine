package com.heytrip.hotel.search.infra.search.dto;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 带评分的酒店搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredHotelResult {
    
    /**
     * 酒店文档
     */
    private HotelIndexDoc hotel;
    
    /**
     * 关键词匹配得分（ES返回的_score）
     */
    private Double keywordScore;
    
    /**
     * 距离用户位置的距离（公里）
     */
    private Double distance;
    
    /**
     * 综合评分（0-100）
     */
    private Double finalScore;
    
    /**
     * 数据来源（hotel_index, poi_nearby等）
     */
    private String source;
}
