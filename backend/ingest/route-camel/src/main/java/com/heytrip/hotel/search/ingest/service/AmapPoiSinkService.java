package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.domain.entity.AmapPoi;
import com.heytrip.hotel.search.domain.entity.MapDataSyncLog;
import com.heytrip.hotel.search.domain.repository.AmapPoiRepository;
import com.heytrip.hotel.search.domain.repository.MapDataSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德POI数据入库事务服务
 * 职责：
 * - 按提交批次大小分批入库
 * - 批次采用显式新事务提交，避免长事务
 * - 支持INSERT ON DUPLICATE KEY UPDATE（增量更新）
 * - 失败时记录详细日志，统计成功/失败并回写同步日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapPoiSinkService {

    private final AmapPoiRepository amapPoiRepository;
    private final MapDataSyncLogRepository mapDataSyncLogRepository;
    private final AmapPoiChangeDetector changeDetector;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 批量入库（分批提交，支持增量更新）
     *
     * @param pois        需入库的POI数据
     * @param commitSize  提交批次大小（例如 1000）
     * @param syncLogId   对应的同步日志ID（可为空）
     * @return [成功数, 失败数]
     */
    public long[] saveInBatches(List<AmapPoi> pois, int commitSize, Long syncLogId) {
        if (pois == null || pois.isEmpty()) {
            return new long[]{0, 0};
        }

        List<AmapPoi> buffer = new ArrayList<>(commitSize);
        long totalSuccess = 0;
        long totalFailure = 0;

        for (AmapPoi poi : pois) {
            buffer.add(poi);
            if (buffer.size() >= commitSize) {
                long[] result = persistOneBatch(buffer, syncLogId);
                totalSuccess += result[0];
                totalFailure += result[1];
                buffer.clear();
            }
        }

        // 处理剩余数据
        if (!buffer.isEmpty()) {
            long[] result = persistOneBatch(buffer, syncLogId);
            totalSuccess += result[0];
            totalFailure += result[1];
        }

        log.info("[AmapPoiSink] 批量入库完成，成功：{}，失败：{}", totalSuccess, totalFailure);
        return new long[]{totalSuccess, totalFailure};
    }

    /**
     * 单批次持久化（新事务，支持UPSERT）
     *
     * @param batch     POI批次数据
     * @param syncLogId 同步日志ID
     * @return [成功数, 失败数]
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected long[] persistOneBatch(List<AmapPoi> batch, Long syncLogId) {
        long success = 0;
        long failure = 0;

        try {
            // 尝试批量保存
            List<AmapPoi> savedPois = amapPoiRepository.saveAll(batch);
            success = savedPois.size();
            
            log.debug("[AmapPoiSink] 批次保存成功，数量：{}", success);

            // 更新同步日志
            if (syncLogId != null) {
                updateSyncLog(syncLogId, success, 0);
            }

        } catch (Exception e) {
            log.error("[AmapPoiSink] 批量保存失败，降级为逐条保存", e);
            
            // 降级逐条保存
            for (AmapPoi poi : batch) {
                try {
                    amapPoiRepository.save(poi);
                    success++;
                } catch (Exception ex) {
                    failure++;
                    log.warn("[AmapPoiSink] POI保存失败: id={}, error={}", 
                            poi.getId(), ex.getMessage());
                }
            }

            // 更新同步日志
            if (syncLogId != null) {
                updateSyncLog(syncLogId, success, failure);
            }
        }

        return new long[]{success, failure};
    }

    /**
     * 使用原生SQL进行UPSERT操作（性能更好）
     *
     * @param pois        POI数据列表
     * @param commitSize  提交批次大小
     * @param syncLogId   同步日志ID
     * @return [成功数, 失败数]
     */
    public long[] upsertInBatches(List<AmapPoi> pois, int commitSize, Long syncLogId) {
        if (pois == null || pois.isEmpty()) {
            return new long[]{0, 0};
        }

        List<AmapPoi> buffer = new ArrayList<>(commitSize);
        long totalSuccess = 0;
        long totalFailure = 0;

        for (AmapPoi poi : pois) {
            buffer.add(poi);
            if (buffer.size() >= commitSize) {
                long[] result = upsertOneBatch(buffer, syncLogId);
                totalSuccess += result[0];
                totalFailure += result[1];
                buffer.clear();
            }
        }

        // 处理剩余数据
        if (!buffer.isEmpty()) {
            long[] result = upsertOneBatch(buffer, syncLogId);
            totalSuccess += result[0];
            totalFailure += result[1];
        }

        log.info("[AmapPoiSink] UPSERT批量入库完成，成功：{}，失败：{}", totalSuccess, totalFailure);
        return new long[]{totalSuccess, totalFailure};
    }

    /**
     * 单批次UPSERT操作（新事务，使用JDBC批量执行）
     *
     * @param batch     POI批次数据
     * @param syncLogId 同步日志ID
     * @return [成功数, 失败数]
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected long[] upsertOneBatch(List<AmapPoi> batch, Long syncLogId) {
        long success = 0;
        long failure = 0;

        try {
            // 使用JDBC批量执行UPSERT（比JPA逐条执行更高效）
            String sql = "INSERT INTO amap_poi (id, name, type, typecode, address, location, longitude, latitude, " +
                    "pcode, pname, citycode, cityname, adcode, adname, parent, distance, data_hash, data_version, " +
                    "source_batch, is_deleted, created_at, updated_at) VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "name = VALUES(name), type = VALUES(type), typecode = VALUES(typecode), address = VALUES(address), " +
                    "location = VALUES(location), longitude = VALUES(longitude), latitude = VALUES(latitude), " +
                    "pcode = VALUES(pcode), pname = VALUES(pname), citycode = VALUES(citycode), cityname = VALUES(cityname), " +
                    "adcode = VALUES(adcode), adname = VALUES(adname), parent = VALUES(parent), distance = VALUES(distance), " +
                    "data_hash = VALUES(data_hash), data_version = VALUES(data_version), source_batch = VALUES(source_batch), " +
                    "updated_at = VALUES(updated_at)";

            int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    AmapPoi poi = batch.get(i);
                    int idx = 1;
                    ps.setString(idx++, poi.getId());
                    ps.setString(idx++, poi.getName());
                    ps.setString(idx++, poi.getType());
                    ps.setString(idx++, poi.getTypecode());
                    ps.setString(idx++, poi.getAddress());
                    ps.setString(idx++, poi.getLocation());
                    ps.setBigDecimal(idx++, poi.getLongitude());
                    ps.setBigDecimal(idx++, poi.getLatitude());
                    ps.setString(idx++, poi.getPcode());
                    ps.setString(idx++, poi.getPname());
                    ps.setString(idx++, poi.getCitycode());
                    ps.setString(idx++, poi.getCityname());
                    ps.setString(idx++, poi.getAdcode());
                    ps.setString(idx++, poi.getAdname());
                    ps.setString(idx++, poi.getParent());
                    ps.setString(idx++, poi.getDistance());
                    ps.setString(idx++, poi.getDataHash());
                    ps.setString(idx++, poi.getDataVersion());
                    ps.setString(idx++, poi.getSourceBatch());
                    ps.setBoolean(idx++, poi.getIsDeleted());
                    ps.setTimestamp(idx++, poi.getCreatedAt() != null ? Timestamp.from(poi.getCreatedAt().toInstant()) : null);
                    ps.setTimestamp(idx++, poi.getUpdatedAt() != null ? Timestamp.from(poi.getUpdatedAt().toInstant()) : null);
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            // 统计成功和失败数量
            for (int result : results) {
                if (result > 0) {
                    success++;
                } else {
                    failure++;
                }
            }

            log.debug("[AmapPoiSink] JDBC批量UPSERT完成，成功：{}，失败：{}", success, failure);

            // 更新同步日志
            if (syncLogId != null) {
                updateSyncLog(syncLogId, success, failure);
            }

        } catch (Exception e) {
            log.error("[AmapPoiSink] JDBC批量UPSERT失败", e);
            failure = batch.size();

            // 更新同步日志
            if (syncLogId != null) {
                updateSyncLog(syncLogId, 0, failure);
            }
        }

        return new long[]{success, failure};
    }

    /**
     * 更新同步日志的统计信息
     *
     * @param syncLogId    同步日志ID
     * @param successCount 成功数
     * @param failureCount 失败数
     */
    private void updateSyncLog(Long syncLogId, long successCount, long failureCount) {
        try {
            mapDataSyncLogRepository.findById(syncLogId).ifPresent(log -> {
                // 累加统计数据
                log.setSuccessCount(log.getSuccessCount() + successCount);
                log.setFailureCount(log.getFailureCount() + failureCount);
                log.setTotalCount(log.getSuccessCount() + log.getFailureCount());
                log.setUpdatedAt(OffsetDateTime.now());
                
                mapDataSyncLogRepository.save(log);
            });
        } catch (Exception e) {
            log.error("[AmapPoiSink] 更新同步日志失败: syncLogId={}", syncLogId, e);
        }
    }

    /**
     * 批量删除指定批次的POI数据
     *
     * @param batchId 批次ID
     * @return 删除数量
     */
    @Transactional
    public long deleteByBatch(String batchId) {
        try {
            List<AmapPoi> pois = amapPoiRepository.findBySourceBatch(batchId);
            if (!pois.isEmpty()) {
                amapPoiRepository.deleteAll(pois);
                log.info("[AmapPoiSink] 删除批次数据完成: batchId={}, count={}", batchId, pois.size());
                return pois.size();
            }
        } catch (Exception e) {
            log.error("[AmapPoiSink] 删除批次数据失败: batchId={}", batchId, e);
        }
        return 0;
    }

    /**
     * 统计指定批次的POI数量
     *
     * @param batchId 批次ID
     * @return POI数量
     */
    public long countByBatch(String batchId) {
        try {
            return amapPoiRepository.countBySourceBatch(batchId);
        } catch (Exception e) {
            log.error("[AmapPoiSink] 统计批次数据失败: batchId={}", batchId, e);
            return 0;
        }
    }

    /**
     * 带变更检测的批量保存
     *
     * @param pois        POI数据列表
     * @param commitSize  提交批次大小
     * @param syncLogId   同步日志ID
     * @return [成功数, 失败数, 变更报告]
     */
    public Object[] saveInBatchesWithChangeDetection(List<AmapPoi> pois, int commitSize, Long syncLogId) {
        // 1. 检测变更
        AmapPoiChangeDetector.ChangeReport changeReport = changeDetector.detectChanges(pois);
        
        // 2. 记录变更摘要
        String changeSummary = changeDetector.generateChangeSummary(changeReport);
        log.info("[AmapPoiSink] {}", changeSummary);
        
        // 3. 只保存有变更的POI（新增和更新）
        List<AmapPoi> poisToSave = changeReport.getChanges().stream()
                .filter(change -> change.getChangeType() == AmapPoiChangeDetector.ChangeType.NEW ||
                                 change.getChangeType() == AmapPoiChangeDetector.ChangeType.UPDATED)
                .map(change -> change.getNewPoi())
                .toList();
        
        // 4. 批量保存
        long[] result = upsertInBatches(poisToSave, commitSize, syncLogId);
        
        log.info("[AmapPoiSink] 变更检测完成保存：应保存={}，实际成功={}，失败={}", 
                poisToSave.size(), result[0], result[1]);
        
        return new Object[]{result[0], result[1], changeReport};
    }
}
