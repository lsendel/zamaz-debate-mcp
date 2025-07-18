package com.zamaz.mcp.common.resilience.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents an alert for retry operation issues.
 */
@Data
@Builder
public class RetryAlert {
    
    private String id;
    private String retryName;
    private RetryAlertType type;
    private AlertSeverity severity;
    private String message;
    private Instant timestamp;
    private String details;
    
    /**
     * Gets a formatted alert message for display.
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s: %s", severity, retryName, message);
    }
    
    /**
     * Checks if this alert is recent (within the last hour).
     */
    public boolean isRecent() {
        return timestamp.isAfter(Instant.now().minusSeconds(3600));
    }
    
    /**
     * Gets age of the alert in minutes.
     */
    public long getAgeMinutes() {
        return (Instant.now().toEpochMilli() - timestamp.toEpochMilli()) / 60000;
    }
}

/**
 * Types of retry alerts.
 */
enum RetryAlertType {
    STATUS_DEGRADATION("Retry operation status has degraded"),
    SUCCESS_RATE_DROP("Success rate has dropped significantly"),
    LOW_SUCCESS_RATE("Success rate is below threshold"),
    HIGH_RETRY_ATTEMPTS("Average retry attempts exceed threshold"),
    HIGH_DURATION("Average execution duration exceeds threshold"),
    CONSECUTIVE_FAILURES("Multiple consecutive failures detected"),
    RATE_SPIKE("Unusual increase in retry attempts"),
    TIMEOUT_INCREASE("Increase in timeout-related failures");
    
    private final String description;
    
    RetryAlertType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

/**
 * Alert severity levels.
 */
enum AlertSeverity {
    LOW(1, "📗"),
    MEDIUM(2, "📙"), 
    HIGH(3, "📕"),
    CRITICAL(4, "🚨");
    
    private final int level;
    private final String emoji;
    
    AlertSeverity(int level, String emoji) {
        this.level = level;
        this.emoji = emoji;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public boolean isHigherThan(AlertSeverity other) {
        return this.level > other.level;
    }
}