package com.heytrip.hotel.search.common.enums;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 洲枚举测试：验证别名匹配与模糊识别功能
 */
class ContinentEnumTest {

    @Test
    void testAsiaAliases() {
        // 亚洲的多种表达
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("Asia"));
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("Asian"));
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("ASIA"));
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("asia"));
    }

    @Test
    void testEuropeAliases() {
        // 欧洲的多种表达
        assertEquals(ContinentEnum.EUROPE, ContinentEnum.fromEnglishName("Europe"));
        assertEquals(ContinentEnum.EUROPE, ContinentEnum.fromEnglishName("European"));
        assertEquals(ContinentEnum.EUROPE, ContinentEnum.fromEnglishName("EUROPE"));
    }

    @Test
    void testAfricaAliases() {
        // 非洲的多种表达
        assertEquals(ContinentEnum.AFRICA, ContinentEnum.fromEnglishName("Africa"));
        assertEquals(ContinentEnum.AFRICA, ContinentEnum.fromEnglishName("African"));
        assertEquals(ContinentEnum.AFRICA, ContinentEnum.fromEnglishName("AFRICA"));
    }

    @Test
    void testNorthAmericaAliases() {
        // 北美洲的多种表达
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("North America"));
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("North American"));
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("Northern America"));
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("N America"));
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("NORTH AMERICA"));
    }

    @Test
    void testSouthAmericaAliases() {
        // 南美洲的多种表达
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("South America"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("South American"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("Southern America"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("S America"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("Latin America"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("SOUTH AMERICA"));
    }

    @Test
    void testOceaniaAliases() {
        // 大洋洲的多种表达
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("Oceania"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("Oceanic"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("Australia and Oceania"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("Pacific"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("OCEANIA"));
    }

    @Test
    void testAntarcticaAliases() {
        // 南极洲的多种表达
        assertEquals(ContinentEnum.ANTARCTICA, ContinentEnum.fromEnglishName("Antarctica"));
        assertEquals(ContinentEnum.ANTARCTICA, ContinentEnum.fromEnglishName("Antarctic"));
        assertEquals(ContinentEnum.ANTARCTICA, ContinentEnum.fromEnglishName("ANTARCTICA"));
    }

    @Test
    void testNormalizationWithSpaces() {
        // 测试带空格、连字符的规范化
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("North  America"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("South-America"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("Australia  and  Oceania"));
    }

    @Test
    void testCaseInsensitive() {
        // 测试大小写不敏感
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("asia"));
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("ASIA"));
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromEnglishName("Asia"));
        assertEquals(ContinentEnum.EUROPE, ContinentEnum.fromEnglishName("european"));
    }

    @Test
    void testChineseNameLookup() {
        // 测试中文名查找
        assertEquals(ContinentEnum.ASIA, ContinentEnum.fromChineseName("亚洲"));
        assertEquals(ContinentEnum.EUROPE, ContinentEnum.fromChineseName("欧洲"));
        assertEquals(ContinentEnum.AFRICA, ContinentEnum.fromChineseName("非洲"));
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromChineseName("北美洲"));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromChineseName("南美洲"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromChineseName("大洋洲"));
        assertEquals(ContinentEnum.ANTARCTICA, ContinentEnum.fromChineseName("南极洲"));
    }

    @Test
    void testNotFound() {
        // 测试不存在的洲
        assertNull(ContinentEnum.fromEnglishName("Atlantis"));
        assertNull(ContinentEnum.fromEnglishName(""));
        assertNull(ContinentEnum.fromEnglishName(null));
        assertNull(ContinentEnum.fromChineseName(""));
        assertNull(ContinentEnum.fromChineseName(null));
    }

    @Test
    void testAllContinents() {
        // 验证所有洲都能正确查找
        assertEquals(7, ContinentEnum.values().length);
        
        assertNotNull(ContinentEnum.fromEnglishName("Asia"));
        assertNotNull(ContinentEnum.fromEnglishName("Europe"));
        assertNotNull(ContinentEnum.fromEnglishName("Africa"));
        assertNotNull(ContinentEnum.fromEnglishName("North America"));
        assertNotNull(ContinentEnum.fromEnglishName("South America"));
        assertNotNull(ContinentEnum.fromEnglishName("Oceania"));
        assertNotNull(ContinentEnum.fromEnglishName("Antarctica"));
    }

    @Test
    void testEdgeCases() {
        // 边界情况测试
        assertEquals(ContinentEnum.NORTH_AMERICA, ContinentEnum.fromEnglishName("  North America  "));
        assertEquals(ContinentEnum.SOUTH_AMERICA, ContinentEnum.fromEnglishName("Latin-America"));
        assertEquals(ContinentEnum.OCEANIA, ContinentEnum.fromEnglishName("pacific"));
    }
}
