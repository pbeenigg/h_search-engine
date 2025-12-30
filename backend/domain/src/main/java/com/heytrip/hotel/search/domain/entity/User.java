package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "user_name", unique = true, nullable = false, length = 50)
    private String userName;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "user_nick", length = 100)
    private String userNick;
    
    @Column(name = "sex", length = 1, columnDefinition = "char(1) default 'U'")
    private String sex;
    
    @Column(name = "timeout", nullable = false, columnDefinition = "int default -1")
    private Integer timeout;
    
    @Column(name = "app_id", nullable = false, length = 100)
    private String appId;
    
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
