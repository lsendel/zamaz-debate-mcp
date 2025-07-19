package com.zamaz.mcp.sidecar.integration;

import com.zamaz.mcp.sidecar.service.*;
import com.zamaz.mcp.sidecar.service.AdvancedRateLimitingService.UserTier;
import com.zamaz.mcp.sidecar.service.DistributedCircuitBreakerService.State;
import com.zamaz.mcp.sidecar.service.AuditLoggingService.AuditEventType;
import com.zamaz.mcp.sidecar.service.AuditLoggingService.AuditSeverity;
import com.zamaz.mcp.sidecar.service.AuditLoggingService.AuditOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive integration tests for MCP Sidecar components
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
public class SidecarIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AdvancedRateLimitingService rateLimitingService;

    @Autowired
    private DistributedCircuitBreakerService circuitBreakerService;

    @Autowired
    private AuditLoggingService auditLoggingService;

    @Autowired
    private AlertingService alertingService;

    @Autowired
    private AdvancedRequestRoutingService routingService;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @MockBean
    private MetricsCollectorService metricsCollectorService;

    private final AtomicInteger userIdCounter = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        // Initialize services
        rateLimitingService.initializeDefaultProfiles();
        circuitBreakerService.initializeDefaultConfigurations();
        alertingService.initializeDefaultRules();
        routingService.initializeDefaultClusters();
        
        // Clear Redis
        redisTemplate.getConnectionFactory().getReactiveConnection().flushAll().block();
        
        // Mock metrics service
        when(metricsCollectorService.recordRequest(anyString(), anyString(), any(Long.class), any(Integer.class)))
                .thenReturn(Mono.empty());
        when(metricsCollectorService.recordRateLimitHit(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(metricsCollectorService.recordCircuitBreakerSuccess(anyString()))
                .thenReturn(Mono.empty());
        when(metricsCollectorService.recordCircuitBreakerFailure(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(metricsCollectorService.recordAuditEvent(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRateLimitingIntegration() {
        String userId = "test-user-" + userIdCounter.incrementAndGet();
        
        // Test setting user tier
        StepVerifier.create(rateLimitingService.setUserTier(userId, UserTier.PREMIUM))
                .verifyComplete();
        
        // Test getting user tier
        StepVerifier.create(rateLimitingService.getUserTier(userId))
                .assertNext(tier -> assertThat(tier).isEqualTo(UserTier.PREMIUM))
                .verifyComplete();
        
        // Test rate limit check
        StepVerifier.create(rateLimitingService.checkRateLimit(userId, "/api/v1/test", "GET"))
                .assertNext(result -> {
                    assertThat(result.isAllowed()).isTrue();
                    assertThat(result.getTier()).isEqualTo(UserTier.PREMIUM);
                })
                .verifyComplete();
        
        // Test REST API
        webTestClient.get()
                .uri("/api/v1/rate-limits/users/{userId}/tier", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tier").isEqualTo("PREMIUM")
                .jsonPath("$.displayName").isEqualTo("Premium");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCircuitBreakerIntegration() {
        String circuitBreakerName = "test-service";
        
        // Test circuit breaker execution
        StepVerifier.create(circuitBreakerService.executeWithCircuitBreaker(
                circuitBreakerName, 
                () -> Mono.just("success")
        ))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getResult()).isEqualTo("success");
                    assertThat(result.getState()).isEqualTo(State.CLOSED);
                })
                .verifyComplete();
        
        // Test circuit breaker with failure
        StepVerifier.create(circuitBreakerService.executeWithCircuitBreaker(
                circuitBreakerName,
                () -> Mono.error(new RuntimeException("Test failure"))
        ))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getError()).contains("Test failure");
                })
                .verifyComplete();
        
        // Test REST API
        webTestClient.get()
                .uri("/api/v1/circuit-breakers/{name}", circuitBreakerName)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo(circuitBreakerName)
                .jsonPath("$.state").isEqualTo("CLOSED");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAuditLoggingIntegration() {
        // Test audit event creation
        AuditLoggingService.AuditEvent event = AuditLoggingService.builder()
                .eventType(AuditEventType.API_ACCESS)
                .severity(AuditSeverity.MEDIUM)
                .outcome(AuditOutcome.SUCCESS)
                .userId("test-user")
                .action("GET")
                .resource("/api/v1/test")
                .description("Test API access")
                .build();
        
        StepVerifier.create(auditLoggingService.logAuditEvent(event))
                .verifyComplete();
        
        // Test audit statistics
        StepVerifier.create(auditLoggingService.getAuditStatistics())
                .assertNext(stats -> {
                    assertThat(stats.get("totalEvents")).isEqualTo(1L);
                    assertThat(stats.get("bufferedEvents")).isEqualTo(1);
                })
                .verifyComplete();
        
        // Test REST API
        webTestClient.get()
                .uri("/api/v1/audit/statistics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalEvents").isEqualTo(1)
                .jsonPath("$.bufferedEvents").isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testEndToEndIntegration() {
        String userId = "e2e-test-user";
        
        // 1. Set up user with PREMIUM tier
        StepVerifier.create(rateLimitingService.setUserTier(userId, UserTier.PREMIUM))
                .verifyComplete();
        
        // 2. Make requests through rate limiting
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(rateLimitingService.checkRateLimit(userId, "/api/v1/test", "GET"))
                    .assertNext(result -> assertThat(result.isAllowed()).isTrue())
                    .verifyComplete();
        }
        
        // 3. Log audit events for the requests
        for (int i = 0; i < 5; i++) {
            AuditLoggingService.AuditEvent event = AuditLoggingService.builder()
                    .eventType(AuditEventType.API_ACCESS)
                    .severity(AuditSeverity.LOW)
                    .outcome(AuditOutcome.SUCCESS)
                    .userId(userId)
                    .action("GET")
                    .resource("/api/v1/test")
                    .requestId("req-" + i)
                    .build();
            
            StepVerifier.create(auditLoggingService.logAuditEvent(event))
                    .verifyComplete();
        }
        
        // 4. Test circuit breaker with successful requests
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(circuitBreakerService.executeWithCircuitBreaker(
                    "test-service",
                    () -> Mono.just("success-" + i)
            ))
                    .assertNext(result -> assertThat(result.isSuccess()).isTrue())
                    .verifyComplete();
        }
        
        // 5. Test routing for the requests
        StepVerifier.create(routingService.routeRequest(
                "/api/v1/test",
                Map.of("User-Agent", "test-client"),
                "session-" + userId
        ))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getInstance()).isNotNull();
                })
                .verifyComplete();
        
        // 6. Verify final state
        StepVerifier.create(rateLimitingService.getUserUsageStats(userId))
                .assertNext(stats -> {
                    assertThat(stats.getTotalUsage()).isNotEmpty();
                })
                .verifyComplete();
        
        StepVerifier.create(auditLoggingService.getAuditStatistics())
                .assertNext(stats -> {
                    assertThat((Long) stats.get("totalEvents")).isGreaterThan(0);
                })
                .verifyComplete();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testHealthChecks() {
        // Test rate limiting health
        webTestClient.get()
                .uri("/api/v1/rate-limits/statistics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rateLimitingEnabled").isEqualTo(true);
        
        // Test circuit breaker health
        webTestClient.get()
                .uri("/api/v1/circuit-breakers/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
        
        // Test routing health
        webTestClient.get()
                .uri("/api/v1/routing/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
        
        // Test audit health
        webTestClient.get()
                .uri("/api/v1/audit/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
        
        // Test alerts health
        webTestClient.get()
                .uri("/api/v1/alerts/statistics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalRules").isNumber();
    }
}