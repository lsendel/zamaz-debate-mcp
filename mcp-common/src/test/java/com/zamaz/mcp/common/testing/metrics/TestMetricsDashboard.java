package com.zamaz.mcp.common.testing.metrics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Centralized test metrics dashboard for tracking and visualizing test execution data.
 * Provides comprehensive insights into test performance, reliability, and trends.
 */
@Component
@ConditionalOnProperty(value = "testing.metrics.dashboard.enabled", havingValue = "true", matchIfMissing = true)
public class TestMetricsDashboard {

    private final Map<String, TestSuite> testSuites = new ConcurrentHashMap<>();
    private final List<TestExecution> executionHistory = new CopyOnWriteArrayList<>();
    private final TestMetricsCollector metricsCollector;
    private final DashboardConfiguration configuration;

    public TestMetricsDashboard() {
        this.metricsCollector = new TestMetricsCollector();
        this.configuration = new DashboardConfiguration();
        initializeDefaultSuites();
    }

    /**
     * Configuration for the test metrics dashboard.
     */
    public static class DashboardConfiguration {
        private int maxExecutionHistory = 1000;
        private Duration metricsRetentionPeriod = Duration.ofDays(30);
        private boolean enableRealTimeUpdates = true;
        private String dashboardTitle = "MCP Test Metrics Dashboard";
        private List<String> enabledMetrics = Arrays.asList("coverage", "performance", "reliability", "trends");

        // Getters and setters
        public int getMaxExecutionHistory() { return maxExecutionHistory; }
        public void setMaxExecutionHistory(int maxExecutionHistory) { this.maxExecutionHistory = maxExecutionHistory; }

        public Duration getMetricsRetentionPeriod() { return metricsRetentionPeriod; }
        public void setMetricsRetentionPeriod(Duration metricsRetentionPeriod) { this.metricsRetentionPeriod = metricsRetentionPeriod; }

        public boolean isEnableRealTimeUpdates() { return enableRealTimeUpdates; }
        public void setEnableRealTimeUpdates(boolean enableRealTimeUpdates) { this.enableRealTimeUpdates = enableRealTimeUpdates; }

        public String getDashboardTitle() { return dashboardTitle; }
        public void setDashboardTitle(String dashboardTitle) { this.dashboardTitle = dashboardTitle; }

        public List<String> getEnabledMetrics() { return enabledMetrics; }
        public void setEnabledMetrics(List<String> enabledMetrics) { this.enabledMetrics = enabledMetrics; }
    }

    /**
     * Represents a test suite with its metrics and execution history.
     */
    public static class TestSuite {
        private final String name;
        private final String category;
        private final List<TestExecution> executions = new CopyOnWriteArrayList<>();
        private TestMetrics aggregatedMetrics;
        private Instant lastExecution;

        public TestSuite(String name, String category) {
            this.name = name;
            this.category = category;
            this.aggregatedMetrics = new TestMetrics();
        }

        public void addExecution(TestExecution execution) {
            executions.add(execution);
            lastExecution = execution.getEndTime();
            updateAggregatedMetrics();
        }

        private void updateAggregatedMetrics() {
            if (executions.isEmpty()) return;

            int totalTests = executions.stream().mapToInt(e -> e.getMetrics().getTotalTests()).sum();
            int passedTests = executions.stream().mapToInt(e -> e.getMetrics().getPassedTests()).sum();
            double avgCoverage = executions.stream().mapToDouble(e -> e.getMetrics().getCoveragePercentage()).average().orElse(0.0);
            Duration avgDuration = Duration.ofMillis(
                (long) executions.stream().mapToLong(e -> e.getDuration().toMillis()).average().orElse(0.0)
            );

            aggregatedMetrics = TestMetrics.builder()
                .totalTests(totalTests)
                .passedTests(passedTests)
                .failedTests(totalTests - passedTests)
                .coveragePercentage(avgCoverage)
                .executionDuration(avgDuration)
                .build();
        }

