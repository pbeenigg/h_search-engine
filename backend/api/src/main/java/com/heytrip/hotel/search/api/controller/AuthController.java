package com.heytrip.hotel.search.api.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.heytrip.hotel.search.common.api.R;
import com.heytrip.hotel.search.domain.entity.User;
import com.heytrip.hotel.search.infra.sys.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    
    private final UserService userService;

    /**
     * 用户登录
     * @param req
     * @return
     */
    @PostMapping("/login")
    @SaIgnore
    public R<LoginResp> login(@Valid @RequestBody LoginReq req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        StpUtil.login(user.getUserName());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        
        LoginResp resp = new LoginResp();
        resp.setTokenName(tokenInfo.getTokenName());
        resp.setTokenValue(tokenInfo.getTokenValue());
        resp.setUserId(user.getUserId());
        resp.setUserName(user.getUserName());
        resp.setUserNick(user.getUserNick());
        return R.ok(resp);
    }

    /**
     * 用户登出
     * @return
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok("登出成功");
    }
    
    /**
     * 获取当前登录用户信息
     * @return
     */
    @GetMapping("/current")
    public R<User> currentUser() {
        String userName = StpUtil.getLoginIdAsString();
        User user = userService.getUserByUserName(userName);
        return R.ok(user);
    }

    @Data
    public static class LoginReq {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginResp {
        private String tokenName;
        private String tokenValue;
        private Long userId;
        private String userName;
        private String userNick;
    }
}
