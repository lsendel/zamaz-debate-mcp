package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.metrics.RetryMetricsCollector;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import com.zamaz.mcp.common.testing.metrics.TestMetricsDashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for retry metrics with the test metrics dashboard.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestProfile(category = "integration", priority = "medium")
@DisplayName("Retry Metrics Dashboard Integration Tests")
class RetryMetricsIntegrationTest {
    
    @Autowired
    private RetryMetricsCollector retryMetricsCollector;
    
    @Autowired
    private TestMetricsDashboard testMetricsDashboard;
    
    @BeforeEach
    void setUp() {
        retryMetricsCollector.clearMetrics();
    }
    
    @Nested
    @DisplayName("Metrics Collection Integration")
    class MetricsCollectionIntegrationTests {
        
        @Test
        @DisplayName("Should integrate retry metrics with test dashboard")
        void shouldIntegrateRetryMetricsWithDashboard() {
            // Simulate retry operations
            String retryName = "dashboard-integration-test";
            
            // Simulate successful retry with 2 attempts
            retryMetricsCollector.recordRetryAttempt(retryName, 1, new RuntimeException("First attempt failed"));
            retryMetricsCollector.recordRetrySuccess(retryName, 2, Duration.ofMillis(500));
            
            // Simulate failed retry with 3 attempts
            retryMetricsCollector.recordRetryAttempt(retryName, 1, new RuntimeException("First attempt failed"));
            retryMetricsCollector.recordRetryAttempt(retryName, 2, new RuntimeException("Second attempt failed"));
            retryMetricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(1200), 
                new RuntimeException("Final attempt failed"));
            
            // Convert retry metrics to test metrics and record in dashboard
            RetryMetricsCollector.RetryStats stats = retryMetricsCollector.getRetryStats(retryName);
            
            TestMetricsDashboard.TestMetrics testMetrics = TestMetricsDashboard.TestMetrics.builder()
                .totalTests((int) stats.getTotalExecutions())
                .passedTests((int) stats.getSuccessfulExecutions())
                .failedTests((int) stats.getFailedExecutions())
                .executionDuration(Duration.ofMillis((long) stats.getAverageDurationMs()))
                .performanceMetric("retry.success_rate", stats.getSuccessRate())
                .performanceMetric("retry.avg_attempts", stats.getAverageAttemptsPerExecution())
                .performanceMetric("retry.avg_duration_ms", stats.getAverageDurationMs())
                .build();
            
            String executionId = "retry-" + System.currentTimeMillis();
            TestMetricsDashboard.TestExecution execution = new TestMetricsDashboard.TestExecution(
                executionId, "retry-tests", "test", Instant.now().minus(Duration.ofSeconds(2)), 
                Instant.now(), testMetrics, TestMetricsDashboard.ExecutionStatus.PARTIAL_SUCCESS, 
                createMetadata(retryName, stats));
            
            testMetricsDashboard.recordExecution(execution);
            
            // Verify integration
            TestMetricsDashboard.TestSuite retrySuite = testMetricsDashboard.getTestSuite("retry-tests");
            assertThat(retrySuite).isNotNull();
            assertThat(retrySuite.getExecutions()).hasSize(1);
            
            TestMetricsDashboard.TestExecution recordedExecution = retrySuite.getExecutions().get(0);
            assertThat(recordedExecution.getMetrics().getTotalTests()).isEqualTo(2);
            assertThat(recordedExecution.getMetrics().getPassedTests()).isEqualTo(1);
            assertThat(recordedExecution.getMetrics().getFailedTests()).isEqualTo(1);
            assertThat(recordedExecution.getMetrics().getPerformanceMetrics())
                .containsEntry("retry.success_rate", 0.5)
                .containsEntry("retry.avg_attempts", 2.5);
        }
        
