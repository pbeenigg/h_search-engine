package com.heytrip.hotel.search.ingest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 高德API Key信息
 * 用于管理API Key的使用状态和配额
 */
@Data
@Builder
public class AmapApiKeyInfo {

    /**
     * API Key
     */
    private String key;

    /**
     * 今日已使用次数
     */
    private Integer todayUsedCount;

    /**
     * 每日配额限制
     */
    private Integer dailyQuota;

    /**
     * 统计日期
     */
    private LocalDate statisticsDate;

    /**
     * 是否在黑名单中
     */
    private Boolean inBlacklist;

    /**
     * 黑名单过期时间（毫秒时间戳）
     */
    private Long blacklistExpireTime;

    /**
     * 连续失败次数
     */
    private Integer consecutiveFailures;

    /**
     * 最后使用时间（毫秒时间戳）
     */
    private Long lastUsedTime;

    /**
     * 判断Key是否可用
     *
     * @return true:可用, false:不可用
     */
    public boolean isAvailable() {
        // 检查是否在黑名单中
        if (Boolean.TRUE.equals(inBlacklist)) {
            if (blacklistExpireTime != null && System.currentTimeMillis() < blacklistExpireTime) {
                return false;
            }
            // 黑名单已过期，重置状态
            inBlacklist = false;
            blacklistExpireTime = null;
            consecutiveFailures = 0;
        }

        // 检查是否超过当日配额
        if (!isSameDate()) {
            // 新的一天，重置计数
            todayUsedCount = 0;
            statisticsDate = LocalDate.now();
        }

        return todayUsedCount < dailyQuota;
    }

    /**
     * 检查统计日期是否为今天
     *
     * @return true:是今天, false:不是今天
     */
    private boolean isSameDate() {
        return statisticsDate != null && statisticsDate.equals(LocalDate.now());
    }

    /**
     * 增加使用次数
     */
    public void incrementUsage() {
        if (!isSameDate()) {
            todayUsedCount = 0;
            statisticsDate = LocalDate.now();
        }
        todayUsedCount++;
        lastUsedTime = System.currentTimeMillis();
    }

    /**
     * 标记为失败并加入黑名单
     *
     * @param blacklistDurationMs 黑名单持续时间（毫秒）
     */
    public void markAsFailure(long blacklistDurationMs) {
        consecutiveFailures++;
        inBlacklist = true;
        blacklistExpireTime = System.currentTimeMillis() + blacklistDurationMs;
    }

    /**
     * 标记为成功并重置失败计数
     */
    public void markAsSuccess() {
        consecutiveFailures = 0;
        inBlacklist = false;
        blacklistExpireTime = null;
    }

    /**
     * 获取剩余配额
     *
     * @return 剩余配额
     */
    public int getRemainingQuota() {
        if (!isSameDate()) {
            return dailyQuota;
        }
        return Math.max(0, dailyQuota - todayUsedCount);
    }
}
