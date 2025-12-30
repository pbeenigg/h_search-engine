package com.heytrip.hotel.search.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 酒店索引搜索权重与参数配置
 * heytrip.search.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "heytrip.search.hotel")
public class HotelSearchWeights {

    // ===== 酒店名称权重（核心字段） =====
    private float nameCnBoost = 8.0f;                // 中文酒店名称权重（分词匹配）
    private float nameEnBoost = 5.0f;                // 英文酒店名称权重（分词匹配）
    // 注意：英文名称的phrase匹配权重为 nameEnBoost * 2.0，keyword精确匹配权重为 nameEnBoost * 3.0
    // 这样可以确保完整匹配"Amii's Homes"得到更高分数（15分），而部分匹配"Amii's"只得到5分
    
    private float nameTraditionalBoost = 3.0f;       // 繁体名称权重（简繁体互搜）
    private float pinyinBoost = 4.0f;                // 拼音搜索权重（中文拼音输入）
    
    // ===== 品牌权重 =====
    private float nerBrandsBoost = 7.0f;             // 品牌实体权重（NER识别的品牌）
    
    // ===== 地址权重 =====
    private float addressBoost = 3.0f;               // 地址权重（中英文地址分词匹配）
    private float addressPinyinBoost = 2.5f;         // 地址拼音权重
    private float addressTokensBoost = 3.5f;         // 地址精细分词权重（更细粒度的地名召回）
    private float addressTraditionalBoost = 2.0f;    // 繁体地址权重
    
    // ===== 地点/城市权重 =====
    private float nerPlacesBoost = 4.5f;             // 地点实体权重（NER识别的地名）
    
    // ===== 关键词/分词权重 =====
    private float keywordsBoost = 6.0f;              // 名称关键词权重（TopK提取的关键词）
    private float tokensBoost = 4.0f;                // 名称分词权重（精细分词召回）


    // 索引配置
    private String readAlias = "hotels_read";
    private String writeAlias = "hotels_write";

}