        @Test
        @DisplayName("Should generate retry analytics for dashboard")
        void shouldGenerateRetryAnalyticsForDashboard() {
            // Create multiple retry scenarios
            String[] retryNames = {"service-a-retry", "service-b-retry", "service-c-retry"};
            
            for (String retryName : retryNames) {
                // Simulate different retry patterns
                if (retryName.contains("service-a")) {
                    // High success rate
                    for (int i = 0; i < 10; i++) {
                        if (i < 9) {
                            retryMetricsCollector.recordRetrySuccess(retryName, 1, Duration.ofMillis(100));
                        } else {
                            retryMetricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(300), 
                                new RuntimeException("Rare failure"));
                        }
                    }
                } else if (retryName.contains("service-b")) {
                    // Medium success rate with retries
                    for (int i = 0; i < 10; i++) {
                        if (i < 7) {
                            retryMetricsCollector.recordRetryAttempt(retryName, 1, new RuntimeException("Transient"));
                            retryMetricsCollector.recordRetrySuccess(retryName, 2, Duration.ofMillis(200));
                        } else {
                            retryMetricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(600), 
                                new RuntimeException("Persistent failure"));
                        }
                    }
                } else {
                    // Low success rate
                    for (int i = 0; i < 10; i++) {
                        if (i < 4) {
                            retryMetricsCollector.recordRetrySuccess(retryName, 3, Duration.ofMillis(500));
                        } else {
                            retryMetricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(900), 
                                new RuntimeException("Frequent failure"));
                        }
                    }
                }
            }
            
            // Generate dashboard overview with retry analytics
            Map<String, RetryAnalytics> analyticsMap = generateRetryAnalytics();
            
            // Verify analytics
            assertThat(analyticsMap).containsKeys(retryNames);
            
            RetryAnalytics serviceAAnalytics = analyticsMap.get("service-a-retry");
            assertThat(serviceAAnalytics.getSuccessRate()).isEqualTo(0.9);
            assertThat(serviceAAnalytics.getStatus()).isEqualTo(RetryStatus.HEALTHY);
            
            RetryAnalytics serviceBAnalytics = analyticsMap.get("service-b-retry");
            assertThat(serviceBAnalytics.getSuccessRate()).isEqualTo(0.7);
            assertThat(serviceBAnalytics.getStatus()).isEqualTo(RetryStatus.WARNING);
            
            RetryAnalytics serviceCAnalytics = analyticsMap.get("service-c-retry");
            assertThat(serviceCAnalytics.getSuccessRate()).isEqualTo(0.4);
            assertThat(serviceCAnalytics.getStatus()).isEqualTo(RetryStatus.CRITICAL);
        }
        
        @Test
        @DisplayName("Should track retry trends over time")
        void shouldTrackRetryTrendsOverTime() {
            String retryName = "trending-service-retry";
            
            // Simulate degrading performance over time
            for (int hour = 0; hour < 24; hour++) {
                double successRate = Math.max(0.1, 1.0 - (hour * 0.03)); // Degrading from 100% to 28%
                
                for (int operation = 0; operation < 10; operation++) {
                    if (Math.random() < successRate) {
                        retryMetricsCollector.recordRetrySuccess(retryName, 1, Duration.ofMillis(100));
                    } else {
                        retryMetricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(500), 
                            new RuntimeException("Service degradation"));
                    }
                }
            }
            
            RetryMetricsCollector.RetryStats stats = retryMetricsCollector.getRetryStats(retryName);
            
            // Verify trend detection
            assertThat(stats.getTotalExecutions()).isEqualTo(240); // 24 hours * 10 operations
            assertThat(stats.getSuccessRate()).isLessThan(0.7); // Should show degradation
            assertThat(stats.getLastError()).isNotNull();
            
            // Generate trend analysis
            RetryTrendAnalysis trendAnalysis = analyzeTrend(stats);
            assertThat(trendAnalysis.getTrend()).isEqualTo(TrendDirection.DECLINING);
            assertThat(trendAnalysis.getRecommendation()).contains("investigate");
        }
    }
    
    @Nested
    @DisplayName("Dashboard Visualization Tests")
    class DashboardVisualizationTests {
        
        @Test
        @DisplayName("Should generate retry dashboard HTML with metrics")
        void shouldGenerateRetryDashboardHTML() {
            // Setup retry data
            setupRetryTestData();
            
            // Generate HTML dashboard
            String dashboardHtml = testMetricsDashboard.generateHtmlDashboard();
            
            // Verify retry metrics are included
            assertThat(dashboardHtml)
                .contains("retry")
                .contains("attempts")
                .contains("success")
                .contains("failure");
        }
        
        @Test
        @DisplayName("Should include retry alerts in dashboard")
        void shouldIncludeRetryAlertsInDashboard() {
            // Create scenarios that should trigger alerts
            String problematicRetry = "problematic-service-retry";
            
            // High failure rate
            for (int i = 0; i < 10; i++) {
                retryMetricsCollector.recordRetryFailure(problematicRetry, 3, Duration.ofSeconds(5), 
                    new RuntimeException("Consistent failures"));
            }
            
            // Convert to test metrics
            RetryMetricsCollector.RetryStats stats = retryMetricsCollector.getRetryStats(problematicRetry);
            
            TestMetricsDashboard.TestMetrics alertMetrics = TestMetricsDashboard.TestMetrics.builder()
                .totalTests((int) stats.getTotalExecutions())
                .passedTests((int) stats.getSuccessfulExecutions())
                .failedTests((int) stats.getFailedExecutions())
                .performanceMetric("retry.success_rate", stats.getSuccessRate())
                .performanceMetric("retry.avg_duration_ms", stats.getAverageDurationMs())
                .failureReason("High retry failure rate detected")
                .build();
            
            TestMetricsDashboard.TestExecution alertExecution = new TestMetricsDashboard.TestExecution(
                "alert-test", "retry-alerts", "production", Instant.now().minus(Duration.ofMinutes(5)), 
                Instant.now(), alertMetrics, TestMetricsDashboard.ExecutionStatus.FAILURE, 
                createMetadata(problematicRetry, stats));
            
            testMetricsDashboard.recordExecution(alertExecution);
            
            // Get dashboard overview and check for alerts
            TestMetricsDashboard.DashboardOverview overview = testMetricsDashboard.getDashboardOverview();
            
            assertThat(overview.getAlerts()).isNotEmpty();
            assertThat(overview.getAlerts())
                .anyMatch(alert -> alert.getTitle().contains("Success Rate") || 
                                  alert.getTitle().contains("Performance"));
        }
    }
    
    // Helper methods
    
    private Map<String, Object> createMetadata(String retryName, RetryMetricsCollector.RetryStats stats) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("retry.name", retryName);
        metadata.put("retry.total_attempts", stats.getTotalAttempts());
        metadata.put("retry.success_rate", stats.getSuccessRate());
        metadata.put("retry.avg_duration_ms", stats.getAverageDurationMs());
        metadata.put("retry.last_error", stats.getLastError());
        return metadata;
    }
    
    private Map<String, RetryAnalytics> generateRetryAnalytics() {
        Map<String, RetryAnalytics> analytics = new HashMap<>();
        
        retryMetricsCollector.getAllRetryStats().forEach((retryName, stats) -> {
            RetryStatus status;
            if (stats.getSuccessRate() >= 0.9) {
                status = RetryStatus.HEALTHY;
            } else if (stats.getSuccessRate() >= 0.7) {
                status = RetryStatus.WARNING;
            } else {
                status = RetryStatus.CRITICAL;
            }
            
            analytics.put(retryName, new RetryAnalytics(
                retryName, stats.getSuccessRate(), status, 
                stats.getAverageAttemptsPerExecution(), stats.getAverageDurationMs()));
        });
        
        return analytics;
    }
    
    private RetryTrendAnalysis analyzeTrend(RetryMetricsCollector.RetryStats stats) {
        TrendDirection trend;
        String recommendation;
        
        if (stats.getSuccessRate() < 0.5) {
            trend = TrendDirection.DECLINING;
            recommendation = "Critical: investigate service reliability issues immediately";
        } else if (stats.getSuccessRate() < 0.8) {
            trend = TrendDirection.STABLE;
            recommendation = "Warning: monitor service performance closely";
        } else {
            trend = TrendDirection.IMPROVING;
            recommendation = "Good: service is performing well";
        }
        
        return new RetryTrendAnalysis(trend, stats.getSuccessRate(), recommendation);
    }
    
    private void setupRetryTestData() {
        String[] services = {"auth-service", "payment-service", "notification-service"};
        
        for (String service : services) {
            String retryName = service + "-retry";
            
            // Mixed success/failure scenarios
            for (int i = 0; i < 5; i++) {
                if (i < 4) {
                    retryMetricsCollector.recordRetrySuccess(retryName, 1, Duration.ofMillis(150));
                } else {
                    retryMetricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(450), 
                        new RuntimeException("Service timeout"));
                }
            }
        }
    }
    
    // Helper classes
    
    static class RetryAnalytics {
        private final String retryName;
        private final double successRate;
        private final RetryStatus status;
        private final double avgAttempts;
        private final double avgDuration;
        
        public RetryAnalytics(String retryName, double successRate, RetryStatus status, 
                             double avgAttempts, double avgDuration) {
            this.retryName = retryName;
            this.successRate = successRate;
            this.status = status;
            this.avgAttempts = avgAttempts;
            this.avgDuration = avgDuration;
        }
        
        public String getRetryName() { return retryName; }
        public double getSuccessRate() { return successRate; }
        public RetryStatus getStatus() { return status; }
        public double getAvgAttempts() { return avgAttempts; }
        public double getAvgDuration() { return avgDuration; }
    }
    
    enum RetryStatus {
        HEALTHY, WARNING, CRITICAL
    }
    
    static class RetryTrendAnalysis {
        private final TrendDirection trend;
        private final double currentRate;
        private final String recommendation;
        
        public RetryTrendAnalysis(TrendDirection trend, double currentRate, String recommendation) {
            this.trend = trend;
            this.currentRate = currentRate;
            this.recommendation = recommendation;
        }
        
        public TrendDirection getTrend() { return trend; }
        public double getCurrentRate() { return currentRate; }
        public String getRecommendation() { return recommendation; }
    }
    
    enum TrendDirection {
        IMPROVING, STABLE, DECLINING
    }
}