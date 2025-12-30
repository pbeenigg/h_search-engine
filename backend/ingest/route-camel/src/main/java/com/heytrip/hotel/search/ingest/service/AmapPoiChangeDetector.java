package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.domain.entity.AmapPoi;
import com.heytrip.hotel.search.domain.repository.AmapPoiRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高德POI数据变更检测器
 * 职责：
 * - 检测POI数据的新增、更新、删除
 * - 比较POI字段的变更
 * - 生成变更报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapPoiChangeDetector {

    private final AmapPoiRepository poiRepository;

    /**
     * 变更类型
     */
    public enum ChangeType {
        NEW,      // 新增
        UPDATED,  // 更新
        DELETED,  // 删除
        UNCHANGED // 未变更
    }

    /**
     * POI变更记录
     */
    @Data
    @Builder
    public static class PoiChange {
        private String poiId;
        private String name;
        private ChangeType changeType;
        private List<FieldChange> fieldChanges;
        private AmapPoi oldPoi;
        private AmapPoi newPoi;
    }

    /**
     * 字段变更记录
     */
    @Data
    @Builder
    public static class FieldChange {
        private String fieldName;
        private String oldValue;
        private String newValue;
    }

    /**
     * 变更报告
     */
    @Data
    @Builder
    public static class ChangeReport {
        private long totalCount;
        private long newCount;
        private long updatedCount;
        private long deletedCount;
        private long unchangedCount;
        private List<PoiChange> changes;
        
        public boolean hasChanges() {
            return newCount > 0 || updatedCount > 0 || deletedCount > 0;
        }
    }

    /**
     * 检测POI数据变更（优化版：使用MD5哈希值批量比对）
     *
     * @param newPois 新采集的POI数据
     * @return 变更报告
     */
    public ChangeReport detectChanges(List<AmapPoi> newPois) {
        log.info("[AmapPoiChangeDetector] 开始检测POI数据变更，待检测数量：{}", newPois.size());
        long startTime = System.currentTimeMillis();
        
        // 1. 提取所有POI的ID，批量查询数据库（避免N+1问题）
        List<String> poiIds = newPois.stream()
                .map(AmapPoi::getId)
                .collect(Collectors.toList());
        
        List<AmapPoi> existingPois = poiRepository.findAllById(poiIds);
        
        // 2. 构建ID -> 旧POI的映射，便于快速查找
        Map<String, AmapPoi> existingPoiMap = existingPois.stream()
                .collect(Collectors.toMap(AmapPoi::getId, poi -> poi));
        
        // 3. 遍历新POI，使用MD5哈希值快速比对
        List<PoiChange> changes = new ArrayList<>();
        long newCount = 0;
        long updatedCount = 0;
        long unchangedCount = 0;
        
        for (AmapPoi newPoi : newPois) {
            AmapPoi existingPoi = existingPoiMap.get(newPoi.getId());
            
            if (existingPoi == null) {
                // 新增的POI
                changes.add(PoiChange.builder()
                        .poiId(newPoi.getId())
                        .name(newPoi.getName())
                        .changeType(ChangeType.NEW)
                        .newPoi(newPoi)
                        .fieldChanges(new ArrayList<>())
                        .build());
                newCount++;
            } else {
                // 使用MD5哈希值快速判断是否变更
                String oldHash = existingPoi.getDataHash();
                String newHash = newPoi.getDataHash();
                
                if (Objects.equals(oldHash, newHash)) {
                    // 哈希值相同，数据未变更
                    unchangedCount++;
                } else {
                    // 哈希值不同，数据有更新
                    changes.add(PoiChange.builder()
                            .poiId(newPoi.getId())
                            .name(newPoi.getName())
                            .changeType(ChangeType.UPDATED)
                            .oldPoi(existingPoi)
                            .newPoi(newPoi)
                            .fieldChanges(new ArrayList<>()) // MD5比对无需详细字段变更
                            .build());
                    updatedCount++;
                }
            }
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        ChangeReport report = ChangeReport.builder()
                .totalCount(newPois.size())
                .newCount(newCount)
                .updatedCount(updatedCount)
                .deletedCount(0L)
                .unchangedCount(unchangedCount)
                .changes(changes)
                .build();
        
        log.info("[AmapPoiChangeDetector] 变更检测完成：总数={}，新增={}，更新={}，未变更={}，耗时={}ms", 
                report.getTotalCount(), report.getNewCount(), report.getUpdatedCount(), 
                report.getUnchangedCount(), elapsedTime);
        
        return report;
    }

    /**
     * 计算POI数据的MD5哈希值
     * 计算字段：name + type + address + location + citycode + adcode
     *
     * @param poi POI实体
     * @return MD5哈希值（32位小写）
     */
    public static String calculatePoiHash(AmapPoi poi) {
        if (poi == null) {
            return null;
        }
        
        try {
            // 拼接关键字段
            StringBuilder sb = new StringBuilder();
            sb.append(poi.getName() != null ? poi.getName() : "");
            sb.append("|");
            sb.append(poi.getType() != null ? poi.getType() : "");
            sb.append("|");
            sb.append(poi.getAddress() != null ? poi.getAddress() : "");
            sb.append("|");
            sb.append(poi.getLocation() != null ? poi.getLocation() : "");
            sb.append("|");
            sb.append(poi.getCitycode() != null ? poi.getCitycode() : "");
            sb.append("|");
            sb.append(poi.getAdcode() != null ? poi.getAdcode() : "");
            
            // 计算MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(sb.toString().getBytes("UTF-8"));
            
            // 转换为32位小写十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("[AmapPoiChangeDetector] 计算POI哈希值失败: poiId={}", poi.getId(), e);
            return null;
        }
    }

    /**
     * 比较POI字段变更（已弃用，MD5比对更高效）
     *
     * @param oldPoi 旧POI
     * @param newPoi 新POI
     * @return 字段变更列表
     * @deprecated 使用MD5哈希值比对代替
     */
    @Deprecated
    private List<FieldChange> compareFields(AmapPoi oldPoi, AmapPoi newPoi) {
        List<FieldChange> changes = new ArrayList<>();
        
        // 比较名称
        if (!Objects.equals(oldPoi.getName(), newPoi.getName())) {
            changes.add(FieldChange.builder()
                    .fieldName("name")
                    .oldValue(oldPoi.getName())
                    .newValue(newPoi.getName())
                    .build());
        }
        
        // 比较类型
        if (!Objects.equals(oldPoi.getType(), newPoi.getType())) {
            changes.add(FieldChange.builder()
                    .fieldName("type")
                    .oldValue(oldPoi.getType())
                    .newValue(newPoi.getType())
                    .build());
        }
        
        // 比较地址
        if (!Objects.equals(oldPoi.getAddress(), newPoi.getAddress())) {
            changes.add(FieldChange.builder()
                    .fieldName("address")
                    .oldValue(oldPoi.getAddress())
                    .newValue(newPoi.getAddress())
                    .build());
        }
        
        // 比较经纬度（使用阈值比较，避免浮点误差）
        if (!isLocationEqual(oldPoi.getLongitude(), newPoi.getLongitude()) ||
            !isLocationEqual(oldPoi.getLatitude(), newPoi.getLatitude())) {
            changes.add(FieldChange.builder()
                    .fieldName("location")
                    .oldValue(formatLocation(oldPoi.getLongitude(), oldPoi.getLatitude()))
                    .newValue(formatLocation(newPoi.getLongitude(), newPoi.getLatitude()))
                    .build());
        }
        
        // 比较电话
        if (!Objects.equals(oldPoi.getAddress(), newPoi.getAddress())) {
            changes.add(FieldChange.builder()
                    .fieldName("address")
                    .oldValue(oldPoi.getAddress())
                    .newValue(newPoi.getAddress())
                    .build());
        }
        
        return changes;
    }

    /**
     * 比较经纬度是否相等（容忍0.000001的误差）
     */
    private boolean isLocationEqual(BigDecimal v1, BigDecimal v2) {
        if (v1 == null && v2 == null) {
            return true;
        }
        if (v1 == null || v2 == null) {
            return false;
        }
        return v1.subtract(v2).abs().compareTo(new BigDecimal("0.000001")) < 0;
    }

    /**
     * 格式化经纬度
     */
    private String formatLocation(BigDecimal lng, BigDecimal lat) {
        if (lng == null || lat == null) {
            return null;
        }
        return String.format("%.6f,%.6f", lng, lat);
    }

    /**
     * 检测已删除的POI（批次比较）
     *
     * @param currentBatchId 当前批次ID
     * @param previousBatchId 上一批次ID
     * @return 已删除的POI列表
     */
    public List<AmapPoi> detectDeletedPois(String currentBatchId, String previousBatchId) {
        log.info("[AmapPoiChangeDetector] 检测已删除POI：current={}, previous={}", 
                currentBatchId, previousBatchId);
        
        // 查询上一批次的POI
        List<AmapPoi> previousPois = poiRepository.findBySourceBatch(previousBatchId);
        
        // 查询当前批次的POI
        List<AmapPoi> currentPois = poiRepository.findBySourceBatch(currentBatchId);
        
        // 找出在上一批次存在但在当前批次不存在的POI
        List<AmapPoi> deletedPois = new ArrayList<>();
        for (AmapPoi previousPoi : previousPois) {
            boolean existsInCurrent = currentPois.stream()
                    .anyMatch(p -> p.getId().equals(previousPoi.getId()));
            if (!existsInCurrent) {
                deletedPois.add(previousPoi);
            }
        }
        
        log.info("[AmapPoiChangeDetector] 检测到{}个已删除POI", deletedPois.size());
        return deletedPois;
    }

    /**
     * 生成变更摘要
     *
     * @param report 变更报告
     * @return 变更摘要
     */
    public String generateChangeSummary(ChangeReport report) {
        StringBuilder summary = new StringBuilder();
        summary.append("POI数据变更摘要：\n");
        summary.append(String.format("- 总数：%d\n", report.getTotalCount()));
        summary.append(String.format("- 新增：%d\n", report.getNewCount()));
        summary.append(String.format("- 更新：%d\n", report.getUpdatedCount()));
        summary.append(String.format("- 删除：%d\n", report.getDeletedCount()));
        summary.append(String.format("- 未变更：%d\n", report.getUnchangedCount()));
        
        if (report.hasChanges()) {
            summary.append("\n关键变更：\n");
            report.getChanges().stream()
                    .filter(c -> c.getChangeType() == ChangeType.UPDATED)
                    .limit(10) // 只显示前10个
                    .forEach(change -> {
                        summary.append(String.format("  - %s (%s)：", change.getName(), change.getPoiId()));
                        change.getFieldChanges().forEach(fc -> 
                                summary.append(String.format("%s变更 ", fc.getFieldName())));
                        summary.append("\n");
                    });
        }
        
        return summary.toString();
    }
}
