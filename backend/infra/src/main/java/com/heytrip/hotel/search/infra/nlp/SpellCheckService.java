package com.heytrip.hotel.search.infra.nlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 拼写检查配置管理器
 * 支持热更新、权重配置和业务领域过滤
 */
@Slf4j
@Component
public class SpellCheckService {
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, List<SpellCheckRule>> rulesCache = new ConcurrentHashMap<>();
    private final Map<String, String> directCorrectMap = new ConcurrentHashMap<>();
    
    @Value("${hotel.search.spellcheck.config.file:spellcheck.properties}")
    private String configFilePath;
    
    /**
     * 拼写检查规则
     */
    @Data
    public static class SpellCheckRule {
        private List<String> wrongSpellings;
        private List<String> correctSpellings;
        private int weight; // 1-10, higher weight = higher priority
        private String businessDomain; // CN, INTL, HMT, ALL
        private String description;
        private long lastUsedTime;
        private int usageCount;
        
        public boolean isValid() {
            return wrongSpellings != null && !wrongSpellings.isEmpty() &&
                   correctSpellings != null && !correctSpellings.isEmpty() &&
                   weight > 0 && weight <= 10;
        }
    }
    
    /**
     * 初始化配置
     */
    @PostConstruct
    public void init() {
        loadConfig();
        log.info("[SpellCheckService] Spell check configuration initialized, rules: {}", directCorrectMap.size());
    }
    
    /**
     * Load configuration file
     */
    public void loadConfig() {
        lock.writeLock().lock();
        try {
            Properties props = new Properties();
            
            // Try to load from classpath
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFilePath)) {
                if (input != null) {
                    props.load(input);
                } else {
                    log.warn("[SpellCheckService] Configuration file not found: {}", configFilePath);
                    // Use default configuration
                    props = getDefaultProperties();
                }
            }
            
