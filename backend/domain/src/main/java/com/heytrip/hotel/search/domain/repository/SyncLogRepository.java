package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 同步日志仓储
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    
    /**
     * 根据任务代码和状态查询同步日志
     * @param jobCode 任务代码
     * @param status 状态
     * @return 同步日志列表
     */
    List<SyncLog> findByJobCodeAndStatus(String jobCode, String status);
}
