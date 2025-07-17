package com.zamaz.mcp.common.versioning;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Metrics collection for API versioning
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiVersionMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> versionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> versionTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    /**
     * Record API version usage
     */
    public void recordVersionUsage(String version, ApiVersionResolver.VersionSource source) {
        String metricName = "api.version.usage";
        String key = version + ":" + source.toString();
        
        versionCounters.computeIfAbsent(key, k -> 
            Counter.builder(metricName)
                .description("API version usage count")
                .tag("version", version)
                .tag("source", source.toString())
                .register(meterRegistry)
        ).increment();
    }

    /**
     * Record response time by version
     */
    public void recordResponseTime(String version, long responseTimeMs) {
        String metricName = "api.version.response.time";
        
        versionTimers.computeIfAbsent(version, k -> 
            Timer.builder(metricName)
                .description("API response time by version")
                .tag("version", version)
                .register(meterRegistry)
        ).record(responseTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record errors by version
     */
    public void recordError(String version, Throwable error) {
        String metricName = "api.version.errors";
        String errorType = error.getClass().getSimpleName();
        String key = version + ":" + errorType;
        
        errorCounters.computeIfAbsent(key, k -> 
            Counter.builder(metricName)
                .description("API errors by version")
                .tag("version", version)
                .tag("error_type", errorType)
                .register(meterRegistry)
        ).increment();
        
        log.error("API error in version {}: {}", version, error.getMessage());
    }

    /**
     * Record deprecated version usage
     */
    public void recordDeprecatedUsage(String version, String remoteAddr) {
        Counter.builder("api.version.deprecated.usage")
            .description("Deprecated API version usage")
            .tag("version", version)
            .tag("client_ip", remoteAddr)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record version transition (when client upgrades)
     */
    public void recordVersionTransition(String fromVersion, String toVersion) {
        Counter.builder("api.version.transitions")
            .description("API version transitions")
            .tag("from_version", fromVersion)
            .tag("to_version", toVersion)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record unsupported version requests
     */
    public void recordUnsupportedVersionRequest(String requestedVersion) {
        Counter.builder("api.version.unsupported.requests")
            .description("Unsupported API version requests")
            .tag("requested_version", requestedVersion)
            .register(meterRegistry)
            .increment();
    }
}