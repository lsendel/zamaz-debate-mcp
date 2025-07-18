package com.zamaz.mcp.common.resilience.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Overall health overview of all retry operations in the system.
 */
@Data
@Builder
public class RetryHealthOverview {
    
    private Instant timestamp;
    private int totalRetryOperations;
    private long healthyCount;
    private long warningCount;
    private long criticalCount;
    private int totalActiveAlerts;
    private Map<String, RetryHealthSnapshot> snapshots;
    private List<RetryAlert> recentAlerts;
    
    /**
     * Calculates the overall system health percentage.
     */
    public double getOverallHealthPercentage() {
        if (totalRetryOperations == 0) {
            return 100.0;
        }
        
        return (double) healthyCount / totalRetryOperations * 100.0;
    }
    
    /**
     * Gets the system-wide health status.
     */
    public RetryHealthStatus getSystemHealthStatus() {
        if (totalRetryOperations == 0) {
            return RetryHealthStatus.UNKNOWN;
        }
        
        double healthPercentage = getOverallHealthPercentage();
        
        if (criticalCount > 0) {
            return RetryHealthStatus.CRITICAL;
        } else if (warningCount > 0 || healthPercentage < 80.0) {
            return RetryHealthStatus.WARNING;
        } else {
            return RetryHealthStatus.HEALTHY;
        }
    }
    
    /**
     * Gets count of alerts by severity.
     */
    public Map<AlertSeverity, Long> getAlertCountsBySeverity() {
        return recentAlerts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                RetryAlert::getSeverity,
                java.util.stream.Collectors.counting()));
    }
    
    /**
     * Checks if the system has any high-priority issues.
     */
    public boolean hasHighPriorityIssues() {
        return criticalCount > 0 || 
               recentAlerts.stream().anyMatch(alert -> 
                   alert.getSeverity() == AlertSeverity.CRITICAL || 
                   alert.getSeverity() == AlertSeverity.HIGH);
    }
    
    /**
     * Gets a summary of the most problematic retry operations.
     */
    public List<String> getMostProblematicRetries() {
        return snapshots.entrySet().stream()
            .filter(entry -> entry.getValue().getStatus() == RetryHealthStatus.CRITICAL)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Generates a compact health summary.
     */
    public String getCompactSummary() {
        return String.format("Health: %.1f%% (%d/%d healthy, %d alerts)", 
            getOverallHealthPercentage(),
            healthyCount, 
            totalRetryOperations,
            totalActiveAlerts);
    }
}