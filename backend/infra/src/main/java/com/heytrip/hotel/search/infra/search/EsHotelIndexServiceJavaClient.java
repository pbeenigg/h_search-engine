package com.heytrip.hotel.search.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.heytrip.hotel.search.common.util.HanlpUtils;
import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import com.heytrip.hotel.search.common.util.LanguageDetector;
import com.heytrip.hotel.search.domain.entity.Hotels;
import com.heytrip.hotel.search.infra.config.HotelSearchWeights;
import com.heytrip.hotel.search.infra.nlp.NlpEnrichmentService;
import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * ES 写入实现：基于 Elasticsearch Java Client（Bulk API，返回 per-item 失败）
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class EsHotelIndexServiceJavaClient implements EsHotelIndexService {

    private final ElasticsearchClient elasticsearchClient;
    private final NlpEnrichmentService nlpEnrichmentService;
    private final HotelSearchWeights hotelSearchWeights;


    /**
     * 单条更新或插入酒店索引文档
     * @param tagSource CN|INTL|HMT
     * @param providerSource Elong|Agoda
     * @param s 持久化实体
     * @param p  解析补齐结果
     */
    @Override
    public void upsert(String tagSource, String providerSource, Hotels s, HotelStructuredExtractor.Result p) {
        try {
            HotelIndexDoc doc = mapDoc(tagSource, providerSource, s, p);
            BulkRequest req = new BulkRequest.Builder()
                    .operations(op -> op.index(i -> i.index(hotelSearchWeights.getWriteAlias()).id(doc.getId()).document(doc)))
                    .build();
            BulkResponse resp = elasticsearchClient.bulk(req);
            if (resp.errors()) {
                String details = resp.items() == null ? "unknown error" : resp.items().stream()
                        .filter(it -> it != null && it.error() != null)
                        .map(it -> (it.id() == null ? "" : it.id()) + ":" + it.error().reason())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("unknown error");
                throw new RuntimeException("es upsert failed (bulk errors): " + details);
            }
        } catch (Exception e) {
            throw new RuntimeException("es upsert failed: " + e.getMessage(), e);
        }
    }

    /**
     * 批量更新或插入酒店索引文档
     *
     * @param hotels          持久化实体列表
     * @param parsedSupplier  解析补齐结果提供函数
     * @param tagSourceFn     业务域提供函数
     * @param providerSourceFn 数据提供商提供函数
     * @return 批量操作结果
     */
    @Override
    public BulkResult bulkUpsert(List<Hotels> hotels,
                                 Function<Hotels, HotelStructuredExtractor.Result> parsedSupplier,
                                 Function<Hotels, String> tagSourceFn,
                                 Function<Hotels, String> providerSourceFn) {
        if (hotels == null || hotels.isEmpty()) {
            return new BulkResult(Collections.emptyList(), Collections.emptyMap());
        }
        try {
            // 构造文档与 hotelId 顺序列表
            List<HotelIndexDoc> docs = new ArrayList<>(hotels.size());
            List<Long> hotelIds = new ArrayList<>(hotels.size());
            for (Hotels s : hotels) {
                String tag = tagSourceFn.apply(s);
                String prov = providerSourceFn.apply(s);
                HotelStructuredExtractor.Result p = parsedSupplier.apply(s);
                docs.add(mapDoc(tag, prov, s, p));
                hotelIds.add(s.getHotelId());
            }
            // 构造 BulkRequest（按顺序与 hotelIds 对齐）
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (HotelIndexDoc d : docs) {
                br.operations(BulkOperation.of(o -> o.index(i -> i.index(hotelSearchWeights.getWriteAlias()).id(d.getId()).document(d))));
            }
            BulkResponse resp = elasticsearchClient.bulk(br.build());
            Map<Long, String> failed = new LinkedHashMap<>();
            List<Long> success = new ArrayList<>();
            // 逐项解析：items 与请求顺序一致
            List<BulkResponseItem> items = resp.items();
            for (int i = 0; i < items.size(); i++) {
                Long hid = hotelIds.get(i);
                BulkResponseItem item = items.get(i);
                if (item.error() != null) {
                    String reason = item.error().reason();
                    failed.put(hid, reason);
                } else {
                    success.add(hid);
                }
            }
            // 兼容性：若返回整体 errors=true 但 items 为空，视为全部失败
            if (items.isEmpty() && resp.errors()) {
                for (Long hid : hotelIds) failed.put(hid, "bulk errors");
            }
            return new BulkResult(success, failed);
        } catch (Exception e) {
            log.error("[ES] bulk upsert exception err={}", e.getMessage(), e);
            // 发生异常：全部视为失败，交由上层重试
            Map<Long, String> failed = new LinkedHashMap<>();
            for (Hotels s : hotels) failed.put(s.getHotelId(), e.getMessage());
            return new BulkResult(Collections.emptyList(), failed);
        }
    }

    private HotelIndexDoc mapDoc(String tagSource, String providerSource, Hotels hotels, HotelStructuredExtractor.Result p) {
        HotelIndexDoc d = new HotelIndexDoc();
        d.setId((tagSource == null ? "" : tagSource) + "_" + hotels.getHotelId());
        d.setTagSource(tagSource);
        d.setProviderSource(providerSource);
        d.setHotelId(hotels.getHotelId());
        // 安全读取 p，避免空指针
        String pNameCn = p == null ? null : p.getNameCn();
        String pNameEn = p == null ? null : p.getNameEn();

        String pCountryCn = p == null ? null : p.getCountryCn();
        String pCountryEn = p == null ? null : p.getCountryEn();
        String pCountryCode = p == null ? null : p.getCountryCode();

        String pCityCn = p == null ? null : p.getCityCn();
        String pCityEn = p == null ? null : p.getCityEn();

        String pRegionCn = p == null ? null : p.getRegionCn();
        String pRegionEn = p == null ? null : p.getRegionEn();

        String pContinentCn = p == null ? null : p.getContinentCn();
        String pContinentEn = p == null ? null : p.getContinentEn();

        String pAddressCn = p == null ? null : p.getAddressCn();
        String pAddressEn = p == null ? null : p.getAddressEn();

        Number pLat = p == null ? null : p.getLatitude();
        Number pLon = p == null ? null : p.getLongitude();

        String pGroupCn = p == null ? null : p.getHotelGroupCn();
        String pGroupEn = p == null ? null : p.getHotelGroupEn();

        String pBrandCn = p == null ? null : p.getHotelBrandCn();
        String pBrandEn = p == null ? null : p.getHotelBrandEn();


        // 字段映射优先级：数据库字段优先（已包含人工修正），解析值作为降级
        d.setNameCn(firstNonNull(hotels.getHotelNameCn(), pNameCn));
        d.setNameEn(firstNonNull(hotels.getHotelNameEn(), pNameEn));

        d.setCountryCn(firstNonNull(hotels.getCountryCn(), pCountryCn));
        d.setCountryEn(firstNonNull(hotels.getCountryEn(), pCountryEn));
        d.setCountryCode(firstNonNull(hotels.getCountryCode(), pCountryCode));

        d.setContinentCn(firstNonNull(hotels.getContinentCn(), pContinentCn));
        d.setContinentEn(firstNonNull(hotels.getContinentEn(), pContinentEn));

        d.setCityCn(firstNonNull(hotels.getCityCn(), pCityCn));
        d.setCityEn(firstNonNull(hotels.getCityEn(), pCityEn));

        d.setRegionCn(firstNonNull(hotels.getRegionCn(), pRegionCn));
        d.setRegionEn(firstNonNull(hotels.getRegionEn(), pRegionEn));

        d.setAddressCn(firstNonNull(hotels.getAddressCn(), pAddressCn));
        d.setAddressEn(firstNonNull(hotels.getAddressEn(), pAddressEn));

        /**
         * 地理位置处理（WGS84 坐标系）
         * - 数据源：谷歌坐标系 WGS84（GPS 全球定位系统标准）
         * - 坐标范围：经度 [-180, 180]，纬度 [-90, 90]
         * - 其他坐标系参考：
         *   * GCJ-02：腾讯地图、高德地图（中国国测局坐标系）
         *   * BD09：百度地图（百度坐标系）
         *   * WGS84：谷歌地图、GPS（国际标准）
         */
        var lon = firstNonNull(pLon, hotels.getLongitude());
        var lat = firstNonNull(pLat, hotels.getLatitude());

        // 设置独立的经纬度字段（用于显示和备用）
        d.setLon(lon == null ? null : lon.doubleValue());
        d.setLat(lat == null ? null : lat.doubleValue());

        // 组合 geo_point 字段（用于地理位置查询）
        if (lat != null && lon != null) {
            // 验证坐标有效性
            double latVal = lat.doubleValue();
            double lonVal = lon.doubleValue();

            if (HanlpUtils.isValidCoordinate(latVal, lonVal)) {
                // 使用 Map 格式，ES 标准格式：{ "lat": 纬度, "lon": 经度 }
                Map<String, Object> location = new HashMap<>();
                location.put("lat", latVal);
                location.put("lon", lonVal);
                d.setLocation(location);
            } else {
                log.warn("[INDEX] 无效坐标 hotelId={} lat={} lon={}", hotels.getHotelId(), latVal, lonVal);
                d.setLocation(null);
            }
        } else {
            d.setLocation(null);
        }
        d.setGroupCn(firstNonNull(pGroupCn, hotels.getHotelGroupCn()));
        d.setGroupEn(firstNonNull(pGroupEn, hotels.getHotelGroupEn()));

        d.setBrandCn(firstNonNull(hotels.getHotelBrandCn(), pBrandCn));
        d.setBrandEn(firstNonNull(hotels.getHotelBrandEn(), pBrandEn));

        // 人工修正字段映射（直接从数据库获取）
        d.setAccommodationType(hotels.getAccommodationType());
        d.setSearchEnable(hotels.getSearchEnable());
        d.setTel(hotels.getTel());
        d.setScore(parseScore(hotels.getScore()));
        
        // 时间字段映射（OffsetDateTime -> Date）
        d.setUpdatedAt(hotels.getUpdatedAt() != null ? Date.from(hotels.getUpdatedAt().toInstant()) : null);

        // nlpEnrichmentService 衍生字段：索引时生成
        long nlpStart = System.nanoTime();
        try {
            // 判空与去空白，避免无意义 nlpEnrichmentService 处理
            String name = Optional.ofNullable(firstNonNull(d.getNameCn(), d.getNameEn()))
                    .map(String::trim).filter(sv -> !sv.isEmpty()).orElse(null);

            String addr = Optional.ofNullable(firstNonNull(d.getAddressCn(), d.getAddressEn()))
                    .map(String::trim).filter(sv -> !sv.isEmpty()).orElse(null);

            String brand = Optional.ofNullable(firstNonNull(d.getBrandCn(), d.getBrandEn()))
                    .map(String::trim).filter(sv -> !sv.isEmpty()).orElse(null);




            d.setNameTokens(name == null ? List.of() : HanlpUtils.filterValidTokens(nlpEnrichmentService.tokenizeFine(name)));
            d.setAddressTokens(addr == null ? List.of() : HanlpUtils.filterValidTokens(nlpEnrichmentService.nlpTokens(addr)));
            d.setNameKeywords(name == null ? List.of() : HanlpUtils.filterValidTokens(nlpEnrichmentService.extractKeywords(name, 5)));
            d.setNerPlaces(addr == null ? List.of() : HanlpUtils.filterValidTokens(nlpEnrichmentService.nerPlaces(addr)));
            d.setNerBrands(brand == null ? List.of() : HanlpUtils.filterValidTokens(nlpEnrichmentService.nerBrands(brand)));

            // 1. 简繁体转换
            if(LanguageDetector.containsChinese(name)) {
                d.setNameTraditional(name == null ? null : nlpEnrichmentService.toTraditional(name));
            }
            if(LanguageDetector.containsChinese(addr)) {
                d.setAddressTraditional(addr == null ? null : nlpEnrichmentService.toTraditional(addr));
            }

            // 2. 品牌识别（基于自定义品牌词典）
            String fullText = (name != null ? name : "") + " " + (brand != null ? brand : "");
            d.setBrandNames(HanlpUtils.filterValidTokens(nlpEnrichmentService.nerBrands(fullText)));

            // 3. 地理层级路径（从大到小：洲 -> 国家 -> -> 市 -> 区）
            List<String> geoHierarchy = new ArrayList<>();
            if(d.getTagSource().equalsIgnoreCase("CN")){
                // 国内酒店，优先使用解析结果
                if (d.getContinentCn() != null && !d.getContinentCn().trim().isEmpty()) {
                    geoHierarchy.add(d.getContinentCn().trim());
                }
                if (d.getCountryCn() != null && !d.getCountryCn().trim().isEmpty()) {
                    geoHierarchy.add(d.getCountryCn().trim());
                }
                if (d.getCityCn() != null && !d.getCityCn().trim().isEmpty()) {
                    geoHierarchy.add(d.getCityCn().trim());
                }
                if (d.getRegionCn() != null && !d.getRegionCn().trim().isEmpty()) {
                    geoHierarchy.add(d.getRegionCn().trim());
                }
            }else{
                // 国际酒店，优先使用解析结果
                if (d.getContinentEn() != null && !d.getContinentEn().trim().isEmpty()) {
                    geoHierarchy.add(d.getContinentEn().trim());
                }
                if (d.getCountryEn() != null && !d.getCountryEn().trim().isEmpty()) {
                    geoHierarchy.add(d.getCountryEn().trim());
                }
                if (d.getCityEn() != null && !d.getCityEn().trim().isEmpty()) {
                    geoHierarchy.add(d.getCityEn().trim());
                }
                if (d.getRegionEn() != null && !d.getRegionEn().trim().isEmpty()) {
                    geoHierarchy.add(d.getRegionEn().trim());
                }
            }
            d.setGeoHierarchy(geoHierarchy.isEmpty() ? List.of() : geoHierarchy);


        } catch (Exception e) {
            // 降级：忽略 nlpEnrichmentService 失败，不影响主流程
            d.setNameTokens(List.of());
            d.setAddressTokens(List.of());
            d.setNameKeywords(List.of());
            d.setNerPlaces(List.of());
            d.setNerBrands(List.of());
            log.warn("[INDEX] nlpEnrichmentService processing failed for hotelId={} err={}", hotels.getHotelId(), e.getMessage(), e);
        } finally {
            long elapsedMs = (System.nanoTime() - nlpStart) / 1_000_000;
            int nameTokensCount = d.getNameTokens() == null ? 0 : d.getNameTokens().size();
            int addressTokensCount = d.getAddressTokens() == null ? 0 : d.getAddressTokens().size();
            int nameKeywordsCount = d.getNameKeywords() == null ? 0 : d.getNameKeywords().size();
            int nerPlacesCount = d.getNerPlaces() == null ? 0 : d.getNerPlaces().size();
            int nerBrandsCount = d.getNerBrands() == null ? 0 : d.getNerBrands().size();

            log.debug("[INDEX] HanLP耗时计算 hotelId={} elapsedMs={}ms nameTokens={} addressTokens={} nameKeywords={} nerPlaces={} nerBrands={} ",
                    hotels.getHotelId(), elapsedMs,
                    nameTokensCount, addressTokensCount, nameKeywordsCount, nerPlacesCount, nerBrandsCount);
        }
        return d;
    }



    /**
     * 返回第一个非空值
     *
     * @param a
     * @param b
     * @param <T>
     * @return
     */
    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    /**
     * 解析分数字符串为Double类型
     * 处理非数字值，返回null
     *
     * @param scoreStr 分数字符串
     * @return Double类型的分数，解析失败返回null
     */
    private static Double parseScore(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(scoreStr.trim());
        } catch (NumberFormatException e) {
            log.warn("[INDEX] 无效的分数值: {}", scoreStr);
            return null;
        }
    }
}
