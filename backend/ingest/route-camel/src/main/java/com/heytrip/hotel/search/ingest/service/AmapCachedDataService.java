package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.domain.entity.AmapCitycode;
import com.heytrip.hotel.search.domain.entity.AmapPoitype;
import com.heytrip.hotel.search.domain.repository.AmapCitycodeRepository;
import com.heytrip.hotel.search.domain.repository.AmapPoitypeRepository;
import com.heytrip.hotel.search.infra.config.AmapCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 高德关联数据缓存服务
 * 职责：
 * - 缓存城市代码数据
 * - 缓存POI类型数据
 * - 提供缓存清理接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapCachedDataService {

    private final AmapCitycodeRepository citycodeRepository;
    private final AmapPoitypeRepository poitypeRepository;

    // ==================== 城市代码缓存方法 ====================

    /**
     * 查询所有城市代码（带缓存）
     *
     * @return 城市代码列表
     */
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'all'")
    public List<AmapCitycode> findAllCitycodes() {
        log.debug("[AmapCachedData] 查询所有城市代码（缓存未命中）");
        return citycodeRepository.findAll();
    }

    /**
     * 查询所有城市级别的代码（带缓存）
     *
     * @return 城市代码列表
     */
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'cities'")
    public List<AmapCitycode> findAllCitiesWithCode() {
        log.debug("[AmapCachedData] 查询所有城市级别代码（缓存未命中）");
        return citycodeRepository.findByCitycodeIsNotNull();
    }

    /**
     * 查询所有地级市及以上城市（带缓存）
     * 包含：地级市、地区、自治州、盟、直辖市、特别行政区
     * 排除：国家级、省级
     *
     * @return 地级市及以上城市列表
     */
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'prefecture_cities'")
    public List<AmapCitycode> findAllPrefectureLevelCities() {
        log.debug("[AmapCachedData] 查询所有地级市及以上城市（缓存未命中）");
        return citycodeRepository.findAllPrefectureLevelCities();
    }

    /**
     * 根据adcode查询城市代码（带缓存）
     *
     * @param adcode 区县代码
     * @return 城市代码
     */
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'adcode:' + #adcode")
    public Optional<AmapCitycode> findCitycodeByAdcode(String adcode) {
        log.debug("[AmapCachedData] 查询城市代码: adcode={} （缓存未命中）", adcode);
        return citycodeRepository.findByAdcode(adcode);
    }

    /**
     * 根据citycode查询城市代码列表（带缓存）
     *
     * @param citycode 城市代码
     * @return 城市代码列表
     */
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'citycode:' + #citycode")
    public List<AmapCitycode> findCitycodesByCitycode(String citycode) {
        log.debug("[AmapCachedData] 查询城市代码列表: citycode={} （缓存未命中）", citycode);
        return citycodeRepository.findByCitycode(citycode);
    }

    /**
     * 根据名称查询城市代码（带缓存）
     *
     * @param name 城市名称
     * @return 城市代码
     */
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'name:' + #name")
    public Optional<AmapCitycode> findCitycodeByName(String name) {
        log.debug("[AmapCachedData] 查询城市代码: name={} （缓存未命中）", name);
        return citycodeRepository.findByName(name);
    }

    // ==================== POI类型缓存方法 ====================

    /**
     * 查询所有POI类型（带缓存）
     *
     * @return POI类型列表
     */
    @Cacheable(value = AmapCacheConfig.CACHE_POITYPE, key = "'all'")
    public List<AmapPoitype> findAllPoitypes() {
        log.debug("[AmapCachedData] 查询所有POI类型（缓存未命中）");
        return poitypeRepository.findAll();
    }

    /**
     * 根据typecode查询POI类型（带缓存）
     *
     * @param typecode POI类型代码
     * @return POI类型
     */
    @Cacheable(value = AmapCacheConfig.CACHE_POITYPE, key = "'typecode:' + #typecode")
    public Optional<AmapPoitype> findPoitypeByTypecode(String typecode) {
        log.debug("[AmapCachedData] 查询POI类型: typecode={} （缓存未命中）", typecode);
        return poitypeRepository.findByTypecode(typecode);
    }

    /**
     * 根据大类中文名查询POI类型列表（带缓存）
     *
     * @param bigCategoryCn 大类中文名
     * @return POI类型列表
     */
    @Cacheable(value = AmapCacheConfig.CACHE_POITYPE, key = "'bigcat:' + #bigCategoryCn")
    public List<AmapPoitype> findPoitypesByBigCategory(String bigCategoryCn) {
        log.debug("[AmapCachedData] 查询POI类型列表: bigCategoryCn={} （缓存未命中）", bigCategoryCn);
        return poitypeRepository.findByBigCategoryCn(bigCategoryCn);
    }

    // ==================== 缓存清理方法 ====================

    /**
     * 清除所有城市代码缓存
     */
    @CacheEvict(value = AmapCacheConfig.CACHE_CITYCODE, allEntries = true)
    public void evictAllCitycodeCaches() {
        log.info("[AmapCachedData] 清除所有城市代码缓存");
    }

    /**
     * 清除所有POI类型缓存
     */
    @CacheEvict(value = AmapCacheConfig.CACHE_POITYPE, allEntries = true)
    public void evictAllPoitypeCaches() {
        log.info("[AmapCachedData] 清除所有POI类型缓存");
    }

    /**
     * 清除所有缓存
     */
    @CacheEvict(value = {AmapCacheConfig.CACHE_CITYCODE, AmapCacheConfig.CACHE_POITYPE}, allEntries = true)
    public void evictAllCaches() {
        log.info("[AmapCachedData] 清除所有高德数据缓存");
    }

    /**
     * 清除指定adcode的缓存
     *
     * @param adcode 区县代码
     */
    @CacheEvict(value = AmapCacheConfig.CACHE_CITYCODE, key = "'adcode:' + #adcode")
    public void evictCitycodeByAdcode(String adcode) {
        log.debug("[AmapCachedData] 清除城市代码缓存: adcode={}", adcode);
    }

    /**
     * 清除指定typecode的缓存
     *
     * @param typecode POI类型代码
     */
    @CacheEvict(value = AmapCacheConfig.CACHE_POITYPE, key = "'typecode:' + #typecode")
    public void evictPoitypeByTypecode(String typecode) {
        log.debug("[AmapCachedData] 清除POI类型缓存: typecode={}", typecode);
    }
}
