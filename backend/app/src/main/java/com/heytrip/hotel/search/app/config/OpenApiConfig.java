package com.heytrip.hotel.search.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 基础配置
 * - 统一标题、版本、许可证
 * - 注册全局安全方案：Header「satoken」
 * - 可在 Swagger UI 顶部通过 Authorize 录入 Token 进行鉴权
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_KEY = "satoken";

    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
            .title("Hotel Search Engine API")
            .version("v1")
            .description("酒店信息采集与入库、任务管理与查询 API 文档")
            .contact(new Contact().name("HeyTrip").email("pax@heytripgo.com"))
            .license(new License().name("Apache-2.0").url("https://www.apache.org/licenses/LICENSE-2.0"));

        // Header 安全定义：satoken
        SecurityScheme tokenScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("satoken")
            .description("Sa-Token 认证，请在此处填写登录后返回的 Token 值");

        SecurityRequirement requirement = new SecurityRequirement().addList(SECURITY_SCHEME_KEY);

        Server localServer = new Server().url("/api").description("本地环境");

        return new OpenAPI()
            .info(info)
            .servers(List.of(localServer))
            .components(new Components().addSecuritySchemes(SECURITY_SCHEME_KEY, tokenScheme))
            .addSecurityItem(requirement);
    }
}
