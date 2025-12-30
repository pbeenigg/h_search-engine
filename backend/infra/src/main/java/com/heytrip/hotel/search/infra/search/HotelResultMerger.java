package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import com.heytrip.hotel.search.infra.search.dto.ScoredHotelResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 酒店搜索结果去重和合并工具
 * 支持基于酒店ID、中英文名称的智能去重
 */
@Slf4j
@Component
public class HotelResultMerger {

    /**
     * 合并多个来源的酒店搜索结果并去重
     * 去重策略：
     * 1. 优先按hotelId去重（最可靠）
     * 2. 其次按标准化的中英文名称去重
     * 3. 保留综合评分最高的记录
     *
     * @param resultLists 多个搜索结果列表
     * @return 去重后的酒店列表（按综合评分降序排序）
     */
    @SafeVarargs
    public final List<ScoredHotelResult> mergeAndDeduplicate(List<ScoredHotelResult>... resultLists) {
        // 1. 合并所有结果
        List<ScoredHotelResult> allResults = new ArrayList<>();
        for (List<ScoredHotelResult> list : resultLists) {
            if (list != null && !list.isEmpty()) {
                allResults.addAll(list);
            }
        }

        if (allResults.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("[MERGE] 合并前总数={}", allResults.size());

        // 2. 按hotelId分组，保留评分最高的
        Map<Long, ScoredHotelResult> hotelIdMap = new HashMap<>();
        List<ScoredHotelResult> noHotelIdResults = new ArrayList<>();

        for (ScoredHotelResult result : allResults) {
            Long hotelId = result.getHotel().getHotelId();
            if (hotelId != null) {
                ScoredHotelResult existing = hotelIdMap.get(hotelId);
                if (existing == null || result.getFinalScore() > existing.getFinalScore()) {
                    hotelIdMap.put(hotelId, result);
                }
            } else {
                noHotelIdResults.add(result);
            }
        }

        // 3. 对没有hotelId的记录，按名称去重
        Map<String, ScoredHotelResult> nameMap = new HashMap<>();
        for (ScoredHotelResult result : noHotelIdResults) {
            String normalizedName = normalizeHotelName(result.getHotel());
            if (StringUtils.hasText(normalizedName)) {
                ScoredHotelResult existing = nameMap.get(normalizedName);
                if (existing == null || result.getFinalScore() > existing.getFinalScore()) {
                    nameMap.put(normalizedName, result);
                }
            }
        }

        // 4. 合并结果
        List<ScoredHotelResult> mergedResults = new ArrayList<>(hotelIdMap.values());
        mergedResults.addAll(nameMap.values());

        // 5. 按综合评分降序排序
        mergedResults.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));

        log.debug("[MERGE] 去重后总数={} (按hotelId去重={}, 按名称去重={})",
                mergedResults.size(), hotelIdMap.size(), nameMap.size());

        return mergedResults;
    }

    /**
     * 标准化酒店名称用于去重
     * 策略：
     * 1. 优先使用中文名称
     * 2. 如果中文名称为空，使用英文名称
     * 3. 转小写、去空格、去特殊字符、去音调
     *
     * @param hotel 酒店文档
     * @return 标准化后的名称
     */
    private String normalizeHotelName(HotelIndexDoc hotel) {
        String name = null;

        // 优先使用中文名称
        if (StringUtils.hasText(hotel.getNameCn())) {
            name = hotel.getNameCn();
        } else if (StringUtils.hasText(hotel.getNameEn())) {
            name = hotel.getNameEn();
        }

        if (name == null) {
            return null;
        }

        // 标准化处理
        name = name.toLowerCase(); // 转小写
        name = name.replaceAll("\\s+", ""); // 去空格
        name = name.replaceAll("[·\\-_()（）【】\\[\\]{}]", ""); // 去常见分隔符
        name = removeDiacritics(name); // 去音调标记

        return name;
    }

    /**
     * 去除字符串中的音调标记（用于英文名称标准化）
     *
     * @param input 原始字符串
     * @return 去除音调后的字符串
     */
    private String removeDiacritics(String input) {
        if (input == null) {
            return null;
        }
        // 使用NFD分解，然后去除音调标记
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    /**
     * 按hotelId去重（保留评分最高的）
     *
     * @param results 搜索结果列表
     * @return 去重后的结果列表
     */
    public List<ScoredHotelResult> deduplicateByHotelId(List<ScoredHotelResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ScoredHotelResult> hotelIdMap = new HashMap<>();
        for (ScoredHotelResult result : results) {
            Long hotelId = result.getHotel().getHotelId();
            if (hotelId != null) {
                ScoredHotelResult existing = hotelIdMap.get(hotelId);
                if (existing == null || result.getFinalScore() > existing.getFinalScore()) {
                    hotelIdMap.put(hotelId, result);
                }
            }
        }

        List<ScoredHotelResult> deduplicated = new ArrayList<>(hotelIdMap.values());
        deduplicated.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));

        return deduplicated;
    }

    /**
     * 过滤并返回TopN结果
     *
     * @param results 搜索结果列表
     * @param topN    返回数量
     * @return TopN结果
     */
    public List<ScoredHotelResult> selectTopN(List<ScoredHotelResult> results, int topN) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        return results.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 过滤掉无效的酒店数据
     * 无效数据：缺少必要字段（hotelId、名称、坐标等）
     *
     * @param results 搜索结果列表
     * @return 过滤后的结果列表
     */
    public List<ScoredHotelResult> filterInvalidData(List<ScoredHotelResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        return results.stream()
                .filter(result -> {
                    HotelIndexDoc hotel = result.getHotel();
                    // 必须有hotelId
                    if (hotel.getHotelId() == null) {
                        log.debug("[FILTER] 过滤无hotelId的数据");
                        return false;
                    }
                    // 必须有名称（中文或英文）
                    if (!StringUtils.hasText(hotel.getNameCn()) && !StringUtils.hasText(hotel.getNameEn())) {
                        log.debug("[FILTER] 过滤无名称的数据 hotelId={}", hotel.getHotelId());
                        return false;
                    }
                    // 必须有坐标
                    if (hotel.getLat() == null || hotel.getLon() == null) {
                        log.debug("[FILTER] 过滤无坐标的数据 hotelId={}", hotel.getHotelId());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
