package com.heytrip.hotel.search.infra.sys.impl;

import com.heytrip.hotel.search.common.util.PasswordEncoder;
import com.heytrip.hotel.search.domain.entity.App;
import com.heytrip.hotel.search.domain.entity.User;
import com.heytrip.hotel.search.domain.repository.AppRepository;
import com.heytrip.hotel.search.domain.repository.UserRepository;
import com.heytrip.hotel.search.infra.sys.DataInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据初始化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitServiceImpl implements DataInitService {
    
    private final AppRepository appRepository;
    private final UserRepository userRepository;
    
    // 默认应用配置
    private static final String DEFAULT_APP_ID = "heytrip_hotel_search_admin";
    private static final String DEFAULT_APP_SECRET = "HeyTrip@Admin#HotelSearch!2025";
    private static final String DEFAULT_APP_ENCRYPTION_KEY = "427ae41e4649b934ca495991b7852b855";
    private static final int DEFAULT_RATE_LIMIT = 100;
    private static final int DEFAULT_APP_TIMEOUT = 720;
    
    // 默认用户配置
    private static final String DEFAULT_USER_NAME = "admin";
    private static final String DEFAULT_USER_PASSWORD = "admin123";
    private static final String DEFAULT_USER_NICK = "系统管理员";
    private static final String DEFAULT_USER_SEX = "U";
    private static final int DEFAULT_USER_TIMEOUT = -1;
    
    @Override
    @Transactional
    public void initDefaultApp() {
        if (appRepository.existsById(DEFAULT_APP_ID)) {
            log.info("默认应用已存在，跳过初始化：{}", DEFAULT_APP_ID);
            return;
        }
        
        App app = new App();
        app.setAppId(DEFAULT_APP_ID);
        app.setSecretKey(DEFAULT_APP_SECRET);
        app.setEncryptionKey(DEFAULT_APP_ENCRYPTION_KEY);
        app.setRateLimit(DEFAULT_RATE_LIMIT);
        app.setTimeout(DEFAULT_APP_TIMEOUT);
        app.setCreateAt(LocalDateTime.now());
        app.setUpdateAt(LocalDateTime.now());
        app.setCreateBy("system");
        app.setUpdateBy("system");
        
        appRepository.save(app);
        log.info("默认应用初始化成功：{}", DEFAULT_APP_ID);
    }
    
    @Override
    @Transactional
    public void initDefaultUser() {
        if (userRepository.existsByUserName(DEFAULT_USER_NAME)) {
            log.info("默认用户已存在，跳过初始化：{}", DEFAULT_USER_NAME);
            return;
        }
        
        // 确保默认应用存在
        if (!appRepository.existsById(DEFAULT_APP_ID)) {
            log.warn("默认应用不存在，先初始化默认应用");
            initDefaultApp();
        }
        
        User user = new User();
        user.setUserName(DEFAULT_USER_NAME);
        user.setPassword(PasswordEncoder.encode(DEFAULT_USER_PASSWORD));
        user.setUserNick(DEFAULT_USER_NICK);
        user.setSex(DEFAULT_USER_SEX);
        user.setTimeout(DEFAULT_USER_TIMEOUT);
        user.setAppId(DEFAULT_APP_ID);
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());
        user.setCreateBy("system");
        user.setUpdateBy("system");
        
        userRepository.save(user);
        log.info("默认用户初始化成功：{}", DEFAULT_USER_NAME);
    }
    
    @Override
    @Transactional
    public void initAllData() {
        log.info("========== 开始系统数据初始化 ==========");
        
        try {
            // 1. 初始化默认应用
            initDefaultApp();
            
            // 2. 初始化默认用户
            initDefaultUser();
            
            log.info("========== 系统数据初始化完成 ==========");
        } catch (Exception e) {
            log.error("系统数据初始化失败", e);
            throw new RuntimeException("系统数据初始化失败", e);
        }
    }
}
