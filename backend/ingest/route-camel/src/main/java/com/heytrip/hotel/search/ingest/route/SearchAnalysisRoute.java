package com.heytrip.hotel.search.ingest.route;

import com.heytrip.hotel.search.domain.entity.SearchLog;
import com.heytrip.hotel.search.domain.repository.SearchLogRepository;
import com.heytrip.hotel.search.infra.notify.MailNotifier;
import com.heytrip.hotel.search.infra.search.SearchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索质量分析定时路由
 * 职责：
 * 1. 每天凌晨2点分析零结果查询
 * 2. 每小时统计搜索质量指标
 * 3. 发送告警邮件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAnalysisRoute extends RouteBuilder {

    private final SearchLogRepository searchLogRepository;
    private final SearchLogService searchLogService;
    private final MailNotifier mailNotifier;

    @Override
    public void configure() throws Exception {
        
        // 每天凌晨2点分析零结果查询
        // Quartz Cron: 秒 分 时 日 月 星期 [年]
        from("quartz://search-analysis-zero-results?cron=0 0 2 * * ?")
                .routeId("search-analysis-zero-results")
                .log(LoggingLevel.DEBUG,"[SEARCH_ANALYSIS] 开始分析零结果查询")
                .bean(this, "analyzeZeroResultQueries")
                .choice()
                    .when(simple("${body} != null"))
                        .log(LoggingLevel.DEBUG,"[SEARCH_ANALYSIS] 零结果查询分析完成")
                        .bean(mailNotifier, "sendText('零结果查询分析报告', ${body})")
                    .otherwise()
                        .log(LoggingLevel.DEBUG,"[SEARCH_ANALYSIS] 昨日无零结果查询")
                .end();

        // 每小时统计搜索质量指标
        // Quartz Cron: 秒 分 时 日 月 星期 [年]
        from("quartz://search-analysis-metrics?cron=0 0 */1 * * ?")
                .routeId("search-analysis-metrics")
                .log(LoggingLevel.DEBUG,"[SEARCH_ANALYSIS] 开始统计搜索质量指标")
                .bean(this, "reportSearchMetrics")
                .choice()
                    .when(simple("${body} != null"))
                        .log(LoggingLevel.DEBUG,"[SEARCH_ANALYSIS] 搜索质量指标统计完成")
                        .bean(mailNotifier, "sendText('搜索质量告警', ${body})")
                    .otherwise()
                        .log(LoggingLevel.DEBUG,"[SEARCH_ANALYSIS] 搜索质量正常，无需告警")
                .end();
    }

    /**
     * 分析零结果查询
     * 
     * @return 分析报告（如果有零结果查询）
     */
    public String analyzeZeroResultQueries() {
        try {
            OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);
            List<SearchLog> zeroResults = searchLogRepository
                    .findByResultCountAndCreatedAtAfter(0, yesterday);
            
            if (zeroResults.isEmpty()) {
                return null;
            }
            
            // 统计高频零结果查询
            Map<String, Long> queryFreq = zeroResults.stream()
                    .collect(Collectors.groupingBy(SearchLog::getQuery, Collectors.counting()));
            
            // 构建报告
            StringBuilder report = new StringBuilder();
            report.append("【搜索质量分析报告】\n\n");
            report.append("统计时间：").append(yesterday.toLocalDate()).append("\n");
            report.append("零结果查询总数：").append(zeroResults.size()).append("\n\n");
            report.append("高频零结果查询（Top 20）：\n");
            
            queryFreq.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(20)
                    .forEach(e -> report.append("  - ").append(e.getKey())
                            .append(": ").append(e.getValue()).append("次\n"));
            
            log.info("[SEARCH_ANALYSIS] 零结果查询分析完成，共{}条", zeroResults.size());
            return report.toString();
            
        } catch (Exception e) {
            log.error("[SEARCH_ANALYSIS] 分析零结果查询失败", e);
            return null;
        }
    }

    /**
     * 统计搜索质量指标并判断是否需要告警
     * 
     * @return 告警消息（如果需要告警）
     */
    public String reportSearchMetrics() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime oneHourAgo = now.minusHours(1);
            
            SearchLogService.SearchMetrics metrics = searchLogService.getMetrics(oneHourAgo, now);
            
            log.info("[SEARCH_ANALYSIS] 过去1小时搜索指标: totalQueries={}, zeroResultRate={}, clickThroughRate={}",
                    metrics.getTotalQueries(),
                    String.format("%.2f%%", metrics.getZeroResultRate() * 100),
                    String.format("%.2f%%", metrics.getClickThroughRate() * 100));
            
            // 如果零结果率过高，发送告警
            if (metrics.getTotalQueries() > 100 && metrics.getZeroResultRate() > 0.15) {
                String alertMsg = String.format(
                        "【搜索质量告警】\n\n" +
                        "时间段：%s ~ %s\n" +
                        "总查询数：%d\n" +
                        "零结果率：%.2f%%（超过阈值15%%）\n" +
                        "点击率：%.2f%%\n\n" +
                        "请及时检查搜索引擎配置和索引质量。",
                        oneHourAgo.toLocalTime(),
                        now.toLocalTime(),
                        metrics.getTotalQueries(),
                        metrics.getZeroResultRate() * 100,
                        metrics.getClickThroughRate() * 100
                );
                
                log.warn("[SEARCH_ANALYSIS] 零结果率过高，发送告警邮件");
                return alertMsg;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("[SEARCH_ANALYSIS] 统计搜索指标失败", e);
            return null;
        }
    }
}
