package com.heytrip.hotel.search.infra.sys;

import com.heytrip.hotel.search.domain.entity.App;
import com.heytrip.hotel.search.domain.entity.User;

public interface CacheService {
    void cacheApp(App app);
    App getAppFromCache(String appId);
    void evictApp(String appId);
    void cacheUser(User user);
    User getUserFromCache(String userName);
    void evictUser(String userName);
}
