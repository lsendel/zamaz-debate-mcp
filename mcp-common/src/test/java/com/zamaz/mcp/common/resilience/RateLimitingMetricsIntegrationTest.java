package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.config.PrometheusMetricsConfiguration;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for rate limiting metrics with Micrometer/Prometheus.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestProfile(category = "integration", priority = "high")
@DisplayName("Rate Limiting Metrics Integration Tests")
class RateLimitingMetricsIntegrationTest {

    @Mock
    private McpRateLimitingService rateLimitingService;

    @Mock
    private McpRateLimitingConfiguration rateLimitingConfig;

    private MeterRegistry meterRegistry;
    private McpRateLimitingController controller;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        controller = new McpRateLimitingController(rateLimitingService, rateLimitingConfig);
        
        // Inject meter registry using reflection
        try {
            java.lang.reflect.Field meterRegistryField = McpRateLimitingController.class.getDeclaredField("meterRegistry");
            meterRegistryField.setAccessible(true);
            meterRegistryField.set(controller, meterRegistry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject meter registry", e);
        }
        
        // Initialize metrics
        controller.initializeMetrics();
    }

    @Nested
    @DisplayName("Metrics Collection Tests")
    class MetricsCollectionTests {

        @Test
        @DisplayName("Should initialize gauges for rate limiting metrics")
        void shouldInitializeGauges() {
            // Verify gauges are registered
            assertThat(meterRegistry.find("rate_limiter.active").gauge()).isNotNull();
            assertThat(meterRegistry.find("rate_limiter.violations.total").gauge()).isNotNull();
        }

