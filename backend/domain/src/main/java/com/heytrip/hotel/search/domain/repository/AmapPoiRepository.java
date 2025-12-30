package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.AmapPoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 高德POI数据Repository
 */
@Repository
public interface AmapPoiRepository extends JpaRepository<AmapPoi, String> {

    /**
     * 根据采集批次查询POI数据
     *
     * @param sourceBatch 采集批次标识
     * @return POI列表
     */
    List<AmapPoi> findBySourceBatch(String sourceBatch);

    /**
     * 根据城市代码查询POI数据
     *
     * @param citycode 城市代码
     * @return POI列表
     */
    List<AmapPoi> findByCitycode(String citycode);

    /**
     * 根据POI类型代码查询POI数据
     *
     * @param typecode POI类型代码
     * @return POI列表
     */
    List<AmapPoi> findByTypecode(String typecode);

    /**
     * 根据城市代码和POI类型代码查询POI数据
     *
     * @param citycode 城市代码
     * @param typecode POI类型代码
     * @return POI列表
     */
    List<AmapPoi> findByCitycodeAndTypecode(String citycode, String typecode);

    /**
     * 统计指定采集批次的POI数量
     *
     * @param sourceBatch 采集批次标识
     * @return POI数量
     */
    long countBySourceBatch(String sourceBatch);

    /**
     * 批量保存或更新POI数据（使用原生SQL提高性能）
     * 注意：此方法需要在调用处手动管理事务
     *
     * @param id POI ID
     * @param name 名称
     * @param type 类型
     * @param typecode 类型代码
     * @param address 地址
     * @param location 位置
     * @param longitude 经度
     * @param latitude 纬度
     * @param pcode 省份代码
     * @param pname 省份名称
     * @param citycode 城市代码
     * @param cityname 城市名称
     * @param adcode 区县代码
     * @param adname 区县名称
     * @param parent 父级POI
     * @param distance 距离
     * @param dataHash 数据哈希
     * @param dataVersion 数据版本
     * @param sourceBatch 采集批次
     * @param isDeleted 是否删除
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO amap_poi (id, name, type, typecode, address, location, longitude, latitude, " +
            "pcode, pname, citycode, cityname, adcode, adname, parent, distance, data_hash, data_version, source_batch, " +
            "is_deleted, created_at, updated_at) VALUES " +
            "(:id, :name, :type, :typecode, :address, :location, :longitude, :latitude, " +
            ":pcode, :pname, :citycode, :cityname, :adcode, :adname, :parent, :distance, :dataHash, " +
            ":dataVersion, :sourceBatch, :isDeleted, :createdAt, :updatedAt) " +
            "ON DUPLICATE KEY UPDATE " +
            "name = VALUES(name), type = VALUES(type), typecode = VALUES(typecode), address = VALUES(address), " +
            "location = VALUES(location), longitude = VALUES(longitude), latitude = VALUES(latitude), " +
            "pcode = VALUES(pcode), pname = VALUES(pname), citycode = VALUES(citycode), cityname = VALUES(cityname), " +
            "adcode = VALUES(adcode), adname = VALUES(adname), parent = VALUES(parent), distance = VALUES(distance), " +
            "data_hash = VALUES(data_hash), data_version = VALUES(data_version), source_batch = VALUES(source_batch), " +
            "updated_at = VALUES(updated_at)",
            nativeQuery = true)
    void upsertPoi(
            @Param("id") String id,
            @Param("name") String name,
            @Param("type") String type,
            @Param("typecode") String typecode,
            @Param("address") String address,
            @Param("location") String location,
            @Param("longitude") BigDecimal longitude,
            @Param("latitude") BigDecimal latitude,
            @Param("pcode") String pcode,
            @Param("pname") String pname,
            @Param("citycode") String citycode,
            @Param("cityname") String cityname,
            @Param("adcode") String adcode,
            @Param("adname") String adname,
            @Param("parent") String parent,
            @Param("distance") String distance,
            @Param("dataHash") String dataHash,
            @Param("dataVersion") String dataVersion,
            @Param("sourceBatch") String sourceBatch,
            @Param("isDeleted") Boolean isDeleted,
            @Param("createdAt") java.time.OffsetDateTime createdAt,
            @Param("updatedAt") java.time.OffsetDateTime updatedAt
    );
}
