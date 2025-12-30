package com.heytrip.hotel.search.domain.repository;

import com.heytrip.hotel.search.domain.entity.AmapPoitype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 高德POI类型Repository
 */
@Repository
public interface AmapPoitypeRepository extends JpaRepository<AmapPoitype, Long> {

    /**
     * 根据typecode查询POI类型
     *
     * @param typecode POI类型代码
     * @return POI类型实体
     */
    Optional<AmapPoitype> findByTypecode(String typecode);

    /**
     * 根据大类中文名称查询POI类型列表
     *
     * @param bigCategoryCn 大类中文名称
     * @return POI类型列表
     */
    List<AmapPoitype> findByBigCategoryCn(String bigCategoryCn);

    /**
     * 检查typecode是否存在
     *
     * @param typecode POI类型代码
     * @return 是否存在
     */
    boolean existsByTypecode(String typecode);

    /**
     * 查询所有的typecode列表
     *
     * @return typecode列表
     */
    @Query("SELECT DISTINCT p.typecode FROM AmapPoitype p WHERE p.typecode IS NOT NULL")
    List<String> findAllTypecodes();
}
