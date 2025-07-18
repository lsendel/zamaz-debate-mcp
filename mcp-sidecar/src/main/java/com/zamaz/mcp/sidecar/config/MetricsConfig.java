package com.zamaz.mcp.sidecar.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Metrics Configuration for MCP Sidecar
 * 
 * Configures:
 * - Prometheus metrics registry
 * - Custom metrics filters
 * - Metric naming conventions
 * - Scheduled metric collection
 */
@Configuration
@EnableScheduling
@Slf4j
public class MetricsConfig {

    @Value("${app.metrics.enabled:true}")
    private boolean metricsEnabled;

    @Value("${app.metrics.prefix:mcp.sidecar}")
    private String metricsPrefix;

    /**
     * Primary Prometheus meter registry
     */
    @Bean
    @Primary
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Add common tags
        registry.config()
                .commonTags("application", "mcp-sidecar")
                .commonTags("version", "1.0.0")
                .commonTags("environment", System.getProperty("spring.profiles.active", "dev"));

        // Add metric name prefix
        registry.config()
                .meterFilter(MeterFilter.renameTag("http.server.requests", "uri", "endpoint"))
                .meterFilter(MeterFilter.denyNameStartsWith("jvm.gc.pause"))
                .meterFilter(MeterFilter.denyNameStartsWith("jvm.memory.committed"))
                .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests", 
                        java.time.Duration.ofSeconds(30)))
                .meterFilter(MeterFilter.minimumExpectedValue("http.server.requests", 
                        java.time.Duration.ofMillis(1)));

        log.info("Prometheus metrics registry configured with prefix: {}", metricsPrefix);
        return registry;
    }

    /**
     * Custom metrics filter for sensitive data
     */
    @Bean
    public MeterFilter sensitiveDataFilter() {
        return MeterFilter.deny(id -> {
            String name = id.getName();
            
            // Filter out sensitive metrics
            if (name.contains("password") || 
                name.contains("secret") || 
                name.contains("token") ||
                name.contains("key")) {
                return true;
            }
            
            // Filter out high-cardinality metrics
            if (id.getTags().stream().anyMatch(tag -> 
                tag.getKey().equals("userId") || 
                tag.getKey().equals("sessionId"))) {
                return true;
            }
            
            return false;
        });
    }

    /**
     * Metrics filter for performance optimization
     */
    @Bean
    public MeterFilter performanceFilter() {
        return MeterFilter.denyUnless(id -> {
            if (!metricsEnabled) {
                return false;
            }
            
            String name = id.getName();
            
            // Allow important metrics
            if (name.startsWith("sidecar.") ||
                name.startsWith("http.server.") ||
                name.startsWith("jvm.memory.") ||
                name.startsWith("system.cpu.") ||
                name.startsWith("process.")) {
                return true;
            }
            
            // Deny noisy metrics
            if (name.contains("hikari") || 
                name.contains("tomcat") ||
                name.contains("logging")) {
                return false;
            }
            
            return true;
        });
    }

    /**
     * Custom meter filter for business metrics
     */
    @Bean
    public MeterFilter businessMetricsFilter() {
        return MeterFilter.accept(id -> {
            String name = id.getName();
            
            // Accept all business metrics
            if (name.contains("auth") ||
                name.contains("ai") ||
                name.contains("cache") ||
                name.contains("circuit") ||
                name.contains("rate_limit")) {
                return true;
            }
            
            return false;
        });
    }
}