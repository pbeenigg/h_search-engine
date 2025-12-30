package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.Hotels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelsRepository extends JpaRepository<Hotels, Long> {
    Optional<Hotels> findBySourceAndHotelId(String source, Long hotelId);
    Optional<Hotels> findByHotelId(Long hotelId);
    boolean existsByHotelId(Long hotelId);
}
