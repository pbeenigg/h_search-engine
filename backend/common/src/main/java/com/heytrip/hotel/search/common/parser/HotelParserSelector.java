package com.heytrip.hotel.search.common.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析器选择器：根据 providerSource/tagSource 选择对应解析器
 */
@Component
@RequiredArgsConstructor
public class HotelParserSelector {

    private final List<HotelParser> parsers;

    /**
     * @param providerSource Elong|Agoda（优先）
     * @param tagSource CN|INTL|HMT（可作为辅助）
     */
    public HotelParser select(String providerSource, String tagSource) {
        String key = (providerSource == null ? "" : providerSource).trim();
        for (HotelParser p : parsers) {
            if (p.name().equalsIgnoreCase(key)) {
                return p;
            }
        }
        // 兜底：HMT 走 Agoda 解析；默认也走 Agoda
        for (HotelParser p : parsers) {
            if ("Agoda".equalsIgnoreCase(p.name())) return p;
        }
        return parsers.isEmpty() ? null : parsers.get(0);
    }
}
