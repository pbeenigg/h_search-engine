package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 应用实体类
 */
@Data
@Entity
@Table(name = "app")
public class App {
    @Id
    @Column(name = "app_id", length = 100)
    private String appId;
    
    @Column(name = "secret_key", nullable = false, length = 255)
    private String secretKey;
    
    @Column(name = "encryption_key", nullable = false, length = 255)
    private String encryptionKey;
    
    @Column(name = "rate_limit", nullable = false, columnDefinition = "int default 100")
    private Integer rateLimit;
    
    @Column(name = "timeout", nullable = false, columnDefinition = "int default -1")
    private Integer timeout;
    
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;
    
    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;
    
    @Column(name = "create_by", length = 50)
    private String createBy;
    
    @Column(name = "update_by", length = 50)
    private String updateBy;
    
    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
        updateAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateAt = LocalDateTime.now();
    }
}
