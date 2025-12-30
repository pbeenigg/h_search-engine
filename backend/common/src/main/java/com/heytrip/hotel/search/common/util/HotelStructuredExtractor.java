package com.heytrip.hotel.search.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.search.common.enums.CountryContinentEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 酒店结构化信息解析工具
 * 从 raw 原文中解析并补齐：国家/国家码/城市/区域/洲/地址/经纬度/酒店集团/酒店品牌。
 * - providerSource = Elong | Agoda
 * - 路径依据《酒店数据源接口API 文档.md》
 */
public class HotelStructuredExtractor {

    private static final ObjectMapper M = new ObjectMapper();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        /**
         * 中文名（可能为空）
         */
        private String nameCn;
        /**
         * 英文名（可能为空）
         */
        private String nameEn;

        // 国家信息
        private String countryCn;
        private String countryEn;
        private String countryCode;

        // 城市信息
        private String cityCn;
        private String cityEn;

        // 区域
        private String regionCn;
        private String regionEn;

        // 洲信息
        private String continentCn;
        private String continentEn;

        // 地址信息
        private String addressCn;
        private String addressEn;

        // 经纬度
        private BigDecimal longitude;
        private BigDecimal latitude;

        // 酒店集团
        private String hotelGroupCn;
        private String hotelGroupEn;

        // 酒店品牌
        private String hotelBrandCn;
        private String hotelBrandEn;

