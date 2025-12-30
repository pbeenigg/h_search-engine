package com.heytrip.hotel.search.common.util;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 请求唯一键生成器
 */
public final class RequestKeyGenerator {
    private RequestKeyGenerator() {}
    
    public static String generate(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        Map<String, String[]> parameterMap = request.getParameterMap();
        String queryParams = new TreeMap<>(parameterMap).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("&"));
        
        String body = "";
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            try {
                BufferedReader reader = request.getReader();
                if (reader != null) {
                    body = reader.lines().collect(Collectors.joining());
                }
            } catch (IOException e) {
            }
        }
        
        String raw = method + ":" + uri + ":" + queryParams + ":" + body;
        return DigestUtil.md5Hex(raw);
    }
}
