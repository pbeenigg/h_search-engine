package com.heytrip.hotel.search.ingest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.domain.entity.JobSchedule;
import com.heytrip.hotel.search.domain.repository.JobScheduleRepository;
import com.heytrip.hotel.search.ingest.model.JobScheduleParams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务调度配置缓存服务
 * 减少数据库查询，提升性能
 * 
 * 缓存策略：
 * - 缓存时间：默认 30 秒
 * - 自动刷新：超过缓存时间后首次访问时刷新
 * - 手动刷新：提供 invalidate 方法清除缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobScheduleCacheService {

    private final JobScheduleRepository jobScheduleRepository;
    private final ObjectMapper objectMapper;

    private static final int CACHE_TTL_SECONDS = 30;

    @Getter
    private static class CachedJobSchedule {
        private final JobSchedule jobSchedule;
        private final JobScheduleParams params;
        private final LocalDateTime cachedAt;

        public CachedJobSchedule(JobSchedule jobSchedule, ObjectMapper objectMapper) {
            this.jobSchedule = jobSchedule;
            this.params = JobScheduleParams.parse(objectMapper, jobSchedule.getParams());
            this.cachedAt = LocalDateTime.now();
        }

        public boolean isExpired(int ttlSeconds) {
            return cachedAt.plusSeconds(ttlSeconds).isBefore(LocalDateTime.now());
        }
    }

    private final ConcurrentHashMap<String, CachedJobSchedule> cache = new ConcurrentHashMap<>();

    /**
     * 获取任务配置（带缓存）
     * @param jobCode 任务代码
     * @return Optional<JobSchedule>
     */
    public Optional<JobSchedule> getJobSchedule(String jobCode) {
        CachedJobSchedule cached = cache.get(jobCode);
        
        // 缓存未命中或已过期
        if (cached == null || cached.isExpired(CACHE_TTL_SECONDS)) {
            return refreshCache(jobCode);
        }
        
        log.debug("[JobScheduleCache] 缓存命中 jobCode={}", jobCode);
        return Optional.of(cached.getJobSchedule());
    }

    /**
     * 获取任务参数（带缓存）
     * @param jobCode 任务代码
     * @return JobScheduleParams
     */
    public JobScheduleParams getJobScheduleParams(String jobCode) {
        CachedJobSchedule cached = cache.get(jobCode);
        
        // 缓存未命中或已过期
        if (cached == null || cached.isExpired(CACHE_TTL_SECONDS)) {
            refreshCache(jobCode);
            cached = cache.get(jobCode);
        }
        
        return cached != null ? cached.getParams() : new JobScheduleParams();
    }

    /**
     * 刷新缓存
     * @param jobCode 任务代码
     * @return Optional<JobSchedule>
     */
    private synchronized Optional<JobSchedule> refreshCache(String jobCode) {
        // 双重检查：避免并发刷新
        CachedJobSchedule cached = cache.get(jobCode);
        if (cached != null && !cached.isExpired(CACHE_TTL_SECONDS)) {
            return Optional.of(cached.getJobSchedule());
        }

        log.debug("[JobScheduleCache] 刷新缓存 jobCode={}", jobCode);
        Optional<JobSchedule> jsOpt = jobScheduleRepository.findByJobCode(jobCode);
        
        jsOpt.ifPresentOrElse(
            js -> cache.put(jobCode, new CachedJobSchedule(js, objectMapper)),
            () -> cache.remove(jobCode)
        );
        
        return jsOpt;
    }

    /**
     * 手动清除缓存
     * @param jobCode 任务代码
     */
    public void invalidate(String jobCode) {
        cache.remove(jobCode);
        log.info("[JobScheduleCache] 缓存已清除 jobCode={}", jobCode);
    }

    /**
     * 清除所有缓存
     */
    public void invalidateAll() {
        cache.clear();
        log.info("[JobScheduleCache] 所有缓存已清除");
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("JobScheduleCache: size=%d, keys=%s", 
                cache.size(), cache.keySet());
    }
}
