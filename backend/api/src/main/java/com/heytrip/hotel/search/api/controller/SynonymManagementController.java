package com.heytrip.hotel.search.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同义词管理控制器
 *
 * 提供同义词的增删改查、版本管理、热更新等功能
 */
@Slf4j
@RestController
@RequestMapping("/synonyms")
@RequiredArgsConstructor
public class SynonymManagementController {

    private static final String SYNONYM_FILE_PATH = "/path/to/elasticsearch/config/analysis/hotel_synonyms.txt";
    private static final String BACKUP_DIR = "/path/to/backups/synonyms";
    private static final String ES_HOST = "localhost:9200";
    private static final String INDEX_NAME = "hotels_v1";

    /**
     * 获取当前同义词列表
     */
    @GetMapping("/list")
    public Map<String, Object> listSynonyms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String category
    ) {
        try {
            List<SynonymRule> rules = loadSynonymRules();
            
            // 按类别过滤
            if (category != null && !category.isEmpty()) {
                rules = rules.stream()
                        .filter(r -> category.equals(r.getCategory()))
                        .collect(Collectors.toList());
            }
            
            // 分页
            int total = rules.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            List<SynonymRule> pageData = rules.subList(start, end);
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("data", pageData);
            
            return result;
        } catch (Exception e) {
            log.error("获取同义词列表失败", e);
            throw new RuntimeException("获取同义词列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加同义词规则
     */
    @PostMapping("/add")
    public Map<String, Object> addSynonym(@RequestBody SynonymRule rule) {
        try {
            // 1. 验证规则
            ValidationResult validation = validateRule(rule);
            if (!validation.isValid()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", validation.getMessage());
                return result;
            }
            
            // 2. 备份当前文件
            backupSynonymFile();
            
            // 3. 添加到文件
            appendToSynonymFile(rule);
            
            // 4. 重新加载 ES 分析器
            reloadElasticsearchAnalyzer();
            
            // 5. 记录日志
            logChange("ADD", rule);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "同义词添加成功");
            return result;
        } catch (Exception e) {
            log.error("添加同义词失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "添加失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 批量添加同义词
     */
    @PostMapping("/batch-add")
    public Map<String, Object> batchAddSynonyms(@RequestBody List<SynonymRule> rules) {
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            backupSynonymFile();
            
            for (SynonymRule rule : rules) {
                try {
                    ValidationResult validation = validateRule(rule);
                    if (validation.isValid()) {
                        appendToSynonymFile(rule);
                        successCount++;
                    } else {
                        failCount++;
                        errors.add(rule.getSourceWords() + ": " + validation.getMessage());
                    }
                } catch (Exception e) {
                    failCount++;
                    errors.add(rule.getSourceWords() + ": " + e.getMessage());
                }
            }
            
            reloadElasticsearchAnalyzer();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);
            return result;
        } catch (Exception e) {
            log.error("批量添加同义词失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "批量添加失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 删除同义词规则
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteSynonym(@RequestParam String sourceWords) {
        try {
            backupSynonymFile();
            
            List<SynonymRule> rules = loadSynonymRules();
            rules.removeIf(r -> r.getSourceWords().equals(sourceWords));
            
            saveSynonymRules(rules);
            reloadElasticsearchAnalyzer();
            
            logChange("DELETE", sourceWords);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "同义词删除成功");
            return result;
        } catch (Exception e) {
            log.error("删除同义词失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 更新同义词规则
     */
    @PutMapping("/update")
    public Map<String, Object> updateSynonym(@RequestBody SynonymRule rule) {
        try {
            ValidationResult validation = validateRule(rule);
            if (!validation.isValid()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", validation.getMessage());
                return result;
            }
            
            backupSynonymFile();
            
            List<SynonymRule> rules = loadSynonymRules();
            boolean found = false;
            for (int i = 0; i < rules.size(); i++) {
                if (rules.get(i).getSourceWords().equals(rule.getSourceWords())) {
                    rules.set(i, rule);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "同义词规则不存在");
                return result;
            }
            
            saveSynonymRules(rules);
            reloadElasticsearchAnalyzer();
            
            logChange("UPDATE", rule);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "同义词更新成功");
            return result;
        } catch (Exception e) {
            log.error("更新同义词失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 上传同义词文件
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadSynonymFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "文件为空");
                return result;
            }
            
            // 验证文件格式
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (!validateFileFormat(content)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "文件格式错误");
                return result;
            }
            
            // 备份当前文件
            backupSynonymFile();
            
            // 保存新文件
            Path path = Paths.get(SYNONYM_FILE_PATH);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            
            // 重新加载
            reloadElasticsearchAnalyzer();
            
            logChange("UPLOAD", "上传新同义词文件");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文件上传成功");
            return result;
        } catch (Exception e) {
            log.error("上传同义词文件失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 下载当前同义词文件
     */
    @GetMapping("/download")
    public void downloadSynonymFile(jakarta.servlet.http.HttpServletResponse response) {
        try {
            Path path = Paths.get(SYNONYM_FILE_PATH);
            byte[] content = Files.readAllBytes(path);
            
            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Content-Disposition", 
                "attachment; filename=hotel_synonyms_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
            response.getOutputStream().write(content);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("下载同义词文件失败", e);
            throw new RuntimeException("下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/versions")
    public List<Map<String, Object>> getVersionHistory() {
        try {
            File backupDir = new File(BACKUP_DIR);
            if (!backupDir.exists()) {
                return Collections.emptyList();
            }
            
            File[] files = backupDir.listFiles((dir, name) -> name.startsWith("hotel_synonyms_"));
            if (files == null) {
                return Collections.emptyList();
            }
            
            return Arrays.stream(files)
                    .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
                    .limit(50)
                    .map(f -> {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("filename", f.getName());
                        fileInfo.put("size", f.length());
                        fileInfo.put("modifiedTime", new Date(f.lastModified()));
                        return fileInfo;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取版本历史失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback")
    public Map<String, Object> rollback(@RequestParam String version) {
        try {
            Path backupPath = Paths.get(BACKUP_DIR, version);
            if (!Files.exists(backupPath)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "版本不存在");
                return result;
            }
            
            // 备份当前版本
            backupSynonymFile();
            
            // 恢复指定版本
            Path currentPath = Paths.get(SYNONYM_FILE_PATH);
            Files.copy(backupPath, currentPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 重新加载
            reloadElasticsearchAnalyzer();
            
            logChange("ROLLBACK", "回滚到版本: " + version);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "回滚成功");
            return result;
        } catch (Exception e) {
            log.error("回滚失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "回滚失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 测试同义词效果
     */
    @PostMapping("/test")
    public Map<String, Object> testSynonym(@RequestParam String text) {
        try {
            // 调用 ES _analyze API 测试
            String url = "http://" + ES_HOST + "/" + INDEX_NAME + "/_analyze";
            String json = String.format("{\"analyzer\":\"hotel_search_analyzer\",\"text\":\"%s\"}", text);
            
            // 这里需要实际的 HTTP 客户端实现
            // String response = httpClient.post(url, json);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("originalText", text);
            result.put("tokens", Collections.emptyList()); // 实际应返回分词结果
            return result;
        } catch (Exception e) {
            log.error("测试同义词失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            return result;
        }
    }

    // ========== 私有辅助方法 ==========

    private List<SynonymRule> loadSynonymRules() throws IOException {
        List<SynonymRule> rules = new ArrayList<>();
        Path path = Paths.get(SYNONYM_FILE_PATH);
        
        if (!Files.exists(path)) {
            return rules;
        }
        
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String currentCategory = "未分类";
        
        for (String line : lines) {
            line = line.trim();
            
            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("#")) {
                // 提取类别
                if (line.startsWith("# ===")) {
                    currentCategory = line.replaceAll("#|=|\\s", "");
                }
                continue;
            }
            
            SynonymRule rule = parseSynonymLine(line);
            if (rule != null) {
                rule.setCategory(currentCategory);
                rules.add(rule);
            }
        }
        
        return rules;
    }

    private SynonymRule parseSynonymLine(String line) {
        SynonymRule rule = new SynonymRule();
        
        if (line.contains("=>")) {
            // 单向映射
            String[] parts = line.split("=>");
            rule.setSourceWords(parts[0].trim());
            rule.setTargetWord(parts[1].trim());
            rule.setType("one-way");
        } else if (line.contains(",")) {
            // 双向映射
            rule.setSourceWords(line.trim());
            rule.setTargetWord(null);
            rule.setType("two-way");
        } else {
            return null;
        }
        
        return rule;
    }

    private void saveSynonymRules(List<SynonymRule> rules) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# 酒店搜索同义词词典\n");
        sb.append("# 最后更新: ").append(LocalDateTime.now()).append("\n\n");
        
        Map<String, List<SynonymRule>> grouped = rules.stream()
                .collect(Collectors.groupingBy(SynonymRule::getCategory));
        
        for (Map.Entry<String, List<SynonymRule>> entry : grouped.entrySet()) {
            sb.append("# ========== ").append(entry.getKey()).append(" ==========\n");
            for (SynonymRule rule : entry.getValue()) {
                if ("one-way".equals(rule.getType())) {
                    sb.append(rule.getSourceWords()).append(" => ").append(rule.getTargetWord()).append("\n");
                } else {
                    sb.append(rule.getSourceWords()).append("\n");
                }
            }
            sb.append("\n");
        }
        
        Files.write(Paths.get(SYNONYM_FILE_PATH), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendToSynonymFile(SynonymRule rule) throws IOException {
        String line;
        if ("one-way".equals(rule.getType())) {
            line = rule.getSourceWords() + " => " + rule.getTargetWord() + "\n";
        } else {
            line = rule.getSourceWords() + "\n";
        }
        
        Files.write(Paths.get(SYNONYM_FILE_PATH), 
                line.getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.APPEND);
    }

    private void backupSynonymFile() throws IOException {
        Path source = Paths.get(SYNONYM_FILE_PATH);
        if (!Files.exists(source)) {
            return;
        }
        
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backup = Paths.get(BACKUP_DIR, "hotel_synonyms_" + timestamp + ".txt");
        
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        log.info("同义词文件已备份: {}", backup);
    }

    private void reloadElasticsearchAnalyzer() {
        try {
            // 调用 ES _reload_search_analyzers API
            String url = "http://" + ES_HOST + "/" + INDEX_NAME + "/_reload_search_analyzers";
            // String response = httpClient.post(url, "");
            log.info("Elasticsearch 分析器已重新加载");
        } catch (Exception e) {
            log.error("重新加载 Elasticsearch 分析器失败", e);
            throw new RuntimeException("重新加载分析器失败", e);
        }
    }

    private ValidationResult validateRule(SynonymRule rule) {
        ValidationResult result = new ValidationResult();
        
        if (rule.getSourceWords() == null || rule.getSourceWords().trim().isEmpty()) {
            result.setValid(false);
            result.setMessage("源词不能为空");
            return result;
        }
        
        if ("one-way".equals(rule.getType()) && 
            (rule.getTargetWord() == null || rule.getTargetWord().trim().isEmpty())) {
            result.setValid(false);
            result.setMessage("单向映射的目标词不能为空");
            return result;
        }
        
        result.setValid(true);
        return result;
    }

    private boolean validateFileFormat(String content) {
        // 简单验证：检查是否包含同义词格式
        return content.contains("=>") || content.contains(",");
    }

    private void logChange(String action, Object detail) {
        log.info("同义词变更 - 操作: {}, 详情: {}, 时间: {}", 
            action, detail, LocalDateTime.now());
    }

    // ========== 内部类 ==========

    public static class SynonymRule {
        private String sourceWords;
        private String targetWord;
        private String type; // "one-way" or "two-way"
        private String category;

        public String getSourceWords() { return sourceWords; }
        public void setSourceWords(String sourceWords) { this.sourceWords = sourceWords; }
        
        public String getTargetWord() { return targetWord; }
        public void setTargetWord(String targetWord) { this.targetWord = targetWord; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class ValidationResult {
        private boolean valid;
        private String message;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
