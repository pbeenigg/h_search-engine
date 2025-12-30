package com.heytrip.hotel.search.infra.search.dto;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 酒店地理位置搜索结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelGeoResult {
    /**
     * 酒店信息
     */
    private HotelIndexDoc hotel;

    /**
     * 距离（公里）
     */
    private Double distanceKm;

    /**
     * 距离文本（如 "1.2km"、"850m"）
     */
    public String getDistanceText() {
        if (distanceKm == null) {
            return "未知";
        }
        if (distanceKm < 1.0) {
            return String.format("%.0fm", distanceKm * 1000);
        } else {
            return String.format("%.1fkm", distanceKm);
        }
    }
    
    /**
     * 参考POI名称
     */
    private String referencePoint;
    
    /**
     * 搜索类型
     */
    private String searchType;
    

    
    /**
     * 构造方法（兼容原有代码）
     */
    public HotelGeoResult(HotelIndexDoc hotel, Double distanceKm) {
        this.hotel = hotel;
        this.distanceKm = distanceKm;
        this.searchType = "GEO_SEARCH";
    }
}
