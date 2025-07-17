package com.zamaz.mcp.common.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration and integration for APM (Application Performance Monitoring) agents
 */
@Component
@Slf4j
public class APMAgentConfig {
    
    @Value("${apm.enabled:false}")
    private boolean apmEnabled;
    
    @Value("${apm.provider:elastic}")
    private String apmProvider;
    
    @Value("${apm.service.name:mcp-system}")
    private String serviceName;
    
    @Value("${apm.service.version:1.0.0}")
    private String serviceVersion;
    
    @Value("${apm.environment:development}")
    private String environment;
    
    @Value("${apm.server.url:http://localhost:8200}")
    private String serverUrl;
    
    @Value("${apm.secret.token:}")
    private String secretToken;
    
    @Value("${apm.api.key:}")
    private String apiKey;
    
    @Value("${apm.sampling.rate:1.0}")
    private double samplingRate;
    
    @Value("${apm.capture.body:all}")
    private String captureBody;
    
    @Value("${apm.capture.headers:true}")
    private boolean captureHeaders;
    
    @Value("${apm.stack.trace.limit:50}")
    private int stackTraceLimit;
    
    @Value("${apm.span.frames.min.duration:5ms}")
    private String spanFramesMinDuration;
    
    @Value("${apm.transaction.max.spans:500}")
    private int transactionMaxSpans;
    
    @Value("${apm.log.correlation.enabled:true}")
    private boolean logCorrelationEnabled;
    
    @Value("${apm.metrics.enabled:true}")
    private boolean metricsEnabled;
    
    @Value("${apm.profiling.enabled:false}")
    private boolean profilingEnabled;
    
    @Value("${apm.disable.instrumentations:}")
    private String disableInstrumentations;
    
    // APM agent state
    private boolean initialized = false;
    private String agentVersion;
    private Map<String, String> agentMetadata;
    
    /**
     * Initialize APM agent configuration
     */
    public void initialize() {
        if (!apmEnabled) {
            log.info("APM monitoring is disabled");
            return;
        }
        
        log.info("Initializing APM agent for provider: {}", apmProvider);
        
        try {
            switch (apmProvider.toLowerCase()) {
                case "elastic", "elasticsearch" -> initializeElasticAPM();
                case "jaeger" -> initializeJaegerAPM();
                case "zipkin" -> initializeZipkinAPM();
                case "newrelic" -> initializeNewRelicAPM();
                case "datadog" -> initializeDatadogAPM();
                case "dynatrace" -> initializeDynatraceAPM();
                case "appdynamics" -> initializeAppDynamicsAPM();
                default -> {
                    log.warn("Unknown APM provider: {}, using generic configuration", apmProvider);
                    initializeGenericAPM();
                }
            }
            
            initialized = true;
            log.info("APM agent initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize APM agent", e);
            throw new RuntimeException("APM agent initialization failed", e);
        }
    }
    
    /**
     * Initialize Elastic APM agent
     */
    private void initializeElasticAPM() {
        log.info("Initializing Elastic APM agent");
        
        // Set system properties for Elastic APM
        System.setProperty("elastic.apm.service_name", serviceName);
        System.setProperty("elastic.apm.service_version", serviceVersion);
        System.setProperty("elastic.apm.environment", environment);
        System.setProperty("elastic.apm.server_url", serverUrl);
        System.setProperty("elastic.apm.sample_rate", String.valueOf(samplingRate));
        System.setProperty("elastic.apm.capture_body", captureBody);
        System.setProperty("elastic.apm.capture_headers", String.valueOf(captureHeaders));
        System.setProperty("elastic.apm.stack_trace_limit", String.valueOf(stackTraceLimit));
        System.setProperty("elastic.apm.span_frames_min_duration", spanFramesMinDuration);
        System.setProperty("elastic.apm.transaction_max_spans", String.valueOf(transactionMaxSpans));
        System.setProperty("elastic.apm.enable_log_correlation", String.valueOf(logCorrelationEnabled));
        System.setProperty("elastic.apm.metrics_enabled", String.valueOf(metricsEnabled));
        System.setProperty("elastic.apm.profiling_enabled", String.valueOf(profilingEnabled));
        
        if (!secretToken.isEmpty()) {
            System.setProperty("elastic.apm.secret_token", secretToken);
        }
        
        if (!apiKey.isEmpty()) {
            System.setProperty("elastic.apm.api_key", apiKey);
        }
        
        if (!disableInstrumentations.isEmpty()) {
            System.setProperty("elastic.apm.disable_instrumentations", disableInstrumentations);
        }
        
        // Initialize agent metadata
        initializeAgentMetadata();
        
        log.info("Elastic APM agent configured");
    }
    
