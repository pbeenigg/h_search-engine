package com.heytrip.hotel.search.api.config;

import com.heytrip.hotel.search.common.config.AuthConfig;
import com.heytrip.hotel.search.common.interceptor.AppSignInterceptor;
import com.heytrip.hotel.search.common.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final AppSignInterceptor appSignInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthConfig authConfig;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authConfig.getExcludePaths())
                .order(1);
        
        registry.addInterceptor(appSignInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authConfig.getExcludePaths())
                .order(2);
    }
}
