package com.heytrip.hotel.search.infra.nlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * - 提供索引时与查询时的轻量能力
 * - 内部对 HanLP 不可用的情况做降级（避免影响主流程）
 */
@Slf4j
@Component
public class NlpEnrichmentService {


    @Resource
    private SpellCheckService spellCheckService;

    /**
     * 精细分词（用于索引侧生成 tokens）
     *
     * @param text 输入文本
     * @return 精细分词结果
     */
    public List<String> tokenizeFine(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();

        try {
            // 首先进行拼写错误纠正（真正集成到搜索流程中）
            String corrected = correctSpelling(t);

            // 酒店领域优化的精细分词
            return segmentHotelDomain(corrected);
        } catch (Throwable ex) {
            log.debug("[NLP] tokenizeFine degrade err={}", ex.getMessage());
            return simpleSplit(t);
        }
    }

    /**
     * 酒店领域优化的精细分词策略
     * 针对酒店名称、地址、品牌等文本进行专门优化
     */
    private List<String> segmentHotelDomain(String text) {
        // 1. 先使用支持自定词典的segment
        List<Term> terms = HanLP.segment(text);

        // 2. 针对酒店领域进行后处理优化
        List<String> optimizedTokens = new ArrayList<>();
        for (Term term : terms) {
            String word = term.word.trim();
            if (word.isEmpty()) continue;

            // 优化数字和单位组合
            if (isNumberWithUnit(word)) {
                optimizedTokens.add(word);
            }
            // 优化品牌词汇
            else if (isBrandWord(word)) {
                optimizedTokens.add(word);
            }
            // 优化地理位置词汇
            else if (isLocationWord(word)) {
                optimizedTokens.add(word);
            }
            // 保持原有分词结果
            else {
                optimizedTokens.add(word);
            }
        }

        return optimizedTokens;
    }

    /**
     * 酒店领域NLP分词（增强版）
     * 结合多种分词策略，提供最佳分词效果
     */
    public List<String> hotelDomainTokens(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return Collections.emptyList();

        try {
            // 纠错处理
            String corrected = correctSpelling(normalized);

            // 多策略分词融合
            List<String> tokens = new ArrayList<>();

            // 策略1：标准分词
            List<Term> standardTerms = StandardTokenizer.segment(corrected);
            for (Term term : standardTerms) {
                // 只保留有意义的词性
                if (isValidNlpToken(term)) {
                    tokens.add(term.word);
                }
            }

            // 策略2：NLP分词（带词性）
            List<Term> nlpTerms = NLPTokenizer.segment(corrected);
            for (Term term : nlpTerms) {
                // 只保留有意义的词性
                if (isValidNlpToken(term)) {
                    tokens.add(term.word);
                }
            }

            // 去重并保持顺序
            return tokens.stream().distinct().collect(Collectors.toList());

        } catch (Throwable ex) {
            log.debug("[NLP] hotelDomainTokens degrade err={}", ex.getMessage());
            return simpleSplit(normalized);
        }
    }

    /**
     * 判断是否为数字+单位的组合（如5星级、3公里）
     */
    private boolean isNumberWithUnit(String word) {
        return word.matches(".*\\d+.*(星|级|星|公里|米|km|m|分钟|小时).*");
    }

    /**
     * 判断是否为品牌词汇
     */
    private boolean isBrandWord(String word) {
        // 常见酒店品牌关键词
        return word.contains("酒店") ||
                word.contains("宾馆") ||
                word.contains("饭店") ||
                word.contains("度假村") ||
                word.contains("希尔顿") ||
                word.contains("万豪") ||
                word.contains("洲际") ||
                word.contains("凯悦") ||
                word.contains("香格里拉") ||
                word.contains("四季") ||
                word.contains("喜来登") ||
                word.contains("威斯汀") ||
                word.contains("瑞吉") ||
                word.contains("假日") ||
                word.contains("名宿") ||
                word.contains("快捷") ||
                word.contains("连锁") ||
                word.contains("国际") ||
                word.contains("大酒店") ||
                word.contains("旅馆") ||
                word.contains("客栈") ||
                word.length() <= 6; // 短词汇可能是品牌名
    }

    /**
     * 判断是否为地理位置词汇
     */
    private boolean isLocationWord(String word) {
        // 地理位置关键词
        return word.contains("市") ||
                word.contains("区") ||
                word.contains("县") ||
                word.contains("省") ||
                word.contains("路") ||
                word.contains("街") ||
                word.contains("大道") ||
                word.contains("广场") ||
                word.contains("中心") ||
                word.contains("站") ||
                word.contains("机场") ||
                word.contains("火车站") ||
                word.contains("汽车站") ||
                word.contains("码头") ||
                word.contains("公园") ||
                word.contains("景区") ||
                word.contains("景点") ||
                word.contains("商圈") ||
                word.contains("购物中心") ||
                word.contains("步行街") ||
                word.contains("夜市") ||
                word.contains("地标") ||
                word.contains("地铁");
    }

