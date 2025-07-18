package com.zamaz.mcp.sidecar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SecurityScanningService
 */
@ExtendWith(MockitoExtension.class)
class SecurityScanningServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private MetricsCollectorService metricsCollectorService;

    private SecurityScanningService securityScanningService;

    @BeforeEach
    void setUp() {
        securityScanningService = new SecurityScanningService(
            redisTemplate,
            webClientBuilder,
            metricsCollectorService
        );
    }

    @Test
    void testSQLInjectionDetection() {
        String maliciousPayload = "{\"query\": \"SELECT * FROM users WHERE id = 1 OR '1'='1'\"}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/users",
                "POST",
                Map.of("Content-Type", "application/json"),
                maliciousPayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("SQL_INJECTION");
            assertThat(result.getRiskScore()).isGreaterThan(0);
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testXSSDetection() {
        String maliciousPayload = "{\"comment\": \"<script>alert('XSS')</script>\"}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/comments",
                "POST",
                Map.of("Content-Type", "application/json"),
                maliciousPayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("XSS");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testPathTraversalDetection() {
        String maliciousPath = "/api/v1/files/../../../etc/passwd";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                maliciousPath,
                "GET",
                Map.of(),
                null
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("PATH_TRAVERSAL");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testCommandInjectionDetection() {
        String maliciousPayload = "{\"command\": \"ls -la; rm -rf /\"}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/execute",
                "POST",
                Map.of("Content-Type", "application/json"),
                maliciousPayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("COMMAND_INJECTION");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testLegitimateRequestAllowed() {
        String legitimatePayload = "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/users",
                "POST",
                Map.of("Content-Type", "application/json"),
                legitimatePayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isFalse();
            assertThat(result.getThreats()).isEmpty();
            assertThat(result.getRiskScore()).isEqualTo(0);
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testPayloadSizeLimit() {
        String largePayload = "x".repeat(2000000); // 2MB payload
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/upload",
                "POST",
                Map.of("Content-Type", "application/json"),
                largePayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("PAYLOAD_SIZE");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testMultipleThreatsDetection() {
        String maliciousPayload = "{\"query\": \"SELECT * FROM users WHERE id = 1 OR '1'='1'\", \"comment\": \"<script>alert('XSS')</script>\"}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/data",
                "POST",
                Map.of("Content-Type", "application/json"),
                maliciousPayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).hasSizeGreaterThan(1);
            assertThat(result.getRiskScore()).isGreaterThan(10);
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testSecurityScannerDetection() {
        Map<String, String> suspiciousHeaders = Map.of(
            "User-Agent", "sqlmap/1.0",
            "Content-Type", "application/json"
        );
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/users",
                "GET",
                suspiciousHeaders,
                null
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("SECURITY_SCANNER");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testPrototypePollutionDetection() {
        String maliciousPayload = "{\"__proto__\": {\"admin\": true}}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/config",
                "POST",
                Map.of("Content-Type", "application/json"),
                maliciousPayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("PROTOTYPE_POLLUTION");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testSSRFDetection() {
        String maliciousPayload = "{\"url\": \"http://localhost:8080/admin\"}";
        
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/fetch",
                "POST",
                Map.of("Content-Type", "application/json"),
                maliciousPayload
            )
        )
        .expectNextMatches(result -> {
            assertThat(result.isBlocked()).isTrue();
            assertThat(result.getThreats()).isNotEmpty();
            assertThat(result.getThreats().get(0).getType()).isEqualTo("SSRF");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testGetSecurityStatistics() {
        StepVerifier.create(
            securityScanningService.getSecurityStatistics()
        )
        .expectNextMatches(stats -> {
            assertThat(stats).containsKey("scanningEnabled");
            assertThat(stats).containsKey("strictMode");
            assertThat(stats).containsKey("totalThreats");
            assertThat(stats).containsKey("suspiciousActivities");
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testIPReputationCheck() {
        // Test with threat intelligence disabled
        StepVerifier.create(
            securityScanningService.checkIPReputation("192.168.1.1")
        )
        .expectNext(true) // Should allow when threat intelligence is disabled
        .verifyComplete();
    }
}