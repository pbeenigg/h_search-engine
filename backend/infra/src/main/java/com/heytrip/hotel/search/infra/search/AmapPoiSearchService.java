package com.heytrip.hotel.search.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.heytrip.hotel.search.infra.config.POISearchWeights;
import com.heytrip.hotel.search.infra.search.doc.AmapPoiIndexDoc;
import com.heytrip.hotel.search.infra.search.dto.PoiSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.heytrip.hotel.search.infra.util.ESDebugUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 高德POI地标搜索服务
 * 职责：提供POI地标的关键词搜索、地理位置搜索等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapPoiSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final POISearchWeights poiSearchWeights;

    /**
     * 根据关键词搜索POI地标
     *
     * @param keyword 搜索关键词
     * @param size    返回数量
     * @return POI地标列表
     */
    public List<PoiSearchResult> searchByKeyword(String keyword, int size) {
        try {
            log.debug("[AMap POI-SEARCH] 关键词搜索 keyword='{}' size={} index={}",
                    keyword, size, poiSearchWeights.getReadAlias());
            
            SearchRequest request = SearchRequest.of(s -> s
                    .index(poiSearchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> b
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("name")
                                                    .query(keyword)
                                                    .boost(poiSearchWeights.getNameCnBoost())
                                            )
                                    )
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("address")
                                                    .query(keyword)
                                                    .boost(poiSearchWeights.getAddressBoost())
                                            )
                                    )
                                    .minimumShouldMatch("1")
                            )
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[AMap POI-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<AmapPoiIndexDoc> response = elasticsearchClient.search(request, AmapPoiIndexDoc.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        AmapPoiIndexDoc poi = hit.source();
                        return PoiSearchResult.builder()
                                .poi(poi)
                                .score(hit.score())
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("[AMap POI-SEARCH] 关键词搜索失败 keyword={}", keyword, e);
            return new ArrayList<>();
        }
    }

    /**
     * 在指定城市搜索POI地标
     *
     * @param keyword  搜索关键词
     * @param cityName 城市名称
     * @param size     返回数量
     * @return POI地标列表
     */
    public List<PoiSearchResult> searchByKeywordAndCity(String keyword, String cityName, int size) {
        try {
            log.debug("[AMap POI-SEARCH] 关键词+城市搜索 keyword='{}' city='{}' size={} index={}",
                    keyword, cityName, size, poiSearchWeights.getReadAlias());
            
            SearchRequest request = SearchRequest.of(s -> s
                    .index(poiSearchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> b
                                    // 城市过滤
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("cityname")
                                                    .value(cityName)
                                            )
                                    )
                                    // 关键词匹配
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("name")
                                                    .query(keyword)
                                                    .boost(poiSearchWeights.getNameCnBoost())
                                            )
                                    )
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("address")
                                                    .query(keyword)
                                                    .boost(poiSearchWeights.getAddressBoost())
                                            )
                                    )
                                    .minimumShouldMatch("1")
                            )
                    )
            );

            if (log.isDebugEnabled()) {
                log.debug("[AMap POI-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<AmapPoiIndexDoc> response = elasticsearchClient.search(request, AmapPoiIndexDoc.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        AmapPoiIndexDoc poi = hit.source();
                        return PoiSearchResult.builder()
                                .poi(poi)
                                .score(hit.score())
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("[AMap POI-SEARCH] 城市关键词搜索失败 keyword={} city={}", keyword, cityName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索指定位置附近的POI地标
     *
     * @param lat      纬度
     * @param lon      经度
     * @param radiusKm 搜索半径（公里）
     * @param size     返回数量
     * @return POI地标列表（按距离排序）
     */
    public List<PoiSearchResult> searchNearby(double lat, double lon, double radiusKm, int size) {
        try {
            log.debug("[AMap POI-SEARCH] 附近搜索 lat={} lon={} radius={}km size={} index={}",
                    lat, lon, radiusKm, size, poiSearchWeights.getReadAlias());
            
            SearchRequest request = SearchRequest.of(s -> s
                    .index(poiSearchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
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
                log.debug("[AMap POI-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<AmapPoiIndexDoc> response = elasticsearchClient.search(request, AmapPoiIndexDoc.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        AmapPoiIndexDoc poi = hit.source();
                        Double distance = null;
                        if (hit.sort() != null && !hit.sort().isEmpty()) {
                            Object sortValue = hit.sort().get(0);
                            if (sortValue instanceof Number) {
                                distance = ((Number) sortValue).doubleValue();
                            }
                        }
                        return PoiSearchResult.builder()
                                .poi(poi)
                                .distance(distance)
                                .score(hit.score())
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("[Amap POI-SEARCH] 附近搜索失败 lat={} lon={} radius={}km", lat, lon, radiusKm, e);
            return new ArrayList<>();
        }
    }

    /**
     * 在指定位置附近搜索符合关键词的POI地标
     *
     * @param keyword  搜索关键词
     * @param lat      纬度
     * @param lon      经度
     * @param radiusKm 搜索半径（公里）
     * @param size     返回数量
     * @return POI地标列表（按距离排序）
     */
    public List<PoiSearchResult> searchNearbyWithKeyword(String keyword, double lat, double lon, double radiusKm, int size) {
        try {
            log.debug("[AMap POI-SEARCH] 附近+关键词搜索 keyword='{}' lat={} lon={} radius={}km size={} index={}",
                    keyword, lat, lon, radiusKm, size, poiSearchWeights.getReadAlias());
            
            SearchRequest request = SearchRequest.of(s -> s
                    .index(poiSearchWeights.getReadAlias())
                    .size(size)
                    .query(q -> q
                            .bool(b -> b
                                    // 地理位置过滤
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
                                            )
                                    )
                                    // 关键词匹配
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("name")
                                                    .query(keyword)
                                                    .boost(poiSearchWeights.getNameCnBoost())
                                            )
                                    )
                                    .should(sh -> sh
                                            .match(m -> m
                                                    .field("address")
                                                    .query(keyword)
                                                    .boost(poiSearchWeights.getAddressBoost())
                                            )
                                    )
                                    .minimumShouldMatch("1")
                            )
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
                log.debug("[AMap POI-SEARCH] ES查询JSON:\n{}", ESDebugUtil.toJson(request));
            }

            SearchResponse<AmapPoiIndexDoc> response = elasticsearchClient.search(request, AmapPoiIndexDoc.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        AmapPoiIndexDoc poi = hit.source();
                        Double distance = null;
                        if (hit.sort() != null && !hit.sort().isEmpty()) {
                            Object sortValue = hit.sort().get(0);
                            if (sortValue instanceof Number) {
                                distance = ((Number) sortValue).doubleValue();
                            }
                        }
                        return PoiSearchResult.builder()
                                .poi(poi)
                                .distance(distance)
                                .score(hit.score())
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("[AMap POI-SEARCH] 附近关键词搜索失败 keyword={} lat={} lon={} radius={}km",
                    keyword, lat, lon, radiusKm, e);
            return new ArrayList<>();
        }
    }
}
