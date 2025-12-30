package com.heytrip.hotel.search.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 语言检测工具类测试
 */
class LanguageDetectorTest {

    @Test
    void testContainsChinese() {
        assertTrue(LanguageDetector.containsChinese("北京"));
        assertTrue(LanguageDetector.containsChinese("Beijing北京"));
        assertFalse(LanguageDetector.containsChinese("Beijing"));
        assertFalse(LanguageDetector.containsChinese(""));
        assertFalse(LanguageDetector.containsChinese(null));
    }

    @Test
    void testContainsEnglish() {
        assertTrue(LanguageDetector.containsEnglish("Beijing"));
        assertTrue(LanguageDetector.containsEnglish("Beijing北京"));
        assertFalse(LanguageDetector.containsEnglish("北京"));
        assertFalse(LanguageDetector.containsEnglish(""));
        assertFalse(LanguageDetector.containsEnglish(null));
    }

    @Test
    void testDetectLanguage() {
        // 纯中文
        assertEquals("zh", LanguageDetector.detectLanguage("北京市朝阳区"));
        assertEquals("zh", LanguageDetector.detectLanguage("希尔顿酒店"));
        
        // 纯英文
        assertEquals("en", LanguageDetector.detectLanguage("Beijing"));
        assertEquals("en", LanguageDetector.detectLanguage("Hilton Hotel"));
        
        // 中英混合（中文为主）
        assertEquals("zh", LanguageDetector.detectLanguage("北京市朝阳区希尔顿酒店Hilton"));
        
        // 中英混合（英文为主）
        assertEquals("en", LanguageDetector.detectLanguage("Hilton Beijing Hotel"));
        
        // 空值
        assertEquals("unknown", LanguageDetector.detectLanguage(""));
        assertEquals("unknown", LanguageDetector.detectLanguage(null));
    }

    @Test
    void testSmartAssign_PureChinese() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssign("北京市朝阳区");
        
        assertEquals("北京市朝阳区", result.getChinese());
        assertNull(result.getEnglish());
    }

    @Test
    void testSmartAssign_PureEnglish() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssign("Beijing Chaoyang District");
        
        assertNull(result.getChinese());
        assertEquals("Beijing Chaoyang District", result.getEnglish());
    }

    @Test
    void testSmartAssign_Mixed_ChineseDominant() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssign("北京市朝阳区希尔顿酒店Hilton");
        
        assertEquals("北京市朝阳区希尔顿酒店Hilton", result.getChinese());
        assertNull(result.getEnglish());
    }

    @Test
    void testSmartAssign_Mixed_EnglishDominant() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssign("Hilton Hotel Beijing");
        
        assertNull(result.getChinese());
        assertEquals("Hilton Hotel Beijing", result.getEnglish());
    }

    @Test
    void testSmartAssign_Null() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssign(null);
        
        assertNull(result.getChinese());
        assertNull(result.getEnglish());
    }

    @Test
    void testSmartMerge_BothExisting() {
        LanguageDetector.BilingualField result = LanguageDetector.smartMerge(
            "北京希尔顿酒店",
            "Beijing Hilton Hotel",
            "混合文本"
        );
        
        assertEquals("北京希尔顿酒店", result.getChinese());
        assertEquals("Beijing Hilton Hotel", result.getEnglish());
    }

    @Test
    void testSmartMerge_OnlyChineseExisting() {
        LanguageDetector.BilingualField result = LanguageDetector.smartMerge(
            "北京希尔顿酒店",
            null,
            "Beijing Hilton Hotel"
        );
        
        assertEquals("北京希尔顿酒店", result.getChinese());
        assertEquals("Beijing Hilton Hotel", result.getEnglish());
    }

    @Test
    void testSmartMerge_OnlyEnglishExisting() {
        LanguageDetector.BilingualField result = LanguageDetector.smartMerge(
            null,
            "Beijing Hilton Hotel",
            "北京希尔顿酒店"
        );
        
        assertEquals("北京希尔顿酒店", result.getChinese());
        assertEquals("Beijing Hilton Hotel", result.getEnglish());
    }

    @Test
    void testSmartMerge_NoneExisting() {
        LanguageDetector.BilingualField result = LanguageDetector.smartMerge(
            null,
            null,
            "Beijing Hilton Hotel"
        );
        
        assertNull(result.getChinese());
        assertEquals("Beijing Hilton Hotel", result.getEnglish());
    }

    @Test
    void testSmartAssignMultiple() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssignMultiple(
            "北京希尔顿酒店",
            "Beijing Hilton Hotel",
            "其他文本"
        );
        
        assertEquals("北京希尔顿酒店", result.getChinese());
        assertEquals("Beijing Hilton Hotel", result.getEnglish());
    }

    @Test
    void testSmartAssignMultiple_WithNull() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssignMultiple(
            null,
            "",
            "Beijing Hilton Hotel",
            "北京希尔顿酒店"
        );
        
        assertEquals("北京希尔顿酒店", result.getChinese());
        assertEquals("Beijing Hilton Hotel", result.getEnglish());
    }

    @Test
    void testSmartAssignMultiple_Empty() {
        LanguageDetector.BilingualField result = LanguageDetector.smartAssignMultiple();
        
        assertNull(result.getChinese());
        assertNull(result.getEnglish());
    }

    @Test
    void testRealWorldScenarios() {
        // 场景1：酒店名称（中文）
        LanguageDetector.BilingualField hotel1 = LanguageDetector.smartAssign("北京希尔顿酒店");
        assertEquals("北京希尔顿酒店", hotel1.getChinese());
        assertNull(hotel1.getEnglish());

        // 场景2：酒店名称（英文）
        LanguageDetector.BilingualField hotel2 = LanguageDetector.smartAssign("Hilton Beijing Hotel");
        assertNull(hotel2.getChinese());
        assertEquals("Hilton Beijing Hotel", hotel2.getEnglish());

        // 场景3：地址（中文为主）
        LanguageDetector.BilingualField addr1 = LanguageDetector.smartAssign("北京市朝阳区东三环北路8号");
        assertEquals("北京市朝阳区东三环北路8号", addr1.getChinese());
        assertNull(addr1.getEnglish());

        // 场景4：地址（英文）
        LanguageDetector.BilingualField addr2 = LanguageDetector.smartAssign("No.8 East 3rd Ring Road North, Chaoyang District");
        assertNull(addr2.getChinese());
        assertEquals("No.8 East 3rd Ring Road North, Chaoyang District", addr2.getEnglish());

        // 场景5：国家名称（中文）
        LanguageDetector.BilingualField country1 = LanguageDetector.smartAssign("中国");
        assertEquals("中国", country1.getChinese());
        assertNull(country1.getEnglish());

        // 场景6：国家名称（英文）
        LanguageDetector.BilingualField country2 = LanguageDetector.smartAssign("China");
        assertNull(country2.getChinese());
        assertEquals("China", country2.getEnglish());
    }
}
