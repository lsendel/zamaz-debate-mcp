package com.zamaz.mcp.common.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit statistics data transfer object
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditStatistics {
    
    private long totalEvents;
    private long successfulEvents;
    private long failedEvents;
    private long highRiskEvents;
    private long uniqueUsers;
    private long uniqueIps;
    
    /**
     * Calculate success rate
     */
    public double getSuccessRate() {
        if (totalEvents == 0) {
            return 0.0;
        }
        return (double) successfulEvents / totalEvents * 100;
    }
    
    /**
     * Calculate failure rate
     */
    public double getFailureRate() {
        if (totalEvents == 0) {
            return 0.0;
        }
        return (double) failedEvents / totalEvents * 100;
    }
    
    /**
     * Calculate risk ratio
     */
    public double getRiskRatio() {
        if (totalEvents == 0) {
            return 0.0;
        }
        return (double) highRiskEvents / totalEvents * 100;
    }
}