package com.heytrip.hotel.search.common.parser;

import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import org.springframework.stereotype.Component;

/**
 * 国际（Agoda）解析器
 */
@Component
public class IntlHotelParser implements HotelParser {
    @Override
    public HotelStructuredExtractor.Result parse(String raw) {
        return HotelStructuredExtractor.extract(raw, "Agoda");
    }

    @Override
    public String name() {
        return "Agoda";
    }
}
