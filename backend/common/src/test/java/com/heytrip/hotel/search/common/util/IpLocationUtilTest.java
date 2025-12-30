package com.heytrip.hotel.search.common.util;

import com.heytrip.hotel.search.common.config.AmapIpConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IP定位工具测试
 */
class IpLocationUtilTest {
    
    private IpLocationUtil ipLocationUtil;
    
    @BeforeEach
    void setUp() {
        // 手动创建配置对象
        AmapIpConfig amapIpConfig = new AmapIpConfig();
        amapIpConfig.setBaseUrl("https://restapi.amap.com/v3/ip");
        amapIpConfig.setKey("cd1e05524701a9985c9ffb00a03de316");
        
        // 创建工具类实例
        ipLocationUtil = new IpLocationUtil(amapIpConfig);
    }
    
    /**
     * 测试国内IP解析（应使用高德API）
     */
    @Test
    void testLocateChinaIp() {
        // 测试广州电信IP
        IpLocation result = ipLocationUtil.locate("113.108.208.1");
        
        assertNotNull(result);
        assertTrue(result.getSuccess(), "解析应该成功");
        assertEquals("amap", result.getSource(), "国内IP应使用高德API");
        assertNotNull(result.getCity(), "城市不应为空");
        assertNotNull(result.getCountryEnum(), "国家枚举不应为空");
        assertNotNull(result.getLatitude(), "纬度不应为空");
        assertNotNull(result.getLongitude(), "经度不应为空");
        
        System.out.println("国内IP解析结果：");
        System.out.println("IP: " + result.getIp());
        System.out.println("国家: " + result.getCountry());
        System.out.println("国家代码: " + result.getCountryCode());
        System.out.println("国家枚举: " + result.getCountryEnum());
        System.out.println("所属洲: " + (result.getCountryEnum() != null ? result.getCountryEnum().getContinent() : "未知"));
        System.out.println("省份: " + result.getProvince());
        System.out.println("城市: " + result.getCity());
        System.out.println("坐标: (" + result.getLatitude() + ", " + result.getLongitude() + ")");
        System.out.println("来源: " + result.getSource());
    }
    
    /**
     * 测试国外IP解析（应使用备用API）
     */
    @Test
    void testLocateForeignIp() {
        // 测试印度AWS IP
        IpLocation result = ipLocationUtil.locate("43.205.50.170");
        
        assertNotNull(result);
        assertTrue(result.getSuccess(), "解析应该成功");
        assertNotEquals("amap", result.getSource(), "国外IP不应使用高德API");
        assertNotNull(result.getCountry(), "国家不应为空");
        assertNotNull(result.getCity(), "城市不应为空");
        assertNotNull(result.getLatitude(), "纬度不应为空");
        assertNotNull(result.getLongitude(), "经度不应为空");
        // 国家枚举可能为null（如果未匹配到）
        
        System.out.println("\n国外IP解析结果：");
        System.out.println("IP: " + result.getIp());
        System.out.println("国家: " + result.getCountry());
        System.out.println("国家代码: " + result.getCountryEnum());
        System.out.println("国家枚举: " + result.getCountryEnum());
        System.out.println("所属洲: " + (result.getCountryEnum() != null ? result.getCountryEnum().getContinent() : "未知"));
        System.out.println("省份: " + result.getProvince());
        System.out.println("城市: " + result.getCity());
        System.out.println("坐标: (" + result.getLatitude() + ", " + result.getLongitude() + ")");
        System.out.println("来源: " + result.getSource());
    }
    
    /**
     * 测试空IP
     */
    @Test
    void testLocateEmptyIp() {
        IpLocation result = ipLocationUtil.locate("");
        
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
    }
    
    /**
     * 测试null IP
     */
    @Test
    void testLocateNullIp() {
        IpLocation result = ipLocationUtil.locate(null);
        
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
    }
    
    /**
     * 测试 海外IP
     */
    @Test
    void testLocateUSIp() {
        // 谷歌DNS IP
        IpLocation result = ipLocationUtil.locate("43.205.50.170");
        
        assertNotNull(result);
        assertTrue(result.getSuccess(), "解析应该成功");
        
        System.out.println("IP解析结果：");
        System.out.println("IP: " + result.getIp());
        System.out.println("国家: " + result.getCountry());
        System.out.println("国家代码: " + result.getCountryCode());
        System.out.println("城市: " + result.getCity());
        System.out.println("来源: " + result.getSource());
        System.out.println("枚举: " + result.getCountryEnum());
        System.out.println("坐标: " + result.getLatitude() + ", " + result.getLongitude());


    }
}
