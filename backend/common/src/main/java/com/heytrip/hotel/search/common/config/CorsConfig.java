package com.heytrip.hotel.search.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS跨域配置
 * 允许前端(localhost:9091)访问后端API
 *
 * @author Pax
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源地址
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:18081",
                "http://127.0.0.1:18081",
                "http://localhost:3000", // 备用前端端口
                "http://127.0.0.1:3000",
                "http://192.168.0.105:3000",
                "http://47.76.191.223:9090",
                "http://47.76.191.223:18081"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "app", "timestamp", "sign", "User-Agent", "X-Requested-With"));

        // 允许发送Cookie
        configuration.setAllowCredentials(true);

        // 预检请求的缓存时间(秒)
        configuration.setMaxAge(3600L);

        // 暴露的响应头（前端可以访问的响应头）
        configuration.setExposedHeaders(Arrays.asList(
                "Content-Length", "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials", "Access-Control-Allow-Headers"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