        @Test
        @DisplayName("Should record rate limit request metrics")
        void shouldRecordRateLimitRequestMetrics() {
            // Record successful request
            controller.recordRateLimitRequest("test-key", true);
            
            // Verify counter was incremented
            Counter requestCounter = meterRegistry.find("rate_limiter.requests")
                .tag("key", "test-key")
                .counter();
            assertThat(requestCounter).isNotNull();
            assertThat(requestCounter.count()).isEqualTo(1.0);
            
            // Record failed request
            controller.recordRateLimitRequest("test-key", false);
            
            // Verify violation counter was incremented
            Counter violationCounter = meterRegistry.find("rate_limiter.violations")
                .tag("key", "test-key")
                .counter();
            assertThat(violationCounter).isNotNull();
            assertThat(violationCounter.count()).isEqualTo(1.0);
            
            // Verify request counter was incremented again
            assertThat(requestCounter.count()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should record timing metrics for rate limit operations")
        void shouldRecordTimingMetrics() throws InterruptedException {
            // Record timed operation
            controller.recordRateLimitTiming("test-operation", () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            // Verify timer was recorded
            io.micrometer.core.instrument.Timer timer = meterRegistry.find("rate_limiter.operation.duration")
                .tag("key", "test-operation")
                .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Metrics Endpoint Tests")
    class MetricsEndpointTests {

        @Test
        @DisplayName("Should return comprehensive metrics from metrics endpoint")
        void shouldReturnComprehensiveMetrics() {
            // Setup mock data
            Map<String, McpRateLimitingService.RateLimitMetrics> serviceMetrics = new HashMap<>();
            
            McpRateLimitingService.RateLimitMetrics metrics1 = new McpRateLimitingService.RateLimitMetrics();
            metrics1.setRequests(100);
            metrics1.setViolations(10);
            metrics1.setCapacity(100);
            metrics1.setAvailablePermits(50);
            serviceMetrics.put("service1:tool1", metrics1);
            
            McpRateLimitingService.RateLimitMetrics metrics2 = new McpRateLimitingService.RateLimitMetrics();
            metrics2.setRequests(200);
            metrics2.setViolations(5);
            metrics2.setCapacity(100);
            metrics2.setAvailablePermits(100);
            serviceMetrics.put("service2:tool2", metrics2);
            
            when(rateLimitingService.getAllRateLimitMetrics()).thenReturn(serviceMetrics);
            when(rateLimitingConfig.isEnabled()).thenReturn(true);
            when(rateLimitingConfig.getDefaultRateLimit()).thenReturn(100);
            when(rateLimitingConfig.getDefaultBurstLimit()).thenReturn(10);
            when(rateLimitingConfig.getCleanupIntervalSeconds()).thenReturn(300);
            
            // Call metrics endpoint
            Map<String, Object> response = controller.getRateLimitingMetrics().getBody();
            
            // Verify response
            assertThat(response).isNotNull();
            assertThat(response).containsKey("totalRateLimiters");
            assertThat(response).containsKey("activeUsers");
            assertThat(response).containsKey("totalRequests");
            assertThat(response).containsKey("totalViolations");
            assertThat(response).containsKey("violationRate");
            assertThat(response).containsKey("micrometerIntegrated");
            
            assertThat(response.get("totalRateLimiters")).isEqualTo(2);
            assertThat(response.get("activeUsers")).isEqualTo(1L); // service1 has used permits
            assertThat(response.get("totalRequests")).isEqualTo(300L);
            assertThat(response.get("totalViolations")).isEqualTo(15L);
            assertThat(response.get("violationRate")).isEqualTo(5.0);
            assertThat(response.get("micrometerIntegrated")).isEqualTo(true);
            
            // Verify metrics were recorded to Micrometer
            assertThat(meterRegistry.find("rate_limiter.violation.rate").gauge()).isNotNull();
            assertThat(meterRegistry.find("rate_limiter.users.active").gauge()).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty metrics gracefully")
        void shouldHandleEmptyMetrics() {
            // Setup empty metrics
            when(rateLimitingService.getAllRateLimitMetrics()).thenReturn(Collections.emptyMap());
            when(rateLimitingConfig.isEnabled()).thenReturn(true);
            when(rateLimitingConfig.getDefaultRateLimit()).thenReturn(100);
            when(rateLimitingConfig.getDefaultBurstLimit()).thenReturn(10);
            when(rateLimitingConfig.getCleanupIntervalSeconds()).thenReturn(300);
            
            // Call metrics endpoint
            Map<String, Object> response = controller.getRateLimitingMetrics().getBody();
            
            // Verify response
            assertThat(response).isNotNull();
            assertThat(response.get("totalRateLimiters")).isEqualTo(0);
            assertThat(response.get("activeUsers")).isEqualTo(0L);
            assertThat(response.get("totalRequests")).isEqualTo(0L);
            assertThat(response.get("totalViolations")).isEqualTo(0L);
            assertThat(response.get("violationRate")).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Status Endpoint Metrics Tests")
    class StatusEndpointMetricsTests {

        @Test
        @DisplayName("Should record metrics when getting rate limit status")
        void shouldRecordMetricsForStatusCheck() {
            // Setup authentication
            Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", 
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            // Setup mock response
            Map<String, McpRateLimitingService.RateLimitStatus> statusMap = new HashMap<>();
            McpRateLimitingService.RateLimitStatus status = new McpRateLimitingService.RateLimitStatus();
            status.setAllowed(true);
            status.setAvailablePermits(10);
            status.setLimit(100);
            statusMap.put("service:tool", status);
            
            when(rateLimitingService.getAllRateLimitStatus(any())).thenReturn(statusMap);
            
            // Call status endpoint
            controller.getRateLimitStatus(auth);
            
            // Verify metrics were recorded
            Counter statusCheckCounter = meterRegistry.find("rate_limiter.requests")
                .tag("key", "status_check_testuser")
                .counter();
            assertThat(statusCheckCounter).isNotNull();
            assertThat(statusCheckCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Integration with PrometheusMetricsConfiguration")
    class PrometheusIntegrationTests {

        @Test
        @DisplayName("Should work with Prometheus configuration")
        void shouldWorkWithPrometheusConfiguration() {
            // Create configuration
            PrometheusMetricsConfiguration config = new PrometheusMetricsConfiguration();
            
            // Test common tags customizer
            MeterRegistryCustomizer<MeterRegistry> commonTagsCustomizer = 
                config.metricsCommonTags(mockEnvironment());
            commonTagsCustomizer.customize(meterRegistry);
            
            // Test rate limiting customizer
            MeterRegistryCustomizer<MeterRegistry> rateLimitingCustomizer = 
                config.rateLimitingMetricsCustomizer();
            rateLimitingCustomizer.customize(meterRegistry);
            
            // Record a metric and verify tags are applied
            controller.recordRateLimitRequest("test-key", true);
            
            Counter counter = meterRegistry.find("rate_limiter.requests")
                .tag("key", "test-key")
                .counter();
            assertThat(counter).isNotNull();
        }

        private org.springframework.core.env.Environment mockEnvironment() {
            org.springframework.core.env.Environment env = mock(org.springframework.core.env.Environment.class);
            when(env.getProperty("spring.application.name", "mcp-common")).thenReturn("test-app");
            when(env.getProperty("spring.profiles.active", "default")).thenReturn("test");
            return env;
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle high volume of metrics efficiently")
        void shouldHandleHighVolumeMetrics() {
            int numberOfOperations = 10000;
            
            long startTime = System.currentTimeMillis();
            
            // Record many metrics
            for (int i = 0; i < numberOfOperations; i++) {
                String key = "key-" + (i % 100); // Use 100 different keys
                boolean allowed = i % 10 != 0; // 10% violations
                
                controller.recordRateLimitRequest(key, allowed);
                
                if (i % 100 == 0) {
                    controller.recordRateLimitTiming(key, () -> {
                        // Simulate quick operation
                    });
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Verify performance
            assertThat(duration).isLessThan(1000L); // Should complete within 1 second
            
            // Verify metrics were recorded
            assertThat(meterRegistry.getMeters()).isNotEmpty();
            
            // Check a sample counter
            Counter sampleCounter = meterRegistry.find("rate_limiter.requests")
                .tag("key", "key-0")
                .counter();
            assertThat(sampleCounter).isNotNull();
            assertThat(sampleCounter.count()).isGreaterThan(0);
        }
    }
}