package com.heytrip.hotel.search.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


/**
 * 密码加密工具类
 */
public final class PasswordEncoder {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    private PasswordEncoder() {}
    
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
    
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }


    public static void main(String[] args) {
        String rawPassword = "admin123";
        String encodedPassword = encode(rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println("Matches: " + matches(rawPassword, encodedPassword));
    }
}
