package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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
 * Configuration for Resilience4j Rate Limiter pattern implementation.
 * Provides rate limiting to prevent overwhelming services.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.resilience.rate-limiter")
@Data
@Slf4j
public class RateLimiterConfig {

    /**
     * Global rate limiter configuration defaults
     */
    private GlobalRateLimiterConfig global = new GlobalRateLimiterConfig();

    /**
     * Service-specific rate limiter configurations
     */
    private Map<String, ServiceRateLimiterConfig> services = new HashMap<>();

    /**
     * Whether to enable rate limiter metrics
     */
    private boolean metricsEnabled = true;

    /**
     * Create and configure the rate limiter registry
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        log.info("Initializing Rate Limiter Registry with global config: {}", global);

        // Create registry with default configuration
        RateLimiterRegistry registry = RateLimiterRegistry.of(
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(global.getLimitForPeriod())
                .limitRefreshPeriod(global.getLimitRefreshPeriod())
                .timeoutDuration(global.getTimeoutDuration())
                .build()
        );

        // Register service-specific configurations
        services.forEach((name, config) -> {
            log.info("Configuring rate limiter for service: {} with config: {}", name, config);
            
            registry.addConfiguration(name, 
                io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                    .limitForPeriod(config.getLimitForPeriod())
                    .limitRefreshPeriod(config.getLimitRefreshPeriod())
                    .timeoutDuration(config.getTimeoutDuration())
                    .build()
            );
        });

        return registry;
    }

    /**
     * Register rate limiter metrics with Micrometer
     */
    @Bean
    public TaggedRateLimiterMetrics rateLimiterMetrics(
            RateLimiterRegistry rateLimiterRegistry,
            MeterRegistry meterRegistry) {
        
        if (!metricsEnabled) {
            log.info("Rate limiter metrics disabled");
            return null;
        }

        TaggedRateLimiterMetrics metrics = TaggedRateLimiterMetrics
            .ofRateLimiterRegistry(rateLimiterRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Rate limiter metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Global rate limiter configuration
     */
    @Data
    public static class GlobalRateLimiterConfig {
        private int limitForPeriod = 100;
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);
        private Duration timeoutDuration = Duration.ofSeconds(5);
    }

    /**
     * Service-specific rate limiter configuration
     */
    @Data
    public static class ServiceRateLimiterConfig {
        private int limitForPeriod = 100;
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);
        private Duration timeoutDuration = Duration.ofSeconds(5);
    }
}