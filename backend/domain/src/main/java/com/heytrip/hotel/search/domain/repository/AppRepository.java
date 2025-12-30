package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppRepository extends JpaRepository<App, String> {
    boolean existsByAppId(String appId);
}
