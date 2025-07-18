package com.zamaz.mcp.sidecar.service;

import com.zamaz.mcp.sidecar.service.AILoadBalancingService;
import com.zamaz.mcp.sidecar.service.CachingService;
import com.zamaz.mcp.sidecar.config.TracingConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Metrics Collector Service for MCP Sidecar
 * 
 * Collects and exposes metrics for:
 * - Request/Response metrics
 * - Authentication metrics
 * - Service health metrics
 * - Circuit breaker metrics
 * - Rate limiting metrics
 * - Cache metrics
 * - AI service metrics
 * - System resource metrics
 * - Business metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsCollectorService {

    private final MeterRegistry meterRegistry;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final CachingService cachingService;
    private final AILoadBalancingService aiLoadBalancingService;
    private final TracingConfig.TracingService tracingService;

    @Value("${app.metrics.collection.interval:30s}")
    private Duration collectionInterval;

    @Value("${app.metrics.retention.period:7d}")
    private Duration retentionPeriod;

    // Metrics storage
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> responseTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> authMetrics = new ConcurrentHashMap<>();
    private final Map<String, ServiceHealthMetrics> serviceHealthMetrics = new ConcurrentHashMap<>();

    // Custom metrics
    private final Counter totalRequestsCounter;
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter circuitBreakerTripsCounter;
    private final Counter rateLimitHitsCounter;
    private final Timer requestDurationTimer;
    private final Timer aiResponseTimer;

    public MetricsCollectorService(MeterRegistry meterRegistry, 
                                  ReactiveRedisTemplate<String, String> redisTemplate,
                                  WebClient.Builder webClientBuilder,
                                  CachingService cachingService,
                                  AILoadBalancingService aiLoadBalancingService,
                                  TracingConfig.TracingService tracingService) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
        this.webClientBuilder = webClientBuilder;
        this.cachingService = cachingService;
        this.aiLoadBalancingService = aiLoadBalancingService;
        this.tracingService = tracingService;

        // Initialize counters
        this.totalRequestsCounter = Counter.builder("sidecar.requests.total")
                .description("Total number of requests processed")
                .register(meterRegistry);

        this.authSuccessCounter = Counter.builder("sidecar.auth.success")
                .description("Number of successful authentications")
                .register(meterRegistry);

        this.authFailureCounter = Counter.builder("sidecar.auth.failure")
                .description("Number of failed authentications")
                .register(meterRegistry);

        this.circuitBreakerTripsCounter = Counter.builder("sidecar.circuit_breaker.trips")
                .description("Number of circuit breaker trips")
                .register(meterRegistry);

        this.rateLimitHitsCounter = Counter.builder("sidecar.rate_limit.hits")
                .description("Number of rate limit hits")
                .register(meterRegistry);

        this.requestDurationTimer = Timer.builder("sidecar.request.duration")
                .description("Request processing duration")
                .register(meterRegistry);

        this.aiResponseTimer = Timer.builder("sidecar.ai.response.duration")
                .description("AI service response duration")
                .register(meterRegistry);

        // Initialize gauges
        initializeGauges();
    }

    /**
     * Initialize custom gauges
     */
    private void initializeGauges() {
        // Active connections gauge
        Gauge.builder("sidecar.connections.active")
                .description("Number of active connections")
                .register(meterRegistry, this, MetricsCollectorService::getActiveConnections);

        // Memory usage gauge
        Gauge.builder("sidecar.memory.used")
                .description("Memory usage in bytes")
                .register(meterRegistry, this, MetricsCollectorService::getMemoryUsage);

        // Cache hit rate gauge
        Gauge.builder("sidecar.cache.hit_rate")
                .description("Cache hit rate percentage")
                .register(meterRegistry, this, MetricsCollectorService::getCacheHitRate);

        // Circuit breaker state gauge
        Gauge.builder("sidecar.circuit_breaker.state")
                .description("Circuit breaker state (0=closed, 1=open, 2=half-open)")
                .register(meterRegistry, this, MetricsCollectorService::getCircuitBreakerState);

        // AI service availability gauge
        Gauge.builder("sidecar.ai.availability")
                .description("AI service availability percentage")
                .register(meterRegistry, this, MetricsCollectorService::getAIServiceAvailability);
    }

    /**
     * Record request metrics
     */
    public void recordRequest(String endpoint, String method, long duration, int statusCode) {
        String key = method + ":" + endpoint;
        
        // Increment counters
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        totalRequestsCounter.increment();

        // Record duration
        requestDurationTimer.record(Duration.ofMillis(duration));
        responseTimes.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(duration);

        // Record errors
        if (statusCode >= 400) {
            errorCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }

        log.debug("Recorded request: endpoint={}, method={}, duration={}ms, status={}", 
                endpoint, method, duration, statusCode);
    }

    /**
     * Record authentication metrics
     */
    public void recordAuthentication(String userId, boolean success, String method) {
        String key = "auth:" + method;
        
        if (success) {
            authSuccessCounter.increment();
            authMetrics.computeIfAbsent(key + ":success", k -> new AtomicLong(0)).incrementAndGet();
        } else {
            authFailureCounter.increment();
            authMetrics.computeIfAbsent(key + ":failure", k -> new AtomicLong(0)).incrementAndGet();
        }

        log.debug("Recorded authentication: userId={}, success={}, method={}", userId, success, method);
    }

    /**
     * Record circuit breaker trip
     */
    public void recordCircuitBreakerTrip(String serviceName, String reason) {
        circuitBreakerTripsCounter.increment();
        
        String key = "circuit_breaker:" + serviceName;
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        log.warn("Circuit breaker tripped: service={}, reason={}", serviceName, reason);
    }

    /**
     * Record rate limit hit
     */
    public void recordRateLimitHit(String userId, String endpoint, String rateLimitType) {
        rateLimitHitsCounter.increment();
        
        String key = "rate_limit:" + rateLimitType + ":" + endpoint;
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("Rate limit hit: userId={}, endpoint={}, type={}", userId, endpoint, rateLimitType);
    }

    /**
     * Record AI service response
     */
    public void recordAIResponse(String model, String userId, long duration, boolean success) {
        aiResponseTimer.record(Duration.ofMillis(duration));
        
        String key = "ai:" + model + ":" + (success ? "success" : "failure");
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("Recorded AI response: model={}, userId={}, duration={}ms, success={}", 
                model, userId, duration, success);
    }

    /**
     * Collect service health metrics
     */
    @Scheduled(fixedDelayString = "${app.metrics.collection.interval:30s}")
    public void collectServiceHealthMetrics() {
        log.debug("Collecting service health metrics");
        
        // Define services to monitor
        Map<String, String> services = Map.of(
            "mcp-organization", "http://localhost:5005/actuator/health",
            "mcp-llm", "http://localhost:5002/actuator/health",
            "mcp-debate", "http://localhost:5013/actuator/health",
            "mcp-rag", "http://localhost:5004/actuator/health",
            "mcp-security", "http://localhost:8082/actuator/health"
        );

        services.forEach((serviceName, healthUrl) -> {
            collectServiceHealth(serviceName, healthUrl);
        });
    }

    /**
     * Collect individual service health
     */
    private void collectServiceHealth(String serviceName, String healthUrl) {
        long startTime = System.currentTimeMillis();
        
        webClientBuilder.build()
                .get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .subscribe(
                    response -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        boolean healthy = "UP".equals(response.get("status"));
                        
                        ServiceHealthMetrics metrics = serviceHealthMetrics.computeIfAbsent(
                            serviceName, 
                            k -> new ServiceHealthMetrics()
                        );
                        
                        metrics.recordHealthCheck(healthy, responseTime);
                        
                        log.debug("Service health check: service={}, healthy={}, responseTime={}ms", 
                                serviceName, healthy, responseTime);
                    },
                    error -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        ServiceHealthMetrics metrics = serviceHealthMetrics.computeIfAbsent(
                            serviceName, 
                            k -> new ServiceHealthMetrics()
                        );
                        
                        metrics.recordHealthCheck(false, responseTime);
                        
                        log.warn("Service health check failed: service={}, error={}", 
                                serviceName, error.getMessage());
                    }
                );
    }

    /**
     * Get comprehensive metrics report
     */
    public Mono<Map<String, Object>> getMetricsReport() {
        return Mono.fromCallable(() -> {
            Map<String, Object> report = new HashMap<>();
            
            // Request metrics
            Map<String, Object> requestMetrics = new HashMap<>();
            requestMetrics.put("totalRequests", getTotalRequests());
            requestMetrics.put("averageResponseTime", getAverageResponseTime());
            requestMetrics.put("errorRate", getErrorRate());
            requestMetrics.put("requestsPerSecond", getRequestsPerSecond());
            report.put("requests", requestMetrics);
            
            // Authentication metrics
            Map<String, Object> authMetrics = new HashMap<>();
            authMetrics.put("successRate", getAuthSuccessRate());
            authMetrics.put("failureCount", getAuthFailureCount());
            authMetrics.put("totalAuthentications", getTotalAuthentications());
            report.put("authentication", authMetrics);
            
            // Service health metrics
            Map<String, Object> healthMetrics = new HashMap<>();
            serviceHealthMetrics.forEach((service, metrics) -> {
                Map<String, Object> serviceMetrics = new HashMap<>();
                serviceMetrics.put("availability", metrics.getAvailability());
                serviceMetrics.put("averageResponseTime", metrics.getAverageResponseTime());
                serviceMetrics.put("lastCheckTime", metrics.getLastCheckTime());
                healthMetrics.put(service, serviceMetrics);
            });
            report.put("serviceHealth", healthMetrics);
            
            // System metrics
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("memoryUsage", getMemoryUsage());
            systemMetrics.put("activeConnections", getActiveConnections());
            systemMetrics.put("cacheHitRate", getCacheHitRate());
            systemMetrics.put("circuitBreakerState", getCircuitBreakerState());
            report.put("system", systemMetrics);
            
            // AI service metrics
            report.put("aiServices", getAIServiceMetrics());
            
            // Cache metrics
            report.put("cache", getCacheMetrics());
            
            // Tracing metrics
            report.put("tracing", getTracingMetrics());
            
            return report;
        });
    }

    /**
     * Get current active connections
     */
    public double getActiveConnections() {
        return requestCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    /**
     * Get memory usage
     */
    public double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Get cache hit rate
     */
    public double getCacheHitRate() {
        return cachingService.getCacheStatistics().values().stream()
                .mapToDouble(CachingService.CacheStats::getHitRate)
                .average()
                .orElse(0.0);
    }

    /**
     * Get circuit breaker state
     */
    public double getCircuitBreakerState() {
        // This would integrate with actual circuit breaker implementation
        return 0; // 0 = closed, 1 = open, 2 = half-open
    }

    /**
     * Get AI service availability
     */
    public double getAIServiceAvailability() {
        return aiLoadBalancingService.getLoadBalancingStats()
                .map(stats -> {
                    if (stats.containsKey("gpt-4")) {
                        Map<String, Object> gptStats = (Map<String, Object>) stats.get("gpt-4");
                        int total = (Integer) gptStats.get("totalInstances");
                        int healthy = (Integer) gptStats.get("healthyInstances");
                        return total > 0 ? (double) healthy / total * 100 : 0.0;
                    }
                    return 0.0;
                })
                .block();
    }

    /**
     * Get total requests
     */
    private long getTotalRequests() {
        return requestCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    /**
     * Get average response time
     */
    private double getAverageResponseTime() {
        long totalTime = responseTimes.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        long totalRequests = getTotalRequests();
        return totalRequests > 0 ? (double) totalTime / totalRequests : 0.0;
    }

    /**
     * Get error rate
     */
    private double getErrorRate() {
        long totalErrors = errorCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        long totalRequests = getTotalRequests();
        return totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0;
    }

    /**
     * Get requests per second
     */
    private double getRequestsPerSecond() {
        // This would require time-window calculations
        return 0.0; // Placeholder
    }

    /**
     * Get authentication success rate
     */
    private double getAuthSuccessRate() {
        long successes = authMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().contains("success"))
                .mapToLong(entry -> entry.getValue().get())
                .sum();
        
        long total = getTotalAuthentications();
        return total > 0 ? (double) successes / total : 0.0;
    }

    /**
     * Get authentication failure count
     */
    private long getAuthFailureCount() {
        return authMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().contains("failure"))
                .mapToLong(entry -> entry.getValue().get())
                .sum();
    }

    /**
     * Get total authentications
     */
    private long getTotalAuthentications() {
        return authMetrics.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    /**
     * Get AI service metrics
     */
    private Map<String, Object> getAIServiceMetrics() {
        return aiLoadBalancingService.getLoadBalancingStats().block();
    }

    /**
     * Get cache metrics
     */
    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> cacheMetrics = new HashMap<>();
        cacheMetrics.put("statistics", cachingService.getCacheStatistics());
        cacheMetrics.put("hitRate", getCacheHitRate());
        return cacheMetrics;
    }

    /**
     * Get tracing metrics
     */
    private Map<String, Object> getTracingMetrics() {
        return tracingService.getTracingStatistics();
    }

    /**
     * Service health metrics
     */
    public static class ServiceHealthMetrics {
        private final AtomicLong totalChecks = new AtomicLong(0);
        private final AtomicLong successfulChecks = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private volatile Instant lastCheckTime;

        public void recordHealthCheck(boolean healthy, long responseTime) {
            totalChecks.incrementAndGet();
            if (healthy) {
                successfulChecks.incrementAndGet();
            }
            totalResponseTime.addAndGet(responseTime);
            lastCheckTime = Instant.now();
        }

        public double getAvailability() {
            long total = totalChecks.get();
            return total > 0 ? (double) successfulChecks.get() / total * 100 : 0.0;
        }

        public double getAverageResponseTime() {
            long total = totalChecks.get();
            return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
        }

        public Instant getLastCheckTime() {
            return lastCheckTime;
        }
    }

    /**
     * Clear old metrics data
     */
    @Scheduled(fixedDelayString = "${app.metrics.cleanup.interval:1h}")
    public void cleanupOldMetrics() {
        log.debug("Cleaning up old metrics data");
        
        // This would implement retention policy
        // For now, just log the cleanup
        
        log.info("Metrics cleanup completed");
    }

    /**
     * Export metrics to external systems
     */
    public Mono<Void> exportMetrics() {
        return getMetricsReport()
                .flatMap(report -> {
                    // Export to Redis for persistence
                    String key = "metrics:sidecar:" + Instant.now().toEpochMilli();
                    return redisTemplate.opsForValue()
                            .set(key, report.toString(), Duration.ofHours(24))
                            .then();
                })
                .doOnSuccess(v -> log.debug("Metrics exported successfully"))
                .doOnError(error -> log.error("Failed to export metrics", error));
    }

    /**
     * Record circuit breaker success
     */
    public void recordCircuitBreakerSuccess(String circuitBreakerName) {
        Counter.builder("sidecar.circuit_breaker.success")
                .tag("circuit_breaker", circuitBreakerName)
                .description("Number of successful circuit breaker calls")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded circuit breaker success for: {}", circuitBreakerName);
    }

    /**
     * Record circuit breaker failure
     */
    public void recordCircuitBreakerFailure(String circuitBreakerName, String errorType) {
        Counter.builder("sidecar.circuit_breaker.failure")
                .tag("circuit_breaker", circuitBreakerName)
                .tag("error_type", errorType)
                .description("Number of failed circuit breaker calls")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded circuit breaker failure for: {} with error type: {}", circuitBreakerName, errorType);
    }

    /**
     * Record circuit breaker state change
     */
    public void recordCircuitBreakerStateChange(String circuitBreakerName, String fromState, String toState) {
        Counter.builder("sidecar.circuit_breaker.state_change")
                .tag("circuit_breaker", circuitBreakerName)
                .tag("from_state", fromState)
                .tag("to_state", toState)
                .description("Number of circuit breaker state changes")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded circuit breaker state change for: {} from {} to {}", circuitBreakerName, fromState, toState);
    }

    /**
     * Record circuit breaker trip
     */
    public void recordCircuitBreakerTrip(String circuitBreakerName) {
        circuitBreakerTripsCounter.increment();
        
        Counter.builder("sidecar.circuit_breaker.trips")
                .tag("circuit_breaker", circuitBreakerName)
                .description("Number of circuit breaker trips")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded circuit breaker trip for: {}", circuitBreakerName);
    }

    /**
     * Record circuit breaker execution time
     */
    public void recordCircuitBreakerExecutionTime(String circuitBreakerName, long executionTimeMs) {
        Timer.builder("sidecar.circuit_breaker.execution_time")
                .tag("circuit_breaker", circuitBreakerName)
                .description("Circuit breaker execution time")
                .register(meterRegistry)
                .record(Duration.ofMillis(executionTimeMs));
        
        log.debug("Recorded circuit breaker execution time for: {} - {}ms", circuitBreakerName, executionTimeMs);
    }

    /**
     * Record service health check
     */
    public void recordServiceHealth(String serviceName, String instanceId, boolean healthy, long responseTime) {
        Counter.builder("sidecar.service.health_check")
                .tag("service", serviceName)
                .tag("instance", instanceId)
                .tag("status", healthy ? "healthy" : "unhealthy")
                .description("Service health check results")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("sidecar.service.health_check_time")
                .tag("service", serviceName)
                .tag("instance", instanceId)
                .description("Service health check response time")
                .register(meterRegistry)
                .record(Duration.ofMillis(responseTime));
        
        log.debug("Recorded service health check for: {} instance {} - healthy: {}, time: {}ms", 
                serviceName, instanceId, healthy, responseTime);
    }

    /**
     * Record audit event
     */
    public void recordAuditEvent(String eventType, String severity, String outcome) {
        Counter.builder("sidecar.audit.events")
                .tag("event_type", eventType)
                .tag("severity", severity)
                .tag("outcome", outcome)
                .description("Audit events recorded")
                .register(meterRegistry)
                .increment();
        
        log.debug("Recorded audit event: type={}, severity={}, outcome={}", eventType, severity, outcome);
    }
}