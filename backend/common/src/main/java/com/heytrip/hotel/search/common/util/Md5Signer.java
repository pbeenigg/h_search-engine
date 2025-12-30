package com.heytrip.hotel.search.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 签名工具：sign = MD5(app + secret + timestamp)
 */
public final class Md5Signer {
    private Md5Signer() {}

    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    public static String buildSign(String app, String secret, long timestampSeconds) {
        // 文档约定：sign=MD5("app"+app+"secret"+secret+"timestamp"+timestamp)
        String raw = "app" + app + "secret" + secret + "timestamp" + timestampSeconds;
        return md5Hex(raw);
    }
}
