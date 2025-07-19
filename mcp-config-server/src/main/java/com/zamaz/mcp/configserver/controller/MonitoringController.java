package com.zamaz.mcp.configserver.controller;

import com.zamaz.mcp.configserver.monitoring.ConfigServerMetrics;
import com.zamaz.mcp.configserver.event.ConfigRefreshEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for monitoring Config Server operations.
 * Provides endpoints for metrics, health, and operational insights.
 */
@RestController
@RequestMapping("/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {

    @Autowired
    private ConfigServerMetrics configServerMetrics;

    @Autowired
    private ConfigRefreshEventListener refreshEventListener;

    @Autowired(required = false)
    private HealthEndpoint healthEndpoint;

    /**
     * Gets current metrics summary.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        ConfigServerMetrics.MetricsSummary summary = configServerMetrics.getMetricsSummary();
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("requests", Map.of(
            "total", summary.getTotalRequests(),
            "successful", summary.getSuccessfulRequests(),
            "failed", summary.getFailedRequests(),
            "successRate", String.format("%.2f%%", summary.getSuccessRate())
        ));
        
        metrics.put("operations", Map.of(
            "encryption", summary.getEncryptionOps(),
            "decryption", summary.getDecryptionOps(),
            "refreshEvents", summary.getRefreshEvents()
        ));
        
        metrics.put("current", Map.of(
            "activeConnections", summary.getActiveConnections(),
            "cachedConfigurations", summary.getCachedConfigs(),
            "lastRequestTime", summary.getLastRequestTime() > 0 ? 
                new java.util.Date(summary.getLastRequestTime()) : "Never"
        ));
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Gets refresh event statistics.
     */
    @GetMapping("/refresh-stats")
    public ResponseEntity<Map<String, Object>> getRefreshStats() {
        return ResponseEntity.ok(refreshEventListener.getRefreshStatistics());
    }

    /**
     * Gets detailed health information.
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        
        if (healthEndpoint != null) {
            var healthStatus = healthEndpoint.health();
            health.put("status", healthStatus.getStatus().getCode());
            health.put("components", healthStatus.getComponents());
        }
        
        // Add custom health information
        health.put("configServer", Map.of(
            "uptime", getUptime(),
            "version", getClass().getPackage().getImplementationVersion(),
            "timestamp", LocalDateTime.now()
        ));
        
        return ResponseEntity.ok(health);
    }

    /**
     * Gets configuration access patterns.
     */
    @GetMapping("/access-patterns")
    public ResponseEntity<Map<String, Object>> getAccessPatterns() {
        // In a real implementation, this would analyze access logs
        Map<String, Object> patterns = new HashMap<>();
        
        patterns.put("mostAccessedApplications", getMostAccessedApplications());
        patterns.put("peakAccessTimes", getPeakAccessTimes());
        patterns.put("errorPatterns", getErrorPatterns());
        
        return ResponseEntity.ok(patterns);
    }

    /**
     * Gets performance metrics.
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> performance = new HashMap<>();
        
        performance.put("averageResponseTime", "15ms"); // Placeholder
        performance.put("p95ResponseTime", "45ms"); // Placeholder
        performance.put("p99ResponseTime", "120ms"); // Placeholder
        performance.put("throughput", "1000 req/min"); // Placeholder
        
        return ResponseEntity.ok(performance);
    }

    /**
     * Gets alert status.
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        Map<String, Object> alerts = new HashMap<>();
        
        // Check for potential issues
        ConfigServerMetrics.MetricsSummary summary = configServerMetrics.getMetricsSummary();
        
        if (summary.getFailedRequests() > summary.getSuccessfulRequests() * 0.1) {
            alerts.put("highFailureRate", Map.of(
                "severity", "WARNING",
                "message", "Failure rate exceeds 10%",
                "failureRate", String.format("%.2f%%", 
                    (summary.getFailedRequests() / summary.getTotalRequests()) * 100)
            ));
        }
        
        if (summary.getActiveConnections() > 100) {
            alerts.put("highConnectionCount", Map.of(
                "severity", "INFO",
                "message", "High number of active connections",
                "activeConnections", summary.getActiveConnections()
            ));
        }
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * Resets monitoring statistics.
     */
    @PostMapping("/reset-stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> resetStatistics() {
        refreshEventListener.resetStatistics();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Statistics reset successfully",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Gets system diagnostics.
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> getDiagnostics() {
        Map<String, Object> diagnostics = new HashMap<>();
        
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        diagnostics.put("jvm", Map.of(
            "totalMemory", formatBytes(runtime.totalMemory()),
            "freeMemory", formatBytes(runtime.freeMemory()),
            "maxMemory", formatBytes(runtime.maxMemory()),
            "availableProcessors", runtime.availableProcessors()
        ));
        
        // Thread information
        diagnostics.put("threads", Map.of(
            "activeCount", Thread.activeCount(),
            "peakThreadCount", java.lang.management.ManagementFactory.getThreadMXBean().getPeakThreadCount()
        ));
        
        // System properties
        diagnostics.put("system", Map.of(
            "os", System.getProperty("os.name"),
            "javaVersion", System.getProperty("java.version"),
            "userTimezone", System.getProperty("user.timezone")
        ));
        
        return ResponseEntity.ok(diagnostics);
    }

    // Helper methods

    private String getUptime() {
        long uptimeMillis = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptimeMillis / (60 * 60 * 1000);
        long minutes = (uptimeMillis % (60 * 60 * 1000)) / (60 * 1000);
        return String.format("%d hours, %d minutes", hours, minutes);
    }

    private Map<String, Integer> getMostAccessedApplications() {
        // Placeholder - would be populated from actual metrics
        return Map.of(
            "mcp-organization", 450,
            "mcp-controller", 380,
            "mcp-llm", 290
        );
    }

    private Map<String, String> getPeakAccessTimes() {
        // Placeholder - would be populated from actual metrics
        return Map.of(
            "morning", "09:00 - 11:00",
            "afternoon", "14:00 - 16:00",
            "evening", "19:00 - 21:00"
        );
    }

    private Map<String, Integer> getErrorPatterns() {
        // Placeholder - would be populated from actual metrics
        return Map.of(
            "connectionTimeout", 15,
            "authenticationFailure", 8,
            "configNotFound", 3
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}