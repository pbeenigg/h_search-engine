package com.heytrip.hotel.search.common.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.heytrip.hotel.search.common.config.AmapIpConfig;
import com.heytrip.hotel.search.common.enums.CountryContinentEnum;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * IP定位解析工具
 * <p>
 * 支持多种IP定位API，优先使用GeoLite2本地数据库，失败后依次尝试高德API和国际API
 * <p>
 * 调用顺序：
 * 1. GeoLite2本地数据库（最快、离线、支持全球IP）
 * 2. 高德API（仅支持国内IP，准确度高）
 * 3. api.ip.sb
 * 4. ipapi.co
 * 5. api.ipapi.is
 */
@Slf4j
@Component
public class IpLocationUtil {
    
    private final AmapIpConfig amapIpConfig;
    private final ResourceLoader resourceLoader;

    private DatabaseReader geoLite2Reader;
    
    // 备用API地址
    private static final String API_IPSB_URL = "https://api.ip.sb/geoip/";
    private static final String API_IPAPI_URL = "https://ipapi.co/";
    private static final String API_IPAPIS_URL = "https://api.ipapi.is?ip=";
    
    // 超时时间（毫秒）
    private static final int TIMEOUT = 200;
    
    // API查询超时时间（毫秒）- 用户要求10ms快速响应
    private static final int QUERY_TIMEOUT = 10;
    
    // IPv4正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    public IpLocationUtil(AmapIpConfig amapIpConfig, ResourceLoader resourceLoader) {
        this.amapIpConfig = amapIpConfig;
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * 初始化GeoLite2数据库读取器
     */
    @PostConstruct
    public void initGeoLite2() {
        try {
            String databasePath = amapIpConfig.getGeolite2();
            if (!StringUtils.hasText(databasePath)) {
                log.warn("GeoLite2数据库路径未配置，将跳过本地数据库查询");
                return;
            }
            
            // 支持classpath:和文件系统路径
            Resource resource = resourceLoader.getResource(databasePath);
            if (resource.exists()) {
                // 使用InputStream加载，支持jar包内的资源
                InputStream inputStream = resource.getInputStream();
                geoLite2Reader = new DatabaseReader.Builder(inputStream).build();
                log.info("GeoLite2数据库加载成功: {}", databasePath);
            } else {
                log.warn("GeoLite2数据库文件不存在: {}，将跳过本地数据库查询", databasePath);
            }
        } catch (Exception e) {
            log.error("GeoLite2数据库加载失败: {}", amapIpConfig.getGeolite2(), e);
        }
    }
    
    /**
     * 关闭GeoLite2数据库读取器
     */
    @PreDestroy
    public void closeGeoLite2() {
        if (geoLite2Reader != null) {
            try {
                geoLite2Reader.close();
                log.info("GeoLite2数据库关闭成功");
            } catch (Exception e) {
                log.error("GeoLite2数据库关闭失败", e);
            }
        }
    }
    
    /**
     * 解析IP地址的地理位置信息
     * <p>
     * 优化策略：
     * 1. 过滤IPv6和局域网IP
     * 2. 异步并发查询所有API（10ms超时）
     * 3. 取结果最完整的一条
     * 
     * @param ip IP地址
     * @return IP定位结果
     */
    public IpLocation locate(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return buildErrorResult(ip, "IP地址不能为空");
        }
        
        // 1. 验证IPv4格式
        if (!isValidIPv4(ip)) {
            log.debug("非法或不支持的IP格式（仅支持IPv4）: {}", ip);
            return buildErrorResult(ip, "仅支持IPv4地址");
        }
        
        // 2. 过滤局域网IP
        if (isPrivateIP(ip)) {
            log.debug("局域网IP无需定位: {}", ip);
            return buildErrorResult(ip, "局域网IP无需定位");
        }
        
        // 3. 异步并发查询所有API（10ms超时）
        List<CompletableFuture<IpLocation>> futures = new ArrayList<>();
        
        // GeoLite2本地数据库（最快，优先）
        futures.add(CompletableFuture.supplyAsync(() -> locateByGeoLite2(ip)));
        
        // 高德API
        futures.add(CompletableFuture.supplyAsync(() -> locateByAmap(ip)));
        
        // 国际API
        futures.add(CompletableFuture.supplyAsync(() -> locateByIpSb(ip)));
        futures.add(CompletableFuture.supplyAsync(() -> locateByIpapi(ip)));
        //futures.add(CompletableFuture.supplyAsync(() -> locateByIpapis(ip)));
        
