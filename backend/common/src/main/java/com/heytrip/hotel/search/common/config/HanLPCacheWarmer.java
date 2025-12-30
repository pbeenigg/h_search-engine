package com.heytrip.hotel.search.common.config;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * HanLP 缓存预热器
 * 
 * 在 Spring Boot 应用启动时自动加载 HanLP 字典，生成缓存文件
 * 这样可以避免首次请求时的延迟，并确保缓存文件在应用启动时就创建好
 * 
 * @author HotelSearch Team
 */
@Slf4j
@Component
public class HanLPCacheWarmer implements ApplicationRunner {

    @PostConstruct
    public void customizeDictionaries() {
        // 添加自定义词典
        CustomDictionary.add("data/dictionary/custom/HotelBrands.txt");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("开始预热 HanLP 缓存...");
        log.info("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 预加载分词功能（会触发核心词典加载）
            log.info("[1/4] 预加载分词功能...");
            HanLP.segment("预热测试");
            log.info("✓ 分词功能预加载完成");
            
            // 2. 预加载简繁体转换（会触发简繁体字典加载）
            log.info("[2/4] 预加载简繁体转换...");
            HanLP.convertToTraditionalChinese("简体");
            HanLP.convertToSimplifiedChinese("繁體");
            log.info("✓ 简繁体转换预加载完成");
            
            // 3. 预加载拼音转换
            log.info("[3/4] 预加载拼音转换...");
            HanLP.convertToPinyinList("测试");
            log.info("✓ 拼音转换预加载完成");
            
            // 4. 预加载关键词提取
            log.info("[4/4] 预加载关键词提取...");
            HanLP.extractKeyword("测试文本", 1);
            log.info("✓ 关键词提取预加载完成");


            // 5. NLP 相关模型预加载（可选）
            log.info("[5/5] 预加载 NLP 相关模型...");
            NLPTokenizer.segment("我爱北京天安门");
            log.info("✓ NLP 相关模型预加载完成");

            // 6. 加载自定义词典
            log.info("[2/6] 加载自定义词典...");
            CustomDictionary.insert("喜玩酒店搜索引擎", "n 1000");
            log.info("✓ 自定义词典插入成功");
            log.info("✓ 自定义词典验证: {}" , CustomDictionary.contains("喜玩酒店搜索引擎"));
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("========================================");
            log.info("✓ HanLP 缓存预热完成！耗时: {}ms", duration);
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ HanLP 缓存预热失败！");
            log.error("========================================");
            log.error("错误信息: {}", e.getMessage(), e);
            
            // 不抛出异常，避免影响应用启动
            // 如果需要强制要求 HanLP 可用，可以取消下面的注释
            // throw new RuntimeException("HanLP 初始化失败", e);
        }
    }
}
