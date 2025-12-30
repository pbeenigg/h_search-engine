package com.heytrip.hotel.search.ingest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelIdsResponse {
    private DataNode data;
    private Boolean hasData;
    private Integer code;
    private Integer bizCode;
    private String message;
    private Boolean isOk;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        private Long maxHotelId;
        private List<Long> hotelIds;
    }
}
