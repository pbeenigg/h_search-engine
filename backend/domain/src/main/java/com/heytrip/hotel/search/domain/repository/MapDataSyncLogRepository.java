package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.MapDataSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 地图数据同步日志Repository
 */
@Repository
public interface MapDataSyncLogRepository extends JpaRepository<MapDataSyncLog, Long> {

    /**
     * 根据数据来源和数据类型查询日志
     *
     * @param source   数据来源
     * @param dataType 数据类型
     * @return 日志列表
     */
    List<MapDataSyncLog> findBySourceAndDataType(String source, String dataType);

    /**
     * 根据状态查询日志
     *
     * @param status 状态
     * @return 日志列表
     */
    List<MapDataSyncLog> findByStatus(String status);

    /**
     * 根据数据来源、数据类型和状态查询日志
     *
     * @param source   数据来源
     * @param dataType 数据类型
     * @param status   状态
     * @return 日志列表
     */
    List<MapDataSyncLog> findBySourceAndDataTypeAndStatus(String source, String dataType, String status);

    /**
     * 根据traceId查询日志
     *
     * @param traceId 追踪ID
     * @return 日志实体
     */
    Optional<MapDataSyncLog> findByTraceId(String traceId);

    /**
     * 查询指定时间范围内的日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<MapDataSyncLog> findByStartTimeBetween(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 查询最近一次成功的同步日志
     *
     * @param source   数据来源
     * @param dataType 数据类型
     * @return 日志实体
     */
    @Query("SELECT m FROM MapDataSyncLog m WHERE m.source = :source AND m.dataType = :dataType " +
            "AND m.status = 'SUCCESS' ORDER BY m.endTime DESC LIMIT 1")
    Optional<MapDataSyncLog> findLastSuccessLog(@Param("source") String source, @Param("dataType") String dataType);

    /**
     * 统计指定数据来源和类型的成功次数
     *
     * @param source   数据来源
     * @param dataType 数据类型
     * @return 成功次数
     */
    @Query("SELECT COUNT(m) FROM MapDataSyncLog m WHERE m.source = :source AND m.dataType = :dataType AND m.status = 'SUCCESS'")
    long countSuccessLogs(@Param("source") String source, @Param("dataType") String dataType);
}
