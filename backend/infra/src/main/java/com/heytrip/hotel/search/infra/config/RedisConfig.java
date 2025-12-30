package com.heytrip.hotel.search.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * 职责：配置 RedisTemplate 和 RedissonClient，支持 Stream 和通用缓存
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:3000ms}")
    private String redisTimeout;

    /**
     * 配置 RedisTemplate<String, Object>
     * 用于 Redis Stream 和通用对象缓存
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置支持 Java 8 日期时间的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 使用配置好的 ObjectMapper 创建序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // key 采用 String 的序列化方式
        template.setKeySerializer(stringSerializer);
        // hash 的 key 也采用 String 的序列化方式
        template.setHashKeySerializer(stringSerializer);

        // value 序列化方式采用 jackson
        template.setValueSerializer(jsonSerializer);
        // hash 的 value 序列化方式采用 jackson
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        return template;
    }



    /**
     * 配置 RedissonClient
     * 使用 Spring 的 Redis 配置来初始化，支持容器环境
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 构建 Redis 地址
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        log.info("========== 初始化 Redisson ==========");
        log.info("Redis 主机: {}", redisHost);
        log.info("Redis 端口: {}", redisPort);
        log.info("Redis 数据库: {}", redisDatabase);
        log.info("连接地址: {}", address);

        // 配置单机模式
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(20)
                .setIdleConnectionTimeout(10000)
                .setTimeout(parseTimeout(redisTimeout))
                // 增加连接超时时间（默认10秒）
                .setConnectTimeout(10000)
                // 设置重试次数
                .setRetryAttempts(3)
                // 设置重试间隔（毫秒）
                .setRetryInterval(1500)
                // DNS 监控间隔（毫秒）- 设置为 -1 禁用
                .setDnsMonitoringInterval(-1);

        // 如果配置了密码，则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
            log.info("Redisson 已配置密码认证");
        }

        // 配置支持Java 8日期时间的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 设置编解码器（使用自定义ObjectMapper）
        config.setCodec(new JsonJacksonCodec(objectMapper));

        // 线程配置
        // 增加线程数以避免线程饥饿导致的连接失败
        config.setThreads(16);  // 增加到16个线程（原来是CPU核心数*2）
        config.setNettyThreads(32);  // 增加Netty线程到32（避免"Unable to write command"错误）

        try {
            RedissonClient client = Redisson.create(config);
            log.info("========== Redisson 客户端初始化完成 ==========");
            
            // 测试连接
            try {
                client.getKeys().count();
                log.info("Redisson 连接测试成功");
            } catch (Exception e) {
                log.warn("Redisson 连接测试失败（非致命错误，继续启动）: {}", e.getMessage());
            }
            
            return client;
        } catch (Exception e) {
            log.error("========== Redisson 初始化失败 ==========");
            log.error("错误信息: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 解析超时时间配置
     * 支持格式：3000ms, 3s, 3000
     */
    private int parseTimeout(String timeout) {
        if (timeout == null || timeout.isEmpty()) {
            return 3000;
        }

        timeout = timeout.trim().toLowerCase();

        if (timeout.endsWith("ms")) {
            return Integer.parseInt(timeout.substring(0, timeout.length() - 2));
        } else if (timeout.endsWith("s")) {
            return Integer.parseInt(timeout.substring(0, timeout.length() - 1)) * 1000;
        } else {
            return Integer.parseInt(timeout);
        }
    }
}
