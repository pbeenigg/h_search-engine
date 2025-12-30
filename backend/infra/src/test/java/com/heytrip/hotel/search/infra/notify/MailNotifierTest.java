package com.heytrip.hotel.search.infra.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 邮件通知测试（单元测试）
 * 注意：此测试需要真实的邮件服务器配置才能运行
 */
class MailNotifierTest {

    private MailNotifier mailNotifier;

    @BeforeEach
    void setUp() {
        mailNotifier = new MailNotifier();
        
        // 使用反射设置私有字段（模拟 @Value 注入）
        ReflectionTestUtils.setField(mailNotifier, "host", "smtp.tz.mail.wo.cn");
        ReflectionTestUtils.setField(mailNotifier, "port", 465);
        ReflectionTestUtils.setField(mailNotifier, "user", "pax@heytripgo.com");
        ReflectionTestUtils.setField(mailNotifier, "pass", "59n-Evyy-d}-7V@k");
        ReflectionTestUtils.setField(mailNotifier, "from", "pax@heytripgo.com");
        ReflectionTestUtils.setField(mailNotifier, "to", "pax@heytripgo.com");
        ReflectionTestUtils.setField(mailNotifier, "ssl", true);
        
        // 初始化 JavaMailSender
        mailNotifier.init();
    }

    @Test
    void testSendText() {
        // 发送测试邮件
        mailNotifier.sendText(
                "[测试] 酒店搜索引擎邮件通知测试",
                "这是一封测试邮件，用于验证邮件发送功能是否正常。\n\n" +
                        "如果您收到此邮件，说明邮件配置正确。\n\n" +
                        "发送时间：" + java.time.LocalDateTime.now()
        );
        
        System.out.println("测试邮件已发送，请检查收件箱");
    }
    
    @Test
    void testSendTextWithEmptyRecipient() {
        // 测试空收件人场景
        ReflectionTestUtils.setField(mailNotifier, "to", "");
        
        mailNotifier.sendText(
                "[测试] 空收件人测试",
                "此邮件不应该被发送"
        );
        
        System.out.println("空收件人测试完成，应该跳过发送");
    }
    
    @Test
    void testSendTextWithMultipleRecipients() {
        // 测试多个收件人
        ReflectionTestUtils.setField(mailNotifier, "to", "pax@heytripgo.com,pbeenig@hotmail.com");
        
        mailNotifier.sendText(
                "[测试] 多收件人测试",
                "这是一封发送给多个收件人的测试邮件。\n\n" +
                        "发送时间：" + java.time.LocalDateTime.now()
        );
        
        System.out.println("多收件人测试邮件已发送");
    }
}
