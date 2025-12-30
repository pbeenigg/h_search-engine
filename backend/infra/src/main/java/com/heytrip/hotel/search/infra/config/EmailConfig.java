package com.heytrip.hotel.search.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通知邮件接口配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "heytrip.mail")
public class EmailConfig {
    /**
     *   # 通知邮件配置
     *   mail:
     *     enabled: false
     *     host: smtp.tz.mail.wo.cn
     *     port: 465
     *     user: pax@heytripgo.com
     *     pass: 59n-Evyy-d}-7V@k
     *     from: pax@heytripgo.com
     *     to: pax@heytripgo.com
     *     ssl: true
     */

    private boolean enabled = false;
    private String host;
    private int port;
    private String user;
    private String pass;
    private String from;
    private String to;
    private boolean ssl = false;
}
