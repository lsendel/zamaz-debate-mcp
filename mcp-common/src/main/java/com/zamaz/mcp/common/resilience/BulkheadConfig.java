package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
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
 * Configuration for Resilience4j Bulkhead pattern implementation.
 * Provides thread isolation to prevent resource exhaustion.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.resilience.bulkhead")
@Data
@Slf4j
public class BulkheadConfig {

    /**
     * Global bulkhead configuration defaults
     */
    private GlobalBulkheadConfig global = new GlobalBulkheadConfig();

    /**
     * Service-specific bulkhead configurations
     */
    private Map<String, ServiceBulkheadConfig> services = new HashMap<>();

    /**
     * Whether to enable bulkhead metrics
     */
    private boolean metricsEnabled = true;

    /**
     * Create and configure the semaphore bulkhead registry
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        log.info("Initializing Bulkhead Registry with global config: {}", global);

        // Create registry with default configuration
        BulkheadRegistry registry = BulkheadRegistry.of(
            io.github.resilience4j.bulkhead.BulkheadConfig.custom()
                .maxConcurrentCalls(global.getMaxConcurrentCalls())
                .maxWaitDuration(global.getMaxWaitDuration())
                .build()
        );

        // Register service-specific configurations
        services.forEach((name, config) -> {
            if (!config.isUseThreadPool()) {
                log.info("Configuring semaphore bulkhead for service: {} with config: {}", name, config);
                
                registry.addConfiguration(name, 
                    io.github.resilience4j.bulkhead.BulkheadConfig.custom()
                        .maxConcurrentCalls(config.getMaxConcurrentCalls())
                        .maxWaitDuration(config.getMaxWaitDuration())
                        .build()
                );
            }
        });

        return registry;
    }

    /**
     * Create and configure the thread pool bulkhead registry
     */
    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
        log.info("Initializing Thread Pool Bulkhead Registry");

        // Create registry with default configuration
        ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.of(
            io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(global.getCoreThreadPoolSize())
                .maxThreadPoolSize(global.getMaxThreadPoolSize())
                .queueCapacity(global.getQueueCapacity())
                .keepAliveDuration(global.getKeepAliveDuration())
                .build()
        );

        // Register service-specific configurations
        services.forEach((name, config) -> {
            if (config.isUseThreadPool()) {
                log.info("Configuring thread pool bulkhead for service: {} with config: {}", name, config);
                
                registry.addConfiguration(name, 
                    io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig.custom()
                        .coreThreadPoolSize(config.getCoreThreadPoolSize())
                        .maxThreadPoolSize(config.getMaxThreadPoolSize())
                        .queueCapacity(config.getQueueCapacity())
                        .keepAliveDuration(config.getKeepAliveDuration())
                        .build()
                );
            }
        });

        return registry;
    }

    /**
     * Register semaphore bulkhead metrics with Micrometer
     */
    @Bean
    public TaggedBulkheadMetrics bulkheadMetrics(
            BulkheadRegistry bulkheadRegistry,
            MeterRegistry meterRegistry) {
        
        if (!metricsEnabled) {
            log.info("Bulkhead metrics disabled");
            return null;
        }

        TaggedBulkheadMetrics metrics = TaggedBulkheadMetrics
            .ofBulkheadRegistry(bulkheadRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Bulkhead metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Register thread pool bulkhead metrics with Micrometer
     */
    @Bean
    public TaggedThreadPoolBulkheadMetrics threadPoolBulkheadMetrics(
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
            MeterRegistry meterRegistry) {
        
        if (!metricsEnabled) {
            log.info("Thread pool bulkhead metrics disabled");
            return null;
        }

        TaggedThreadPoolBulkheadMetrics metrics = TaggedThreadPoolBulkheadMetrics
            .ofThreadPoolBulkheadRegistry(threadPoolBulkheadRegistry);
        metrics.bindTo(meterRegistry);
        
        log.info("Thread pool bulkhead metrics registered with Micrometer");
        return metrics;
    }

    /**
     * Global bulkhead configuration
     */
    @Data
    public static class GlobalBulkheadConfig {
        // Semaphore bulkhead configuration
        private int maxConcurrentCalls = 25;
        private Duration maxWaitDuration = Duration.ofSeconds(1);
        
        // Thread pool bulkhead configuration
        private int coreThreadPoolSize = 10;
        private int maxThreadPoolSize = 20;
        private int queueCapacity = 100;
        private Duration keepAliveDuration = Duration.ofMillis(500);
    }

    /**
     * Service-specific bulkhead configuration
     */
    @Data
    public static class ServiceBulkheadConfig {
        // Whether to use thread pool bulkhead instead of semaphore
        private boolean useThreadPool = false;
        
        // Semaphore bulkhead configuration
        private int maxConcurrentCalls = 25;
        private Duration maxWaitDuration = Duration.ofSeconds(1);
        
        // Thread pool bulkhead configuration
        private int coreThreadPoolSize = 10;
        private int maxThreadPoolSize = 20;
        private int queueCapacity = 100;
        private Duration keepAliveDuration = Duration.ofMillis(500);
    }
}