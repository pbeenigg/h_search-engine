package com.heytrip.hotel.search.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.heytrip.hotel.search.common.api.R;
import com.heytrip.hotel.search.domain.entity.App;
import com.heytrip.hotel.search.infra.sys.AppService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 应用管理接口
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
@Validated
public class AppController {
    
    private final AppService appService;
    
    /**
     * 查询应用详情
     * @param appId
     * @return
     */
    @GetMapping("/{appId}")
    public R<App> getApp(@PathVariable String appId) {
        App app = appService.getAppById(appId);
        return R.ok(app);
    }
    
    /**
     * 应用列表
     * @param page
     * @param size
     * @return
     */
    @GetMapping
    public R<Page<App>> listApps(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Page<App> apps = appService.listApps(PageRequest.of(page, size));
        return R.ok(apps);
    }
    
    /**
     * 更新应用信息
     * @param appId
     * @param req
     * @return
     */
    @PutMapping("/{appId}")
    public R<Void> updateApp(@PathVariable String appId, 
                             @Valid @RequestBody UpdateAppReq req) {
        String operator = StpUtil.getLoginIdAsString();
        appService.updateApp(appId, req.getRateLimit(), req.getTimeout(), operator);
        return R.ok("应用更新成功");
    }
    
    /**
     * 刷新应用密钥
     * @param appId
     * @return
     */
    @PostMapping("/{appId}/refresh-secret")
    public R<Void> refreshSecret(@PathVariable String appId) {
        String operator = StpUtil.getLoginIdAsString();
        appService.refreshSecretKey(appId, operator);
        return R.ok("密钥刷新成功");
    }
    
    @Data
    public static class UpdateAppReq {
        private Integer rateLimit;
        private Integer timeout;
    }
}
