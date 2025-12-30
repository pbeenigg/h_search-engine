package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.SyncLogDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogDetailRepository extends JpaRepository<SyncLogDetail, Long> {
    List<SyncLogDetail> findBySyncLogId(Long syncLogId);
    Page<SyncLogDetail> findBySyncLogId(Long syncLogId, Pageable pageable);
}
