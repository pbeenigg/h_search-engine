package com.heytrip.hotel.search.common.util;

import lombok.extern.slf4j.Slf4j;


/**
 * 应用签名验证器
 */
@Slf4j
public final class AppSignValidator {
    private AppSignValidator() {}
    
    public static boolean validate(String appId, String secret, String sign, 
                                    long timestamp, long allowedOffset) {
        long currentSeconds = System.currentTimeMillis() / 1000;
        long diff = Math.abs(currentSeconds - timestamp);
        
        if (diff > allowedOffset) {
            log.warn("时间戳偏差过大: {} 秒, appId={}", diff, appId);
            return false;
        }
        
        String expectedSign = Md5Signer.buildSign(appId, secret, timestamp);
        return expectedSign.equalsIgnoreCase(sign);
    }
}
