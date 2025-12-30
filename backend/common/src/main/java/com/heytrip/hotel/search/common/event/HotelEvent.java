package com.heytrip.hotel.search.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis Stream -> 事件 DTO（生产者与消费者共享）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelEvent {
    private String eventType;        // UPSERT
    private Long rowId;              // hotels.id（可空）
    private Long hotelId;            // 酒店ID
    private String providerSource;   // Elong | Agoda
    private String tagSource;        // CN | INTL | HMT（非空）
    private String traceId;
    private Long syncLogId;
    private String fetchedAt;        // ISO8601 文本



}
