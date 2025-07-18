package com.zamaz.mcp.common.resilience.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Represents a point-in-time snapshot of retry operation health.
 */
@Data
@Builder
public class RetryHealthSnapshot {
    
    private String retryName;
    private Instant timestamp;
    private RetryHealthStatus status;
    private double successRate;
    private double averageAttempts;
    private double averageDuration;
    private long totalExecutions;
    private List<String> recentErrors;
    private List<String> healthIssues;
    
    /**
     * Calculates a health score from 0.0 (critical) to 1.0 (perfect).
     */
    public double calculateHealthScore() {
        double successWeight = 0.4;
        double attemptsWeight = 0.3;
        double durationWeight = 0.3;
        
        // Success rate component (0-1)
        double successComponent = successRate;
        
        // Attempts component (inverse normalized, 1.0 = ideal, decreasing with more attempts)
        double attemptsComponent = Math.max(0.0, Math.min(1.0, 2.0 / Math.max(1.0, averageAttempts)));
        
        // Duration component (inverse normalized, assuming 1000ms as ideal)
        double durationComponent = Math.max(0.0, Math.min(1.0, 1000.0 / Math.max(100.0, averageDuration)));
        
        return successWeight * successComponent + 
               attemptsWeight * attemptsComponent + 
               durationWeight * durationComponent;
    }
    
    /**
     * Determines if this snapshot indicates degraded performance compared to another.
     */
    public boolean isHealthDegraded(RetryHealthSnapshot previous) {
        if (previous == null) {
            return false;
        }
        
        return this.calculateHealthScore() < previous.calculateHealthScore() - 0.1;
    }
    
    /**
     * Gets a human-readable summary of the health status.
     */
    public String getHealthSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Status: %s", status));
        
        if (!healthIssues.isEmpty()) {
            summary.append(" (").append(healthIssues.size()).append(" issues)");
        }
        
        return summary.toString();
    }
}

/**
 * Enumeration of retry health statuses.
 */
enum RetryHealthStatus {
    UNKNOWN(AlertSeverity.LOW),
    HEALTHY(AlertSeverity.LOW),
    WARNING(AlertSeverity.MEDIUM), 
    CRITICAL(AlertSeverity.HIGH);
    
    private final AlertSeverity severity;
    
    RetryHealthStatus(AlertSeverity severity) {
        this.severity = severity;
    }
    
    public AlertSeverity getSeverity() {
        return severity;
    }
}