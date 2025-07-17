package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System health monitoring component
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemHealthMonitor {
    
    private final MeterRegistry meterRegistry;
    
    // System MXBeans
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    // Health tracking
    private final ConcurrentHashMap<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SystemAlert> activeAlerts = new ConcurrentHashMap<>();
    
    // Health thresholds
    private static final double CPU_WARNING_THRESHOLD = 0.7;
    private static final double CPU_CRITICAL_THRESHOLD = 0.9;
    private static final double MEMORY_WARNING_THRESHOLD = 0.8;
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.9;
    private static final double DISK_WARNING_THRESHOLD = 0.8;
    private static final double DISK_CRITICAL_THRESHOLD = 0.9;
    private static final int THREAD_WARNING_THRESHOLD = 200;
    private static final int THREAD_CRITICAL_THRESHOLD = 500;
    
    /**
     * Register a custom health check
     */
    public void registerHealthCheck(String name, HealthCheck healthCheck) {
        healthChecks.put(name, healthCheck);
        log.info("Registered health check: {}", name);
    }
    
    /**
     * Remove a health check
     */
    public void removeHealthCheck(String name) {
        healthChecks.remove(name);
        log.info("Removed health check: {}", name);
    }
    
    /**
     * Get current system health status
     */
    public SystemHealthStatus getSystemHealthStatus() {
        List<HealthCheckResult> results = new ArrayList<>();
        
        // System health checks
        results.add(checkCpuHealth());
        results.add(checkMemoryHealth());
        results.add(checkDiskHealth());
        results.add(checkThreadHealth());
        
        // Custom health checks
        healthChecks.forEach((name, healthCheck) -> {
            try {
                HealthCheckResult result = healthCheck.check();
                result.setName(name);
                results.add(result);
            } catch (Exception e) {
                log.error("Health check '{}' failed", name, e);
                results.add(HealthCheckResult.builder()
                    .name(name)
                    .status(HealthStatus.CRITICAL)
                    .message("Health check failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
            }
        });
        
        // Calculate overall health
        HealthStatus overallStatus = calculateOverallHealth(results);
        
        return SystemHealthStatus.builder()
            .overallStatus(overallStatus)
            .timestamp(LocalDateTime.now())
            .healthChecks(results)
            .activeAlerts(new ArrayList<>(activeAlerts.values()))
            .build();
    }
    
    /**
     * Monitor system health periodically
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorSystemHealth() {
        try {
            SystemHealthStatus status = getSystemHealthStatus();
            
            // Update metrics
            updateHealthMetrics(status);
            
            // Check for alerts
            checkForAlerts(status);
            
            // Log health status
            logHealthStatus(status);
            
        } catch (Exception e) {
            log.error("Error monitoring system health", e);
        }
    }
    
    /**
     * Check CPU health
     */
    private HealthCheckResult checkCpuHealth() {
        double cpuUsage = osMXBean.getProcessCpuLoad();
        
        HealthStatus status;
        String message;
        
        if (cpuUsage >= CPU_CRITICAL_THRESHOLD) {
            status = HealthStatus.CRITICAL;
            message = String.format("CPU usage critical: %.2f%%", cpuUsage * 100);
        } else if (cpuUsage >= CPU_WARNING_THRESHOLD) {
            status = HealthStatus.WARNING;
            message = String.format("CPU usage warning: %.2f%%", cpuUsage * 100);
        } else {
            status = HealthStatus.HEALTHY;
            message = String.format("CPU usage healthy: %.2f%%", cpuUsage * 100);
        }
        
        return HealthCheckResult.builder()
            .name("cpu")
            .status(status)
            .message(message)
            .value(cpuUsage)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Check memory health
     */
    private HealthCheckResult checkMemoryHealth() {
        var heapUsage = memoryMXBean.getHeapMemoryUsage();
        double memoryUsage = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        HealthStatus status;
        String message;
        
        if (memoryUsage >= MEMORY_CRITICAL_THRESHOLD) {
            status = HealthStatus.CRITICAL;
            message = String.format("Memory usage critical: %.2f%%", memoryUsage * 100);
        } else if (memoryUsage >= MEMORY_WARNING_THRESHOLD) {
            status = HealthStatus.WARNING;
            message = String.format("Memory usage warning: %.2f%%", memoryUsage * 100);
        } else {
            status = HealthStatus.HEALTHY;
            message = String.format("Memory usage healthy: %.2f%%", memoryUsage * 100);
        }
        
        return HealthCheckResult.builder()
            .name("memory")
            .status(status)
            .message(message)
            .value(memoryUsage)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Check disk health
     */
    private HealthCheckResult checkDiskHealth() {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long usableSpace = root.getUsableSpace();
        double diskUsage = 1.0 - ((double) usableSpace / totalSpace);
        
        HealthStatus status;
        String message;
        
        if (diskUsage >= DISK_CRITICAL_THRESHOLD) {
            status = HealthStatus.CRITICAL;
            message = String.format("Disk usage critical: %.2f%%", diskUsage * 100);
        } else if (diskUsage >= DISK_WARNING_THRESHOLD) {
            status = HealthStatus.WARNING;
            message = String.format("Disk usage warning: %.2f%%", diskUsage * 100);
        } else {
            status = HealthStatus.HEALTHY;
            message = String.format("Disk usage healthy: %.2f%%", diskUsage * 100);
        }
        
        return HealthCheckResult.builder()
            .name("disk")
            .status(status)
            .message(message)
            .value(diskUsage)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Check thread health
     */
    private HealthCheckResult checkThreadHealth() {
        int threadCount = threadMXBean.getThreadCount();
        
        HealthStatus status;
        String message;
        
        if (threadCount >= THREAD_CRITICAL_THRESHOLD) {
            status = HealthStatus.CRITICAL;
            message = String.format("Thread count critical: %d threads", threadCount);
        } else if (threadCount >= THREAD_WARNING_THRESHOLD) {
            status = HealthStatus.WARNING;
            message = String.format("Thread count warning: %d threads", threadCount);
        } else {
            status = HealthStatus.HEALTHY;
            message = String.format("Thread count healthy: %d threads", threadCount);
        }
        
        return HealthCheckResult.builder()
            .name("threads")
            .status(status)
            .message(message)
            .value(threadCount)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Calculate overall health status
     */
    private HealthStatus calculateOverallHealth(List<HealthCheckResult> results) {
        boolean hasCritical = results.stream()
            .anyMatch(result -> result.getStatus() == HealthStatus.CRITICAL);
        
        if (hasCritical) {
            return HealthStatus.CRITICAL;
        }
        
        boolean hasWarning = results.stream()
            .anyMatch(result -> result.getStatus() == HealthStatus.WARNING);
        
        if (hasWarning) {
            return HealthStatus.WARNING;
        }
        
        return HealthStatus.HEALTHY;
    }
    
    /**
     * Update health metrics
     */
    private void updateHealthMetrics(SystemHealthStatus status) {
        // Overall health score
        double healthScore = calculateHealthScore(status);
        meterRegistry.gauge("system.health.score", healthScore);
        
        // Individual health check metrics
        status.getHealthChecks().forEach(result -> {
            double score = result.getStatus() == HealthStatus.HEALTHY ? 1.0 : 
                          result.getStatus() == HealthStatus.WARNING ? 0.5 : 0.0;
            
            meterRegistry.gauge("system.health.check", 
                io.micrometer.core.instrument.Tags.of("name", result.getName()), score);
            
            if (result.getValue() != null) {
                meterRegistry.gauge("system.health.value", 
                    io.micrometer.core.instrument.Tags.of("name", result.getName()), 
                    result.getValue());
            }
        });
        
        // Active alerts count
        meterRegistry.gauge("system.health.alerts.active", activeAlerts.size());
    }
    
    /**
     * Check for alerts
     */
    private void checkForAlerts(SystemHealthStatus status) {
        status.getHealthChecks().forEach(result -> {
            String alertKey = "health_check_" + result.getName();
            
            if (result.getStatus() == HealthStatus.CRITICAL || result.getStatus() == HealthStatus.WARNING) {
                // Create or update alert
                SystemAlert alert = SystemAlert.builder()
                    .id(alertKey)
                    .type("HEALTH_CHECK")
                    .severity(result.getStatus() == HealthStatus.CRITICAL ? AlertSeverity.CRITICAL : AlertSeverity.WARNING)
                    .title(String.format("Health Check Alert: %s", result.getName()))
                    .message(result.getMessage())
                    .source("SystemHealthMonitor")
                    .timestamp(LocalDateTime.now())
                    .build();
                
                activeAlerts.put(alertKey, alert);
                
                // Record alert metric
                meterRegistry.counter("system.health.alerts.triggered", 
                    "type", "health_check",
                    "name", result.getName(),
                    "severity", alert.getSeverity().name().toLowerCase()).increment();
                
            } else {
                // Clear alert if health is good
                if (activeAlerts.containsKey(alertKey)) {
                    activeAlerts.remove(alertKey);
                    
                    // Record alert resolution
                    meterRegistry.counter("system.health.alerts.resolved", 
                        "type", "health_check",
                        "name", result.getName()).increment();
                }
            }
        });
    }
    
    /**
     * Log health status
     */
    private void logHealthStatus(SystemHealthStatus status) {
        if (status.getOverallStatus() == HealthStatus.CRITICAL) {
            log.error("System health is CRITICAL");
        } else if (status.getOverallStatus() == HealthStatus.WARNING) {
            log.warn("System health is WARNING");
        } else {
            log.debug("System health is HEALTHY");
        }
        
        // Log individual health check issues
        status.getHealthChecks().stream()
            .filter(result -> result.getStatus() != HealthStatus.HEALTHY)
            .forEach(result -> {
                if (result.getStatus() == HealthStatus.CRITICAL) {
                    log.error("Health check '{}': {}", result.getName(), result.getMessage());
                } else {
                    log.warn("Health check '{}': {}", result.getName(), result.getMessage());
                }
            });
    }
    
    /**
     * Calculate health score (0.0 to 1.0)
     */
    private double calculateHealthScore(SystemHealthStatus status) {
        if (status.getHealthChecks().isEmpty()) {
            return 1.0;
        }
        
        double totalScore = status.getHealthChecks().stream()
            .mapToDouble(result -> switch (result.getStatus()) {
                case HEALTHY -> 1.0;
                case WARNING -> 0.5;
                case CRITICAL -> 0.0;
            })
            .sum();
        
        return totalScore / status.getHealthChecks().size();
    }
    
    // Enums and data classes
    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public interface HealthCheck {
        HealthCheckResult check();
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class HealthCheckResult {
        private String name;
        private HealthStatus status;
        private String message;
        private Double value;
        private LocalDateTime timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SystemHealthStatus {
        private HealthStatus overallStatus;
        private LocalDateTime timestamp;
        private List<HealthCheckResult> healthChecks;
        private List<SystemAlert> activeAlerts;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SystemAlert {
        private String id;
        private String type;
        private AlertSeverity severity;
        private String title;
        private String message;
        private String source;
        private LocalDateTime timestamp;
    }
}