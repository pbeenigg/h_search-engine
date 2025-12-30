package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.AmapCitycode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 高德城市代码Repository
 */
@Repository
public interface AmapCitycodeRepository extends JpaRepository<AmapCitycode, Long> {

    /**
     * 根据adcode查询城市代码
     *
     * @param adcode 区县代码
     * @return 城市代码实体
     */
    Optional<AmapCitycode> findByAdcode(String adcode);

    /**
     * 根据citycode查询城市代码列表
     *
     * @param citycode 城市代码
     * @return 城市代码列表
     */
    List<AmapCitycode> findByCitycode(String citycode);

    /**
     * 根据名称查询城市代码
     *
     * @param name 城市名称
     * @return 城市代码实体
     */
    Optional<AmapCitycode> findByName(String name);

    /**
     * 查询所有城市级别的代码（citycode不为空的记录）
     *
     * @return 城市代码列表
     */
    List<AmapCitycode> findByCitycodeIsNotNull();

    /**
     * 检查adcode是否存在
     *
     * @param adcode 区县代码
     * @return 是否存在
     */
    boolean existsByAdcode(String adcode);

    /**
     * 查询所有地级市及以上城市（排除国家级和省级）
     * 包含：地级市、地区、自治州、盟、直辖市、特别行政区
     * 规则：adcode为6位数字，且以00结尾（如：110100北京市东城区 → 110000北京市）
     *
     * @return 地级市及以上城市列表
     */
    @Query(value = """
        SELECT * FROM amap_citycode 
        WHERE 
            -- 排除国家级
            name != '中国'
            AND name != '中华人民共和国'
            -- 排除省份名称
            AND name NOT IN ('河北省', '山西省', '辽宁省', '吉林省', '黑龙江省', 
                            '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省', '河南省', '湖北省', '湖南省',
                            '广东省', '海南省', '四川省', '贵州省', '云南省', '陕西省', '甘肃省', '青海省')
            -- 保留地级市：4位数字+00格式（如：110000, 440100）
            AND adcode REGEXP '^[1-9][0-9]{3}00$'
        ORDER BY adcode
        """, nativeQuery = true)
    List<AmapCitycode> findAllPrefectureLevelCities();
}
