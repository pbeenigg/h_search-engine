package com.heytrip.hotel.search.common.parser;

import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;

/**
 * 酒店解析器接口
 * 职责：
 * - 基于 providerSource/tagSource 与 raw 原文，解析结构化字段
 * - 当前阶段直接复用 HotelStructuredExtractor 的解析结果
 */
public interface HotelParser {

    /**
     * 解析 raw，返回结构化结果
     * @param raw 原文（可能为转义 JSON 字符串）
     * @return 结构化解析结果
     */
    HotelStructuredExtractor.Result parse(String raw);

    /**
     * 解析器支持的 providerSource（Elong|Agoda）或标签源（CN|INTL|HMT）的说明，可用于路由选择
     */
    String name();
}
