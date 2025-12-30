package com.heytrip.hotel.search.common.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HanLP 工具类
 */
public class HanlpUtils {


    /**
     * 过滤分词结果中的无效符号
     * 1. 清洗被符号包裹的内容，提取纯净的中文/英文（如 **豪华** -> 豪华）
     * 2. 过滤纯符号和 HTML 标签
     * 3. 过滤单个字符,单个字母,单个中文
     * 4. 去除重复项（大小写不敏感，如 "Hotel" 和 "hotel" 视为重复）
     *
     * @param tokens 原始分词列表
     * @return 过滤后的有效分词列表
     */
    public static List<String> filterValidTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        // 使用 Map 进行大小写不敏感的去重，key 为小写形式，value 为原始值
        Map<String, String> uniqueTokens = new LinkedHashMap<>();
        tokens.stream()
                .filter(token -> token != null && !token.isBlank())
                .map(String::trim)
                .map(HanlpUtils::cleanToken)  // 清洗符号
                .filter(token -> token != null && !token.isEmpty())
                .filter(HanlpUtils::isValidToken)  // 验证有效性
                .forEach(token -> {
                    String lowerKey = token.toLowerCase();
                    // 如果已存在，保留第一个出现的原始大小写形式
                    uniqueTokens.putIfAbsent(lowerKey, token);
                });

        return new ArrayList<>(uniqueTokens.values());
    }

    /**
     * 清洗分词，去除包裹的符号，保留纯净的中文/英文/数字
     * 例如：**豪华** -> 豪华，《豪华》 -> 豪华，{haohua} -> haohua
     *
     * @param token 原始分词
     * @return 清洗后的分词，如果是纯符号则返回 null
     */
    public static String cleanToken(String token) {
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

    /**
     * 清理无意义的连字符
     * - 去除开头和结尾的连字符
     * - 将连续的连字符替换为单个连字符
     *
     * @param text 文本
     * @return 清理后的文本
     */
    public static String cleanHyphens(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 去除开头和结尾的连字符
        String result = text.replaceAll("^-+", "").replaceAll("-+$", "");

        // 将连续的连字符替换为单个连字符
        result = result.replaceAll("-{2,}", "-");

        return result;
    }

    /**
     * 判断是否为有效的分词
     * 有效分词必须满足以下条件：
     * 1. 包含至少一个中文或英文字母
     * 2. 不是 HTML 标签或纯数字单字符
     *
     * @param token 分词（已清洗）
     * @return true 表示有效，false 表示无效
     */
    public static boolean isValidToken(String token) {
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

        // 3. 过滤单个字符（包括单个字母、单个中文、单个数字等）
        if (token.length() == 1) {
            return false;
        }

        return true;
    }

    /**
     * 判断是否为 HTML 标签
     */
    public static boolean isHtmlTag(String token) {
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
                "table", "tr", "td", "th", "thead", "tbody", "tfoot",
                "form", "input", "button", "select", "option",
                "header", "footer", "nav", "section", "article",
                "script", "style", "link", "meta"
        };

        for (String tagName : htmlTagNames) {
            if (lower.equals(tagName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断字符是否为中文字符
     *
     * @param c 字符
     * @return true 表示是中文，false 表示不是
     */
    public static boolean isChinese(char c) {
        // Unicode 中文字符范围
        return (c >= 0x4E00 && c <= 0x9FA5) ||  // 基本汉字
                (c >= 0x3400 && c <= 0x4DBF) ||  // 扩展A
                (c >= 0x20000 && c <= 0x2A6DF) || // 扩展B
                (c >= 0x2A700 && c <= 0x2B73F) || // 扩展C
                (c >= 0x2B740 && c <= 0x2B81F) || // 扩展D
                (c >= 0x2B820 && c <= 0x2CEAF) || // 扩展E
                (c >= 0xF900 && c <= 0xFAFF) ||   // 兼容汉字
                (c >= 0x2F800 && c <= 0x2FA1F);   // 兼容扩展
    }

    /**
     * 验证坐标有效性
     *
     * @param lat 纬度
     * @param lon 经度
     * @return true 表示有效，false 表示无效
     */
    public static boolean isValidCoordinate(double lat, double lon) {
        // 纬度范围：-90 到 90
        // 经度范围：-180 到 180
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }


    public static void main(String[] args) throws IOException {
        System.out.println(HanLP.segment("你好，欢迎使用HanLP汉语处理包！"));



        CRFLexicalAnalyzer analyzer1 = new CRFLexicalAnalyzer();
        String[] tests = new String[]{
                "商品和服务",
                "上海华安工业（集团）公司董事长谭旭光和秘书胡花蕊来到美国纽约现代艺术博物馆参观",
                "微软公司於1975年由比爾·蓋茲和保羅·艾倫創立，18年啟動以智慧雲端、前端為導向的大改組。" // 支持繁体中文
        };
        for (String sentence : tests)
        {
            System.out.println(analyzer1.analyze(sentence));
        }
        System.out.println(NLPTokenizer.segment("我新造一个词叫攻城狮，你能识别并标注正确词性吗？"));

        List<Term> termList1 = StandardTokenizer.segment("商品和服务");
        System.out.println(termList1);

        List<Term> termList2 = IndexTokenizer.segment("主副食品");
        for (Term term : termList2)
        {
            System.out.println(term + " [" + term.offset + ":" + (term.offset + term.word.length()) + "]");
        }


        Segment nShortSegment = new NShortSegment().enableCustomDictionary(false).enablePlaceRecognize(true).enableOrganizationRecognize(true);
        Segment shortestSegment = new DijkstraSegment().enableCustomDictionary(false).enablePlaceRecognize(true).enableOrganizationRecognize(true);
        String[] testCase1 = new String[]{
                "今天，刘志军案的关键人物,山西女商人丁书苗在市二中院出庭受审。",
                "刘喜杰石国祥会见吴亚琴先进事迹报告团成员",
        };
        for (String sentence : testCase1)
        {
            System.out.println("N-最短分词：" + nShortSegment.seg(sentence) + "\n最短路分词：" + shortestSegment.seg(sentence));
        }

        CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
        String[] tests11 = new String[]{
                "商品和服务",
                "上海华安工业（集团）公司董事长谭旭光和秘书胡花蕊来到美国纽约现代艺术博物馆参观",
                "微软公司於1975年由比爾·蓋茲和保羅·艾倫創立，18年啟動以智慧雲端、前端為導向的大改組。" // 支持繁体中文
        };
        for (String sentence : tests11)
        {
            System.out.println(analyzer.analyze(sentence));
        }

        String[] testCase2 = new String[]{
                "我在上海林原科技有限公司兼职工作，",
                "我经常在台川喜宴餐厅吃饭，",
                "偶尔去地中海影城看电影。",
        };
        Segment segment = HanLP.newSegment().enableOrganizationRecognize(true);
        for (String sentence : testCase2)
        {
            List<Term> termList = segment.seg(sentence);
            System.out.println(termList);
        }


    }



}
