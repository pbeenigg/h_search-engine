package com.heytrip.hotel.search.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.heytrip.hotel.search.infra.config.HotelSearchWeights;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.heytrip.hotel.search.infra.util.ESDebugUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 酒店关键词搜索服务
 * 基于Elasticsearch实现全库关键词搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelKeywordSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final HotelSearchWeights searchWeights;
    private final HotelSearchCacheService cacheService;

    /**
     * 关键词搜索酒店（全库搜索）
     *
     * @param keyword 搜索关键词
     * @param tag     业务域过滤（可选：CN/INTL/HMT）
     * @param size    返回数量
     * @return 酒店列表（按相关性排序）
     */
    public List<HotelIndexDoc> searchByKeyword(String keyword, String tag, int size) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        // 1. 尝试从缓存获取
        List<HotelIndexDoc> cachedResults = cacheService.getKeywordSearchCache(keyword, tag, size);
        if (cachedResults != null) {
            log.debug("[Hotel KEYWORD-SEARCH] 缓存命中 keyword='{}' tag='{}' size={} 结果数={}", 
                    keyword, tag, size, cachedResults.size());
            return cachedResults;
        }

        // 2. 缓存未命中，查询ES
        try {
            log.debug("[Hotel KEYWORD-SEARCH] 全库关键词搜索 keyword='{}' tag='{}' size={}", keyword, tag, size);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(searchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> {
                                var boolQuery = b
                                    // === 酒店名称匹配（多层级权重） ===
                                    
                                    // 1. 中文酒店名称（最高权重）
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("nameCn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getNameCnBoost())
                                            )
                                    )
                                    // 2. 英文酒店名称 - 分词匹配
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("nameEn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getNameEnBoost())
                                            )
                                    )
                                    // 3. 英文酒店名称 - 短语匹配（完整短语得分更高）
                                    .should(sh -> sh
                                            .matchPhrase(m -> m
                                                    .field("nameEn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getNameEnBoost() * 2.0f)
                                            )
                                    )
                                    // 4. 英文酒店名称 - 精确匹配（使用keyword子字段，权重最高）
                                    .should(sh -> sh
                                            .term(t -> t
                                                    .field("nameEn.keyword")
                                                    .value(keyword)
                                                    .boost(searchWeights.getNameEnBoost() * 3.0f)
                                            )
                                    )
                                    // 5. 繁体名称匹配
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("nameTraditional")
                                                    .query(keyword)
                                                    .boost(searchWeights.getNameTraditionalBoost())
                                            )
                                    )
                                    
                                    // === 品牌匹配 ===
                                    
                                    // 中文品牌
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("brandCn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getNerBrandsBoost())
                                            )
                                    )
                                    // 英文品牌
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("brandEn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getNerBrandsBoost())
                                            )
                                    )
                                    // NER品牌实体
                                    .should(sh -> sh
                                            .term(t -> t
                                                    .field("nerBrands")
                                                    .value(keyword)
                                                    .boost(searchWeights.getNerBrandsBoost())
                                            )
                                    )
                                    
                                    // === 地址匹配 ===
                                    
                                    // 中文地址
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("addressCn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getAddressBoost())
                                            )
                                    )
                                    // 英文地址
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("addressEn")
                                                    .query(keyword)
                                                    .boost(searchWeights.getAddressBoost())
                                            )
                                    )
                                    // 地址分词（精细粒度）
                                    .should(sh -> sh
                                            .term(t -> t
                                                    .field("addressTokens")
                                                    .value(keyword)
                                                    .boost(searchWeights.getAddressTokensBoost())
                                            )
                                    )
                                    // 繁体地址
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("addressTraditional")
                                                    .query(keyword)
                                                    .boost(searchWeights.getAddressTraditionalBoost())
                                            )
                                    )
                                    
                                    // === 城市/地点匹配 ===
                                    
                                    // 中文城市
                                    .should(sh -> sh
                                            .term(t -> t
                                                    .field("cityCn")
                                                    .value(keyword)
                                                    .boost(searchWeights.getNerPlacesBoost())
                                            )
                                    )
                                    // 英文城市
                                    .should(sh -> sh
                                            .term(t -> t
                                                    .field("cityEn")
                                                    .value(keyword)
                                                    .boost(searchWeights.getNerPlacesBoost())
                                            )
                                    )
                                    
                                    // === 其他衍生字段 ===
                                    
                                    // 名称分词（精细粒度）
                                    .should(sh -> sh
                                            .term(t -> t
                                                    .field("nameTokens")
                                                    .value(keyword)
                                                    .boost(searchWeights.getNameEnBoost())
                                            )
                                    )

                                    
                                    // 至少匹配一个
                                    .minimumShouldMatch("1");
                                
                                // 如果指定了tag，添加tagSource过滤
                                if (StringUtils.hasText(tag)) {
                                    boolQuery.filter(f -> f
                                            .term(t -> t
                                                    .field("tagSource")
                                                    .value(tag)
                                            )
                                    );
                                }
                                
                                return boolQuery;
                            })
                    )
                    // 添加高亮配置
                    .highlight(h -> h
                            .fields("nameCn", f -> f.numberOfFragments(0))
                            .fields("nameEn", f -> f.numberOfFragments(0))
                            .fields("nameTraditional", f -> f.numberOfFragments(0))
                            .fields("addressCn", f -> f.numberOfFragments(0))
                            .fields("addressEn", f -> f.numberOfFragments(0))
                            .preTags("<em>")
                            .postTags("</em>")
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[Hotel KEYWORD-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(request, HotelIndexDoc.class);

            List<HotelIndexDoc> hotels = response.hits().hits().stream()
                    .map(hit -> {
                        HotelIndexDoc hotel = hit.source();
                        // 设置相关性得分
                        hotel.setScore(hit.score());

                        // 提取高亮结果
                        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                            var highlights = hit.highlight();
                            if (highlights.containsKey("nameCn") && !highlights.get("nameCn").isEmpty()) {
                                hotel.setHighlightedNameCn(highlights.get("nameCn").get(0));
                            }
                            if (highlights.containsKey("nameEn") && !highlights.get("nameEn").isEmpty()) {
                                hotel.setHighlightedNameEn(highlights.get("nameEn").get(0));
                            }
                            if (highlights.containsKey("nameTraditional") && !highlights.get("nameTraditional").isEmpty()) {
                                hotel.setHighlightedNameTraditional(highlights.get("nameTraditional").get(0));
                            }
                            if (highlights.containsKey("addressCn") && !highlights.get("addressCn").isEmpty()) {
                                hotel.setHighlightedAddressCn(highlights.get("addressCn").get(0));
                            }
                            if (highlights.containsKey("addressEn") && !highlights.get("addressEn").isEmpty()) {
                                hotel.setHighlightedAddressEn(highlights.get("addressEn").get(0));
                            }
                        }
                        return hotel;
                    })
                    .toList();

            log.debug("[Hotel KEYWORD-SEARCH] 搜索完成 keyword='{}' 结果数={}", keyword, hotels.size());
            
            // 3. 设置缓存
            cacheService.setKeywordSearchCache(keyword, tag, size, hotels);
            
            return hotels;

        } catch (IOException e) {
            log.error("[Hotel KEYWORD-SEARCH] 搜索失败 keyword='{}'", keyword, e);
            return new ArrayList<>();
        }
    }

    /**
     * 关键词 + 城市搜索
     *
     * @param keyword 搜索关键词
     * @param city    城市名称
     * @param tag     业务域过滤（可选：CN/INTL/HMT）
     * @param size    返回数量
     * @return 酒店列表
     */
    public List<HotelIndexDoc> searchByKeywordAndCity(String keyword, String city, String tag, int size) {
        if (!StringUtils.hasText(keyword) && !StringUtils.hasText(city)) {
            return new ArrayList<>();
        }

        // 1. 尝试从缓存获取
        List<HotelIndexDoc> cachedResults = cacheService.getKeywordCitySearchCache(keyword, city, tag, size);
        if (cachedResults != null) {
            log.debug("[Hotel KEYWORD-SEARCH] 缓存命中 keyword='{}' city='{}' tag='{}' size={} 结果数={}", 
                    keyword, city, tag, size, cachedResults.size());
            return cachedResults;
        }

        // 2. 缓存未命中，查询ES
        try {
            log.debug("[Hotel KEYWORD-SEARCH] 城市关键词搜索 keyword='{}' city='{}' tag='{}' size={}", keyword, city, tag, size);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(searchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> {
                                // 城市过滤
                                if (StringUtils.hasText(city)) {
                                    b.filter(f -> f
                                            .bool(fb -> fb
                                                    .should(sh -> sh
                                                            .term(t -> t.field("cityCn").value(city))
                                                    )
                                                    .should(sh -> sh
                                                            .term(t -> t.field("cityEn").value(city))
                                                    )
                                                    .minimumShouldMatch("1")
                                            )
                                    );
                                }

                                // 关键词匹配（与searchByKeyword保持一致的多层级匹配）
                                if (StringUtils.hasText(keyword)) {
                                    b
                                            // 中文酒店名称
                                            .should(sh -> sh.match(m -> m.field("nameCn").query(keyword).boost(searchWeights.getNameCnBoost())))
                                            // 英文酒店名称 - 分词匹配
                                            .should(sh -> sh.match(m -> m.field("nameEn").query(keyword).boost(searchWeights.getNameEnBoost())))
                                            // 英文酒店名称 - 短语匹配
                                            .should(sh -> sh.matchPhrase(m -> m.field("nameEn").query(keyword).boost(searchWeights.getNameEnBoost() * 2.0f)))
                                            // 英文酒店名称 - 精确匹配
                                            .should(sh -> sh.term(t -> t.field("nameEn.keyword").value(keyword).boost(searchWeights.getNameEnBoost() * 3.0f)))
                                            // 繁体名称
                                            .should(sh -> sh.match(m -> m.field("nameTraditional").query(keyword).boost(searchWeights.getNameTraditionalBoost())))
                                            // 品牌
                                            .should(sh -> sh.match(m -> m.field("brandCn").query(keyword).boost(searchWeights.getNerBrandsBoost())))
                                            .should(sh -> sh.match(m -> m.field("brandEn").query(keyword).boost(searchWeights.getNerBrandsBoost())))
                                            .should(sh -> sh.term(t -> t.field("nerBrands").value(keyword).boost(searchWeights.getNerBrandsBoost())))
                                            // 地址
                                            .should(sh -> sh.match(m -> m.field("addressCn").query(keyword).boost(searchWeights.getAddressBoost())))
                                            .should(sh -> sh.match(m -> m.field("addressEn").query(keyword).boost(searchWeights.getAddressBoost())))
                                            .should(sh -> sh.term(t -> t.field("addressTokens").value(keyword).boost(searchWeights.getAddressTokensBoost())))
                                            .should(sh -> sh.match(m -> m.field("addressTraditional").query(keyword).boost(searchWeights.getAddressTraditionalBoost())))
                                            // 衍生字段
                                            .should(sh -> sh.term(t -> t.field("nameTokens").value(keyword).boost(searchWeights.getNameEnBoost())))
                                            .minimumShouldMatch("1");
                                }

                                // 如果指定了tag，添加tagSource过滤
                                if (StringUtils.hasText(tag)) {
                                    b.filter(f -> f
                                            .term(t -> t
                                                    .field("tagSource")
                                                    .value(tag)
                                            )
                                    );
                                }

                                return b;
                            })
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[Hotel KEYWORD-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(request, HotelIndexDoc.class);

            List<HotelIndexDoc> hotels = response.hits().hits().stream()
                    .map(hit -> {
                        HotelIndexDoc hotel = hit.source();
                        // 设置相关性得分
                        hotel.setScore(hit.score());
                        return hotel;
                    })
                    .toList();

            log.debug("[Hotel KEYWORD-SEARCH] 搜索完成 keyword='{}' city='{}' 结果数={}", keyword, city, hotels.size());
            
            // 3. 设置缓存
            cacheService.setKeywordCitySearchCache(keyword, city, tag, size, hotels);
            
            return hotels;

        } catch (IOException e) {
            log.error("[Hotel KEYWORD-SEARCH] 城市搜索失败 keyword='{}' city='{}'", keyword, city, e);
            return new ArrayList<>();
        }
    }

    /**
     * 按国家和区域过滤的关键词搜索
     * 用于获取与最高分结果同一国家/区域的更多酒店
     *
     * @param keyword 搜索关键词
     * @param country 国家名称（中文）
     * @param region  区域/省份名称（中文，可选）
     * @param tag     业务域过滤（可选：CN/INTL/HMT）
     * @param size    返回数量
     * @return 酒店列表（按相关性排序）
     */
    public List<HotelIndexDoc> searchByKeywordWithRegion(String keyword, String country, String region, String tag, int size) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(country)) {
            return new ArrayList<>();
        }

        try {
            log.debug("[Hotel KEYWORD-SEARCH] 区域关键词搜索 keyword='{}' country='{}' region='{}' tag='{}' size={}", 
                    keyword, country, region, tag, size);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(searchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> {
                                var boolQuery = b
                                    // === 关键词匹配（与searchByKeyword相同的查询逻辑） ===
                                    .should(sh -> sh.match(m -> m.field("nameCn").query(keyword).boost(searchWeights.getNameCnBoost())))
                                    .should(sh -> sh.match(m -> m.field("nameEn").query(keyword).boost(searchWeights.getNameEnBoost())))
                                    .should(sh -> sh.matchPhrase(m -> m.field("nameEn").query(keyword).boost(searchWeights.getNameEnBoost() * 2.0f)))
                                    .should(sh -> sh.term(t -> t.field("nameEn.keyword").value(keyword).boost(searchWeights.getNameEnBoost() * 3.0f)))
                                    .should(sh -> sh.match(m -> m.field("nameTraditional").query(keyword).boost(searchWeights.getNameTraditionalBoost())))
                                    .should(sh -> sh.match(m -> m.field("brandCn").query(keyword).boost(searchWeights.getNerBrandsBoost())))
                                    .should(sh -> sh.match(m -> m.field("brandEn").query(keyword).boost(searchWeights.getNerBrandsBoost())))
                                    .should(sh -> sh.term(t -> t.field("nerBrands").value(keyword).boost(searchWeights.getNerBrandsBoost())))
                                    .should(sh -> sh.match(m -> m.field("addressCn").query(keyword).boost(searchWeights.getAddressBoost())))
                                    .should(sh -> sh.match(m -> m.field("addressEn").query(keyword).boost(searchWeights.getAddressBoost())))
                                    .should(sh -> sh.term(t -> t.field("addressTokens").value(keyword).boost(searchWeights.getAddressTokensBoost())))
                                    .should(sh -> sh.match(m -> m.field("addressTraditional").query(keyword).boost(searchWeights.getAddressTraditionalBoost())))
                                    .should(sh -> sh.term(t -> t.field("cityCn").value(keyword).boost(searchWeights.getNerPlacesBoost())))
                                    .should(sh -> sh.term(t -> t.field("cityEn").value(keyword).boost(searchWeights.getNerPlacesBoost())))
                                    .should(sh -> sh.term(t -> t.field("nameTokens").value(keyword).boost(searchWeights.getNameEnBoost())))
                                    .minimumShouldMatch("1")
                                    
                                    // === 国家过滤（必须匹配） ===
                                    .filter(f -> f.term(t -> t.field("countryCn").value(country)));
                                
                                // 如果指定了区域，添加区域过滤
                                if (StringUtils.hasText(region)) {
                                    boolQuery.filter(f -> f.term(t -> t.field("regionCn").value(region)));
                                }
                                
                                // 如果指定了tag，添加tagSource过滤
                                if (StringUtils.hasText(tag)) {
                                    boolQuery.filter(f -> f.term(t -> t.field("tagSource").value(tag)));
                                }
                                
                                return boolQuery;
                            })
                    )
                    // 添加高亮配置
                    .highlight(h -> h
                            .fields("nameCn", f -> f.numberOfFragments(0))
                            .fields("nameEn", f -> f.numberOfFragments(0))
                            .fields("nameTraditional", f -> f.numberOfFragments(0))
                            .fields("addressCn", f -> f.numberOfFragments(0))
                            .fields("addressEn", f -> f.numberOfFragments(0))
                            .preTags("<em>")
                            .postTags("</em>")
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[Hotel KEYWORD-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(request, HotelIndexDoc.class);

            List<HotelIndexDoc> hotels = response.hits().hits().stream()
                    .map(hit -> {
                        HotelIndexDoc hotel = hit.source();
                        // 设置相关性得分
                        hotel.setScore(hit.score());
                        
                        // 提取高亮结果
                        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                            var highlights = hit.highlight();
                            if (highlights.containsKey("nameCn") && !highlights.get("nameCn").isEmpty()) {
                                hotel.setHighlightedNameCn(highlights.get("nameCn").get(0));
                            }
                            if (highlights.containsKey("nameEn") && !highlights.get("nameEn").isEmpty()) {
                                hotel.setHighlightedNameEn(highlights.get("nameEn").get(0));
                            }
                            if (highlights.containsKey("nameTraditional") && !highlights.get("nameTraditional").isEmpty()) {
                                hotel.setHighlightedNameTraditional(highlights.get("nameTraditional").get(0));
                            }
                            if (highlights.containsKey("addressCn") && !highlights.get("addressCn").isEmpty()) {
                                hotel.setHighlightedAddressCn(highlights.get("addressCn").get(0));
                            }
                            if (highlights.containsKey("addressEn") && !highlights.get("addressEn").isEmpty()) {
                                hotel.setHighlightedAddressEn(highlights.get("addressEn").get(0));
                            }
                        }
                        return hotel;
                    })
                    .toList();

            log.debug("[Hotel KEYWORD-SEARCH] 区域搜索完成 keyword='{}' country='{}' region='{}' 结果数={}", 
                    keyword, country, region, hotels.size());
            
            return hotels;

        } catch (IOException e) {
            log.error("[Hotel KEYWORD-SEARCH] 区域搜索失败 keyword='{}' country='{}' region='{}'", keyword, country, region, e);
            return new ArrayList<>();
        }
    }
}
