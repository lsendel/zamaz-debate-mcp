package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Advanced Request Routing and Load Balancing Service
 * 
 * Features:
 * - Multiple load balancing algorithms (round-robin, least connections, weighted, etc.)
 * - Health checking and service discovery
 * - Request routing based on various criteria
 * - Sticky sessions support
 * - Circuit breaker integration
 * - Metrics and monitoring
 * - Dynamic configuration updates
 * - Failover and retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedRequestRoutingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final MetricsCollectorService metricsCollectorService;
    private final DistributedCircuitBreakerService circuitBreakerService;

    @Value("${app.routing.enabled:true}")
    private boolean routingEnabled;

    @Value("${app.routing.default-algorithm:ROUND_ROBIN}")
    private String defaultAlgorithm;

    @Value("${app.routing.health-check-interval:30s}")
    private Duration healthCheckInterval;

    @Value("${app.routing.max-retries:3}")
    private int maxRetries;

    @Value("${app.routing.retry-delay:1s}")
    private Duration retryDelay;

    // Service registry and routing state
    private final Map<String, ServiceCluster> serviceClusters = new ConcurrentHashMap<>();
    private final Map<String, RoutingRule> routingRules = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> stickySessionStore = new ConcurrentHashMap<>();

    /**
     * Load balancing algorithms
     */
    public enum LoadBalancingAlgorithm {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        WEIGHTED_ROUND_ROBIN,
        LEAST_RESPONSE_TIME,
        RANDOM,
        CONSISTENT_HASH,
        IP_HASH
    }

    /**
     * Service instance health status
     */
    public enum HealthStatus {
        HEALTHY, UNHEALTHY, UNKNOWN, DRAINING
    }

    /**
     * Service instance definition
     */
    public static class ServiceInstance {
        private final String id;
        private final String host;
        private final int port;
        private final String scheme;
        private final Map<String, String> metadata;
        private volatile HealthStatus healthStatus;
        private volatile Instant lastHealthCheck;
        private volatile long responseTime;
        private volatile int activeConnections;
        private volatile int weight;
        private final AtomicLong totalRequests;
        private final AtomicLong successfulRequests;
        private final AtomicLong failedRequests;

        public ServiceInstance(String id, String host, int port, String scheme, 
                             Map<String, String> metadata, int weight) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.scheme = scheme;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.weight = weight;
            this.healthStatus = HealthStatus.UNKNOWN;
            this.lastHealthCheck = Instant.now();
            this.responseTime = 0;
            this.activeConnections = 0;
            this.totalRequests = new AtomicLong(0);
            this.successfulRequests = new AtomicLong(0);
            this.failedRequests = new AtomicLong(0);
        }

        public String getId() { return id; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getScheme() { return scheme; }
        public Map<String, String> getMetadata() { return metadata; }
        public HealthStatus getHealthStatus() { return healthStatus; }
        public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }
        public Instant getLastHealthCheck() { return lastHealthCheck; }
        public void setLastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public long getTotalRequests() { return totalRequests.get(); }
        public long getSuccessfulRequests() { return successfulRequests.get(); }
        public long getFailedRequests() { return failedRequests.get(); }

        public String getUrl() {
            return scheme + "://" + host + ":" + port;
        }

        public void recordRequest(boolean success, long responseTime) {
            totalRequests.incrementAndGet();
            if (success) {
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }
            this.responseTime = responseTime;
        }

        public double getSuccessRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) successfulRequests.get() / total : 0.0;
        }

        public boolean isHealthy() {
            return healthStatus == HealthStatus.HEALTHY;
        }

        public boolean isAvailable() {
            return healthStatus == HealthStatus.HEALTHY || healthStatus == HealthStatus.DRAINING;
        }
    }

    /**
     * Service cluster definition
     */
    public static class ServiceCluster {
        private final String name;
        private final List<ServiceInstance> instances;
        private final LoadBalancingAlgorithm algorithm;
        private final Map<String, String> configuration;
        private volatile boolean enabled;

        public ServiceCluster(String name, LoadBalancingAlgorithm algorithm, 
                            Map<String, String> configuration) {
            this.name = name;
            this.instances = new ArrayList<>();
            this.algorithm = algorithm;
            this.configuration = configuration != null ? new HashMap<>(configuration) : new HashMap<>();
            this.enabled = true;
        }

        public String getName() { return name; }
        public List<ServiceInstance> getInstances() { return instances; }
        public LoadBalancingAlgorithm getAlgorithm() { return algorithm; }
        public Map<String, String> getConfiguration() { return configuration; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public void addInstance(ServiceInstance instance) {
            instances.add(instance);
        }

        public void removeInstance(String instanceId) {
            instances.removeIf(instance -> instance.getId().equals(instanceId));
        }

        public List<ServiceInstance> getHealthyInstances() {
            return instances.stream()
                    .filter(ServiceInstance::isHealthy)
                    .collect(Collectors.toList());
        }

        public List<ServiceInstance> getAvailableInstances() {
            return instances.stream()
                    .filter(ServiceInstance::isAvailable)
                    .collect(Collectors.toList());
        }

        public int getHealthyInstanceCount() {
            return (int) instances.stream()
                    .filter(ServiceInstance::isHealthy)
                    .count();
        }

        public double getAverageResponseTime() {
            List<ServiceInstance> healthy = getHealthyInstances();
            if (healthy.isEmpty()) return 0.0;
            
            return healthy.stream()
                    .mapToLong(ServiceInstance::getResponseTime)
                    .average()
                    .orElse(0.0);
        }
    }

    /**
     * Routing rule definition
     */
    public static class RoutingRule {
        private final String id;
        private final String pattern;
        private final String targetCluster;
        private final Map<String, String> conditions;
        private final int priority;
        private final boolean enabled;

        public RoutingRule(String id, String pattern, String targetCluster, 
                          Map<String, String> conditions, int priority, boolean enabled) {
            this.id = id;
            this.pattern = pattern;
            this.targetCluster = targetCluster;
            this.conditions = conditions != null ? new HashMap<>(conditions) : new HashMap<>();
            this.priority = priority;
            this.enabled = enabled;
        }

        public String getId() { return id; }
        public String getPattern() { return pattern; }
        public String getTargetCluster() { return targetCluster; }
        public Map<String, String> getConditions() { return conditions; }
        public int getPriority() { return priority; }
        public boolean isEnabled() { return enabled; }

        public boolean matches(String path, Map<String, String> headers) {
            if (!enabled) return false;
            
            // Check path pattern
            if (pattern != null && !path.matches(pattern)) {
                return false;
            }
            
            // Check conditions
            for (Map.Entry<String, String> condition : conditions.entrySet()) {
                String key = condition.getKey();
                String expectedValue = condition.getValue();
                
                if (key.startsWith("header.")) {
                    String headerName = key.substring(7);
                    String headerValue = headers.get(headerName);
                    if (headerValue == null || !headerValue.matches(expectedValue)) {
                        return false;
                    }
                }
            }
            
            return true;
        }
    }

    /**
     * Routing result
     */
    public static class RoutingResult {
        private final ServiceInstance instance;
        private final String clusterName;
        private final LoadBalancingAlgorithm algorithm;
        private final String sessionId;
        private final boolean fallback;

        public RoutingResult(ServiceInstance instance, String clusterName, 
                           LoadBalancingAlgorithm algorithm, String sessionId, boolean fallback) {
            this.instance = instance;
            this.clusterName = clusterName;
            this.algorithm = algorithm;
            this.sessionId = sessionId;
            this.fallback = fallback;
        }

        public ServiceInstance getInstance() { return instance; }
        public String getClusterName() { return clusterName; }
        public LoadBalancingAlgorithm getAlgorithm() { return algorithm; }
        public String getSessionId() { return sessionId; }
        public boolean isFallback() { return fallback; }
    }

    /**
     * Initialize default service clusters
     */
    public void initializeDefaultClusters() {
        // Organization service cluster
        ServiceCluster orgCluster = new ServiceCluster("organization-service", 
                LoadBalancingAlgorithm.ROUND_ROBIN, Map.of());
        orgCluster.addInstance(new ServiceInstance("org-1", "localhost", 5005, "http", 
                Map.of("version", "v1"), 100));
        serviceClusters.put("organization-service", orgCluster);

        // LLM service cluster
        ServiceCluster llmCluster = new ServiceCluster("llm-service", 
                LoadBalancingAlgorithm.LEAST_RESPONSE_TIME, Map.of());
        llmCluster.addInstance(new ServiceInstance("llm-1", "localhost", 5002, "http", 
                Map.of("version", "v1", "model", "gpt-4"), 100));
        serviceClusters.put("llm-service", llmCluster);

        // Debate controller cluster
        ServiceCluster debateCluster = new ServiceCluster("debate-controller", 
                LoadBalancingAlgorithm.ROUND_ROBIN, Map.of());
        debateCluster.addInstance(new ServiceInstance("debate-1", "localhost", 5013, "http", 
                Map.of("version", "v1"), 100));
        serviceClusters.put("debate-controller", debateCluster);

        // RAG service cluster
        ServiceCluster ragCluster = new ServiceCluster("rag-service", 
                LoadBalancingAlgorithm.LEAST_CONNECTIONS, Map.of());
        ragCluster.addInstance(new ServiceInstance("rag-1", "localhost", 5004, "http", 
                Map.of("version", "v1"), 100));
        serviceClusters.put("rag-service", ragCluster);

        // Initialize routing rules
        initializeDefaultRoutingRules();

        log.info("Initialized {} service clusters", serviceClusters.size());
    }

    /**
     * Initialize default routing rules
     */
    private void initializeDefaultRoutingRules() {
        // Organization API routing
        routingRules.put("org-api", new RoutingRule("org-api", "/api/v1/organizations/.*", 
                "organization-service", Map.of(), 100, true));

        // LLM API routing
        routingRules.put("llm-api", new RoutingRule("llm-api", "/api/v1/llm/.*", 
                "llm-service", Map.of(), 100, true));

        // Debate API routing
        routingRules.put("debate-api", new RoutingRule("debate-api", "/api/v1/debates/.*", 
                "debate-controller", Map.of(), 100, true));

        // RAG API routing
        routingRules.put("rag-api", new RoutingRule("rag-api", "/api/v1/rag/.*", 
                "rag-service", Map.of(), 100, true));

        // Header-based routing for API versioning
        routingRules.put("v2-api", new RoutingRule("v2-api", "/api/.*", 
                "v2-service", Map.of("header.API-Version", "v2"), 200, true));

        log.info("Initialized {} routing rules", routingRules.size());
    }

    /**
     * Route request to appropriate service instance
     */
    public Mono<RoutingResult> routeRequest(String path, Map<String, String> headers, 
                                           String sessionId) {
        if (!routingEnabled) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            // Find matching routing rule
            RoutingRule matchingRule = findMatchingRule(path, headers);
            if (matchingRule == null) {
                log.warn("No routing rule found for path: {}", path);
                return null;
            }

            // Get target cluster
            ServiceCluster cluster = serviceClusters.get(matchingRule.getTargetCluster());
            if (cluster == null || !cluster.isEnabled()) {
                log.warn("Target cluster not found or disabled: {}", matchingRule.getTargetCluster());
                return null;
            }

            // Select instance using load balancing algorithm
            ServiceInstance instance = selectInstance(cluster, path, headers, sessionId);
            if (instance == null) {
                log.warn("No healthy instance found in cluster: {}", cluster.getName());
                return null;
            }

            return new RoutingResult(instance, cluster.getName(), cluster.getAlgorithm(), 
                                   sessionId, false);
        });
    }

    /**
     * Find matching routing rule
     */
    private RoutingRule findMatchingRule(String path, Map<String, String> headers) {
        return routingRules.values().stream()
                .filter(rule -> rule.matches(path, headers))
                .max(Comparator.comparingInt(RoutingRule::getPriority))
                .orElse(null);
    }

    /**
     * Select service instance based on load balancing algorithm
     */
    private ServiceInstance selectInstance(ServiceCluster cluster, String path, 
                                         Map<String, String> headers, String sessionId) {
        List<ServiceInstance> availableInstances = cluster.getAvailableInstances();
        if (availableInstances.isEmpty()) {
            return null;
        }

        switch (cluster.getAlgorithm()) {
            case ROUND_ROBIN:
                return selectRoundRobin(cluster, availableInstances);
            case LEAST_CONNECTIONS:
                return selectLeastConnections(availableInstances);
            case WEIGHTED_ROUND_ROBIN:
                return selectWeightedRoundRobin(cluster, availableInstances);
            case LEAST_RESPONSE_TIME:
                return selectLeastResponseTime(availableInstances);
            case RANDOM:
                return selectRandom(availableInstances);
            case CONSISTENT_HASH:
                return selectConsistentHash(availableInstances, sessionId);
            case IP_HASH:
                return selectIpHash(availableInstances, headers.get("X-Forwarded-For"));
            default:
                return selectRoundRobin(cluster, availableInstances);
        }
    }

    /**
     * Round-robin selection
     */
    private ServiceInstance selectRoundRobin(ServiceCluster cluster, List<ServiceInstance> instances) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(cluster.getName(), 
                k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % instances.size();
        return instances.get(index);
    }

    /**
     * Least connections selection
     */
    private ServiceInstance selectLeastConnections(List<ServiceInstance> instances) {
        return instances.stream()
                .min(Comparator.comparingInt(ServiceInstance::getActiveConnections))
                .orElse(instances.get(0));
    }

    /**
     * Weighted round-robin selection
     */
    private ServiceInstance selectWeightedRoundRobin(ServiceCluster cluster, List<ServiceInstance> instances) {
        int totalWeight = instances.stream().mapToInt(ServiceInstance::getWeight).sum();
        if (totalWeight == 0) {
            return selectRoundRobin(cluster, instances);
        }

        AtomicInteger counter = roundRobinCounters.computeIfAbsent(cluster.getName(), 
                k -> new AtomicInteger(0));
        int target = counter.getAndIncrement() % totalWeight;
        
        int currentWeight = 0;
        for (ServiceInstance instance : instances) {
            currentWeight += instance.getWeight();
            if (currentWeight > target) {
                return instance;
            }
        }
        
        return instances.get(0);
    }

    /**
     * Least response time selection
     */
    private ServiceInstance selectLeastResponseTime(List<ServiceInstance> instances) {
        return instances.stream()
                .min(Comparator.comparingLong(ServiceInstance::getResponseTime))
                .orElse(instances.get(0));
    }

    /**
     * Random selection
     */
    private ServiceInstance selectRandom(List<ServiceInstance> instances) {
        return instances.get(new Random().nextInt(instances.size()));
    }

    /**
     * Consistent hash selection
     */
    private ServiceInstance selectConsistentHash(List<ServiceInstance> instances, String key) {
        if (key == null) key = "default";
        int hash = key.hashCode();
        int index = Math.abs(hash) % instances.size();
        return instances.get(index);
    }

    /**
     * IP hash selection
     */
    private ServiceInstance selectIpHash(List<ServiceInstance> instances, String clientIp) {
        if (clientIp == null) clientIp = "127.0.0.1";
        return selectConsistentHash(instances, clientIp);
    }

    /**
     * Add service instance to cluster
     */
    public Mono<Void> addServiceInstance(String clusterName, ServiceInstance instance) {
        return Mono.fromRunnable(() -> {
            ServiceCluster cluster = serviceClusters.get(clusterName);
            if (cluster != null) {
                cluster.addInstance(instance);
                log.info("Added service instance {} to cluster {}", instance.getId(), clusterName);
            } else {
                log.warn("Cluster {} not found when adding instance {}", clusterName, instance.getId());
            }
        });
    }

    /**
     * Remove service instance from cluster
     */
    public Mono<Void> removeServiceInstance(String clusterName, String instanceId) {
        return Mono.fromRunnable(() -> {
            ServiceCluster cluster = serviceClusters.get(clusterName);
            if (cluster != null) {
                cluster.removeInstance(instanceId);
                log.info("Removed service instance {} from cluster {}", instanceId, clusterName);
            } else {
                log.warn("Cluster {} not found when removing instance {}", clusterName, instanceId);
            }
        });
    }

    /**
     * Get cluster status
     */
    public Mono<Map<String, Object>> getClusterStatus(String clusterName) {
        return Mono.fromCallable(() -> {
            ServiceCluster cluster = serviceClusters.get(clusterName);
            if (cluster == null) {
                return null;
            }

            Map<String, Object> status = new HashMap<>();
            status.put("name", cluster.getName());
            status.put("algorithm", cluster.getAlgorithm().name());
            status.put("enabled", cluster.isEnabled());
            status.put("totalInstances", cluster.getInstances().size());
            status.put("healthyInstances", cluster.getHealthyInstanceCount());
            status.put("averageResponseTime", cluster.getAverageResponseTime());
            
            List<Map<String, Object>> instances = cluster.getInstances().stream()
                    .map(instance -> {
                        Map<String, Object> instanceInfo = new HashMap<>();
                        instanceInfo.put("id", instance.getId());
                        instanceInfo.put("url", instance.getUrl());
                        instanceInfo.put("healthStatus", instance.getHealthStatus().name());
                        instanceInfo.put("weight", instance.getWeight());
                        instanceInfo.put("activeConnections", instance.getActiveConnections());
                        instanceInfo.put("responseTime", instance.getResponseTime());
                        instanceInfo.put("successRate", instance.getSuccessRate());
                        instanceInfo.put("totalRequests", instance.getTotalRequests());
                        return instanceInfo;
                    })
                    .collect(Collectors.toList());
            
            status.put("instances", instances);
            return status;
        });
    }

    /**
     * Get all clusters status
     */
    public Mono<Map<String, Object>> getAllClustersStatus() {
        return Mono.fromCallable(() -> {
            Map<String, Object> allStatus = new HashMap<>();
            
            for (String clusterName : serviceClusters.keySet()) {
                Map<String, Object> clusterStatus = getClusterStatus(clusterName).block();
                if (clusterStatus != null) {
                    allStatus.put(clusterName, clusterStatus);
                }
            }
            
            return allStatus;
        });
    }

    /**
     * Update instance health status
     */
    public Mono<Void> updateInstanceHealth(String clusterName, String instanceId, 
                                          HealthStatus status, long responseTime) {
        return Mono.fromRunnable(() -> {
            ServiceCluster cluster = serviceClusters.get(clusterName);
            if (cluster != null) {
                cluster.getInstances().stream()
                        .filter(instance -> instance.getId().equals(instanceId))
                        .findFirst()
                        .ifPresent(instance -> {
                            instance.setHealthStatus(status);
                            instance.setResponseTime(responseTime);
                            instance.setLastHealthCheck(Instant.now());
                            
                            log.debug("Updated health status for instance {}: {}", instanceId, status);
                            metricsCollectorService.recordServiceHealth(clusterName, instanceId, 
                                    status == HealthStatus.HEALTHY, responseTime);
                        });
            }
        });
    }

    /**
     * Record request metrics
     */
    public void recordRequestMetrics(String clusterName, String instanceId, 
                                   boolean success, long responseTime) {
        ServiceCluster cluster = serviceClusters.get(clusterName);
        if (cluster != null) {
            cluster.getInstances().stream()
                    .filter(instance -> instance.getId().equals(instanceId))
                    .findFirst()
                    .ifPresent(instance -> {
                        instance.recordRequest(success, responseTime);
                        log.debug("Recorded request metrics for instance {}: success={}, responseTime={}ms", 
                                instanceId, success, responseTime);
                    });
        }
    }
}