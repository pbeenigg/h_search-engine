package com.heytrip.hotel.search.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.heytrip.hotel.search.common.api.R;
import com.heytrip.hotel.search.domain.entity.User;
import com.heytrip.hotel.search.infra.sys.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;

    /**
     * 创建用户
     * @param req
     * @return
     */
    @PostMapping
    public R<User> createUser(@Valid @RequestBody CreateUserReq req) {
        String operator = StpUtil.getLoginIdAsString();
        User user = userService.createUser(
            req.getUserName(), 
            req.getPassword(), 
            req.getUserNick(), 
            req.getSex(), 
            operator
        );
        return R.ok(user);
    }


    /**
     * 更新用户信息
     * @param userId
     * @param req
     * @return
     */
    @PutMapping("/{userId}")
    public R<Void> updateUser(@PathVariable Long userId, 
                              @Valid @RequestBody UpdateUserReq req) {
        String operator = StpUtil.getLoginIdAsString();
        userService.updateUser(userId, req.getUserNick(), req.getSex(), operator);
        return R.ok("用户更新成功");
    }

    /**
     * 修改密码
     * @param userId
     * @param req
     * @return
     */
    @PostMapping("/{userId}/change-password")
    public R<Void> changePassword(@PathVariable Long userId, 
                                   @Valid @RequestBody ChangePasswordReq req) {
        userService.changePassword(userId, req.getOldPassword(), req.getNewPassword());
        return R.ok("密码修改成功");
    }
    

    /**
     * 删除用户
     * @param userId
     * @return
     */
    @DeleteMapping("/{userId}")
    public R<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return R.ok("用户删除成功");
    }
    
   /**
     * 查询用户详情
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    public R<User> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return R.ok(user);
    }

    /**
     * 查询用户列表
     * @param page
     * @param size
     * @return
     */
    @GetMapping
    public R<Page<User>> listUsers(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userService.listUsers(PageRequest.of(page, size));
        return R.ok(users);
    }
    
    @Data
    public static class CreateUserReq {
        @NotBlank(message = "用户名不能为空")
        private String userName;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String userNick;
        private String sex;
    }
    
    @Data
    public static class UpdateUserReq {
        private String userNick;
        private String sex;
    }
    
    @Data
    public static class ChangePasswordReq {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
