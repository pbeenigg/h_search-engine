package com.heytrip.hotel.search.infra.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * 显式启用 Spring Data Elasticsearch 仓库扫描，
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = {
        "com.heytrip.hotel.search.infra.search.repo"
})
public class ElasticsearchRepositoryConfig {
}
