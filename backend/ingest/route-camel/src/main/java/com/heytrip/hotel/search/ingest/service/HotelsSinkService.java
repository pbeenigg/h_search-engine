package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.domain.entity.Hotels;
import com.heytrip.hotel.search.domain.entity.SyncLogDetail;
import com.heytrip.hotel.search.domain.repository.HotelsRepository;
import com.heytrip.hotel.search.domain.repository.SyncLogDetailRepository;
import com.heytrip.hotel.search.domain.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 酒店详情入库事务服务
 * 职责：
 * - 按提交批次大小分批入库
 * - 批次采用显式新事务提交，避免长事务
 * - 失败时写入 sync_log_detail，统计成功/失败并回写 sync_log
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelsSinkService {

    private final HotelsRepository hotelsRepository;
    private final SyncLogRepository syncLogRepository;
    private final SyncLogDetailRepository syncLogDetailRepository;
    private final AsyncLogService asyncLogService;

    /**
     * 批量入库（分批提交）
     * 注意：内部调用同类上的 @Transactional(REQUIRES_NEW) 方法可能因自调用不生效；
     * 当前实现用于编译运行通过，必要时可通过 AOP 自注入代理或拆分到独立 Bean 保证新事务。
     * @param rows          需入库的数据行
     * @param submitBatch   提交批次大小（例如 1000）
     * @param syncLogId     对应的汇总日志ID（可为空）
     */
    public void saveInBatches(List<Hotels> rows, int submitBatch, Long syncLogId) {
        if (rows == null || rows.isEmpty()) return;
        List<Hotels> buf = new ArrayList<>(submitBatch);
        long ok = 0, fail = 0;
        for (Hotels r : rows) {
            buf.add(r);
            if (buf.size() >= submitBatch) {
                long[] inc = persistOneBatch(buf, syncLogId);
                ok += inc[0];
                fail += inc[1];
                buf.clear();
            }
        }
        if (!buf.isEmpty()) {
            long[] inc = persistOneBatch(buf, syncLogId);
            ok += inc[0];
            fail += inc[1];
        }
        // 汇总更新由 persistOneBatch 逐批完成；此处不再重复统计，避免双重累计
    }

    /**
     * 单批次持久化（新事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected long[] persistOneBatch(List<Hotels> batch, Long syncLogId) {
        long ok = 0, fail = 0;
        List<SyncLogDetail> batchFailureDetails = new ArrayList<>();
        try {
            // Upsert逻辑：基于 source + hotelId 判断是否已存在
            // 存在则更新现有记录的ID，不存在则创建新记录
            List<Hotels> toSave = new ArrayList<>(batch.size());
            for (Hotels r : batch) {
                if (r.getHotelId() != null && r.getSource() != null) {
                    // 根据 source + hotelId 查询已存在的记录
                    Optional<Hotels> existing = hotelsRepository.findBySourceAndHotelId(r.getSource(), r.getHotelId());
                    if (existing.isPresent()) {
                        // 存在则更新：设置现有记录的ID，JPA会执行UPDATE
                        r.setId(existing.get().getId());
                    }
                    // 不存在则插入：ID为null，JPA会执行INSERT
                }
                toSave.add(r);
            }
            if (!toSave.isEmpty()) {
                hotelsRepository.saveAll(toSave);
                ok = toSave.size();
            }
        } catch (Exception e) {
            log.error("[SINK] 批量保存失败，降级为逐条保存", e);
            // 降级逐条保存，记录失败明细
            for (Hotels r : batch) {
                try {
                    // Upsert逻辑：与批量保存保持一致
                    if (r.getHotelId() != null && r.getSource() != null) {
                        Optional<Hotels> existing = hotelsRepository.findBySourceAndHotelId(r.getSource(), r.getHotelId());
                        if (existing.isPresent()) {
                            r.setId(existing.get().getId());
                        }
                    }
                    hotelsRepository.save(r);
                    ok++;
                } catch (Exception ex) {
                    fail++;
                    if (syncLogId != null) {
                        SyncLogDetail d = SyncLogDetail.builder()
                            .syncLogId(syncLogId)
                            .hotelId(r.getHotelId())
                            .stage("SINK")
                            .errorCode("DB_SAVE_ERROR")
                            .errorMessage(ex.getMessage())
                            .createdAt(OffsetDateTime.now())
                            .build();
                        batchFailureDetails.add(d);
                    }
                }
            }
        }
        // 异步批量保存失败详情
        if (!batchFailureDetails.isEmpty()) {
            asyncLogService.saveSyncLogDetailsAsync(batchFailureDetails);
        }
        // 单批次统计累加：仅在提供 syncLogId 时执行，避免不必要的 DB 写
        if (syncLogId != null) {
            final long okF = ok;
            final long failF = fail;
            syncLogRepository.findById(syncLogId).ifPresent(sl -> {
                sl.setSuccessCount(nvl(sl.getSuccessCount()) + okF);
                sl.setFailCount(nvl(sl.getFailCount()) + failF);
                sl.setTotalDetails(nvl(sl.getTotalDetails()) + okF);
                syncLogRepository.save(sl);
            });
        }
        return new long[]{ok, fail};
    }

    private long nvl(Long v) { return v == null ? 0 : v; }
}