    /**
     * 判断NLP分词结果是否有意义
     */
    private boolean isValidNlpToken(Term term) {
        // 保留有意义的词性
        String nature = term.nature.toString();
        return !nature.equals("w") &&     // 标点符号
                !nature.equals("ws") &&    // 网址
                !nature.equals("wt") &&    // 时间
                !nature.equals("nz");      // 其他名词
    }

    /**
     * 关键词抽取（TopK，索引/查询两侧均可用）
     */
    public List<String> extractKeywords(String text, int topK) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        int k = Math.max(1, Math.min(topK, 10));
        try {
            // 集成拼写错误纠正
            String corrected = correctSpelling(t);
            return HanLP.extractKeyword(corrected, k);
        } catch (Throwable ex) {
            log.debug("[NLP] extractKeywords degrade err={}", ex.getMessage());
            List<String> toks = simpleSplit(t);
            return toks.stream().distinct().limit(k).collect(Collectors.toList());
        }
    }

    /**
     * 标准分词（StandardTokenizer）——保留词序列
     */
    public List<String> standardTokens(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {
            List<Term> list = StandardTokenizer.segment(t);
            return list.stream().map(term -> term.word).collect(Collectors.toList());
        } catch (Throwable ex) {
            log.debug("[NLP] standardTokens degrade err={}", ex.getMessage());
            return simpleSplit(t);
        }
    }

    /**
     * NLP分词（NLPTokenizer）——带词性/NER，返回词序列
     */
    public List<String> nlpTokens(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {
            return NLPTokenizer.segment(t).stream().map(term -> term.word).collect(Collectors.toList());
        } catch (Throwable ex) {
            log.debug("[NLP] nlpTokens degrade err={}", ex.getMessage());
            return simpleSplit(t);
        }
    }

    /**
     * 索引分词（IndexTokenizer）：长词全切分，返回带偏移的结果
     */
    public List<OffsetToken> indexTokens(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {
            List<Term> list = IndexTokenizer.segment(t);
            List<OffsetToken> out = new ArrayList<>(list.size());
            for (Term term : list) {
                int begin = term.offset;
                int end = term.offset + term.word.length();
                out.add(new OffsetToken(term.word, begin, end));
            }
            return out;
        } catch (Throwable ex) {
            log.debug("[NLP] indexTokens degrade err={}", ex.getMessage());
            // 退化：无偏移
            return simpleSplit(t).stream().map(w -> new OffsetToken(w, -1, -1)).collect(Collectors.toList());
        }
    }

    /**
     * 地点实体识别（占位：根据常见后缀/字典可扩展）
     */
    public List<String> nerPlaces(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {
            // 集成拼写错误纠正
            String corrected = correctSpelling(t);

            // 使用 HanLP 的地名识别
            Segment seg = HanLP.newSegment().enablePlaceRecognize(true);
            return seg.seg(corrected).stream()
                    .filter(term -> term.nature == Nature.ns)
                    .map(term -> term.word)
                    .distinct()
                    .limit(20)
                    .collect(Collectors.toList());
        } catch (Throwable ex) {
            return Collections.emptyList();
        }
    }



    /**
     * 人名识别（中国人名），返回分出的可能人名
     */
    public List<String> nerPersons(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {

            Segment seg = HanLP.newSegment().enableNameRecognize(true);
            return seg.seg(text).stream()
                    .filter(term -> term.nature == Nature.nr)
                    .map(term -> term.word)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Throwable ex) {
            log.debug("[NLP] nerPersons degrade err={}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 机构名识别
     */
    public List<String> nerOrgs(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {

            Segment seg = HanLP.newSegment().enableOrganizationRecognize(true);
            return seg.seg(text).stream()
                    .filter(term -> term.nature == Nature.nt)
                    .map(term -> term.word)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Throwable ex) {
            log.debug("[NLP] nerOrgs degrade err={}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 简体/繁体转换
     */
    public String toSimplified(String text) {
        try {
            return HanLP.convertToSimplifiedChinese(text);
        } catch (Throwable ex) {
            return text;
        }
    }

    public String toTraditional(String text) {
        try {
            return HanLP.convertToTraditionalChinese(text);
        } catch (Throwable ex) {
            return text;
        }
    }

    /**
     * 拼音转换（全拼与首字母）
     */
    public List<String> toPinyin(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {
            List<Pinyin> pys = HanLP.convertToPinyinList(t);
            return pys.stream().map(Pinyin::getPinyinWithoutTone).collect(Collectors.toList());
        } catch (Throwable ex) {
            return Collections.emptyList();
        }
    }

    public String toPinyinHead(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return "";
        try {
            List<Pinyin> pys = HanLP.convertToPinyinList(t);
            StringBuilder sb = new StringBuilder();
            for (Pinyin p : pys) sb.append(p.getHead());
            return sb.toString();
        } catch (Throwable ex) {
            return "";
        }
    }


    /**
     * 品牌名称（基于自定义品牌词典）
     *
     * @param text 输入文本
     * @return 品牌列表
     */
    public List<String> nerBrands(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return Collections.emptyList();
        try {
            // 分词并过滤出品牌词性（nb）
            List<Term> terms = HanLP.segment(t);
            return terms.stream()
                    .filter(term -> "nb".equals(term.nature.toString()))
                    .map(term -> term.word)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Throwable ex) {
            log.debug("[NLP] extractBrands degrade err={}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 用户自定义词典：动态增删
     */
    public boolean addCustomWord(String word, String natureAndFreq) {
        try {
            // 使用 add(word, attr) 更符合 HanLP 1.x API，返回 boolean
            return CustomDictionary.add(word, natureAndFreq);
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * 移除自定义词
     */
    public boolean removeCustomWord(String word) {
        try {
            CustomDictionary.remove(word);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * 规范化（去空白/全半角/大小写等，可按需扩展简繁转换）
     */
    public String normalize(String text) {
        if (text == null) return "";
        String t = text.trim();
        if (t.isEmpty()) return "";
        return t.replace('\u00A0', ' ').replaceAll("\\s+", " ");
    }

    /**
     * 偏移标注的分词结果
     */
    @Data
    public static class OffsetToken {
        private final String token;
        private final int begin;
        private final int end;
    }

    private List<String> simpleSplit(String t) {
        List<String> out = new ArrayList<>();
        for (String p : t.split("[^\u4e00-\u9fa5A-Za-z0-9]+")) {
            String s = p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }


    /**
     * 智能拼写错误纠正（可配置系统）
     * 使用可配置的拼写错误纠正系统，支持热更新、权重配置、业务域过滤
     */
    private String correctSpelling(String text) {
        if (text == null || text.isEmpty()) return text;

        // 使用可配置的拼写错误纠正系统
        String corrected = spellCheckService.getCorrection(text, "ALL"); // 使用通用业务域
        return corrected;
    }

    /**
     * 智能词性标注和过滤
     * 根据酒店领域的特点，过滤无意义词汇并优化有意义的词汇
     */
    public List<String> smartTokenFilter(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return Collections.emptyList();

        try {
            // 使用NLP分词获得词性信息
            List<Term> nlpTerms = NLPTokenizer.segment(normalized);
            List<String> filteredTokens = new ArrayList<>();

            for (Term term : nlpTerms) {
                String word = term.word.trim();
                if (word.isEmpty()) continue;

                String nature = term.nature.toString();

                // 保留有价值的词性
                if (isValuableToken(word, nature)) {
                    filteredTokens.add(word);
                }
            }

            return filteredTokens;

        } catch (Throwable ex) {
            log.debug("[NLP] smartTokenFilter degrade err={}", ex.getMessage());
            return simpleSplit(normalized);
        }
    }

    /**
     * 判断是否为有价值的词汇
     */
    private boolean isValuableToken(String word, String nature) {
        // 保留名词类
        if (nature.startsWith("n")) {
            return !word.matches(".*[0-9]+.*") && word.length() > 1;
        }

        // 保留动词（但过滤一些常见的无意义动词）
        if (nature.startsWith("v")) {
            return !word.equals("是") && !word.equals("有") && !word.equals("在");
        }

        // 保留形容词
        if (nature.startsWith("a")) {
            return true;
        }

        // 保留地理相关词汇
        if (nature.equals("ns") || nature.equals("nt") || nature.equals("nz")) {
            return true;
        }

        return false;
    }

    /**
     * 酒店领域专用实体提取
     * 自动识别和提取酒店相关的关键实体
     */
    public HotelEntity extractHotelEntities(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return new HotelEntity();

        try {
            // 使用NLP分词获得词性信息
            List<Term> terms = NLPTokenizer.segment(normalized);

            HotelEntity entity = new HotelEntity();

            for (Term term : terms) {
                String word = term.word.trim();
                String nature = term.nature.toString();

                // 提取地名实体
                if (nature.equals("ns")) {
                    entity.getPlaceNames().add(word);
                }
                // 提取品牌/机构名
                else if (nature.equals("nt") || nature.equals("nz")) {
                    entity.getOrganizationNames().add(word);
                }
                // 提取人名
                else if (nature.equals("nr")) {
                    entity.getPersonNames().add(word);
                }
            }

            return entity;

        } catch (Throwable ex) {
            log.debug("[NLP] extractHotelEntities degrade err={}", ex.getMessage());
            return new HotelEntity();
        }
    }

    /**
     * 酒店实体类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HotelEntity {
        private List<String> placeNames = new ArrayList<>();
        private List<String> organizationNames = new ArrayList<>();
        private List<String> personNames = new ArrayList<>();
        private List<String> facilityKeywords = new ArrayList<>();
        private List<String> brandKeywords = new ArrayList<>();

        public List<String> getAllEntities() {
            List<String> all = new ArrayList<>();
            all.addAll(placeNames);
            all.addAll(organizationNames);
            all.addAll(personNames);
            all.addAll(facilityKeywords);
            all.addAll(brandKeywords);
            return all;
        }
    }

}
    


