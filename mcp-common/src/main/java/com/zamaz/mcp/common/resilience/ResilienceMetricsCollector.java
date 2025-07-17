package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Collects and exposes Resilience4j metrics to Micrometer
 */
@Configuration
@Slf4j
@ConditionalOnProperty(value = "mcp.resilience.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceMetricsCollector {

    /**
     * Register circuit breaker metrics
     */
    @Bean
    @ConditionalOnBean(CircuitBreakerRegistry.class)
    public TaggedCircuitBreakerMetrics circuitBreakerMetrics(
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        
        TaggedCircuitBreakerMetrics metrics = TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(circuitBreakerRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Circuit breaker metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Register retry metrics
     */
    @Bean
    @ConditionalOnBean(RetryRegistry.class)
    public TaggedRetryMetrics retryMetrics(
            RetryRegistry retryRegistry,
            MeterRegistry meterRegistry) {
        
        TaggedRetryMetrics metrics = TaggedRetryMetrics
                .ofRetryRegistry(retryRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Retry metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Register rate limiter metrics
     */
    @Bean
    @ConditionalOnBean(RateLimiterRegistry.class)
    public TaggedRateLimiterMetrics rateLimiterMetrics(
            RateLimiterRegistry rateLimiterRegistry,
            MeterRegistry meterRegistry) {
        
        TaggedRateLimiterMetrics metrics = TaggedRateLimiterMetrics
                .ofRateLimiterRegistry(rateLimiterRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Rate limiter metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Register bulkhead metrics
     */
    @Bean
    @ConditionalOnBean(BulkheadRegistry.class)
    public TaggedBulkheadMetrics bulkheadMetrics(
            BulkheadRegistry bulkheadRegistry,
            MeterRegistry meterRegistry) {
        
        TaggedBulkheadMetrics metrics = TaggedBulkheadMetrics
                .ofBulkheadRegistry(bulkheadRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Bulkhead metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Register thread pool bulkhead metrics
     */
    @Bean
    @ConditionalOnBean(ThreadPoolBulkheadRegistry.class)
    public TaggedThreadPoolBulkheadMetrics threadPoolBulkheadMetrics(
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
            MeterRegistry meterRegistry) {
        
        TaggedThreadPoolBulkheadMetrics metrics = TaggedThreadPoolBulkheadMetrics
                .ofThreadPoolBulkheadRegistry(threadPoolBulkheadRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Thread pool bulkhead metrics registered with Micrometer");
        return metrics;
    }
}