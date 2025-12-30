package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.common.util.HanlpUtils;
import com.heytrip.hotel.search.infra.nlp.NlpEnrichmentService;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词命中率计算器
 * 用于评估搜索关键词在酒店各字段中的命中程度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordHitRateCalculator {

    private final NlpEnrichmentService nlpEnrichmentService;

    /**
     * 计算关键词命中率
     * 
     * @param keyword 搜索关键词
     * @param hotel 酒店文档
     * @return 命中率（0.0-1.0），1.0表示100%命中
     */
    public double calculateHitRate(String keyword, HotelIndexDoc hotel) {
        if (!StringUtils.hasText(keyword) || hotel == null) {
            return 0.0;
        }

        // 第一步：将搜索关键词分词（使用简单split分词，性能优先）
        Set<String> searchTokens = tokenize(keyword);
        if (searchTokens.isEmpty()) {
            return 0.0;
        }

        // 第二步：收集酒店所有可匹配字段的tokens
        Set<String> hotelTokens = new HashSet<>();
        
        // 名称字段（优先级最高）
        if (StringUtils.hasText(hotel.getNameCn())) {
            hotelTokens.addAll(tokenize(hotel.getNameCn()));
        }
        if (StringUtils.hasText(hotel.getNameEn())) {
            hotelTokens.addAll(tokenize(hotel.getNameEn()));
        }
        if (StringUtils.hasText(hotel.getNameTraditional())) {
            hotelTokens.addAll(tokenize(hotel.getNameTraditional()));
        }
        
        // 名称衍生字段
        if (hotel.getNameTokens() != null && !hotel.getNameTokens().isEmpty()) {
            hotelTokens.addAll(hotel.getNameTokens().stream()
                    .filter(StringUtils::hasText)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()));
        }
        if (hotel.getNameKeywords() != null && !hotel.getNameKeywords().isEmpty()) {
            hotelTokens.addAll(hotel.getNameKeywords().stream()
                    .filter(StringUtils::hasText)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()));
        }
        
        // 品牌字段
        if (StringUtils.hasText(hotel.getBrandCn())) {
            hotelTokens.addAll(tokenize(hotel.getBrandCn()));
        }
        if (StringUtils.hasText(hotel.getBrandEn())) {
            hotelTokens.addAll(tokenize(hotel.getBrandEn()));
        }
        if (hotel.getNerBrands() != null && !hotel.getNerBrands().isEmpty()) {
            hotelTokens.addAll(hotel.getNerBrands().stream()
                    .filter(StringUtils::hasText)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()));
        }

        // 第三步：计算命中率
        int totalTokens = searchTokens.size();
        int hitTokens = 0;
        
        for (String searchToken : searchTokens) {
            if (hotelTokens.contains(searchToken)) {
                hitTokens++;
            }
        }

        double hitRate = (double) hitTokens / totalTokens;
        
        // 改为TRACE级别，避免DEBUG日志影响性能
        if (log.isTraceEnabled()) {
            log.trace("[HIT-RATE] hotelId={} keyword='{}' searchTokens={} hitTokens={}/{} hitRate={:.2f}%",
                    hotel.getHotelId(), keyword, searchTokens, hitTokens, totalTokens, hitRate * 100);
        }
        
        return hitRate;
    }

    /**
     * 计算精确命中率（考虑短语匹配）
     * 
     * @param keyword 搜索关键词
     * @param hotel 酒店文档
     * @return 精确命中率（0.0-1.5），包含精确匹配加成
     */
    public double calculatePreciseHitRate(String keyword, HotelIndexDoc hotel) {
        if (!StringUtils.hasText(keyword) || hotel == null) {
            return 0.0;
        }

        // 基础命中率
        double baseHitRate = calculateHitRate(keyword, hotel);
        
        // 检查是否有精确匹配或短语匹配
        String normalizedKeyword = keyword.toLowerCase().trim();
        double bonus = 0.0;
        
        // 完全精确匹配（最高加成）
        if (isExactMatch(normalizedKeyword, hotel.getNameEn()) || 
            isExactMatch(normalizedKeyword, hotel.getNameCn())) {
            bonus = 0.5; // 50%加成
        }
        // 短语匹配（中等加成）
        else if (isPhraseMatch(normalizedKeyword, hotel.getNameEn()) || 
                 isPhraseMatch(normalizedKeyword, hotel.getNameCn())) {
            bonus = 0.3; // 30%加成
        }
        
        double preciseHitRate = baseHitRate + bonus;
        
        // 改为TRACE级别
        if (log.isTraceEnabled()) {
            log.trace("[HIT-RATE] 精确命中率 hotelId={} base={:.2f} bonus={:.2f} total={:.2f}",
                    hotel.getHotelId(), baseHitRate, bonus, preciseHitRate);
        }
        
        return preciseHitRate;
    }

    /**
     * 分词：将文本转为小写token集合（简单split分词，性能优先）
     */
    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptySet();
        }
        
        // 简单分词：按空格和标点符号分割
        String normalized = text.toLowerCase();
        String[] tokens = normalized.split("[\\s\\p{Punct}]+");
        
        Set<String> result = new HashSet<>();
        for (String token : tokens) {
            if (token != null && !token.isEmpty() && token.length() > 1) {
                result.add(token);
            }
        }
        return result;
    }

    /**
     * 分词：将文本转为小写token集合 (HanLp分词，精度更高但性能较低)
     */
    private Set<String> tokenizeHanLP(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptySet();
        }

        // 性能优化：直接使用简单分词，避免复杂的正则表达式
        String normalized = text.toLowerCase();
        List<String>  tokens = HanlpUtils.filterValidTokens(nlpEnrichmentService.tokenizeFine(normalized));

        Set<String> result = new HashSet<>();
        for (String token : tokens) {
            if (token != null && !token.isEmpty()) {
                result.add(token);
            }
        }
        return result;
    }

    /**
     * 检查是否精确匹配
     */
    private boolean isExactMatch(String keyword, String field) {
        if (!StringUtils.hasText(field)) {
            return false;
        }
        String normalizedField = field.toLowerCase().trim();
        return normalizedField.equals(keyword);
    }

    /**
     * 检查是否短语匹配（包含关键词）
     */
    private boolean isPhraseMatch(String keyword, String field) {
        if (!StringUtils.hasText(field)) {
            return false;
        }
        String normalizedField = field.toLowerCase();
        return normalizedField.contains(keyword);
    }
}
