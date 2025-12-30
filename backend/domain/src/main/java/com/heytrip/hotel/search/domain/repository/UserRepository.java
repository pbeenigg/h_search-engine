package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);
    boolean existsByUserName(String userName);
    Optional<User> findByAppId(String appId);
}
