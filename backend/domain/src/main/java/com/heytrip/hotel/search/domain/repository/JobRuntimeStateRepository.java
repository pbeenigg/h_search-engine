package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.JobRuntimeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRuntimeStateRepository extends JpaRepository<JobRuntimeState, Long> {
    Optional<JobRuntimeState> findByJobCode(String jobCode);
}
