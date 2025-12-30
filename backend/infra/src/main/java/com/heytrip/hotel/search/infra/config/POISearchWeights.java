package com.heytrip.hotel.search.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * POI权重与参数配置
 * heytrip.search.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "heytrip.search.poi")
public class POISearchWeights {

    // should 通道权重 - 优化后配置，提升搜索精准度
    private float nameCnBoost;
    private float nameEnBoost;
    private float addressBoost;
    private float addressTokensBoost;
    private float latLngBoost ;


    // 索引配置
    private String readAlias = "amap_poi_read";
    private String writeAlias = "amap_poi_write";

}
