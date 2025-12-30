package com.heytrip.hotel.search.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 供应商接口配置
 * prefix: heytrip.supplier
 */
@Data
@Component
@ConfigurationProperties(prefix = "heytrip.supplier")
public class SupplierConfig {
    /** 基础地址，如：http://supplier.example.com */
    private String baseUrl;
    /** app 标识 */
    private String app;
    /** 密钥（仅用于签名，勿泄露） */
    private String secret;
    /** HTTP 超时（秒），用于 Camel HTTP 组件（可选，未配置则走任务配置或默认值） */
    private Integer httpTimeoutSec;
}
