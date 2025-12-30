package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {
}
