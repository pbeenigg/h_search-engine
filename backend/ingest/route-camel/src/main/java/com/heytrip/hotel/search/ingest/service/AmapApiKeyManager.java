package com.heytrip.hotel.search.ingest.service;

import com.heytrip.hotel.search.infra.config.AmapConfig;
import com.heytrip.hotel.search.ingest.dto.AmapApiKeyInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 高德API Key管理器
 * 职责：
 * - 管理多个API Key的轮询使用
 * - 跟踪每个Key的使用次数和配额
 * - 故障Key自动加入黑名单
 * - 配额耗尽时自动切换到下一个Key
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapApiKeyManager {

    private final AmapConfig amapConfig;

    /**
     * Key信息映射表
     */
    private final Map<String, AmapApiKeyInfo> keyInfoMap = new ConcurrentHashMap<>();

    /**
     * 当前轮询索引（用于ROUND_ROBIN策略）
     */
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /**
     * 初始化所有API Key
     */
    @PostConstruct
    public void init() {
        List<String> keys = amapConfig.getApi().getKeys();
        if (keys == null || keys.isEmpty()) {
            log.warn("[AmapApiKeyManager] 未配置任何API Key，POI采集将无法进行");
            return;
        }

        int dailyQuota = amapConfig.getApi().getDailyQuotaPerKey();
        for (String key : keys) {
            AmapApiKeyInfo info = AmapApiKeyInfo.builder()
                    .key(key)
                    .todayUsedCount(0)
                    .dailyQuota(dailyQuota)
                    .statisticsDate(LocalDate.now())
                    .inBlacklist(false)
                    .blacklistExpireTime(null)
                    .consecutiveFailures(0)
                    .lastUsedTime(null)
                    .build();
            keyInfoMap.put(key, info);
        }

        log.info("[AmapApiKeyManager] 初始化完成，共{}个API Key，每个Key每日配额：{}", keys.size(), dailyQuota);
    }

    /**
     * 获取下一个可用的API Key
     *
     * @return API Key，如果所有Key都不可用则返回null
     */
    public String getNextAvailableKey() {
        List<String> keys = amapConfig.getApi().getKeys();
        if (keys == null || keys.isEmpty()) {
            log.error("[AmapApiKeyManager] 没有配置API Key");
            return null;
        }

        String strategy = amapConfig.getApi().getKeyRotationStrategy();
        
        if ("ROUND_ROBIN".equalsIgnoreCase(strategy)) {
            return getNextKeyByRoundRobin(keys);
        } else if ("RANDOM".equalsIgnoreCase(strategy)) {
            return getNextKeyByRandom(keys);
        } else {
            // 默认使用轮询策略
            return getNextKeyByRoundRobin(keys);
        }
    }

    /**
     * 轮询策略获取下一个可用Key
     *
     * @param keys Key列表
     * @return 可用的Key，如果都不可用则返回null
     */
    private String getNextKeyByRoundRobin(List<String> keys) {
        int size = keys.size();
        int attempts = 0;

        while (attempts < size) {
            int index = currentIndex.getAndIncrement() % size;
            String key = keys.get(index);
            AmapApiKeyInfo info = keyInfoMap.get(key);

            if (info != null && info.isAvailable()) {
                log.debug("[AmapApiKeyManager] 轮询策略选中Key: {} (剩余配额: {})", 
                        maskKey(key), info.getRemainingQuota());
                return key;
            }

            attempts++;
        }

        log.error("[AmapApiKeyManager] 所有API Key都不可用！");
        logKeyStatus();
        return null;
    }

    /**
     * 随机策略获取下一个可用Key
     *
     * @param keys Key列表
     * @return 可用的Key，如果都不可用则返回null
     */
    private String getNextKeyByRandom(List<String> keys) {
        List<String> availableKeys = keys.stream()
                .filter(key -> {
                    AmapApiKeyInfo info = keyInfoMap.get(key);
                    return info != null && info.isAvailable();
                })
                .collect(Collectors.toList());

        if (availableKeys.isEmpty()) {
            log.error("[AmapApiKeyManager] 所有API Key都不可用！");
            logKeyStatus();
            return null;
        }

        int randomIndex = (int) (Math.random() * availableKeys.size());
        String key = availableKeys.get(randomIndex);
        
        AmapApiKeyInfo info = keyInfoMap.get(key);
        log.debug("[AmapApiKeyManager] 随机策略选中Key: {} (剩余配额: {})", 
                maskKey(key), info.getRemainingQuota());
        
        return key;
    }

    /**
     * 标记Key使用成功
     *
     * @param key API Key
     */
    public void markSuccess(String key) {
        AmapApiKeyInfo info = keyInfoMap.get(key);
        if (info != null) {
            info.incrementUsage();
            info.markAsSuccess();
            
            log.debug("[AmapApiKeyManager] Key使用成功: {}, 今日已使用: {}/{}", 
                    maskKey(key), info.getTodayUsedCount(), info.getDailyQuota());
        }
    }

    /**
     * 标记Key使用失败（加入黑名单）
     *
     * @param key    API Key
     * @param reason 失败原因
     */
    public void markFailure(String key, String reason) {
        AmapApiKeyInfo info = keyInfoMap.get(key);
        if (info != null) {
            long blacklistDuration = amapConfig.getApi().getKeyBlacklistDuration() * 1000L;
            info.markAsFailure(blacklistDuration);
            
            log.warn("[AmapApiKeyManager] Key加入黑名单: {}, 原因: {}, 持续时间: {}秒, 连续失败次数: {}", 
                    maskKey(key), reason, amapConfig.getApi().getKeyBlacklistDuration(), 
                    info.getConsecutiveFailures());
        }
    }

    /**
     * 检查指定Key是否可用
     *
     * @param key API Key
     * @return true:可用, false:不可用
     */
    public boolean isKeyAvailable(String key) {
        AmapApiKeyInfo info = keyInfoMap.get(key);
        return info != null && info.isAvailable();
    }

    /**
     * 获取所有Key的状态信息
     *
     * @return Key状态信息列表
     */
    public List<AmapApiKeyInfo> getAllKeyStatus() {
        return keyInfoMap.values().stream()
                .collect(Collectors.toList());
    }

    /**
     * 获取可用Key的数量
     *
     * @return 可用Key数量
     */
    public int getAvailableKeyCount() {
        return (int) keyInfoMap.values().stream()
                .filter(AmapApiKeyInfo::isAvailable)
                .count();
    }

    /**
     * 检查是否还有可用的Key
     *
     * @return true:有可用Key, false:无可用Key
     */
    public boolean hasAvailableKey() {
        return getAvailableKeyCount() > 0;
    }

    /**
     * 打印所有Key的状态（用于调试）
     */
    public void logKeyStatus() {
        log.info("[AmapApiKeyManager] ========== API Key状态汇总 ==========");
        keyInfoMap.forEach((key, info) -> {
            log.info("[AmapApiKeyManager] Key: {}, 可用: {}, 今日使用: {}/{}, 黑名单: {}, 连续失败: {}", 
                    maskKey(key), 
                    info.isAvailable(),
                    info.getTodayUsedCount(), 
                    info.getDailyQuota(),
                    info.getInBlacklist(),
                    info.getConsecutiveFailures());
        });
        log.info("[AmapApiKeyManager] ==========================================");
    }

    /**
     * 掩码Key（保护敏感信息）
     *
     * @param key API Key
     * @return 掩码后的Key
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /**
     * 重置所有Key的统计信息（通常在新的一天开始时调用）
     */
    public void resetDailyStatistics() {
        keyInfoMap.values().forEach(info -> {
            info.setTodayUsedCount(0);
            info.setStatisticsDate(LocalDate.now());
        });
        log.info("[AmapApiKeyManager] 已重置所有Key的每日统计信息");
    }
}
