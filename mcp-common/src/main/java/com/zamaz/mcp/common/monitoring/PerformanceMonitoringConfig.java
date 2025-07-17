package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configuration for performance monitoring and APM integration
 */
@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringConfig {
    
    @Value("${monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${monitoring.prometheus.enabled:true}")
    private boolean prometheusEnabled;
    
    @Value("${monitoring.prometheus.endpoint:/metrics}")
    private String prometheusEndpoint;
    
    @Value("${monitoring.application.name:mcp-system}")
    private String applicationName;
    
    @Value("${monitoring.application.version:1.0.0}")
    private String applicationVersion;
    
    @Value("${monitoring.application.environment:development}")
    private String environment;
    
    @Value("${monitoring.metrics.export.interval:30s}")
    private String exportInterval;
    
    @Value("${monitoring.metrics.distribution.percentiles:0.5,0.75,0.95,0.99}")
    private double[] percentiles;
    
    @Value("${monitoring.metrics.distribution.sla.enabled:true}")
    private boolean slaEnabled;
    
    @Value("${monitoring.jvm.enabled:true}")
    private boolean jvmMetricsEnabled;
    
    @Value("${monitoring.database.enabled:true}")
    private boolean databaseMetricsEnabled;
    
    @Value("${monitoring.custom.enabled:true}")
    private boolean customMetricsEnabled;
    
    @Value("${monitoring.alerting.enabled:true}")
    private boolean alertingEnabled;
    
    @Value("${monitoring.profiling.enabled:false}")
    private boolean profilingEnabled;
    
    /**
     * Primary meter registry configuration
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        if (!monitoringEnabled) {
            log.info("Monitoring disabled, using simple meter registry");
            return new SimpleMeterRegistry();
        }
        
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        
        // Add Prometheus registry if enabled
        if (prometheusEnabled) {
            PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT,
                CollectorRegistry.defaultRegistry,
                Clock.SYSTEM
            );
            composite.add(prometheus);
            log.info("Prometheus metrics registry enabled");
        }
        
        // Add simple registry as fallback
        composite.add(new SimpleMeterRegistry());
        
        log.info("Composite meter registry configured with {} registries", composite.getRegistries().size());
        return composite;
    }
    
    /**
     * Prometheus meter registry for metrics export
     */
    @Bean
    @ConditionalOnMissingBean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        if (!prometheusEnabled) {
            return null;
        }
        
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            CollectorRegistry.defaultRegistry,
            Clock.SYSTEM
        );
        
        log.info("Prometheus meter registry created");
        return registry;
    }
    
    /**
     * Meter registry customizer for common tags and filters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> {
            // Add common tags
            registry.config()
                .commonTags("application", applicationName)
                .commonTags("version", applicationVersion)
                .commonTags("environment", environment);
            
            // Configure distribution statistics
            registry.config()
                .meterFilter(MeterFilter.denyNameStartsWith("jvm.threads."))
                .meterFilter(MeterFilter.maximumAllowableMetrics(10000))
                .meterFilter(MeterFilter.deny(id -> id.getName().startsWith("http.server.requests") 
                    && id.getTag("uri") != null 
                    && id.getTag("uri").startsWith("/actuator")));
            
            // Configure SLA boundaries for timers
            if (slaEnabled) {
                registry.config()
                    .meterFilter(MeterFilter.replaceTagValues("uri", uri -> {
                        if (uri.startsWith("/api/")) {
                            return uri.replaceAll("/\\d+", "/{id}");
                        }
                        return uri;
                    }))
                    .meterFilter(MeterFilter.replaceTagValues("status", status -> {
                        if (status.startsWith("2")) return "2xx";
                        if (status.startsWith("3")) return "3xx";
                        if (status.startsWith("4")) return "4xx";
                        if (status.startsWith("5")) return "5xx";
                        return status;
                    }));
            }
            
            // Configure percentiles for distribution metrics
            registry.config()
                .meterFilter(MeterFilter.replaceTagValues("method", method -> method.toUpperCase()))
                .meterFilter(MeterFilter.replaceTagValues("exception", exception -> {
                    if (exception.contains(".")) {
                        return exception.substring(exception.lastIndexOf('.') + 1);
                    }
                    return exception;
                }))
                .meterFilter(MeterFilter.replaceTagValues("outcome", outcome -> outcome.toUpperCase()));
            
            log.info("Meter registry customized with common tags and filters");
        };
    }
    
    /**
     * Performance metrics collector
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceMetricsCollector performanceMetricsCollector(MeterRegistry meterRegistry) {
        return new PerformanceMetricsCollector(meterRegistry);
    }
    
    /**
     * APM agent configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public APMAgentConfig apmAgentConfig() {
        return new APMAgentConfig();
    }
    
    /**
     * Performance monitoring service
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceMonitoringService performanceMonitoringService(
            MeterRegistry meterRegistry,
            PerformanceMetricsCollector metricsCollector,
            APMAgentConfig apmConfig) {
        return new PerformanceMonitoringService(meterRegistry, metricsCollector, apmConfig);
    }
    
    /**
     * System health monitor
     */
    @Bean
    @ConditionalOnMissingBean
    public SystemHealthMonitor systemHealthMonitor(MeterRegistry meterRegistry) {
        return new SystemHealthMonitor(meterRegistry);
    }
    
    /**
     * Application performance monitor
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationPerformanceMonitor applicationPerformanceMonitor(
            MeterRegistry meterRegistry,
            PerformanceMetricsCollector metricsCollector) {
        return new ApplicationPerformanceMonitor(meterRegistry, metricsCollector);
    }
    
    /**
     * Database performance monitor
     */
    @Bean
    @ConditionalOnMissingBean
    public DatabasePerformanceMonitor databasePerformanceMonitor(MeterRegistry meterRegistry) {
        if (!databaseMetricsEnabled) {
            return null;
        }
        return new DatabasePerformanceMonitor(meterRegistry);
    }
    
    /**
     * Custom metrics publisher
     */
    @Bean
    @ConditionalOnMissingBean
    public CustomMetricsPublisher customMetricsPublisher(MeterRegistry meterRegistry) {
        if (!customMetricsEnabled) {
            return null;
        }
        return new CustomMetricsPublisher(meterRegistry);
    }
    
    /**
     * Performance alerting service
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceAlertingService performanceAlertingService(
            MeterRegistry meterRegistry,
            SystemHealthMonitor healthMonitor) {
        if (!alertingEnabled) {
            return null;
        }
        return new PerformanceAlertingService(meterRegistry, healthMonitor);
    }
    
    /**
     * Profiling service
     */
    @Bean
    @ConditionalOnMissingBean
    public ProfilingService profilingService(MeterRegistry meterRegistry) {
        if (!profilingEnabled) {
            return null;
        }
        return new ProfilingService(meterRegistry);
    }
    
    /**
     * Configure executor for monitoring tasks
     */
    @Bean(name = "monitoringExecutor")
    @ConditionalOnMissingBean(name = "monitoringExecutor")
    public Executor monitoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Monitoring-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * Performance monitoring health indicator
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceMonitoringHealthIndicator performanceMonitoringHealthIndicator(
            PerformanceMonitoringService monitoringService) {
        return new PerformanceMonitoringHealthIndicator(monitoringService);
    }
    
    /**
     * Metrics endpoint customizer
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricsEndpointCustomizer metricsEndpointCustomizer() {
        return new MetricsEndpointCustomizer();
    }
}