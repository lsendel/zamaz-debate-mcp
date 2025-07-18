package com.zamaz.mcp.common.resilience.monitoring;

import com.zamaz.mcp.common.resilience.metrics.CircuitBreakerMetricsCollector;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive monitoring and alerting service for circuit breakers.
 * Provides real-time monitoring, threshold-based alerting, and health assessment.
 */
@Service
@Slf4j
@ConditionalOnProperty(value = "resilience.circuit-breaker.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class CircuitBreakerMonitoringService {

    private final CircuitBreakerMetricsCollector metricsCollector;
    private final List<CircuitBreakerAlertHandler> alertHandlers;
    private final Map<String, CircuitBreakerHealthStatus> healthStatusMap;
    private final Map<String, AlertThresholds> thresholdsMap;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean monitoringActive;
    private final MonitoringConfiguration config;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public CircuitBreakerMonitoringService(CircuitBreakerMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.alertHandlers = new ArrayList<>();
        this.healthStatusMap = new ConcurrentHashMap<>();
        this.thresholdsMap = new ConcurrentHashMap<>();
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "CircuitBreakerMonitoring");
            t.setDaemon(true);
            return t;
        });
        this.monitoringActive = new AtomicBoolean(false);
        this.config = new MonitoringConfiguration();
    }

    /**
     * Configuration for circuit breaker monitoring.
     */
    public static class MonitoringConfiguration {
        private Duration monitoringInterval = Duration.ofSeconds(30);
        private Duration alertCooldownPeriod = Duration.ofMinutes(5);
        private boolean enableRealTimeAlerting = true;
        private boolean enableHealthScoreCalculation = true;
        private boolean enableTrendAnalysis = true;
        private int maxAlertHistory = 100;

        // Getters and setters
        public Duration getMonitoringInterval() { return monitoringInterval; }
        public void setMonitoringInterval(Duration monitoringInterval) { this.monitoringInterval = monitoringInterval; }

        public Duration getAlertCooldownPeriod() { return alertCooldownPeriod; }
        public void setAlertCooldownPeriod(Duration alertCooldownPeriod) { this.alertCooldownPeriod = alertCooldownPeriod; }

        public boolean isEnableRealTimeAlerting() { return enableRealTimeAlerting; }
        public void setEnableRealTimeAlerting(boolean enableRealTimeAlerting) { this.enableRealTimeAlerting = enableRealTimeAlerting; }

        public boolean isEnableHealthScoreCalculation() { return enableHealthScoreCalculation; }
        public void setEnableHealthScoreCalculation(boolean enableHealthScoreCalculation) { this.enableHealthScoreCalculation = enableHealthScoreCalculation; }

        public boolean isEnableTrendAnalysis() { return enableTrendAnalysis; }
        public void setEnableTrendAnalysis(boolean enableTrendAnalysis) { this.enableTrendAnalysis = enableTrendAnalysis; }

        public int getMaxAlertHistory() { return maxAlertHistory; }
        public void setMaxAlertHistory(int maxAlertHistory) { this.maxAlertHistory = maxAlertHistory; }
    }

    /**
     * Alert thresholds for circuit breaker monitoring.
     */
    public static class AlertThresholds {
        private double healthScoreWarning = 0.7;
        private double healthScoreCritical = 0.5;
        private double failureRateWarning = 0.1;
        private double failureRateCritical = 0.2;
        private double callNotPermittedRateWarning = 0.05;
        private double callNotPermittedRateCritical = 0.15;
        private double fallbackFailureRateWarning = 0.2;
        private double fallbackFailureRateCritical = 0.5;
        private Duration responseTimeWarning = Duration.ofMillis(1000);
        private Duration responseTimeCritical = Duration.ofMillis(3000);

        // Getters and setters
        public double getHealthScoreWarning() { return healthScoreWarning; }
        public void setHealthScoreWarning(double healthScoreWarning) { this.healthScoreWarning = healthScoreWarning; }

        public double getHealthScoreCritical() { return healthScoreCritical; }
        public void setHealthScoreCritical(double healthScoreCritical) { this.healthScoreCritical = healthScoreCritical; }

        public double getFailureRateWarning() { return failureRateWarning; }
        public void setFailureRateWarning(double failureRateWarning) { this.failureRateWarning = failureRateWarning; }

        public double getFailureRateCritical() { return failureRateCritical; }
        public void setFailureRateCritical(double failureRateCritical) { this.failureRateCritical = failureRateCritical; }

        public double getCallNotPermittedRateWarning() { return callNotPermittedRateWarning; }
        public void setCallNotPermittedRateWarning(double callNotPermittedRateWarning) { this.callNotPermittedRateWarning = callNotPermittedRateWarning; }

        public double getCallNotPermittedRateCritical() { return callNotPermittedRateCritical; }
        public void setCallNotPermittedRateCritical(double callNotPermittedRateCritical) { this.callNotPermittedRateCritical = callNotPermittedRateCritical; }

        public double getFallbackFailureRateWarning() { return fallbackFailureRateWarning; }
        public void setFallbackFailureRateWarning(double fallbackFailureRateWarning) { this.fallbackFailureRateWarning = fallbackFailureRateWarning; }

        public double getFallbackFailureRateCritical() { return fallbackFailureRateCritical; }
        public void setFallbackFailureRateCritical(double fallbackFailureRateCritical) { this.fallbackFailureRateCritical = fallbackFailureRateCritical; }

        public Duration getResponseTimeWarning() { return responseTimeWarning; }
        public void setResponseTimeWarning(Duration responseTimeWarning) { this.responseTimeWarning = responseTimeWarning; }

        public Duration getResponseTimeCritical() { return responseTimeCritical; }
        public void setResponseTimeCritical(Duration responseTimeCritical) { this.responseTimeCritical = responseTimeCritical; }
    }

    /**
     * Health status for a circuit breaker.
     */
    public static class CircuitBreakerHealthStatus {
        private final String circuitBreakerName;
        private HealthLevel currentHealthLevel;
        private double currentHealthScore;
        private Instant lastAssessment;
        private List<CircuitBreakerAlert> alertHistory;
        private Map<String, Object> diagnostics;
        private TrendAnalysis trend;

        public CircuitBreakerHealthStatus(String circuitBreakerName) {
            this.circuitBreakerName = circuitBreakerName;
            this.currentHealthLevel = HealthLevel.HEALTHY;
            this.currentHealthScore = 1.0;
            this.lastAssessment = Instant.now();
            this.alertHistory = new ArrayList<>();
            this.diagnostics = new HashMap<>();
            this.trend = new TrendAnalysis();
        }

        // Getters and setters
        public String getCircuitBreakerName() { return circuitBreakerName; }
        public HealthLevel getCurrentHealthLevel() { return currentHealthLevel; }
        public void setCurrentHealthLevel(HealthLevel currentHealthLevel) { this.currentHealthLevel = currentHealthLevel; }
        public double getCurrentHealthScore() { return currentHealthScore; }
        public void setCurrentHealthScore(double currentHealthScore) { this.currentHealthScore = currentHealthScore; }
        public Instant getLastAssessment() { return lastAssessment; }
        public void setLastAssessment(Instant lastAssessment) { this.lastAssessment = lastAssessment; }
        public List<CircuitBreakerAlert> getAlertHistory() { return alertHistory; }
        public Map<String, Object> getDiagnostics() { return diagnostics; }
        public TrendAnalysis getTrend() { return trend; }
    }

    /**
     * Health levels for circuit breakers.
     */
    public enum HealthLevel {
        HEALTHY, WARNING, CRITICAL, UNKNOWN
    }

    /**
     * Trend analysis for circuit breaker performance.
     */
    public static class TrendAnalysis {
        private TrendDirection healthTrend = TrendDirection.STABLE;
        private TrendDirection performanceTrend = TrendDirection.STABLE;
        private double healthChangePercent = 0.0;
        private double performanceChangePercent = 0.0;
        private Instant lastUpdate = Instant.now();

        // Getters and setters
        public TrendDirection getHealthTrend() { return healthTrend; }
        public void setHealthTrend(TrendDirection healthTrend) { this.healthTrend = healthTrend; }
        public TrendDirection getPerformanceTrend() { return performanceTrend; }
        public void setPerformanceTrend(TrendDirection performanceTrend) { this.performanceTrend = performanceTrend; }
        public double getHealthChangePercent() { return healthChangePercent; }
        public void setHealthChangePercent(double healthChangePercent) { this.healthChangePercent = healthChangePercent; }
        public double getPerformanceChangePercent() { return performanceChangePercent; }
        public void setPerformanceChangePercent(double performanceChangePercent) { this.performanceChangePercent = performanceChangePercent; }
        public Instant getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }
    }

    /**
     * Trend directions.
     */
    public enum TrendDirection {
        IMPROVING, STABLE, DECLINING
    }

    /**
     * Circuit breaker alert.
     */
    public static class CircuitBreakerAlert {
        private final String circuitBreakerName;
        private final AlertSeverity severity;
        private final AlertType type;
        private final String title;
        private final String description;
        private final Instant timestamp;
        private final Map<String, Object> context;
        private boolean acknowledged;
        private Instant acknowledgedAt;
        private String acknowledgedBy;

        public CircuitBreakerAlert(String circuitBreakerName, AlertSeverity severity, AlertType type,
                                 String title, String description, Map<String, Object> context) {
            this.circuitBreakerName = circuitBreakerName;
            this.severity = severity;
            this.type = type;
            this.title = title;
            this.description = description;
            this.timestamp = Instant.now();
            this.context = new HashMap<>(context);
            this.acknowledged = false;
        }

        // Getters and setters
        public String getCircuitBreakerName() { return circuitBreakerName; }
        public AlertSeverity getSeverity() { return severity; }
        public AlertType getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getContext() { return context; }
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
        public Instant getAcknowledgedAt() { return acknowledgedAt; }
        public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
        public String getAcknowledgedBy() { return acknowledgedBy; }
        public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

        public void acknowledge(String acknowledgedBy) {
            this.acknowledged = true;
            this.acknowledgedAt = Instant.now();
            this.acknowledgedBy = acknowledgedBy;
        }
    }

    /**
     * Alert severity levels.
     */
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }

    /**
     * Alert types.
     */
    public enum AlertType {
        HEALTH_DEGRADATION, HIGH_FAILURE_RATE, CIRCUIT_OPEN, HIGH_CALL_REJECTION, 
        FALLBACK_FAILURE, SLOW_RESPONSE, STATE_CHANGE, RECOVERY
    }

    /**
     * Interface for handling circuit breaker alerts.
     */
    public interface CircuitBreakerAlertHandler {
        void handleAlert(CircuitBreakerAlert alert);
        boolean supportsAlertType(AlertType alertType);
        String getHandlerName();
    }

    @PostConstruct
    public void startMonitoring() {
        if (monitoringActive.compareAndSet(false, true)) {
            log.info("Starting circuit breaker monitoring service");
            
            // Schedule periodic monitoring
            scheduledExecutor.scheduleAtFixedRate(
                this::performMonitoringCheck,
                0,
                config.getMonitoringInterval().toSeconds(),
                TimeUnit.SECONDS
            );

            // Schedule health assessment
            scheduledExecutor.scheduleAtFixedRate(
                this::performHealthAssessment,
                30, // Start after 30 seconds
                60, // Run every minute
                TimeUnit.SECONDS
            );

            log.info("Circuit breaker monitoring service started with interval: {}", 
                    config.getMonitoringInterval());
        }
    }

    @PreDestroy
    public void stopMonitoring() {
        if (monitoringActive.compareAndSet(true, false)) {
            log.info("Stopping circuit breaker monitoring service");
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Performs monitoring check for all circuit breakers.
     */
    @Scheduled(fixedRateString = "${resilience.circuit-breaker.monitoring.interval:30000}")
    protected void performMonitoringCheck() {
        if (!monitoringActive.get()) {
            return;
        }

        try {
            Map<String, CircuitBreakerMetricsCollector.CircuitBreakerStats> allStats = 
                metricsCollector.getAllCircuitBreakerStats();

            for (Map.Entry<String, CircuitBreakerMetricsCollector.CircuitBreakerStats> entry : allStats.entrySet()) {
                String circuitBreakerName = entry.getKey();
                CircuitBreakerMetricsCollector.CircuitBreakerStats stats = entry.getValue();

                checkThresholds(circuitBreakerName, stats);
                updateHealthStatus(circuitBreakerName, stats);
            }

            log.debug("Completed monitoring check for {} circuit breakers", allStats.size());

        } catch (Exception e) {
            log.error("Error during monitoring check", e);
        }
    }

    /**
     * Performs health assessment and trend analysis.
     */
    protected void performHealthAssessment() {
        if (!config.isEnableHealthScoreCalculation()) {
            return;
        }

        try {
            for (CircuitBreakerHealthStatus healthStatus : healthStatusMap.values()) {
                updateTrendAnalysis(healthStatus);
                updateDiagnostics(healthStatus);
            }

            log.debug("Completed health assessment for {} circuit breakers", healthStatusMap.size());

        } catch (Exception e) {
            log.error("Error during health assessment", e);
        }
    }

    /**
     * Checks thresholds and generates alerts if necessary.
     */
    private void checkThresholds(String circuitBreakerName, 
                               CircuitBreakerMetricsCollector.CircuitBreakerStats stats) {
        AlertThresholds thresholds = getThresholds(circuitBreakerName);

        // Check health score
        checkHealthScoreThreshold(circuitBreakerName, stats, thresholds);

        // Check failure rate
        checkFailureRateThreshold(circuitBreakerName, stats, thresholds);

        // Check call not permitted rate
        checkCallNotPermittedThreshold(circuitBreakerName, stats, thresholds);

        // Check fallback failure rate
        checkFallbackThreshold(circuitBreakerName, stats, thresholds);

        // Check response time
        checkResponseTimeThreshold(circuitBreakerName, stats, thresholds);

        // Check circuit state
        checkCircuitStateChanges(circuitBreakerName, stats);
    }

    private void checkHealthScoreThreshold(String circuitBreakerName,
                                         CircuitBreakerMetricsCollector.CircuitBreakerStats stats,
                                         AlertThresholds thresholds) {
        double healthScore = stats.getHealthScore();

        if (healthScore < thresholds.getHealthScoreCritical()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.CRITICAL, AlertType.HEALTH_DEGRADATION,
                "Critical Health Score", 
                String.format("Circuit breaker health score %.1f%% is critically low (threshold: %.1f%%)",
                    healthScore * 100, thresholds.getHealthScoreCritical() * 100),
                Map.of("health_score", healthScore, "threshold", thresholds.getHealthScoreCritical()));

        } else if (healthScore < thresholds.getHealthScoreWarning()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.WARNING, AlertType.HEALTH_DEGRADATION,
                "Low Health Score",
                String.format("Circuit breaker health score %.1f%% is below warning threshold (%.1f%%)",
                    healthScore * 100, thresholds.getHealthScoreWarning() * 100),
                Map.of("health_score", healthScore, "threshold", thresholds.getHealthScoreWarning()));
        }
    }

    private void checkFailureRateThreshold(String circuitBreakerName,
                                         CircuitBreakerMetricsCollector.CircuitBreakerStats stats,
                                         AlertThresholds thresholds) {
        double failureRate = stats.getFailureRate();

        if (failureRate > thresholds.getFailureRateCritical()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.CRITICAL, AlertType.HIGH_FAILURE_RATE,
                "Critical Failure Rate",
                String.format("Circuit breaker failure rate %.1f%% exceeds critical threshold (%.1f%%)",
                    failureRate * 100, thresholds.getFailureRateCritical() * 100),
                Map.of("failure_rate", failureRate, "threshold", thresholds.getFailureRateCritical(),
                       "failed_executions", stats.getFailedExecutions(), "total_executions", stats.getTotalExecutions()));

        } else if (failureRate > thresholds.getFailureRateWarning()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.WARNING, AlertType.HIGH_FAILURE_RATE,
                "High Failure Rate",
                String.format("Circuit breaker failure rate %.1f%% exceeds warning threshold (%.1f%%)",
                    failureRate * 100, thresholds.getFailureRateWarning() * 100),
                Map.of("failure_rate", failureRate, "threshold", thresholds.getFailureRateWarning()));
        }
    }

    private void checkCallNotPermittedThreshold(String circuitBreakerName,
                                              CircuitBreakerMetricsCollector.CircuitBreakerStats stats,
                                              AlertThresholds thresholds) {
        double callNotPermittedRate = stats.getCallNotPermittedRate();

        if (callNotPermittedRate > thresholds.getCallNotPermittedRateCritical()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.CRITICAL, AlertType.HIGH_CALL_REJECTION,
                "Critical Call Rejection Rate",
                String.format("Circuit breaker is rejecting %.1f%% of calls (critical threshold: %.1f%%)",
                    callNotPermittedRate * 100, thresholds.getCallNotPermittedRateCritical() * 100),
                Map.of("call_not_permitted_rate", callNotPermittedRate, 
                       "threshold", thresholds.getCallNotPermittedRateCritical(),
                       "calls_not_permitted", stats.getCallsNotPermitted()));

        } else if (callNotPermittedRate > thresholds.getCallNotPermittedRateWarning()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.WARNING, AlertType.HIGH_CALL_REJECTION,
                "High Call Rejection Rate",
                String.format("Circuit breaker is rejecting %.1f%% of calls (warning threshold: %.1f%%)",
                    callNotPermittedRate * 100, thresholds.getCallNotPermittedRateWarning() * 100),
                Map.of("call_not_permitted_rate", callNotPermittedRate, 
                       "threshold", thresholds.getCallNotPermittedRateWarning()));
        }
    }

    private void checkFallbackThreshold(String circuitBreakerName,
                                      CircuitBreakerMetricsCollector.CircuitBreakerStats stats,
                                      AlertThresholds thresholds) {
        if (stats.getFallbackExecutions() == 0) {
            return; // No fallbacks to check
        }

        double fallbackFailureRate = 1.0 - stats.getFallbackSuccessRate();

        if (fallbackFailureRate > thresholds.getFallbackFailureRateCritical()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.CRITICAL, AlertType.FALLBACK_FAILURE,
                "Critical Fallback Failure Rate",
                String.format("Fallback failure rate %.1f%% exceeds critical threshold (%.1f%%)",
                    fallbackFailureRate * 100, thresholds.getFallbackFailureRateCritical() * 100),
                Map.of("fallback_failure_rate", fallbackFailureRate, 
                       "threshold", thresholds.getFallbackFailureRateCritical(),
                       "fallback_executions", stats.getFallbackExecutions(),
                       "successful_fallbacks", stats.getSuccessfulFallbacks()));

        } else if (fallbackFailureRate > thresholds.getFallbackFailureRateWarning()) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.WARNING, AlertType.FALLBACK_FAILURE,
                "High Fallback Failure Rate",
                String.format("Fallback failure rate %.1f%% exceeds warning threshold (%.1f%%)",
                    fallbackFailureRate * 100, thresholds.getFallbackFailureRateWarning() * 100),
                Map.of("fallback_failure_rate", fallbackFailureRate, 
                       "threshold", thresholds.getFallbackFailureRateWarning()));
        }
    }

    private void checkResponseTimeThreshold(String circuitBreakerName,
                                          CircuitBreakerMetricsCollector.CircuitBreakerStats stats,
                                          AlertThresholds thresholds) {
        double avgResponseTime = stats.getAverageExecutionTimeMs();
        Duration responseTime = Duration.ofMillis((long) avgResponseTime);

        if (responseTime.compareTo(thresholds.getResponseTimeCritical()) > 0) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.CRITICAL, AlertType.SLOW_RESPONSE,
                "Critical Response Time",
                String.format("Average response time %dms exceeds critical threshold (%dms)",
                    responseTime.toMillis(), thresholds.getResponseTimeCritical().toMillis()),
                Map.of("avg_response_time_ms", avgResponseTime, 
                       "threshold_ms", thresholds.getResponseTimeCritical().toMillis()));

        } else if (responseTime.compareTo(thresholds.getResponseTimeWarning()) > 0) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.WARNING, AlertType.SLOW_RESPONSE,
                "Slow Response Time",
                String.format("Average response time %dms exceeds warning threshold (%dms)",
                    responseTime.toMillis(), thresholds.getResponseTimeWarning().toMillis()),
                Map.of("avg_response_time_ms", avgResponseTime, 
                       "threshold_ms", thresholds.getResponseTimeWarning().toMillis()));
        }
    }

    private void checkCircuitStateChanges(String circuitBreakerName,
                                        CircuitBreakerMetricsCollector.CircuitBreakerStats stats) {
        CircuitBreaker.State currentState = stats.getCurrentState();

        if (currentState == CircuitBreaker.State.OPEN) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.CRITICAL, AlertType.CIRCUIT_OPEN,
                "Circuit Breaker Open",
                "Circuit breaker is in OPEN state, blocking all calls",
                Map.of("current_state", currentState.name(), 
                       "previous_state", stats.getPreviousState().name(),
                       "state_changes", stats.getStateChanges()));

        } else if (currentState == CircuitBreaker.State.HALF_OPEN) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.WARNING, AlertType.STATE_CHANGE,
                "Circuit Breaker Half-Open",
                "Circuit breaker is in HALF_OPEN state, testing recovery",
                Map.of("current_state", currentState.name(), 
                       "previous_state", stats.getPreviousState().name()));

        } else if (currentState == CircuitBreaker.State.CLOSED && 
                   stats.getPreviousState() != CircuitBreaker.State.CLOSED) {
            createAndHandleAlert(circuitBreakerName, AlertSeverity.INFO, AlertType.RECOVERY,
                "Circuit Breaker Recovery",
                "Circuit breaker has recovered and is now CLOSED",
                Map.of("current_state", currentState.name(), 
                       "previous_state", stats.getPreviousState().name()));
        }
    }

    private void createAndHandleAlert(String circuitBreakerName, AlertSeverity severity, AlertType type,
                                    String title, String description, Map<String, Object> context) {
        if (!config.isEnableRealTimeAlerting()) {
            return;
        }

        // Check cooldown period
        if (isInCooldownPeriod(circuitBreakerName, type)) {
            return;
        }

        CircuitBreakerAlert alert = new CircuitBreakerAlert(circuitBreakerName, severity, type, 
                                                           title, description, context);

        // Add to health status
        CircuitBreakerHealthStatus healthStatus = healthStatusMap.computeIfAbsent(circuitBreakerName,
            k -> new CircuitBreakerHealthStatus(circuitBreakerName));
        
        healthStatus.getAlertHistory().add(alert);
        
        // Maintain alert history size
        if (healthStatus.getAlertHistory().size() > config.getMaxAlertHistory()) {
            healthStatus.getAlertHistory().remove(0);
        }

        // Handle alert
        for (CircuitBreakerAlertHandler handler : alertHandlers) {
            if (handler.supportsAlertType(type)) {
                try {
                    handler.handleAlert(alert);
                } catch (Exception e) {
                    log.error("Error handling alert with handler: {}", handler.getHandlerName(), e);
                }
            }
        }

        log.info("Generated {} alert for circuit breaker '{}': {}", 
                severity, circuitBreakerName, title);
    }

    private boolean isInCooldownPeriod(String circuitBreakerName, AlertType alertType) {
        CircuitBreakerHealthStatus healthStatus = healthStatusMap.get(circuitBreakerName);
        if (healthStatus == null) {
            return false;
        }

        Instant cooldownThreshold = Instant.now().minus(config.getAlertCooldownPeriod());
        
        return healthStatus.getAlertHistory().stream()
            .filter(alert -> alert.getType() == alertType)
            .anyMatch(alert -> alert.getTimestamp().isAfter(cooldownThreshold));
    }

    private void updateHealthStatus(String circuitBreakerName,
                                  CircuitBreakerMetricsCollector.CircuitBreakerStats stats) {
        CircuitBreakerHealthStatus healthStatus = healthStatusMap.computeIfAbsent(circuitBreakerName,
            k -> new CircuitBreakerHealthStatus(circuitBreakerName));

        // Update health score
        double healthScore = stats.getHealthScore();
        healthStatus.setCurrentHealthScore(healthScore);

        // Update health level
        HealthLevel newHealthLevel = calculateHealthLevel(healthScore, stats);
        healthStatus.setCurrentHealthLevel(newHealthLevel);

        // Update last assessment time
        healthStatus.setLastAssessment(Instant.now());
    }

    private HealthLevel calculateHealthLevel(double healthScore,
                                           CircuitBreakerMetricsCollector.CircuitBreakerStats stats) {
        AlertThresholds thresholds = getThresholds(stats.toString()); // Using a default key

        if (stats.getCurrentState() == CircuitBreaker.State.OPEN) {
            return HealthLevel.CRITICAL;
        }

        if (healthScore < thresholds.getHealthScoreCritical()) {
            return HealthLevel.CRITICAL;
        } else if (healthScore < thresholds.getHealthScoreWarning()) {
            return HealthLevel.WARNING;
        } else if (stats.getTotalExecutions() == 0) {
            return HealthLevel.UNKNOWN;
        } else {
            return HealthLevel.HEALTHY;
        }
    }

    private void updateTrendAnalysis(CircuitBreakerHealthStatus healthStatus) {
        if (!config.isEnableTrendAnalysis()) {
            return;
        }

        // Implementation would analyze historical data to determine trends
        // This is a simplified version
        TrendAnalysis trend = healthStatus.getTrend();
        trend.setLastUpdate(Instant.now());
        
        // In a full implementation, this would analyze historical health scores
        // and performance metrics to determine trend direction and change percentages
    }

    private void updateDiagnostics(CircuitBreakerHealthStatus healthStatus) {
        Map<String, Object> diagnostics = healthStatus.getDiagnostics();
        
        // Update diagnostic information
        diagnostics.put("last_assessment", healthStatus.getLastAssessment());
        diagnostics.put("health_level", healthStatus.getCurrentHealthLevel());
        diagnostics.put("health_score", healthStatus.getCurrentHealthScore());
        diagnostics.put("alert_count_last_hour", countRecentAlerts(healthStatus, Duration.ofHours(1)));
        diagnostics.put("alert_count_last_day", countRecentAlerts(healthStatus, Duration.ofDays(1)));
    }

    private long countRecentAlerts(CircuitBreakerHealthStatus healthStatus, Duration period) {
        Instant threshold = Instant.now().minus(period);
        return healthStatus.getAlertHistory().stream()
            .filter(alert -> alert.getTimestamp().isAfter(threshold))
            .count();
    }

    // Public API methods

    public void addAlertHandler(CircuitBreakerAlertHandler handler) {
        alertHandlers.add(handler);
        log.info("Added alert handler: {}", handler.getHandlerName());
    }

    public void removeAlertHandler(CircuitBreakerAlertHandler handler) {
        alertHandlers.remove(handler);
        log.info("Removed alert handler: {}", handler.getHandlerName());
    }

    public void setThresholds(String circuitBreakerName, AlertThresholds thresholds) {
        thresholdsMap.put(circuitBreakerName, thresholds);
        log.info("Updated thresholds for circuit breaker: {}", circuitBreakerName);
    }

    public AlertThresholds getThresholds(String circuitBreakerName) {
        return thresholdsMap.getOrDefault(circuitBreakerName, new AlertThresholds());
    }

    public CircuitBreakerHealthStatus getHealthStatus(String circuitBreakerName) {
        return healthStatusMap.get(circuitBreakerName);
    }

    public Map<String, CircuitBreakerHealthStatus> getAllHealthStatuses() {
        return new HashMap<>(healthStatusMap);
    }

    public List<CircuitBreakerAlert> getRecentAlerts(Duration period) {
        Instant threshold = Instant.now().minus(period);
        return healthStatusMap.values().stream()
            .flatMap(status -> status.getAlertHistory().stream())
            .filter(alert -> alert.getTimestamp().isAfter(threshold))
            .sorted(Comparator.comparing(CircuitBreakerAlert::getTimestamp).reversed())
            .toList();
    }

    public void acknowledgeAlert(String circuitBreakerName, Instant alertTimestamp, String acknowledgedBy) {
        CircuitBreakerHealthStatus healthStatus = healthStatusMap.get(circuitBreakerName);
        if (healthStatus != null) {
            healthStatus.getAlertHistory().stream()
                .filter(alert -> alert.getTimestamp().equals(alertTimestamp))
                .findFirst()
                .ifPresent(alert -> alert.acknowledge(acknowledgedBy));
        }
    }

    public MonitoringConfiguration getConfiguration() {
        return config;
    }

    public boolean isMonitoringActive() {
        return monitoringActive.get();
    }
}