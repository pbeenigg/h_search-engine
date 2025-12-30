package com.heytrip.hotel.search.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置
 * 为异步 API 日志写入提供专用线程池
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 异步 日志专用线程池
     * 特点：
     * - 核心线程数 2，最大线程数 5
     * - 队列容量 1000，避免内存溢出
     * - CallerRunsPolicy：队列满时由调用线程执行，避免丢失日志
     */
    @Bean("asyncLogExecutor")
    public Executor asyncLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-log-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("[AsyncConfig] 异步 API 日志线程池已初始化 core={} max={} queue={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
