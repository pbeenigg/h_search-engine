package com.heytrip.hotel.search.api.controller;

import com.heytrip.hotel.search.common.api.R;
import com.heytrip.hotel.search.infra.search.HotelGeoSearchService;
import com.heytrip.hotel.search.infra.search.dto.PopularCity;
import com.heytrip.hotel.search.infra.search.dto.PopularHotels;
import com.heytrip.hotel.search.infra.search.dto.SmartSearchRequest;
import com.heytrip.hotel.search.infra.search.dto.SmartSearchResult;
import com.heytrip.hotel.search.infra.search.SmartHotelSearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索接口控制器
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final HotelGeoSearchService geoSearchService;
    private final SmartHotelSearchService smartHotelSearchService;

    /**
     * 搜索服务根路径
     * 返回可用的搜索接口列表
     */
    @GetMapping
    public R<Object> searchRoot() {
        return R.ok(java.util.Map.of(
                "service", "Hotel Search API",
                "version", "1.0.0",
                "endpoints", java.util.Map.of(
                        "smart", "/search/smart?q={keyword}&city={city}&lat={lat}&lon={lon}",
                        "cities", "/search/cities",
                        "hotels", "/search/hotels"
                )
        ));
    }

    /**
     * 智能酒店搜索接口
     * 支持：
     * 1. 关键词搜索
     * 2. IP地址定位
     * 3. 用户指定地理位置（国家、城市、经纬度）
     * 4. 多数据源融合（酒店索引 + POI地标）
     * 5. 智能排序（关键词匹配60% + 距离40%）
     *
     * @param q 搜索关键词（可选）
     * @param tag   业务域（可选：CN=中国大陆/INTL=国际/HMT=港澳台）
     * @param country 国家（可选）
     * @param city    城市（可选）
     * @param lat     纬度（可选）
     * @param lon     经度（可选）
     * @param size    返回数量（默认5条）
     * @param request HttpServletRequest（用于获取真实IP）
     * @return 智能搜索结果
     */
    @GetMapping(path = "/smart", produces = "application/json;charset=UTF-8")
    public R<SmartSearchResult> smartSearch(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            HttpServletRequest request) {

        // 获取真实IP地址
        String userIp = getClientIp(request);

        // 构建智能搜索请求
        SmartSearchRequest searchRequest = SmartSearchRequest.builder()
                .keyword(q)
                .tag(tag)
                .userIp(userIp)
                .userLat(lat)
                .userLon(lon)
                .country(country)
                .city(city)
                .size(size)
                .build();

        log.info("[API] 智能搜索 keyword='{}' tag='{}' userIp='{}' country='{}' city='{}' lat={} lon={} size={}",
                q, tag, userIp, country, city, lat, lon, size);

        // 执行智能搜索
        SmartSearchResult result = smartHotelSearchService.smartSearch(searchRequest);

        return R.ok(result);
    }





    /**
     * 获取热门城市坐标（示例数据）
     *
     * @return 热门城市坐标列表
     */
    @GetMapping("/cities")
    public R<List<PopularCity>> getPopularCities() {
        List<PopularCity> cities = List.of(
                new PopularCity("北京", "Beijing", "中国","China",39.9042, 116.4074),
                new PopularCity("上海", "Shanghai", "中国","China", 31.2304, 121.4737),
                new PopularCity("广州", "Guangzhou", "中国","China", 23.1291, 113.2644),
                new PopularCity("深圳", "Shenzhen", "中国","China", 22.5431, 114.0579),
                new PopularCity("杭州", "Hangzhou", "中国","China", 30.2741, 120.1551),
                new PopularCity("成都", "Chengdu", "中国","China", 30.5728, 104.0668),
                new PopularCity("西安", "Xi'an", "中国","China", 34.3416, 108.9398),
                new PopularCity("重庆", "Chongqing", "中国","China", 29.5630, 106.5516),
                new PopularCity("武汉", "Wuhan", "中国","China", 30.5928, 114.3055),
                new PopularCity("南京", "Nanjing", "中国","China", 32.0603, 118.7969),
                new PopularCity("香港", "Hong Kong", "中国","China", 22.3193, 114.1694),
                new PopularCity("澳门", "Macau", "中国","China", 22.1987, 113.5439),
                new PopularCity("台北", "Taipei", "中国","China", 25.0330, 121.5654)
        );
        return R.ok(cities);
    }


    /**
     * 获取热门酒店推荐
     *
     * @return           热门酒店列表
     */
    @GetMapping("/hotels")
    public R<List<PopularHotels>> getPopularHotels() {
        List<PopularHotels> hotels = List.of(
                new PopularHotels("北京", "Beijing", "中国","China", "北京王府井希尔顿酒店", "Hilton Beijing Wangfujing",39.9087, 116.4180),
                new PopularHotels("上海", "Shanghai", "中国","China", "上海外滩华尔道夫酒店", "Waldorf Astoria Shanghai on the Bund", 31.2430, 121.4905),
                new PopularHotels("广州", "Guangzhou", "中国","China", "广州四季酒店", "Four Seasons Hotel Guangzhou", 23.1194, 113.3245),
                new PopularHotels("深圳", "Shenzhen", "中国","China", "深圳福田香格里拉大酒店", "Shangri-La Hotel Shenzhen", 22.5333, 114.0540),
                new PopularHotels("杭州", "Hangzhou", "中国","China", "杭州西湖国宾馆", "State Guest House Hangzhou", 30.2444, 120.1500),
                new PopularHotels("成都", "Chengdu", "中国","China", "成都瑞吉酒店", "The St. Regis Chengdu", 30.6575, 104.0660),
                new PopularHotels("西安", "Xi'an", "中国","China", "西安索菲特传奇酒店", "Sofitel Legend People's Grand Hotel Xi'an", 34.2632, 108.9470),
                new PopularHotels("重庆", "Chongqing", "中国","China", "重庆JW万豪酒店", "JW Marriott Hotel Chongqing", 29.5628, 106.5525),
                new PopularHotels("武汉", "Wuhan", "中国","China", "武汉光谷希尔顿酒店", "Hilton Wuhan Optics Valley", 30.5280, 114.4035),
                new PopularHotels("南京", "Nanjing", "中国","China", "南京金陵饭店", "Jinling Hotel Nanjing", 32.0600, 118.7890)
        );
        return R.ok(hotels);
    }


    
    /**
     * 获取客户端真实IP地址
     * 考虑代理和负载均衡的情况
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String emptyToNull(String s) { return (s == null || s.isEmpty()) ? null : s; }
}
