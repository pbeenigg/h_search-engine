package com.heytrip.hotel.search.infra.sys.impl;

import com.heytrip.hotel.search.domain.entity.App;
import com.heytrip.hotel.search.domain.entity.User;
import com.heytrip.hotel.search.infra.sys.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String APP_CACHE_PREFIX = "auth:app:";
    private static final String USER_CACHE_PREFIX = "auth:user:";
    private static final long CACHE_TTL_MINUTES = 30;
    
    @Override
    public void cacheApp(App app) {
        String key = APP_CACHE_PREFIX + app.getAppId();
        redisTemplate.opsForValue().set(key, app, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("缓存应用: {}", app.getAppId());
    }
    
    @Override
    public App getAppFromCache(String appId) {
        String key = APP_CACHE_PREFIX + appId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof App) {
            log.debug("命中应用缓存: {}", appId);
            return (App) cached;
        }
        return null;
    }
    
    @Override
    public void evictApp(String appId) {
        String key = APP_CACHE_PREFIX + appId;
        redisTemplate.delete(key);
        log.debug("清除应用缓存: {}", appId);
    }
    
    @Override
    public void cacheUser(User user) {
        String key = USER_CACHE_PREFIX + user.getUserName();
        redisTemplate.opsForValue().set(key, user, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("缓存用户: {}", user.getUserName());
    }
    
    @Override
    public User getUserFromCache(String userName) {
        String key = USER_CACHE_PREFIX + userName;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof User) {
            log.debug("命中用户缓存: {}", userName);
            return (User) cached;
        }
        return null;
    }
    
    @Override
    public void evictUser(String userName) {
        String key = USER_CACHE_PREFIX + userName;
        redisTemplate.delete(key);
        log.debug("清除用户缓存: {}", userName);
    }
}
