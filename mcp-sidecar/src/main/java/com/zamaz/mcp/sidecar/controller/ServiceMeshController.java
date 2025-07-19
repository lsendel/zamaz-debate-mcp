package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.ServiceMeshIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Service Mesh Controller for MCP Sidecar
 * 
 * Provides REST endpoints for service mesh management and monitoring
 */
@RestController
@RequestMapping("/api/v1/mesh")
@RequiredArgsConstructor
@Slf4j
public class ServiceMeshController {

    private final ServiceMeshIntegrationService meshService;

    /**
     * Get service mesh health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getMeshHealth() {
        return meshService.getMeshHealth()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Mesh health requested"))
                .onErrorResume(error -> {
                    log.error("Error getting mesh health: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get discovered services
     */
    @GetMapping("/services")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, ServiceMeshIntegrationService.ServiceInstance>>> getDiscoveredServices() {
        return meshService.getDiscoveredServices()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Discovered services requested"))
                .onErrorResume(error -> {
                    log.error("Error getting discovered services: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get mesh metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getMeshMetrics() {
        return meshService.getMeshMetrics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Mesh metrics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting mesh metrics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get traffic policies
     */
    @GetMapping("/traffic-policies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, ServiceMeshIntegrationService.TrafficPolicy>>> getTrafficPolicies() {
        return meshService.getTrafficPolicies()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Traffic policies requested"))
                .onErrorResume(error -> {
                    log.error("Error getting traffic policies: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Apply traffic policy
     */
    @PostMapping("/traffic-policies/{serviceName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> applyTrafficPolicy(
            @PathVariable String serviceName,
            @RequestBody TrafficPolicyRequest request) {
        
        return Mono.fromCallable(() -> {
            // Convert request to TrafficPolicy
            ServiceMeshIntegrationService.CircuitBreakerConfig circuitBreaker = 
                new ServiceMeshIntegrationService.CircuitBreakerConfig(
                    request.getCircuitBreaker().getConsecutiveErrors(),
                    Duration.parse(request.getCircuitBreaker().getInterval()),
                    Duration.parse(request.getCircuitBreaker().getBaseEjectionTime()),
                    request.getCircuitBreaker().getMaxEjectionPercent()
                );

            ServiceMeshIntegrationService.RetryConfig retry = 
                new ServiceMeshIntegrationService.RetryConfig(
                    request.getRetry().getAttempts(),
                    Duration.parse(request.getRetry().getPerTryTimeout()),
                    request.getRetry().getRetryOn()
                );

            ServiceMeshIntegrationService.TimeoutConfig timeout = 
                new ServiceMeshIntegrationService.TimeoutConfig(
                    Duration.parse(request.getTimeout().getRequestTimeout()),
                    Duration.parse(request.getTimeout().getIdleTimeout())
                );

            ServiceMeshIntegrationService.LoadBalancingConfig loadBalancing = 
                new ServiceMeshIntegrationService.LoadBalancingConfig(
                    ServiceMeshIntegrationService.LoadBalancingConfig.LoadBalancingMethod
                        .valueOf(request.getLoadBalancing().getMethod().toUpperCase()),
                    null // ConsistentHashConfig would be constructed here if provided
                );

            ServiceMeshIntegrationService.TrafficPolicy policy = 
                new ServiceMeshIntegrationService.TrafficPolicy(
                    serviceName + "-policy",
                    request.getNamespace(),
                    request.getTrafficSplit(),
                    circuitBreaker,
                    retry,
                    timeout,
                    loadBalancing
                );

            return policy;
        })
        .flatMap(policy -> meshService.applyTrafficPolicy(serviceName, policy))
        .map(success -> {
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "status", "Traffic policy applied",
                    "serviceName", serviceName
                ));
            } else {
                return ResponseEntity.internalServerError().<Map<String, String>>build();
            }
        })
        .doOnSuccess(response -> log.info("Traffic policy applied for service: {}", serviceName))
        .onErrorResume(error -> {
            log.error("Error applying traffic policy for service {}: {}", serviceName, error.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * Create canary deployment
     */
    @PostMapping("/canary/{serviceName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> createCanaryDeployment(
            @PathVariable String serviceName,
            @RequestBody CanaryDeploymentRequest request) {
        
        return meshService.createCanaryDeployment(serviceName, request.getCanaryVersion(), request.getTrafficPercent())
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok(Map.of(
                            "status", "Canary deployment created",
                            "serviceName", serviceName,
                            "canaryVersion", request.getCanaryVersion(),
                            "trafficPercent", String.valueOf(request.getTrafficPercent())
                        ));
                    } else {
                        return ResponseEntity.internalServerError().<Map<String, String>>build();
                    }
                })
                .doOnSuccess(response -> log.info("Canary deployment created for service: {} version: {} traffic: {}%",
                        serviceName, request.getCanaryVersion(), request.getTrafficPercent()))
                .onErrorResume(error -> {
                    log.error("Error creating canary deployment for service {}: {}", serviceName, error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Inject fault
     */
    @PostMapping("/fault-injection/{serviceName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> injectFault(
            @PathVariable String serviceName,
            @RequestBody FaultInjectionRequest request) {
        
        Duration delay = Duration.parse(request.getDelay());
        
        return meshService.injectFault(serviceName, request.getFaultType(), request.getPercentage(), delay)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok(Map.of(
                            "status", "Fault injection applied",
                            "serviceName", serviceName,
                            "faultType", request.getFaultType(),
                            "percentage", String.valueOf(request.getPercentage())
                        ));
                    } else {
                        return ResponseEntity.internalServerError().<Map<String, String>>build();
                    }
                })
                .doOnSuccess(response -> log.info("Fault injection applied for service: {} type: {} percentage: {}%",
                        serviceName, request.getFaultType(), request.getPercentage()))
                .onErrorResume(error -> {
                    log.error("Error injecting fault for service {}: {}", serviceName, error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Export mesh configuration
     */
    @GetMapping("/config/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, Object>>> exportMeshConfiguration() {
        return meshService.exportMeshConfiguration()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Mesh configuration export requested"))
                .onErrorResume(error -> {
                    log.error("Error exporting mesh configuration: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get service details
     */
    @GetMapping("/services/{serviceName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<ServiceDetailsResponse>> getServiceDetails(@PathVariable String serviceName) {
        return meshService.getDiscoveredServices()
                .map(services -> {
                    ServiceMeshIntegrationService.ServiceInstance service = services.values().stream()
                            .filter(s -> s.getName().equals(serviceName))
                            .findFirst()
                            .orElse(null);

                    if (service == null) {
                        return ResponseEntity.notFound().<ServiceDetailsResponse>build();
                    }

                    ServiceDetailsResponse response = new ServiceDetailsResponse(
                        service.getName(),
                        service.getNamespace(),
                        service.getVersion(),
                        service.getEndpoint(),
                        service.isHealthy(),
                        service.getLastSeen(),
                        service.getLabels(),
                        service.getAnnotations(),
                        service.getMeshMetadata()
                    );

                    return ResponseEntity.ok(response);
                })
                .doOnSuccess(response -> log.debug("Service details requested for: {}", serviceName))
                .onErrorResume(error -> {
                    log.error("Error getting service details for {}: {}", serviceName, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get mesh topology
     */
    @GetMapping("/topology")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getMeshTopology() {
        return Mono.zip(
                meshService.getDiscoveredServices(),
                meshService.getTrafficPolicies(),
                meshService.getMeshMetrics()
        ).map(tuple -> {
            Map<String, ServiceMeshIntegrationService.ServiceInstance> services = tuple.getT1();
            Map<String, ServiceMeshIntegrationService.TrafficPolicy> policies = tuple.getT2();
            Map<String, Object> metrics = tuple.getT3();

            Map<String, Object> topology = Map.of(
                "services", services,
                "trafficPolicies", policies,
                "metrics", metrics,
                "connections", generateServiceConnections(services),
                "timestamp", java.time.Instant.now()
            );

            return ResponseEntity.ok(topology);
        })
        .doOnSuccess(response -> log.debug("Mesh topology requested"))
        .onErrorResume(error -> {
            log.error("Error getting mesh topology: {}", error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Generate service connections for topology visualization
     */
    private Map<String, Object> generateServiceConnections(
            Map<String, ServiceMeshIntegrationService.ServiceInstance> services) {
        
        // This would analyze actual service-to-service communication patterns
        // For now, return a simplified connection map
        Map<String, Object> connections = new java.util.HashMap<>();
        
        services.values().forEach(service -> {
            connections.put(service.getName(), Map.of(
                "inbound", java.util.List.of("mcp-sidecar"),
                "outbound", java.util.List.of("external-api", "database"),
                "protocols", java.util.List.of("HTTP", "gRPC")
            ));
        });
        
        return connections;
    }

    /**
     * Request DTOs
     */
    public static class TrafficPolicyRequest {
        private String namespace;
        private Map<String, Integer> trafficSplit;
        private CircuitBreakerRequest circuitBreaker;
        private RetryRequest retry;
        private TimeoutRequest timeout;
        private LoadBalancingRequest loadBalancing;

        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public Map<String, Integer> getTrafficSplit() { return trafficSplit; }
        public void setTrafficSplit(Map<String, Integer> trafficSplit) { this.trafficSplit = trafficSplit; }
        public CircuitBreakerRequest getCircuitBreaker() { return circuitBreaker; }
        public void setCircuitBreaker(CircuitBreakerRequest circuitBreaker) { this.circuitBreaker = circuitBreaker; }
        public RetryRequest getRetry() { return retry; }
        public void setRetry(RetryRequest retry) { this.retry = retry; }
        public TimeoutRequest getTimeout() { return timeout; }
        public void setTimeout(TimeoutRequest timeout) { this.timeout = timeout; }
        public LoadBalancingRequest getLoadBalancing() { return loadBalancing; }
        public void setLoadBalancing(LoadBalancingRequest loadBalancing) { this.loadBalancing = loadBalancing; }
    }

    public static class CircuitBreakerRequest {
        private int consecutiveErrors;
        private String interval;
        private String baseEjectionTime;
        private int maxEjectionPercent;

        public int getConsecutiveErrors() { return consecutiveErrors; }
        public void setConsecutiveErrors(int consecutiveErrors) { this.consecutiveErrors = consecutiveErrors; }
        public String getInterval() { return interval; }
        public void setInterval(String interval) { this.interval = interval; }
        public String getBaseEjectionTime() { return baseEjectionTime; }
        public void setBaseEjectionTime(String baseEjectionTime) { this.baseEjectionTime = baseEjectionTime; }
        public int getMaxEjectionPercent() { return maxEjectionPercent; }
        public void setMaxEjectionPercent(int maxEjectionPercent) { this.maxEjectionPercent = maxEjectionPercent; }
    }

    public static class RetryRequest {
        private int attempts;
        private String perTryTimeout;
        private java.util.Set<String> retryOn;

        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        public String getPerTryTimeout() { return perTryTimeout; }
        public void setPerTryTimeout(String perTryTimeout) { this.perTryTimeout = perTryTimeout; }
        public java.util.Set<String> getRetryOn() { return retryOn; }
        public void setRetryOn(java.util.Set<String> retryOn) { this.retryOn = retryOn; }
    }

    public static class TimeoutRequest {
        private String requestTimeout;
        private String idleTimeout;

        public String getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(String requestTimeout) { this.requestTimeout = requestTimeout; }
        public String getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(String idleTimeout) { this.idleTimeout = idleTimeout; }
    }

    public static class LoadBalancingRequest {
        private String method;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }

    public static class CanaryDeploymentRequest {
        private String canaryVersion;
        private int trafficPercent;

        public String getCanaryVersion() { return canaryVersion; }
        public void setCanaryVersion(String canaryVersion) { this.canaryVersion = canaryVersion; }
        public int getTrafficPercent() { return trafficPercent; }
        public void setTrafficPercent(int trafficPercent) { this.trafficPercent = trafficPercent; }
    }

    public static class FaultInjectionRequest {
        private String faultType;
        private double percentage;
        private String delay;

        public String getFaultType() { return faultType; }
        public void setFaultType(String faultType) { this.faultType = faultType; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public String getDelay() { return delay; }
        public void setDelay(String delay) { this.delay = delay; }
    }

    /**
     * Response DTOs
     */
    public static class ServiceDetailsResponse {
        private final String name;
        private final String namespace;
        private final String version;
        private final String endpoint;
        private final boolean healthy;
        private final java.time.Instant lastSeen;
        private final Map<String, String> labels;
        private final Map<String, String> annotations;
        private final Map<String, Object> meshMetadata;

        public ServiceDetailsResponse(String name, String namespace, String version, String endpoint,
                                    boolean healthy, java.time.Instant lastSeen,
                                    Map<String, String> labels, Map<String, String> annotations,
                                    Map<String, Object> meshMetadata) {
            this.name = name;
            this.namespace = namespace;
            this.version = version;
            this.endpoint = endpoint;
            this.healthy = healthy;
            this.lastSeen = lastSeen;
            this.labels = labels;
            this.annotations = annotations;
            this.meshMetadata = meshMetadata;
        }

        public String getName() { return name; }
        public String getNamespace() { return namespace; }
        public String getVersion() { return version; }
        public String getEndpoint() { return endpoint; }
        public boolean isHealthy() { return healthy; }
        public java.time.Instant getLastSeen() { return lastSeen; }
        public Map<String, String> getLabels() { return labels; }
        public Map<String, String> getAnnotations() { return annotations; }
        public Map<String, Object> getMeshMetadata() { return meshMetadata; }
    }
}