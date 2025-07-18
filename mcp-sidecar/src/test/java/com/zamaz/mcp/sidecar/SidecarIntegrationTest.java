package com.zamaz.mcp.sidecar;

import com.zamaz.mcp.sidecar.controller.AuthController;
import com.zamaz.mcp.sidecar.service.AuthenticationService;
import com.zamaz.mcp.sidecar.service.RBACIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for MCP Sidecar
 * 
 * Tests the complete sidecar functionality including authentication,
 * authorization, fallback mechanisms, and service integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.security.user.name=test",
        "spring.security.user.password=test123",
        "spring.security.user.roles=USER",
        "jwt.secret=test-secret-key-for-integration-tests",
        "jwt.expiration=3600",
        "SECURITY_SERVICE_URL=http://localhost:8082",
        "ORGANIZATION_SERVICE_URL=http://localhost:5005",
        "LLM_SERVICE_URL=http://localhost:5002",
        "CONTROLLER_SERVICE_URL=http://localhost:5013",
        "RAG_SERVICE_URL=http://localhost:5004"
})
class SidecarIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RBACIntegrationService rbacIntegrationService;

    @Test
    void testAuthenticationEndpoint() {
        // Test login endpoint
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("test");
        loginRequest.setPassword("test123");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.username").isEqualTo("test");
    }

    @Test
    void testAuthenticationWithInvalidCredentials() {
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("invalid");
        loginRequest.setPassword("invalid");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testAuthHealthEndpoint() {
        webTestClient.get()
                .uri("/api/v1/auth/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("Authentication service is healthy");
    }

    @Test
    void testFallbackEndpoints() {
        // Test organization fallback
        webTestClient.get()
                .uri("/fallback/organization")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("mcp-organization")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();

        // Test LLM fallback
        webTestClient.get()
                .uri("/fallback/llm")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("AI_SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("mcp-llm");

        // Test debate fallback
        webTestClient.get()
                .uri("/fallback/debate")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("DEBATE_SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("mcp-controller");

        // Test RAG fallback
        webTestClient.get()
                .uri("/fallback/rag")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("RAG_SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("mcp-rag");

        // Test generic fallback
        webTestClient.get()
                .uri("/fallback/unknown-service")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.service").isEqualTo("unknown-service");
    }

    @Test
    void testHealthFallback() {
        webTestClient.get()
                .uri("/fallback/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("DEGRADED")
                .jsonPath("$.services.sidecar").isEqualTo("UP")
                .jsonPath("$.services.downstream").isEqualTo("DEGRADED");
    }

    @Test
    void testRBACIntegration() {
        // Mock RBAC service responses
        when(rbacIntegrationService.validatePermission(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(true));
        
        when(rbacIntegrationService.getUserRoles(anyString(), anyString()))
                .thenReturn(Mono.just(List.of("USER")));
        
        when(rbacIntegrationService.getUserPermissions(anyString(), anyString()))
                .thenReturn(Mono.just(List.of("DEBATE_VIEW", "DEBATE_CREATE")));
        
        when(rbacIntegrationService.validateOrganizationMembership(anyString(), anyString()))
                .thenReturn(Mono.just(true));

        // Test permission validation
        webTestClient.get()
                .uri("/api/v1/rbac/validate-permission")
                .header("X-User-ID", "test-user")
                .header("X-Organization-ID", "test-org")
                .header("X-Permission", "DEBATE_VIEW")
                .header("X-Resource", "debate")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testSecurityConfiguration() {
        // Test that security endpoints are properly configured
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();

        // Test that protected endpoints require authentication
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testCorsConfiguration() {
        webTestClient.options()
                .uri("/api/v1/auth/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Access-Control-Allow-Origin");
    }

    @Test
    void testActuatorEndpoints() {
        // Test health endpoint
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();

        // Test info endpoint
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();

        // Test metrics endpoint
        webTestClient.get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.names").exists();
    }

    @Test
    void testPrometheusMetrics() {
        // Test prometheus metrics endpoint
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/plain;version=0.0.4;charset=utf-8");
    }

    @Test
    void testRequestValidation() {
        // Test validation on login request
        AuthController.LoginRequest emptyRequest = new AuthController.LoginRequest();
        
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(emptyRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // Test validation on refresh token request
        AuthController.RefreshTokenRequest emptyRefreshRequest = new AuthController.RefreshTokenRequest();
        
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .bodyValue(emptyRefreshRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testErrorHandling() {
        // Test that proper error responses are returned
        webTestClient.get()
                .uri("/api/v1/auth/nonexistent")
                .exchange()
                .expectStatus().isNotFound();

        // Test method not allowed
        webTestClient.post()
                .uri("/api/v1/auth/health")
                .exchange()
                .expectStatus().isMethodNotAllowed();
    }

    @Test
    void testRateLimiting() {
        // Test rate limiting (this would require Redis to be properly configured)
        // For now, just verify the endpoint exists
        webTestClient.get()
                .uri("/api/v1/auth/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testCircuitBreakerConfiguration() {
        // Test that circuit breaker is properly configured
        // This would require actual service calls to test properly
        // For now, verify the application starts correctly with circuit breaker config
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}