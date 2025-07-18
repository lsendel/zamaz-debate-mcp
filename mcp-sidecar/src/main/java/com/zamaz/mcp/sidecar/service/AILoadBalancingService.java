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

/**
 * AI Service Load Balancing Service for MCP Sidecar
 * 
 * Provides intelligent load balancing for AI/LLM services with multiple strategies:
 * - Round Robin
 * - Weighted Round Robin
 * - Least Connections
 * - Response Time Based
 * - Health-based routing
 * - Model-specific routing
 * - Queue-based routing for expensive operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AILoadBalancingService {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${app.ai.load-balancer.strategy:weighted-round-robin}")
    private String loadBalancerStrategy;

    @Value("${app.ai.load-balancer.health-check.interval:30s}")
    private Duration healthCheckInterval;

    @Value("${app.ai.load-balancer.health-check.timeout:5s}")
    private Duration healthCheckTimeout;

    // Service registry
    private final Map<String, List<AIServiceInstance>> serviceRegistry = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, ServiceHealthStatus> healthStatus = new ConcurrentHashMap<>();

    /**
     * AI Service Instance
     */
    public static class AIServiceInstance {
        private final String id;
        private final String url;
        private final String model;
        private final int weight;
        private final int priority;
        private final AtomicLong activeConnections;
        private final AtomicLong totalRequests;
        private final AtomicLong totalResponseTime;
        private final AtomicLong failureCount;
        private final Set<String> capabilities;
        private volatile boolean healthy;
        private volatile Instant lastHealthCheck;

        public AIServiceInstance(String id, String url, String model, int weight, int priority) {
            this.id = id;
            this.url = url;
            this.model = model;
            this.weight = weight;
            this.priority = priority;
            this.activeConnections = new AtomicLong(0);
            this.totalRequests = new AtomicLong(0);
            this.totalResponseTime = new AtomicLong(0);
            this.failureCount = new AtomicLong(0);
            this.capabilities = new HashSet<>();
            this.healthy = true;
            this.lastHealthCheck = Instant.now();
        }

        // Getters and utility methods
        public String getId() { return id; }
        public String getUrl() { return url; }
        public String getModel() { return model; }
        public int getWeight() { return weight; }
        public int getPriority() { return priority; }
        public long getActiveConnections() { return activeConnections.get(); }
        public long getTotalRequests() { return totalRequests.get(); }
        public long getAverageResponseTime() { 
            long total = totalRequests.get();
            return total > 0 ? totalResponseTime.get() / total : 0;
        }
        public long getFailureCount() { return failureCount.get(); }
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public Instant getLastHealthCheck() { return lastHealthCheck; }
        public void setLastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
        public Set<String> getCapabilities() { return capabilities; }

        public void incrementActiveConnections() { activeConnections.incrementAndGet(); }
        public void decrementActiveConnections() { activeConnections.decrementAndGet(); }
        public void recordRequest(long responseTime) {
            totalRequests.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
        }
        public void recordFailure() { failureCount.incrementAndGet(); }
    }

    /**
     * Service Health Status
     */
    public static class ServiceHealthStatus {
        private final String serviceId;
        private volatile boolean healthy;
        private volatile Instant lastCheck;
        private volatile String lastError;
        private final AtomicInteger consecutiveFailures;

        public ServiceHealthStatus(String serviceId) {
            this.serviceId = serviceId;
            this.healthy = true;
            this.lastCheck = Instant.now();
            this.consecutiveFailures = new AtomicInteger(0);
        }

        public String getServiceId() { return serviceId; }
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public Instant getLastCheck() { return lastCheck; }
        public void setLastCheck(Instant lastCheck) { this.lastCheck = lastCheck; }
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
        public int getConsecutiveFailures() { return consecutiveFailures.get(); }
        public void recordFailure() { consecutiveFailures.incrementAndGet(); }
        public void recordSuccess() { consecutiveFailures.set(0); }
    }

    /**
     * Initialize AI service instances
     */
    public void initializeServices() {
        // GPT-4 instances
        List<AIServiceInstance> gpt4Instances = Arrays.asList(
            new AIServiceInstance("gpt4-primary", "http://localhost:5002", "gpt-4", 10, 1),
            new AIServiceInstance("gpt4-secondary", "http://localhost:5002", "gpt-4", 8, 2),
            new AIServiceInstance("gpt4-fallback", "http://localhost:5002", "gpt-4", 5, 3)
        );
        serviceRegistry.put("gpt-4", gpt4Instances);

        // Claude instances
        List<AIServiceInstance> claudeInstances = Arrays.asList(
            new AIServiceInstance("claude-primary", "http://localhost:5002", "claude-3", 10, 1),
            new AIServiceInstance("claude-secondary", "http://localhost:5002", "claude-3", 8, 2)
        );
        serviceRegistry.put("claude-3", claudeInstances);

        // Local model instances
        List<AIServiceInstance> localInstances = Arrays.asList(
            new AIServiceInstance("local-primary", "http://localhost:5002", "local-model", 5, 1),
            new AIServiceInstance("local-secondary", "http://localhost:5002", "local-model", 3, 2)
        );
        serviceRegistry.put("local-model", localInstances);

        // Initialize round robin counters
        serviceRegistry.keySet().forEach(service -> 
            roundRobinCounters.put(service, new AtomicInteger(0))
        );

        log.info("Initialized AI service registry with {} services", serviceRegistry.size());
    }

    /**
     * Select best AI service instance based on configured strategy
     */
    public Mono<AIServiceInstance> selectAIService(String modelType, AIRequest request) {
        return Mono.fromCallable(() -> {
            List<AIServiceInstance> instances = serviceRegistry.get(modelType);
            if (instances == null || instances.isEmpty()) {
                throw new RuntimeException("No instances available for model: " + modelType);
            }

            // Filter healthy instances
            List<AIServiceInstance> healthyInstances = instances.stream()
                    .filter(AIServiceInstance::isHealthy)
                    .toList();

            if (healthyInstances.isEmpty()) {
                log.warn("No healthy instances available for model: {}, using unhealthy instances", modelType);
                healthyInstances = instances;
            }

            return switch (loadBalancerStrategy.toLowerCase()) {
                case "round-robin" -> selectRoundRobin(modelType, healthyInstances);
                case "weighted-round-robin" -> selectWeightedRoundRobin(modelType, healthyInstances);
                case "least-connections" -> selectLeastConnections(healthyInstances);
                case "response-time" -> selectByResponseTime(healthyInstances);
                case "priority" -> selectByPriority(healthyInstances);
                default -> selectWeightedRoundRobin(modelType, healthyInstances);
            };
        });
    }

    /**
     * Round Robin selection
     */
    private AIServiceInstance selectRoundRobin(String modelType, List<AIServiceInstance> instances) {
        AtomicInteger counter = roundRobinCounters.get(modelType);
        int index = counter.getAndIncrement() % instances.size();
        return instances.get(index);
    }

    /**
     * Weighted Round Robin selection
     */
    private AIServiceInstance selectWeightedRoundRobin(String modelType, List<AIServiceInstance> instances) {
        int totalWeight = instances.stream().mapToInt(AIServiceInstance::getWeight).sum();
        AtomicInteger counter = roundRobinCounters.get(modelType);
        int weightedIndex = counter.getAndIncrement() % totalWeight;
        
        int currentWeight = 0;
        for (AIServiceInstance instance : instances) {
            currentWeight += instance.getWeight();
            if (weightedIndex < currentWeight) {
                return instance;
            }
        }
        
        return instances.get(0);
    }

    /**
     * Least Connections selection
     */
    private AIServiceInstance selectLeastConnections(List<AIServiceInstance> instances) {
        return instances.stream()
                .min(Comparator.comparingLong(AIServiceInstance::getActiveConnections))
                .orElse(instances.get(0));
    }

    /**
     * Response Time based selection
     */
    private AIServiceInstance selectByResponseTime(List<AIServiceInstance> instances) {
        return instances.stream()
                .min(Comparator.comparingLong(AIServiceInstance::getAverageResponseTime))
                .orElse(instances.get(0));
    }

    /**
     * Priority based selection
     */
    private AIServiceInstance selectByPriority(List<AIServiceInstance> instances) {
        return instances.stream()
                .min(Comparator.comparingInt(AIServiceInstance::getPriority))
                .orElse(instances.get(0));
    }

    /**
     * Execute AI request with load balancing
     */
    public Mono<AIResponse> executeAIRequest(String modelType, AIRequest request) {
        return selectAIService(modelType, request)
                .flatMap(instance -> {
                    log.debug("Routing AI request to instance: {} ({})", instance.getId(), instance.getUrl());
                    
                    instance.incrementActiveConnections();
                    long startTime = System.currentTimeMillis();
                    
                    return webClientBuilder.build()
                            .post()
                            .uri(instance.getUrl() + "/api/v1/llm/generate")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(AIResponse.class)
                            .map(response -> {
                                long responseTime = System.currentTimeMillis() - startTime;
                                instance.recordRequest(responseTime);
                                instance.decrementActiveConnections();
                                
                                log.debug("AI request completed: instance={}, responseTime={}ms", 
                                        instance.getId(), responseTime);
                                
                                return response;
                            })
                            .onErrorResume(error -> {
                                instance.recordFailure();
                                instance.decrementActiveConnections();
                                
                                log.error("AI request failed: instance={}, error={}", 
                                        instance.getId(), error.getMessage());
                                
                                // Try fallback instance
                                return tryFallbackInstance(modelType, request, instance);
                            });
                });
    }

    /**
     * Try fallback instance on failure
     */
    private Mono<AIResponse> tryFallbackInstance(String modelType, AIRequest request, AIServiceInstance failedInstance) {
        return selectAIService(modelType, request)
                .filter(instance -> !instance.getId().equals(failedInstance.getId()))
                .flatMap(instance -> {
                    log.warn("Trying fallback instance: {} for model: {}", instance.getId(), modelType);
                    
                    instance.incrementActiveConnections();
                    long startTime = System.currentTimeMillis();
                    
                    return webClientBuilder.build()
                            .post()
                            .uri(instance.getUrl() + "/api/v1/llm/generate")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(AIResponse.class)
                            .map(response -> {
                                long responseTime = System.currentTimeMillis() - startTime;
                                instance.recordRequest(responseTime);
                                instance.decrementActiveConnections();
                                return response;
                            })
                            .onErrorResume(error -> {
                                instance.recordFailure();
                                instance.decrementActiveConnections();
                                return Mono.error(new RuntimeException("All AI instances failed for model: " + modelType));
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No fallback instances available for model: " + modelType)));
    }

    /**
     * Perform health checks on all instances
     */
    public Mono<Void> performHealthChecks() {
        return Mono.fromRunnable(() -> {
            log.debug("Performing health checks on all AI service instances");
            
            serviceRegistry.values().stream()
                    .flatMap(Collection::stream)
                    .forEach(instance -> {
                        if (shouldPerformHealthCheck(instance)) {
                            performHealthCheck(instance);
                        }
                    });
        });
    }

    /**
     * Check if health check should be performed
     */
    private boolean shouldPerformHealthCheck(AIServiceInstance instance) {
        return instance.getLastHealthCheck().isBefore(Instant.now().minus(healthCheckInterval));
    }

    /**
     * Perform health check on specific instance
     */
    private void performHealthCheck(AIServiceInstance instance) {
        webClientBuilder.build()
                .get()
                .uri(instance.getUrl() + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(healthCheckTimeout)
                .subscribe(
                    response -> {
                        instance.setHealthy(true);
                        instance.setLastHealthCheck(Instant.now());
                        log.debug("Health check passed for instance: {}", instance.getId());
                    },
                    error -> {
                        instance.setHealthy(false);
                        instance.setLastHealthCheck(Instant.now());
                        log.warn("Health check failed for instance: {}, error: {}", 
                                instance.getId(), error.getMessage());
                    }
                );
    }

    /**
     * Get load balancing statistics
     */
    public Mono<Map<String, Object>> getLoadBalancingStats() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            serviceRegistry.forEach((modelType, instances) -> {
                Map<String, Object> modelStats = new HashMap<>();
                modelStats.put("totalInstances", instances.size());
                modelStats.put("healthyInstances", instances.stream().mapToInt(i -> i.isHealthy() ? 1 : 0).sum());
                modelStats.put("totalRequests", instances.stream().mapToLong(AIServiceInstance::getTotalRequests).sum());
                modelStats.put("activeConnections", instances.stream().mapToLong(AIServiceInstance::getActiveConnections).sum());
                modelStats.put("averageResponseTime", instances.stream()
                        .mapToLong(AIServiceInstance::getAverageResponseTime)
                        .filter(time -> time > 0)
                        .average()
                        .orElse(0));
                modelStats.put("totalFailures", instances.stream().mapToLong(AIServiceInstance::getFailureCount).sum());
                
                stats.put(modelType, modelStats);
            });
            
            stats.put("strategy", loadBalancerStrategy);
            stats.put("healthCheckInterval", healthCheckInterval.toString());
            
            return stats;
        });
    }

    /**
     * AI Request DTO
     */
    public static class AIRequest {
        private String prompt;
        private String model;
        private Map<String, Object> parameters;
        private Integer maxTokens;
        private Double temperature;
        private String userId;
        private String organizationId;

        // Getters and setters
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    }

    /**
     * AI Response DTO
     */
    public static class AIResponse {
        private String id;
        private String response;
        private String model;
        private Integer tokensUsed;
        private Long responseTime;
        private Map<String, Object> metadata;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Integer getTokensUsed() { return tokensUsed; }
        public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
        public Long getResponseTime() { return responseTime; }
        public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}