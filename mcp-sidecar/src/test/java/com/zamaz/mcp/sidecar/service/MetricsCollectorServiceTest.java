package com.zamaz.mcp.sidecar.service;

import com.zamaz.mcp.sidecar.config.TracingConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetricsCollectorService
 */
@ExtendWith(MockitoExtension.class)
class MetricsCollectorServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CachingService cachingService;

    @Mock
    private AILoadBalancingService aiLoadBalancingService;

    @Mock
    private TracingConfig.TracingService tracingService;

    private MeterRegistry meterRegistry;
    private MetricsCollectorService metricsCollectorService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        metricsCollectorService = new MetricsCollectorService(
            meterRegistry,
            redisTemplate,
            webClientBuilder,
            cachingService,
            aiLoadBalancingService,
            tracingService
        );
    }

    @Test
    void testRecordRequest() {
        String endpoint = "/api/v1/users";
        String method = "GET";
        long duration = 100L;
        int statusCode = 200;

        // This should not throw any exception
        metricsCollectorService.recordRequest(endpoint, method, duration, statusCode);

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("sidecar.requests.total").count()).isEqualTo(1.0);
    }

    @Test
    void testRecordRequestWithError() {
        String endpoint = "/api/v1/users";
        String method = "GET";
        long duration = 100L;
        int statusCode = 500;

        metricsCollectorService.recordRequest(endpoint, method, duration, statusCode);

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("sidecar.requests.total").count()).isEqualTo(1.0);
    }

    @Test
    void testRecordAuthentication() {
        String userId = "user123";
        boolean success = true;
        String method = "jwt";

        metricsCollectorService.recordAuthentication(userId, success, method);

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("sidecar.auth.success").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("sidecar.auth.failure").count()).isEqualTo(0.0);
    }

    @Test
    void testRecordAuthenticationFailure() {
        String userId = "user123";
        boolean success = false;
        String method = "jwt";

        metricsCollectorService.recordAuthentication(userId, success, method);

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("sidecar.auth.success").count()).isEqualTo(0.0);
        assertThat(meterRegistry.counter("sidecar.auth.failure").count()).isEqualTo(1.0);
    }

    @Test
    void testRecordCircuitBreakerTrip() {
        String serviceName = "organization-service";
        String reason = "High error rate";

        metricsCollectorService.recordCircuitBreakerTrip(serviceName, reason);

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("sidecar.circuit_breaker.trips").count()).isEqualTo(1.0);
    }

    @Test
    void testRecordRateLimitHit() {
        String userId = "user123";
        String endpoint = "/api/v1/users";
        String rateLimitType = "user";

        metricsCollectorService.recordRateLimitHit(userId, endpoint, rateLimitType);

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("sidecar.rate_limit.hits").count()).isEqualTo(1.0);
    }

    @Test
    void testRecordAIResponse() {
        String model = "gpt-4";
        String userId = "user123";
        long duration = 1000L;
        boolean success = true;

        metricsCollectorService.recordAIResponse(model, userId, duration, success);

        // Verify metrics were recorded
        assertThat(meterRegistry.timer("sidecar.ai.response.duration").count()).isEqualTo(1L);
    }

    @Test
    void testGetMetricsReport() {
        // Mock dependencies
        when(cachingService.getCacheStatistics()).thenReturn(Map.of());
        when(aiLoadBalancingService.getLoadBalancingStats()).thenReturn(Mono.just(Map.of()));
        when(tracingService.getTracingStatistics()).thenReturn(Map.of());

        StepVerifier.create(
            metricsCollectorService.getMetricsReport()
        )
        .expectNextMatches(report -> {
            assertThat(report).containsKey("requests");
            assertThat(report).containsKey("authentication");
            assertThat(report).containsKey("serviceHealth");
            assertThat(report).containsKey("system");
            assertThat(report).containsKey("aiServices");
            assertThat(report).containsKey("cache");
            assertThat(report).containsKey("tracing");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testGetActiveConnections() {
        // Record some requests first
        metricsCollectorService.recordRequest("/api/v1/users", "GET", 100, 200);
        metricsCollectorService.recordRequest("/api/v1/organizations", "GET", 200, 200);

        double activeConnections = metricsCollectorService.getActiveConnections();
        assertThat(activeConnections).isGreaterThan(0);
    }

    @Test
    void testGetMemoryUsage() {
        double memoryUsage = metricsCollectorService.getMemoryUsage();
        assertThat(memoryUsage).isGreaterThan(0);
    }

    @Test
    void testGetCacheHitRate() {
        // Mock cache statistics
        when(cachingService.getCacheStatistics()).thenReturn(Map.of());

        double hitRate = metricsCollectorService.getCacheHitRate();
        assertThat(hitRate).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testGetCircuitBreakerState() {
        double state = metricsCollectorService.getCircuitBreakerState();
        assertThat(state).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testGetAIServiceAvailability() {
        // Mock AI service statistics
        Map<String, Object> aiStats = Map.of(
            "gpt-4", Map.of(
                "totalInstances", 3,
                "healthyInstances", 2
            )
        );
        when(aiLoadBalancingService.getLoadBalancingStats()).thenReturn(Mono.just(aiStats));

        double availability = metricsCollectorService.getAIServiceAvailability();
        assertThat(availability).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testCollectServiceHealthMetrics() {
        // Mock web client for health check
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("status", "UP")));

        // This should not throw any exception
        metricsCollectorService.collectServiceHealthMetrics();
    }

    @Test
    void testServiceHealthMetricsRecording() {
        MetricsCollectorService.ServiceHealthMetrics metrics = 
            new MetricsCollectorService.ServiceHealthMetrics();

        // Record successful health check
        metrics.recordHealthCheck(true, 100L);
        assertThat(metrics.getAvailability()).isEqualTo(100.0);
        assertThat(metrics.getAverageResponseTime()).isEqualTo(100.0);

        // Record failed health check
        metrics.recordHealthCheck(false, 200L);
        assertThat(metrics.getAvailability()).isEqualTo(50.0);
        assertThat(metrics.getAverageResponseTime()).isEqualTo(150.0);
    }

    @Test
    void testExportMetrics() {
        // Mock dependencies
        when(cachingService.getCacheStatistics()).thenReturn(Map.of());
        when(aiLoadBalancingService.getLoadBalancingStats()).thenReturn(Mono.just(Map.of()));
        when(tracingService.getTracingStatistics()).thenReturn(Map.of());
        when(valueOperations.set(anyString(), anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(
            metricsCollectorService.exportMetrics()
        )
        .expectComplete()
        .verify();
    }

    @Test
    void testMultipleMetricsRecording() {
        // Record multiple metrics
        for (int i = 0; i < 10; i++) {
            metricsCollectorService.recordRequest("/api/v1/test", "GET", 100, 200);
            metricsCollectorService.recordAuthentication("user" + i, true, "jwt");
        }

        // Verify counters
        assertThat(meterRegistry.counter("sidecar.requests.total").count()).isEqualTo(10.0);
        assertThat(meterRegistry.counter("sidecar.auth.success").count()).isEqualTo(10.0);
        assertThat(meterRegistry.timer("sidecar.request.duration").count()).isEqualTo(10L);
    }
}