package com.heytrip.hotel.search.infra.search;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分词过滤功能测试（符号清洗版）
 */
class TokenFilterTest {

    /**
     * 模拟 EsHotelIndexServiceJavaClient 中的过滤逻辑
     */
    private List<String> filterValidTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        return tokens.stream()
                .filter(token -> token != null && !token.isBlank())
                .map(String::trim)
                .map(this::cleanToken)
                .filter(token -> token != null && !token.isEmpty())
                .filter(this::isValidToken)
                .distinct()
                .toList();
    }

    private String cleanToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        // 提取所有有效字符（中文、英文、数字、空格、连字符）
        StringBuilder cleaned = new StringBuilder();
        for (char c : token.toCharArray()) {
            if (Character.isLetterOrDigit(c) || isChinese(c) || c == ' ' || c == '-') {
                cleaned.append(c);
            }
        }

        String result = cleaned.toString().trim();
        
        // 如果清洗后为空，说明是纯符号
        if (result.isEmpty()) {
            return null;
        }

        // 清理无意义的连字符（开头、结尾、连续的连字符）
        result = cleanHyphens(result);
        
        // 再次检查是否为空
        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    private String cleanHyphens(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 去除开头和结尾的连字符
        String result = text.replaceAll("^-+", "").replaceAll("-+$", "");
        
        // 将连续的连字符替换为单个连字符
        result = result.replaceAll("-{2,}", "-");
        
        return result;
    }

    private boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // 1. 过滤 HTML 标签
        if (isHtmlTag(token)) {
            return false;
        }

        // 2. 必须包含至少一个中文或英文字母
        boolean hasValidChar = false;
        for (char c : token.toCharArray()) {
            if (Character.isLetter(c) || isChinese(c)) {
                hasValidChar = true;
                break;
            }
        }

        if (!hasValidChar) {
            return false;
        }

        // 3. 过滤单个字符（除非是中文）
        if (token.length() == 1 && !isChinese(token.charAt(0))) {
            return false;
        }

        return true;
    }

    private boolean isHtmlTag(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String lower = token.toLowerCase();

        // 常见的 HTML 标签名称（已清洗后的）
        String[] htmlTagNames = {
                "br", "p", "div", "span", "a", "img",
                "b", "i", "u", "strong", "em",
                "h1", "h2", "h3", "h4", "h5", "h6",
                "ul", "ol", "li",
                "table", "tr", "td", "th", "thead", "tbody", "tfoot"
        };

        for (String tagName : htmlTagNames) {
            if (lower.equals(tagName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isChinese(char c) {
        return (c >= 0x4E00 && c <= 0x9FA5) ||
                (c >= 0x3400 && c <= 0x4DBF) ||
                (c >= 0x20000 && c <= 0x2A6DF) ||
                (c >= 0x2A700 && c <= 0x2B73F) ||
                (c >= 0x2B740 && c <= 0x2B81F) ||
                (c >= 0x2B820 && c <= 0x2CEAF) ||
                (c >= 0xF900 && c <= 0xFAFF) ||
                (c >= 0x2F800 && c <= 0x2FA1F);
    }

    @Test
    void testFilterInvalidSymbols() {
        // 测试过滤无效符号
        List<String> input = Arrays.asList(
                "北京",           // 有效：中文
                "Hotel",         // 有效：英文
                "{}",            // 无效：纯符号
                "<>",            // 无效：纯符号
                "=",             // 无效：纯符号
                "[]",            // 无效：纯符号
                "())",           // 无效：纯符号
                "***",           // 无效：纯符号
                "---",           // 无效：纯符号
                "...",           // 无效：纯符号
                "///",           // 无效：纯符号
                "|||",            // 无效：纯符号
                "123"            // 无效：纯数字
        );

        List<String> result = filterValidTokens(input);

        // 验证只保留有效词汇（纯数字被过滤）
        assertEquals(2, result.size());
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("Hotel"));

        // 验证无效符号被过滤
        assertFalse(result.contains("{}"));
        assertFalse(result.contains("<>"));
        assertFalse(result.contains("="));
        assertFalse(result.contains("123"));
    }

    @Test
    void testFilterMixedTokens() {
        // 测试混合内容（符号+有效字符）
        List<String> input = Arrays.asList(
                "北京酒店",        // 有效：纯中文
                "Beijing-Hotel",  // 有效：英文+符号（包含字母）
                "Room-101",       // 有效：英文+数字+符号
                "5星级",          // 有效：数字+中文
                "**豪华**",       // 清洗后：豪华
                "<div>",          // 清洗后：div（HTML标签，过滤）
                "{name}",         // 清洗后：name
                "100%",           // 清洗后：100（纯数字，过滤）
                "@#$%",           // 无效：纯符号
                "----"            // 无效：纯符号
        );

        List<String> result = filterValidTokens(input);

        // 验证包含有效字符的词汇被保留
        assertTrue(result.contains("北京酒店"));
        assertTrue(result.contains("Beijing-Hotel"));
        assertTrue(result.contains("Room-101"));
        assertTrue(result.contains("5星级"));
        assertTrue(result.contains("豪华"));  // 清洗后
        assertTrue(result.contains("name"));   // 清洗后

        // 验证纯符号和 HTML 标签被过滤
        assertFalse(result.contains("div"));   // HTML 标签
        assertFalse(result.contains("100"));   // 纯数字
        assertFalse(result.contains("@#$%"));
        assertFalse(result.contains("----"));
    }

    @Test
    void testFilterEmptyAndNull() {
        // 测试空值和 null
        List<String> input = Arrays.asList(
                "北京",
                "",              // 空字符串
                "   ",           // 空白字符
                null,            // null
                "Hotel"
        );

        List<String> result = filterValidTokens(input);

        assertEquals(2, result.size());
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("Hotel"));
    }

    @Test
    void testFilterDuplicates() {
        // 测试去重
        List<String> input = Arrays.asList(
                "北京",
                "北京",           // 重复
                "Hotel",
                "Hotel",         // 重复
                "  北京  ",      // 去空白后重复
                "HOTEL"          // 不同（大小写敏感）
        );

        List<String> result = filterValidTokens(input);

        assertEquals(3, result.size());
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("Hotel"));
        assertTrue(result.contains("HOTEL"));
    }

    @Test
    void testFilterChineseCharacters() {
        // 测试各种中文字符
        List<String> input = Arrays.asList(
                "北京",           // 基本汉字
                "𠀀",            // 扩展B（需要两个char）
                "㐀",            // 扩展A
                "豪华酒店",       // 常用汉字
                "{}",            // 无效符号
                "中国香港"        // 基本汉字
        );

        List<String> result = filterValidTokens(input);

        // 验证中文字符被保留
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("豪华酒店"));
        assertTrue(result.contains("中国香港"));

        // 验证符号被过滤
        assertFalse(result.contains("{}"));
    }

    @Test
    void testFilterEnglishAndNumbers() {
        // 测试英文和数字
        List<String> input = Arrays.asList(
                "ABC",           // 大写英文
                "abc",           // 小写英文
                "A1B2",          // 字母数字混合
                "Room",          // 英文单词
                "5F",            // 数字+字母
                "===",           // 无效符号
                "..."            // 无效符号
        );

        List<String> result = filterValidTokens(input);

        assertEquals(5, result.size());
        assertTrue(result.contains("ABC"));
        assertTrue(result.contains("abc"));
        assertTrue(result.contains("A1B2"));
        assertTrue(result.contains("Room"));
        assertTrue(result.contains("5F"));

        assertFalse(result.contains("==="));
        assertFalse(result.contains("..."));
    }

    @Test
    void testFilterRealWorldExample() {
        // 模拟真实场景：酒店名称分词结果
        List<String> input = Arrays.asList(
                "北京",
                "希尔顿",
                "酒店",
                "Hilton",
                "Hotel",
                "(",             // 无效
                ")",             // 无效
                "星级",
                "-",             // 无效
                "豪华",
                "套房",
                "{",             // 无效
                "}",             // 无效
                "Beijing"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("希尔顿"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("Hilton"));
        assertTrue(result.contains("Hotel"));
        assertTrue(result.contains("星级"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));
        assertTrue(result.contains("Beijing"));

        // 验证无效符号被过滤
        assertFalse(result.contains("("));
        assertFalse(result.contains(")"));
        assertFalse(result.contains("-"));
        assertFalse(result.contains("{"));
        assertFalse(result.contains("}"));
    }

    @Test
    void testEmptyInput() {
        // 测试空输入
        List<String> result1 = filterValidTokens(null);
        assertEquals(0, result1.size());

        List<String> result2 = filterValidTokens(List.of());
        assertEquals(0, result2.size());
    }

    @Test
    void testCleanWrappedSymbols() {
        // 测试清洗被符号包裹的内容
        List<String> input = Arrays.asList(
                "**豪华**",       // 清洗后：豪华
                "《豪华》",       // 清洗后：豪华
                "「豪华」",       // 清洗后：豪华
                "[豪华]",        // 清洗后：豪华
                "{haohua}",      // 清洗后：haohua
                "【豪华】",       // 清洗后：豪华
                "<豪华>",        // 清洗后：豪华
                "(豪华)",        // 清洗后：豪华
                "---豪华---",    // 清洗后：豪华（去除开头和结尾的连字符）
                "***酒店***"     // 清洗后：酒店
        );

        List<String> result = filterValidTokens(input);

        // 验证所有内容都被清洗并保留
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("haohua"));
        assertTrue(result.contains("酒店"));
        
        // 由于去重，豪华只出现一次
        assertEquals(3, result.size());
    }

    @Test
    void testCleanMixedContent() {
        // 测试混合内容的清洗
        List<String> input = Arrays.asList(
                "北京-酒店",      // 保留连字符
                "Beijing Hotel", // 保留空格
                "5-星级",        // 保留连字符和数字
                "**北京**希尔顿", // 清洗后：北京希尔顿
                "《豪华》套房",   // 清洗后：豪华套房
                "[Room]-101"     // 清洗后：Room-101
        );

        List<String> result = filterValidTokens(input);

        // 验证清洗结果
        assertTrue(result.contains("北京-酒店"));
        assertTrue(result.contains("Beijing Hotel"));
        assertTrue(result.contains("5-星级"));
        assertTrue(result.contains("北京希尔顿"));
        assertTrue(result.contains("豪华套房"));
        assertTrue(result.contains("Room-101"));
    }

    @Test
    void testFilterPureSymbols() {
        // 测试过滤纯符号
        List<String> input = Arrays.asList(
                "北京",
                "{}",            // 纯符号
                "酒店",
                "===",           // 纯符号
                "豪华",
                "***",           // 纯符号
                "---",           // 纯符号
                "...",           // 纯符号
                "套房"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertEquals(4, result.size());
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));

        // 验证纯符号被过滤
        assertFalse(result.contains("{}"));
        assertFalse(result.contains("==="));
        assertFalse(result.contains("***"));
        assertFalse(result.contains("---"));
        assertFalse(result.contains("..."));
    }

    @Test
    void testFilterHtmlTagsAfterCleaning() {
        // 测试 HTML 标签在清洗后被过滤
        List<String> input = Arrays.asList(
                "北京",
                "<br>",          // 清洗后：br（HTML标签）
                "<p>",           // 清洗后：p（HTML标签）
                "</div>",        // 清洗后：div（HTML标签）
                "酒店",
                "<span>",        // 清洗后：span（HTML标签）
                "豪华"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertEquals(3, result.size());
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("豪华"));

        // 验证 HTML 标签被过滤
        assertFalse(result.contains("br"));
        assertFalse(result.contains("p"));
        assertFalse(result.contains("div"));
        assertFalse(result.contains("span"));
    }

    @Test
    void testFilterSingleCharacters() {
        // 测试过滤单个字符（除了中文）
        List<String> input = Arrays.asList(
                "北京酒店",       // 保留
                "a",             // 过滤：单个英文字符
                "1",             // 过滤：单个数字
                "北",            // 保留：单个中文
                "Hotel",         // 保留
                "b",             // 过滤：单个英文字符
                "京",            // 保留：单个中文
                "豪华"           // 保留
        );

        List<String> result = filterValidTokens(input);

        // 验证多字符词汇被保留
        assertTrue(result.contains("北京酒店"));
        assertTrue(result.contains("Hotel"));
        assertTrue(result.contains("豪华"));

        // 验证单个中文字符被保留
        assertTrue(result.contains("北"));
        assertTrue(result.contains("京"));

        // 验证单个英文字符和数字被过滤
        assertFalse(result.contains("a"));
        assertFalse(result.contains("b"));
        assertFalse(result.contains("1"));
    }

    @Test
    void testRealWorldExample() {
        // 真实场景：富文本编辑器输出
        List<String> input = Arrays.asList(
                "**北京**",       // 清洗后：北京
                "《希尔顿》",     // 清洗后：希尔顿
                "酒店",
                "<p>",           // 清洗后：p（过滤）
                "位于",
                "【市中心】",     // 清洗后：市中心
                "</p>",          // 清洗后：p（过滤）
                "<br>",          // 清洗后：br（过滤）
                "设施",
                "***齐全***",    // 清洗后：齐全
                "{sys}",     // 清洗后：sys
                "优质",
                "5",             // 过滤：单个数字
                "星级",
                "「豪华」",       // 清洗后：豪华
                "套房"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("希尔顿"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("位于"));
        assertTrue(result.contains("市中心"));
        assertTrue(result.contains("设施"));
        assertTrue(result.contains("齐全"));
        assertTrue(result.contains("service"));
        assertTrue(result.contains("优质"));
        assertTrue(result.contains("星级"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));

        // 验证 HTML 标签被过滤
        assertFalse(result.contains("p"));
        assertFalse(result.contains("br"));

        // 验证单个数字被过滤
        assertFalse(result.contains("5"));
    }

    @Test
    void testChineseTraditionalAndSimplified() {
        // 测试简体和繁体中文
        List<String> input = Arrays.asList(
                "**北京**",       // 简体
                "《臺北》",       // 繁体
                "【香港】",       // 简体
                "「澳門」",       // 繁体
                "***广州***",    // 简体
                "【深圳】"        // 简体
        );

        List<String> result = filterValidTokens(input);

        // 验证简繁体都被正确处理
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("臺北"));
        assertTrue(result.contains("香港"));
        assertTrue(result.contains("澳門"));
        assertTrue(result.contains("广州"));
        assertTrue(result.contains("深圳"));
    }

    @Test
    void testCleanHyphens() {
        // 测试连字符清理
        List<String> input = Arrays.asList(
                "北京-酒店",      // 保留：有意义的连字符
                "---豪华---",    // 清洗后：豪华（去除开头和结尾）
                "--套房--",      // 清洗后：套房
                "-希尔顿",       // 清洗后：希尔顿（去除开头）
                "万豪-",         // 清洗后：万豪（去除结尾）
                "Room---101",    // 清洗后：Room-101（连续连字符变单个）
                "A--B--C",       // 清洗后：A-B-C
                "---",           // 清洗后：null（纯连字符）
                "北京--上海"      // 清洗后：北京-上海
        );

        List<String> result = filterValidTokens(input);

        // 验证有意义的连字符被保留
        assertTrue(result.contains("北京-酒店"));
        assertTrue(result.contains("Room-101"));
        assertTrue(result.contains("A-B-C"));
        assertTrue(result.contains("北京-上海"));

        // 验证无意义的连字符被去除
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));
        assertTrue(result.contains("希尔顿"));
        assertTrue(result.contains("万豪"));

        // 验证纯连字符被过滤
        assertFalse(result.contains("---"));
    }

    @Test
    void testFilterHtmlTags() {
        // 测试过滤 HTML 标签
        List<String> input = Arrays.asList(
                "北京",
                "<br>",          // HTML 标签
                "酒店",
                "<p>",           // HTML 标签
                "</p>",          // HTML 标签
                "豪华",
                "<div>",         // HTML 标签
                "</div>",        // HTML 标签
                "套房",
                "<span>",        // HTML 标签
                "<img>",         // HTML 标签
                "<a>",           // HTML 标签
                "Hotel"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));
        assertTrue(result.contains("Hotel"));

        // 验证 HTML 标签被过滤
        assertFalse(result.contains("<br>"));
        assertFalse(result.contains("<p>"));
        assertFalse(result.contains("</p>"));
        assertFalse(result.contains("<div>"));
        assertFalse(result.contains("</div>"));
        assertFalse(result.contains("<span>"));
        assertFalse(result.contains("<img>"));
        assertFalse(result.contains("<a>"));
    }

    @Test
    void testFilterRichTextMarks() {
        // 测试过滤富文本标记
        List<String> input = Arrays.asList(
                "北京",
                "{p}",           // 被大括号包裹的单个字符
                "希尔顿",
                "{a}",           // 被大括号包裹的单个字符
                "酒店",
                "《一》",         // 被书名号包裹的单个字符
                "豪华",
                "《二》",         // 被书名号包裹的单个字符
                "套房",
                "{1}",           // 被大括号包裹的数字
                "Hotel"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("希尔顿"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));
        assertTrue(result.contains("Hotel"));

        // 验证富文本标记被过滤
        assertFalse(result.contains("{p}"));
        assertFalse(result.contains("{a}"));
        assertFalse(result.contains("《一》"));
        assertFalse(result.contains("《二》"));
        assertFalse(result.contains("{1}"));
    }

    @Test
    void testFilterWrappedShortWords() {
        // 测试过滤被符号包裹的短词（有效字符 <= 3）
        List<String> input = Arrays.asList(
                "北京酒店",       // 有效：长词
                "<br>",          // 无效：HTML 标签
                "(a)",           // 无效：被符号包裹的单个字符
                "[ab]",          // 无效：被符号包裹的短词
                "{abc}",         // 无效：被符号包裹的短词
                "《北京》",       // 无效：被书名号包裹的短词
                "希尔顿酒店",     // 有效：长词
                "<div>",         // 无效：HTML 标签
                "(Hotel)",       // 无效：被符号包裹的短词（5个字符但被括号包裹）
                "Beijing-Hotel", // 有效：长词（包含符号但有效字符多）
                "{name}",        // 无效：被符号包裹的短词
                "豪华套房"        // 有效：长词
        );

        List<String> result = filterValidTokens(input);

        // 验证长词被保留
        assertTrue(result.contains("北京酒店"));
        assertTrue(result.contains("希尔顿酒店"));
        assertTrue(result.contains("Beijing-Hotel"));
        assertTrue(result.contains("豪华套房"));

        // 验证被符号包裹的短词被过滤
        assertFalse(result.contains("<br>"));
        assertFalse(result.contains("(a)"));
        assertFalse(result.contains("[ab]"));
        assertFalse(result.contains("{abc}"));
        assertFalse(result.contains("《北京》"));
        assertFalse(result.contains("<div>"));
        assertFalse(result.contains("{name}"));
    }

    @Test
    void testFilterComplexHtmlContent() {
        // 测试复杂的 HTML 内容
        List<String> input = Arrays.asList(
                "北京",
                "<h1>",          // HTML 标签
                "希尔顿",
                "</h1>",         // HTML 标签
                "<ul>",          // HTML 标签
                "<li>",          // HTML 标签
                "酒店",
                "</li>",         // HTML 标签
                "</ul>",         // HTML 标签
                "<table>",       // HTML 标签
                "<tr>",          // HTML 标签
                "<td>",          // HTML 标签
                "豪华",
                "</td>",         // HTML 标签
                "</tr>",         // HTML 标签
                "</table>",      // HTML 标签
                "套房"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertEquals(5, result.size());
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("希尔顿"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));

        // 验证所有 HTML 标签被过滤
        assertFalse(result.contains("<h1>"));
        assertFalse(result.contains("</h1>"));
        assertFalse(result.contains("<ul>"));
        assertFalse(result.contains("<li>"));
        assertFalse(result.contains("</li>"));
        assertFalse(result.contains("</ul>"));
        assertFalse(result.contains("<table>"));
        assertFalse(result.contains("<tr>"));
        assertFalse(result.contains("<td>"));
        assertFalse(result.contains("</td>"));
        assertFalse(result.contains("</tr>"));
        assertFalse(result.contains("</table>"));
    }

    @Test
    void testRealWorldRichTextExample() {
        // 模拟真实的富文本编辑器输出
        List<String> input = Arrays.asList(
                "北京",
                "希尔顿",
                "酒店",
                "<p>",           // HTML 段落标签（清洗后：p，过滤）
                "位于",
                "市中心",
                "</p>",          // HTML 段落标签（清洗后：p，过滤）
                "<br>",          // HTML 换行标签（清洗后：br，过滤）
                "<div>",         // HTML div 标签（清洗后：div，过滤）
                "设施",
                "齐全",
                "</div>",        // HTML div 标签（清洗后：div，过滤）
                "{p}",           // 富文本变量（清洗后：p，过滤）
                "服务",
                "优质",
                "《一》",         // 书名号包裹的短词（清洗后：一）
                "星级",
                "<span>",        // HTML span 标签（清洗后：span，过滤）
                "豪华",
                "</span>",       // HTML span 标签（清洗后：span，过滤）
                "套房"
        );

        List<String> result = filterValidTokens(input);

        // 验证有效词汇被保留
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("希尔顿"));
        assertTrue(result.contains("酒店"));
        assertTrue(result.contains("位于"));
        assertTrue(result.contains("市中心"));
        assertTrue(result.contains("设施"));
        assertTrue(result.contains("齐全"));
        assertTrue(result.contains("服务"));
        assertTrue(result.contains("优质"));
        assertTrue(result.contains("一"));    // 清洗后的单个中文
        assertTrue(result.contains("星级"));
        assertTrue(result.contains("豪华"));
        assertTrue(result.contains("套房"));

        // 验证 HTML 标签被过滤
        assertFalse(result.contains("p"));
        assertFalse(result.contains("br"));
        assertFalse(result.contains("div"));
        assertFalse(result.contains("span"));
    }
}
