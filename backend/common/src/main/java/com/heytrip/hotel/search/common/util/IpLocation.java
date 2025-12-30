package com.heytrip.hotel.search.common.util;

import com.heytrip.hotel.search.common.enums.CountryContinentEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP定位结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpLocation {
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 国家
     */
    private String country;
    
    /**
     * 国家代码
     */
    private String countryCode;
    
    /**
     * 国家枚举（含洲信息）
     */
    private CountryContinentEnum countryEnum;
    
    /**
     * 省份/州
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 纬度
     */
    private Double latitude;
    
    /**
     * 经度
     */
    private Double longitude;
    
    /**
     * 数据来源：amap, ipsb, ipapi, ipapis
     */
    private String source;
    
    /**
     * 是否解析成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
