package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.CodeQualityTrends;
import com.zamaz.mcp.github.entity.PRHistoricalMetrics;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.DeveloperProfile;
import com.zamaz.mcp.github.repository.CodeQualityTrendsRepository;
import com.zamaz.mcp.github.repository.PRHistoricalMetricsRepository;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import com.zamaz.mcp.github.repository.DeveloperProfileRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing historical trends in code quality and team performance metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalTrendAnalysisService {

    private final CodeQualityTrendsRepository codeQualityTrendsRepository;
    private final PRHistoricalMetricsRepository prHistoricalMetricsRepository;
    private final PullRequestReviewRepository pullRequestReviewRepository;
    private final DeveloperProfileRepository developerProfileRepository;

    /**
     * Calculate and store code quality trends for a repository
     */
    @Transactional
    public void calculateCodeQualityTrends(Long repositoryId, LocalDate analysisDate, PeriodType periodType) {
        log.info("Calculating code quality trends for repository {} on {} ({})", repositoryId, analysisDate, periodType);

        LocalDateTime startDate = calculatePeriodStart(analysisDate, periodType);
        LocalDateTime endDate = calculatePeriodEnd(analysisDate, periodType);

        List<PRHistoricalMetrics> metrics = prHistoricalMetricsRepository
            .findByRepositoryIdAndCreatedAtBetween(repositoryId, startDate, endDate);

        if (metrics.isEmpty()) {
            log.warn("No PR metrics found for repository {} in period {} to {}", repositoryId, startDate, endDate);
            return;
        }

        // Calculate various quality metrics
        Map<String, TrendMetric> trendMetrics = calculateTrendMetrics(metrics);

        // Store trends in database
        for (Map.Entry<String, TrendMetric> entry : trendMetrics.entrySet()) {
            String metricName = entry.getKey();
            TrendMetric metric = entry.getValue();

            CodeQualityTrends trend = CodeQualityTrends.builder()
                .repositoryId(repositoryId)
                .analysisDate(analysisDate)
                .periodType(periodType.getValue())
                .metricName(metricName)
                .metricValue(metric.getValue())
                .trendDirection(metric.getTrendDirection())
                .changePercentage(metric.getChangePercentage())
                .developerCount(metric.getDeveloperCount())
                .prCount(metrics.size())
                .issueCount(metric.getIssueCount())
                .baselineValue(metric.getBaselineValue())
                .targetValue(metric.getTargetValue())
                .build();

            codeQualityTrendsRepository.save(trend);
        }

        log.info("Calculated and stored {} quality trends for repository {}", trendMetrics.size(), repositoryId);
    }

    /**
     * Get comprehensive trend analysis for a repository
     */
    @Transactional(readOnly = true)
    public TrendAnalysisReport getTrendAnalysisReport(Long repositoryId, int months) {
        log.info("Generating trend analysis report for repository {} over {} months", repositoryId, months);

        LocalDate fromDate = LocalDate.now().minusMonths(months);
        List<CodeQualityTrends> trends = codeQualityTrendsRepository
            .findByRepositoryIdAndAnalysisDateAfter(repositoryId, fromDate);

        if (trends.isEmpty()) {
            log.warn("No trend data found for repository {} since {}", repositoryId, fromDate);
            return TrendAnalysisReport.builder()
                .repositoryId(repositoryId)
                .analysisDateRange(fromDate)
                .trends(Collections.emptyMap())
                .overallHealthScore(BigDecimal.ZERO)
                .build();
        }

        // Group trends by metric name
        Map<String, List<CodeQualityTrends>> trendsByMetric = trends.stream()
            .collect(Collectors.groupingBy(CodeQualityTrends::getMetricName));

        // Analyze each metric trend
        Map<String, MetricTrendAnalysis> metricAnalyses = new HashMap<>();
        for (Map.Entry<String, List<CodeQualityTrends>> entry : trendsByMetric.entrySet()) {
            String metricName = entry.getKey();
            List<CodeQualityTrends> metricTrends = entry.getValue();
            metricAnalyses.put(metricName, analyzeMetricTrend(metricTrends));
        }

        // Calculate overall health score
        BigDecimal overallHealthScore = calculateOverallHealthScore(metricAnalyses);

        // Generate insights and recommendations
        List<TrendInsight> insights = generateTrendInsights(metricAnalyses);
        List<String> recommendations = generateRecommendations(metricAnalyses);

        return TrendAnalysisReport.builder()
            .repositoryId(repositoryId)
            .analysisDateRange(fromDate)
            .trends(metricAnalyses)
            .overallHealthScore(overallHealthScore)
            .insights(insights)
            .recommendations(recommendations)
            .keyMetrics(identifyKeyMetrics(metricAnalyses))
            .alertingMetrics(identifyAlertingMetrics(metricAnalyses))
            .build();
    }

    /**
     * Compare trends between different time periods
     */
    @Transactional(readOnly = true)
    public TrendComparisonReport compareTrends(Long repositoryId, LocalDate period1Start, LocalDate period1End,
                                             LocalDate period2Start, LocalDate period2End) {
        log.info("Comparing trends for repository {} between periods {} to {} vs {} to {}", 
                repositoryId, period1Start, period1End, period2Start, period2End);

        List<CodeQualityTrends> period1Trends = codeQualityTrendsRepository
            .findByRepositoryIdAndAnalysisDateBetween(repositoryId, period1Start, period1End);
        
        List<CodeQualityTrends> period2Trends = codeQualityTrendsRepository
            .findByRepositoryIdAndAnalysisDateBetween(repositoryId, period2Start, period2End);

        Map<String, TrendComparison> comparisons = new HashMap<>();
        
        // Get all unique metric names from both periods
        Set<String> allMetrics = new HashSet<>();
        allMetrics.addAll(period1Trends.stream().map(CodeQualityTrends::getMetricName).collect(Collectors.toSet()));
        allMetrics.addAll(period2Trends.stream().map(CodeQualityTrends::getMetricName).collect(Collectors.toSet()));

        for (String metricName : allMetrics) {
            List<CodeQualityTrends> metric1 = period1Trends.stream()
                .filter(t -> t.getMetricName().equals(metricName))
                .collect(Collectors.toList());
            
            List<CodeQualityTrends> metric2 = period2Trends.stream()
                .filter(t -> t.getMetricName().equals(metricName))
                .collect(Collectors.toList());

            comparisons.put(metricName, compareMetricTrends(metric1, metric2));
        }

        return TrendComparisonReport.builder()
            .repositoryId(repositoryId)
            .period1Start(period1Start)
            .period1End(period1End)
            .period2Start(period2Start)
            .period2End(period2End)
            .comparisons(comparisons)
            .significantChanges(identifySignificantChanges(comparisons))
            .improvements(identifyImprovements(comparisons))
            .regressions(identifyRegressions(comparisons))
            .build();
    }

    /**
     * Predict future trends based on historical data
     */
    @Transactional(readOnly = true)
    public TrendPredictionReport predictTrends(Long repositoryId, int historicalMonths, int predictionMonths) {
        log.info("Predicting trends for repository {} using {} months of history to predict {} months ahead", 
                repositoryId, historicalMonths, predictionMonths);

        LocalDate fromDate = LocalDate.now().minusMonths(historicalMonths);
        List<CodeQualityTrends> historicalTrends = codeQualityTrendsRepository
            .findByRepositoryIdAndAnalysisDateAfter(repositoryId, fromDate);

        Map<String, List<CodeQualityTrends>> trendsByMetric = historicalTrends.stream()
            .collect(Collectors.groupingBy(CodeQualityTrends::getMetricName));

        Map<String, TrendPrediction> predictions = new HashMap<>();
        for (Map.Entry<String, List<CodeQualityTrends>> entry : trendsByMetric.entrySet()) {
            String metricName = entry.getKey();
            List<CodeQualityTrends> metricTrends = entry.getValue();
            
            // Sort by date
            metricTrends.sort(Comparator.comparing(CodeQualityTrends::getAnalysisDate));
            
            predictions.put(metricName, predictMetricTrend(metricTrends, predictionMonths));
        }

        return TrendPredictionReport.builder()
            .repositoryId(repositoryId)
            .historicalDateRange(fromDate)
            .predictionMonths(predictionMonths)
            .predictions(predictions)
            .confidenceScore(calculatePredictionConfidence(predictions))
            .riskFactors(identifyRiskFactors(predictions))
            .actionableInsights(generateActionableInsights(predictions))
            .build();
    }

    /**
     * Get team performance trends
     */
    @Transactional(readOnly = true)
    public TeamPerformanceTrends getTeamPerformanceTrends(Long repositoryId, int months) {
        log.info("Analyzing team performance trends for repository {} over {} months", repositoryId, months);

        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        List<PRHistoricalMetrics> metrics = prHistoricalMetricsRepository
            .findByRepositoryIdAndCreatedAtAfter(repositoryId, fromDate);

        Map<Long, List<PRHistoricalMetrics>> metricsByDeveloper = metrics.stream()
            .collect(Collectors.groupingBy(PRHistoricalMetrics::getPrAuthorId));

        Map<Long, DeveloperPerformanceTrend> developerTrends = new HashMap<>();
        for (Map.Entry<Long, List<PRHistoricalMetrics>> entry : metricsByDeveloper.entrySet()) {
            Long developerId = entry.getKey();
            List<PRHistoricalMetrics> developerMetrics = entry.getValue();
            
            developerTrends.put(developerId, analyzeDeveloperPerformanceTrend(developerId, developerMetrics));
        }

        return TeamPerformanceTrends.builder()
            .repositoryId(repositoryId)
            .analysisDateRange(fromDate)
            .developerTrends(developerTrends)
            .teamAverages(calculateTeamAverages(metrics))
            .collaborationMetrics(calculateCollaborationMetrics(metrics))
            .productivityTrends(calculateProductivityTrends(metrics))
            .qualityTrends(calculateQualityTrends(metrics))
            .build();
    }

    /**
     * Scheduled task to calculate daily trends
     */
    @Scheduled(cron = "0 30 1 * * ?") // Run daily at 1:30 AM
    public void calculateDailyTrends() {
        log.info("Starting scheduled daily trend calculation");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Long> repositoryIds = prHistoricalMetricsRepository.findDistinctRepositoryIds();

        for (Long repositoryId : repositoryIds) {
            try {
                calculateCodeQualityTrends(repositoryId, yesterday, PeriodType.DAILY);
            } catch (Exception e) {
                log.error("Error calculating daily trends for repository {}: {}", 
                         repositoryId, e.getMessage(), e);
            }
        }

        log.info("Completed scheduled daily trend calculation for {} repositories", repositoryIds.size());
    }

    /**
     * Scheduled task to calculate weekly trends
     */
    @Scheduled(cron = "0 0 2 * * MON") // Run every Monday at 2 AM
    public void calculateWeeklyTrends() {
        log.info("Starting scheduled weekly trend calculation");

        LocalDate lastWeek = LocalDate.now().minusWeeks(1);
        List<Long> repositoryIds = prHistoricalMetricsRepository.findDistinctRepositoryIds();

        for (Long repositoryId : repositoryIds) {
            try {
                calculateCodeQualityTrends(repositoryId, lastWeek, PeriodType.WEEKLY);
            } catch (Exception e) {
                log.error("Error calculating weekly trends for repository {}: {}", 
                         repositoryId, e.getMessage(), e);
            }
        }

        log.info("Completed scheduled weekly trend calculation for {} repositories", repositoryIds.size());
    }

    /**
     * Scheduled task to calculate monthly trends
     */
    @Scheduled(cron = "0 0 3 1 * ?") // Run on the 1st of each month at 3 AM
    public void calculateMonthlyTrends() {
        log.info("Starting scheduled monthly trend calculation");

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        List<Long> repositoryIds = prHistoricalMetricsRepository.findDistinctRepositoryIds();

        for (Long repositoryId : repositoryIds) {
            try {
                calculateCodeQualityTrends(repositoryId, lastMonth, PeriodType.MONTHLY);
            } catch (Exception e) {
                log.error("Error calculating monthly trends for repository {}: {}", 
                         repositoryId, e.getMessage(), e);
            }
        }

        log.info("Completed scheduled monthly trend calculation for {} repositories", repositoryIds.size());
    }

    // Private helper methods

    private Map<String, TrendMetric> calculateTrendMetrics(List<PRHistoricalMetrics> metrics) {
        Map<String, TrendMetric> trendMetrics = new HashMap<>();

        // Calculate average complexity
        BigDecimal avgComplexity = metrics.stream()
            .filter(m -> m.getComplexityScore() != null)
            .map(PRHistoricalMetrics::getComplexityScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);

        trendMetrics.put("average_complexity", TrendMetric.builder()
            .value(avgComplexity)
            .trendDirection(calculateTrendDirection(avgComplexity, getHistoricalAverage("average_complexity")))
            .changePercentage(calculateChangePercentage(avgComplexity, getHistoricalAverage("average_complexity")))
            .developerCount(getDeveloperCount(metrics))
            .issueCount(getIssueCount(metrics))
            .baselineValue(getBaselineValue("average_complexity"))
            .targetValue(getTargetValue("average_complexity"))
            .build());

        // Calculate average code quality
        BigDecimal avgQuality = metrics.stream()
            .filter(m -> m.getCodeQualityScore() != null)
            .map(PRHistoricalMetrics::getCodeQualityScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);

        trendMetrics.put("average_quality", TrendMetric.builder()
            .value(avgQuality)
            .trendDirection(calculateTrendDirection(avgQuality, getHistoricalAverage("average_quality")))
            .changePercentage(calculateChangePercentage(avgQuality, getHistoricalAverage("average_quality")))
            .developerCount(getDeveloperCount(metrics))
            .issueCount(getIssueCount(metrics))
            .baselineValue(getBaselineValue("average_quality"))
            .targetValue(getTargetValue("average_quality"))
            .build());

        // Calculate bug rate
        long bugCount = metrics.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsBugfix()))
            .count();
        
        BigDecimal bugRate = BigDecimal.valueOf(bugCount)
            .divide(BigDecimal.valueOf(metrics.size()), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        trendMetrics.put("bug_rate", TrendMetric.builder()
            .value(bugRate)
            .trendDirection(calculateTrendDirection(bugRate, getHistoricalAverage("bug_rate")))
            .changePercentage(calculateChangePercentage(bugRate, getHistoricalAverage("bug_rate")))
            .developerCount(getDeveloperCount(metrics))
            .issueCount(getIssueCount(metrics))
            .baselineValue(getBaselineValue("bug_rate"))
            .targetValue(getTargetValue("bug_rate"))
            .build());

        // Add more metrics as needed...

        return trendMetrics;
    }

    private LocalDateTime calculatePeriodStart(LocalDate analysisDate, PeriodType periodType) {
        switch (periodType) {
            case DAILY:
                return analysisDate.atStartOfDay();
            case WEEKLY:
                return analysisDate.minusDays(6).atStartOfDay();
            case MONTHLY:
                return analysisDate.withDayOfMonth(1).atStartOfDay();
            default:
                throw new IllegalArgumentException("Unknown period type: " + periodType);
        }
    }

    private LocalDateTime calculatePeriodEnd(LocalDate analysisDate, PeriodType periodType) {
        switch (periodType) {
            case DAILY:
                return analysisDate.atTime(23, 59, 59);
            case WEEKLY:
                return analysisDate.atTime(23, 59, 59);
            case MONTHLY:
                return analysisDate.withDayOfMonth(analysisDate.lengthOfMonth()).atTime(23, 59, 59);
            default:
                throw new IllegalArgumentException("Unknown period type: " + periodType);
        }
    }

    // Additional helper methods would be implemented here...

    // Data classes for the service
    @Data
    @Builder
    public static class TrendAnalysisReport {
        private Long repositoryId;
        private LocalDate analysisDateRange;
        private Map<String, MetricTrendAnalysis> trends;
        private BigDecimal overallHealthScore;
        private List<TrendInsight> insights;
        private List<String> recommendations;
        private Map<String, Object> keyMetrics;
        private List<String> alertingMetrics;
    }

    @Data
    @Builder
    public static class TrendComparisonReport {
        private Long repositoryId;
        private LocalDate period1Start;
        private LocalDate period1End;
        private LocalDate period2Start;
        private LocalDate period2End;
        private Map<String, TrendComparison> comparisons;
        private List<String> significantChanges;
        private List<String> improvements;
        private List<String> regressions;
    }

    @Data
    @Builder
    public static class TrendPredictionReport {
        private Long repositoryId;
        private LocalDate historicalDateRange;
        private Integer predictionMonths;
        private Map<String, TrendPrediction> predictions;
        private BigDecimal confidenceScore;
        private List<String> riskFactors;
        private List<String> actionableInsights;
    }

    @Data
    @Builder
    public static class TeamPerformanceTrends {
        private Long repositoryId;
        private LocalDateTime analysisDateRange;
        private Map<Long, DeveloperPerformanceTrend> developerTrends;
        private Map<String, Object> teamAverages;
        private Map<String, Object> collaborationMetrics;
        private Map<String, Object> productivityTrends;
        private Map<String, Object> qualityTrends;
    }

    @Data
    @Builder
    public static class TrendMetric {
        private BigDecimal value;
        private String trendDirection;
        private BigDecimal changePercentage;
        private Integer developerCount;
        private Integer issueCount;
        private BigDecimal baselineValue;
        private BigDecimal targetValue;
    }

    @Data
    @Builder
    public static class MetricTrendAnalysis {
        private String metricName;
        private String currentTrend;
        private BigDecimal currentValue;
        private BigDecimal changeFromPrevious;
        private List<TrendDataPoint> dataPoints;
        private BigDecimal volatility;
        private String outlook;
    }

    @Data
    @Builder
    public static class TrendInsight {
        private String title;
        private String description;
        private String severity;
        private String category;
        private List<String> affectedMetrics;
        private String recommendation;
    }

    @Data
    @Builder
    public static class TrendComparison {
        private String metricName;
        private BigDecimal period1Average;
        private BigDecimal period2Average;
        private BigDecimal changePercentage;
        private String changeDirection;
        private String significance;
    }

    @Data
    @Builder
    public static class TrendPrediction {
        private String metricName;
        private List<PredictedDataPoint> predictedValues;
        private BigDecimal confidence;
        private String trendDirection;
        private List<String> assumptions;
    }

    @Data
    @Builder
    public static class DeveloperPerformanceTrend {
        private Long developerId;
        private String trendDirection;
        private Map<String, Object> metrics;
        private List<String> strengths;
        private List<String> improvements;
    }

    @Data
    @Builder
    public static class TrendDataPoint {
        private LocalDate date;
        private BigDecimal value;
        private String trend;
    }

    @Data
    @Builder
    public static class PredictedDataPoint {
        private LocalDate date;
        private BigDecimal predictedValue;
        private BigDecimal confidence;
    }

    public enum PeriodType {
        DAILY("daily"),
        WEEKLY("weekly"),
        MONTHLY("monthly");

        private final String value;

        PeriodType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}