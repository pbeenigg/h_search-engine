package com.heytrip.hotel.search.common.parser;

import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import org.springframework.stereotype.Component;

/**
 * 国内（艺龙）解析器
 */
@Component
public class CnHotelParser implements HotelParser {
    @Override
    public HotelStructuredExtractor.Result parse(String raw) {
        return HotelStructuredExtractor.extract(raw, "Elong");
    }

    @Override
    public String name() {
        return "Elong";
    }
}
