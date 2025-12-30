package com.heytrip.hotel.search.infra.sys.impl;

import com.heytrip.hotel.search.common.exception.AuthException;
import com.heytrip.hotel.search.common.exception.BusinessException;
import com.heytrip.hotel.search.common.util.PasswordEncoder;
import com.heytrip.hotel.search.domain.entity.App;
import com.heytrip.hotel.search.domain.entity.User;
import com.heytrip.hotel.search.domain.repository.AppRepository;
import com.heytrip.hotel.search.domain.repository.UserRepository;
import com.heytrip.hotel.search.infra.sys.CacheService;
import com.heytrip.hotel.search.infra.sys.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final AppRepository appRepository;
    private final CacheService cacheService;
    private final RedissonClient redissonClient;
    
    private static final String USER_CREATE_LOCK_PREFIX = "lock:user:create:";
    
    @Override
    @Transactional
    public User createUser(String userName, String password, String userNick, 
                          String sex, String createBy) {
        String lockKey = USER_CREATE_LOCK_PREFIX + userName;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            
            if (userRepository.existsByUserName(userName)) {
                throw new BusinessException("用户名已存在");
            }
            
            String appId = userName + "_" + System.currentTimeMillis();
            App app = new App();
            app.setAppId(appId);
            app.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
            app.setEncryptionKey(UUID.randomUUID().toString().replace("-", ""));
            app.setRateLimit(1000);
            app.setTimeout(-1);
            app.setCreateBy(createBy);
            app.setUpdateBy(createBy);
            appRepository.save(app);
            cacheService.cacheApp(app);
            
            User user = new User();
            user.setUserName(userName);
            user.setPassword(PasswordEncoder.encode(password));
            user.setUserNick(userNick);
            user.setSex(sex == null ? "U" : sex);
            user.setTimeout(-1);
            user.setAppId(appId);
            user.setCreateBy(createBy);
            user.setUpdateBy(createBy);
            
            User savedUser = userRepository.save(user);
            cacheService.cacheUser(savedUser);
            
            log.info("创建用户成功: userName={}, appId={}", userName, appId);
            return savedUser;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("创建用户失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    @Override
    @Transactional
    public void updateUser(Long userId, String userNick, String sex, String updateBy) {
        User user = getUserById(userId);
        if (userNick != null) user.setUserNick(userNick);
        if (sex != null) user.setSex(sex);
        user.setUpdateBy(updateBy);
        userRepository.save(user);
        cacheService.evictUser(user.getUserName());
        log.info("更新用户成功: userId={}", userId);
    }
    
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!PasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(PasswordEncoder.encode(newPassword));
        userRepository.save(user);
        cacheService.evictUser(user.getUserName());
        log.info("修改密码成功: userId={}", userId);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        cacheService.evictUser(user.getUserName());
        cacheService.evictApp(user.getAppId());
        appRepository.deleteById(user.getAppId());
        userRepository.deleteById(userId);
        log.info("删除用户成功: userId={}, userName={}", userId, user.getUserName());
    }
    
    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    @Override
    public User getUserByUserName(String userName) {
        User cached = cacheService.getUserFromCache(userName);
        if (cached != null) {
            return cached;
        }
        
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        cacheService.cacheUser(user);
        return user;
    }
    
    @Override
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Override
    public User login(String userName, String password) {
        User user = getUserByUserName(userName);
        
        if (!PasswordEncoder.matches(password, user.getPassword())) {
            throw new AuthException("用户名或密码错误");
        }
        
        log.info("用户登录成功: userName={}", userName);
        return user;
    }
}
