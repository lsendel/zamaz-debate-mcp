package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Resilience4j Retry pattern implementation.
 * Provides automatic retry with exponential backoff for transient failures.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.resilience.retry")
@Data
@Slf4j
public class RetryConfig {

    /**
     * Global retry configuration defaults
     */
    private GlobalRetryConfig global = new GlobalRetryConfig();

    /**
     * Service-specific retry configurations
     */
    private Map<String, ServiceRetryConfig> services = new HashMap<>();

    /**
     * Whether to enable retry metrics
     */
    private boolean metricsEnabled = true;

    /**
     * Create and configure the retry registry
     */
    @Bean
    public RetryRegistry retryRegistry() {
        log.info("Initializing Retry Registry with global config: {}", global);

        // Create registry with default configuration
        RetryRegistry registry = RetryRegistry.of(
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(global.getMaxAttempts())
                .waitDuration(global.getWaitDuration())
                .retryOnException(throwable -> {
                    // Default: retry on all exceptions except specific ones
                    return !(throwable instanceof IllegalArgumentException ||
                            throwable instanceof IllegalStateException ||
                            throwable instanceof NullPointerException);
                })
                .build()
        );

        // Register service-specific configurations
        services.forEach((name, config) -> {
            log.info("Configuring retry for service: {} with config: {}", name, config);
            
            io.github.resilience4j.retry.RetryConfig.Builder<Object> builder = 
                io.github.resilience4j.retry.RetryConfig.custom()
                    .maxAttempts(config.getMaxAttempts())
                    .waitDuration(config.getWaitDuration());

            // Configure exponential backoff if enabled
            if (config.isExponentialBackoff()) {
                builder.intervalFunction(io.github.resilience4j.retry.IntervalFunction
                    .ofExponentialBackoff(
                        config.getWaitDuration().toMillis(),
                        config.getExponentialMultiplier(),
                        config.getMaxWaitDuration().toMillis()
                    ));
            }

            // Configure jitter if enabled
            if (config.isUseJitter()) {
                builder.intervalFunction(io.github.resilience4j.retry.IntervalFunction
                    .ofExponentialRandomBackoff(
                        config.getWaitDuration().toMillis(),
                        config.getExponentialMultiplier(),
                        config.getJitterFactor(),
                        config.getMaxWaitDuration().toMillis()
                    ));
            }

            registry.addConfiguration(name, builder.build());
        });

        return registry;
    }

    /**
     * Register retry metrics with Micrometer
     */
    @Bean
    public TaggedRetryMetrics retryMetrics(RetryRegistry retryRegistry, MeterRegistry meterRegistry) {
        if (!metricsEnabled) {
            log.info("Retry metrics disabled");
            return null;
        }

        TaggedRetryMetrics metrics = TaggedRetryMetrics.ofRetryRegistry(retryRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Retry metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Global retry configuration
     */
    @Data
    public static class GlobalRetryConfig {
        private int maxAttempts = 3;
        private Duration waitDuration = Duration.ofSeconds(1);
        private Duration maxWaitDuration = Duration.ofMinutes(1);
        private boolean exponentialBackoff = true;
        private double exponentialMultiplier = 2.0;
        private boolean useJitter = true;
        private double jitterFactor = 0.5;
    }

    /**
     * Service-specific retry configuration
     */
    @Data
    public static class ServiceRetryConfig {
        private int maxAttempts = 3;
        private Duration waitDuration = Duration.ofSeconds(1);
        private Duration maxWaitDuration = Duration.ofMinutes(1);
        private boolean exponentialBackoff = true;
        private double exponentialMultiplier = 2.0;
        private boolean useJitter = true;
        private double jitterFactor = 0.5;
    }
}