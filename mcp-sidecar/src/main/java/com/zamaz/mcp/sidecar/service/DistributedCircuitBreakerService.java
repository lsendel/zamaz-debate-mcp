package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Distributed Circuit Breaker Service using Redis for state coordination
 * 
 * Features:
 * - Distributed circuit breaker state across multiple instances
 * - Redis-based state synchronization
 * - Configurable failure thresholds and timeouts
 * - Automatic state transitions (CLOSED -> OPEN -> HALF_OPEN -> CLOSED)
 * - Metrics collection and monitoring
 * - Fallback mechanism support
 * - Service-specific and global circuit breakers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedCircuitBreakerService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${app.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${app.circuit-breaker.failure-threshold:5}")
    private int defaultFailureThreshold;

    @Value("${app.circuit-breaker.timeout:PT1M}")
    private Duration defaultTimeout;

    @Value("${app.circuit-breaker.half-open-max-calls:3}")
    private int defaultHalfOpenMaxCalls;

    @Value("${app.circuit-breaker.sliding-window-size:100}")
    private int defaultSlidingWindowSize;

    @Value("${app.circuit-breaker.minimum-number-of-calls:10}")
    private int defaultMinimumNumberOfCalls;

    // Local cache for circuit breaker states
    private final Map<String, CircuitBreakerState> localStateCache = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreakerConfig> circuitBreakerConfigs = new ConcurrentHashMap<>();

    /**
     * Circuit breaker states
     */
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    /**
     * Circuit breaker configuration
     */
    public static class CircuitBreakerConfig {
        private final String name;
        private final int failureThreshold;
        private final Duration timeout;
        private final int halfOpenMaxCalls;
        private final int slidingWindowSize;
        private final int minimumNumberOfCalls;
        private final double failureRateThreshold;

        public CircuitBreakerConfig(String name, int failureThreshold, Duration timeout,
                                   int halfOpenMaxCalls, int slidingWindowSize, 
                                   int minimumNumberOfCalls, double failureRateThreshold) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
            this.halfOpenMaxCalls = halfOpenMaxCalls;
            this.slidingWindowSize = slidingWindowSize;
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            this.failureRateThreshold = failureRateThreshold;
        }

        public String getName() { return name; }
        public int getFailureThreshold() { return failureThreshold; }
        public Duration getTimeout() { return timeout; }
        public int getHalfOpenMaxCalls() { return halfOpenMaxCalls; }
        public int getSlidingWindowSize() { return slidingWindowSize; }
        public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
        public double getFailureRateThreshold() { return failureRateThreshold; }
    }

    /**
     * Circuit breaker state
     */
    public static class CircuitBreakerState {
        private final String name;
        private volatile State state;
        private volatile Instant lastStateChange;
        private volatile int failureCount;
        private volatile int successCount;
        private volatile int halfOpenCallCount;
        private final AtomicLong totalCalls;
        private final AtomicLong successfulCalls;
        private final AtomicLong failedCalls;
        private final CircuitBreakerConfig config;

        public CircuitBreakerState(String name, CircuitBreakerConfig config) {
            this.name = name;
            this.config = config;
            this.state = State.CLOSED;
            this.lastStateChange = Instant.now();
            this.failureCount = 0;
            this.successCount = 0;
            this.halfOpenCallCount = 0;
            this.totalCalls = new AtomicLong(0);
            this.successfulCalls = new AtomicLong(0);
            this.failedCalls = new AtomicLong(0);
        }

        public String getName() { return name; }
        public State getState() { return state; }
        public void setState(State state) { 
            this.state = state; 
            this.lastStateChange = Instant.now();
        }
        public Instant getLastStateChange() { return lastStateChange; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getHalfOpenCallCount() { return halfOpenCallCount; }
        public void setHalfOpenCallCount(int halfOpenCallCount) { this.halfOpenCallCount = halfOpenCallCount; }
        public long getTotalCalls() { return totalCalls.get(); }
        public long getSuccessfulCalls() { return successfulCalls.get(); }
        public long getFailedCalls() { return failedCalls.get(); }
        public CircuitBreakerConfig getConfig() { return config; }

        public void recordSuccess() {
            totalCalls.incrementAndGet();
            successfulCalls.incrementAndGet();
            successCount++;
            failureCount = 0;
        }

        public void recordFailure() {
            totalCalls.incrementAndGet();
            failedCalls.incrementAndGet();
            failureCount++;
            successCount = 0;
        }

        public double getFailureRate() {
            long total = totalCalls.get();
            if (total == 0) return 0.0;
            return (double) failedCalls.get() / total;
        }

        public boolean shouldTransitionToOpen() {
            return failureCount >= config.getFailureThreshold() || 
                   (totalCalls.get() >= config.getMinimumNumberOfCalls() && 
                    getFailureRate() >= config.getFailureRateThreshold());
        }

        public boolean shouldTransitionToHalfOpen() {
            return state == State.OPEN && 
                   Duration.between(lastStateChange, Instant.now()).compareTo(config.getTimeout()) >= 0;
        }

        public boolean shouldTransitionToClosed() {
            return state == State.HALF_OPEN && successCount >= config.getHalfOpenMaxCalls();
        }
    }

    /**
     * Circuit breaker execution result
     */
    public static class CircuitBreakerResult<T> {
        private final boolean success;
        private final T result;
        private final String error;
        private final State state;
        private final boolean fallbackUsed;

        public CircuitBreakerResult(boolean success, T result, String error, State state, boolean fallbackUsed) {
            this.success = success;
            this.result = result;
            this.error = error;
            this.state = state;
            this.fallbackUsed = fallbackUsed;
        }

        public boolean isSuccess() { return success; }
        public T getResult() { return result; }
        public String getError() { return error; }
        public State getState() { return state; }
        public boolean isFallbackUsed() { return fallbackUsed; }
    }

    /**
     * Initialize default circuit breaker configurations
     */
    public void initializeDefaultConfigurations() {
        // Organization service circuit breaker
        registerCircuitBreaker("organization-service", new CircuitBreakerConfig(
            "organization-service", 
            5, 
            Duration.ofMinutes(1), 
            3, 
            100, 
            10, 
            0.5
        ));

        // LLM service circuit breaker
        registerCircuitBreaker("llm-service", new CircuitBreakerConfig(
            "llm-service", 
            3, 
            Duration.ofSeconds(30), 
            2, 
            50, 
            5, 
            0.6
        ));

        // Debate controller circuit breaker
        registerCircuitBreaker("debate-controller", new CircuitBreakerConfig(
            "debate-controller", 
            5, 
            Duration.ofMinutes(1), 
            3, 
            100, 
            10, 
            0.5
        ));

        // RAG service circuit breaker
        registerCircuitBreaker("rag-service", new CircuitBreakerConfig(
            "rag-service", 
            4, 
            Duration.ofSeconds(45), 
            2, 
            75, 
            8, 
            0.55
        ));

        // External API circuit breaker
        registerCircuitBreaker("external-api", new CircuitBreakerConfig(
            "external-api", 
            10, 
            Duration.ofMinutes(2), 
            5, 
            200, 
            20, 
            0.4
        ));

        log.info("Initialized {} circuit breaker configurations", circuitBreakerConfigs.size());
    }

    /**
     * Register a circuit breaker
     */
    public void registerCircuitBreaker(String name, CircuitBreakerConfig config) {
        circuitBreakerConfigs.put(name, config);
        localStateCache.put(name, new CircuitBreakerState(name, config));
        log.debug("Registered circuit breaker: {}", name);
    }

    /**
     * Execute operation with circuit breaker protection
     */
    public <T> Mono<CircuitBreakerResult<T>> executeWithCircuitBreaker(
            String circuitBreakerName, 
            Supplier<Mono<T>> operation) {
        return executeWithCircuitBreaker(circuitBreakerName, operation, null);
    }

    /**
     * Execute operation with circuit breaker protection and fallback
     */
    public <T> Mono<CircuitBreakerResult<T>> executeWithCircuitBreaker(
            String circuitBreakerName, 
            Supplier<Mono<T>> operation, 
            Supplier<Mono<T>> fallback) {
        
        if (!circuitBreakerEnabled) {
            return operation.get()
                    .map(result -> new CircuitBreakerResult<>(true, result, null, State.CLOSED, false))
                    .onErrorResume(error -> {
                        if (fallback != null) {
                            return fallback.get()
                                    .map(result -> new CircuitBreakerResult<>(true, result, null, State.CLOSED, true))
                                    .onErrorReturn(new CircuitBreakerResult<>(false, null, error.getMessage(), State.CLOSED, true));
                        }
                        return Mono.just(new CircuitBreakerResult<>(false, null, error.getMessage(), State.CLOSED, false));
                    });
        }

        return getCircuitBreakerState(circuitBreakerName)
                .flatMap(state -> {
                    // Check if circuit breaker allows execution
                    if (!allowsExecution(state)) {
                        // Circuit is open, use fallback if available
                        if (fallback != null) {
                            return fallback.get()
                                    .map(result -> new CircuitBreakerResult<>(true, result, null, state.getState(), true))
                                    .onErrorReturn(new CircuitBreakerResult<>(false, null, "Circuit breaker is open and fallback failed", state.getState(), true));
                        }
                        return Mono.just(new CircuitBreakerResult<>(false, null, "Circuit breaker is open", state.getState(), false));
                    }

                    // Execute the operation
                    return operation.get()
                            .flatMap(result -> {
                                // Record success
                                return recordSuccess(circuitBreakerName, state)
                                        .map(updatedState -> new CircuitBreakerResult<>(true, result, null, updatedState.getState(), false));
                            })
                            .onErrorResume(error -> {
                                // Record failure
                                return recordFailure(circuitBreakerName, state, error)
                                        .flatMap(updatedState -> {
                                            // Try fallback if available
                                            if (fallback != null) {
                                                return fallback.get()
                                                        .map(result -> new CircuitBreakerResult<>(true, result, null, updatedState.getState(), true))
                                                        .onErrorReturn(new CircuitBreakerResult<>(false, null, error.getMessage(), updatedState.getState(), true));
                                            }
                                            return Mono.just(new CircuitBreakerResult<>(false, null, error.getMessage(), updatedState.getState(), false));
                                        });
                            });
                });
    }

    /**
     * Get circuit breaker state from Redis with local cache fallback
     */
    private Mono<CircuitBreakerState> getCircuitBreakerState(String name) {
        return getDistributedState(name)
                .switchIfEmpty(Mono.defer(() -> {
                    // Fallback to local cache
                    CircuitBreakerState localState = localStateCache.get(name);
                    if (localState != null) {
                        return Mono.just(localState);
                    }
                    
                    // Create new state if not found
                    CircuitBreakerConfig config = circuitBreakerConfigs.get(name);
                    if (config == null) {
                        config = new CircuitBreakerConfig(name, defaultFailureThreshold, defaultTimeout, 
                                defaultHalfOpenMaxCalls, defaultSlidingWindowSize, defaultMinimumNumberOfCalls, 0.5);
                        circuitBreakerConfigs.put(name, config);
                    }
                    
                    CircuitBreakerState newState = new CircuitBreakerState(name, config);
                    localStateCache.put(name, newState);
                    return Mono.just(newState);
                }));
    }

    /**
     * Get distributed state from Redis
     */
    private Mono<CircuitBreakerState> getDistributedState(String name) {
        String key = "circuit_breaker:" + name;
        
        return redisTemplate.opsForHash().entries(key)
                .collectMap(entry -> (String) entry.getKey(), entry -> (String) entry.getValue())
                .filter(map -> !map.isEmpty())
                .map(stateMap -> {
                    CircuitBreakerConfig config = circuitBreakerConfigs.get(name);
                    if (config == null) {
                        config = new CircuitBreakerConfig(name, defaultFailureThreshold, defaultTimeout,
                                defaultHalfOpenMaxCalls, defaultSlidingWindowSize, defaultMinimumNumberOfCalls, 0.5);
                    }
                    
                    CircuitBreakerState state = new CircuitBreakerState(name, config);
                    state.setState(State.valueOf(stateMap.getOrDefault("state", "CLOSED")));
                    state.setFailureCount(Integer.parseInt(stateMap.getOrDefault("failureCount", "0")));
                    state.setSuccessCount(Integer.parseInt(stateMap.getOrDefault("successCount", "0")));
                    state.setHalfOpenCallCount(Integer.parseInt(stateMap.getOrDefault("halfOpenCallCount", "0")));
                    
                    // Update local cache
                    localStateCache.put(name, state);
                    return state;
                })
                .onErrorResume(error -> {
                    log.warn("Failed to get distributed circuit breaker state for {}: {}", name, error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Update distributed state in Redis
     */
    private Mono<Void> updateDistributedState(CircuitBreakerState state) {
        String key = "circuit_breaker:" + state.getName();
        
        Map<String, String> stateMap = Map.of(
            "state", state.getState().name(),
            "failureCount", String.valueOf(state.getFailureCount()),
            "successCount", String.valueOf(state.getSuccessCount()),
            "halfOpenCallCount", String.valueOf(state.getHalfOpenCallCount()),
            "lastStateChange", state.getLastStateChange().toString(),
            "totalCalls", String.valueOf(state.getTotalCalls()),
            "successfulCalls", String.valueOf(state.getSuccessfulCalls()),
            "failedCalls", String.valueOf(state.getFailedCalls())
        );
        
        return redisTemplate.opsForHash().putAll(key, stateMap)
                .then(redisTemplate.expire(key, Duration.ofHours(1)))
                .then()
                .doOnSuccess(v -> log.debug("Updated distributed state for circuit breaker: {}", state.getName()))
                .onErrorResume(error -> {
                    log.warn("Failed to update distributed state for {}: {}", state.getName(), error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Check if circuit breaker allows execution
     */
    private boolean allowsExecution(CircuitBreakerState state) {
        switch (state.getState()) {
            case CLOSED:
                return true;
            case OPEN:
                // Check if timeout has passed to transition to half-open
                if (state.shouldTransitionToHalfOpen()) {
                    transitionToHalfOpen(state);
                    return true;
                }
                return false;
            case HALF_OPEN:
                // Allow limited calls in half-open state
                return state.getHalfOpenCallCount() < state.getConfig().getHalfOpenMaxCalls();
            default:
                return false;
        }
    }

    /**
     * Record successful execution
     */
    private Mono<CircuitBreakerState> recordSuccess(String name, CircuitBreakerState state) {
        state.recordSuccess();
        
        // Check state transitions
        if (state.getState() == State.HALF_OPEN && state.shouldTransitionToClosed()) {
            transitionToClosed(state);
        }
        
        // Update distributed state
        return updateDistributedState(state)
                .then(Mono.just(state))
                .doOnSuccess(s -> metricsCollectorService.recordCircuitBreakerSuccess(name));
    }

    /**
     * Record failed execution
     */
    private Mono<CircuitBreakerState> recordFailure(String name, CircuitBreakerState state, Throwable error) {
        state.recordFailure();
        
        // Check state transitions
        if (state.getState() == State.CLOSED && state.shouldTransitionToOpen()) {
            transitionToOpen(state);
        } else if (state.getState() == State.HALF_OPEN) {
            transitionToOpen(state);
        }
        
        // Update distributed state
        return updateDistributedState(state)
                .then(Mono.just(state))
                .doOnSuccess(s -> metricsCollectorService.recordCircuitBreakerFailure(name, error.getClass().getSimpleName()));
    }

    /**
     * Transition to OPEN state
     */
    private void transitionToOpen(CircuitBreakerState state) {
        State previousState = state.getState();
        state.setState(State.OPEN);
        state.setHalfOpenCallCount(0);
        
        log.warn("Circuit breaker {} transitioned from {} to OPEN", state.getName(), previousState);
        metricsCollectorService.recordCircuitBreakerStateChange(state.getName(), previousState.name(), State.OPEN.name());
    }

    /**
     * Transition to HALF_OPEN state
     */
    private void transitionToHalfOpen(CircuitBreakerState state) {
        State previousState = state.getState();
        state.setState(State.HALF_OPEN);
        state.setHalfOpenCallCount(0);
        
        log.info("Circuit breaker {} transitioned from {} to HALF_OPEN", state.getName(), previousState);
        metricsCollectorService.recordCircuitBreakerStateChange(state.getName(), previousState.name(), State.HALF_OPEN.name());
    }

    /**
     * Transition to CLOSED state
     */
    private void transitionToClosed(CircuitBreakerState state) {
        State previousState = state.getState();
        state.setState(State.CLOSED);
        state.setFailureCount(0);
        state.setSuccessCount(0);
        state.setHalfOpenCallCount(0);
        
        log.info("Circuit breaker {} transitioned from {} to CLOSED", state.getName(), previousState);
        metricsCollectorService.recordCircuitBreakerStateChange(state.getName(), previousState.name(), State.CLOSED.name());
    }

    /**
     * Get circuit breaker status
     */
    public Mono<Map<String, Object>> getCircuitBreakerStatus(String name) {
        return getCircuitBreakerState(name)
                .map(state -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("name", state.getName());
                    status.put("state", state.getState().name());
                    status.put("failureCount", state.getFailureCount());
                    status.put("successCount", state.getSuccessCount());
                    status.put("totalCalls", state.getTotalCalls());
                    status.put("successfulCalls", state.getSuccessfulCalls());
                    status.put("failedCalls", state.getFailedCalls());
                    status.put("failureRate", state.getFailureRate());
                    status.put("lastStateChange", state.getLastStateChange().toString());
                    status.put("config", Map.of(
                        "failureThreshold", state.getConfig().getFailureThreshold(),
                        "timeout", state.getConfig().getTimeout().toString(),
                        "halfOpenMaxCalls", state.getConfig().getHalfOpenMaxCalls(),
                        "slidingWindowSize", state.getConfig().getSlidingWindowSize(),
                        "minimumNumberOfCalls", state.getConfig().getMinimumNumberOfCalls(),
                        "failureRateThreshold", state.getConfig().getFailureRateThreshold()
                    ));
                    return status;
                });
    }

    /**
     * Get all circuit breaker statuses
     */
    public Mono<Map<String, Object>> getAllCircuitBreakerStatuses() {
        return Mono.fromCallable(() -> {
            Map<String, Object> allStatuses = new HashMap<>();
            
            for (String name : circuitBreakerConfigs.keySet()) {
                CircuitBreakerState state = localStateCache.get(name);
                if (state != null) {
                    Map<String, Object> status = new HashMap<>();
                    status.put("state", state.getState().name());
                    status.put("failureCount", state.getFailureCount());
                    status.put("totalCalls", state.getTotalCalls());
                    status.put("failureRate", state.getFailureRate());
                    status.put("lastStateChange", state.getLastStateChange().toString());
                    allStatuses.put(name, status);
                }
            }
            
            return allStatuses;
        });
    }

    /**
     * Reset circuit breaker
     */
    public Mono<Void> resetCircuitBreaker(String name) {
        return getCircuitBreakerState(name)
                .flatMap(state -> {
                    state.setState(State.CLOSED);
                    state.setFailureCount(0);
                    state.setSuccessCount(0);
                    state.setHalfOpenCallCount(0);
                    
                    return updateDistributedState(state)
                            .doOnSuccess(v -> log.info("Reset circuit breaker: {}", name));
                });
    }

    /**
     * Force circuit breaker state
     */
    public Mono<Void> forceCircuitBreakerState(String name, State state) {
        return getCircuitBreakerState(name)
                .flatMap(cbState -> {
                    State previousState = cbState.getState();
                    cbState.setState(state);
                    
                    return updateDistributedState(cbState)
                            .doOnSuccess(v -> {
                                log.info("Forced circuit breaker {} from {} to {}", name, previousState, state);
                                metricsCollectorService.recordCircuitBreakerStateChange(name, previousState.name(), state.name());
                            });
                });
    }
}