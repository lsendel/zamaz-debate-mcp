package com.zamaz.mcp.common.resilience.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple logging-based alert handler for circuit breaker monitoring.
 */
@Component
@Slf4j
public class LoggingAlertHandler implements CircuitBreakerMonitoringService.CircuitBreakerAlertHandler {

    @Override
    public void handleAlert(CircuitBreakerMonitoringService.CircuitBreakerAlert alert) {
        String logMessage = formatAlert(alert);
        
        switch (alert.getSeverity()) {
            case CRITICAL:
                log.error("🚨 CRITICAL ALERT: {}", logMessage);
                break;
            case WARNING:
                log.warn("⚠️ WARNING ALERT: {}", logMessage);
                break;
            case INFO:
                log.info("ℹ️ INFO ALERT: {}", logMessage);
                break;
        }
    }

    @Override
    public boolean supportsAlertType(CircuitBreakerMonitoringService.AlertType alertType) {
        return true; // Support all alert types
    }

    @Override
    public String getHandlerName() {
        return "LoggingAlertHandler";
    }

    private String formatAlert(CircuitBreakerMonitoringService.CircuitBreakerAlert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("CircuitBreaker='").append(alert.getCircuitBreakerName()).append("', ");
        sb.append("Type=").append(alert.getType()).append(", ");
        sb.append("Title='").append(alert.getTitle()).append("', ");
        sb.append("Description='").append(alert.getDescription()).append("', ");
        sb.append("Timestamp=").append(alert.getTimestamp());
        
        if (!alert.getContext().isEmpty()) {
            sb.append(", Context=").append(alert.getContext());
        }
        
        return sb.toString();
    }
}