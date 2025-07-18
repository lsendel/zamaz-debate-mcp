package com.zamaz.mcp.common.resilience.monitoring;

import com.zamaz.mcp.common.resilience.metrics.RetryMetricsCollector;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for RetryMonitoringService.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestProfile(category = "unit", priority = "medium")
@DisplayName("RetryMonitoringService Tests")
class RetryMonitoringServiceTest {
    
    private RetryMetricsCollector metricsCollector;
    private RetryMonitoringService monitoringService;
    
    @BeforeEach
    void setUp() {
        metricsCollector = new RetryMetricsCollector();
        monitoringService = new RetryMonitoringService(metricsCollector);
        
        // Set test thresholds
        ReflectionTestUtils.setField(monitoringService, "successRateThreshold", 0.8);
        ReflectionTestUtils.setField(monitoringService, "averageAttemptsThreshold", 2.5);
        ReflectionTestUtils.setField(monitoringService, "durationThresholdMs", 5000L);
        ReflectionTestUtils.setField(monitoringService, "alertCooldownMinutes", 1);
        
        monitoringService.initialize();
    }
    
    @Nested
    @DisplayName("Health Status Detection Tests")
    class HealthStatusDetectionTests {
        
        @Test
        @DisplayName("Should detect healthy retry operations")
        void shouldDetectHealthyRetryOperations() {
            // Setup healthy retry metrics
            String retryName = "healthy-service-retry";
            setupHealthyRetryMetrics(retryName);
            
            monitoringService.performHealthCheck();
            
            Optional<RetryHealthSnapshot> health = monitoringService.getRetryHealth(retryName);
            assertThat(health).isPresent();
            assertThat(health.get().getStatus()).isEqualTo(RetryHealthStatus.HEALTHY);
            assertThat(health.get().getHealthIssues()).isEmpty();
            assertThat(health.get().calculateHealthScore()).isGreaterThan(0.8);
        }
        
        @Test
        @DisplayName("Should detect warning status for degraded performance")
        void shouldDetectWarningStatus() {
            // Setup degraded retry metrics
            String retryName = "degraded-service-retry";
            setupDegradedRetryMetrics(retryName);
            
            monitoringService.performHealthCheck();
            
            Optional<RetryHealthSnapshot> health = monitoringService.getRetryHealth(retryName);
            assertThat(health).isPresent();
            assertThat(health.get().getStatus()).isEqualTo(RetryHealthStatus.WARNING);
            assertThat(health.get().getHealthIssues()).isNotEmpty();
            assertThat(health.get().getSuccessRate()).isBetween(0.6, 0.8);
        }
        
        @Test
        @DisplayName("Should detect critical status for poor performance")
        void shouldDetectCriticalStatus() {
            // Setup poor retry metrics
            String retryName = "critical-service-retry";
            setupCriticalRetryMetrics(retryName);
            
            monitoringService.performHealthCheck();
            
            Optional<RetryHealthSnapshot> health = monitoringService.getRetryHealth(retryName);
            assertThat(health).isPresent();
            assertThat(health.get().getStatus()).isEqualTo(RetryHealthStatus.CRITICAL);
            assertThat(health.get().getHealthIssues()).hasSizeGreaterThan(1);
            assertThat(health.get().getSuccessRate()).isLessThan(0.6);
        }
    }
    
    @Nested
    @DisplayName("Alert Generation Tests")
    class AlertGenerationTests {
        
        @Test
        @DisplayName("Should generate alerts for low success rate")
        void shouldGenerateAlertsForLowSuccessRate() {
            String retryName = "failing-service-retry";
            setupCriticalRetryMetrics(retryName);
            
            monitoringService.performHealthCheck();
            
            List<RetryAlert> alerts = monitoringService.getActiveAlerts(retryName);
            assertThat(alerts).isNotEmpty();
            assertThat(alerts).anyMatch(alert -> 
                alert.getType() == RetryAlertType.LOW_SUCCESS_RATE);
        }
        
