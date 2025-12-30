package com.heytrip.hotel.search.infra.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 热门酒店
 */
@Data
@AllArgsConstructor
public class PopularHotels {
    private String cityCn;
    private String cityEn;

    private String countryCn;
    private String countryEn;
    private String nameCn;
    private String nameEn;
    private double lat;
    private double lon;
}
