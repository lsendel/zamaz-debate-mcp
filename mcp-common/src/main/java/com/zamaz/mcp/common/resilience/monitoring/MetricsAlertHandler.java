package com.zamaz.mcp.common.resilience.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics-based alert handler that publishes alert metrics to monitoring systems.
 */
@Component
@ConditionalOnBean(MeterRegistry.class)
@Slf4j
public class MetricsAlertHandler implements CircuitBreakerMonitoringService.CircuitBreakerAlertHandler {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> alertCounters = new ConcurrentHashMap<>();

    @Autowired
    public MetricsAlertHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleAlert(CircuitBreakerMonitoringService.CircuitBreakerAlert alert) {
        // Increment general alert counter
        getOrCreateCounter("circuit-breaker.alerts.total", 
                          "circuit_breaker", alert.getCircuitBreakerName(),
                          "severity", alert.getSeverity().name(),
                          "type", alert.getType().name())
            .increment();

        // Increment severity-specific counter
        getOrCreateCounter("circuit-breaker.alerts.by-severity",
                          "circuit_breaker", alert.getCircuitBreakerName(),
                          "severity", alert.getSeverity().name())
            .increment();

        // Increment type-specific counter
        getOrCreateCounter("circuit-breaker.alerts.by-type",
                          "circuit_breaker", alert.getCircuitBreakerName(),
                          "type", alert.getType().name())
            .increment();

        // Record context metrics if available
        recordContextMetrics(alert);

        log.debug("Recorded metrics for alert: {} - {}", alert.getType(), alert.getTitle());
    }

    @Override
    public boolean supportsAlertType(CircuitBreakerMonitoringService.AlertType alertType) {
        return true; // Support all alert types for metrics collection
    }

    @Override
    public String getHandlerName() {
        return "MetricsAlertHandler";
    }

    private Counter getOrCreateCounter(String name, String... tags) {
        String key = name + ":" + String.join(":", tags);
        return alertCounters.computeIfAbsent(key, k -> 
            Counter.builder(name)
                .tags(tags)
                .description("Circuit breaker alert counter")
                .register(meterRegistry));
    }

    private void recordContextMetrics(CircuitBreakerMonitoringService.CircuitBreakerAlert alert) {
        Map<String, Object> context = alert.getContext();
        String circuitBreakerName = alert.getCircuitBreakerName();

        // Record specific context metrics based on alert type
        switch (alert.getType()) {
            case HEALTH_DEGRADATION:
                if (context.containsKey("health_score")) {
                    meterRegistry.gauge("circuit-breaker.alert.health-score", 
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName),
                        ((Number) context.get("health_score")).doubleValue() * 100);
                }
                break;

            case HIGH_FAILURE_RATE:
                if (context.containsKey("failure_rate")) {
                    meterRegistry.gauge("circuit-breaker.alert.failure-rate",
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName),
                        ((Number) context.get("failure_rate")).doubleValue() * 100);
                }
                break;

            case HIGH_CALL_REJECTION:
                if (context.containsKey("call_not_permitted_rate")) {
                    meterRegistry.gauge("circuit-breaker.alert.call-rejection-rate",
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName),
                        ((Number) context.get("call_not_permitted_rate")).doubleValue() * 100);
                }
                if (context.containsKey("calls_not_permitted")) {
                    meterRegistry.gauge("circuit-breaker.alert.calls-not-permitted",
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName),
                        ((Number) context.get("calls_not_permitted")).doubleValue());
                }
                break;

            case FALLBACK_FAILURE:
                if (context.containsKey("fallback_failure_rate")) {
                    meterRegistry.gauge("circuit-breaker.alert.fallback-failure-rate",
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName),
                        ((Number) context.get("fallback_failure_rate")).doubleValue() * 100);
                }
                break;

            case SLOW_RESPONSE:
                if (context.containsKey("avg_response_time_ms")) {
                    meterRegistry.gauge("circuit-breaker.alert.response-time",
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName),
                        ((Number) context.get("avg_response_time_ms")).doubleValue());
                }
                break;

            case STATE_CHANGE:
            case CIRCUIT_OPEN:
            case RECOVERY:
                if (context.containsKey("current_state")) {
                    String currentState = (String) context.get("current_state");
                    meterRegistry.gauge("circuit-breaker.alert.state-numeric",
                        io.micrometer.core.instrument.Tags.of("circuit_breaker", circuitBreakerName, 
                                                             "state", currentState),
                        mapStateToNumeric(currentState));
                }
                break;
        }
    }

    private double mapStateToNumeric(String state) {
        switch (state) {
            case "CLOSED": return 0.0;
            case "HALF_OPEN": return 1.0;
            case "OPEN": return 2.0;
            default: return -1.0;
        }
    }
}