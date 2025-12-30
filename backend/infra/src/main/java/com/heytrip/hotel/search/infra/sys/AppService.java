package com.heytrip.hotel.search.infra.sys;

import com.heytrip.hotel.search.domain.entity.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppService {
    App getAppById(String appId);
    Page<App> listApps(Pageable pageable);
    void updateApp(String appId, Integer rateLimit, Integer timeout, String updateBy);
    void refreshSecretKey(String appId, String updateBy);
}
