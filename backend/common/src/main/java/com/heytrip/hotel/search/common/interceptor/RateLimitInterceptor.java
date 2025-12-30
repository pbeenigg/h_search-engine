package com.heytrip.hotel.search.common.interceptor;

import com.heytrip.hotel.search.common.exception.RateLimitException;
import com.heytrip.hotel.search.common.util.RequestKeyGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RedissonClient redissonClient;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                            Object handler) throws Exception {
        String appId = request.getHeader("app");
        if (appId == null) {
            return true;
        }
        
        Object rateLimitObj = request.getAttribute("RATE_LIMIT");
        int rateLimit = (rateLimitObj instanceof Integer) ? (Integer) rateLimitObj : 1000;
        
        String requestKey = RequestKeyGenerator.generate(request);
        String rateLimiterKey = RATE_LIMIT_PREFIX + appId + ":" + requestKey;
        
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);
        
        boolean exists = rateLimiter.isExists();
        if (!exists) {
            rateLimiter.trySetRate(RateType.OVERALL, rateLimit, 1, RateIntervalUnit.SECONDS);
            rateLimiter.expire(java.time.Duration.ofMinutes(2));
        }
        
        boolean acquired = rateLimiter.tryAcquire(1);
        
        if (!acquired) {
            log.warn("限流触发: appId={}, requestKey={}, limit={}/秒", appId, requestKey, rateLimit);
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }
        
        log.debug("限流检查通过: appId={}, requestKey={}", appId, requestKey);
        return true;
    }
}