        // 酒店描述（可能为空）
        private String descriptionCn;
        private String descriptionEn;
    }

    /**
     * @param raw            原文（可能为转义 JSON 字符串）
     * @param providerSource Elong|Agoda
     */
    public static Result extract(String raw, String providerSource) {
        if (raw == null || raw.isBlank()) return new Result();
        try {
            String json = tryUnescapeToJson(raw);
            JsonNode root = M.readTree(json);
            if ("Elong".equalsIgnoreCase(providerSource)) {
                return fromElong(root);
            } else {
                return fromAgoda(root);
            }
        } catch (Exception e) {
            return new Result();
        }
    }


    /**
     * 提取 TagSource 信息
     * 业务域过滤（可选：CN/INTL/HMT）
     * 中国大陆  = CN  | 港澳台 = HMT | 国际其他国家 = INTL
     * 根据解析出的国家名称，映射到业务域标签
     *
     * @param raw            酒店原文 JSON
     * @param providerSource 数据源（Elong/Agoda）
     * @return 返回业务域标签（CN/HMT/INTL）
     */
    public static String extractTagSource(String raw, String providerSource) {
        if (raw == null || raw.isBlank() || providerSource == null) {
            return "INTL"; // 默认返回国际
        }

        try {
            JsonNode root = M.readTree(raw);
            String countryEn = null;
            String countryCode = null;

            // 根据不同数据源提取国家信息
            if ("Elong".equalsIgnoreCase(providerSource)) {
                // 艺龙数据：从 Result.Detail.CountryNameEn 或 Result.Detail.CountryCode 提取
                countryEn = text(root, "Result.Detail.CountryNameEn");
                countryCode = text(root, "Result.Detail.CountryCode");
            } else if ("Agoda".equalsIgnoreCase(providerSource)) {
                // Agoda 数据：从 summary.address.countryName 或 summary.countryCode 提取
                countryEn = text(root, "summary.address.countryName");
                countryCode = text(root, "summary.countryCode");
            }

            // 优先使用国家代码匹配
            if (isNotBlank(countryCode)) {
                return mapCountryCodeToTagSource(countryCode);
            }

            // 其次使用国家英文名匹配
            if (isNotBlank(countryEn)) {
                return mapCountryNameToTagSource(countryEn);
            }

            // 无法识别，默认返回国际
            return "INTL";

        } catch (Exception e) {
            // 解析失败，默认返回国际
            return "INTL";
        }
    }

    /**
     * 根据国家代码映射到业务域标签
     *
     * @param countryCode 国家代码（如 CN/HK/MO/TW/US 等）
     * @return 业务域标签（CN/HMT/INTL）
     */
    private static String mapCountryCodeToTagSource(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "INTL";
        }

        String code = countryCode.trim().toUpperCase();

        // 中国大陆
        if ("CN".equals(code)) {
            return "CN";
        }

        // 港澳台
        if ("HK".equals(code) || "MO".equals(code) || "TW".equals(code)) {
            return "HMT";
        }

        // 其他国家
        return "INTL";
    }

    /**
     * 根据国家英文名映射到业务域标签
     *
     * @param countryEn 国家英文名
     * @return 业务域标签（CN/HMT/INTL）
     */
    private static String mapCountryNameToTagSource(String countryEn) {
        if (countryEn == null || countryEn.isBlank()) {
            return "INTL";
        }

        // 使用 CountryContinentEnum 进行匹配
        CountryContinentEnum country = CountryContinentEnum.fromEnglishName(countryEn);

        if (country == null) {
            return "INTL";
        }

        // 根据枚举值判断业务域
        switch (country) {
            case CN:
                return "CN";
            case HK:
            case MO:
            case TW:
                return "HMT";
            default:
                return "INTL";
        }
    }

    private static Result fromElong(JsonNode root) {
        // 原始字段提取
        String nameCn = text(root, "Result.Detail.HotelName");
        String nameEn = text(root, "Result.Detail.HotelNameEn");
        String countryCn = text(root, "Result.Detail.CountryName");
        String countryEn = text(root, "Result.Detail.CountryNameEn");
        String city = text(root, "Result.Detail.CityName");
        String cityEn = text(root, "Result.Detail.CityNameEn");
        String region = text(root, "Result.Detail.DistrictName");
        String regionEn = text(root, "Result.Detail.DistrictNameEn");
        String address = text(root, "Result.Detail.Address");
        String addressEn = text(root, "Result.Detail.AddressEn");
        
        // 智能语言识别和字段分配
        // 酒店名称：如果中英文字段有混合情况，智能识别并重新分配
        LanguageDetector.BilingualField hotelName = LanguageDetector.smartMerge(nameCn, nameEn, null);
        nameCn = hotelName.getChinese();
        nameEn = hotelName.getEnglish();
        
        // 国家名称：智能识别
        LanguageDetector.BilingualField country = LanguageDetector.smartMerge(countryCn, countryEn, null);
        countryCn = country.getChinese();
        countryEn = country.getEnglish();
        
        // 城市名称：智能识别
        LanguageDetector.BilingualField cityField = LanguageDetector.smartMerge(city, cityEn, null);
        city = cityField.getChinese();
        cityEn = cityField.getEnglish();
        
        // 地区名称：智能识别
        LanguageDetector.BilingualField regionField = LanguageDetector.smartMerge(region, regionEn, null);
        region = regionField.getChinese();
        regionEn = regionField.getEnglish();
        
        // 地址：智能识别（优先使用专用字段，如果为空则尝试智能分配）
        LanguageDetector.BilingualField addressField = LanguageDetector.smartMerge(address, addressEn, null);
        address = addressField.getChinese();
        addressEn = addressField.getEnglish();
        BigDecimal lon = toLon(text(root, "Result.Detail.GoogleLon"));
        BigDecimal lat = toLat(text(root, "Result.Detail.GoogleLat"));
        
        // 酒店集团：智能识别
        String groupCn = text(root, "Result.Detail.GroupName");
        String groupEn = text(root, "Result.Detail.GroupNameEn");
        LanguageDetector.BilingualField groupField = LanguageDetector.smartMerge(groupCn, groupEn, null);
        groupCn = groupField.getChinese();
        groupEn = groupField.getEnglish();
        
        // 酒店品牌：智能识别
        String brandCn = text(root, "Result.Detail.BrandName");
        String brandEn = text(root, "Result.Detail.BrandNameEn");
        LanguageDetector.BilingualField brandField = LanguageDetector.smartMerge(brandCn, brandEn, null);
        brandCn = brandField.getChinese();
        brandEn = brandField.getEnglish();

        // 酒店介绍和描述处理：智能识别
        String introCn = text(root, "Result.Detail.IntroEditor");
        String introEn = text(root, "Result.Detail.IntroEditorEn");
        String descriptionCn = text(root, "Result.Detail.Description");
        String descriptionEn = text(root, "Result.Detail.DescriptionEn");
        
        // 合并描述字段并智能识别
        LanguageDetector.BilingualField descField = LanguageDetector.smartMerge(
            firstNonBlank(introCn, descriptionCn),
            firstNonBlank(introEn, descriptionEn),
            null
        );
        String firstDescriptionCn = descField.getChinese();
        String firstDescriptionEn = descField.getEnglish();


        // 默认值CN
        String countryCode = "CN";
        String continentEn = "Asia";
        String continentCn = "亚洲";
        if (isNotBlank(countryCn)) {  //中文国家名不为空
            CountryContinentEnum continentEnum = CountryContinentEnum.fromChineseName(countryCn);
            if (continentEnum != null) {
                countryCode = continentEnum.getShortCode();
                countryCn = continentEnum.getNameCn();
                countryEn = continentEnum.getNameEn();
                continentEn = continentEnum.getContinent().getNameEn();
                continentCn = continentEnum.getContinent().getNameCn();

            }
        } else if (isNotBlank(countryEn)) { //英文国家名不为空
            CountryContinentEnum continentEnum = CountryContinentEnum.fromEnglishName(countryEn);
            if (continentEnum != null) {
                countryCode = continentEnum.getShortCode();
                countryCn = continentEnum.getNameCn();
                countryEn = continentEnum.getNameEn();
                continentEn = continentEnum.getContinent().getNameEn();
                continentCn = continentEnum.getContinent().getNameCn();
            }
        }
        return Result.builder()
                .nameCn(emptyToNull(nameCn))
                .nameEn(emptyToNull(nameEn))
                .continentCn(emptyToNull(continentCn))
                .continentEn(emptyToNull(continentEn))
                .countryCn(emptyToNull(countryCn))
                .countryEn(emptyToNull(countryEn))
                .countryCode(emptyToNull(countryCode.toUpperCase()))
                .cityCn(emptyToNull(city))
                .cityEn(emptyToNull(cityEn))
                .regionCn(emptyToNull(region))
                .regionEn(emptyToNull(regionEn))
                .addressCn(emptyToNull(address))
                .addressEn(emptyToNull(addressEn))
                .longitude(lon)
                .latitude(lat)
                .hotelGroupCn(emptyToNull(groupCn))
                .hotelGroupEn(emptyToNull(groupEn))
                .hotelBrandCn(emptyToNull(brandCn))
                .hotelBrandEn(emptyToNull(brandEn))
                .descriptionCn(emptyToNull(firstDescriptionCn))
                .descriptionEn(emptyToNull(firstDescriptionEn))
                .build();
    }

    private static Result fromAgoda(JsonNode root) {
        // 原始字段提取
        String name = text(root, "summary.propertyName.englishName");
        String addr1 = text(root, "summary.address.address1");
        String addr2 = text(root, "summary.address.address2");
        String address = join(addr1, addr2);
        String city = text(root, "summary.address.cityName");
        String region = text(root, "summary.address.areaName");
        String country = text(root, "summary.address.countryName");
        String countryCode = text(root, "summary.countryCode");
        String continent = text(root, "summary.address.regionName");
        BigDecimal lat = toLat(text(root, "summary.coordinate.lat"));
        BigDecimal lon = toLon(text(root, "summary.coordinate.lng"));
        String descriptionLong = text(root, "description.long");
        String descriptionShort = text(root, "description.short");
        String description = firstNonBlank(descriptionLong, descriptionShort);

        // 智能语言识别和字段分配
        // 酒店名称：Agoda 数据可能包含中英文混合
        LanguageDetector.BilingualField hotelName = LanguageDetector.smartAssign(name);
        String nameCn = hotelName.getChinese();
        String nameEn = hotelName.getEnglish();
        
        // 地址：智能识别
        LanguageDetector.BilingualField addressField = LanguageDetector.smartAssign(address);
        String addressCn = addressField.getChinese();
        String addressEn = addressField.getEnglish();
        
        // 城市：智能识别
        LanguageDetector.BilingualField cityField = LanguageDetector.smartAssign(city);
        String cityCn = cityField.getChinese();
        String cityEn = cityField.getEnglish();
        
        // 地区：智能识别
        LanguageDetector.BilingualField regionField = LanguageDetector.smartAssign(region);
        String regionCn = regionField.getChinese();
        String regionEn = regionField.getEnglish();
        
        // 国家：智能识别
        LanguageDetector.BilingualField countryField = LanguageDetector.smartAssign(country);
        String countryCn = countryField.getChinese();
        String countryEn = countryField.getEnglish();
        
        // 洲：智能识别
        LanguageDetector.BilingualField continentField = LanguageDetector.smartAssign(continent);
        String continentCn = continentField.getChinese();
        String continentEn = continentField.getEnglish();
        
        // 描述：智能识别
        LanguageDetector.BilingualField descField = LanguageDetector.smartAssign(description);
        String descriptionCn = descField.getChinese();
        String descriptionEn = descField.getEnglish();

        // 通过国家枚举补齐国家和洲的中英文信息
        if (isNotBlank(countryEn)) {
            CountryContinentEnum continentEnum = CountryContinentEnum.fromEnglishName(countryEn);
            if (continentEnum != null) {
                // 如果中文国家名为空，从枚举中获取
                if (countryCn == null) {
                    countryCn = continentEnum.getNameCn();
                }
                // 如果洲的中文名为空，从枚举中获取
                if (continentCn == null) {
                    continentCn = continentEnum.getContinent().getNameCn();
                }
                // 如果洲的英文名为空，从枚举中获取
                if (continentEn == null) {
                    continentEn = continentEnum.getContinent().getNameEn();
                }
            }
        }

        // Agoda 未提供集团/品牌统一字段，这里置空
        return Result.builder()
                .nameCn(emptyToNull(nameCn))
                .nameEn(emptyToNull(nameEn))
                .countryEn(emptyToNull(countryEn))
                .countryCn(emptyToNull(countryCn))
                .countryCode(emptyToNull(countryCode != null ? countryCode.toUpperCase() : null))
                .continentEn(emptyToNull(continentEn))
                .continentCn(emptyToNull(continentCn))
                .cityCn(emptyToNull(cityCn))
                .cityEn(emptyToNull(cityEn))
                .regionCn(emptyToNull(regionCn))
                .regionEn(emptyToNull(regionEn))
                .addressCn(emptyToNull(addressCn))
                .addressEn(emptyToNull(addressEn))
                .longitude(lon)
                .latitude(lat)
                .hotelGroupEn(null)
                .hotelBrandEn(null)
                .descriptionCn(emptyToNull(descriptionCn))
                .descriptionEn(emptyToNull(descriptionEn))
                .build();
    }

    private static String tryUnescapeToJson(String input) {
        String trimmed = input.trim();
        try {
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return trimmed;
            }
            String unescaped = M.readValue(trimmed, String.class);
            return unescaped;
        } catch (Exception ignore) {
            return trimmed;
        }
    }

    private static String text(JsonNode root, String path) {
        String[] parts = path.split("\\.");
        JsonNode cur = root;
        for (String p : parts) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        if (cur == null || cur.isNull()) return null;
        if (cur.isTextual()) return cur.asText();
        if (cur.isNumber()) return cur.numberValue().toString();
        return cur.toString();
    }

    private static BigDecimal toLat(String s) {
        try {
            if (isBlank(s)) return null;
            double v = Double.parseDouble(s);
            if (v < -90 || v > 90) return null;
            return BigDecimal.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal toLon(String s) {
        try {
            if (isBlank(s)) return null;
            double v = Double.parseDouble(s);
            if (v < -180 || v > 180) return null;
            return BigDecimal.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static String join(String a, String b) {
        if (isBlank(a) && isBlank(b)) return null;
        if (isBlank(a)) return b;
        if (isBlank(b)) return a;
        return a + ", " + b;
    }

    private static String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (!isBlank(b) ? b : null);
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
