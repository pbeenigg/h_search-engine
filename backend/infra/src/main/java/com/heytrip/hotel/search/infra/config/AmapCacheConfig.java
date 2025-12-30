package com.heytrip.hotel.search.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 高德POI缓存配置
 * 使用Redis作为分布式缓存实现，支持集群环境下的缓存共享
 */
@Configuration
@EnableCaching
public class AmapCacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String CACHE_CITYCODE = "amap:citycode";
    public static final String CACHE_POITYPE = "amap:poitype";
    public static final String CACHE_COLLECT_STATUS = "amap:collect:status";

    /**
     * 配置Redis缓存管理器
     */
    @Bean
    public CacheManager amapCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 配置JSON序列化器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        GenericJackson2JsonRedisSerializer jsonSerializer = 
                new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 默认缓存配置：24小时过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();
        
        // 针对不同缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 城市代码缓存：24小时（数据不常变化）
        cacheConfigurations.put(CACHE_CITYCODE, defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // POI类型缓存：24小时（数据不常变化）
        cacheConfigurations.put(CACHE_POITYPE, defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 采集任务状态缓存：1小时（频繁变化）
        cacheConfigurations.put(CACHE_COLLECT_STATUS, defaultConfig.entryTtl(Duration.ofHours(1)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
