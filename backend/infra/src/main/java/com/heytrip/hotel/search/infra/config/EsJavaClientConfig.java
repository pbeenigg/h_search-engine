package com.heytrip.hotel.search.infra.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Elasticsearch Java Client 配置
 * 读取 spring.elasticsearch.* 配置来构建客户端
 */
@Slf4j
@Configuration
public class EsJavaClientConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String uris;
    @Value("${spring.elasticsearch.username:}")
    private String username;
    @Value("${spring.elasticsearch.password:}")
    private String password;
    @Value("${spring.elasticsearch.socket-timeout:30000}")
    private int socketTimeoutMs;

    @Bean(destroyMethod = "close")
    public RestClient restClient() {
        // 仅取第一个地址作为客户端入口（可按需扩展多节点）
        String first = uris.split(",")[0].trim();
        URI uri = URI.create(first);
        RestClientBuilder builder = RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()));
        builder.setRequestConfigCallback(cfg -> cfg
                .setConnectTimeout(10_000)
                .setSocketTimeout(socketTimeoutMs)
        );
        if (username != null && !username.isEmpty()) {
            BasicCredentialsProvider creds = new BasicCredentialsProvider();
            creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(http -> http.setDefaultCredentialsProvider(creds));
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
