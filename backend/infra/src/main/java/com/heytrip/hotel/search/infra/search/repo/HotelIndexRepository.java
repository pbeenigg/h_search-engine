package com.heytrip.hotel.search.infra.search.repo;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelIndexRepository extends ElasticsearchRepository<HotelIndexDoc, String> {
}