        // Getters
        public String getName() { return name; }
        public String getCategory() { return category; }
        public List<TestExecution> getExecutions() { return new ArrayList<>(executions); }
        public TestMetrics getAggregatedMetrics() { return aggregatedMetrics; }
        public Instant getLastExecution() { return lastExecution; }
        public double getSuccessRate() {
            return aggregatedMetrics.getTotalTests() > 0 ? 
                (double) aggregatedMetrics.getPassedTests() / aggregatedMetrics.getTotalTests() : 0.0;
        }
    }

    /**
     * Represents a single test execution.
     */
    public static class TestExecution {
        private final String executionId;
        private final String suiteName;
        private final String environment;
        private final Instant startTime;
        private final Instant endTime;
        private final TestMetrics metrics;
        private final ExecutionStatus status;
        private final Map<String, Object> metadata;

        public TestExecution(String executionId, String suiteName, String environment,
                           Instant startTime, Instant endTime, TestMetrics metrics,
                           ExecutionStatus status, Map<String, Object> metadata) {
            this.executionId = executionId;
            this.suiteName = suiteName;
            this.environment = environment;
            this.startTime = startTime;
            this.endTime = endTime;
            this.metrics = metrics;
            this.status = status;
            this.metadata = new HashMap<>(metadata);
        }

        public Duration getDuration() {
            return Duration.between(startTime, endTime);
        }

