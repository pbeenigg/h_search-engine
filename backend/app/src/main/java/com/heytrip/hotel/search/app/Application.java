package com.heytrip.hotel.search.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 应用启动入口
 * - 单体 + 多模块装配
 * - 集成 Spring Boot / Camel / JPA / Redisson
 */
@SpringBootApplication(scanBasePackages = "com.heytrip.hotel.search")
@ConfigurationPropertiesScan(basePackages = "com.heytrip.hotel.search")
public class Application {
    public static void main(String[] args) {
        // 在 Spring 启动之前配置 HanLP root 路径
        // 这样可以确保 HanLP 类加载时使用正确的路径
        configureHanLPRoot();
        
        SpringApplication.run(Application.class, args);
    }
    
    /**
     * 配置 HanLP 的 root 路径
     * 优先使用环境变量 HANLP_ROOT，如果没有则使用默认路径
     */
    private static void configureHanLPRoot() {
        String hanlpRootPath = System.getenv("HANLP_ROOT");
        
        if (hanlpRootPath != null && !hanlpRootPath.trim().isEmpty()) {
            // 使用环境变量指定的路径
            System.setProperty("HANLP_ROOT", hanlpRootPath);
            System.out.println("[HanLP] 使用环境变量 HANLP_ROOT: " + hanlpRootPath);
        } else {
            // 本地开发环境，使用项目默认路径
            String defaultPath = System.getProperty("user.dir") + "/backend/common/";
            System.setProperty("HANLP_ROOT", defaultPath);
            System.out.println("[HanLP] 使用默认路径: " + defaultPath);
        }
        
        System.out.println("[HanLP] root 路径已设置: " + System.getProperty("HANLP_ROOT"));
    }
}
