package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.AdvancedRequestRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Request Routing Controller for MCP Sidecar
 * 
 * Provides REST endpoints for managing request routing and load balancing
 */
@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
@Slf4j
public class RoutingController {

    private final AdvancedRequestRoutingService routingService;

    /**
     * Get cluster status
     */
    @GetMapping("/clusters/{clusterName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getClusterStatus(@PathVariable String clusterName) {
        return routingService.getClusterStatus(clusterName)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.debug("Cluster status requested for: {}", clusterName))
                .onErrorResume(error -> {
                    log.error("Error getting cluster status for {}: {}", clusterName, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get all clusters status
     */
    @GetMapping("/clusters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getAllClustersStatus() {
        return routingService.getAllClustersStatus()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("All clusters status requested"))
                .onErrorResume(error -> {
                    log.error("Error getting all clusters status: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Add service instance to cluster
     */
    @PostMapping("/clusters/{clusterName}/instances")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> addServiceInstance(
            @PathVariable String clusterName,
            @RequestBody AddServiceInstanceRequest request) {
        
        return Mono.fromCallable(() -> {
            AdvancedRequestRoutingService.ServiceInstance instance = 
                new AdvancedRequestRoutingService.ServiceInstance(
                    request.getId(),
                    request.getHost(),
                    request.getPort(),
                    request.getScheme(),
                    request.getMetadata(),
                    request.getWeight()
                );
            return instance;
        })
        .flatMap(instance -> routingService.addServiceInstance(clusterName, instance))
        .then(Mono.just(ResponseEntity.ok(Map.of(
            "status", "Service instance added",
            "clusterName", clusterName,
            "instanceId", request.getId()
        ))))
        .doOnSuccess(response -> log.info("Service instance added: cluster={}, instance={}", 
                clusterName, request.getId()))
        .onErrorResume(error -> {
            log.error("Error adding service instance to cluster {}: {}", clusterName, error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Remove service instance from cluster
     */
    @DeleteMapping("/clusters/{clusterName}/instances/{instanceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> removeServiceInstance(
            @PathVariable String clusterName,
            @PathVariable String instanceId) {
        
        return routingService.removeServiceInstance(clusterName, instanceId)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Service instance removed",
                    "clusterName", clusterName,
                    "instanceId", instanceId
                ))))
                .doOnSuccess(response -> log.info("Service instance removed: cluster={}, instance={}", 
                        clusterName, instanceId))
                .onErrorResume(error -> {
                    log.error("Error removing service instance from cluster {}: {}", clusterName, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Update instance health status
     */
    @PutMapping("/clusters/{clusterName}/instances/{instanceId}/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, String>>> updateInstanceHealth(
            @PathVariable String clusterName,
            @PathVariable String instanceId,
            @RequestBody UpdateHealthRequest request) {
        
        try {
            AdvancedRequestRoutingService.HealthStatus status = 
                AdvancedRequestRoutingService.HealthStatus.valueOf(request.getStatus().toUpperCase());
            
            return routingService.updateInstanceHealth(clusterName, instanceId, status, request.getResponseTime())
                    .then(Mono.just(ResponseEntity.ok(Map.of(
                        "status", "Instance health updated",
                        "clusterName", clusterName,
                        "instanceId", instanceId,
                        "healthStatus", status.name()
                    ))))
                    .doOnSuccess(response -> log.info("Instance health updated: cluster={}, instance={}, status={}", 
                            clusterName, instanceId, status))
                    .onErrorResume(error -> {
                        log.error("Error updating instance health: {}", error.getMessage());
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid health status specified: {}", request.getStatus());
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * Test routing for a path
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<RoutingTestResponse>> testRouting(
            @RequestBody RoutingTestRequest request) {
        
        return routingService.routeRequest(request.getPath(), request.getHeaders(), request.getSessionId())
                .map(result -> {
                    RoutingTestResponse response = new RoutingTestResponse(
                        result.getInstance().getId(),
                        result.getInstance().getUrl(),
                        result.getClusterName(),
                        result.getAlgorithm().name(),
                        result.isFallback()
                    );
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.debug("Routing test performed for path: {}", request.getPath()))
                .onErrorResume(error -> {
                    log.error("Error testing routing: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get routing statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getRoutingStatistics() {
        return routingService.getAllClustersStatus()
                .map(clusters -> {
                    Map<String, Object> statistics = new java.util.HashMap<>();
                    
                    int totalInstances = 0;
                    int healthyInstances = 0;
                    long totalRequests = 0;
                    double averageResponseTime = 0.0;
                    
                    for (Map.Entry<String, Object> entry : clusters.entrySet()) {
                        Map<String, Object> cluster = (Map<String, Object>) entry.getValue();
                        totalInstances += (Integer) cluster.get("totalInstances");
                        healthyInstances += (Integer) cluster.get("healthyInstances");
                        
                        java.util.List<Map<String, Object>> instances = 
                            (java.util.List<Map<String, Object>>) cluster.get("instances");
                        
                        for (Map<String, Object> instance : instances) {
                            totalRequests += ((Number) instance.get("totalRequests")).longValue();
                            averageResponseTime += ((Number) instance.get("responseTime")).doubleValue();
                        }
                    }
                    
                    if (totalInstances > 0) {
                        averageResponseTime = averageResponseTime / totalInstances;
                    }
                    
                    statistics.put("totalClusters", clusters.size());
                    statistics.put("totalInstances", totalInstances);
                    statistics.put("healthyInstances", healthyInstances);
                    statistics.put("healthyInstancePercentage", 
                        totalInstances > 0 ? (double) healthyInstances / totalInstances * 100 : 0.0);
                    statistics.put("totalRequests", totalRequests);
                    statistics.put("averageResponseTime", averageResponseTime);
                    statistics.put("clusters", clusters);
                    
                    return ResponseEntity.ok(statistics);
                })
                .doOnSuccess(response -> log.debug("Routing statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting routing statistics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get available load balancing algorithms
     */
    @GetMapping("/algorithms")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public ResponseEntity<java.util.List<String>> getAvailableAlgorithms() {
        java.util.List<String> algorithms = java.util.Arrays.stream(
                AdvancedRequestRoutingService.LoadBalancingAlgorithm.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(algorithms);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getRoutingHealth() {
        return routingService.getAllClustersStatus()
                .map(clusters -> {
                    int totalClusters = clusters.size();
                    int healthyClusters = 0;
                    
                    for (Map.Entry<String, Object> entry : clusters.entrySet()) {
                        Map<String, Object> cluster = (Map<String, Object>) entry.getValue();
                        int totalInstances = (Integer) cluster.get("totalInstances");
                        int healthyInstances = (Integer) cluster.get("healthyInstances");
                        
                        if (totalInstances > 0 && healthyInstances > 0) {
                            healthyClusters++;
                        }
                    }
                    
                    Map<String, Object> health = Map.of(
                        "status", healthyClusters == totalClusters ? "UP" : "DEGRADED",
                        "totalClusters", totalClusters,
                        "healthyClusters", healthyClusters,
                        "unhealthyClusters", totalClusters - healthyClusters
                    );
                    
                    return ResponseEntity.ok(health);
                })
                .doOnSuccess(response -> log.debug("Routing health check requested"))
                .onErrorResume(error -> {
                    log.error("Error getting routing health: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Request/Response DTOs
     */
    public static class AddServiceInstanceRequest {
        private String id;
        private String host;
        private int port;
        private String scheme = "http";
        private Map<String, String> metadata = new java.util.HashMap<>();
        private int weight = 100;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getScheme() { return scheme; }
        public void setScheme(String scheme) { this.scheme = scheme; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
    }

    public static class UpdateHealthRequest {
        private String status;
        private long responseTime;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    }

    public static class RoutingTestRequest {
        private String path;
        private Map<String, String> headers = new java.util.HashMap<>();
        private String sessionId;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    public static class RoutingTestResponse {
        private final String instanceId;
        private final String instanceUrl;
        private final String clusterName;
        private final String algorithm;
        private final boolean fallback;
        
        public RoutingTestResponse(String instanceId, String instanceUrl, String clusterName, 
                                  String algorithm, boolean fallback) {
            this.instanceId = instanceId;
            this.instanceUrl = instanceUrl;
            this.clusterName = clusterName;
            this.algorithm = algorithm;
            this.fallback = fallback;
        }
        
        public String getInstanceId() { return instanceId; }
        public String getInstanceUrl() { return instanceUrl; }
        public String getClusterName() { return clusterName; }
        public String getAlgorithm() { return algorithm; }
        public boolean isFallback() { return fallback; }
    }
}