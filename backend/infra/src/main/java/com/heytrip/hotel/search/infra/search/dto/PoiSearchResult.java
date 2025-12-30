package com.heytrip.hotel.search.infra.search.dto;

import com.heytrip.hotel.search.infra.search.doc.AmapPoiIndexDoc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POI搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoiSearchResult {
    
    /**
     * POI文档
     */
    private AmapPoiIndexDoc poi;
    
    /**
     * 距离（公里），仅在地理位置搜索时有值
     */
    private Double distance;
    
    /**
     * 搜索相关性评分
     */
    private Double score;
}
