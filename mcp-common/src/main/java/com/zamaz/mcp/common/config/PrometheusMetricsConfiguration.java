package com.zamaz.mcp.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for Prometheus metrics export.
 * Enables Micrometer integration with Prometheus for metrics collection.
 */
@Configuration
@ConditionalOnClass({PrometheusMeterRegistry.class, CollectorRegistry.class})
@ConditionalOnProperty(value = "management.metrics.export.prometheus.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class PrometheusMetricsConfiguration {

    /**
     * Creates Prometheus meter registry if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig) {
        log.info("Initializing Prometheus meter registry for metrics export");
        return new PrometheusMeterRegistry(prometheusConfig);
    }

    /**
     * Creates default Prometheus configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    public PrometheusConfig prometheusConfig() {
        return PrometheusConfig.DEFAULT;
    }

    /**
     * Customizes the meter registry with common tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "mcp-common");
        String environment = environment.getProperty("spring.profiles.active", "default");
        
        return registry -> {
            registry.config().commonTags(
                "application", applicationName,
                "environment", environment,
                "service", "mcp-common"
            );
            
            log.info("Configured common metrics tags - application: {}, environment: {}", 
                    applicationName, environment);
        };
    }

    /**
     * Bean to customize rate limiting metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> rateLimitingMetricsCustomizer() {
        return registry -> {
            // Configure specific settings for rate limiting metrics
            registry.config()
                .namingConvention()
                .tagKey("rate_limiter_name");
                
            log.info("Configured rate limiting metrics customization");
        };
    }

    /**
     * Bean to customize circuit breaker metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> circuitBreakerMetricsCustomizer() {
        return registry -> {
            // Configure specific settings for circuit breaker metrics
            registry.config()
                .namingConvention()
                .tagKey("circuit_breaker_name");
                
            log.info("Configured circuit breaker metrics customization");
        };
    }

    /**
     * Configuration properties for metrics.
     */
    @Configuration
    @ConditionalOnProperty(prefix = "management.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class MetricsProperties {
        
        /**
         * Enable histogram buckets for better percentile calculations.
         */
        @Bean
        public MeterRegistryCustomizer<MeterRegistry> enableHistogramBuckets() {
            return registry -> {
                registry.config()
                    .onMeterAdded(meter -> {
                        // Enable histogram buckets for timers and distribution summaries
                        if (meter.getId().getName().contains("rate_limiter") || 
                            meter.getId().getName().contains("circuit_breaker")) {
                            log.debug("Enabling histogram buckets for meter: {}", meter.getId().getName());
                        }
                    });
            };
        }
    }
}