        // 等待所有任务完成或超时（10ms）
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allOf.get(QUERY_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // 超时或异常，不影响后续处理
            log.debug("部分API查询超时（{}ms）: {}", QUERY_TIMEOUT, ip);
        }
        
        // 4. 收集所有成功的结果
        List<IpLocation> successResults = new ArrayList<>();
        for (CompletableFuture<IpLocation> future : futures) {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                try {
                    IpLocation result = future.getNow(null);
                    if (result != null && result.getSuccess() && isValidResult(result)) {
                        successResults.add(result);
                    }
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        }
        
        // 5. 选择结果最完整的一条
        if (!successResults.isEmpty()) {
            IpLocation bestResult = selectBestResult(successResults);
            log.info("IP定位成功 [{}] IP: {}, 国家: {}, 省: {}, 城市: {}, 坐标: ({}, {})",
                bestResult.getSource(), ip, bestResult.getCountry(), 
                bestResult.getProvince(), bestResult.getCity(),
                bestResult.getLatitude(), bestResult.getLongitude());
            return bestResult;
        }
        
        // 所有API都失败
        log.warn("所有IP定位API均失败或超时，IP: {}", ip);
        return buildErrorResult(ip, "所有IP定位API均失败或超时");
    }
    
    /**
     * 验证是否为合法的IPv4地址
     */
    private boolean isValidIPv4(String ip) {
        return IPV4_PATTERN.matcher(ip).matches();
    }
    
