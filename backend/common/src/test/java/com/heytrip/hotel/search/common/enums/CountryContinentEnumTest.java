package com.heytrip.hotel.search.common.enums;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 国家-洲枚举测试：验证别名匹配与模糊识别功能
 */
public class CountryContinentEnumTest {

    @Test
    public void testTurkeyAliases() {
        // 土耳其的多种表达
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("Turkey"));
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("Türkiye"));
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("Turkiye"));
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("TURKEY"));
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("turkey"));
    }

    @Test
    public void testRussiaAliases() {
        // 俄罗斯的多种表达
        assertEquals(CountryContinentEnum.RU, CountryContinentEnum.fromEnglishName("Russian Federation"));
        assertEquals(CountryContinentEnum.RU, CountryContinentEnum.fromEnglishName("Russia"));
        assertEquals(CountryContinentEnum.RU, CountryContinentEnum.fromEnglishName("RUSSIA"));
        assertEquals(CountryContinentEnum.RU, CountryContinentEnum.fromEnglishName("russia"));
    }

    @Test
    public void testMyanmarAliases() {
        // 缅甸的多种表达
        assertEquals(CountryContinentEnum.MM, CountryContinentEnum.fromEnglishName("Republic of the Union of Myanmar"));
        assertEquals(CountryContinentEnum.MM, CountryContinentEnum.fromEnglishName("Myanmar"));
        assertEquals(CountryContinentEnum.MM, CountryContinentEnum.fromEnglishName("Burma"));
        assertEquals(CountryContinentEnum.MM, CountryContinentEnum.fromEnglishName("BURMA"));
    }

    @Test
    public void testIranAliases() {
        // 伊朗的多种表达
        assertEquals(CountryContinentEnum.IR, CountryContinentEnum.fromEnglishName("Islamic Republic of Iran"));
        assertEquals(CountryContinentEnum.IR, CountryContinentEnum.fromEnglishName("Iran"));
        assertEquals(CountryContinentEnum.IR, CountryContinentEnum.fromEnglishName("IRAN"));
    }

    @Test
    public void testSyriaAliases() {
        // 叙利亚的多种表达
        assertEquals(CountryContinentEnum.SY, CountryContinentEnum.fromEnglishName("The Syrian Arab Republic"));
        assertEquals(CountryContinentEnum.SY, CountryContinentEnum.fromEnglishName("Syria"));
        assertEquals(CountryContinentEnum.SY, CountryContinentEnum.fromEnglishName("SYRIA"));
    }

    @Test
    public void testKoreaAliases() {
        // 韩国的多种表达
        assertEquals(CountryContinentEnum.KR, CountryContinentEnum.fromEnglishName("South Korea"));
        assertEquals(CountryContinentEnum.KR, CountryContinentEnum.fromEnglishName("Korea"));
        assertEquals(CountryContinentEnum.KR, CountryContinentEnum.fromEnglishName("Republic of Korea"));

        // 朝鲜的多种表达
        assertEquals(CountryContinentEnum.KP, CountryContinentEnum.fromEnglishName("North Korea"));
        assertEquals(CountryContinentEnum.KP, CountryContinentEnum.fromEnglishName("Democratic People's Republic of Korea"));
        assertEquals(CountryContinentEnum.KP, CountryContinentEnum.fromEnglishName("DPRK"));
    }

    @Test
    public void testUKAliases() {
        // 英国的多种表达
        assertEquals(CountryContinentEnum.GB, CountryContinentEnum.fromEnglishName("United Kingdom"));
        assertEquals(CountryContinentEnum.GB, CountryContinentEnum.fromEnglishName("UK"));
        assertEquals(CountryContinentEnum.GB, CountryContinentEnum.fromEnglishName("Britain"));
        assertEquals(CountryContinentEnum.GB, CountryContinentEnum.fromEnglishName("Great Britain"));
    }

    @Test
    public void testUSAAliases() {
        // 美国的多种表达
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromEnglishName("United States"));
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromEnglishName("USA"));
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromEnglishName("United States of America"));
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromEnglishName("America"));
    }

    @Test
    public void testNormalizationWithSpaces() {
        // 测试带空格、连字符的规范化
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromEnglishName("United  States"));
        assertEquals(CountryContinentEnum.GB, CountryContinentEnum.fromEnglishName("Great Britain"));
        assertEquals(CountryContinentEnum.KR, CountryContinentEnum.fromEnglishName("South  Korea"));
    }

    @Test
    public void testNormalizationWithDiacritics() {
        // 测试音标符号的规范化
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("Türkiye"));
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromEnglishName("Turkiye"));
    }

    @Test
    public void testCaseInsensitive() {
        // 测试大小写不敏感
        assertEquals(CountryContinentEnum.CN, CountryContinentEnum.fromEnglishName("china"));
        assertEquals(CountryContinentEnum.CN, CountryContinentEnum.fromEnglishName("CHINA"));
        assertEquals(CountryContinentEnum.CN, CountryContinentEnum.fromEnglishName("China"));
    }

    @Test
    public void testNotFound() {
        // 测试不存在的国家
        assertNull(CountryContinentEnum.fromEnglishName("Atlantis"));
        assertNull(CountryContinentEnum.fromEnglishName(""));
        assertNull(CountryContinentEnum.fromEnglishName(null));
    }

    @Test
    public void testContinentMapping() {
        // 验证洲信息正确
        assertEquals(ContinentEnum.ASIA, CountryContinentEnum.TR.getContinent());
        assertEquals(ContinentEnum.EUROPE, CountryContinentEnum.RU.getContinent());
        assertEquals(ContinentEnum.NORTH_AMERICA, CountryContinentEnum.US.getContinent());
    }

    @Test
    public void testShortCodeLookup() {
        // 测试国家代码查找
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromShortCode("TR"));
        assertEquals(CountryContinentEnum.RU, CountryContinentEnum.fromShortCode("RU"));
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromShortCode("US"));
        assertEquals(CountryContinentEnum.CN, CountryContinentEnum.fromShortCode("cn"));
    }

    @Test
    public void testChineseNameLookup() {
        // 测试中文名查找
        assertEquals(CountryContinentEnum.TR, CountryContinentEnum.fromChineseName("土耳其"));
        assertEquals(CountryContinentEnum.RU, CountryContinentEnum.fromChineseName("俄罗斯"));
        assertEquals(CountryContinentEnum.US, CountryContinentEnum.fromChineseName("美国"));
        assertEquals(CountryContinentEnum.CN, CountryContinentEnum.fromChineseName("中国"));
    }
}
