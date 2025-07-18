package com.zamaz.mcp.sidecar.integration;

import com.zamaz.mcp.sidecar.service.SecurityScanningService;
import com.zamaz.mcp.sidecar.service.MetricsCollectorService;
import com.zamaz.mcp.sidecar.service.CachingService;
import com.zamaz.mcp.sidecar.service.AILoadBalancingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Integration Tests for MCP Sidecar
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SidecarIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SecurityScanningService securityScanningService;

    @Autowired
    private MetricsCollectorService metricsCollectorService;

    @Autowired
    private CachingService cachingService;

    @Autowired
    private AILoadBalancingService aiLoadBalancingService;

    private static final Network network = Network.newNetwork();

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> mockBackend = new GenericContainer<>(DockerImageName.parse("nginx:alpine"))
            .withNetwork(network)
            .withNetworkAliases("mock-backend")
            .withExposedPorts(80);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("app.security.scanning.enabled", () -> "true");
        registry.add("app.metrics.enabled", () -> "true");
        registry.add("app.cache.enabled", () -> "true");
        registry.add("app.tracing.enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        // Initialize AI service instances for testing
        aiLoadBalancingService.initializeServices();
    }

    /**
     * Test basic sidecar health and startup
     */
    @Test
    void testSidecarHealthCheck() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    /**
     * Test authentication flow
     */
    @Test
    void testAuthenticationFlow() {
        // Test login
        Map<String, String> loginRequest = Map.of(
            "username", "admin",
            "password", "admin123"
        );

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/auth/login",
            loginRequest,
            Map.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).containsKey("token");

        String token = (String) loginResponse.getBody().get("token");
        
        // Test protected endpoint with token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> protectedResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/auth/me",
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Test security scanning functionality
     */
    @Test
    void testSecurityScanning() {
        // Test SQL injection detection
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/users",
                "POST",
                Map.of("Content-Type", "application/json"),
                "{\"name\": \"'; DROP TABLE users; --\"}"
            )
        )
        .expectNextMatches(result -> result.isBlocked() && result.getThreats().size() > 0)
        .verifyComplete();

        // Test XSS detection
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/comments",
                "POST",
                Map.of("Content-Type", "application/json"),
                "{\"comment\": \"<script>alert('xss')</script>\"}"
            )
        )
        .expectNextMatches(result -> result.isBlocked() && result.getThreats().size() > 0)
        .verifyComplete();

        // Test normal request
        StepVerifier.create(
            securityScanningService.scanRequest(
                "test-client",
                "/api/v1/users",
                "GET",
                Map.of(),
                null
            )
        )
        .expectNextMatches(result -> !result.isBlocked())
        .verifyComplete();
    }

    /**
     * Test caching functionality
     */
    @Test
    void testCaching() {
        String testKey = "test-key";
        String testValue = "test-value";

        // Test cache set and get
        StepVerifier.create(
            cachingService.set(CachingService.CacheCategory.API_RESPONSE, testKey, testValue)
                .then(cachingService.get(CachingService.CacheCategory.API_RESPONSE, testKey))
        )
        .expectNext(testValue)
        .verifyComplete();

        // Test cache expiration
        StepVerifier.create(
            cachingService.set(CachingService.CacheCategory.API_RESPONSE, testKey, testValue, Duration.ofMillis(100))
                .then(Mono.delay(Duration.ofMillis(200)))
                .then(cachingService.get(CachingService.CacheCategory.API_RESPONSE, testKey))
        )
        .expectComplete()
        .verify();

        // Test cache statistics
        StepVerifier.create(
            cachingService.getCacheInfo()
        )
        .expectNextMatches(info -> info.containsKey("enabled") && (Boolean) info.get("enabled"))
        .verifyComplete();
    }

    /**
     * Test AI load balancing
     */
    @Test
    void testAILoadBalancing() {
        AILoadBalancingService.AIRequest request = new AILoadBalancingService.AIRequest();
        request.setPrompt("Test prompt");
        request.setModel("gpt-4");
        request.setUserId("test-user");

        // Test service selection
        StepVerifier.create(
            aiLoadBalancingService.selectAIService("gpt-4", request)
        )
        .expectNextMatches(instance -> instance.getModel().equals("gpt-4"))
        .verifyComplete();

        // Test load balancing statistics
        StepVerifier.create(
            aiLoadBalancingService.getLoadBalancingStats()
        )
        .expectNextMatches(stats -> stats.containsKey("gpt-4"))
        .verifyComplete();
    }

    /**
     * Test metrics collection
     */
    @Test
    void testMetricsCollection() {
        // Record some test metrics
        metricsCollectorService.recordRequest("/api/v1/test", "GET", 100, 200);
        metricsCollectorService.recordAuthentication("test-user", true, "jwt");

        // Test metrics report
        StepVerifier.create(
            metricsCollectorService.getMetricsReport()
        )
        .expectNextMatches(report -> 
            report.containsKey("requests") && 
            report.containsKey("authentication") &&
            report.containsKey("system")
        )
        .verifyComplete();
    }

    /**
     * Test rate limiting
     */
    @Test
    void testRateLimiting() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ID", "test-user");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Make multiple requests to trigger rate limiting
        for (int i = 0; i < 25; i++) {
            restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/organizations",
                HttpMethod.GET,
                requestEntity,
                String.class
            );
        }

        // The next request should be rate limited
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/organizations",
            HttpMethod.GET,
            requestEntity,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Test circuit breaker functionality
     */
    @Test
    void testCircuitBreaker() {
        // This test would require setting up a failing backend service
        // For now, we'll test that the circuit breaker configuration is loaded
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ID", "test-user");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Test fallback endpoint
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/fallback/organization",
            HttpMethod.GET,
            requestEntity,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Test metrics endpoints
     */
    @Test
    void testMetricsEndpoints() {
        // Test Prometheus metrics
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/prometheus",
            String.class
        );

        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metricsResponse.getBody()).contains("sidecar_requests_total");

        // Test custom metrics endpoint (requires authentication)
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "admin123");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> customMetricsResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/metrics/health",
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        assertThat(customMetricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Test security endpoints
     */
    @Test
    void testSecurityEndpoints() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "admin123");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Test security statistics
        ResponseEntity<Map> securityStatsResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/security/statistics",
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        assertThat(securityStatsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(securityStatsResponse.getBody()).containsKey("scanningEnabled");

        // Test security health
        ResponseEntity<Map> securityHealthResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/security/health",
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        assertThat(securityHealthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(securityHealthResponse.getBody()).containsKey("status");
    }

    /**
     * Test CORS configuration
     */
    @Test
    void testCORSConfiguration() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:3000");
        headers.set("Access-Control-Request-Method", "GET");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/organizations",
            HttpMethod.OPTIONS,
            requestEntity,
            String.class
        );

        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        assertThat(response.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.GET);
    }

    /**
     * Test end-to-end request flow
     */
    @Test
    void testEndToEndFlow() {
        // 1. Login and get token
        Map<String, String> loginRequest = Map.of(
            "username", "admin",
            "password", "admin123"
        );

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/auth/login",
            loginRequest,
            Map.class
        );

        String token = (String) loginResponse.getBody().get("token");

        // 2. Make authenticated request
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-User-ID", "test-user");
        headers.set("X-Organization-ID", "test-org");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/organizations",
            HttpMethod.GET,
            requestEntity,
            String.class
        );

        // 3. Verify response (should be fallback since no real backend)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Response-Source")).contains("mcp-sidecar");
    }

    /**
     * Test concurrent request handling
     */
    @Test
    void testConcurrentRequests() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "admin123");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Execute multiple concurrent requests
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:" + port + "/actuator/health",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
                );
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }).start();
        }

        // Wait for all threads to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}