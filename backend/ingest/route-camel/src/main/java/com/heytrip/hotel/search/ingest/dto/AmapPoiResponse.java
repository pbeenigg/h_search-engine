package com.heytrip.hotel.search.ingest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 高德POI API响应DTO
 * 对应API：https://restapi.amap.com/v5/place/text
 */
@Data
public class AmapPoiResponse {

    /**
     * 状态码（1:成功, 0:失败）
     */
    private String status;

    /**
     * 状态信息
     */
    private String info;

    /**
     * 详细状态码（10000:正常, 其他:异常）
     */
    private String infocode;

    /**
     * 返回的POI数量
     */
    private String count;

    /**
     * POI列表
     */
    private List<AmapPoiItem> pois;

    /**
     * 判断响应是否成功
     *
     * @return true:成功, false:失败
     */
    public boolean isSuccess() {
        return "1".equals(status) && "10000".equals(infocode);
    }

    /**
     * 判断是否有数据
     *
     * @return true:有数据, false:无数据
     */
    public boolean hasData() {
        return pois != null && !pois.isEmpty();
    }
}
