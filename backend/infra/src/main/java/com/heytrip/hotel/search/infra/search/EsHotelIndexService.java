package com.heytrip.hotel.search.infra.search;

import com.heytrip.hotel.search.common.util.HotelStructuredExtractor;
import com.heytrip.hotel.search.domain.entity.Hotels;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ES 酒店索引服务接口
 */
public interface EsHotelIndexService {

    /**
     * 将 hotels + parsed 结果写入 ES（UPSERT）。
     * 目前为骨架实现，后续接入 Elasticsearch 客户端。
     * @param tagSource CN|INTL|HMT
     * @param providerSource Elong|Agoda
     * @param hotel 持久化实体
     * @param parsed  解析补齐结果
     */
    void upsert(String tagSource, String providerSource, Hotels hotel, HotelStructuredExtractor.Result parsed);

    /**
     * 批量 upsert（批量大小 1000），内部实现 refresh=false。
     * 返回 per-item 结果，便于调用方按失败清单做 DLQ。
     */
    BulkResult bulkUpsert(List<Hotels> hotels,
                          Function<Hotels, HotelStructuredExtractor.Result> parsedSupplier,
                          Function<Hotels, String> tagSourceFn,
                          Function<Hotels, String> providerSourceFn);

    /**
     * 批量写入结果
     */
    class BulkResult {
        private final List<Long> successIds;
        private final Map<Long, String> failed; // hotelId -> error message

        public BulkResult(List<Long> successIds, Map<Long, String> failed) {
            this.successIds = successIds;
            this.failed = failed;
        }

        public List<Long> getSuccessIds() { return successIds; }
        public Map<Long, String> getFailed() { return failed; }
        public boolean allSucceeded() { return failed == null || failed.isEmpty(); }
        public int successCount() { return successIds == null ? 0 : successIds.size(); }
        public int failCount() { return failed == null ? 0 : failed.size(); }
    }
}
