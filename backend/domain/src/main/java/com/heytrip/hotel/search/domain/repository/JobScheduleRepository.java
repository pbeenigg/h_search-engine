package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.JobSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobScheduleRepository extends JpaRepository<JobSchedule, Long> {
    Optional<JobSchedule> findByJobCode(String jobCode);
}
