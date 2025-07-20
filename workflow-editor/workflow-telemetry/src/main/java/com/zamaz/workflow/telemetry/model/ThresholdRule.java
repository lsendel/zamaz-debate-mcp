package com.zamaz.workflow.telemetry.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
public class ThresholdRule {
    private final String id;
    private final String deviceId;
    private final String metric;
    private final String operator;
    private final Double threshold;
    private final Duration window;
    private final String severity;
    private final String description;
    private final boolean enabled;
    
    public boolean matches(TelemetryData data) {
        return enabled && 
               (deviceId == null || deviceId.equals(data.getDeviceId())) &&
               data.getMetrics().containsKey(metric);
    }
    
    public boolean isViolated(TelemetryData data) {
        Object metricValue = data.getMetrics().get(metric);
        if (!(metricValue instanceof Number)) {
            return false;
        }
        
        double value = ((Number) metricValue).doubleValue();
        
        switch (operator) {
            case ">": return value > threshold;
            case "<": return value < threshold;
            case ">=": return value >= threshold;
            case "<=": return value <= threshold;
            case "=": return Math.abs(value - threshold) < 0.001;
            case "!=": return Math.abs(value - threshold) >= 0.001;
            default: return false;
        }
    }
}