package com.zamaz.mcp.pattern.reporting;

import com.zamaz.mcp.pattern.core.PatternCategory;
import com.zamaz.mcp.pattern.core.PatternSeverity;
import com.zamaz.mcp.pattern.core.PatternType;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import com.zamaz.mcp.pattern.model.PatternReport;
import com.zamaz.mcp.pattern.model.ComprehensiveRecommendations;
import com.zamaz.mcp.pattern.model.QualityMetrics;
import com.zamaz.mcp.pattern.model.TrendAnalysis;
import com.zamaz.mcp.pattern.repository.PatternReportRepository;
import com.zamaz.mcp.pattern.repository.PatternDetectionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating comprehensive pattern analysis reports and visualizations.
 * 
 * This service provides:
 * - Executive summaries
 * - Detailed technical reports
 * - Trend analysis
 * - Team performance metrics
 * - Visual dashboards
 * - Export capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternReportingService {
    
    private final PatternReportRepository reportRepository;
    private final PatternDetectionResultRepository resultRepository;
    private final ReportGenerator reportGenerator;
    private final ChartGenerator chartGenerator;
    private final TrendAnalyzer trendAnalyzer;
    private final MetricsCalculator metricsCalculator;
    
    /**
     * Generate a comprehensive pattern analysis report.
     * 
     * @param organizationId Organization identifier
     * @param analysisResults Pattern detection results
     * @param recommendations Generated recommendations
     * @return Comprehensive pattern report
     */
    @Transactional
    public PatternReport generateComprehensiveReport(String organizationId,
                                                   List<PatternDetectionResult> analysisResults,
                                                   ComprehensiveRecommendations recommendations) {
        
        log.info("Generating comprehensive report for organization: {}", organizationId);
        
        try {
            // Generate executive summary
            ExecutiveSummary executiveSummary = generateExecutiveSummary(analysisResults, recommendations);
            
            // Generate technical analysis
            TechnicalAnalysis technicalAnalysis = generateTechnicalAnalysis(analysisResults);
            
            // Generate trend analysis
            TrendAnalysis trendAnalysis = generateTrendAnalysis(organizationId, analysisResults);
            
            // Generate quality metrics
            QualityMetrics qualityMetrics = generateQualityMetrics(analysisResults);
            
            // Generate team insights
            TeamInsights teamInsights = generateTeamInsights(organizationId, analysisResults);
            
            // Generate visualizations
            List<ReportVisualization> visualizations = generateVisualizations(analysisResults);
            
            // Generate recommendations summary
            RecommendationsSummary recommendationsSummary = generateRecommendationsSummary(recommendations);
            
            // Create the comprehensive report
            PatternReport report = PatternReport.builder()
                    .organizationId(organizationId)
                    .reportId(UUID.randomUUID().toString())
                    .generatedAt(LocalDateTime.now())
                    .reportType(ReportType.COMPREHENSIVE)
                    .executiveSummary(executiveSummary)
                    .technicalAnalysis(technicalAnalysis)
                    .trendAnalysis(trendAnalysis)
                    .qualityMetrics(qualityMetrics)
                    .teamInsights(teamInsights)
                    .visualizations(visualizations)
                    .recommendationsSummary(recommendationsSummary)
                    .metadata(createReportMetadata(analysisResults))
                    .build();
            
            // Save the report
            PatternReport savedReport = reportRepository.save(report);
            
            log.info("Comprehensive report generated successfully with ID: {}", savedReport.getReportId());
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("Error generating comprehensive report for organization: {}", organizationId, e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    /**
     * Generate an executive summary report.
     * 
     * @param organizationId Organization identifier
     * @param analysisResults Pattern detection results
     * @return Executive summary report
     */
    public ExecutiveSummaryReport generateExecutiveSummaryReport(String organizationId,
                                                               List<PatternDetectionResult> analysisResults) {
        
        log.info("Generating executive summary report for organization: {}", organizationId);
        
        try {
            // Calculate key metrics
            long totalIssues = analysisResults.stream()
                    .filter(result -> result.getPatternCategory() == PatternCategory.CODE_SMELL ||
                                     result.getPatternCategory() == PatternCategory.ANTI_PATTERN)
                    .count();
            
            long criticalIssues = analysisResults.stream()
                    .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                    .count();
            
            double qualityScore = calculateOverallQualityScore(analysisResults);
            
            // Calculate trends
            QualityTrend qualityTrend = calculateQualityTrend(organizationId);
            
            // Generate key insights
            List<String> keyInsights = generateKeyInsights(analysisResults);
            
            // Generate recommendations
            List<String> topRecommendations = generateTopRecommendations(analysisResults);
            
            // Calculate business impact
            BusinessImpact businessImpact = calculateBusinessImpact(analysisResults);
            
            return ExecutiveSummaryReport.builder()
                    .organizationId(organizationId)
                    .reportDate(LocalDateTime.now())
                    .totalIssues(totalIssues)
                    .criticalIssues(criticalIssues)
                    .qualityScore(qualityScore)
                    .qualityTrend(qualityTrend)
                    .keyInsights(keyInsights)
                    .topRecommendations(topRecommendations)
                    .businessImpact(businessImpact)
                    .build();
            
        } catch (Exception e) {
            log.error("Error generating executive summary for organization: {}", organizationId, e);
            throw new RuntimeException("Executive summary generation failed", e);
        }
    }
    
    /**
     * Generate a technical detailed report.
     * 
     * @param organizationId Organization identifier
     * @param analysisResults Pattern detection results
     * @return Technical detailed report
     */
    public TechnicalDetailedReport generateTechnicalReport(String organizationId,
                                                         List<PatternDetectionResult> analysisResults) {
        
        log.info("Generating technical detailed report for organization: {}", organizationId);
        
        try {
            // Group results by category
            Map<PatternCategory, List<PatternDetectionResult>> resultsByCategory =
                    analysisResults.stream()
                            .collect(Collectors.groupingBy(PatternDetectionResult::getPatternCategory));
            
            // Generate detailed analysis for each category
            List<CategoryAnalysis> categoryAnalyses = resultsByCategory.entrySet().stream()
                    .map(entry -> generateCategoryAnalysis(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            
            // Generate file-by-file analysis
            Map<String, List<PatternDetectionResult>> resultsByFile =
                    analysisResults.stream()
                            .collect(Collectors.groupingBy(result -> result.getFilePath().toString()));
            
            List<FileAnalysis> fileAnalyses = resultsByFile.entrySet().stream()
                    .map(entry -> generateFileAnalysis(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            
            // Generate metrics breakdown
            MetricsBreakdown metricsBreakdown = generateMetricsBreakdown(analysisResults);
            
            // Generate pattern distribution
            PatternDistribution patternDistribution = generatePatternDistribution(analysisResults);
            
            return TechnicalDetailedReport.builder()
                    .organizationId(organizationId)
                    .reportDate(LocalDateTime.now())
                    .categoryAnalyses(categoryAnalyses)
                    .fileAnalyses(fileAnalyses)
                    .metricsBreakdown(metricsBreakdown)
                    .patternDistribution(patternDistribution)
                    .build();
            
        } catch (Exception e) {
            log.error("Error generating technical report for organization: {}", organizationId, e);
            throw new RuntimeException("Technical report generation failed", e);
        }
    }
    
    /**
     * Generate a trend analysis report.
     * 
     * @param organizationId Organization identifier
     * @param timeRange Time range for analysis
     * @return Trend analysis report
     */
    public TrendAnalysisReport generateTrendAnalysisReport(String organizationId,
                                                         TimeRange timeRange) {
        
        log.info("Generating trend analysis report for organization: {} over {}", 
                 organizationId, timeRange);
        
        try {
            // Get historical data
            List<PatternDetectionResult> historicalResults = 
                    resultRepository.findByOrganizationIdAndCreatedAtBetween(
                            organizationId, 
                            timeRange.getStartDate(), 
                            timeRange.getEndDate());
            
            // Calculate trends
            List<QualityTrend> qualityTrends = trendAnalyzer.calculateQualityTrends(historicalResults, timeRange);
            List<PatternTrend> patternTrends = trendAnalyzer.calculatePatternTrends(historicalResults, timeRange);
            List<TeamTrend> teamTrends = trendAnalyzer.calculateTeamTrends(organizationId, historicalResults, timeRange);
            
            // Generate forecasting
            QualityForecast forecast = generateQualityForecast(qualityTrends);
            
            // Generate insights
            List<TrendInsight> insights = generateTrendInsights(qualityTrends, patternTrends);
            
            return TrendAnalysisReport.builder()
                    .organizationId(organizationId)
                    .timeRange(timeRange)
                    .reportDate(LocalDateTime.now())
                    .qualityTrends(qualityTrends)
                    .patternTrends(patternTrends)
                    .teamTrends(teamTrends)
                    .forecast(forecast)
                    .insights(insights)
                    .build();
            
        } catch (Exception e) {
            log.error("Error generating trend analysis for organization: {}", organizationId, e);
            throw new RuntimeException("Trend analysis generation failed", e);
        }
    }
    
    /**
     * Generate dashboard data for real-time visualization.
     * 
     * @param organizationId Organization identifier
     * @return Dashboard data
     */
    public DashboardData generateDashboardData(String organizationId) {
        
        log.info("Generating dashboard data for organization: {}", organizationId);
        
        try {
            // Get recent results
            List<PatternDetectionResult> recentResults = resultRepository
                    .findByOrganizationIdAndCreatedAtAfter(organizationId, 
                            LocalDateTime.now().minusDays(30));
            
            // Calculate current metrics
            DashboardMetrics currentMetrics = calculateDashboardMetrics(recentResults);
            
            // Generate charts
            List<ChartData> charts = generateDashboardCharts(recentResults);
            
            // Generate alerts
            List<DashboardAlert> alerts = generateDashboardAlerts(recentResults);
            
            // Generate key performance indicators
            List<KeyPerformanceIndicator> kpis = generateKPIs(organizationId, recentResults);
            
            // Generate activity feed
            List<ActivityFeedItem> activityFeed = generateActivityFeed(recentResults);
            
            return DashboardData.builder()
                    .organizationId(organizationId)
                    .lastUpdated(LocalDateTime.now())
                    .currentMetrics(currentMetrics)
                    .charts(charts)
                    .alerts(alerts)
                    .kpis(kpis)
                    .activityFeed(activityFeed)
                    .build();
            
        } catch (Exception e) {
            log.error("Error generating dashboard data for organization: {}", organizationId, e);
            throw new RuntimeException("Dashboard data generation failed", e);
        }
    }
    
    /**
     * Export a report in the specified format.
     * 
     * @param reportId Report identifier
     * @param format Export format (PDF, HTML, JSON, CSV)
     * @return Exported report data
     */
    public ExportedReport exportReport(String reportId, ExportFormat format) {
        
        log.info("Exporting report {} in format {}", reportId, format);
        
        try {
            // Get the report
            PatternReport report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            
            // Generate export based on format
            byte[] exportData = switch (format) {
                case PDF -> generatePdfReport(report);
                case HTML -> generateHtmlReport(report);
                case JSON -> generateJsonReport(report);
                case CSV -> generateCsvReport(report);
                case EXCEL -> generateExcelReport(report);
            };
            
            return ExportedReport.builder()
                    .reportId(reportId)
                    .format(format)
                    .data(exportData)
                    .filename(generateFilename(report, format))
                    .contentType(getContentType(format))
                    .exportedAt(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("Error exporting report {} in format {}", reportId, format, e);
            throw new RuntimeException("Report export failed", e);
        }
    }
    
    // Private helper methods
    
    private ExecutiveSummary generateExecutiveSummary(List<PatternDetectionResult> results,
                                                     ComprehensiveRecommendations recommendations) {
        
        long totalIssues = results.stream()
                .filter(result -> result.getPatternCategory() == PatternCategory.CODE_SMELL ||
                                 result.getPatternCategory() == PatternCategory.ANTI_PATTERN)
                .count();
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        double qualityScore = calculateOverallQualityScore(results);
        
        return ExecutiveSummary.builder()
                .totalIssues(totalIssues)
                .criticalIssues(criticalIssues)
                .qualityScore(qualityScore)
                .keyFindings(generateKeyFindings(results))
                .recommendationCount(recommendations.getRefactoringPlans().size() + 
                                   recommendations.getImprovementSuggestions().size())
                .estimatedImprovementTime(recommendations.getActionPlan().getEstimatedDuration())
                .build();
    }
    
    private TechnicalAnalysis generateTechnicalAnalysis(List<PatternDetectionResult> results) {
        
        Map<PatternCategory, Long> patternsByCategory = results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getPatternCategory, 
                        Collectors.counting()));
        
        Map<PatternSeverity, Long> patternsBySeverity = results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getSeverity, 
                        Collectors.counting()));
        
        return TechnicalAnalysis.builder()
                .totalPatterns(results.size())
                .patternsByCategory(patternsByCategory)
                .patternsBySeverity(patternsBySeverity)
                .averageConfidence(results.stream()
                        .mapToDouble(PatternDetectionResult::getConfidence)
                        .average()
                        .orElse(0.0))
                .mostCommonPatterns(findMostCommonPatterns(results))
                .build();
    }
    
    private TrendAnalysis generateTrendAnalysis(String organizationId, List<PatternDetectionResult> results) {
        // For now, return empty trends - would need historical data
        return TrendAnalysis.builder()
                .organizationId(organizationId)
                .timeRange(TimeRange.LAST_30_DAYS)
                .qualityTrend(QualityTrend.STABLE)
                .trendData(new ArrayList<>())
                .build();
    }
    
    private QualityMetrics generateQualityMetrics(List<PatternDetectionResult> results) {
        return metricsCalculator.calculateQualityMetrics(results);
    }
    
    private TeamInsights generateTeamInsights(String organizationId, List<PatternDetectionResult> results) {
        // Generate insights about team coding patterns
        return TeamInsights.builder()
                .organizationId(organizationId)
                .teamSize(estimateTeamSize(results))
                .codingPatterns(identifyTeamCodingPatterns(results))
                .strengths(identifyTeamStrengths(results))
                .improvementAreas(identifyImprovementAreas(results))
                .build();
    }
    
    private List<ReportVisualization> generateVisualizations(List<PatternDetectionResult> results) {
        List<ReportVisualization> visualizations = new ArrayList<>();
        
        // Pattern distribution pie chart
        visualizations.add(chartGenerator.generatePatternDistributionChart(results));
        
        // Severity distribution bar chart
        visualizations.add(chartGenerator.generateSeverityDistributionChart(results));
        
        // Quality metrics radar chart
        visualizations.add(chartGenerator.generateQualityRadarChart(results));
        
        // File complexity heatmap
        visualizations.add(chartGenerator.generateFileComplexityHeatmap(results));
        
        return visualizations;
    }
    
    private RecommendationsSummary generateRecommendationsSummary(ComprehensiveRecommendations recommendations) {
        return RecommendationsSummary.builder()
                .totalRecommendations(recommendations.getRefactoringPlans().size() + 
                                    recommendations.getImprovementSuggestions().size())
                .highPriorityCount(countHighPriorityRecommendations(recommendations))
                .estimatedEffort(recommendations.getActionPlan().getEstimatedDuration())
                .expectedImpact(calculateExpectedImpact(recommendations))
                .build();
    }
    
    private Map<String, Object> createReportMetadata(List<PatternDetectionResult> results) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("totalResults", results.size());
        metadata.put("analysisDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.put("fileCount", results.stream()
                .map(result -> result.getFilePath().toString())
                .distinct()
                .count());
        metadata.put("averageConfidence", results.stream()
                .mapToDouble(PatternDetectionResult::getConfidence)
                .average()
                .orElse(0.0));
        
        return metadata;
    }
    
    private double calculateOverallQualityScore(List<PatternDetectionResult> results) {
        if (results.isEmpty()) return 100.0;
        
        double score = 100.0;
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        long highSeverityIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        long mediumSeverityIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.MEDIUM)
                .count();
        
        score -= criticalIssues * 15;
        score -= highSeverityIssues * 10;
        score -= mediumSeverityIssues * 5;
        
        return Math.max(0, score);
    }
    
    private QualityTrend calculateQualityTrend(String organizationId) {
        // Would need historical data to calculate actual trends
        return QualityTrend.STABLE;
    }
    
    private List<String> generateKeyInsights(List<PatternDetectionResult> results) {
        List<String> insights = new ArrayList<>();
        
        // Most common pattern type
        Map<PatternType, Long> patternCounts = results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getPatternType, 
                        Collectors.counting()));
        
        PatternType mostCommon = patternCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        if (mostCommon != null) {
            insights.add("Most common pattern: " + mostCommon.name());
        }
        
        // Quality assessment
        double qualityScore = calculateOverallQualityScore(results);
        if (qualityScore >= 80) {
            insights.add("Code quality is generally good");
        } else if (qualityScore >= 60) {
            insights.add("Code quality needs improvement");
        } else {
            insights.add("Code quality requires immediate attention");
        }
        
        // Critical issues
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        if (criticalIssues > 0) {
            insights.add(criticalIssues + " critical issues require immediate attention");
        }
        
        return insights;
    }
    
    private List<String> generateTopRecommendations(List<PatternDetectionResult> results) {
        return results.stream()
                .filter(result -> result.getSeverity().isAtLeast(PatternSeverity.HIGH))
                .limit(5)
                .map(result -> result.getSuggestions().isEmpty() ? 
                        "Address " + result.getPatternType().name() : 
                        result.getSuggestions().get(0))
                .collect(Collectors.toList());
    }
    
    private BusinessImpact calculateBusinessImpact(List<PatternDetectionResult> results) {
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        // Simple impact calculation - would be more sophisticated in practice
        if (criticalIssues > 10) {
            return BusinessImpact.HIGH;
        } else if (criticalIssues > 5) {
            return BusinessImpact.MEDIUM;
        } else {
            return BusinessImpact.LOW;
        }
    }
    
    private List<String> generateKeyFindings(List<PatternDetectionResult> results) {
        List<String> findings = new ArrayList<>();
        
        // Add key findings based on analysis
        findings.add("Total patterns detected: " + results.size());
        
        long negativePatterns = results.stream()
                .filter(PatternDetectionResult::isNegativePattern)
                .count();
        
        if (negativePatterns > 0) {
            findings.add("Negative patterns requiring attention: " + negativePatterns);
        }
        
        return findings;
    }
    
    private List<PatternType> findMostCommonPatterns(List<PatternDetectionResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getPatternType, 
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<PatternType, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private CategoryAnalysis generateCategoryAnalysis(PatternCategory category, 
                                                     List<PatternDetectionResult> results) {
        return CategoryAnalysis.builder()
                .category(category)
                .totalCount(results.size())
                .severityDistribution(results.stream()
                        .collect(Collectors.groupingBy(PatternDetectionResult::getSeverity, 
                                Collectors.counting())))
                .averageConfidence(results.stream()
                        .mapToDouble(PatternDetectionResult::getConfidence)
                        .average()
                        .orElse(0.0))
                .topPatterns(results.stream()
                        .collect(Collectors.groupingBy(PatternDetectionResult::getPatternType, 
                                Collectors.counting()))
                        .entrySet().stream()
                        .sorted(Map.Entry.<PatternType, Long>comparingByValue().reversed())
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private FileAnalysis generateFileAnalysis(String filePath, List<PatternDetectionResult> results) {
        return FileAnalysis.builder()
                .filePath(filePath)
                .totalPatterns(results.size())
                .criticalIssues(results.stream()
                        .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                        .count())
                .qualityScore(calculateFileQualityScore(results))
                .patternTypes(results.stream()
                        .map(PatternDetectionResult::getPatternType)
                        .collect(Collectors.toSet()))
                .build();
    }
    
    private double calculateFileQualityScore(List<PatternDetectionResult> results) {
        // Calculate quality score for a specific file
        return calculateOverallQualityScore(results);
    }
    
    private MetricsBreakdown generateMetricsBreakdown(List<PatternDetectionResult> results) {
        return MetricsBreakdown.builder()
                .totalPatterns(results.size())
                .positivePatterns(results.stream()
                        .filter(PatternDetectionResult::isPositivePattern)
                        .count())
                .negativePatterns(results.stream()
                        .filter(PatternDetectionResult::isNegativePattern)
                        .count())
                .averageConfidence(results.stream()
                        .mapToDouble(PatternDetectionResult::getConfidence)
                        .average()
                        .orElse(0.0))
                .build();
    }
    
    private PatternDistribution generatePatternDistribution(List<PatternDetectionResult> results) {
        Map<PatternType, Long> distribution = results.stream()
                .collect(Collectors.groupingBy(PatternDetectionResult::getPatternType, 
                        Collectors.counting()));
        
        return PatternDistribution.builder()
                .distribution(distribution)
                .totalPatterns(results.size())
                .uniquePatterns(distribution.size())
                .build();
    }
    
    private QualityForecast generateQualityForecast(List<QualityTrend> trends) {
        // Generate forecast based on trends
        return QualityForecast.builder()
                .forecastType(ForecastType.LINEAR)
                .timeframe(30) // 30 days
                .predictedQualityScore(75.0) // Placeholder
                .confidence(0.7)
                .build();
    }
    
    private List<TrendInsight> generateTrendInsights(List<QualityTrend> qualityTrends, 
                                                    List<PatternTrend> patternTrends) {
        List<TrendInsight> insights = new ArrayList<>();
        
        // Add insights based on trends
        insights.add(TrendInsight.builder()
                .type(InsightType.QUALITY_TREND)
                .message("Quality trend analysis shows stable patterns")
                .severity(PatternSeverity.LOW)
                .build());
        
        return insights;
    }
    
    private DashboardMetrics calculateDashboardMetrics(List<PatternDetectionResult> results) {
        return DashboardMetrics.builder()
                .totalPatterns(results.size())
                .criticalIssues(results.stream()
                        .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                        .count())
                .qualityScore(calculateOverallQualityScore(results))
                .trendsDirection(TrendDirection.STABLE)
                .build();
    }
    
    private List<ChartData> generateDashboardCharts(List<PatternDetectionResult> results) {
        List<ChartData> charts = new ArrayList<>();
        
        // Pattern distribution chart
        charts.add(chartGenerator.generatePatternDistributionChart(results));
        
        // Severity trend chart
        charts.add(chartGenerator.generateSeverityTrendChart(results));
        
        return charts;
    }
    
    private List<DashboardAlert> generateDashboardAlerts(List<PatternDetectionResult> results) {
        List<DashboardAlert> alerts = new ArrayList<>();
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        if (criticalIssues > 0) {
            alerts.add(DashboardAlert.builder()
                    .type(AlertType.CRITICAL_ISSUES)
                    .message(criticalIssues + " critical issues require immediate attention")
                    .severity(PatternSeverity.CRITICAL)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
        
        return alerts;
    }
    
    private List<KeyPerformanceIndicator> generateKPIs(String organizationId, 
                                                      List<PatternDetectionResult> results) {
        List<KeyPerformanceIndicator> kpis = new ArrayList<>();
        
        // Code quality KPI
        kpis.add(KeyPerformanceIndicator.builder()
                .name("Code Quality Score")
                .currentValue(calculateOverallQualityScore(results))
                .targetValue(85.0)
                .trend(TrendDirection.STABLE)
                .unit("Score")
                .build());
        
        // Critical issues KPI
        kpis.add(KeyPerformanceIndicator.builder()
                .name("Critical Issues")
                .currentValue(results.stream()
                        .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                        .count())
                .targetValue(0.0)
                .trend(TrendDirection.STABLE)
                .unit("Count")
                .build());
        
        return kpis;
    }
    
    private List<ActivityFeedItem> generateActivityFeed(List<PatternDetectionResult> results) {
        return results.stream()
                .sorted(Comparator.comparing(PatternDetectionResult::getDetectedAt).reversed())
                .limit(10)
                .map(result -> ActivityFeedItem.builder()
                        .type(ActivityType.PATTERN_DETECTED)
                        .message("Pattern detected: " + result.getPatternType().name())
                        .timestamp(result.getDetectedAt())
                        .severity(result.getSeverity())
                        .build())
                .collect(Collectors.toList());
    }
    
    private byte[] generatePdfReport(PatternReport report) {
        // Generate PDF report - would use a PDF library
        return "PDF Report".getBytes();
    }
    
    private byte[] generateHtmlReport(PatternReport report) {
        // Generate HTML report
        return reportGenerator.generateHtmlReport(report).getBytes();
    }
    
    private byte[] generateJsonReport(PatternReport report) {
        // Generate JSON report
        return reportGenerator.generateJsonReport(report).getBytes();
    }
    
    private byte[] generateCsvReport(PatternReport report) {
        // Generate CSV report
        return reportGenerator.generateCsvReport(report).getBytes();
    }
    
    private byte[] generateExcelReport(PatternReport report) {
        // Generate Excel report
        return reportGenerator.generateExcelReport(report);
    }
    
    private String generateFilename(PatternReport report, ExportFormat format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("pattern_report_%s_%s.%s", 
                report.getOrganizationId(), timestamp, format.name().toLowerCase());
    }
    
    private String getContentType(ExportFormat format) {
        return switch (format) {
            case PDF -> "application/pdf";
            case HTML -> "text/html";
            case JSON -> "application/json";
            case CSV -> "text/csv";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }
    
    private int estimateTeamSize(List<PatternDetectionResult> results) {
        // Estimate team size based on code patterns - placeholder
        return 5;
    }
    
    private List<String> identifyTeamCodingPatterns(List<PatternDetectionResult> results) {
        return results.stream()
                .filter(PatternDetectionResult::isPositivePattern)
                .map(result -> result.getPatternType().name())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private List<String> identifyTeamStrengths(List<PatternDetectionResult> results) {
        List<String> strengths = new ArrayList<>();
        
        long positivePatterns = results.stream()
                .filter(PatternDetectionResult::isPositivePattern)
                .count();
        
        if (positivePatterns > results.size() * 0.5) {
            strengths.add("Good use of design patterns");
        }
        
        return strengths;
    }
    
    private List<String> identifyImprovementAreas(List<PatternDetectionResult> results) {
        List<String> areas = new ArrayList<>();
        
        long codeSmells = results.stream()
                .filter(result -> result.getPatternCategory() == PatternCategory.CODE_SMELL)
                .count();
        
        if (codeSmells > results.size() * 0.3) {
            areas.add("Code smell reduction");
        }
        
        return areas;
    }
    
    private long countHighPriorityRecommendations(ComprehensiveRecommendations recommendations) {
        return recommendations.getActionPlan().getActions().stream()
                .filter(action -> action.getPriority() == PatternPriority.HIGH ||
                                 action.getPriority() == PatternPriority.CRITICAL)
                .count();
    }
    
    private double calculateExpectedImpact(ComprehensiveRecommendations recommendations) {
        // Calculate expected impact based on recommendations
        return 0.7; // Placeholder
    }
    
    // Enums for report types
    public enum ReportType {
        COMPREHENSIVE, EXECUTIVE_SUMMARY, TECHNICAL_DETAILED, TREND_ANALYSIS
    }
    
    public enum ExportFormat {
        PDF, HTML, JSON, CSV, EXCEL
    }
    
    public enum QualityTrend {
        IMPROVING, STABLE, DECLINING
    }
    
    public enum BusinessImpact {
        LOW, MEDIUM, HIGH
    }
    
    public enum TrendDirection {
        UP, DOWN, STABLE
    }
    
    public enum AlertType {
        CRITICAL_ISSUES, QUALITY_DEGRADATION, PERFORMANCE_ISSUE
    }
    
    public enum ActivityType {
        PATTERN_DETECTED, ISSUE_RESOLVED, REPORT_GENERATED
    }
    
    public enum ForecastType {
        LINEAR, EXPONENTIAL, SEASONAL
    }
    
    public enum InsightType {
        QUALITY_TREND, PATTERN_TREND, TEAM_PERFORMANCE
    }
}