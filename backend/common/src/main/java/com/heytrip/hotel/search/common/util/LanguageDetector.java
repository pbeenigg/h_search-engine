package com.heytrip.hotel.search.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.regex.Pattern;

/**
 * 语言检测工具类
 * 用于识别文本是中文还是英文，并智能分配到对应的字段
 */
public class LanguageDetector {

    /**
     * 中文字符正则表达式（包括中日韩统一表意文字）
     * Unicode 范围：
     * - \u4E00-\u9FFF: CJK 统一表意文字
     * - \u3400-\u4DBF: CJK 扩展 A
     * - \uF900-\uFAFF: CJK 兼容表意文字
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]+");

    /**
     * 英文字符正则表达式（字母、数字、常见标点）
     */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * 双语字段结果
     */
    @Data
    @AllArgsConstructor
    public static class BilingualField {
        /**
         * 中文内容
         */
        private String chinese;
        
        /**
         * 英文内容
         */
        private String english;

        public BilingualField() {
            this.chinese = null;
            this.english = null;
        }
    }

    /**
     * 判断文本是否包含中文字符
     *
     * @param text 待检测文本
     * @return true 如果包含中文字符
     */
    public static boolean containsChinese(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CHINESE_PATTERN.matcher(text).find();
    }

    /**
     * 判断文本是否包含英文字符
     *
     * @param text 待检测文本
     * @return true 如果包含英文字符
     */
    public static boolean containsEnglish(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return ENGLISH_PATTERN.matcher(text).find();
    }

    /**
     * 判断文本主要语言类型
     *
     * @param text 待检测文本
     * @return "zh" 中文为主, "en" 英文为主, "mixed" 中英混合, "unknown" 无法识别
     */
    public static String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }

        boolean hasChinese = containsChinese(text);
        boolean hasEnglish = containsEnglish(text);

        if (hasChinese && hasEnglish) {
            // 统计中英文字符数量，判断主要语言
            int chineseCount = countChineseChars(text);
            int englishCount = countEnglishChars(text);
            
            if (chineseCount > englishCount) {
                return "zh";
            } else if (englishCount > chineseCount) {
                return "en";
            } else {
                return "mixed";
            }
        } else if (hasChinese) {
            return "zh";
        } else if (hasEnglish) {
            return "en";
        } else {
            return "unknown";
        }
    }

    /**
     * 统计中文字符数量
     */
    private static int countChineseChars(String text) {
        if (text == null) return 0;
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计英文字符数量
     */
    private static int countEnglishChars(String text) {
        if (text == null) return 0;
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isEnglish(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断字符是否为中文
     */
    private static boolean isChinese(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) ||
               (c >= 0x3400 && c <= 0x4DBF) ||
               (c >= 0xF900 && c <= 0xFAFF);
    }

    /**
     * 判断字符是否为英文字母
     */
    private static boolean isEnglish(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 智能分配双语字段
     * 根据文本内容自动识别语言，并分配到对应的中文或英文字段
     *
     * @param text 待分配的文本
     * @return 双语字段对象，包含中文和英文内容
     */
    public static BilingualField smartAssign(String text) {
        BilingualField result = new BilingualField();
        
        if (text == null || text.isBlank()) {
            return result;
        }

        String language = detectLanguage(text);
        
        switch (language) {
            case "zh":
                // 主要是中文，分配到中文字段
                result.setChinese(text);
                break;
            case "en":
                // 主要是英文，分配到英文字段
                result.setEnglish(text);
                break;
            case "mixed":
                // 中英混合，尝试分离或同时分配
                // 如果中文字符较多，优先分配到中文字段
                int chineseCount = countChineseChars(text);
                int englishCount = countEnglishChars(text);
                if (chineseCount > englishCount) {
                    result.setChinese(text);
                } else {
                    result.setEnglish(text);
                }
                break;
            default:
                // 无法识别，默认分配到英文字段
                result.setEnglish(text);
                break;
        }
        
        return result;
    }

    /**
     * 智能合并双语字段
     * 优先使用已有的中英文字段，如果某个字段为空，则尝试从混合字段中提取
     *
     * @param existingCn 已有的中文字段
     * @param existingEn 已有的英文字段
     * @param mixedText  混合文本（可能包含中英文）
     * @return 双语字段对象
     */
    public static BilingualField smartMerge(String existingCn, String existingEn, String mixedText) {
        BilingualField result = new BilingualField();
        
        // 优先使用已有的中英文字段
        if (existingCn != null && !existingCn.isBlank()) {
            result.setChinese(existingCn);
        }
        
        if (existingEn != null && !existingEn.isBlank()) {
            result.setEnglish(existingEn);
        }
        
        // 如果某个字段为空，尝试从混合文本中提取
        if (mixedText != null && !mixedText.isBlank()) {
            BilingualField extracted = smartAssign(mixedText);
            
            if (result.getChinese() == null && extracted.getChinese() != null) {
                result.setChinese(extracted.getChinese());
            }
            
            if (result.getEnglish() == null && extracted.getEnglish() != null) {
                result.setEnglish(extracted.getEnglish());
            }
        }
        
        return result;
    }

    /**
     * 批量智能分配多个字段
     * 当有多个可能的来源时，按优先级尝试分配
     *
     * @param texts 按优先级排序的文本数组
     * @return 双语字段对象
     */
    public static BilingualField smartAssignMultiple(String... texts) {
        BilingualField result = new BilingualField();
        
        if (texts == null || texts.length == 0) {
            return result;
        }
        
        // 按优先级尝试每个文本
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            
            BilingualField assigned = smartAssign(text);
            
            // 如果中文字段还未赋值，且当前文本有中文内容
            if (result.getChinese() == null && assigned.getChinese() != null) {
                result.setChinese(assigned.getChinese());
            }
            
            // 如果英文字段还未赋值，且当前文本有英文内容
            if (result.getEnglish() == null && assigned.getEnglish() != null) {
                result.setEnglish(assigned.getEnglish());
            }
            
            // 如果两个字段都已赋值，提前结束
            if (result.getChinese() != null && result.getEnglish() != null) {
                break;
            }
        }
        
        return result;
    }
}
