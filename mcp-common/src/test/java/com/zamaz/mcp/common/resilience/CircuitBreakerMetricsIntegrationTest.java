package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.metrics.CircuitBreakerMetricsCollector;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import com.zamaz.mcp.common.testing.metrics.MetricsIntegration;
import com.zamaz.mcp.common.testing.metrics.TestMetricsDashboard;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for circuit breaker metrics integration with the test metrics dashboard.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestProfile(category = "integration", priority = "high")
@DisplayName("Circuit Breaker Metrics Integration Tests")
class CircuitBreakerMetricsIntegrationTest {

    private CircuitBreakerMetricsCollector metricsCollector;
    private TestMetricsDashboard dashboard;
    private MetricsIntegration.CircuitBreakerMetricsIntegration integration;

    @BeforeEach
    void setUp() {
        metricsCollector = new CircuitBreakerMetricsCollector();
        dashboard = new TestMetricsDashboard();
        integration = new MetricsIntegration.CircuitBreakerMetricsIntegration(metricsCollector);
    }

    @Nested
    @DisplayName("Metrics Collection Tests")
    class MetricsCollectionTests {

        @Test
        @DisplayName("Should collect aggregated circuit breaker metrics")
        void shouldCollectAggregatedMetrics() {
            // Setup test data
            String cb1 = "test-cb-1";
            String cb2 = "test-cb-2";
            
            // Simulate successful executions
            for (int i = 0; i < 10; i++) {
                metricsCollector.recordSuccessfulExecution(cb1, Duration.ofMillis(100));
                metricsCollector.recordSuccessfulExecution(cb2, Duration.ofMillis(150));
            }
            
            // Simulate some failures
            for (int i = 0; i < 2; i++) {
                metricsCollector.recordFailedExecution(cb1, Duration.ofMillis(200), 
                    new RuntimeException("Test failure"));
            }
            
            // Collect metrics
            TestMetricsDashboard.TestMetrics metrics = integration.collectCircuitBreakerMetrics();
            
            // Verify aggregated metrics
            assertThat(metrics.getTotalTests()).isEqualTo(22); // 10+10+2
            assertThat(metrics.getPassedTests()).isEqualTo(20); // 10+10
            assertThat(metrics.getFailedTests()).isEqualTo(2);
            assertThat(metrics.getPerformanceMetrics()).containsKey("circuit_breaker_health_score");
            assertThat(metrics.getPerformanceMetrics()).containsKey("success_rate_percent");
            assertThat(metrics.getPerformanceMetrics()).containsKey("failure_rate_percent");
            assertThat(metrics.getPerformanceMetrics()).containsKey("total_circuit_breakers");
            
            // Verify calculated rates
            double successRate = metrics.getPerformanceMetrics().get("success_rate_percent");
            double failureRate = metrics.getPerformanceMetrics().get("failure_rate_percent");
            double totalCircuitBreakers = metrics.getPerformanceMetrics().get("total_circuit_breakers");
            
            assertThat(successRate).isEqualTo(20.0 / 22.0 * 100, offset(0.01));
            assertThat(failureRate).isEqualTo(2.0 / 22.0 * 100, offset(0.01));
            assertThat(totalCircuitBreakers).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should collect individual circuit breaker metrics")
        void shouldCollectIndividualMetrics() {
            String cbName = "individual-test-cb";
            
            // Setup test data
            for (int i = 0; i < 8; i++) {
                metricsCollector.recordSuccessfulExecution(cbName, Duration.ofMillis(120));
            }
            
            for (int i = 0; i < 2; i++) {
                metricsCollector.recordFailedExecution(cbName, Duration.ofMillis(300), 
                    new RuntimeException("Individual test failure"));
            }
            
            // Simulate state changes
            metricsCollector.recordStateChange(cbName, CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
            metricsCollector.recordStateChange(cbName, CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
            
            // Simulate calls not permitted and fallbacks
            metricsCollector.recordCallNotPermitted(cbName);
            metricsCollector.recordFallbackExecution(cbName, true, Duration.ofMillis(50));
            
            // Collect individual metrics
            Map<String, TestMetricsDashboard.TestMetrics> individualMetrics = 
                integration.collectIndividualCircuitBreakerMetrics();
            
            assertThat(individualMetrics).containsKey(cbName);
            
            TestMetricsDashboard.TestMetrics metrics = individualMetrics.get(cbName);
            assertThat(metrics.getTotalTests()).isEqualTo(10);
            assertThat(metrics.getPassedTests()).isEqualTo(8);
            assertThat(metrics.getFailedTests()).isEqualTo(2);
            
            Map<String, Double> performanceMetrics = metrics.getPerformanceMetrics();
            assertThat(performanceMetrics).containsKey("health_score");
            assertThat(performanceMetrics).containsKey("success_rate_percent");
            assertThat(performanceMetrics).containsKey("failure_rate_percent");
            assertThat(performanceMetrics).containsKey("calls_not_permitted");
            assertThat(performanceMetrics).containsKey("fallback_executions");
            assertThat(performanceMetrics).containsKey("state_changes");
            assertThat(performanceMetrics).containsKey("current_state");
            
            // Verify specific values
            assertThat(performanceMetrics.get("success_rate_percent")).isEqualTo(80.0);
            assertThat(performanceMetrics.get("failure_rate_percent")).isEqualTo(20.0);
            assertThat(performanceMetrics.get("calls_not_permitted")).isEqualTo(1.0);
            assertThat(performanceMetrics.get("fallback_executions")).isEqualTo(1.0);
            assertThat(performanceMetrics.get("state_changes")).isEqualTo(2.0);
            assertThat(performanceMetrics.get("current_state")).isEqualTo(1.0); // HALF_OPEN
        }

        @Test
        @DisplayName("Should handle empty metrics gracefully")
        void shouldHandleEmptyMetrics() {
            TestMetricsDashboard.TestMetrics metrics = integration.collectCircuitBreakerMetrics();
            
            assertThat(metrics.getTotalTests()).isEqualTo(0);
            assertThat(metrics.getPassedTests()).isEqualTo(0);
            assertThat(metrics.getFailedTests()).isEqualTo(0);
            assertThat(metrics.getPerformanceMetrics().get("circuit_breaker_health_score")).isEqualTo(100.0);
            assertThat(metrics.getPerformanceMetrics().get("success_rate_percent")).isEqualTo(100.0);
            assertThat(metrics.getPerformanceMetrics().get("failure_rate_percent")).isEqualTo(0.0);
            assertThat(metrics.getPerformanceMetrics().get("total_circuit_breakers")).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Alert Generation Tests")
    class AlertGenerationTests {

        @Test
        @DisplayName("Should generate health score alerts")
        void shouldGenerateHealthScoreAlerts() {
            String cbName = "unhealthy-cb";
            
            // Create a circuit breaker with poor health
            for (int i = 0; i < 3; i++) {
                metricsCollector.recordSuccessfulExecution(cbName, Duration.ofMillis(100));
            }
            
            for (int i = 0; i < 7; i++) {
                metricsCollector.recordFailedExecution(cbName, Duration.ofMillis(500), 
                    new RuntimeException("Health test failure"));
            }
            
            List<TestMetricsDashboard.PerformanceAlert> alerts = integration.generateCircuitBreakerAlerts();
            
            assertThat(alerts).isNotEmpty();
            assertThat(alerts).anyMatch(alert -> 
                alert.getTitle().equals("Circuit Breaker Health Issue") &&
                alert.getDescription().contains(cbName) &&
                alert.getSeverity() == TestMetricsDashboard.AlertSeverity.HIGH);
        }

        @Test
        @DisplayName("Should generate failure rate alerts")
        void shouldGenerateFailureRateAlerts() {
            String cbName = "high-failure-cb";
            
            // Create a circuit breaker with high failure rate
            for (int i = 0; i < 5; i++) {
                metricsCollector.recordSuccessfulExecution(cbName, Duration.ofMillis(100));
            }
            
            for (int i = 0; i < 3; i++) {
                metricsCollector.recordFailedExecution(cbName, Duration.ofMillis(300), 
                    new RuntimeException("Failure rate test"));
            }
            
            List<TestMetricsDashboard.PerformanceAlert> alerts = integration.generateCircuitBreakerAlerts();
            
            assertThat(alerts).anyMatch(alert -> 
                alert.getTitle().equals("High Circuit Breaker Failure Rate") &&
                alert.getDescription().contains(cbName) &&
                alert.getSeverity() == TestMetricsDashboard.AlertSeverity.MEDIUM);
        }

        @Test
        @DisplayName("Should generate circuit open alerts")
        void shouldGenerateCircuitOpenAlerts() {
            String cbName = "open-cb";
            
            // Simulate circuit breaker opening
            metricsCollector.recordStateChange(cbName, CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
            
            List<TestMetricsDashboard.PerformanceAlert> alerts = integration.generateCircuitBreakerAlerts();
            
            assertThat(alerts).anyMatch(alert -> 
                alert.getTitle().equals("Circuit Breaker Open") &&
                alert.getDescription().contains(cbName) &&
                alert.getSeverity() == TestMetricsDashboard.AlertSeverity.CRITICAL);
        }

        @Test
        @DisplayName("Should generate call rejection alerts")
        void shouldGenerateCallRejectionAlerts() {
            String cbName = "rejection-cb";
            
            // Simulate high call rejection rate
            for (int i = 0; i < 10; i++) {
                metricsCollector.recordSuccessfulExecution(cbName, Duration.ofMillis(100));
            }
            
            for (int i = 0; i < 2; i++) {
                metricsCollector.recordCallNotPermitted(cbName);
            }
            
            List<TestMetricsDashboard.PerformanceAlert> alerts = integration.generateCircuitBreakerAlerts();
            
            assertThat(alerts).anyMatch(alert -> 
                alert.getTitle().equals("High Call Rejection Rate") &&
                alert.getDescription().contains(cbName) &&
                alert.getSeverity() == TestMetricsDashboard.AlertSeverity.MEDIUM);
        }

        @Test
        @DisplayName("Should generate fallback failure alerts")
        void shouldGenerateFallbackFailureAlerts() {
            String cbName = "fallback-failure-cb";
            
            // Simulate fallback failures
            metricsCollector.recordFallbackExecution(cbName, true, Duration.ofMillis(50)); // 1 success
            metricsCollector.recordFallbackExecution(cbName, false, Duration.ofMillis(100)); // 1 failure
            metricsCollector.recordFallbackExecution(cbName, false, Duration.ofMillis(150)); // 1 failure
            metricsCollector.recordFallbackExecution(cbName, false, Duration.ofMillis(200)); // 1 failure
            metricsCollector.recordFallbackExecution(cbName, false, Duration.ofMillis(250)); // 1 failure
            // Total: 1 success, 4 failures = 20% success rate (below 80% threshold)
            
            List<TestMetricsDashboard.PerformanceAlert> alerts = integration.generateCircuitBreakerAlerts();
            
            assertThat(alerts).anyMatch(alert -> 
                alert.getTitle().equals("Fallback Mechanism Issues") &&
                alert.getDescription().contains(cbName) &&
                alert.getSeverity() == TestMetricsDashboard.AlertSeverity.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Dashboard Integration Tests")
    class DashboardIntegrationTests {

        @Test
        @DisplayName("Should integrate circuit breaker metrics into dashboard")
        void shouldIntegrateToDashboard() {
            // Setup test circuit breakers
            String cb1 = "dashboard-cb-1";
            String cb2 = "dashboard-cb-2";
            
            // Create realistic test data
            for (int i = 0; i < 50; i++) {
                metricsCollector.recordSuccessfulExecution(cb1, Duration.ofMillis(120));
            }
            
            for (int i = 0; i < 5; i++) {
                metricsCollector.recordFailedExecution(cb1, Duration.ofMillis(300), 
                    new RuntimeException("Dashboard test failure"));
            }
            
            for (int i = 0; i < 30; i++) {
                metricsCollector.recordSuccessfulExecution(cb2, Duration.ofMillis(80));
            }
            
            // Record metrics to dashboard
            TestMetricsDashboard.TestMetrics aggregateMetrics = integration.collectCircuitBreakerMetrics();
            
            String executionId = "cb-integration-test-" + System.currentTimeMillis();
            Instant now = Instant.now();
            TestMetricsDashboard.TestExecution execution = new TestMetricsDashboard.TestExecution(
                executionId, "circuit-breaker-tests", "test", now.minus(Duration.ofMinutes(2)), 
                now, aggregateMetrics, TestMetricsDashboard.ExecutionStatus.SUCCESS, Map.of());
            
            dashboard.recordExecution(execution);
            
            // Verify dashboard overview
            TestMetricsDashboard.DashboardOverview overview = dashboard.getDashboardOverview();
            assertThat(overview.getTotalExecutions()).isGreaterThan(0);
            
            // Verify test suite was created and populated
            TestMetricsDashboard.TestSuite circuitBreakerSuite = dashboard.getTestSuite("circuit-breaker-tests");
            assertThat(circuitBreakerSuite).isNotNull();
            assertThat(circuitBreakerSuite.getExecutions()).isNotEmpty();
            assertThat(circuitBreakerSuite.getAggregatedMetrics().getTotalTests()).isEqualTo(85); // 50+5+30
        }

        @Test
        @DisplayName("Should generate comprehensive HTML dashboard with circuit breaker metrics")
        void shouldGenerateHtmlDashboard() {
            // Setup comprehensive test data
            String[] circuitBreakers = {"service-a-cb", "service-b-cb", "database-cb"};
            
            for (String cbName : circuitBreakers) {
                // Different patterns for each circuit breaker
                int successCount = 40 + (int)(Math.random() * 20);
                int failureCount = 2 + (int)(Math.random() * 8);
                
                for (int i = 0; i < successCount; i++) {
                    metricsCollector.recordSuccessfulExecution(cbName, 
                        Duration.ofMillis(100 + (long)(Math.random() * 100)));
                }
                
                for (int i = 0; i < failureCount; i++) {
                    metricsCollector.recordFailedExecution(cbName, 
                        Duration.ofMillis(200 + (long)(Math.random() * 200)),
                        new RuntimeException("Test failure for " + cbName));
                }
            }
            
            // Record to dashboard
            TestMetricsDashboard.TestMetrics metrics = integration.collectCircuitBreakerMetrics();
            String executionId = "html-test-" + System.currentTimeMillis();
            Instant now = Instant.now();
            TestMetricsDashboard.TestExecution execution = new TestMetricsDashboard.TestExecution(
                executionId, "circuit-breaker-tests", "test", 
                now.minus(Duration.ofMinutes(5)), now, metrics, 
                TestMetricsDashboard.ExecutionStatus.SUCCESS, Map.of());
            
            dashboard.recordExecution(execution);
            
            // Generate HTML dashboard
            String html = dashboard.generateHtmlDashboard();
            
            // Verify HTML contains circuit breaker specific content
            assertThat(html).isNotEmpty();
            assertThat(html).contains("Circuit Breaker Tests");
            assertThat(html).contains("MCP Test Metrics Dashboard");
            assertThat(html).contains("overview-cards");
            assertThat(html).contains("suites-section");
            
            // Verify the HTML is well-formed
            assertThat(html).startsWith("<!DOCTYPE html>");
            assertThat(html).endsWith("</html>");
            assertThat(html).contains("<head>");
            assertThat(html).contains("</head>");
            assertThat(html).contains("<body>");
            assertThat(html).contains("</body>");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large number of circuit breaker metrics efficiently")
        void shouldHandleLargeMetricsSet() {
            // Create a large number of circuit breakers with metrics
            int numberOfCircuitBreakers = 100;
            int executionsPerCircuitBreaker = 1000;
            
            long startTime = System.currentTimeMillis();
            
            for (int cb = 0; cb < numberOfCircuitBreakers; cb++) {
                String cbName = "performance-test-cb-" + cb;
                
                for (int exec = 0; exec < executionsPerCircuitBreaker; exec++) {
                    if (exec % 10 == 0) {
                        // 10% failure rate
                        metricsCollector.recordFailedExecution(cbName, Duration.ofMillis(200), 
                            new RuntimeException("Performance test failure"));
                    } else {
                        metricsCollector.recordSuccessfulExecution(cbName, Duration.ofMillis(100));
                    }
                }
            }
            
            // Collect metrics
            TestMetricsDashboard.TestMetrics aggregateMetrics = integration.collectCircuitBreakerMetrics();
            Map<String, TestMetricsDashboard.TestMetrics> individualMetrics = 
                integration.collectIndividualCircuitBreakerMetrics();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Verify performance
            assertThat(duration).isLessThan(5000L); // Should complete within 5 seconds
            
            // Verify correctness
            assertThat(aggregateMetrics.getTotalTests()).isEqualTo(numberOfCircuitBreakers * executionsPerCircuitBreaker);
            assertThat(individualMetrics).hasSize(numberOfCircuitBreakers);
            
            // Verify individual metrics are correct
            for (TestMetricsDashboard.TestMetrics metrics : individualMetrics.values()) {
                assertThat(metrics.getTotalTests()).isEqualTo(executionsPerCircuitBreaker);
                assertThat(metrics.getPassedTests()).isEqualTo((int)(executionsPerCircuitBreaker * 0.9));
                assertThat(metrics.getFailedTests()).isEqualTo((int)(executionsPerCircuitBreaker * 0.1));
            }
        }
    }
}