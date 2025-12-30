package com.heytrip.hotel.search.api.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限数据源
 * 简化实现：
 * - admin 账号授予 job:manage 权限
 */
@Component
public class SaPermissionConfig implements StpInterface {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> list = new ArrayList<>();
        if ("admin".equals(String.valueOf(loginId))) {
            list.add("job:manage");
        }
        return list;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