    /**
     * Initialize Jaeger APM agent
     */
    private void initializeJaegerAPM() {
        log.info("Initializing Jaeger APM agent");
        
        // Set system properties for Jaeger
        System.setProperty("jaeger.service-name", serviceName);
        System.setProperty("jaeger.agent-host", extractHostFromUrl(serverUrl));
        System.setProperty("jaeger.agent-port", extractPortFromUrl(serverUrl));
        System.setProperty("jaeger.sampler-type", "probabilistic");
        System.setProperty("jaeger.sampler-param", String.valueOf(samplingRate));
        System.setProperty("jaeger.reporter-log-spans", "false");
        System.setProperty("jaeger.reporter-flush-interval", "1000");
        
        // Set tags
        System.setProperty("jaeger.tags", String.format("version=%s,environment=%s", serviceVersion, environment));
        
        initializeAgentMetadata();
        
        log.info("Jaeger APM agent configured");
    }
    
    /**
     * Initialize Zipkin APM agent
     */
    private void initializeZipkinAPM() {
        log.info("Initializing Zipkin APM agent");
        
        // Set system properties for Zipkin
        System.setProperty("zipkin.service-name", serviceName);
        System.setProperty("zipkin.base-url", serverUrl);
        System.setProperty("zipkin.sampler.percentage", String.valueOf(samplingRate));
        System.setProperty("zipkin.compression.enabled", "true");
        
        initializeAgentMetadata();
        
        log.info("Zipkin APM agent configured");
    }
    
    /**
     * Initialize New Relic APM agent
     */
    private void initializeNewRelicAPM() {
        log.info("Initializing New Relic APM agent");
        
        // Set system properties for New Relic
        System.setProperty("newrelic.config.app_name", serviceName);
        System.setProperty("newrelic.config.license_key", apiKey);
        System.setProperty("newrelic.environment", environment);
        System.setProperty("newrelic.config.distributed_tracing.enabled", "true");
        System.setProperty("newrelic.config.span_events.enabled", "true");
        
        initializeAgentMetadata();
        
        log.info("New Relic APM agent configured");
    }
    
    /**
     * Initialize Datadog APM agent
     */
    private void initializeDatadogAPM() {
        log.info("Initializing Datadog APM agent");
        
        // Set system properties for Datadog
        System.setProperty("dd.service", serviceName);
        System.setProperty("dd.version", serviceVersion);
        System.setProperty("dd.env", environment);
        System.setProperty("dd.agent.host", extractHostFromUrl(serverUrl));
        System.setProperty("dd.agent.port", extractPortFromUrl(serverUrl));
        System.setProperty("dd.trace.sample.rate", String.valueOf(samplingRate));
        System.setProperty("dd.logs.injection", String.valueOf(logCorrelationEnabled));
        System.setProperty("dd.profiling.enabled", String.valueOf(profilingEnabled));
        
        if (!apiKey.isEmpty()) {
            System.setProperty("dd.api.key", apiKey);
        }
        
        initializeAgentMetadata();
        
        log.info("Datadog APM agent configured");
    }
    
    /**
     * Initialize Dynatrace APM agent
     */
    private void initializeDynatraceAPM() {
        log.info("Initializing Dynatrace APM agent");
        
        // Set system properties for Dynatrace
        System.setProperty("dt.service.name", serviceName);
        System.setProperty("dt.service.version", serviceVersion);
        System.setProperty("dt.environment", environment);
        System.setProperty("dt.server.url", serverUrl);
        System.setProperty("dt.sampling.rate", String.valueOf(samplingRate));
        
        if (!apiKey.isEmpty()) {
            System.setProperty("dt.api.token", apiKey);
        }
        
        initializeAgentMetadata();
        
        log.info("Dynatrace APM agent configured");
    }
    
    /**
     * Initialize AppDynamics APM agent
     */
    private void initializeAppDynamicsAPM() {
        log.info("Initializing AppDynamics APM agent");
        
        // Set system properties for AppDynamics
        System.setProperty("appdynamics.application.name", serviceName);
        System.setProperty("appdynamics.tier.name", serviceName + "-tier");
        System.setProperty("appdynamics.node.name", serviceName + "-node");
        System.setProperty("appdynamics.controller.hostName", extractHostFromUrl(serverUrl));
        System.setProperty("appdynamics.controller.port", extractPortFromUrl(serverUrl));
        System.setProperty("appdynamics.agent.accountName", environment);
        
        if (!apiKey.isEmpty()) {
            System.setProperty("appdynamics.agent.accountAccessKey", apiKey);
        }
        
        initializeAgentMetadata();
        
        log.info("AppDynamics APM agent configured");
    }
    
