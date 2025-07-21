package com.zamaz.mcp.controller.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for monitoring metrics and triggering alerts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowAlertingService {
    
    private final AgenticFlowMetricsCollector metricsCollector;
    
    @Value("${agentic-flow.alerts.error-threshold:10}")
    private int errorThreshold;
    
    @Value("${agentic-flow.alerts.latency-threshold-ms:5000}")
    private long latencyThresholdMs;
    
    @Value("${agentic-flow.alerts.queue-size-threshold:1000}")
    private int queueSizeThreshold;
    
    // Alert tracking
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    
    /**
     * Scheduled method to check metrics and trigger alerts.
     */
    @Scheduled(fixedDelay = 60000) // Check every minute
    public void checkMetricsAndAlert() {
        log.debug("Running alerting checks");
        
        checkErrorRates();
        checkLatency();
        checkQueueSizes();
        checkResourceUsage();
        
        // Clean up resolved alerts
        cleanupResolvedAlerts();
    }
    
    private void checkErrorRates() {
        errorCounts.forEach((flowType, count) -> {
            if (count.get() > errorThreshold) {
                String alertKey = "high_error_rate_" + flowType;
                
                if (!activeAlerts.containsKey(alertKey)) {
                    Alert alert = Alert.builder()
                        .id(UUID.randomUUID().toString())
                        .type(AlertType.HIGH_ERROR_RATE)
                        .severity(AlertSeverity.CRITICAL)
                        .flowType(flowType)
                        .message(String.format("High error rate for flow type %s: %d errors in last period", 
                            flowType, count.get()))
                        .timestamp(LocalDateTime.now())
                        .build();
                    
                    triggerAlert(alert);
                    activeAlerts.put(alertKey, alert);
                }
            }
        });
    }
    
    private void checkLatency() {
        // In real implementation, would query actual metrics
        // This is a placeholder
        String flowType = "TREE_OF_THOUGHTS";
        long avgLatency = 6000; // Mock value
        
        if (avgLatency > latencyThresholdMs) {
            String alertKey = "high_latency_" + flowType;
            
            if (!activeAlerts.containsKey(alertKey)) {
                Alert alert = Alert.builder()
                    .id(UUID.randomUUID().toString())
                    .type(AlertType.HIGH_LATENCY)
                    .severity(AlertSeverity.WARNING)
                    .flowType(flowType)
                    .message(String.format("High latency for flow type %s: %dms average", 
                        flowType, avgLatency))
                    .timestamp(LocalDateTime.now())
                    .build();
                
                triggerAlert(alert);
                activeAlerts.put(alertKey, alert);
            }
        }
    }
    
    private void checkQueueSizes() {
        // In real implementation, would query RabbitMQ
        int queueSize = 500; // Mock value
        
        if (queueSize > queueSizeThreshold) {
            String alertKey = "high_queue_size";
            
            if (!activeAlerts.containsKey(alertKey)) {
                Alert alert = Alert.builder()
                    .id(UUID.randomUUID().toString())
                    .type(AlertType.QUEUE_BACKLOG)
                    .severity(AlertSeverity.WARNING)
                    .message(String.format("High queue size: %d messages pending", queueSize))
                    .timestamp(LocalDateTime.now())
                    .build();
                
                triggerAlert(alert);
                activeAlerts.put(alertKey, alert);
            }
        }
    }
    
    private void checkResourceUsage() {
        // Check CPU, memory, connection pool usage
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();
        
        if (cpuUsage > 80) {
            triggerResourceAlert("CPU", cpuUsage);
        }
        
        if (memoryUsage > 85) {
            triggerResourceAlert("Memory", memoryUsage);
        }
    }
    
    private void triggerResourceAlert(String resourceType, double usage) {
        String alertKey = "high_resource_" + resourceType.toLowerCase();
        
        if (!activeAlerts.containsKey(alertKey)) {
            Alert alert = Alert.builder()
                .id(UUID.randomUUID().toString())
                .type(AlertType.RESOURCE_EXHAUSTION)
                .severity(usage > 90 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING)
                .message(String.format("High %s usage: %.1f%%", resourceType, usage))
                .timestamp(LocalDateTime.now())
                .build();
            
            triggerAlert(alert);
            activeAlerts.put(alertKey, alert);
        }
    }
    
    private void triggerAlert(Alert alert) {
        log.warn("ALERT: {} - {}", alert.getSeverity(), alert.getMessage());
        
        // In real implementation, would:
        // 1. Send to monitoring system (e.g., PagerDuty, OpsGenie)
        // 2. Send email/Slack notifications
        // 3. Log to centralized logging
        // 4. Update dashboard
        
        // For now, just log
        switch (alert.getSeverity()) {
            case CRITICAL:
                log.error("CRITICAL ALERT: {}", alert);
                break;
            case WARNING:
                log.warn("WARNING ALERT: {}", alert);
                break;
            case INFO:
                log.info("INFO ALERT: {}", alert);
                break;
        }
    }
    
    private void cleanupResolvedAlerts() {
        // Remove alerts that have been resolved
        activeAlerts.entrySet().removeIf(entry -> {
            Alert alert = entry.getValue();
            
            // Check if condition still exists
            boolean resolved = isAlertResolved(alert);
            
            if (resolved) {
                log.info("Alert resolved: {}", alert.getMessage());
                return true;
            }
            
            return false;
        });
    }
    
    private boolean isAlertResolved(Alert alert) {
        // Check if the alert condition is still valid
        // This is a simplified check
        return alert.getTimestamp().isBefore(LocalDateTime.now().minusMinutes(5));
    }
    
    private double getCpuUsage() {
        // In real implementation, would get actual CPU usage
        return 45.0;
    }
    
    private double getMemoryUsage() {
        // In real implementation, would get actual memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return ((double) (totalMemory - freeMemory) / totalMemory) * 100;
    }
    
    /**
     * Records an error for alerting purposes.
     */
    public void recordError(String flowType) {
        errorCounts.computeIfAbsent(flowType, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Resets error counts (called periodically).
     */
    @Scheduled(fixedDelay = 300000) // Reset every 5 minutes
    public void resetErrorCounts() {
        errorCounts.clear();
    }
}

/**
 * Alert data model.
 */
@lombok.Data
@lombok.Builder
class Alert {
    private String id;
    private AlertType type;
    private AlertSeverity severity;
    private String flowType;
    private String message;
    private LocalDateTime timestamp;
}

/**
 * Alert types.
 */
enum AlertType {
    HIGH_ERROR_RATE,
    HIGH_LATENCY,
    QUEUE_BACKLOG,
    RESOURCE_EXHAUSTION,
    SERVICE_DOWN,
    DEGRADED_PERFORMANCE
}

/**
 * Alert severity levels.
 */
enum AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}