package com.heytrip.hotel.search.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.heytrip.hotel.search.infra.config.HotelSearchWeights;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import com.heytrip.hotel.search.infra.search.dto.HotelGeoResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.heytrip.hotel.search.infra.util.ESDebugUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 酒店地理位置搜索服务
 * 基于 Elasticsearch Geo-Point 实现附近酒店搜索、区域搜索等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelGeoSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final HotelSearchWeights hotelSearchWeights;

    /**
     * 搜索指定位置附近的酒店
     *
     * @param lat      纬度（WGS84 坐标系）
     * @param lon      经度（WGS84 坐标系）
     * @param radiusKm 搜索半径（公里）
     * @param tag      业务域过滤（可选：CN/INTL/HMT）
     * @param size     返回数量
     * @return 酒店列表（按距离排序）
     */
    public List<HotelGeoResult> searchNearby(double lat, double lon, double radiusKm, String tag, int size) {
        try {
            log.debug("[Hotel GEO] 附近酒店搜索 lat={} lon={} radius={}km tag='{}' size={} index={}",
                    lat, lon, radiusKm, tag, size, hotelSearchWeights.getReadAlias());
            
            // 使用geo_distance查询和排序（location字段已改为geo_point类型）
            SearchRequest request = SearchRequest.of(s -> s
                    .index(hotelSearchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> {
                                var boolQuery = b
                                        .filter(f -> f.geoDistance(gd -> gd
                                    .field("location")
                                    .distance(radiusKm + "km")
                                    .location(gl -> gl
                                            .latlon(ll -> ll
                                                    .lat(lat)
                                                    .lon(lon)
                                            )
                                    )
                                    .distanceType(GeoDistanceType.Arc)
                            ));
                                
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
                    .sort(sort -> sort
                            .geoDistance(gd -> gd
                                    .field("location")
                                    .location(gl -> gl
                                            .latlon(ll -> ll
                                                    .lat(lat)
                                                    .lon(lon)
                                            )
                                    )
                                    .order(SortOrder.Asc)
                                    .unit(DistanceUnit.Kilometers)
                            )
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[Hotel GEO] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(request, HotelIndexDoc.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        HotelIndexDoc hotel = hit.source();
                        // 设置相关性得分
                        hotel.setScore(hit.score());
                        
                        // 从排序字段中获取距离
                        Double distance = null;
                        if (hit.sort() != null && !hit.sort().isEmpty()) {
                            Object sortValue = hit.sort().get(0);
                            if (sortValue instanceof Number) {
                                distance = ((Number) sortValue).doubleValue();
                            }
                        }
                        // 如果排序字段中没有距离，则手动计算
                        if (distance == null && hotel.getLat() != null && hotel.getLon() != null) {
                            distance = calculateDistance(lat, lon, hotel.getLat(), hotel.getLon());
                        }
                        return new HotelGeoResult(hotel, distance);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[Hotel GEO] 附近酒店搜索失败 lat={} lon={} radius={}km index={} err={}",
                    lat, lon, radiusKm, hotelSearchWeights.getReadAlias(), e.getMessage(), e);
            
            // 如果geo查询失败，尝试降级为全索引搜索（不限制地理位置）
            log.warn("[Hotel GEO] 尝试降级为全索引搜索（忽略地理限制）");
            try {
                SearchRequest fallbackRequest = SearchRequest.of(s -> s
                        .index(hotelSearchWeights.getReadAlias())
                        .size(size)
                );
                SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(fallbackRequest, HotelIndexDoc.class);
                
                return response.hits().hits().stream()
                        .map(hit -> {
                            HotelIndexDoc hotel = hit.source();
                            // 设置相关性得分
                            hotel.setScore(hit.score());
                            
                            // 计算距离
                            Double distance = null;
                            if (hotel.getLat() != null && hotel.getLon() != null) {
                                distance = calculateDistance(lat, lon, hotel.getLat(), hotel.getLon());
                            }
                            return new HotelGeoResult(hotel, distance);
                        })
                        .collect(Collectors.toList());
            } catch (Exception fallbackError) {
                log.error("[Hotel GEO] 降级查询也失败 err={}", fallbackError.getMessage());
                return new ArrayList<>();
            }
        }
    }



    /**
     * 在指定位置附近搜索符合关键词的酒店
     *
     * @param lat      纬度
     * @param lon      经度
     * @param radiusKm 搜索半径（公里）
     * @param keyword  关键词（酒店名称、品牌等）
     * @param tag      业务域过滤（可选：CN/INTL/HMT）
     * @param size     返回数量
     * @return 酒店列表（按距离排序）
     */
    public List<HotelGeoResult> searchNearbyWithKeyword(double lat, double lon, double radiusKm, String keyword, String tag, int size) {
        try {
            log.debug("[Hotel GEO-KEYWORD] 附近关键词搜索 lat={} lon={} radius={}km keyword='{}' tag='{}' size={} index={}",
                    lat, lon, radiusKm, keyword, tag, size, hotelSearchWeights.getReadAlias());
            
            SearchRequest request = SearchRequest.of(s -> s
                    .index(hotelSearchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> {
                                var boolQuery = b
                                    // 地理位置过滤（使用geo_distance）
                                    .filter(f -> f
                                            .geoDistance(gd -> gd
                                                    .field("location")
                                                    .distance(radiusKm + "km")
                                                    .location(gl -> gl
                                                            .latlon(ll -> ll
                                                                    .lat(lat)
                                                                    .lon(lon)
                                                            )
                                                    )
                                                    .distanceType(GeoDistanceType.Arc)
                                            )
                                    )
                                    // === 关键词匹配（与HotelKeywordSearchService保持一致）===
                                    
                                    // 中文酒店名称
                                    .should(sh -> sh.match(m -> m.field("nameCn").query(keyword).boost(hotelSearchWeights.getNameCnBoost())))
                                    // 英文酒店名称 - 分词匹配
                                    .should(sh -> sh.match(m -> m.field("nameEn").query(keyword).boost(hotelSearchWeights.getNameEnBoost())))
                                    // 英文酒店名称 - 短语匹配
                                    .should(sh -> sh.matchPhrase(m -> m.field("nameEn").query(keyword).boost(hotelSearchWeights.getNameEnBoost() * 2.0f)))
                                    // 英文酒店名称 - 精确匹配
                                    .should(sh -> sh.term(t -> t.field("nameEn.keyword").value(keyword).boost(hotelSearchWeights.getNameEnBoost() * 3.0f)))
                                    // 繁体名称
                                    .should(sh -> sh.match(m -> m.field("nameTraditional").query(keyword).boost(hotelSearchWeights.getNameTraditionalBoost())))
                                    
                                    // 品牌
                                    .should(sh -> sh.match(m -> m.field("brandCn").query(keyword).boost(hotelSearchWeights.getNerBrandsBoost())))
                                    .should(sh -> sh.match(m -> m.field("brandEn").query(keyword).boost(hotelSearchWeights.getNerBrandsBoost())))
                                    .should(sh -> sh.term(t -> t.field("nerBrands").value(keyword).boost(hotelSearchWeights.getNerBrandsBoost())))
                                    
                                    // 地址
                                    .should(sh -> sh.match(m -> m.field("addressCn").query(keyword).boost(hotelSearchWeights.getAddressBoost())))
                                    .should(sh -> sh.match(m -> m.field("addressEn").query(keyword).boost(hotelSearchWeights.getAddressBoost())))
                                    .should(sh -> sh.term(t -> t.field("addressTokens").value(keyword).boost(hotelSearchWeights.getAddressTokensBoost())))
                                    .should(sh -> sh.match(m -> m.field("addressTraditional").query(keyword).boost(hotelSearchWeights.getAddressTraditionalBoost())))

                                    // 衍生字段
                                    .should(sh -> sh.term(t -> t.field("nameTokens").value(keyword).boost(hotelSearchWeights.getNameEnBoost())))

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
                    .sort(sort -> sort
                            .geoDistance(gd -> gd
                                    .field("location")
                                    .location(gl -> gl
                                            .latlon(ll -> ll
                                                    .lat(lat)
                                                    .lon(lon)
                                            )
                                    )
                                    .order(SortOrder.Asc)
                                    .unit(DistanceUnit.Kilometers)
                            )
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[Hotel GEO-KEYWORD] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(request, HotelIndexDoc.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        HotelIndexDoc hotel = hit.source();
                        // 设置相关性得分
                        hotel.setScore(hit.score());
                        
                        // 从排序字段中获取距离
                        Double distance = null;
                        if (hit.sort() != null && !hit.sort().isEmpty()) {
                            Object sortValue = hit.sort().get(0);
                            if (sortValue instanceof Number) {
                                distance = ((Number) sortValue).doubleValue();
                            }
                        }
                        // 如果排序字段中没有距离，则手动计算
                        if (distance == null && hotel.getLat() != null && hotel.getLon() != null) {
                            distance = calculateDistance(lat, lon, hotel.getLat(), hotel.getLon());
                        }
                        return new HotelGeoResult(hotel, distance);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[Hotel GEO-KEYWORD] 附近关键词搜索失败 lat={} lon={} radius={}km keyword='{}' index={} err={}",
                    lat, lon, radiusKm, keyword, hotelSearchWeights.getReadAlias(), e.getMessage(), e);
            
            // 降级为仅关键词搜索（忽略地理限制）
            log.warn("[Hotel GEO-KEYWORD] 尝试降级为纯关键词搜索（忽略地理限制）");
            try {
                SearchRequest fallbackRequest = SearchRequest.of(s -> s
                        .index(hotelSearchWeights.getReadAlias())
                        .size(size)
                        .query(q -> q
                                .bool(b -> b
                                        .should(sh -> sh.match(m -> m.field("nameCn").query(keyword)))
                                        .should(sh -> sh.match(m -> m.field("nameEn").query(keyword)))
                                        .should(sh -> sh.match(m -> m.field("brandCn").query(keyword)))
                                        .should(sh -> sh.match(m -> m.field("brandEn").query(keyword)))
                                        .minimumShouldMatch("1")
                                )
                        )
                );
                SearchResponse<HotelIndexDoc> response = elasticsearchClient.search(fallbackRequest, HotelIndexDoc.class);
                
                return response.hits().hits().stream()
                        .map(hit -> {
                            HotelIndexDoc hotel = hit.source();
                            // 设置相关性得分
                            hotel.setScore(hit.score());
                            
                            Double distance = null;
                            if (hotel.getLat() != null && hotel.getLon() != null) {
                                distance = calculateDistance(lat, lon, hotel.getLat(), hotel.getLon());
                            }
                            return new HotelGeoResult(hotel, distance);
                        })
                        .collect(Collectors.toList());
            } catch (Exception fallbackError) {
                log.error("[Hotel GEO-KEYWORD] 降级查询也失败 err={}", fallbackError.getMessage());
                return new ArrayList<>();
            }
        }
    }

    /**
     * 使用 Haversine 公式计算两点间距离（公里）
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 格式化距离文本
     *
     * @param distanceKm 距离（公里）
     * @return 格式化后的距离文本（如 "1.2km"、"850m"）
     */
    public static String formatDistance(Double distanceKm) {
        if (distanceKm == null) {
            return "未知";
        }
        if (distanceKm < 1.0) {
            return String.format("%.0fm", distanceKm * 1000);
        } else {
            return String.format("%.1fkm", distanceKm);
        }
    }
}
