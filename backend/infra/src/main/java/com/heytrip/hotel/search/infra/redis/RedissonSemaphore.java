package com.heytrip.hotel.search.infra.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * Redisson 全局信号量封装
 */
@Component
@RequiredArgsConstructor
public class RedissonSemaphore {
    private final RedissonClient redissonClient;

    public RSemaphore getSemaphore(String key, int permits) {
        RSemaphore semaphore = redissonClient.getSemaphore(key);
        // 初始化许可数（仅在未设置时生效）
        if (semaphore.trySetPermits(permits)) {
            return semaphore;
        }
        return semaphore;
    }
}
