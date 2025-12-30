package com.heytrip.hotel.search.common.exception;

/**
 * 认证异常
 */
public class AuthException extends BusinessException {
    public AuthException(String message) {
        super(401, message);
    }
}
