package com.zamaz.telemetry.application.port;

import com.zamaz.telemetry.domain.entity.TelemetryData;

public interface WorkflowNotificationPort {
    void notifyTelemetryReceived(TelemetryData data);
    
    void notifyThresholdExceeded(String deviceId, String metricName, double value, double threshold);
    
    void notifyDataQualityIssue(String deviceId, int qualityScore);
}