package com.heytrip.hotel.search.infra.sys.impl;

import com.heytrip.hotel.search.common.exception.BusinessException;
import com.heytrip.hotel.search.domain.entity.App;
import com.heytrip.hotel.search.domain.repository.AppRepository;
import com.heytrip.hotel.search.infra.sys.AppService;
import com.heytrip.hotel.search.infra.sys.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {
    
    private final AppRepository appRepository;
    private final CacheService cacheService;
    
    @Override
    public App getAppById(String appId) {
        App cached = cacheService.getAppFromCache(appId);
        if (cached != null) {
            return cached;
        }
        
        App app = appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException("应用不存在"));
        cacheService.cacheApp(app);
        return app;
    }
    
    @Override
    public Page<App> listApps(Pageable pageable) {
        return appRepository.findAll(pageable);
    }
    
    @Override
    @Transactional
    public void updateApp(String appId, Integer rateLimit, Integer timeout, String updateBy) {
        App app = getAppById(appId);
        if (rateLimit != null) app.setRateLimit(rateLimit);
        if (timeout != null) app.setTimeout(timeout);
        app.setUpdateBy(updateBy);
        appRepository.save(app);
        cacheService.evictApp(appId);
        log.info("更新应用成功: appId={}", appId);
    }
    
    @Override
    @Transactional
    public void refreshSecretKey(String appId, String updateBy) {
        App app = getAppById(appId);
        app.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
        app.setUpdateBy(updateBy);
        appRepository.save(app);
        cacheService.evictApp(appId);
        log.info("刷新应用密钥成功: appId={}", appId);
    }
}