            // Parse configuration
            Map<String, List<SpellCheckRule>> newRulesCache = new HashMap<>();
            Map<String, String> newDirectMap = new HashMap<>();
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value == null || value.trim().isEmpty()) continue;
                
                try {
                    SpellCheckRule rule = parseRule(key, value);
                    if (rule != null && rule.isValid()) {
                        
                        // Group by business domain
                        String domain = rule.getBusinessDomain();
                        newRulesCache.computeIfAbsent(domain, k -> new ArrayList<>()).add(rule);
                        
                        // Build direct mapping
                        for (String wrong : rule.getWrongSpellings()) {
                            String normalizedWrong = normalizeText(wrong);
                            newDirectMap.put(normalizedWrong, rule.getCorrectSpellings().get(0)); // Take first correct option
                        }
                    }
                } catch (Exception e) {
                    log.error("[SpellCheckService] Failed to parse rule: {} = {}", key, value, e);
                }
            }
            
            // Update cache
            rulesCache.clear();
            rulesCache.putAll(newRulesCache);
            directCorrectMap.clear();
            directCorrectMap.putAll(newDirectMap);
            
            log.info("[SpellCheckService] Configuration loaded, total rules: {}, direct mappings: {}",
                    getAllRules().size(), directCorrectMap.size());
            
        } catch (Exception e) {
            log.error("[SpellCheckService] Configuration loading failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Parse rule
     */
    private SpellCheckRule parseRule(String key, String value) {
        String[] parts = value.split("\\|");
        if (parts.length < 2) return null;
        
        // Parse wrong and correct spellings
        String wrongPart = parts[0].trim();
        String correctPart = parts[1].trim();
        
        List<String> wrongSpellings = Arrays.stream(wrongPart.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeText)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        List<String> correctSpellings = Arrays.stream(correctPart.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeText)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (wrongSpellings.isEmpty() || correctSpellings.isEmpty()) return null;
        
        SpellCheckRule rule = new SpellCheckRule();
        rule.setWrongSpellings(wrongSpellings);
        rule.setCorrectSpellings(correctSpellings);
        
        // Parse weight
        if (parts.length > 2) {
            try {
                rule.setWeight(Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                rule.setWeight(5); // Default weight
            }
        } else {
            rule.setWeight(5);
        }
        
        // Parse business domain
        if (parts.length > 3) {
            rule.setBusinessDomain(parts[3].trim());
        } else {
            rule.setBusinessDomain("ALL");
        }
        
        // Parse description
        if (parts.length > 4) {
            rule.setDescription(parts[4].trim());
        } else {
            rule.setDescription("");
        }
        
        return rule;
    }
    
    /**
     * Get correction result
     */
    public String getCorrection(String text, String businessDomain) {
        String normalizedText = normalizeText(text);
        
        // 1. Direct lookup
        String directResult = directCorrectMap.get(normalizedText);
        if (directResult != null) {
            recordUsage(normalizedText);
            return directResult;
        }
        
        // 2. Fuzzy matching (edit distance)
        String fuzzyResult = findFuzzyMatch(normalizedText, businessDomain);
        if (fuzzyResult != null) {
            recordUsage(normalizedText);
            return fuzzyResult;
        }
        
        return text; // Return original text if no correction found
    }
    
    /**
     * Fuzzy matching (edit distance)
     */
    private String findFuzzyMatch(String text, String businessDomain) {
        List<SpellCheckRule> rules = new ArrayList<>();
        
        // Get rules for relevant business domains
        rules.addAll(getRules("ALL"));
        if (!"ALL".equals(businessDomain)) {
            rules.addAll(getRules(businessDomain));
        }
        
        // Sort by weight
        rules.sort((r1, r2) -> Integer.compare(r2.getWeight(), r1.getWeight()));
        
        for (SpellCheckRule rule : rules) {
            for (String wrong : rule.getWrongSpellings()) {
                if (isSimilar(text, wrong)) {
                    return rule.getCorrectSpellings().get(0);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if two strings are similar (edit distance algorithm)
     */
    private boolean isSimilar(String text, String target) {
        int maxDistance = Math.min(2, Math.max(1, target.length() / 3));
        return editDistance(text, target) <= maxDistance;
    }
    
    /**
     * Calculate edit distance
     */
    private int editDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * Normalize text (fixed version)
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        
        // First remove spaces and tabs
        text = text.trim().toLowerCase().replaceAll("\\s+", "");
        
        // Handle different punctuation symbols separately
        text = text.replaceAll("[，。！？；：]", "");
        text = text.replaceAll("[\"\"\"\"'']", "");
        text = text.replaceAll("[（）【】]", "");
        
        return text;
    }
    
    /**
     * Get all rules
     */
    public List<SpellCheckRule> getAllRules() {
        lock.readLock().lock();
        try {
            List<SpellCheckRule> allRules = new ArrayList<>();
            for (List<SpellCheckRule> rules : rulesCache.values()) {
                allRules.addAll(rules);
            }
            return allRules;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get rules for specified business domain
     */
    public List<SpellCheckRule> getRules(String businessDomain) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rulesCache.getOrDefault(businessDomain, Collections.emptyList()));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Record usage
     */
    private void recordUsage(String normalizedText) {
        for (List<SpellCheckRule> rules : rulesCache.values()) {
            for (SpellCheckRule rule : rules) {
                if (rule.getWrongSpellings().contains(normalizedText)) {
                    rule.setLastUsedTime(System.currentTimeMillis());
                    rule.setUsageCount(rule.getUsageCount() + 1);
                    break;
                }
            }
        }
    }
    
    /**
     * Add new rule
     */
    public void addRule(SpellCheckRule rule) {
        if (rule == null || !rule.isValid()) return;
        
        lock.writeLock().lock();
        try {
            // Add to rules cache
            rulesCache.computeIfAbsent(rule.getBusinessDomain(), k -> new ArrayList<>()).add(rule);
            
            // Add to direct mapping
            for (String wrong : rule.getWrongSpellings()) {
                String normalizedWrong = normalizeText(wrong);
                directCorrectMap.put(normalizedWrong, rule.getCorrectSpellings().get(0));
            }
            
            log.info("[SpellCheckService] Rule added successfully: {} -> {}",
                    rule.getWrongSpellings(), rule.getCorrectSpellings());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove rule
     */
    public void removeRule(String ruleId) {
        lock.writeLock().lock();
        try {
            for (List<SpellCheckRule> rules : rulesCache.values()) {
                rules.removeIf(rule -> rule.getWrongSpellings().toString().equals(ruleId));
            }
            
            // Clean direct mapping
            directCorrectMap.entrySet().removeIf(entry -> entry.getKey().equals(ruleId));
            
            log.info("[SpellCheckService] Rule removed successfully: {}", ruleId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        lock.readLock().lock();
        try {
            stats.put("totalRules", getAllRules().size());
            stats.put("directMappings", directCorrectMap.size());
            stats.put("businessDomains", new ArrayList<>(rulesCache.keySet()));
            
            // Usage frequency statistics
            Map<String, Integer> usageStats = new HashMap<>();
            for (SpellCheckRule rule : getAllRules()) {
                if (rule.getUsageCount() > 0) {
                    usageStats.put(rule.getWrongSpellings().toString(), rule.getUsageCount());
                }
            }
            stats.put("usageStats", usageStats);
            
        } finally {
            lock.readLock().unlock();
        }
        
        return stats;
    }
    
    /**
     * Get default configuration
     */
    private Properties getDefaultProperties() {
        Properties props = new Properties();
        
        // Geographic error correction
        props.setProperty("geo.beijing", "北亰,北平 -> 北京 | 9 | ALL | Beijing geographic error correction");
        props.setProperty("geo.shenzhen", "深坳,深壕 -> 深圳 | 8 | CN | Shenzhen geographic error correction");
        props.setProperty("geo.cities", "上海市,广州市 -> 上海,广州 | 7 | CN | City name suffix removal");
        
        // Airport error correction
        props.setProperty("airport.capital", "首都机场,PEK -> 北京首都国际机场 | 9 | CN | Capital airport standardization");
        props.setProperty("airport.pudong", "浦东机场,PVG -> 上海浦东国际机场 | 9 | CN | Pudong airport standardization");
        props.setProperty("airport.hongqiao", "虹桥机场,SHA -> 上海虹桥国际机场 | 8 | CN | Hongqiao airport standardization");
        
        // Brand error correction
        props.setProperty("brand.hilton", "希尔敦,Hilton Hotel -> 希尔顿 | 9 | ALL | Hilton brand correction");
        props.setProperty("brand.marriott", "万豪酒店,Marriott Hotel -> 万豪 | 9 | ALL | Marriott brand correction");
        props.setProperty("brand.intercontinental", "洲际酒店,InterContinental Hotel -> 洲际 | 9 | ALL | InterContinental brand correction");
        
        return props;
    }
    
    /**
     * Reload configuration (hot update)
     */
    public void reloadConfig() {
        log.info("[SpellCheckService] Starting hot update configuration...");
        loadConfig();
        log.info("[SpellCheckService] Hot update completed");
    }
    
    /**
     * Export configuration as JSON
     */
    public String exportConfigAsJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getAllRules());
        } catch (Exception e) {
            log.error("[SpellCheckService] Export configuration failed", e);
            return "[]";
        }
    }
    
    /**
     * Import configuration (JSON format)
     */
    public void importConfigFromJson(String jsonConfig) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<SpellCheckRule> rules = mapper.readValue(jsonConfig, 
                    mapper.getTypeFactory().constructCollectionType(List.class, SpellCheckRule.class));
            
            for (SpellCheckRule rule : rules) {
                if (rule.isValid()) {
                    addRule(rule);
                }
            }
            
            log.info("[SpellCheckService] Configuration imported successfully, count: {}", rules.size());
        } catch (Exception e) {
            log.error("[SpellCheckService] Import configuration failed", e);
        }
    }
}