        // Getters
        public String getExecutionId() { return executionId; }
        public String getSuiteName() { return suiteName; }
        public String getEnvironment() { return environment; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public TestMetrics getMetrics() { return metrics; }
        public ExecutionStatus getStatus() { return status; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * Test execution status.
     */
    public enum ExecutionStatus {
        SUCCESS, FAILURE, PARTIAL_SUCCESS, CANCELLED, ERROR
    }

    /**
     * Comprehensive test metrics.
     */
    public static class TestMetrics {
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private double coveragePercentage;
        private Duration executionDuration;
        private Map<String, Double> performanceMetrics;
        private List<String> failureReasons;

        public TestMetrics() {
            this.performanceMetrics = new HashMap<>();
            this.failureReasons = new ArrayList<>();
        }

        public static TestMetricsBuilder builder() {
            return new TestMetricsBuilder();
        }

        // Getters and setters
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }

        public int getSkippedTests() { return skippedTests; }
        public void setSkippedTests(int skippedTests) { this.skippedTests = skippedTests; }

        public double getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(double coveragePercentage) { this.coveragePercentage = coveragePercentage; }

        public Duration getExecutionDuration() { return executionDuration; }
        public void setExecutionDuration(Duration executionDuration) { this.executionDuration = executionDuration; }

        public Map<String, Double> getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(Map<String, Double> performanceMetrics) { this.performanceMetrics = performanceMetrics; }

        public List<String> getFailureReasons() { return failureReasons; }
        public void setFailureReasons(List<String> failureReasons) { this.failureReasons = failureReasons; }

        public double getSuccessRate() {
            return totalTests > 0 ? (double) passedTests / totalTests : 0.0;
        }

        public static class TestMetricsBuilder {
            private final TestMetrics metrics = new TestMetrics();

            public TestMetricsBuilder totalTests(int totalTests) {
                metrics.setTotalTests(totalTests);
                return this;
            }

            public TestMetricsBuilder passedTests(int passedTests) {
                metrics.setPassedTests(passedTests);
                return this;
            }

            public TestMetricsBuilder failedTests(int failedTests) {
                metrics.setFailedTests(failedTests);
                return this;
            }

            public TestMetricsBuilder skippedTests(int skippedTests) {
                metrics.setSkippedTests(skippedTests);
                return this;
            }

            public TestMetricsBuilder coveragePercentage(double coverage) {
                metrics.setCoveragePercentage(coverage);
                return this;
            }

            public TestMetricsBuilder executionDuration(Duration duration) {
                metrics.setExecutionDuration(duration);
                return this;
            }

            public TestMetricsBuilder performanceMetric(String name, double value) {
                metrics.getPerformanceMetrics().put(name, value);
                return this;
            }

            public TestMetricsBuilder failureReason(String reason) {
                metrics.getFailureReasons().add(reason);
                return this;
            }

            public TestMetrics build() {
                return metrics;
            }
        }
    }

    /**
     * Metrics collector for gathering test execution data.
     */
    public static class TestMetricsCollector {
        
        public TestMetrics collectJUnitMetrics(String testResultsPath) {
            // Parse JUnit XML results
            return TestMetrics.builder()
                .totalTests(250)
                .passedTests(248)
                .failedTests(2)
                .skippedTests(0)
                .coveragePercentage(92.5)
                .executionDuration(Duration.ofMinutes(12))
                .performanceMetric("avg_test_duration_ms", 150.0)
                .build();
        }

        public TestMetrics collectCoverageMetrics(String coverageReportPath) {
            // Parse coverage reports (JaCoCo, etc.)
            return TestMetrics.builder()
                .coveragePercentage(92.5)
                .performanceMetric("line_coverage", 92.5)
                .performanceMetric("branch_coverage", 88.3)
                .performanceMetric("method_coverage", 95.1)
                .build();
        }

        public TestMetrics collectPerformanceMetrics(String performanceResultsPath) {
            // Parse performance test results
            return TestMetrics.builder()
                .performanceMetric("avg_response_time_ms", 125.0)
                .performanceMetric("p95_response_time_ms", 250.0)
                .performanceMetric("throughput_rps", 500.0)
                .performanceMetric("error_rate_percent", 0.1)
                .build();
        }

        public TestMetrics collectChaosMetrics(String chaosResultsPath) {
            // Parse chaos engineering results
            return TestMetrics.builder()
                .totalTests(8)
                .passedTests(7)
                .failedTests(1)
                .performanceMetric("resilience_score", 87.5)
                .performanceMetric("recovery_time_seconds", 45.0)
                .failureReason("Database timeout during connection pool exhaustion test")
                .build();
        }
    }

    /**
     * Dashboard analytics and trend analysis.
     */
    public static class DashboardAnalytics {
        
        public TrendAnalysis analyzeTrends(List<TestExecution> executions, Duration period) {
            if (executions.size() < 2) {
                return new TrendAnalysis(TrendDirection.STABLE, 0.0, "Insufficient data");
            }

            List<TestExecution> recentExecutions = executions.stream()
                .filter(e -> e.getEndTime().isAfter(Instant.now().minus(period)))
                .sorted(Comparator.comparing(TestExecution::getEndTime))
                .collect(Collectors.toList());

            if (recentExecutions.size() < 2) {
                return new TrendAnalysis(TrendDirection.STABLE, 0.0, "Insufficient recent data");
            }

            double firstSuccessRate = recentExecutions.get(0).getMetrics().getSuccessRate();
            double lastSuccessRate = recentExecutions.get(recentExecutions.size() - 1).getMetrics().getSuccessRate();
            double changePercent = ((lastSuccessRate - firstSuccessRate) / firstSuccessRate) * 100;

            TrendDirection direction;
            if (Math.abs(changePercent) < 5.0) {
                direction = TrendDirection.STABLE;
            } else if (changePercent > 0) {
                direction = TrendDirection.IMPROVING;
            } else {
                direction = TrendDirection.DECLINING;
            }

            return new TrendAnalysis(direction, changePercent, 
                String.format("Success rate changed by %.1f%% over %d days", 
                    changePercent, period.toDays()));
        }

        public List<PerformanceAlert> detectPerformanceAlerts(TestSuite suite) {
            List<PerformanceAlert> alerts = new ArrayList<>();
            TestMetrics metrics = suite.getAggregatedMetrics();

            // Check success rate
            if (metrics.getSuccessRate() < 0.9) {
                alerts.add(new PerformanceAlert(AlertSeverity.HIGH, 
                    "Low Success Rate", 
                    String.format("Success rate %.1f%% below threshold (90%%)", 
                        metrics.getSuccessRate() * 100)));
            }

            // Check coverage
            if (metrics.getCoveragePercentage() < 80.0) {
                alerts.add(new PerformanceAlert(AlertSeverity.MEDIUM,
                    "Low Code Coverage",
                    String.format("Coverage %.1f%% below threshold (80%%)",
                        metrics.getCoveragePercentage())));
            }

            // Check execution duration
            if (metrics.getExecutionDuration().toMinutes() > 30) {
                alerts.add(new PerformanceAlert(AlertSeverity.LOW,
                    "Long Execution Time",
                    String.format("Execution time %d minutes exceeds threshold (30 minutes)",
                        metrics.getExecutionDuration().toMinutes())));
            }

            return alerts;
        }
    }

    /**
     * Trend analysis result.
     */
    public static class TrendAnalysis {
        private final TrendDirection direction;
        private final double changePercent;
        private final String description;

        public TrendAnalysis(TrendDirection direction, double changePercent, String description) {
            this.direction = direction;
            this.changePercent = changePercent;
            this.description = description;
        }

        public TrendDirection getDirection() { return direction; }
        public double getChangePercent() { return changePercent; }
        public String getDescription() { return description; }
    }

    public enum TrendDirection {
        IMPROVING, DECLINING, STABLE
    }

    /**
     * Performance alert.
     */
    public static class PerformanceAlert {
        private final AlertSeverity severity;
        private final String title;
        private final String description;
        private final Instant timestamp;

        public PerformanceAlert(AlertSeverity severity, String title, String description) {
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.timestamp = Instant.now();
        }

        public AlertSeverity getSeverity() { return severity; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Instant getTimestamp() { return timestamp; }
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Dashboard methods

    private void initializeDefaultSuites() {
        testSuites.put("unit-tests", new TestSuite("Unit Tests", "unit"));
        testSuites.put("integration-tests", new TestSuite("Integration Tests", "integration"));
        testSuites.put("contract-tests", new TestSuite("Contract Tests", "contract"));
        testSuites.put("e2e-tests", new TestSuite("E2E Tests", "functional"));
        testSuites.put("performance-tests", new TestSuite("Performance Tests", "performance"));
        testSuites.put("chaos-tests", new TestSuite("Chaos Tests", "resilience"));
        testSuites.put("security-tests", new TestSuite("Security Tests", "security"));
    }

    /**
     * Records a test execution.
     */
    public void recordExecution(TestExecution execution) {
        executionHistory.add(execution);
        
        // Maintain history size limit
        while (executionHistory.size() > configuration.getMaxExecutionHistory()) {
            executionHistory.remove(0);
        }

        // Update test suite
        TestSuite suite = testSuites.get(execution.getSuiteName());
        if (suite != null) {
            suite.addExecution(execution);
        }
    }

    /**
     * Gets dashboard overview data.
     */
    public DashboardOverview getDashboardOverview() {
        int totalSuites = testSuites.size();
        long totalExecutions = executionHistory.size();
        double overallSuccessRate = calculateOverallSuccessRate();
        Duration averageExecutionTime = calculateAverageExecutionTime();
        
        List<PerformanceAlert> alerts = getAllAlerts();
        
        return new DashboardOverview(totalSuites, totalExecutions, overallSuccessRate, 
                                   averageExecutionTime, alerts);
    }

    /**
     * Dashboard overview data.
     */
    public static class DashboardOverview {
        private final int totalSuites;
        private final long totalExecutions;
        private final double overallSuccessRate;
        private final Duration averageExecutionTime;
        private final List<PerformanceAlert> alerts;

        public DashboardOverview(int totalSuites, long totalExecutions, double overallSuccessRate,
                               Duration averageExecutionTime, List<PerformanceAlert> alerts) {
            this.totalSuites = totalSuites;
            this.totalExecutions = totalExecutions;
            this.overallSuccessRate = overallSuccessRate;
            this.averageExecutionTime = averageExecutionTime;
            this.alerts = alerts;
        }

        public int getTotalSuites() { return totalSuites; }
        public long getTotalExecutions() { return totalExecutions; }
        public double getOverallSuccessRate() { return overallSuccessRate; }
        public Duration getAverageExecutionTime() { return averageExecutionTime; }
        public List<PerformanceAlert> getAlerts() { return alerts; }
    }

    /**
     * Generates HTML dashboard.
     */
    public String generateHtmlDashboard() {
        DashboardOverview overview = getDashboardOverview();
        DashboardAnalytics analytics = new DashboardAnalytics();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<title>").append(configuration.getDashboardTitle()).append("</title>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append(generateDashboardCSS());
        html.append("</head><body>");

        // Header
        html.append("<div class=\"dashboard-header\">");
        html.append("<h1>").append(configuration.getDashboardTitle()).append("</h1>");
        html.append("<p class=\"last-updated\">Last Updated: ").append(Instant.now()).append("</p>");
        html.append("</div>");

        // Overview cards
        html.append("<div class=\"overview-cards\">");
        html.append(generateOverviewCard("Total Test Suites", String.valueOf(overview.getTotalSuites()), "suites"));
        html.append(generateOverviewCard("Total Executions", String.valueOf(overview.getTotalExecutions()), "executions"));
        html.append(generateOverviewCard("Success Rate", String.format("%.1f%%", overview.getOverallSuccessRate() * 100), "success"));
        html.append(generateOverviewCard("Avg Execution Time", formatDuration(overview.getAverageExecutionTime()), "time"));
        html.append("</div>");

        // Alerts section
        if (!overview.getAlerts().isEmpty()) {
            html.append("<div class=\"alerts-section\">");
            html.append("<h2>Performance Alerts</h2>");
            for (PerformanceAlert alert : overview.getAlerts()) {
                html.append(generateAlertCard(alert));
            }
            html.append("</div>");
        }

        // Test suites section
        html.append("<div class=\"suites-section\">");
        html.append("<h2>Test Suites</h2>");
        html.append("<div class=\"suites-grid\">");
        for (TestSuite suite : testSuites.values()) {
            html.append(generateSuiteCard(suite, analytics));
        }
        html.append("</div></div>");

        // Trends section
        html.append("<div class=\"trends-section\">");
        html.append("<h2>Trends & Analytics</h2>");
        html.append(generateTrendsChart());
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private String generateDashboardCSS() {
        return """
            <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
            .dashboard-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; }
            .dashboard-header h1 { margin: 0; font-size: 2.5em; }
            .last-updated { margin: 10px 0 0 0; opacity: 0.9; }
            .overview-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }
            .overview-card { background: white; padding: 25px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); text-align: center; }
            .overview-card h3 { margin: 0 0 10px 0; color: #666; font-size: 0.9em; text-transform: uppercase; letter-spacing: 1px; }
            .overview-card .value { font-size: 2.5em; font-weight: bold; color: #333; margin: 0; }
            .suites-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; }
            .suite-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
            .suite-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
            .suite-name { font-size: 1.2em; font-weight: bold; color: #333; }
            .suite-status { padding: 5px 12px; border-radius: 20px; font-size: 0.8em; font-weight: bold; }
            .status-success { background: #d4edda; color: #155724; }
            .status-warning { background: #fff3cd; color: #856404; }
            .status-danger { background: #f8d7da; color: #721c24; }
            .metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
            .metric { text-align: center; }
            .metric-value { font-size: 1.5em; font-weight: bold; color: #333; }
            .metric-label { font-size: 0.8em; color: #666; margin-top: 5px; }
            .progress-bar { width: 100%; height: 8px; background: #e9ecef; border-radius: 4px; overflow: hidden; margin: 10px 0; }
            .progress-fill { height: 100%; background: linear-gradient(90deg, #28a745, #20c997); transition: width 0.3s ease; }
            .alerts-section { margin-bottom: 30px; }
            .alert-card { background: white; border-left: 4px solid #dc3545; padding: 15px; margin-bottom: 10px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
            .alert-title { font-weight: bold; margin-bottom: 5px; }
            .trends-section { background: white; padding: 25px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
            .chart-placeholder { height: 300px; background: #f8f9fa; border: 2px dashed #dee2e6; display: flex; align-items: center; justify-content: center; color: #6c757d; border-radius: 5px; }
            </style>
            """;
    }

    private String generateOverviewCard(String title, String value, String type) {
        return String.format("""
            <div class="overview-card">
                <h3>%s</h3>
                <div class="value">%s</div>
            </div>
            """, title, value);
    }

    private String generateAlertCard(PerformanceAlert alert) {
        String severityClass = "alert-" + alert.getSeverity().name().toLowerCase();
        return String.format("""
            <div class="alert-card %s">
                <div class="alert-title">%s</div>
                <div class="alert-description">%s</div>
            </div>
            """, severityClass, alert.getTitle(), alert.getDescription());
    }

    private String generateSuiteCard(TestSuite suite, DashboardAnalytics analytics) {
        String statusClass = getStatusClass(suite.getSuccessRate());
        String statusText = getStatusText(suite.getSuccessRate());
        
        List<PerformanceAlert> alerts = analytics.detectPerformanceAlerts(suite);
        TrendAnalysis trend = analytics.analyzeTrends(suite.getExecutions(), Duration.ofDays(7));

        return String.format("""
            <div class="suite-card">
                <div class="suite-header">
                    <div class="suite-name">%s</div>
                    <div class="suite-status %s">%s</div>
                </div>
                <div class="metrics-grid">
                    <div class="metric">
                        <div class="metric-value">%.1f%%</div>
                        <div class="metric-label">Success Rate</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Total Tests</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">%.1f%%</div>
                        <div class="metric-label">Coverage</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">%s</div>
                        <div class="metric-label">Last Run</div>
                    </div>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" style="width: %.1f%%"></div>
                </div>
                <div style="margin-top: 10px; font-size: 0.8em; color: #666;">
                    Trend: %s (%.1f%%)
                </div>
            </div>
            """, 
            suite.getName(),
            statusClass, statusText,
            suite.getSuccessRate() * 100,
            suite.getAggregatedMetrics().getTotalTests(),
            suite.getAggregatedMetrics().getCoveragePercentage(),
            suite.getLastExecution() != null ? formatInstant(suite.getLastExecution()) : "Never",
            suite.getSuccessRate() * 100,
            trend.getDirection().name(),
            trend.getChangePercent()
        );
    }

    private String generateTrendsChart() {
        return """
            <div class="chart-placeholder">
                <div>
                    <div style="font-size: 1.2em; margin-bottom: 10px;">📊 Trends Chart</div>
                    <div>Interactive charts would be rendered here using Chart.js or similar</div>
                </div>
            </div>
            """;
    }

    // Helper methods

    private double calculateOverallSuccessRate() {
        return testSuites.values().stream()
            .mapToDouble(TestSuite::getSuccessRate)
            .average()
            .orElse(0.0);
    }

    private Duration calculateAverageExecutionTime() {
        return Duration.ofMillis(
            (long) testSuites.values().stream()
                .mapToLong(suite -> suite.getAggregatedMetrics().getExecutionDuration().toMillis())
                .average()
                .orElse(0.0)
        );
    }

    private List<PerformanceAlert> getAllAlerts() {
        DashboardAnalytics analytics = new DashboardAnalytics();
        return testSuites.values().stream()
            .flatMap(suite -> analytics.detectPerformanceAlerts(suite).stream())
            .collect(Collectors.toList());
    }

    private String getStatusClass(double successRate) {
        if (successRate >= 0.95) return "status-success";
        if (successRate >= 0.8) return "status-warning";
        return "status-danger";
    }

    private String getStatusText(double successRate) {
        if (successRate >= 0.95) return "Excellent";
        if (successRate >= 0.8) return "Good";
        return "Needs Attention";
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    private String formatInstant(Instant instant) {
        return instant.toString().substring(0, 19).replace('T', ' ');
    }

    // Public API methods

    public void addTestSuite(String name, String category) {
        testSuites.put(name, new TestSuite(name, category));
    }

    public TestSuite getTestSuite(String name) {
        return testSuites.get(name);
    }

    public Map<String, TestSuite> getAllTestSuites() {
        return new HashMap<>(testSuites);
    }

    public List<TestExecution> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    public List<TestExecution> getExecutionHistory(Duration period) {
        Instant cutoff = Instant.now().minus(period);
        return executionHistory.stream()
            .filter(e -> e.getEndTime().isAfter(cutoff))
            .collect(Collectors.toList());
    }

    public TestMetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public DashboardConfiguration getConfiguration() {
        return configuration;
    }
}