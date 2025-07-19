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

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for ServiceMeshIntegrationService
 */
@ExtendWith(MockitoExtension.class)
class ServiceMeshIntegrationServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private MetricsCollectorService metricsCollectorService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private ServiceMeshIntegrationService serviceMeshService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        
        serviceMeshService = new ServiceMeshIntegrationService(
            redisTemplate, metricsCollectorService, webClientBuilder
        );
    }

    @Test
    void shouldGetMeshHealth() {
        // When
        Mono<Map<String, Object>> result = serviceMeshService.getMeshHealth();

        // Then
        StepVerifier.create(result)
            .assertNext(health -> {
                assertThat(health).containsKey("meshEnabled");
                assertThat(health).containsKey("meshType");
                assertThat(health).containsKey("meshHealthy");
                assertThat(health).containsKey("totalServices");
                assertThat(health).containsKey("healthyServices");
                assertThat(health).containsKey("lastDiscovery");
                assertThat(health).containsKey("discoveryInterval");
            })
            .verifyComplete();
    }

    @Test
    void shouldGetDiscoveredServices() {
        // When
        Mono<Map<String, ServiceMeshIntegrationService.ServiceInstance>> result = 
            serviceMeshService.getDiscoveredServices();

        // Then
        StepVerifier.create(result)
            .assertNext(services -> {
                assertThat(services).isNotNull();
                // Services would be empty initially in test
                assertThat(services).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void shouldGetMeshMetrics() {
        // When
        Mono<Map<String, Object>> result = serviceMeshService.getMeshMetrics();

        // Then
        StepVerifier.create(result)
            .assertNext(metrics -> {
                assertThat(metrics).containsKey("averageSuccessRate");
                assertThat(metrics).containsKey("averageLatency");
                assertThat(metrics).containsKey("totalRequests");
                assertThat(metrics).containsKey("serviceMetrics");
            })
            .verifyComplete();
    }

    @Test
    void shouldCreateTrafficPolicy() {
        // Given
        String serviceName = "test-service";
        ServiceMeshIntegrationService.CircuitBreakerConfig circuitBreaker = 
            new ServiceMeshIntegrationService.CircuitBreakerConfig(
                5, Duration.ofSeconds(30), Duration.ofSeconds(30), 50
            );

        ServiceMeshIntegrationService.RetryConfig retry = 
            new ServiceMeshIntegrationService.RetryConfig(
                3, Duration.ofSeconds(5), Set.of("5xx", "gateway-error")
            );

        ServiceMeshIntegrationService.TimeoutConfig timeout = 
            new ServiceMeshIntegrationService.TimeoutConfig(
                Duration.ofSeconds(30), Duration.ofSeconds(300)
            );

        ServiceMeshIntegrationService.LoadBalancingConfig loadBalancing = 
            new ServiceMeshIntegrationService.LoadBalancingConfig(
                ServiceMeshIntegrationService.LoadBalancingConfig.LoadBalancingMethod.ROUND_ROBIN, 
                null
            );

        ServiceMeshIntegrationService.TrafficPolicy policy = 
            new ServiceMeshIntegrationService.TrafficPolicy(
                serviceName + "-policy", "default", Map.of("v1", 100),
                circuitBreaker, retry, timeout, loadBalancing
            );

        // When
        Mono<Boolean> result = serviceMeshService.applyTrafficPolicy(serviceName, policy);

        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void shouldCreateCanaryDeployment() {
        // Given
        String serviceName = "test-service";
        String canaryVersion = "v2";
        int trafficPercent = 10;

        // Create initial traffic policy
        ServiceMeshIntegrationService.CircuitBreakerConfig circuitBreaker = 
            new ServiceMeshIntegrationService.CircuitBreakerConfig(
                5, Duration.ofSeconds(30), Duration.ofSeconds(30), 50
            );

        ServiceMeshIntegrationService.RetryConfig retry = 
            new ServiceMeshIntegrationService.RetryConfig(
                3, Duration.ofSeconds(5), Set.of("5xx", "gateway-error")
            );

        ServiceMeshIntegrationService.TimeoutConfig timeout = 
            new ServiceMeshIntegrationService.TimeoutConfig(
                Duration.ofSeconds(30), Duration.ofSeconds(300)
            );

        ServiceMeshIntegrationService.LoadBalancingConfig loadBalancing = 
            new ServiceMeshIntegrationService.LoadBalancingConfig(
                ServiceMeshIntegrationService.LoadBalancingConfig.LoadBalancingMethod.ROUND_ROBIN, 
                null
            );

        ServiceMeshIntegrationService.TrafficPolicy initialPolicy = 
            new ServiceMeshIntegrationService.TrafficPolicy(
                serviceName + "-policy", "default", Map.of("v1", 100),
                circuitBreaker, retry, timeout, loadBalancing
            );

        // Apply initial policy first
        serviceMeshService.applyTrafficPolicy(serviceName, initialPolicy).block();

        // When
        Mono<Boolean> result = serviceMeshService.createCanaryDeployment(
            serviceName, canaryVersion, trafficPercent
        );

        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();

        // Verify traffic policy was updated
        StepVerifier.create(serviceMeshService.getTrafficPolicies())
            .assertNext(policies -> {
                assertThat(policies).containsKey(serviceName);
                ServiceMeshIntegrationService.TrafficPolicy updatedPolicy = policies.get(serviceName);
                assertThat(updatedPolicy.getTrafficSplit()).containsEntry("v1", 90);
                assertThat(updatedPolicy.getTrafficSplit()).containsEntry(canaryVersion, 10);
            })
            .verifyComplete();
    }

    @Test
    void shouldInjectFault() {
        // Given
        String serviceName = "test-service";
        String faultType = "delay";
        double percentage = 10.0;
        Duration delay = Duration.ofMillis(500);

        // When
        Mono<Boolean> result = serviceMeshService.injectFault(serviceName, faultType, percentage, delay);

        // Then
        StepVerifier.create(result)
            .expectNext(false) // Will be false since service doesn't exist in test
            .verifyComplete();
    }

    @Test
    void shouldExportMeshConfiguration() {
        // When
        Mono<Map<String, Object>> result = serviceMeshService.exportMeshConfiguration();

        // Then
        StepVerifier.create(result)
            .assertNext(config -> {
                assertThat(config).containsKey("meshType");
                assertThat(config).containsKey("meshNamespace");
                assertThat(config).containsKey("discoveryInterval");
                assertThat(config).containsKey("healthCheckInterval");
                assertThat(config).containsKey("trafficPolicies");
                assertThat(config).containsKey("destinationRules");
                assertThat(config).containsKey("virtualServices");
            })
            .verifyComplete();
    }

    @Test
    void shouldCreateServiceInstance() {
        // Given
        String name = "test-service";
        String namespace = "default";
        String version = "v1";
        String endpoint = "http://localhost:8080";
        Map<String, String> labels = Map.of("app", name, "version", version);
        Map<String, String> annotations = Map.of("sidecar.istio.io/inject", "true");

        // When
        ServiceMeshIntegrationService.ServiceInstance instance = 
            new ServiceMeshIntegrationService.ServiceInstance(
                name, namespace, version, endpoint, labels, annotations
            );

        // Then
        assertThat(instance.getName()).isEqualTo(name);
        assertThat(instance.getNamespace()).isEqualTo(namespace);
        assertThat(instance.getVersion()).isEqualTo(version);
        assertThat(instance.getEndpoint()).isEqualTo(endpoint);
        assertThat(instance.getLabels()).isEqualTo(labels);
        assertThat(instance.getAnnotations()).isEqualTo(annotations);
        assertThat(instance.isHealthy()).isTrue();
        assertThat(instance.getServiceKey()).isEqualTo(namespace + "/" + name + ":" + version);
    }

    @Test
    void shouldCreateTrafficPolicyWithAllComponents() {
        // Given
        String name = "test-policy";
        String namespace = "default";
        Map<String, Integer> trafficSplit = Map.of("v1", 80, "v2", 20);

        ServiceMeshIntegrationService.CircuitBreakerConfig circuitBreaker = 
            new ServiceMeshIntegrationService.CircuitBreakerConfig(
                3, Duration.ofSeconds(30), Duration.ofSeconds(30), 50
            );

        ServiceMeshIntegrationService.RetryConfig retry = 
            new ServiceMeshIntegrationService.RetryConfig(
                3, Duration.ofSeconds(5), Set.of("5xx", "gateway-error", "connect-failure")
            );

        ServiceMeshIntegrationService.TimeoutConfig timeout = 
            new ServiceMeshIntegrationService.TimeoutConfig(
                Duration.ofSeconds(30), Duration.ofSeconds(300)
            );

        ServiceMeshIntegrationService.LoadBalancingConfig loadBalancing = 
            new ServiceMeshIntegrationService.LoadBalancingConfig(
                ServiceMeshIntegrationService.LoadBalancingConfig.LoadBalancingMethod.LEAST_CONN, 
                null
            );

        // When
        ServiceMeshIntegrationService.TrafficPolicy policy = 
            new ServiceMeshIntegrationService.TrafficPolicy(
                name, namespace, trafficSplit, circuitBreaker, retry, timeout, loadBalancing
            );

        // Then
        assertThat(policy.getName()).isEqualTo(name);
        assertThat(policy.getNamespace()).isEqualTo(namespace);
        assertThat(policy.getTrafficSplit()).isEqualTo(trafficSplit);
        assertThat(policy.getCircuitBreaker()).isEqualTo(circuitBreaker);
        assertThat(policy.getRetry()).isEqualTo(retry);
        assertThat(policy.getTimeout()).isEqualTo(timeout);
        assertThat(policy.getLoadBalancing()).isEqualTo(loadBalancing);

        // Test circuit breaker configuration
        assertThat(circuitBreaker.getConsecutiveErrors()).isEqualTo(3);
        assertThat(circuitBreaker.getInterval()).isEqualTo(Duration.ofSeconds(30));
        assertThat(circuitBreaker.getBaseEjectionTime()).isEqualTo(Duration.ofSeconds(30));
        assertThat(circuitBreaker.getMaxEjectionPercent()).isEqualTo(50);

        // Test retry configuration
        assertThat(retry.getAttempts()).isEqualTo(3);
        assertThat(retry.getPerTryTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(retry.getRetryOn()).containsExactlyInAnyOrder("5xx", "gateway-error", "connect-failure");

        // Test timeout configuration
        assertThat(timeout.getRequestTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(timeout.getIdleTimeout()).isEqualTo(Duration.ofSeconds(300));

        // Test load balancing configuration
        assertThat(loadBalancing.getMethod())
            .isEqualTo(ServiceMeshIntegrationService.LoadBalancingConfig.LoadBalancingMethod.LEAST_CONN);
    }

    @Test
    void shouldCreateMeshMetrics() {
        // Given
        String serviceName = "test-service";
        long requestCount = 1000L;
        double successRate = 0.95;
        double averageLatency = 50.0;
        double p95Latency = 95.0;
        double p99Latency = 150.0;
        Map<String, Double> errorRates = Map.of("4xx", 0.03, "5xx", 0.02);

        // When
        ServiceMeshIntegrationService.MeshMetrics metrics = 
            new ServiceMeshIntegrationService.MeshMetrics(
                serviceName, requestCount, successRate, averageLatency, 
                p95Latency, p99Latency, errorRates
            );

        // Then
        assertThat(metrics.getServiceName()).isEqualTo(serviceName);
        assertThat(metrics.getRequestCount()).isEqualTo(requestCount);
        assertThat(metrics.getSuccessRate()).isEqualTo(successRate);
        assertThat(metrics.getAverageLatency()).isEqualTo(averageLatency);
        assertThat(metrics.getP95Latency()).isEqualTo(p95Latency);
        assertThat(metrics.getP99Latency()).isEqualTo(p99Latency);
        assertThat(metrics.getErrorRates()).isEqualTo(errorRates);
        assertThat(metrics.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateConsistentHashConfig() {
        // Given
        String header = "x-user-id";
        String cookie = "session-id";
        boolean useSourceIp = true;

        // When
        ServiceMeshIntegrationService.ConsistentHashConfig config = 
            new ServiceMeshIntegrationService.ConsistentHashConfig(header, cookie, useSourceIp);

        // Then
        assertThat(config.getHeader()).isEqualTo(header);
        assertThat(config.getCookie()).isEqualTo(cookie);
        assertThat(config.isUseSourceIp()).isTrue();
    }

    @Test
    void shouldUpdateServiceInstanceHealth() {
        // Given
        ServiceMeshIntegrationService.ServiceInstance instance = 
            new ServiceMeshIntegrationService.ServiceInstance(
                "test-service", "default", "v1", "http://localhost:8080",
                Map.of(), Map.of()
            );

        // Initially healthy
        assertThat(instance.isHealthy()).isTrue();

        // When
        instance.setHealthy(false);

        // Then
        assertThat(instance.isHealthy()).isFalse();
        assertThat(instance.getLastSeen()).isNotNull();
    }
}