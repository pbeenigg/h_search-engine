package com.heytrip.hotel.search.common.exception;

/**
 * 频率限制异常
 */
public class RateLimitException extends BusinessException {
    public RateLimitException(String message) {
        super(429, message);
    }
}
