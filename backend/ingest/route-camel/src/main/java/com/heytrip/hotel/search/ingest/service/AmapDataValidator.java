package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.domain.entity.AmapPoi;
import com.heytrip.hotel.search.infra.config.AmapConfig;
import com.heytrip.hotel.search.ingest.dto.AmapPoiItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德POI数据校验器
 * 职责：
 * - 校验POI数据的必填字段
 * - 校验经纬度合法性
 * - 校验数据格式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapDataValidator {

    private final AmapConfig amapConfig;

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * 校验POI数据（从API响应）
     *
     * @param poiItem POI项目
     * @return 校验结果
     */
    public ValidationResult validate(AmapPoiItem poiItem) {
        List<String> errors = new ArrayList<>();

        if (poiItem == null) {
            errors.add("POI数据为空");
            return new ValidationResult(false, errors);
        }

        // 必填字段校验
        if (amapConfig.getValidation().getCheckRequiredFields()) {
            validateRequiredFields(poiItem, errors);
        }

        // 经纬度合法性校验
        if (amapConfig.getValidation().getCheckLocation() && 
                StringUtils.hasText(poiItem.getLocation())) {
            validateLocation(poiItem.getLocation(), errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 校验POI实体数据
     *
     * @param poi POI实体
     * @return 校验结果
     */
    public ValidationResult validate(AmapPoi poi) {
        List<String> errors = new ArrayList<>();

        if (poi == null) {
            errors.add("POI实体为空");
            return new ValidationResult(false, errors);
        }

        // 必填字段校验
        if (amapConfig.getValidation().getCheckRequiredFields()) {
            if (!StringUtils.hasText(poi.getId())) {
                errors.add("POI ID不能为空");
            }
            if (!StringUtils.hasText(poi.getName())) {
                errors.add("POI名称不能为空");
            }
        }

        // 经纬度合法性校验
        if (amapConfig.getValidation().getCheckLocation() && 
                poi.getLongitude() != null && poi.getLatitude() != null) {
            validateCoordinates(poi.getLongitude(), poi.getLatitude(), errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 校验必填字段
     *
     * @param poiItem POI项目
     * @param errors  错误列表
     */
    private void validateRequiredFields(AmapPoiItem poiItem, List<String> errors) {
        if (!StringUtils.hasText(poiItem.getId())) {
            errors.add("POI ID不能为空");
        }
        if (!StringUtils.hasText(poiItem.getName())) {
            errors.add("POI名称不能为空");
        }
    }

    /**
     * 校验经纬度字符串格式和范围
     *
     * @param location 经纬度字符串（格式：lng,lat）
     * @param errors   错误列表
     */
    private void validateLocation(String location, List<String> errors) {
        try {
            String[] parts = location.split(",");
            if (parts.length != 2) {
                errors.add("经纬度格式不正确，应为：lng,lat");
                return;
            }

            BigDecimal lng = new BigDecimal(parts[0].trim());
            BigDecimal lat = new BigDecimal(parts[1].trim());

            validateCoordinates(lng, lat, errors);
        } catch (NumberFormatException e) {
            errors.add("经纬度解析失败：" + e.getMessage());
        }
    }

    /**
     * 校验经纬度数值范围
     *
     * @param lng    经度
     * @param lat    纬度
     * @param errors 错误列表
     */
    private void validateCoordinates(BigDecimal lng, BigDecimal lat, List<String> errors) {
        AmapConfig.LocationBounds bounds = amapConfig.getValidation().getLocationBounds();

        double lngValue = lng.doubleValue();
        double latValue = lat.doubleValue();

        // 基本经纬度范围校验
        if (lngValue < -180 || lngValue > 180) {
            errors.add(String.format("经度超出有效范围[-180, 180]: %.6f", lngValue));
        }
        if (latValue < -90 || latValue > 90) {
            errors.add(String.format("纬度超出有效范围[-90, 90]: %.6f", latValue));
        }

        // 中国境内范围校验
        if (lngValue < bounds.getMinLng() || lngValue > bounds.getMaxLng()) {
            errors.add(String.format("经度不在中国境内范围[%.1f, %.1f]: %.6f", 
                    bounds.getMinLng(), bounds.getMaxLng(), lngValue));
        }
        if (latValue < bounds.getMinLat() || latValue > bounds.getMaxLat()) {
            errors.add(String.format("纬度不在中国境内范围[%.1f, %.1f]: %.6f", 
                    bounds.getMinLat(), bounds.getMaxLat(), latValue));
        }
    }

    /**
     * 批量校验POI数据
     *
     * @param poiItems POI项目列表
     * @return 有效的POI项目列表
     */
    public List<AmapPoiItem> filterValidPois(List<AmapPoiItem> poiItems) {
        if (poiItems == null || poiItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<AmapPoiItem> validPois = new ArrayList<>();
        int invalidCount = 0;

        for (AmapPoiItem item : poiItems) {
            ValidationResult result = validate(item);
            if (result.isValid()) {
                validPois.add(item);
            } else {
                invalidCount++;
                log.debug("[AmapDataValidator] POI数据校验失败: id={}, errors={}", 
                        item.getId(), result.getErrorMessage());
            }
        }

        if (invalidCount > 0) {
            log.warn("[AmapDataValidator] 共{}条POI数据校验失败", invalidCount);
        }

        return validPois;
    }

    /**
     * 解析经纬度字符串
     *
     * @param location 经纬度字符串（格式：lng,lat）
     * @return [经度, 纬度]数组，解析失败返回null
     */
    public BigDecimal[] parseLocation(String location) {
        if (!StringUtils.hasText(location)) {
            return null;
        }

        try {
            String[] parts = location.split(",");
            if (parts.length != 2) {
                return null;
            }

            BigDecimal lng = new BigDecimal(parts[0].trim());
            BigDecimal lat = new BigDecimal(parts[1].trim());

            return new BigDecimal[]{lng, lat};
        } catch (NumberFormatException e) {
            log.warn("[AmapDataValidator] 经纬度解析失败: location={}, error={}", location, e.getMessage());
            return null;
        }
    }
}
