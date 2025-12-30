package com.heytrip.hotel.search.infra.sys;

import com.heytrip.hotel.search.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    User createUser(String userName, String password, String userNick, String sex, String createBy);
    void updateUser(Long userId, String userNick, String sex, String updateBy);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void deleteUser(Long userId);
    User getUserById(Long userId);
    User getUserByUserName(String userName);
    Page<User> listUsers(Pageable pageable);
    User login(String userName, String password);
}
