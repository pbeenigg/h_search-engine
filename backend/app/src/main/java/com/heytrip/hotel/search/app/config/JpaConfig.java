package com.heytrip.hotel.search.app.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 扫描配置
 * 说明：
 * - 显式开启对 domain 模块实体与仓库包的扫描，避免因包路径不在主应用包扫描范围内而导致 Bean 未找到。
 * - 同时开启事务管理。
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"com.heytrip.hotel.search.api"})
@EnableJpaRepositories(basePackages = {"com.heytrip.hotel.search.domain.repository"})
@EntityScan(basePackages = {"com.heytrip.hotel.search.domain.entity"})
public class JpaConfig {
}
