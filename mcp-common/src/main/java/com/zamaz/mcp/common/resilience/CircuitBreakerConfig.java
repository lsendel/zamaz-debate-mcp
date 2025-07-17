package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Resilience4j Circuit Breaker pattern implementation.
 * Provides fault tolerance and prevents cascading failures in distributed systems.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.resilience.circuit-breaker")
@Data
@Slf4j
public class CircuitBreakerConfig {

    /**
     * Global circuit breaker configuration defaults
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * Service-specific circuit breaker configurations
     */
    private Map<String, ServiceConfig> services = new HashMap<>();

    /**
     * Whether to enable circuit breaker metrics
     */
    private boolean metricsEnabled = true;

    /**
     * Whether to enable detailed event logging
     */
    private boolean eventLoggingEnabled = true;

    /**
     * Create and configure the circuit breaker registry
     */
    @Bean
    @Primary
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        log.info("Initializing Circuit Breaker Registry with global config: {}", global);

        // Create registry with default configuration
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(global.getFailureRateThreshold())
                .slowCallRateThreshold(global.getSlowCallRateThreshold())
                .slowCallDurationThreshold(global.getSlowCallDurationThreshold())
                .permittedNumberOfCallsInHalfOpenState(global.getPermittedCallsInHalfOpenState())
                .slidingWindowType(global.getSlidingWindowType())
                .slidingWindowSize(global.getSlidingWindowSize())
                .minimumNumberOfCalls(global.getMinimumNumberOfCalls())
                .waitDurationInOpenState(global.getWaitDurationInOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(global.isAutomaticTransitionEnabled())
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build()
        );

        // Register service-specific configurations
        services.forEach((name, config) -> {
            log.info("Configuring circuit breaker for service: {} with config: {}", name, config);
            
            registry.addConfiguration(name, 
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                    .failureRateThreshold(config.getFailureRateThreshold())
                    .slowCallRateThreshold(config.getSlowCallRateThreshold())
                    .slowCallDurationThreshold(config.getSlowCallDurationThreshold())
                    .permittedNumberOfCallsInHalfOpenState(config.getPermittedCallsInHalfOpenState())
                    .slidingWindowType(config.getSlidingWindowType())
                    .slidingWindowSize(config.getSlidingWindowSize())
                    .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
                    .waitDurationInOpenState(config.getWaitDurationInOpenState())
                    .automaticTransitionFromOpenToHalfOpenEnabled(config.isAutomaticTransitionEnabled())
                    .build()
            );
        });

        // Register event consumers
        if (eventLoggingEnabled) {
            registry.getEventPublisher()
                .onEntryAdded(entryAddedEvent -> {
                    CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                    log.info("Circuit breaker {} added", circuitBreaker.getName());
                    registerEventLogging(circuitBreaker);
                })
                .onEntryRemoved(entryRemovedEvent -> 
                    log.info("Circuit breaker {} removed", entryRemovedEvent.getRemovedEntry().getName())
                );
        }

        return registry;
    }

    /**
     * Register circuit breaker metrics with Micrometer
     */
    @Bean
    public TaggedCircuitBreakerMetrics circuitBreakerMetrics(
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        
        if (!metricsEnabled) {
            log.info("Circuit breaker metrics disabled");
            return null;
        }

        TaggedCircuitBreakerMetrics metrics = TaggedCircuitBreakerMetrics
            .ofCircuitBreakerRegistry(circuitBreakerRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Circuit breaker metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Create circuit breaker manager for programmatic circuit breaker management
     */
    @Bean
    public CircuitBreakerManager circuitBreakerManager(CircuitBreakerRegistry registry) {
        return new CircuitBreakerManager(registry);
    }

    /**
     * Register event logging for a circuit breaker
     */
    private void registerEventLogging(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Circuit breaker {} state transition: {} -> {}", 
                    event.getCircuitBreakerName(), 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            )
            .onFailureRateExceeded(event -> 
                log.error("Circuit breaker {} failure rate exceeded: {}%", 
                    event.getCircuitBreakerName(), 
                    event.getFailureRate())
            )
            .onSlowCallRateExceeded(event -> 
                log.warn("Circuit breaker {} slow call rate exceeded: {}%", 
                    event.getCircuitBreakerName(), 
                    event.getSlowCallRate())
            )
            .onCallNotPermitted(event -> 
                log.debug("Circuit breaker {} call not permitted", 
                    event.getCircuitBreakerName())
            )
            .onError(event -> 
                log.error("Circuit breaker {} recorded error: {}", 
                    event.getCircuitBreakerName(), 
                    event.getThrowable().getMessage())
            )
            .onSuccess(event -> {
                if (log.isTraceEnabled()) {
                    log.trace("Circuit breaker {} call succeeded in {}ms", 
                        event.getCircuitBreakerName(), 
                        event.getElapsedDuration().toMillis());
                }
            });
    }

    /**
     * Global circuit breaker configuration
     */
    @Data
    public static class GlobalConfig {
        private float failureRateThreshold = 50.0f;
        private float slowCallRateThreshold = 100.0f;
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);
        private int permittedCallsInHalfOpenState = 10;
        private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType slidingWindowType = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
        private int slidingWindowSize = 100;
        private int minimumNumberOfCalls = 10;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private boolean automaticTransitionEnabled = true;
    }

    /**
     * Service-specific circuit breaker configuration
     */
    @Data
    public static class ServiceConfig {
        private float failureRateThreshold = 50.0f;
        private float slowCallRateThreshold = 100.0f;
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);
        private int permittedCallsInHalfOpenState = 10;
        private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType slidingWindowType = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
        private int slidingWindowSize = 100;
        private int minimumNumberOfCalls = 10;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private boolean automaticTransitionEnabled = true;
    }
}