package com.heytrip.hotel.search.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthConfig {
    private List<String> excludePaths = List.of(
        "/auth/**",
        "/health",
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    );
    
    private long signTimeoutSeconds = 10L;
}
