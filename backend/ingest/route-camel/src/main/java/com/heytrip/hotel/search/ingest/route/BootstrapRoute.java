package com.heytrip.hotel.search.ingest.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * 最小可运行的 Camel 路由：
 * - 每60秒输出一次心跳日志，验证 Camel 与应用装配正常
 */
@Component
public class BootstrapRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("timer:ingest-heartbeat?period=60000")
            .routeId("ingest-heartbeat")
            .setBody(simple("INGEST HEARTBEAT - Camel is alive"))
            .log(LoggingLevel.DEBUG,"${body}");
    }
}
