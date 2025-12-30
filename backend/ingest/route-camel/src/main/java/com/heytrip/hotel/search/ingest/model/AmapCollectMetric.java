package com.heytrip.hotel.search.ingest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高德POI采集指标
 * 定义：每一个城市+每一个POI类型 作为一个采集指标
 * 例如：北京市 + 餐饮服务，就是一个采集指标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmapCollectMetric {

    /**
     * 城市代码（或区县代码）
     */
    private String cityCode;

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * POI类型代码
     */
    private String typeCode;

    /**
     * POI类型名称
     */
    private String typeName;

    /**
     * 采集指标唯一标识（城市代码_类型代码）
     */
    private String metricKey;

    /**
     * 采集状态（PENDING:待采集, PROCESSING:采集中, COMPLETED:已完成, FAILED:失败）
     */
    private String status;

    /**
     * 采集到的POI数量
     */
    private Long collectedCount;

    /**
     * 生成采集指标Key
     *
     * @param cityCode 城市代码
     * @param typeCode POI类型代码
     * @return 采集指标Key
     */
    public static String generateMetricKey(String cityCode, String typeCode) {
        return cityCode + "_" + typeCode;
    }
}
