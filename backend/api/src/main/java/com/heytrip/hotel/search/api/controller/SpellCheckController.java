package com.heytrip.hotel.search.api.controller;

import com.heytrip.hotel.search.infra.nlp.SpellCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 拼写检查控制器API
 *
 * 提供文本拼写检查和规则管理功能
 */
@Slf4j
@RestController
@RequestMapping("/spellcheck")
@RequiredArgsConstructor
public class SpellCheckController {
    
    private final SpellCheckService spellCheckService;
    
    /**
     * 文本纠错
     * POST /api/spellcheck/correct
     */
    @PostMapping("/correct")
    public ResponseEntity<Map<String, Object>> correctText(
            @RequestParam String text,
            @RequestParam(defaultValue = "ALL") String businessDomain) {
        
        try {
            String corrected = spellCheckService.getCorrection(text, businessDomain);
            boolean wasCorrected = !corrected.equals(text);
            
            Map<String, Object> result = new HashMap<>();
            result.put("original", text);
            result.put("corrected", corrected);
            result.put("wasCorrected", wasCorrected);
            result.put("businessDomain", businessDomain);
            
            log.info("[SpellCheckAPI] 文本纠错: {} -> {} (领域: {}, 是否纠错: {})", 
                    text, corrected, businessDomain, wasCorrected);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 文本纠错失败: {}", text, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "纠错失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 获取规则列表
     * GET /api/spellcheck/rules
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getAllRules(
            @RequestParam(name = "businessDomain", defaultValue = "ALL") String businessDomain) {
        
        try {
            List<SpellCheckService.SpellCheckRule> rules;
            if ("ALL".equals(businessDomain)) {
                rules = spellCheckService.getAllRules();
            } else {
                rules = spellCheckService.getRules(businessDomain);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("rules", rules);
            result.put("totalCount", rules.size());
            result.put("businessDomain", businessDomain);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 获取规则列表失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取规则失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 添加规则
     * POST /api/spellcheck/rules
     */
    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> addRule(@RequestBody SpellCheckService.SpellCheckRule rule) {
        try {
            if (rule == null || !rule.isValid()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "规则无效");
                error.put("message", "请检查规则格式，权重必须在1-10之间");
                return ResponseEntity.badRequest().body(error);
            }

            spellCheckService.addRule(rule);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "添加规则成功");
            result.put("rule", rule);
            
            log.info("[SpellCheckAPI] 添加规则: {}", rule);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 添加规则失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "添加规则失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 删除规则
     * DELETE /api/spellcheck/rules/{ruleId}
     */
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> removeRule(@PathVariable String ruleId) {
        try {
            spellCheckService.removeRule(ruleId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "删除规则成功");
            result.put("ruleId", ruleId);
            
            log.info("[SpellCheckAPI] 删除规则: {}", ruleId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 删除规则失败: {}", ruleId, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "删除规则失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 重载配置
     * POST /api/spellcheck/reload
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConfig() {
        try {
            CompletableFuture.runAsync(() -> {
                spellCheckService.reloadConfig();
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置重载已启动");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("[SpellCheckAPI] 重载拼写检查配置");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 重载配置失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "重载配置失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 获取统计信息
     * GET /api/spellcheck/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = spellCheckService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 获取统计信息失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取统计信息失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 导出配置
     * GET /api/spellcheck/export
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportConfig() {
        try {
            String jsonConfig = spellCheckService.exportConfigAsJson();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .header("Content-Disposition", "attachment; filename=\"spellcheck-config.json\"")
                    .body(jsonConfig);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 导出配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 导入配置
     * POST /api/spellcheck/import
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importConfig(@RequestBody Map<String, Object> config) {
        try {
            String jsonConfig = config.get("jsonConfig").toString();

            spellCheckService.importConfigFromJson(jsonConfig);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "导入配置成功");
            
            log.info("[SpellCheckAPI] 导入配置成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 导入配置失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "导入配置失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 批量纠错
     * POST /api/spellcheck/correct-batch
     */
    @PostMapping("/correct-batch")
    public ResponseEntity<Map<String, Object>> correctBatch(
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) request.get("texts");
            String businessDomain = request.getOrDefault("businessDomain", "ALL").toString();
            
            List<Map<String, Object>> results = texts.stream().map(text -> {
                String corrected = spellCheckService.getCorrection(text, businessDomain);
                boolean wasCorrected = !corrected.equals(text);
                
                Map<String, Object> result = new HashMap<>();
                result.put("original", text);
                result.put("corrected", corrected);
                result.put("wasCorrected", wasCorrected);
                
                return result;
            }).collect(java.util.stream.Collectors.toList());
            
            long correctedCount = results.stream().mapToLong(r -> (Boolean) r.get("wasCorrected") ? 1 : 0).sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("totalCount", texts.size());
            response.put("correctedCount", correctedCount);
            response.put("correctionRate", correctedCount * 100.0 / texts.size());
            response.put("businessDomain", businessDomain);
            
            log.info("[SpellCheckAPI] 批量纠错: {}/{} ({:.1f}%)", 
                    correctedCount, texts.size(), response.get("correctionRate"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[SpellCheckAPI] 批量纠错失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "批量纠错失败");
            error.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
