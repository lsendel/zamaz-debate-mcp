package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance alerting service for monitoring and alerting on performance issues
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceAlertingService {
    
    private final MeterRegistry meterRegistry;
    private final SystemHealthMonitor systemHealthMonitor;
    
    @Value("${monitoring.alerting.enabled:true}")
    private boolean alertingEnabled;
    
    @Value("${monitoring.alerting.email.enabled:false}")
    private boolean emailAlertsEnabled;
    
    @Value("${monitoring.alerting.slack.enabled:false}")
    private boolean slackAlertsEnabled;
    
    @Value("${monitoring.alerting.webhook.enabled:false}")
    private boolean webhookAlertsEnabled;
    
    @Value("${monitoring.alerting.cooldown.minutes:15}")
    private int alertCooldownMinutes;
    
    @Value("${monitoring.alerting.batch.size:10}")
    private int alertBatchSize;
    
    @Value("${monitoring.alerting.batch.window.seconds:30}")
    private int alertBatchWindowSeconds;
    
    // Alert tracking
    private final ConcurrentHashMap<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ActiveAlert> activeAlerts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    private final List<AlertHistory> alertHistory = Collections.synchronizedList(new ArrayList<>());
    
    // Alert thresholds
    private static final double DEFAULT_CPU_THRESHOLD = 85.0;
    private static final double DEFAULT_MEMORY_THRESHOLD = 90.0;
    private static final double DEFAULT_DISK_THRESHOLD = 85.0;
    private static final double DEFAULT_ERROR_RATE_THRESHOLD = 5.0;
    private static final long DEFAULT_RESPONSE_TIME_THRESHOLD = 5000L;
    
    /**
     * Initialize default alert rules
     */
    public void initializeDefaultAlerts() {
        if (!alertingEnabled) {
            log.info("Performance alerting is disabled");
            return;
        }
        
        log.info("Initializing default performance alert rules");
        
        // System resource alerts
        addAlertRule("high_cpu_usage", "CPU usage is high", 
            () -> getCurrentCpuUsage() > DEFAULT_CPU_THRESHOLD, 
            AlertSeverity.WARNING, AlertCategory.SYSTEM);
        
        addAlertRule("high_memory_usage", "Memory usage is high", 
            () -> getCurrentMemoryUsage() > DEFAULT_MEMORY_THRESHOLD, 
            AlertSeverity.CRITICAL, AlertCategory.SYSTEM);
        
        addAlertRule("high_disk_usage", "Disk usage is high", 
            () -> getCurrentDiskUsage() > DEFAULT_DISK_THRESHOLD, 
            AlertSeverity.WARNING, AlertCategory.SYSTEM);
        
        // Application performance alerts
        addAlertRule("high_error_rate", "Application error rate is high", 
            () -> getCurrentErrorRate() > DEFAULT_ERROR_RATE_THRESHOLD, 
            AlertSeverity.CRITICAL, AlertCategory.APPLICATION);
        
        addAlertRule("slow_response_time", "Application response time is slow", 
            () -> getAverageResponseTime() > DEFAULT_RESPONSE_TIME_THRESHOLD, 
            AlertSeverity.WARNING, AlertCategory.APPLICATION);
        
        // Health check alerts
        addAlertRule("system_health_critical", "System health is critical", 
            () -> {
                var status = systemHealthMonitor.getSystemHealthStatus();
                return status.getOverallStatus() == SystemHealthMonitor.HealthStatus.CRITICAL;
            }, 
            AlertSeverity.CRITICAL, AlertCategory.HEALTH);
        
        addAlertRule("database_connectivity", "Database connectivity issues", 
            () -> !isDatabaseConnected(), 
            AlertSeverity.CRITICAL, AlertCategory.DATABASE);
        
        log.info("Default performance alert rules initialized: {} rules", alertRules.size());
    }
    
    /**
     * Add custom alert rule
     */
    public void addAlertRule(String name, String description, AlertCondition condition, 
                           AlertSeverity severity, AlertCategory category) {
        AlertRule rule = AlertRule.builder()
            .name(name)
            .description(description)
            .condition(condition)
            .severity(severity)
            .category(category)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        alertRules.put(name, rule);
        log.info("Added alert rule: {} ({})", name, severity);
    }
    
    /**
     * Remove alert rule
     */
    public void removeAlertRule(String name) {
        alertRules.remove(name);
        log.info("Removed alert rule: {}", name);
    }
    
    /**
     * Enable/disable alert rule
     */
    public void setAlertRuleEnabled(String name, boolean enabled) {
        AlertRule rule = alertRules.get(name);
        if (rule != null) {
            rule.setEnabled(enabled);
            log.info("Alert rule '{}' {}", name, enabled ? "enabled" : "disabled");
        }
    }
    
    /**
     * Get all alert rules
     */
    public Map<String, AlertRule> getAllAlertRules() {
        return new HashMap<>(alertRules);
    }
    
    /**
     * Get all active alerts
     */
    public Map<String, ActiveAlert> getActiveAlerts() {
        return new HashMap<>(activeAlerts);
    }
    
    /**
     * Get alert history
     */
    public List<AlertHistory> getAlertHistory(int limit) {
        return alertHistory.stream()
            .sorted(Comparator.comparing(AlertHistory::getTimestamp).reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * Get alert statistics
     */
    public AlertStatistics getAlertStatistics() {
        long totalAlerts = alertHistory.size();
        long criticalAlerts = alertHistory.stream()
            .mapToLong(alert -> alert.getSeverity() == AlertSeverity.CRITICAL ? 1 : 0)
            .sum();
        long warningAlerts = alertHistory.stream()
            .mapToLong(alert -> alert.getSeverity() == AlertSeverity.WARNING ? 1 : 0)
            .sum();
        long infoAlerts = totalAlerts - criticalAlerts - warningAlerts;
        
        return AlertStatistics.builder()
            .totalAlerts(totalAlerts)
            .criticalAlerts(criticalAlerts)
            .warningAlerts(warningAlerts)
            .infoAlerts(infoAlerts)
            .activeAlerts(activeAlerts.size())
            .alertRules(alertRules.size())
            .build();
    }
    
    /**
     * Trigger manual alert
     */
    public void triggerAlert(String name, String message, AlertSeverity severity, 
                           AlertCategory category) {
        ActiveAlert alert = ActiveAlert.builder()
            .name(name)
            .message(message)
            .severity(severity)
            .category(category)
            .triggeredAt(LocalDateTime.now())
            .source("manual")
            .build();
        
        processAlert(alert);
    }
    
    /**
     * Resolve alert
     */
    public void resolveAlert(String name) {
        ActiveAlert alert = activeAlerts.remove(name);
        if (alert != null) {
            alert.setResolvedAt(LocalDateTime.now());
            
            // Add to history
            alertHistory.add(AlertHistory.builder()
                .name(alert.getName())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .category(alert.getCategory())
                .timestamp(alert.getTriggeredAt())
                .resolvedAt(alert.getResolvedAt())
                .duration(java.time.Duration.between(alert.getTriggeredAt(), alert.getResolvedAt()))
                .source(alert.getSource())
                .build());
            
            log.info("Alert resolved: {} after {}", name, 
                java.time.Duration.between(alert.getTriggeredAt(), alert.getResolvedAt()));
            
            // Update metrics
            meterRegistry.counter("performance.alerts.resolved", 
                "name", name, "severity", alert.getSeverity().name().toLowerCase()).increment();
        }
    }
    
    /**
     * Check alert conditions periodically
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void checkAlertConditions() {
        if (!alertingEnabled) {
            return;
        }
        
        try {
            alertRules.forEach((name, rule) -> {
                if (!rule.isEnabled()) {
                    return;
                }
                
                try {
                    boolean conditionMet = rule.getCondition().evaluate();
                    
                    if (conditionMet) {
                        // Check cooldown period
                        if (isInCooldownPeriod(name)) {
                            return;
                        }
                        
                        // Create alert
                        ActiveAlert alert = ActiveAlert.builder()
                            .name(name)
                            .message(rule.getDescription())
                            .severity(rule.getSeverity())
                            .category(rule.getCategory())
                            .triggeredAt(LocalDateTime.now())
                            .source("rule")
                            .build();
                        
                        processAlert(alert);
                        
                    } else {
                        // Condition not met, resolve alert if active
                        if (activeAlerts.containsKey(name)) {
                            resolveAlert(name);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Error evaluating alert condition for rule: {}", name, e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error checking alert conditions", e);
        }
    }
    
    /**
     * Send alert notifications
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void sendAlertNotifications() {
        if (!alertingEnabled) {
            return;
        }
        
        try {
            // Get pending alerts
            List<ActiveAlert> pendingAlerts = activeAlerts.values().stream()
                .filter(alert -> alert.getNotificationSentAt() == null)
                .sorted(Comparator.comparing(ActiveAlert::getSeverity).reversed())
                .limit(alertBatchSize)
                .toList();
            
            if (pendingAlerts.isEmpty()) {
                return;
            }
            
            // Send notifications
            for (ActiveAlert alert : pendingAlerts) {
                sendNotification(alert);
                alert.setNotificationSentAt(LocalDateTime.now());
            }
            
        } catch (Exception e) {
            log.error("Error sending alert notifications", e);
        }
    }
    
    /**
     * Clean up old alerts
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldAlerts() {
        try {
            // Clean up alert history
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            alertHistory.removeIf(alert -> alert.getTimestamp().isBefore(cutoff));
            
            // Clean up old cooldown entries
            LocalDateTime cooldownCutoff = LocalDateTime.now().minusMinutes(alertCooldownMinutes);
            lastAlertTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(cooldownCutoff));
            
            log.debug("Cleaned up old alert data");
            
        } catch (Exception e) {
            log.error("Error cleaning up old alerts", e);
        }
    }
    
    private void processAlert(ActiveAlert alert) {
        String alertKey = alert.getName();
        
        // Check if alert is already active
        if (activeAlerts.containsKey(alertKey)) {
            return;
        }
        
        // Add to active alerts
        activeAlerts.put(alertKey, alert);
        
        // Update last alert time
        lastAlertTimes.put(alertKey, LocalDateTime.now());
        
        // Update metrics
        meterRegistry.counter("performance.alerts.triggered", 
            "name", alert.getName(), 
            "severity", alert.getSeverity().name().toLowerCase(),
            "category", alert.getCategory().name().toLowerCase()).increment();
        
        meterRegistry.gauge("performance.alerts.active", activeAlerts.size());
        
        log.warn("Alert triggered: {} - {} [{}]", 
            alert.getName(), alert.getMessage(), alert.getSeverity());
    }
    
    private boolean isInCooldownPeriod(String alertName) {
        LocalDateTime lastAlert = lastAlertTimes.get(alertName);
        if (lastAlert == null) {
            return false;
        }
        
        return LocalDateTime.now().isBefore(lastAlert.plusMinutes(alertCooldownMinutes));
    }
    
    private void sendNotification(ActiveAlert alert) {
        try {
            // Email notification
            if (emailAlertsEnabled) {
                sendEmailNotification(alert);
            }
            
            // Slack notification
            if (slackAlertsEnabled) {
                sendSlackNotification(alert);
            }
            
            // Webhook notification
            if (webhookAlertsEnabled) {
                sendWebhookNotification(alert);
            }
            
        } catch (Exception e) {
            log.error("Failed to send notification for alert: {}", alert.getName(), e);
        }
    }
    
    private void sendEmailNotification(ActiveAlert alert) {
        // In a real implementation, this would send an email
        log.info("EMAIL ALERT: {} - {} [{}]", 
            alert.getName(), alert.getMessage(), alert.getSeverity());
    }
    
    private void sendSlackNotification(ActiveAlert alert) {
        // In a real implementation, this would send a Slack message
        log.info("SLACK ALERT: {} - {} [{}]", 
            alert.getName(), alert.getMessage(), alert.getSeverity());
    }
    
    private void sendWebhookNotification(ActiveAlert alert) {
        // In a real implementation, this would send a webhook
        log.info("WEBHOOK ALERT: {} - {} [{}]", 
            alert.getName(), alert.getMessage(), alert.getSeverity());
    }
    
    // Helper methods for alert conditions
    private double getCurrentCpuUsage() {
        return java.lang.management.ManagementFactory.getOperatingSystemMXBean()
            .getProcessCpuLoad() * 100;
    }
    
    private double getCurrentMemoryUsage() {
        var memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
        var heapUsage = memoryBean.getHeapMemoryUsage();
        return (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
    }
    
    private double getCurrentDiskUsage() {
        java.io.File root = new java.io.File("/");
        long totalSpace = root.getTotalSpace();
        long usableSpace = root.getUsableSpace();
        return (1.0 - (double) usableSpace / totalSpace) * 100;
    }
    
    private double getCurrentErrorRate() {
        // This would be implemented based on actual error tracking
        return 0.0;
    }
    
    private long getAverageResponseTime() {
        // This would be implemented based on actual response time tracking
        return 0L;
    }
    
    private boolean isDatabaseConnected() {
        // This would be implemented based on actual database connectivity check
        return true;
    }
    
    // Enums and interfaces
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public enum AlertCategory {
        SYSTEM, APPLICATION, DATABASE, SECURITY, BUSINESS, HEALTH
    }
    
    @FunctionalInterface
    public interface AlertCondition {
        boolean evaluate();
    }
    
    // Data classes
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AlertRule {
        private String name;
        private String description;
        private AlertCondition condition;
        private AlertSeverity severity;
        private AlertCategory category;
        private boolean enabled;
        private LocalDateTime createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ActiveAlert {
        private String name;
        private String message;
        private AlertSeverity severity;
        private AlertCategory category;
        private LocalDateTime triggeredAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime notificationSentAt;
        private String source;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AlertHistory {
        private String name;
        private String message;
        private AlertSeverity severity;
        private AlertCategory category;
        private LocalDateTime timestamp;
        private LocalDateTime resolvedAt;
        private java.time.Duration duration;
        private String source;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AlertStatistics {
        private long totalAlerts;
        private long criticalAlerts;
        private long warningAlerts;
        private long infoAlerts;
        private long activeAlerts;
        private long alertRules;
    }
}