        @Test
        @DisplayName("Should generate alerts for high retry attempts")
        void shouldGenerateAlertsForHighRetryAttempts() {
            String retryName = "high-attempts-retry";
            
            // Setup metrics with high average attempts
            for (int i = 0; i < 10; i++) {
                metricsCollector.recordRetryAttempt(retryName, 1, new RuntimeException("First attempt"));
                metricsCollector.recordRetryAttempt(retryName, 2, new RuntimeException("Second attempt"));
                metricsCollector.recordRetryAttempt(retryName, 3, new RuntimeException("Third attempt"));
                metricsCollector.recordRetrySuccess(retryName, 4, Duration.ofMillis(200));
            }
            
            monitoringService.performHealthCheck();
            
            List<RetryAlert> alerts = monitoringService.getActiveAlerts(retryName);
            assertThat(alerts).anyMatch(alert -> 
                alert.getType() == RetryAlertType.HIGH_RETRY_ATTEMPTS);
        }
        
        @Test
        @DisplayName("Should generate alerts for high duration")
        void shouldGenerateAlertsForHighDuration() {
            String retryName = "slow-service-retry";
            
            // Setup metrics with high duration
            for (int i = 0; i < 5; i++) {
                metricsCollector.recordRetrySuccess(retryName, 1, Duration.ofMillis(8000)); // Above threshold
            }
            
            monitoringService.performHealthCheck();
            
            List<RetryAlert> alerts = monitoringService.getActiveAlerts(retryName);
            assertThat(alerts).anyMatch(alert -> 
                alert.getType() == RetryAlertType.HIGH_DURATION);
        }
        
        @Test
        @DisplayName("Should apply cooldown to prevent alert spam")
        void shouldApplyCooldownToPreventAlertSpam() {
            String retryName = "cooldown-test-retry";
            setupCriticalRetryMetrics(retryName);
            
            // First health check - should generate alerts
            monitoringService.performHealthCheck();
            List<RetryAlert> firstAlerts = monitoringService.getActiveAlerts(retryName);
            int firstAlertCount = firstAlerts.size();
            
            // Add more critical metrics (same conditions)
            setupCriticalRetryMetrics(retryName);
            
            // Second health check immediately - should be suppressed by cooldown
            monitoringService.performHealthCheck();
            List<RetryAlert> secondAlerts = monitoringService.getActiveAlerts(retryName);
            
            // Should not generate new alerts due to cooldown
            assertThat(secondAlerts).hasSize(firstAlertCount);
        }
    }
    
    @Nested
    @DisplayName("Health Overview Tests")
    class HealthOverviewTests {
        
        @Test
        @DisplayName("Should provide comprehensive health overview")
        void shouldProvideComprehensiveHealthOverview() {
            // Setup multiple retry operations with different health statuses
            setupHealthyRetryMetrics("healthy-service-1");
            setupHealthyRetryMetrics("healthy-service-2");
            setupDegradedRetryMetrics("warning-service-1");
            setupCriticalRetryMetrics("critical-service-1");
            
            monitoringService.performHealthCheck();
            
            RetryHealthOverview overview = monitoringService.getHealthOverview();
            
            assertThat(overview.getTotalRetryOperations()).isEqualTo(4);
            assertThat(overview.getHealthyCount()).isEqualTo(2);
            assertThat(overview.getWarningCount()).isEqualTo(1);
            assertThat(overview.getCriticalCount()).isEqualTo(1);
            assertThat(overview.getOverallHealthPercentage()).isEqualTo(50.0); // 2/4 healthy
            assertThat(overview.getSystemHealthStatus()).isEqualTo(RetryHealthStatus.CRITICAL); // Has critical operations
        }
        
        @Test
        @DisplayName("Should identify most problematic retry operations")
        void shouldIdentifyMostProblematicRetryOperations() {
            setupHealthyRetryMetrics("good-service");
            setupCriticalRetryMetrics("bad-service-1");
            setupCriticalRetryMetrics("bad-service-2");
            setupDegradedRetryMetrics("ok-service");
            
            monitoringService.performHealthCheck();
            
            RetryHealthOverview overview = monitoringService.getHealthOverview();
            List<String> problematic = overview.getMostProblematicRetries();
            
            assertThat(problematic).containsExactlyInAnyOrder("bad-service-1", "bad-service-2");
        }
        