    /**
     * 判断是否为局域网IP
     */
    private boolean isPrivateIP(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从多个结果中选择最完整的一条
     * 评分标准：国家(1分) + 省份(1分) + 城市(2分) + 坐标(2分)
     */
    private IpLocation selectBestResult(List<IpLocation> results) {
        if (results.isEmpty()) {
            return null;
        }
        
        IpLocation best = results.get(0);
        int bestScore = calculateCompletenessScore(best);
        
        for (int i = 1; i < results.size(); i++) {
            IpLocation current = results.get(i);
            int currentScore = calculateCompletenessScore(current);
            if (currentScore > bestScore) {
                best = current;
                bestScore = currentScore;
            }
        }
        
        log.debug("选择最完整的结果 [{}] 得分: {} / 6", best.getSource(), bestScore);
        return best;
    }
    
    /**
     * 计算结果完整度得分
     */
    private int calculateCompletenessScore(IpLocation location) {
        int score = 0;
        if (!isEmpty(location.getCountry())) score += 1;
        if (!isEmpty(location.getProvince())) score += 1;
        if (!isEmpty(location.getCity())) score += 2;
        if (location.getLatitude() != null && location.getLongitude() != null) score += 2;
        return score;
    }
    
    /**
     * 验证解析结果是否有效
     * <p>
     * 有效标准：必须同时包含国家、城市和坐标信息
     * 
     * @param location 解析结果
     * @return true-有效，false-无效
     */
    private boolean isValidResult(IpLocation location) {
        if (location == null) {
            return false;
        }
        
        boolean hasCountry = !isEmpty(location.getCountry());
        boolean hasCity = !isEmpty(location.getCity());
        boolean hasLatitude = location.getLatitude() != null;
        boolean hasLongitude = location.getLongitude() != null;
        
        // 必须同时包含国家、城市和坐标
        if (!hasCountry || !hasCity || !hasLatitude || !hasLongitude) {
            log.debug("解析结果不完整，国家: {}, 城市: {}, 坐标: ({}, {})", 
                     location.getCountry(), location.getCity(),
                     location.getLatitude(), location.getLongitude());
            return false;
        }
        
        return true;
    }
    
    /**
     * 使用GeoLite2本地数据库解析IP
     */
    private IpLocation locateByGeoLite2(String ip) {
        if (geoLite2Reader == null) {
            return buildErrorResult(ip, "GeoLite2数据库未加载");
        }
        
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = geoLite2Reader.city(ipAddress);
            
            // 获取国家信息
            Country country = response.getCountry();
            String countryName = country.getNames().get("zh-CN");
            if (countryName == null || countryName.isEmpty()) {
                countryName = country.getName();
            }
            String countryCode = country.getIsoCode();
            
            // 获取城市信息
            City city = response.getCity();
            String cityName = city.getNames().get("zh-CN");
            if (cityName == null || cityName.isEmpty()) {
                cityName = city.getName();
            }
            
            // 获取省份/州信息
            Subdivision subdivision = response.getMostSpecificSubdivision();
            String provinceName = subdivision.getNames().get("zh-CN");
            if (provinceName == null || provinceName.isEmpty()) {
                provinceName = subdivision.getName();
            }
            
            // 获取坐标信息
            Location location = response.getLocation();
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();
            
            if (isEmpty(countryName) && isEmpty(cityName)) {
                return buildErrorResult(ip, "GeoLite2返回数据为空");
            }
            
            CountryContinentEnum countryEnum = matchCountryEnum(countryName, countryCode);
            
            return IpLocation.builder()
                    .ip(ip)
                    .country(countryName)
                    .countryCode(countryCode)
                    .countryEnum(countryEnum)
                    .province(provinceName)
                    .city(cityName)
                    .latitude(latitude)
                    .longitude(longitude)
                    .source("geolite2")
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.debug("GeoLite2查询失败，IP: {}, 错误: {}", ip, e.getMessage());
            return buildErrorResult(ip, "GeoLite2查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用高德API解析IP（仅国内IP）
     */
    private IpLocation locateByAmap(String ip) {
        try {
            String url = amapIpConfig.getBaseUrl() + 
                        "?key=" + amapIpConfig.getKey() + 
                        "&ip=" + ip + 
                        "&output=json";
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("高德API请求失败，状态码: {}", response.getStatus());
                return buildErrorResult(ip, "高德API请求失败");
            }
            
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);
            
            // 检查响应状态
            String status = json.getStr("status");
            String infocode = json.getStr("infocode");
            
            if (!"1".equals(status) || !"10000".equals(infocode)) {
                log.debug("高德API返回错误，status: {}, infocode: {}, info: {}", 
                         status, infocode, json.getStr("info"));
                return buildErrorResult(ip, "高德API返回错误: " + json.getStr("info"));
            }
            
            // 解析结果
            String province = json.getStr("province");
            String city = json.getStr("city");
            String rectangle = json.getStr("rectangle");
            
            // 如果没有省份和城市数据，说明可能是国外IP
            if (isEmpty(province) && isEmpty(city)) {
                log.debug("高德API无法解析IP（可能是国外IP）: {}", ip);
                return buildErrorResult(ip, "高德API无法解析此IP");
            }
            
            // 解析坐标（rectangle格式：113.1017375,22.93212254;113.6770499,23.3809537）
            Double latitude = null;
            Double longitude = null;
            if (rectangle != null && rectangle.contains(";")) {
                String[] parts = rectangle.split(";");
                if (parts.length > 0) {
                    String[] coords = parts[0].split(",");
                    if (coords.length == 2) {
                        try {
                            longitude = Double.parseDouble(coords[0]);
                            latitude = Double.parseDouble(coords[1]);
                        } catch (NumberFormatException e) {
                            log.warn("解析高德坐标失败: {}", rectangle);
                        }
                    }
                }
            }


            CountryContinentEnum countryEnum = CountryContinentEnum.CN;
            return IpLocation.builder()
                    .ip(ip)
                    .country("中国")
                    .countryCode("CN")
                    .countryEnum(countryEnum)
                    .province(province)
                    .city(city)
                    .latitude(latitude)
                    .longitude(longitude)
                    .source("amap")
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("高德API调用异常，IP: {}", ip, e);
            return buildErrorResult(ip, "高德API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 使用 api.ip.sb 解析IP
     */
    private IpLocation locateByIpSb(String ip) {
        try {
            String url = API_IPSB_URL + ip;
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("api.ip.sb请求失败，状态码: {}", response.getStatus());
                return buildErrorResult(ip, "api.ip.sb请求失败");
            }
            
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);
            
            String country = json.getStr("country");
            String city = json.getStr("city");
            String region = json.getStr("region");
            Double latitude = json.getDouble("latitude");
            Double longitude = json.getDouble("longitude");
            
            if (isEmpty(country) && isEmpty(city)) {
                return buildErrorResult(ip, "api.ip.sb返回数据为空");
            }
            
            CountryContinentEnum countryEnum = matchCountryEnum(country, null);
            
            return IpLocation.builder()
                    .ip(ip)
                    .country(country)
                    .countryCode(countryEnum.getShortCode())
                    .countryEnum(countryEnum)
                    .province(region)
                    .city(city)
                    .latitude(latitude)
                    .longitude(longitude)
                    .source("ipsb")
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("api.ip.sb调用异常，IP: {}", ip, e);
            return buildErrorResult(ip, "api.ip.sb调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 使用 ipapi.co 解析IP
     */
    private IpLocation locateByIpapi(String ip) {
        try {
            String url = API_IPAPI_URL + ip + "/json/";
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("ipapi.co请求失败，状态码: {}", response.getStatus());
                return buildErrorResult(ip, "ipapi.co请求失败");
            }
            
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);
            
            String countryName = json.getStr("country_name");
            String countryCode = json.getStr("country_code");
            String city = json.getStr("city");
            String region = json.getStr("region");
            Double latitude = json.getDouble("latitude");
            Double longitude = json.getDouble("longitude");
            
            if (isEmpty(countryName) && isEmpty(city)) {
                return buildErrorResult(ip, "ipapi.co返回数据为空");
            }
            
            CountryContinentEnum countryEnum = matchCountryEnum(countryName, countryCode);
            
            return IpLocation.builder()
                    .ip(ip)
                    .country(countryName)
                    .countryCode(countryCode)
                    .countryEnum(countryEnum)
                    .province(region)
                    .city(city)
                    .latitude(latitude)
                    .longitude(longitude)
                    .source("ipapi")
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("ipapi.co调用异常，IP: {}", ip, e);
            return buildErrorResult(ip, "ipapi.co调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 使用 api.ipapi.is 解析IP
     */
    private IpLocation locateByIpapis(String ip) {
        try {
            String url = API_IPAPIS_URL + ip;
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("api.ipapi.is请求失败，状态码: {}", response.getStatus());
                return buildErrorResult(ip, "api.ipapi.is请求失败");
            }
            
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);
            
            JSONObject location = json.getJSONObject("location");
            if (location == null) {
                return buildErrorResult(ip, "api.ipapi.is返回数据格式错误");
            }
            
            String country = location.getStr("country");
            String countryCode = location.getStr("country_code");
            String city = location.getStr("city");
            String state = location.getStr("state");
            Double latitude = location.getDouble("latitude");
            Double longitude = location.getDouble("longitude");
            
            if (isEmpty(country) && isEmpty(city)) {
                return buildErrorResult(ip, "api.ipapi.is返回数据为空");
            }
            
            CountryContinentEnum countryEnum = matchCountryEnum(country, countryCode);
            
            return IpLocation.builder()
                    .ip(ip)
                    .country(country)
                    .countryCode(countryCode)
                    .countryEnum(countryEnum)
                    .province(state)
                    .city(city)
                    .latitude(latitude)
                    .longitude(longitude)
                    .source("ipapis")
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("api.ipapi.is调用异常，IP: {}", ip, e);
            return buildErrorResult(ip, "api.ipapi.is调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 构建错误结果
     */
    private IpLocation buildErrorResult(String ip, String errorMessage) {
        return IpLocation.builder()
                .ip(ip)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 匹配国家枚举
     * <p>
     * 优先使用国家代码匹配，其次使用国家名称匹配
     * 
     * @param countryName 国家名称
     * @param countryCode 国家代码
     * @return 匹配的国家枚举，未匹配到返回null
     */
    private CountryContinentEnum matchCountryEnum(String countryName, String countryCode) {
        // 1. 优先使用国家代码匹配（最准确）
        if (!isEmpty(countryCode)) {
            CountryContinentEnum byCode = CountryContinentEnum.fromShortCode(countryCode);
            if (byCode != null) {
                log.debug("通过国家代码匹配到枚举: {} -> {}", countryCode, byCode);
                return byCode;
            }
        }
        
        // 2. 使用国家名称匹配（支持中英文及别名）
        if (!isEmpty(countryName)) {
            // 尝试中文名称匹配
            CountryContinentEnum byCn = CountryContinentEnum.fromChineseName(countryName);
            if (byCn != null) {
                log.debug("通过中文名称匹配到枚举: {} -> {}", countryName, byCn);
                return byCn;
            }
            
            // 尝试英文名称匹配（支持别名和模糊匹配）
            CountryContinentEnum byEn = CountryContinentEnum.fromEnglishName(countryName);
            if (byEn != null) {
                log.debug("通过英文名称匹配到枚举: {} -> {}", countryName, byEn);
                return byEn;
            }
        }
        
        log.debug("未能匹配到国家枚举，countryName: {}, countryCode: {}", countryName, countryCode);
        return null;
    }
    
    /**
     * 判断字符串是否为空
     * <p>
     * 以下情况视为空：
     * - null
     * - 空字符串 ""
     * - 空白字符串 "   "
     * - 字符串 "[]" （空数组）
     * - 字符串 "null" （某些API返回字符串null）
     * 
     * @param str 待判断字符串
     * @return true-为空，false-非空
     */
    private boolean isEmpty(String str) {
        if (str == null || str.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = str.trim();
        
        // 判断是否为空数组字符串
        if ("[]".equals(trimmed)) {
            return true;
        }
        
        // 判断是否为字符串"null"
        if ("null".equalsIgnoreCase(trimmed)) {
            return true;
        }
        
        return false;
    }
}
