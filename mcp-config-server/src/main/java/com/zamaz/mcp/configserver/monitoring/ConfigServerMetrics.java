package com.zamaz.mcp.configserver.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collector for Spring Cloud Config Server.
 * Tracks configuration access patterns, performance, and errors.
 */
@Component
public class ConfigServerMetrics {

    private final MeterRegistry meterRegistry;
    
    // Counters
    private Counter configRequestsTotal;
    private Counter configRequestsSuccess;
    private Counter configRequestsFailed;
    private Counter encryptionOperations;
    private Counter decryptionOperations;
    private Counter refreshEvents;
    
    // Timers
    private Timer configRequestDuration;
    private Timer encryptionDuration;
    private Timer gitOperationDuration;
    
    // Gauges
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong lastConfigRequestTime = new AtomicLong(0);
    private final AtomicInteger cachedConfigurations = new AtomicInteger(0);

    @Autowired
    public ConfigServerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        // Initialize counters
        configRequestsTotal = Counter.builder("config_server_requests_total")
            .description("Total number of configuration requests")
            .register(meterRegistry);
            
        configRequestsSuccess = Counter.builder("config_server_requests_success")
            .description("Number of successful configuration requests")
            .register(meterRegistry);
            
        configRequestsFailed = Counter.builder("config_server_requests_failed")
            .description("Number of failed configuration requests")
            .register(meterRegistry);
            
        encryptionOperations = Counter.builder("config_server_encryption_operations")
            .description("Number of encryption operations")
            .register(meterRegistry);
            
        decryptionOperations = Counter.builder("config_server_decryption_operations")
            .description("Number of decryption operations")
            .register(meterRegistry);
            
        refreshEvents = Counter.builder("config_server_refresh_events")
            .description("Number of configuration refresh events")
            .register(meterRegistry);
        
        // Initialize timers
        configRequestDuration = Timer.builder("config_server_request_duration")
            .description("Duration of configuration requests")
            .register(meterRegistry);
            
        encryptionDuration = Timer.builder("config_server_encryption_duration")
            .description("Duration of encryption operations")
            .register(meterRegistry);
            
        gitOperationDuration = Timer.builder("config_server_git_operation_duration")
            .description("Duration of Git operations")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("config_server_active_connections", activeConnections, AtomicInteger::get)
            .description("Number of active connections to Config Server")
            .register(meterRegistry);
            
        Gauge.builder("config_server_last_request_timestamp", lastConfigRequestTime, AtomicLong::get)
            .description("Timestamp of last configuration request")
            .register(meterRegistry);
            
        Gauge.builder("config_server_cached_configurations", cachedConfigurations, AtomicInteger::get)
            .description("Number of cached configurations")
            .register(meterRegistry);
    }

    /**
     * Records a configuration request.
     */
    public Timer.Sample startConfigRequest() {
        activeConnections.incrementAndGet();
        lastConfigRequestTime.set(System.currentTimeMillis());
        configRequestsTotal.increment();
        return Timer.start(meterRegistry);
    }

    /**
     * Records successful configuration request completion.
     */
    public void recordConfigRequestSuccess(Timer.Sample sample) {
        sample.stop(configRequestDuration);
        configRequestsSuccess.increment();
        activeConnections.decrementAndGet();
    }

    /**
     * Records failed configuration request.
     */
    public void recordConfigRequestFailure(Timer.Sample sample, String reason) {
        sample.stop(Timer.builder("config_server_request_duration")
            .tag("status", "failed")
            .tag("reason", reason)
            .register(meterRegistry));
        configRequestsFailed.increment();
        activeConnections.decrementAndGet();
    }

    /**
     * Records an encryption operation.
     */
    public Timer.Sample startEncryption() {
        encryptionOperations.increment();
        return Timer.start(meterRegistry);
    }

    /**
     * Records encryption completion.
     */
    public void recordEncryptionComplete(Timer.Sample sample) {
        sample.stop(encryptionDuration);
    }

    /**
     * Records a decryption operation.
     */
    public void recordDecryption() {
        decryptionOperations.increment();
    }

    /**
     * Records a refresh event.
     */
    public void recordRefreshEvent(String source) {
        Counter.builder("config_server_refresh_events")
            .tag("source", source)
            .register(meterRegistry)
            .increment();
        refreshEvents.increment();
    }

    /**
     * Records a Git operation.
     */
    public Timer.Sample startGitOperation(String operation) {
        return Timer.start(meterRegistry);
    }

    /**
     * Records Git operation completion.
     */
    public void recordGitOperationComplete(Timer.Sample sample, String operation, boolean success) {
        sample.stop(Timer.builder("config_server_git_operation_duration")
            .tag("operation", operation)
            .tag("status", success ? "success" : "failed")
            .register(meterRegistry));
    }

    /**
     * Updates cached configuration count.
     */
    public void updateCachedConfigurations(int count) {
        cachedConfigurations.set(count);
    }

    /**
     * Records configuration access by application.
     */
    public void recordConfigAccess(String application, String profile, String label) {
        Counter.builder("config_server_access_by_application")
            .tag("application", application)
            .tag("profile", profile)
            .tag("label", label)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records configuration property access.
     */
    public void recordPropertyAccess(String property, boolean encrypted) {
        Counter.builder("config_server_property_access")
            .tag("property", sanitizePropertyName(property))
            .tag("encrypted", String.valueOf(encrypted))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Sanitizes property names for metrics.
     */
    private String sanitizePropertyName(String property) {
        // Remove sensitive parts from property names
        if (property.toLowerCase().contains("password")) {
            return "*.password";
        }
        if (property.toLowerCase().contains("secret")) {
            return "*.secret";
        }
        if (property.toLowerCase().contains("key")) {
            return "*.key";
        }
        return property;
    }

    /**
     * Gets current metrics summary.
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
            configRequestsTotal.count(),
            configRequestsSuccess.count(),
            configRequestsFailed.count(),
            encryptionOperations.count(),
            decryptionOperations.count(),
            refreshEvents.count(),
            activeConnections.get(),
            cachedConfigurations.get(),
            lastConfigRequestTime.get()
        );
    }

    /**
     * Metrics summary class.
     */
    public static class MetricsSummary {
        private final double totalRequests;
        private final double successfulRequests;
        private final double failedRequests;
        private final double encryptionOps;
        private final double decryptionOps;
        private final double refreshEvents;
        private final int activeConnections;
        private final int cachedConfigs;
        private final long lastRequestTime;

        public MetricsSummary(double totalRequests, double successfulRequests, 
                             double failedRequests, double encryptionOps,
                             double decryptionOps, double refreshEvents,
                             int activeConnections, int cachedConfigs,
                             long lastRequestTime) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.encryptionOps = encryptionOps;
            this.decryptionOps = decryptionOps;
            this.refreshEvents = refreshEvents;
            this.activeConnections = activeConnections;
            this.cachedConfigs = cachedConfigs;
            this.lastRequestTime = lastRequestTime;
        }

        // Getters
        public double getTotalRequests() {
            return totalRequests;
        }

        public double getSuccessfulRequests() {
            return successfulRequests;
        }

        public double getFailedRequests() {
            return failedRequests;
        }

        public double getSuccessRate() {
            return totalRequests > 0 ? (successfulRequests / totalRequests) * 100 : 0;
        }

        public double getEncryptionOps() {
            return encryptionOps;
        }

        public double getDecryptionOps() {
            return decryptionOps;
        }

        public double getRefreshEvents() {
            return refreshEvents;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public int getCachedConfigs() {
            return cachedConfigs;
        }

        public long getLastRequestTime() {
            return lastRequestTime;
        }
    }
}