        @Test
        @DisplayName("Should detect high-priority issues")
        void shouldDetectHighPriorityIssues() {
            setupCriticalRetryMetrics("critical-service");
            
            monitoringService.performHealthCheck();
            
            RetryHealthOverview overview = monitoringService.getHealthOverview();
            
            assertThat(overview.hasHighPriorityIssues()).isTrue();
            assertThat(overview.getAlertCountsBySeverity()).containsKey(AlertSeverity.HIGH);
        }
    }
    
    @Nested
    @DisplayName("Reporting Tests")
    class ReportingTests {
        
        @Test
        @DisplayName("Should generate comprehensive health report")
        void shouldGenerateComprehensiveHealthReport() {
            setupHealthyRetryMetrics("service-a");
            setupDegradedRetryMetrics("service-b");
            setupCriticalRetryMetrics("service-c");
            
            monitoringService.performHealthCheck();
            
            String report = monitoringService.generateHealthReport();
            
            assertThat(report)
                .contains("Retry Health Report")
                .contains("service-a")
                .contains("service-b") 
                .contains("service-c")
                .contains("HEALTHY")
                .contains("WARNING")
                .contains("CRITICAL")
                .contains("Success Rate")
                .contains("Avg Attempts");
        }
        
        @Test
        @DisplayName("Should provide compact health summary")
        void shouldProvideCompactHealthSummary() {
            setupHealthyRetryMetrics("service-1");
            setupCriticalRetryMetrics("service-2");
            
            monitoringService.performHealthCheck();
            
            RetryHealthOverview overview = monitoringService.getHealthOverview();
            String summary = overview.getCompactSummary();
            
            assertThat(summary)
                .contains("Health: 50.0%")
                .contains("(1/2 healthy")
                .contains("alerts)");
        }
    }
    
    @Nested
    @DisplayName("Alert Management Tests")
    class AlertManagementTests {
        
        @Test
        @DisplayName("Should clear alerts for specific retry operation")
        void shouldClearAlertsForSpecificRetryOperation() {
            String retryName = "test-service-retry";
            setupCriticalRetryMetrics(retryName);
            
            monitoringService.performHealthCheck();
            
            // Verify alerts exist
            assertThat(monitoringService.getActiveAlerts(retryName)).isNotEmpty();
            
            // Clear alerts
            monitoringService.clearAlerts(retryName);
            
            // Verify alerts are cleared
            assertThat(monitoringService.getActiveAlerts(retryName)).isEmpty();
        }
        
        @Test
        @DisplayName("Should track alert age and recency")
        void shouldTrackAlertAgeAndRecency() {
            String retryName = "age-test-retry";
            setupCriticalRetryMetrics(retryName);
            
            monitoringService.performHealthCheck();
            
            List<RetryAlert> alerts = monitoringService.getActiveAlerts(retryName);
            assertThat(alerts).isNotEmpty();
            
            RetryAlert alert = alerts.get(0);
            assertThat(alert.isRecent()).isTrue();
            assertThat(alert.getAgeMinutes()).isLessThan(1);
            assertThat(alert.getFormattedMessage()).contains(alert.getRetryName());
        }
    }
    
    // Helper methods for setting up test data
    
    private void setupHealthyRetryMetrics(String retryName) {
        // High success rate, low attempts, fast execution
        for (int i = 0; i < 10; i++) {
            metricsCollector.recordRetrySuccess(retryName, 1, Duration.ofMillis(150));
        }
    }
    
    private void setupDegradedRetryMetrics(String retryName) {
        // Medium success rate, some retries, moderate duration
        for (int i = 0; i < 10; i++) {
            if (i < 7) {
                metricsCollector.recordRetryAttempt(retryName, 1, new RuntimeException("Transient failure"));
                metricsCollector.recordRetrySuccess(retryName, 2, Duration.ofMillis(800));
            } else {
                metricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(2000), 
                    new RuntimeException("Service unavailable"));
            }
        }
    }
    
    private void setupCriticalRetryMetrics(String retryName) {
        // Low success rate, many retries, slow execution
        for (int i = 0; i < 10; i++) {
            if (i < 3) {
                metricsCollector.recordRetrySuccess(retryName, 3, Duration.ofMillis(6000));
            } else {
                metricsCollector.recordRetryFailure(retryName, 3, Duration.ofMillis(8000), 
                    new RuntimeException("Service failure"));
            }
        }
    }
}