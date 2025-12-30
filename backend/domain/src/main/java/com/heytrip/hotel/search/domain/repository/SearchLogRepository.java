package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 搜索日志仓储接口
 */
@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 查询指定时间范围内的搜索日志
     */
    List<SearchLog> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * 查询零结果的搜索日志
     */
    List<SearchLog> findByResultCountAndCreatedAtAfter(Integer resultCount, OffsetDateTime after);

    /**
     * 统计指定时间范围内的搜索总数
     */
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    /**
     * 统计零结果查询数量
     */
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.resultCount = 0 AND s.createdAt BETWEEN :start AND :end")
    Long countZeroResultsByCreatedAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    /**
     * 统计有点击的查询数量
     */
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.clickedHotelId IS NOT NULL AND s.createdAt BETWEEN :start AND :end")
    Long countWithClicksByCreatedAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    /**
     * 查询高频零结果查询词
     */
    @Query("SELECT s.query, COUNT(s) as cnt FROM SearchLog s WHERE s.resultCount = 0 AND s.createdAt >= :after GROUP BY s.query ORDER BY cnt DESC")
    List<Object[]> findTopZeroResultQueries(@Param("after") OffsetDateTime after);
}
