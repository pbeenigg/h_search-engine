package com.heytrip.hotel.search.common.enums;

import lombok.Getter;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 洲枚举（支持别名匹配与模糊识别）
 */
@Getter
public enum ContinentEnum {
    ASIA("亚洲", "Asia", "Asian"),
    EUROPE("欧洲", "Europe", "European"),
    AFRICA("非洲", "Africa", "African"),
    NORTH_AMERICA("北美洲", "North America", "North American", "Northern America", "N America"),
    SOUTH_AMERICA("南美洲", "South America", "South American", "Southern America", "S America", "Latin America"),
    OCEANIA("大洋洲", "Oceania", "Oceanic", "Australia and Oceania", "Pacific"),
    ANTARCTICA("南极洲", "Antarctica", "Antarctic");

    private final String nameCn;
    private final String nameEn;
    private final Set<String> aliases;

    // 静态缓存：规范化后的别名 -> 枚举映射
    private static final Map<String, ContinentEnum> NORMALIZED_ALIAS_MAP = new HashMap<>();

    static {
        for (ContinentEnum continent : values()) {
            // 主名称
            NORMALIZED_ALIAS_MAP.put(normalizeForMatch(continent.nameEn), continent);
            // 所有别名
            for (String alias : continent.aliases) {
                NORMALIZED_ALIAS_MAP.put(normalizeForMatch(alias), continent);
            }
        }
    }

    ContinentEnum(String nameCn, String nameEn, String... aliases) {
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.aliases = aliases == null ? Collections.emptySet() : Set.of(aliases);
    }

    /**
     * 根据英文名称查找洲（支持别名与模糊匹配）
     * @param nameEn 英文名称
     * @return 匹配的洲枚举，未找到返回 null
     */
    public static ContinentEnum fromEnglishName(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) {
            return null;
        }
        
        // 1. 精确匹配主名称
        for (ContinentEnum continent : values()) {
            if (continent.nameEn.equalsIgnoreCase(nameEn)) {
                return continent;
            }
        }
        
        // 2. 精确匹配别名
        for (ContinentEnum continent : values()) {
            for (String alias : continent.aliases) {
                if (alias.equalsIgnoreCase(nameEn)) {
                    return continent;
                }
            }
        }
        
        // 3. 规范化后模糊匹配（去除空格、大小写等）
        String normalized = normalizeForMatch(nameEn);
        return NORMALIZED_ALIAS_MAP.get(normalized);
    }

    /**
     * 根据中文名称查找洲
     * @param nameCn 中文名称
     * @return 匹配的洲枚举，未找到返回 null
     */
    public static ContinentEnum fromChineseName(String nameCn) {
        if (nameCn == null || nameCn.isBlank()) {
            return null;
        }
        for (ContinentEnum continent : values()) {
            if (continent.nameCn.equals(nameCn)) {
                return continent;
            }
        }
        return null;
    }

    /**
     * 规范化字符串用于匹配：
     * - 转小写
     * - 去除空格、连字符、点号
     */
    private static String normalizeForMatch(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        
        String result = input.toLowerCase().trim();
        
        // Unicode 规范化（NFD 分解 + 去除音标）
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // 去除常见分隔符和标点
        result = result.replaceAll("[\\s\\-_.,']+", "");
        
        // 统一常见词汇
        result = result.replace("&", "and")
                .replace("and", "")
                .replace("the", "");
        
        return result;
    }
}
