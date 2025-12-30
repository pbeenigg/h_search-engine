package com.heytrip.hotel.search.infra.sys.listener;

import com.heytrip.hotel.search.infra.sys.DataInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器
 * 在应用启动完成后执行数据初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    
    private final DataInitService dataInitService;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("应用启动完成，开始执行数据初始化...");
        
        try {
            dataInitService.initAllData();
        } catch (Exception e) {
            log.error("数据初始化失败，但不影响应用启动", e);
        }
    }
}
