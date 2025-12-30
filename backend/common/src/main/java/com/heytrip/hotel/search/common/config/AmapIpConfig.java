package com.heytrip.hotel.search.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 高德IP定位配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "amap.ip")
public class AmapIpConfig {
    
    /**
     * 高德IP定位API地址
     */
    private String baseUrl;
    
    /**
     * 高德API Key
     */
    private String key;

    /**
     * GeoLite2数据库路径
     */
    private String geolite2;
}
