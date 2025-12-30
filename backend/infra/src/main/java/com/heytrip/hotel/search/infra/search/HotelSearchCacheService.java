package com.heytrip.hotel.search.infra.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 酒店搜索结果缓存服务
 * 用于缓存ES查询结果，减少ES查询压力，提升响应速度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelSearchCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 缓存TTL：5分钟
     */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 缓存Key前缀
     */
    private static final String CACHE_PREFIX = "hotel:search:";

    /**
     * 获取关键词搜索缓存
     *
     * @param keyword 搜索关键词
     * @param tag     业务域标签
     * @param size    返回数量
     * @return 缓存的搜索结果，如果缓存不存在返回null
     */
    public List<HotelIndexDoc> getKeywordSearchCache(String keyword, String tag, int size) {
        String cacheKey = buildKeywordCacheKey(keyword, tag, size);
        return getCache(cacheKey);
    }

    /**
     * 设置关键词搜索缓存
     *
     * @param keyword 搜索关键词
     * @param tag     业务域标签
     * @param size    返回数量
     * @param results 搜索结果
     */
    public void setKeywordSearchCache(String keyword, String tag, int size, List<HotelIndexDoc> results) {
        String cacheKey = buildKeywordCacheKey(keyword, tag, size);
        setCache(cacheKey, results);
    }

    /**
     * 获取关键词+城市搜索缓存
     *
     * @param keyword 搜索关键词
     * @param city    城市
     * @param tag     业务域标签
     * @param size    返回数量
     * @return 缓存的搜索结果，如果缓存不存在返回null
     */
    public List<HotelIndexDoc> getKeywordCitySearchCache(String keyword, String city, String tag, int size) {
        String cacheKey = buildKeywordCityCacheKey(keyword, city, tag, size);
        return getCache(cacheKey);
    }

    /**
     * 设置关键词+城市搜索缓存
     *
     * @param keyword 搜索关键词
     * @param city    城市
     * @param tag     业务域标签
     * @param size    返回数量
     * @param results 搜索结果
     */
    public void setKeywordCitySearchCache(String keyword, String city, String tag, int size, List<HotelIndexDoc> results) {
        String cacheKey = buildKeywordCityCacheKey(keyword, city, tag, size);
        setCache(cacheKey, results);
    }

    /**
     * 获取地理+关键词搜索缓存
     *
     * @param lat     纬度
     * @param lon     经度
     * @param radius  搜索半径（公里）
     * @param keyword 搜索关键词
     * @param tag     业务域标签
     * @param size    返回数量
     * @return 缓存的搜索结果，如果缓存不存在返回null
     */
    public List<HotelIndexDoc> getGeoKeywordSearchCache(Double lat, Double lon, Double radius, 
                                                         String keyword, String tag, int size) {
        String cacheKey = buildGeoKeywordCacheKey(lat, lon, radius, keyword, tag, size);
        return getCache(cacheKey);
    }

    /**
     * 设置地理+关键词搜索缓存
     *
     * @param lat     纬度
     * @param lon     经度
     * @param radius  搜索半径（公里）
     * @param keyword 搜索关键词
     * @param tag     业务域标签
     * @param size    返回数量
     * @param results 搜索结果
     */
    public void setGeoKeywordSearchCache(Double lat, Double lon, Double radius, 
                                         String keyword, String tag, int size, 
                                         List<HotelIndexDoc> results) {
        String cacheKey = buildGeoKeywordCacheKey(lat, lon, radius, keyword, tag, size);
        setCache(cacheKey, results);
    }

    /**
     * 从缓存获取数据
     */
    private List<HotelIndexDoc> getCache(String cacheKey) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                List<HotelIndexDoc> results = objectMapper.readValue(
                        cachedJson, 
                        new TypeReference<List<HotelIndexDoc>>() {}
                );
                log.debug("[CACHE-HIT] 缓存命中 key={} 结果数={}", cacheKey, results.size());
                return results;
            }
            log.debug("[CACHE-MISS] 缓存未命中 key={}", cacheKey);
            return null;
        } catch (Exception e) {
            log.warn("[CACHE-ERROR] 缓存读取失败 key={}", cacheKey, e);
            return null;
        }
    }

    /**
     * 设置缓存
     */
    private void setCache(String cacheKey, List<HotelIndexDoc> results) {
        if (results == null || results.isEmpty()) {
            // 不缓存空结果
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            log.debug("[CACHE-SET] 缓存设置成功 key={} 结果数={} TTL={}分钟", 
                    cacheKey, results.size(), CACHE_TTL.toMinutes());
        } catch (Exception e) {
            log.warn("[CACHE-ERROR] 缓存写入失败 key={}", cacheKey, e);
        }
    }

    /**
     * 构建关键词搜索缓存Key
     */
    private String buildKeywordCacheKey(String keyword, String tag, int size) {
        return String.format("%skeyword:%s:tag:%s:size:%d", 
                CACHE_PREFIX, 
                keyword.toLowerCase(), 
                tag != null ? tag : "all", 
                size);
    }

    /**
     * 构建关键词+城市搜索缓存Key
     */
    private String buildKeywordCityCacheKey(String keyword, String city, String tag, int size) {
        return String.format("%skeyword:%s:city:%s:tag:%s:size:%d", 
                CACHE_PREFIX, 
                keyword.toLowerCase(), 
                city.toLowerCase(), 
                tag != null ? tag : "all", 
                size);
    }

    /**
     * 构建地理+关键词搜索缓存Key
     * 坐标精确到小数点后3位（约111米精度）
     */
    private String buildGeoKeywordCacheKey(Double lat, Double lon, Double radius, 
                                           String keyword, String tag, int size) {
        String latStr = String.format("%.3f", lat);
        String lonStr = String.format("%.3f", lon);
        String radiusStr = String.format("%.1f", radius);
        
        return String.format("%sgeo:%s,%s:radius:%s:keyword:%s:tag:%s:size:%d", 
                CACHE_PREFIX, 
                latStr, 
                lonStr, 
                radiusStr, 
                keyword != null ? keyword.toLowerCase() : "none", 
                tag != null ? tag : "all", 
                size);
    }
}
