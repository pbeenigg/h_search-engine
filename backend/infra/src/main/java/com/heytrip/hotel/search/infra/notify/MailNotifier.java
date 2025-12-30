package com.heytrip.hotel.search.infra.notify;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 邮件通知工具（基于 Spring Boot JavaMailSender）
 * 配置示例（application.yml）：
 * heytrip:
 *   mail:
 *     host: smtp.example.com
 *     port: 465
 *     user: no-reply@example.com
 *     pass: your_password_or_token
 *     from: no-reply@example.com
 *     to: dev1@example.com,dev2@example.com
 *     ssl: true
 */
@Slf4j
@Component
public class MailNotifier {

    @Value("${heytrip.mail.enabled:false}")
    private Boolean enabled;

    @Value("${heytrip.mail.host:}")
    private String host;
    @Value("${heytrip.mail.port:465}")
    private int port;
    @Value("${heytrip.mail.user:}")
    private String user;
    @Value("${heytrip.mail.pass:}")
    private String pass;
    @Value("${heytrip.mail.from:}")
    private String from;
    @Value("${heytrip.mail.to:}")
    private String to;
    @Value("${heytrip.mail.ssl:true}")
    private boolean ssl;

    private JavaMailSender mailSender;

    @PostConstruct
    public void init() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(user);
        sender.setPassword(pass);
        
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        
        props.put("mail.debug", "false");
        
        this.mailSender = sender;
        log.info("[MAIL] 邮件发送器初始化完成 host={} port={} ssl={}", host, port, ssl);
    }

    /**
     * 发送文本邮件
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    public void sendText(String subject, String content) {
        if (enabled == null || !enabled) {
            log.warn("[MAIL] 邮件发送未启用，跳过发送 subject={}", subject);
            return;
        }
        try {
            List<String> tos = Arrays.stream((to == null ? "" : to).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            
            if (tos.isEmpty()) {
                log.warn("[MAIL] 收件人为空，已跳过发送 subject={}", subject);
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(tos.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("[MAIL] 已发送通知，收件人={} 标题={}", tos, subject);
        } catch (Exception e) {
            log.error("[MAIL] 发送失败 subject={} err={}", subject, e.getMessage(), e);
        }
    }

    /**
     * 发送文本邮件
     * @param subject 邮件标题
     * @param content 邮件内容
     * @param receive 收件人，多个用逗号分隔；若为空则使用配置中的默认收件人
     */
    public void sendText(String subject, String content, String receive) {
        if (enabled == null || !enabled) {
            log.warn("[MAIL] 邮件发送未启用，跳过发送 subject={},receive={}", subject,receive);
            return;
        }
        try {
            List<String> tos = Arrays.stream((receive == null ? to : receive).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (tos.isEmpty()) {
                log.warn("[MAIL] 收件人为空，已跳过发送 subject={}", subject);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(tos.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("[MAIL] 已发送通知，收件人={} 标题={}", tos, subject);
        } catch (Exception e) {
            log.error("[MAIL] 发送失败 subject={} err={}", subject, e.getMessage(), e);
        }
    }
}
