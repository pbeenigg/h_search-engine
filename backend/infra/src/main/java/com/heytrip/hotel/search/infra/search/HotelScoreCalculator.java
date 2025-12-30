package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.infra.search.doc.HotelIndexDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 酒店综合评分计算器
 * 权重分配：
 * - ES相关性得分：50%（包含多层级匹配：match、matchPhrase、keyword精确）
 * - 关键词命中率：30%（使用简单split分词，性能开销小）
 * - 距离用户位置：20%
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelScoreCalculator {

    private final KeywordHitRateCalculator hitRateCalculator;

    /**
     * ES相关性得分权重（50%）
     * 已包含多层级匹配：标准match、matchPhrase、keyword精确匹配
     */
    private static final double ES_SCORE_WEIGHT = 0.5;

    /**
     * 关键词命中率权重（30%）
     * 使用简单split分词，性能开销小（<1ms/次）
     */
    private static final double HIT_RATE_WEIGHT = 0.3;

    /**
     * 距离用户位置权重（20%）
     */
    private static final double DISTANCE_WEIGHT = 0.2;

    /**
     * 最大距离（公里）- 超过此距离评分为0
     */
    private static final double MAX_DISTANCE_KM = 100.0;

    /**
     * 计算酒店综合得分（带关键词命中率）
     *
     * @param hotel           酒店文档
     * @param keyword         搜索关键词（用于计算命中率）
     * @param esScore         ES返回的原始_score
     * @param distanceKm      距离用户位置的距离（公里）
     * @param userLat         用户纬度（可选）
     * @param userLon         用户经度（可选）
     * @return 综合得分（0-100）
     */
    public double calculateScore(HotelIndexDoc hotel, String keyword, Double esScore, Double distanceKm, 
                                 Double userLat, Double userLon) {
        
        // 1. ES相关性得分（0-50分）
        double esScoreNormalized = normalizeEsScore(esScore);
        double esPoints = esScoreNormalized * ES_SCORE_WEIGHT * 100;

        // 2. 关键词命中率得分（0-30分）
        // 使用简单分词，性能开销小（<1ms），可以启用
        double hitRatePoints = 0.0;
        if (keyword != null && !keyword.isEmpty()) {
            double hitRate = hitRateCalculator.calculatePreciseHitRate(keyword, hotel);
            // 命中率已经是0-1.5的范围（含加成），需要归一化到0-1
            double normalizedHitRate = Math.min(1.0, hitRate);
            hitRatePoints = normalizedHitRate * HIT_RATE_WEIGHT * 100;
        }

        // 3. 距离得分（0-20分）
        double distancePoints = 0.0;
        if (userLat != null && userLon != null && hotel.getLat() != null && hotel.getLon() != null) {
            // 如果没有提供距离，则计算距离
            if (distanceKm == null) {
                distanceKm = calculateDistance(userLat, userLon, hotel.getLat(), hotel.getLon());
            }
            double distanceScoreNormalized = normalizeDistanceScore(distanceKm);
            distancePoints = distanceScoreNormalized * DISTANCE_WEIGHT * 100;
        } else {
            // 如果没有用户位置，则距离得分为平均分（保持公平性）
            distancePoints = DISTANCE_WEIGHT * 100 * 0.5;
        }

        // 4. 综合得分
        double totalScore = esPoints + hitRatePoints + distancePoints;

        // 改为TRACE级别，减少性能开销
        if (log.isTraceEnabled()) {
            log.trace("[Score] hotelId={} keyword='{}' esScore={:.2f} hitRate={:.2f} distance={}km " +
                            "→ es={:.2f} hitRate={:.2f} dist={:.2f} → total={:.2f}",
                    hotel.getHotelId(), keyword, esScore, hitRatePoints, distanceKm,
                    esPoints, hitRatePoints, distancePoints, totalScore);
        }

        return totalScore;
    }

    /**
     * 计算酒店综合得分（兼容方法，无关键词）
     *
     * @param hotel           酒店文档
     * @param keywordScore    关键词匹配得分（ES返回的_score）
     * @param distanceKm      距离用户位置的距离（公里）
     * @param userLat         用户纬度（可选）
     * @param userLon         用户经度（可选）
     * @return 综合得分（0-100）
     */
    public double calculateScore(HotelIndexDoc hotel, Double keywordScore, Double distanceKm, 
                                 Double userLat, Double userLon) {
        return calculateScore(hotel, null, keywordScore, distanceKm, userLat, userLon);
    }

    /**
     * 计算酒店综合得分（不计算命中率，性能优化版本）
     * 用于大量结果的初步排序，避免命中率计算的开销
     *
     * @param hotel           酒店文档
     * @param esScore         ES返回的原始_score
     * @param distanceKm      距离用户位置的距离（公里）
     * @param userLat         用户纬度（可选）
     * @param userLon         用户经度（可选）
     * @return 综合得分（0-100）
     */
    public double calculateScoreWithoutHitRate(HotelIndexDoc hotel, Double esScore, Double distanceKm,
                                               Double userLat, Double userLon) {
        // 1. ES相关性得分（0-70分，提高权重补偿命中率缺失）
        double esScoreNormalized = normalizeEsScore(esScore);
        double esPoints = esScoreNormalized * 0.7 * 100;

        // 2. 距离得分（0-30分）
        double distancePoints = 0.0;
        if (userLat != null && userLon != null && hotel.getLat() != null && hotel.getLon() != null) {
            if (distanceKm == null) {
                distanceKm = calculateDistance(userLat, userLon, hotel.getLat(), hotel.getLon());
            }
            double distanceScoreNormalized = normalizeDistanceScore(distanceKm);
            distancePoints = distanceScoreNormalized * 0.3 * 100;
        } else {
            distancePoints = 0.3 * 100 * 0.5;
        }

        return esPoints + distancePoints;
    }

    /**
     * 归一化ES得分到0-1区间
     * ES的_score通常在0-200之间（优化后可能更高），需要归一化
     *
     * @param score ES返回的原始_score
     * @return 归一化后的得分（0-1）
     */
    private double normalizeEsScore(Double score) {
        if (score == null || score <= 0) {
            return 0.0;
        }
        // 使用对数函数压缩分数，避免极端值
        // log(1 + score) / log(1 + 200) 将0-200映射到0-1
        // 这样可以适应更高的ES得分（多层级匹配后得分更高）
        double normalized = Math.log(1 + score) / Math.log(1 + 200);
        return Math.min(1.0, normalized);
    }

    /**
     * 归一化距离得分到0-1区间
     * 距离越近，得分越高
     *
     * @param distanceKm 距离（公里）
     * @return 归一化后的得分（0-1）
     */
    private double normalizeDistanceScore(Double distanceKm) {
        if (distanceKm == null || distanceKm < 0) {
            return 0.5; // 未知距离，给予中等分数
        }
        if (distanceKm >= MAX_DISTANCE_KM) {
            return 0.0; // 超过最大距离，得分为0
        }
        // 距离得分：1 - (distance / maxDistance)
        // 距离0公里得1分，距离100公里得0分，线性递减
        return 1.0 - (distanceKm / MAX_DISTANCE_KM);
    }

    /**
     * 使用 Haversine 公式计算两点间距离（公里）
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 计算酒店综合得分（简化版，无距离考虑）
     *
     * @param hotel        酒店文档
     * @param keywordScore 关键词匹配得分
     * @return 综合得分（0-100）
     */
    public double calculateScore(HotelIndexDoc hotel, Double keywordScore) {
        return calculateScore(hotel, keywordScore, null, null, null);
    }
}
