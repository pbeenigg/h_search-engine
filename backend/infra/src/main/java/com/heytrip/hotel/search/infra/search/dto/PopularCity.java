package com.heytrip.hotel.search.infra.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 热门城市坐标信息
 */
@Data
@AllArgsConstructor
public class PopularCity {

    private String nameCn;
    private String nameEn;
    private String countryCn;
    private String countryEn;
    private double lat;
    private double lon;
}
