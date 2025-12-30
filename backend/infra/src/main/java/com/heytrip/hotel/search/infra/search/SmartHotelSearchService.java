package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.common.util.IpLocation;
import com.heytrip.hotel.search.common.util.IpLocationUtil;
import com.heytrip.hotel.search.infra.config.HotelSearchWeights;
import com.heytrip.hotel.search.infra.search.doc.AmapPoiIndexDoc;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import com.heytrip.hotel.search.infra.search.dto.PoiSearchResult;
import com.heytrip.hotel.search.infra.search.dto.ScoredHotelResult;
import com.heytrip.hotel.search.infra.search.dto.SmartSearchRequest;
import com.heytrip.hotel.search.infra.search.dto.SmartSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 智能酒店搜索编排服务
 * 职责：整合多数据源（酒店索引、POI地标、IP定位），实现智能搜索和结果融合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartHotelSearchService {

    private final HotelGeoSearchService hotelGeoSearchService;
    private final HotelKeywordSearchService hotelKeywordSearchService;
    private final AmapPoiSearchService amapPoiSearchService;
    private final IpLocationUtil ipLocationUtil;
    private final HotelScoreCalculator scoreCalculator;
    private final HotelResultMerger resultMerger;
    private final SearchLogService searchLogService;

    // 酒店索引搜索结果数量
    @Value("${heytrip.search.smart.hotel-index-size:5}")
    private int hotelIndexSize;

    // AMap POI索引搜索结果数量
    @Value("${heytrip.search.smart.poi-index-size:2}")
    private int poiIndexSize;

    // 搜索半径（公里）
    @Value("${heytrip.search.smart.search-radius-km:10}")
    private double searchRadiusKm;

    // 异步搜索超时时间（秒）
    @Value("${heytrip.search.smart.async-timeout-seconds:3}")
    private int asyncTimeoutSeconds;



    /**
     * 智能搜索酒店
     *
     * @param request 搜索请求
     * @return 智能搜索结果
     */
    public SmartSearchResult smartSearch(SmartSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("[SMART-SEARCH] 开始智能搜索 keyword='{}' userIp='{}' country='{}' city='{}'",
                request.getKeyword(), request.getUserIp(), request.getCountry(), request.getCity());

        // 第一步：异步获取IP定位（如果提供了IP且没有提供地理位置）
        long ipPhaseStart = System.currentTimeMillis();
        CompletableFuture<IpLocation> ipLocationFuture = null;

        String requestUserIp = request.getUserIp();
        //requestUserIp = "183.6.6.101"; //调式使用固定IP
        // 过滤localhost IP，避免无效定位
        boolean isValidIp = StringUtils.hasText(requestUserIp)
                && !requestUserIp.equals("0:0:0:0:0:0:0:1")
                && !requestUserIp.equals("127.0.0.1")
                && !requestUserIp.equals("::1");
        
        if (isValidIp && request.getUserLat() == null && request.getUserLon() == null) {
            String finalIp = requestUserIp;
            ipLocationFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[SMART-SEARCH] 开始IP定位 ip={}", finalIp);
                    return ipLocationUtil.locate(finalIp);
                } catch (Exception e) {
                    log.warn("[SMART-SEARCH] IP定位失败 ip={}", request.getUserIp(), e);
                    return null;
                }
            });
        }

        // 第二步：确定搜索位置（用户指定 > IP定位 > 无位置）
        log.info("[PERF] IP定位异步启动完成 耗时={}ms", System.currentTimeMillis() - ipPhaseStart);
        
        Double searchLat = request.getUserLat();
        Double searchLon = request.getUserLon();
        String searchCountry = request.getCountry();
        String searchCity = request.getCity();

        // 尝试快速获取IP定位结果（不阻塞，最多等待100ms）
        if (ipLocationFuture != null) {
            long ipWaitStart = System.currentTimeMillis();
            try {
                // 只等待100ms，避免阻塞搜索
                IpLocation ipLocation = ipLocationFuture.get(30, TimeUnit.MILLISECONDS);
                long ipWaitDuration = System.currentTimeMillis() - ipWaitStart;
                log.info("[PERF] IP定位快速获取完成 耗时={}ms", ipWaitDuration);
                
                if (ipLocation != null && ipLocation.getSuccess()) {
                    if (searchLat == null && ipLocation.getLatitude() != null) {
                        searchLat = ipLocation.getLatitude();
                        searchLon = ipLocation.getLongitude();
                    }
                    if (!StringUtils.hasText(searchCountry)) {
                        searchCountry = ipLocation.getCountry();
                    }
                    if (!StringUtils.hasText(searchCity)) {
                        searchCity = ipLocation.getCity();
                    }
                    log.info("[SMART-SEARCH] IP定位成功 ip={} country={} city={} lat={} lon={}",
                            request.getUserIp(), searchCountry, searchCity, searchLat, searchLon);
                }
            } catch (TimeoutException e) {
                // IP定位超过100ms，放弃等待，直接进行搜索
                log.info("[PERF] IP定位超过100ms，放弃等待，使用纯关键词搜索");
            } catch (Exception e) {
                log.warn("[SMART-SEARCH] IP定位失败 ip={}", request.getUserIp());
            }
        }

        // 第三步：异步多源搜索
        long searchPhaseStart = System.currentTimeMillis();
        List<CompletableFuture<List<ScoredHotelResult>>> futures = new ArrayList<>();

        // 创建final变量副本供lambda使用
        final String finalKeyword = request.getKeyword();
        final String finalTag = request.getTag();
        final Double finalLat = searchLat;
        final Double finalLon = searchLon;
        final String finalCity = searchCity;
        final String finalCountry = searchCountry;

        // 3.1 纯关键词搜索（无地理位置时）
        if (StringUtils.hasText(finalKeyword) && finalLat == null) {
            CompletableFuture<List<ScoredHotelResult>> keywordFuture = CompletableFuture.supplyAsync(() ->
                    searchByKeywordOnly(finalKeyword, finalCity, finalTag));
            futures.add(keywordFuture);
        }
        
        // 3.2 地理位置+关键词搜索
        else if (StringUtils.hasText(finalKeyword) && finalLat != null) {
            CompletableFuture<List<ScoredHotelResult>> geoKeywordFuture = CompletableFuture.supplyAsync(() ->
                    searchByGeoAndKeyword(finalKeyword, finalLat, finalLon, finalTag));
            futures.add(geoKeywordFuture);
        }
        
        // 3.3 仅地理位置搜索
        else if (finalLat != null) {
            CompletableFuture<List<ScoredHotelResult>> geoOnlyFuture = CompletableFuture.supplyAsync(() ->
                    searchByGeoOnly(finalLat, finalLon, finalTag));
            futures.add(geoOnlyFuture);
        }

        // 3.4 AMap POI地标搜索（仅当有关键词时）
        if (StringUtils.hasText(finalKeyword)) {
            CompletableFuture<List<ScoredHotelResult>> poiFuture = CompletableFuture.supplyAsync(() ->
                    searchHotelsNearPoi(finalKeyword, finalLat, finalLon, finalCity, finalTag));
            futures.add(poiFuture);
        }

        // 第四步：等待所有搜索完成并合并结果
        long waitStart = System.currentTimeMillis();
        log.info("[PERF] 异步任务启动完成 futures数量={} 耗时={}ms", futures.size(), waitStart - searchPhaseStart);
        
        List<ScoredHotelResult> allResults = new ArrayList<>();
        for (CompletableFuture<List<ScoredHotelResult>> future : futures) {
            try {
                List<ScoredHotelResult> results = future.get(asyncTimeoutSeconds, TimeUnit.SECONDS);
                if (results != null && !results.isEmpty()) {
                    allResults.addAll(results);
                }
            } catch (Exception e) {
                log.warn("[SMART-SEARCH] 某个搜索源超时或失败", e);
            }
        }

        long waitDuration = System.currentTimeMillis() - waitStart;
        log.info("[PERF] 异步任务等待完成 总结果数={} 耗时={}ms", allResults.size(), waitDuration);

        // 第五步：过滤无效数据
        long filterStart = System.currentTimeMillis();
        List<ScoredHotelResult> validResults = resultMerger.filterInvalidData(allResults);
        log.info("[PERF] 过滤完成 过滤前={} 过滤后={} 耗时={}ms", 
                allResults.size(), validResults.size(), System.currentTimeMillis() - filterStart);

        // 第六步：去重和合并（按hotelId和名称）
        long mergeStart = System.currentTimeMillis();
        List<ScoredHotelResult> mergedResults = resultMerger.mergeAndDeduplicate(validResults);
        log.info("[PERF] 去重完成 去重前={} 去重后={} 耗时={}ms", 
                validResults.size(), mergedResults.size(), System.currentTimeMillis() - mergeStart);

        // 第七步：选择TopN
        int topN = request.getSize() != null ? request.getSize() : 5;
        List<ScoredHotelResult> topResults = resultMerger.selectTopN(mergedResults, topN);

        // 第八步：提取酒店文档列表
        List<HotelIndexDoc> hotels = topResults.stream()
                .map(ScoredHotelResult::getHotel)
                .filter(hh -> hh.getScore()>100) // 过滤掉分数过低的酒店
                .sorted((s1,s2) -> ( Double.compare(s1.getScore(), s2.getScore()) * -1)) // 按最终分数降序
                .toList();

        long duration = System.currentTimeMillis() - startTime;
        log.info("[SMART-SEARCH] 搜索完成 keyword='{}' 结果数={} 耗时={}ms",
                request.getKeyword(), hotels.size(), duration);

        // 第九步：提取识别到的国别区域信息（基于最高分结果）
        String detectedCountry = null;
        String detectedRegion = null;
        String detectedCity = null;
        
        if (!topResults.isEmpty()) {
            ScoredHotelResult topResult = topResults.stream()
                    .max((a, b) -> Double.compare(a.getFinalScore(), b.getFinalScore()))
                    .orElse(null);
            if (topResult != null && topResult.getHotel() != null) {
                detectedCountry = topResult.getHotel().getCountryCn();
                detectedRegion = topResult.getHotel().getRegionCn();
                detectedCity = topResult.getHotel().getCityCn();
                log.info("[SMART-SEARCH] 识别结果区域 country='{}' region='{}' city='{}'", 
                        detectedCountry, detectedRegion, detectedCity);
            }
        }

        // 第十步：异步记录搜索日志
        searchLogService.logSearch(
                request.getKeyword(),
                null,
                hotels.size(),
                duration,
                request.getUserId(),
                request.getUserIp(),
                searchCountry,
                searchCity,
                searchLat,
                searchLon
        );

        // 构建返回结果
        return SmartSearchResult.builder()
                .keyword(request.getKeyword())
                .total((long) hotels.size())
                .hotels(hotels)
                .searchLat(searchLat)
                .searchLon(searchLon)
                .searchCountry(searchCountry)
                .searchCity(searchCity)
                .detectedCountry(detectedCountry)
                .detectedRegion(detectedRegion)
                .detectedCity(detectedCity)
                .durationMs(duration)
                .build();
    }

    /**
     * 纯关键词搜索（无地理位置）
     * 优化：识别最高分结果的国别区域，进行二次过滤搜索
     */
    private List<ScoredHotelResult> searchByKeywordOnly(String keyword, String city, String tag) {
        long methodStart = System.currentTimeMillis();
        try {
            log.info("[PERF] 开始纯关键词搜索 keyword='{}'", keyword);

            List<HotelIndexDoc> hotels;
            if (StringUtils.hasText(city)) {
                // 关键词 + 城市
                hotels = hotelKeywordSearchService.searchByKeywordAndCity(keyword, city, tag, hotelIndexSize);
            } else {
                // 仅关键词全库搜索
                hotels = hotelKeywordSearchService.searchByKeyword(keyword, tag, hotelIndexSize);
            }

            List<ScoredHotelResult> results = new ArrayList<>();
            for (HotelIndexDoc hotel : hotels) {
                // 纯关键词搜索，使用命中率计算提升准确性
                double score = scoreCalculator.calculateScore(hotel, keyword, 10.0, null, null, null);
                results.add(ScoredHotelResult.builder()
                        .hotel(hotel)
                        .keywordScore(10.0)
                        .distance(null)
                        .finalScore(score * 1.2) // 酒店直接匹配权重提升20%
                        .source("hotel_direct")
                        .build());
            }

            // 优化：识别最高分结果的国别区域，进行二次搜索
            if (!results.isEmpty() && !StringUtils.hasText(city)) {
                // 获取最高分结果
                ScoredHotelResult topResult = results.stream()
                        .max((a, b) -> Double.compare(a.getFinalScore(), b.getFinalScore()))
                        .orElse(null);
                
                if (topResult != null && StringUtils.hasText(topResult.getHotel().getCountryCn())) {
                    String detectedCountry = topResult.getHotel().getCountryCn();
                    String detectedRegion = topResult.getHotel().getRegionCn();
                    
                    log.info("[SMART-SEARCH] 识别国别区域 country='{}' region='{}'", detectedCountry, detectedRegion);
                    
                    // 二次搜索：获取同国家/区域的更多结果
                    List<HotelIndexDoc> regionHotels = hotelKeywordSearchService.searchByKeywordWithRegion(
                            keyword, detectedCountry, detectedRegion, tag, hotelIndexSize);
                    
                    // 合并区域结果（去重）
                    for (HotelIndexDoc hotel : regionHotels) {
                        boolean exists = results.stream()
                                .anyMatch(r -> r.getHotel().getId().equals(hotel.getId()));
                        if (!exists) {
                            double score = scoreCalculator.calculateScore(hotel, keyword, 10.0, null, null, null);
                            results.add(ScoredHotelResult.builder()
                                    .hotel(hotel)
                                    .keywordScore(10.0)
                                    .distance(null)
                                    .finalScore(score * 1.1) // 区域匹配权重提升10%
                                    .source("hotel_region_filtered")
                                    .build());
                        }
                    }
                    
                    log.info("[SMART-SEARCH] 区域过滤后结果数={}", results.size());
                }
            }

            long duration = System.currentTimeMillis() - methodStart;
            log.info("[PERF] 纯关键词搜索完成 结果数={} 耗时={}ms", results.size(), duration);
            return results;

        } catch (Exception e) {
            log.error("[KEYWORD-ONLY] 搜索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 地理位置 + 关键词搜索
     */
    private List<ScoredHotelResult> searchByGeoAndKeyword(String keyword, Double lat, Double lon, String tag) {
        long methodStart = System.currentTimeMillis();
        try {
            log.info("[PERF] 开始地理+关键词搜索 keyword='{}' lat={} lon={}", keyword, lat, lon);

            var geoResults = hotelGeoSearchService.searchNearbyWithKeyword(
                    lat, lon, searchRadiusKm, keyword, tag, hotelIndexSize);

            List<ScoredHotelResult> results = new ArrayList<>();
            for (var geoResult : geoResults) {
                // 地理+关键词搜索，使用命中率计算
                double score = scoreCalculator.calculateScore(
                        geoResult.getHotel(),
                        keyword,
                        10.0,
                        geoResult.getDistanceKm(),
                        lat, lon
                );

                results.add(ScoredHotelResult.builder()
                        .hotel(geoResult.getHotel())
                        .keywordScore(10.0)
                        .distance(geoResult.getDistanceKm())
                        .finalScore(score * 1.2) // 酒店直接匹配权重提升20%
                        .source("hotel_direct")
                        .build());
            }

            long duration = System.currentTimeMillis() - methodStart;
            log.info("[PERF] 地理+关键词搜索完成 结果数={} 耗时={}ms", results.size(), duration);
            return results;

        } catch (Exception e) {
            log.error("[Hotel GEO-KEYWORD] 搜索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 仅地理位置搜索
     */
    private List<ScoredHotelResult> searchByGeoOnly(Double lat, Double lon, String tag) {
        try {
            log.debug("[Hotel GEO-ONLY] 仅地理位置搜索 lat={} lon={} tag='{}'", lat, lon, tag);

            var geoResults = hotelGeoSearchService.searchNearby(lat, lon, searchRadiusKm, tag, hotelIndexSize);

            List<ScoredHotelResult> results = new ArrayList<>();
            for (var geoResult : geoResults) {
                double score = scoreCalculator.calculateScore(
                        geoResult.getHotel(),
                        5.0,
                        geoResult.getDistanceKm(),
                        lat, lon
                );
                results.add(ScoredHotelResult.builder()
                        .hotel(geoResult.getHotel())
                        .keywordScore(5.0)
                        .distance(geoResult.getDistanceKm())
                        .finalScore(score)
                        .source("geo_only")
                        .build());
            }

            log.debug("[Hotel GEO-ONLY] 搜索完成 结果数={}", results.size());
            return results;

        } catch (Exception e) {
            log.error("[Hotel GEO-ONLY] 搜索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 从Amap POI地标搜索，然后查找附近酒店
     */
    private List<ScoredHotelResult> searchHotelsNearPoi(String keyword, Double lat, Double lon, String city, String tag) {
        try {
            log.debug("[Amap POI-SEARCH] 搜索POI地标 keyword='{}' city='{}' lat={} lon={} tag='{}'",
                    keyword, city, lat, lon, tag);

            List<PoiSearchResult> poiResults;

            if (StringUtils.hasText(keyword) && lat != null && lon != null) {
                // 关键词 + 地理位置搜索POI
                poiResults = amapPoiSearchService.searchNearbyWithKeyword(
                        keyword, lat, lon, searchRadiusKm, poiIndexSize);
            } else if (StringUtils.hasText(keyword) && StringUtils.hasText(city)) {
                // 关键词 + 城市搜索POI
                poiResults = amapPoiSearchService.searchByKeywordAndCity(keyword, city, poiIndexSize);
            } else if (StringUtils.hasText(keyword)) {
                // 仅关键词搜索POI
                poiResults = amapPoiSearchService.searchByKeyword(keyword, poiIndexSize);
            } else if (lat != null && lon != null) {
                // 仅地理位置搜索POI
                poiResults = amapPoiSearchService.searchNearby(lat, lon, searchRadiusKm, poiIndexSize);
            } else {
                return new ArrayList<>();
            }

            log.debug("[Amap POI-SEARCH] POI搜索完成 结果数={}", poiResults.size());

            // 对每个POI，查找附近的酒店
            List<ScoredHotelResult> hotelResults = new ArrayList<>();
            for (PoiSearchResult poiResult : poiResults) {
                AmapPoiIndexDoc poi = poiResult.getPoi();
                Double poiScore = poiResult.getScore();
                
                // 过滤低得分POI，避免低相关性POI混入
                // 如果POI得分过低（<5.0），说明关键词匹配度不高，跳过
                if (poiScore == null || poiScore < 5.0) {
                    log.debug("[Amap POI-SEARCH] POI得分过低，跳过 poi={} score={}", poi.getName(), poiScore);
                    continue;
                }
                
                // 验证POI坐标有效性
                if (poi.getLat() == null || poi.getLon() == null) {
                    log.debug("[Amap POI-SEARCH] POI坐标为空，跳过 poi={}", poi.getName());
                    continue;
                }
                
                // 验证坐标范围
                if (poi.getLat() < -90 || poi.getLat() > 90 || poi.getLon() < -180 || poi.getLon() > 180) {
                    log.warn("[Amap POI-SEARCH] POI坐标无效 poi={} lat={} lon={}",
                            poi.getName(), poi.getLat(), poi.getLon());
                    continue;
                }
                
                try {
                    log.debug("[Amap POI-SEARCH] 查找POI附近酒店 poi={} lat={} lon={}",
                            poi.getName(), poi.getLat(), poi.getLon());
                    
                    var nearbyHotels = hotelGeoSearchService.searchNearby(
                            poi.getLat(), poi.getLon(), searchRadiusKm, tag, hotelIndexSize); // 搜索POI周边3公里内的5个酒店

                    for (var hotelGeoResult : nearbyHotels) {
                        // POI间接匹配，关键词命中率基于POI得分判断
                        double score = scoreCalculator.calculateScore(
                                hotelGeoResult.getHotel(),
                                null, // POI间接匹配不传关键词，避免误判命中率
                                poiScore,
                                hotelGeoResult.getDistanceKm(),
                                poi.getLat(), poi.getLon()
                        );
                        hotelResults.add(ScoredHotelResult.builder()
                                .hotel(hotelGeoResult.getHotel())
                                .keywordScore(poiScore)
                                .distance(hotelGeoResult.getDistanceKm())
                                .finalScore(score * 0.6) // POI间接匹配权重降低40%，确保酒店直接匹配优先
                                .source("poi_nearby")
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("[Amap POI-SEARCH] POI附近酒店搜索失败 poi={} lat={} lon={} err={}",
                            poi.getName(), poi.getLat(), poi.getLon(), e.getMessage());
                    // 单个POI失败不影响整体搜索，继续处理下一个
                }
            }

            log.debug("[Amap POI-SEARCH] 找到POI附近酒店 结果数={}", hotelResults.size());
            return hotelResults;

        } catch (Exception e) {
            log.error("[Amap POI-SEARCH] 搜索失败", e);
            return new ArrayList<>();
        }
    }
}
