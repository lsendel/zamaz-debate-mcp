package com.zamaz.mcp.common.resilience.monitoring;

import com.zamaz.mcp.common.resilience.metrics.RetryMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Monitoring service for retry operations with alerting and analytics capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "mcp.resilience.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class RetryMonitoringService {
    
    private final RetryMetricsCollector metricsCollector;
    private final Map<String, RetryHealthSnapshot> healthSnapshots = new ConcurrentHashMap<>();
    private final Map<String, List<RetryAlert>> activeAlerts = new ConcurrentHashMap<>();
    
    @Value("${mcp.resilience.monitoring.success-rate-threshold:0.8}")
    private double successRateThreshold;
    
    @Value("${mcp.resilience.monitoring.average-attempts-threshold:2.5}")
    private double averageAttemptsThreshold;
    
    @Value("${mcp.resilience.monitoring.duration-threshold-ms:5000}")
    private long durationThresholdMs;
    
    @Value("${mcp.resilience.monitoring.alert-cooldown-minutes:15}")
    private int alertCooldownMinutes;
    
    @PostConstruct
    public void initialize() {
        log.info("Retry monitoring service initialized with thresholds: " +
                "successRate={}, avgAttempts={}, durationMs={}", 
                successRateThreshold, averageAttemptsThreshold, durationThresholdMs);
    }
    
    /**
     * Performs comprehensive health check of all retry operations.
     */
    @Scheduled(fixedRateString = "${mcp.resilience.monitoring.check-interval-ms:60000}")
    public void performHealthCheck() {
        log.debug("Performing retry health check...");
        
        Map<String, RetryMetricsCollector.RetryStats> allStats = metricsCollector.getAllRetryStats();
        Map<String, RetryHealthSnapshot> newSnapshots = new HashMap<>();
        
        for (Map.Entry<String, RetryMetricsCollector.RetryStats> entry : allStats.entrySet()) {
            String retryName = entry.getKey();
            RetryMetricsCollector.RetryStats stats = entry.getValue();
            
            RetryHealthSnapshot snapshot = analyzeRetryHealth(retryName, stats);
            newSnapshots.put(retryName, snapshot);
            
            // Compare with previous snapshot and detect issues
            RetryHealthSnapshot previousSnapshot = healthSnapshots.get(retryName);
            List<RetryAlert> newAlerts = detectHealthIssues(retryName, snapshot, previousSnapshot);
            
            if (!newAlerts.isEmpty()) {
                processNewAlerts(retryName, newAlerts);
            }
        }
        
        healthSnapshots.putAll(newSnapshots);
        cleanupExpiredAlerts();
        
        log.debug("Health check completed for {} retry operations", allStats.size());
    }
    
    /**
     * Analyzes the health of a specific retry operation.
     */
    private RetryHealthSnapshot analyzeRetryHealth(String retryName, RetryMetricsCollector.RetryStats stats) {
        RetryHealthStatus status = determineHealthStatus(stats);
        List<String> healthIssues = identifyHealthIssues(stats);
        
        return RetryHealthSnapshot.builder()
            .retryName(retryName)
            .timestamp(Instant.now())
            .status(status)
            .successRate(stats.getSuccessRate())
            .averageAttempts(stats.getAverageAttemptsPerExecution())
            .averageDuration(stats.getAverageDurationMs())
            .totalExecutions(stats.getTotalExecutions())
            .recentErrors(extractRecentErrors(stats))
            .healthIssues(healthIssues)
            .build();
    }
    
    /**
     * Determines the overall health status based on metrics.
     */
    private RetryHealthStatus determineHealthStatus(RetryMetricsCollector.RetryStats stats) {
        if (stats.getTotalExecutions() == 0) {
            return RetryHealthStatus.UNKNOWN;
        }
        
        boolean successRateOk = stats.getSuccessRate() >= successRateThreshold;
        boolean attemptsOk = stats.getAverageAttemptsPerExecution() <= averageAttemptsThreshold;
        boolean durationOk = stats.getAverageDurationMs() <= durationThresholdMs;
        
        if (successRateOk && attemptsOk && durationOk) {
            return RetryHealthStatus.HEALTHY;
        } else if (stats.getSuccessRate() >= 0.6) {
            return RetryHealthStatus.WARNING;
        } else {
            return RetryHealthStatus.CRITICAL;
        }
    }
    
    /**
     * Identifies specific health issues.
     */
    private List<String> identifyHealthIssues(RetryMetricsCollector.RetryStats stats) {
        List<String> issues = new ArrayList<>();
        
        if (stats.getSuccessRate() < successRateThreshold) {
            issues.add(String.format("Low success rate: %.1f%% (threshold: %.1f%%)", 
                stats.getSuccessRate() * 100, successRateThreshold * 100));
        }
        
        if (stats.getAverageAttemptsPerExecution() > averageAttemptsThreshold) {
            issues.add(String.format("High average attempts: %.1f (threshold: %.1f)", 
                stats.getAverageAttemptsPerExecution(), averageAttemptsThreshold));
        }
        
        if (stats.getAverageDurationMs() > durationThresholdMs) {
            issues.add(String.format("High average duration: %.0fms (threshold: %dms)", 
                stats.getAverageDurationMs(), durationThresholdMs));
        }
        
        if (stats.getFailedExecutions() > 0) {
            double failureRate = (double) stats.getFailedExecutions() / stats.getTotalExecutions();
            if (failureRate > 0.1) { // More than 10% failures
                issues.add(String.format("High failure rate: %.1f%%", failureRate * 100));
            }
        }
        
        return issues;
    }
    
    /**
     * Extracts recent error information.
     */
    private List<String> extractRecentErrors(RetryMetricsCollector.RetryStats stats) {
        List<String> errors = new ArrayList<>();
        
        if (stats.getLastError() != null) {
            errors.add(stats.getLastError());
        }
        
        return errors;
    }
    
    /**
     * Detects health issues by comparing current and previous snapshots.
     */
    private List<RetryAlert> detectHealthIssues(String retryName, RetryHealthSnapshot current, 
                                               RetryHealthSnapshot previous) {
        List<RetryAlert> alerts = new ArrayList<>();
        
        // Check for status degradation
        if (previous != null && current.getStatus().ordinal() > previous.getStatus().ordinal()) {
            alerts.add(createAlert(retryName, RetryAlertType.STATUS_DEGRADATION,
                String.format("Status degraded from %s to %s", previous.getStatus(), current.getStatus()),
                current.getStatus().getSeverity()));
        }
        
        // Check for success rate drop
        if (previous != null && current.getSuccessRate() < previous.getSuccessRate() - 0.1) {
            alerts.add(createAlert(retryName, RetryAlertType.SUCCESS_RATE_DROP,
                String.format("Success rate dropped from %.1f%% to %.1f%%", 
                    previous.getSuccessRate() * 100, current.getSuccessRate() * 100),
                AlertSeverity.HIGH));
        }
        
        // Check for threshold violations
        if (current.getSuccessRate() < successRateThreshold) {
            alerts.add(createAlert(retryName, RetryAlertType.LOW_SUCCESS_RATE,
                String.format("Success rate %.1f%% below threshold %.1f%%", 
                    current.getSuccessRate() * 100, successRateThreshold * 100),
                current.getSuccessRate() < 0.5 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH));
        }
        
        if (current.getAverageAttempts() > averageAttemptsThreshold) {
            alerts.add(createAlert(retryName, RetryAlertType.HIGH_RETRY_ATTEMPTS,
                String.format("Average attempts %.1f above threshold %.1f", 
                    current.getAverageAttempts(), averageAttemptsThreshold),
                AlertSeverity.MEDIUM));
        }
        
        if (current.getAverageDuration() > durationThresholdMs) {
            alerts.add(createAlert(retryName, RetryAlertType.HIGH_DURATION,
                String.format("Average duration %.0fms above threshold %dms", 
                    current.getAverageDuration(), durationThresholdMs),
                AlertSeverity.MEDIUM));
        }
        
        return alerts;
    }
    
    /**
     * Creates a retry alert.
     */
    private RetryAlert createAlert(String retryName, RetryAlertType type, String message, AlertSeverity severity) {
        return RetryAlert.builder()
            .id(UUID.randomUUID().toString())
            .retryName(retryName)
            .type(type)
            .severity(severity)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Processes new alerts, applying cooldown logic and notifications.
     */
    private void processNewAlerts(String retryName, List<RetryAlert> newAlerts) {
        List<RetryAlert> currentAlerts = activeAlerts.computeIfAbsent(retryName, k -> new ArrayList<>());
        
        for (RetryAlert alert : newAlerts) {
            // Check if similar alert exists within cooldown period
            boolean shouldSuppress = currentAlerts.stream()
                .anyMatch(existing -> isSimilarAlert(existing, alert) && 
                    isWithinCooldown(existing.getTimestamp()));
            
            if (!shouldSuppress) {
                currentAlerts.add(alert);
                notifyAlert(alert);
                log.warn("Retry alert triggered: [{}] {} - {}", 
                    alert.getSeverity(), alert.getRetryName(), alert.getMessage());
            } else {
                log.debug("Alert suppressed due to cooldown: {} - {}", alert.getRetryName(), alert.getMessage());
            }
        }
    }
    
    /**
     * Checks if two alerts are similar (same type and retry name).
     */
    private boolean isSimilarAlert(RetryAlert existing, RetryAlert newAlert) {
        return existing.getType() == newAlert.getType() && 
               existing.getRetryName().equals(newAlert.getRetryName());
    }
    
    /**
     * Checks if a timestamp is within the cooldown period.
     */
    private boolean isWithinCooldown(Instant timestamp) {
        return timestamp.isAfter(Instant.now().minus(Duration.ofMinutes(alertCooldownMinutes)));
    }
    
    /**
     * Sends notification for an alert.
     */
    private void notifyAlert(RetryAlert alert) {
        // In a real implementation, this would integrate with notification systems
        // (Slack, PagerDuty, email, etc.)
        log.info("RETRY ALERT [{}]: {} - {}", alert.getSeverity(), alert.getRetryName(), alert.getMessage());
    }
    
    /**
     * Removes expired alerts.
     */
    private void cleanupExpiredAlerts() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24)); // Keep alerts for 24 hours
        
        activeAlerts.forEach((retryName, alerts) -> {
            alerts.removeIf(alert -> alert.getTimestamp().isBefore(cutoff));
        });
        
        // Remove empty alert lists
        activeAlerts.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Gets current health overview of all retry operations.
     */
    public RetryHealthOverview getHealthOverview() {
        Map<String, RetryHealthSnapshot> currentSnapshots = new HashMap<>(healthSnapshots);
        
        long healthyCount = currentSnapshots.values().stream()
            .mapToLong(s -> s.getStatus() == RetryHealthStatus.HEALTHY ? 1 : 0)
            .sum();
        
        long warningCount = currentSnapshots.values().stream()
            .mapToLong(s -> s.getStatus() == RetryHealthStatus.WARNING ? 1 : 0)
            .sum();
        
        long criticalCount = currentSnapshots.values().stream()
            .mapToLong(s -> s.getStatus() == RetryHealthStatus.CRITICAL ? 1 : 0)
            .sum();
        
        List<RetryAlert> allActiveAlerts = activeAlerts.values().stream()
            .flatMap(List::stream)
            .sorted(Comparator.comparing(RetryAlert::getTimestamp).reversed())
            .collect(Collectors.toList());
        
        return RetryHealthOverview.builder()
            .timestamp(Instant.now())
            .totalRetryOperations(currentSnapshots.size())
            .healthyCount(healthyCount)
            .warningCount(warningCount)
            .criticalCount(criticalCount)
            .totalActiveAlerts(allActiveAlerts.size())
            .snapshots(currentSnapshots)
            .recentAlerts(allActiveAlerts)
            .build();
    }
    
    /**
     * Gets health snapshot for a specific retry operation.
     */
    public Optional<RetryHealthSnapshot> getRetryHealth(String retryName) {
        return Optional.ofNullable(healthSnapshots.get(retryName));
    }
    
    /**
     * Gets active alerts for a specific retry operation.
     */
    public List<RetryAlert> getActiveAlerts(String retryName) {
        return new ArrayList<>(activeAlerts.getOrDefault(retryName, Collections.emptyList()));
    }
    
    /**
     * Gets all active alerts.
     */
    public List<RetryAlert> getAllActiveAlerts() {
        return activeAlerts.values().stream()
            .flatMap(List::stream)
            .sorted(Comparator.comparing(RetryAlert::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Manually clears alerts for a specific retry operation.
     */
    public void clearAlerts(String retryName) {
        activeAlerts.remove(retryName);
        log.info("Manually cleared alerts for retry operation: {}", retryName);
    }
    
    /**
     * Generates retry health report.
     */
    public String generateHealthReport() {
        RetryHealthOverview overview = getHealthOverview();
        StringBuilder report = new StringBuilder();
        
        report.append("=== Retry Health Report ===\n");
        report.append(String.format("Generated: %s\n", overview.getTimestamp()));
        report.append(String.format("Total Operations: %d\n", overview.getTotalRetryOperations()));
        report.append(String.format("Healthy: %d, Warning: %d, Critical: %d\n", 
            overview.getHealthyCount(), overview.getWarningCount(), overview.getCriticalCount()));
        report.append(String.format("Active Alerts: %d\n\n", overview.getTotalActiveAlerts()));
        
        // Detail each retry operation
        overview.getSnapshots().forEach((name, snapshot) -> {
            report.append(String.format("Retry: %s\n", name));
            report.append(String.format("  Status: %s\n", snapshot.getStatus()));
            report.append(String.format("  Success Rate: %.1f%%\n", snapshot.getSuccessRate() * 100));
            report.append(String.format("  Avg Attempts: %.1f\n", snapshot.getAverageAttempts()));
            report.append(String.format("  Avg Duration: %.0fms\n", snapshot.getAverageDuration()));
            
            if (!snapshot.getHealthIssues().isEmpty()) {
                report.append("  Issues:\n");
                snapshot.getHealthIssues().forEach(issue -> report.append(String.format("    - %s\n", issue)));
            }
            report.append("\n");
        });
        
        return report.toString();
    }
}