    /**
     * Initialize generic APM configuration
     */
    private void initializeGenericAPM() {
        log.info("Initializing generic APM configuration");
        
        // Set common properties
        System.setProperty("apm.service.name", serviceName);
        System.setProperty("apm.service.version", serviceVersion);
        System.setProperty("apm.environment", environment);
        System.setProperty("apm.server.url", serverUrl);
        System.setProperty("apm.sampling.rate", String.valueOf(samplingRate));
        
        initializeAgentMetadata();
        
        log.info("Generic APM configuration applied");
    }
    
    /**
     * Initialize agent metadata
     */
    private void initializeAgentMetadata() {
        agentMetadata = new HashMap<>();
        agentMetadata.put("provider", apmProvider);
        agentMetadata.put("service_name", serviceName);
        agentMetadata.put("service_version", serviceVersion);
        agentMetadata.put("environment", environment);
        agentMetadata.put("server_url", serverUrl);
        agentMetadata.put("sampling_rate", String.valueOf(samplingRate));
        agentMetadata.put("metrics_enabled", String.valueOf(metricsEnabled));
        agentMetadata.put("profiling_enabled", String.valueOf(profilingEnabled));
        agentMetadata.put("log_correlation_enabled", String.valueOf(logCorrelationEnabled));
        
        // Try to detect agent version
        try {
            agentVersion = detectAgentVersion();
            agentMetadata.put("agent_version", agentVersion);
        } catch (Exception e) {
            log.debug("Could not detect agent version", e);
            agentVersion = "unknown";
        }
    }
    
    /**
     * Get APM agent status
     */
    public APMStatus getStatus() {
        return APMStatus.builder()
            .enabled(apmEnabled)
            .initialized(initialized)
            .provider(apmProvider)
            .serviceName(serviceName)
            .serviceVersion(serviceVersion)
            .environment(environment)
            .serverUrl(serverUrl)
            .samplingRate(samplingRate)
            .agentVersion(agentVersion)
            .metricsEnabled(metricsEnabled)
            .profilingEnabled(profilingEnabled)
            .logCorrelationEnabled(logCorrelationEnabled)
            .metadata(agentMetadata)
            .build();
    }
    
    /**
     * Check if APM is enabled and initialized
     */
    public boolean isActive() {
        return apmEnabled && initialized;
    }
    
    /**
     * Get current trace ID (implementation depends on APM provider)
     */
    public String getCurrentTraceId() {
        if (!isActive()) {
            return null;
        }
        
        // This would be implemented based on the specific APM provider
        // For now, return a placeholder
        return "trace-" + System.currentTimeMillis();
    }
    
    /**
     * Get current span ID (implementation depends on APM provider)
     */
    public String getCurrentSpanId() {
        if (!isActive()) {
            return null;
        }
        
        // This would be implemented based on the specific APM provider
        // For now, return a placeholder
        return "span-" + System.currentTimeMillis();
    }
    
    /**
     * Create custom span (implementation depends on APM provider)
     */
    public void createCustomSpan(String name, String type, Runnable operation) {
        if (!isActive()) {
            operation.run();
            return;
        }
        
        // This would be implemented based on the specific APM provider
        // For now, just execute the operation
        long start = System.currentTimeMillis();
        try {
            operation.run();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.debug("Custom span '{}' of type '{}' took {}ms", name, type, duration);
        }
    }
    
    private String extractHostFromUrl(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    private String extractPortFromUrl(String url) {
        try {
            int port = new java.net.URL(url).getPort();
            return port != -1 ? String.valueOf(port) : "8080";
        } catch (Exception e) {
            return "8080";
        }
    }
    
    private String detectAgentVersion() {
        // This would detect the actual agent version based on the provider
        // For now, return a placeholder
        return "1.0.0";
    }
    
    // Status data class
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class APMStatus {
        private boolean enabled;
        private boolean initialized;
        private String provider;
        private String serviceName;
        private String serviceVersion;
        private String environment;
        private String serverUrl;
        private double samplingRate;
        private String agentVersion;
        private boolean metricsEnabled;
        private boolean profilingEnabled;
        private boolean logCorrelationEnabled;
        private Map<String, String> metadata;
    }
}