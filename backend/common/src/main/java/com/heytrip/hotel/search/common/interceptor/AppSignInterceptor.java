package com.heytrip.hotel.search.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.search.common.config.AuthConfig;
import com.heytrip.hotel.search.common.exception.AuthException;
import com.heytrip.hotel.search.common.util.AppSignValidator;
import com.heytrip.hotel.search.domain.entity.App;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * APP签名认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppSignInterceptor implements HandlerInterceptor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthConfig authConfig;
    
    private static final String APP_CACHE_PREFIX = "auth:app:";
    private static final long APP_CACHE_TTL = 30;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                            Object handler) throws Exception {
        String appId = request.getHeader("app");
        String sign = request.getHeader("sign");
        String timestampStr = request.getHeader("timestamp");
        
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(sign) || StrUtil.isBlank(timestampStr)) {
            return true;
        }
        
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            throw new AuthException("时间戳格式错误");
        }
        
        App app = getAppFromCache(appId);
        if (app == null) {
            throw new AuthException("应用不存在");
        }
        
        if (app.getTimeout() != -1) {
            LocalDateTime expireTime = app.getCreateAt().plusMinutes(app.getTimeout());
            if (LocalDateTime.now().isAfter(expireTime)) {
                throw new AuthException("应用已过期");
            }
        }
        
        boolean valid = AppSignValidator.validate(
            appId, 
            app.getSecretKey(), 
            sign, 
            timestamp, 
            authConfig.getSignTimeoutSeconds()
        );
        
        if (!valid) {
            throw new AuthException("签名验证失败");
        }
        
        request.setAttribute("APP_ID", appId);
        request.setAttribute("RATE_LIMIT", app.getRateLimit());
        log.debug("APP签名认证成功: {}", appId);
        return true;
    }
    
    private App getAppFromCache(String appId) {
        String cacheKey = APP_CACHE_PREFIX + appId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached instanceof App) {
            return (App) cached;
        }
        
        return null;
    }
}
