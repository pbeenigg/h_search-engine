package com.heytrip.hotel.search.infra.search.dto;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


@Data
@AllArgsConstructor
public class SearchResult {

    private long total;
    private List<HotelIndexDoc> items;
}
