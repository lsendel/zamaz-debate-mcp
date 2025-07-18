package com.zamaz.mcp.common.resilience.monitoring;

import com.zamaz.mcp.common.resilience.metrics.CircuitBreakerMetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified resilience monitoring dashboard that provides comprehensive insights
 * into circuit breaker health, performance, and trends.
 */
@Service
@Slf4j
@ConditionalOnProperty(value = "resilience.monitoring.dashboard.enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceMonitoringDashboard {

    private final CircuitBreakerMetricsCollector metricsCollector;
    private final CircuitBreakerMonitoringService monitoringService;
    private final DashboardConfiguration configuration;

    @Autowired
    public ResilienceMonitoringDashboard(CircuitBreakerMetricsCollector metricsCollector,
                                       CircuitBreakerMonitoringService monitoringService) {
        this.metricsCollector = metricsCollector;
        this.monitoringService = monitoringService;
        this.configuration = new DashboardConfiguration();
    }

    /**
     * Dashboard configuration.
     */
    public static class DashboardConfiguration {
        private String title = "Resilience Monitoring Dashboard";
        private int maxAlertDisplayCount = 20;
        private Duration defaultTimeRange = Duration.ofHours(24);
        private boolean enableRealTimeUpdates = true;
        private boolean showDetailedMetrics = true;
        private String themeColor = "#2563eb";

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getMaxAlertDisplayCount() { return maxAlertDisplayCount; }
        public void setMaxAlertDisplayCount(int maxAlertDisplayCount) { this.maxAlertDisplayCount = maxAlertDisplayCount; }
        public Duration getDefaultTimeRange() { return defaultTimeRange; }
        public void setDefaultTimeRange(Duration defaultTimeRange) { this.defaultTimeRange = defaultTimeRange; }
        public boolean isEnableRealTimeUpdates() { return enableRealTimeUpdates; }
        public void setEnableRealTimeUpdates(boolean enableRealTimeUpdates) { this.enableRealTimeUpdates = enableRealTimeUpdates; }
        public boolean isShowDetailedMetrics() { return showDetailedMetrics; }
        public void setShowDetailedMetrics(boolean showDetailedMetrics) { this.showDetailedMetrics = showDetailedMetrics; }
        public String getThemeColor() { return themeColor; }
        public void setThemeColor(String themeColor) { this.themeColor = themeColor; }
    }

    /**
     * Dashboard overview data.
     */
    public static class DashboardOverview {
        private final int totalCircuitBreakers;
        private final int healthyCircuitBreakers;
        private final int warningCircuitBreakers;
        private final int criticalCircuitBreakers;
        private final double overallHealthScore;
        private final long totalExecutions;
        private final double overallSuccessRate;
        private final int activeAlerts;
        private final Instant lastUpdate;

        public DashboardOverview(int totalCircuitBreakers, int healthyCircuitBreakers, int warningCircuitBreakers,
                               int criticalCircuitBreakers, double overallHealthScore, long totalExecutions,
                               double overallSuccessRate, int activeAlerts, Instant lastUpdate) {
            this.totalCircuitBreakers = totalCircuitBreakers;
            this.healthyCircuitBreakers = healthyCircuitBreakers;
            this.warningCircuitBreakers = warningCircuitBreakers;
            this.criticalCircuitBreakers = criticalCircuitBreakers;
            this.overallHealthScore = overallHealthScore;
            this.totalExecutions = totalExecutions;
            this.overallSuccessRate = overallSuccessRate;
            this.activeAlerts = activeAlerts;
            this.lastUpdate = lastUpdate;
        }

        // Getters
        public int getTotalCircuitBreakers() { return totalCircuitBreakers; }
        public int getHealthyCircuitBreakers() { return healthyCircuitBreakers; }
        public int getWarningCircuitBreakers() { return warningCircuitBreakers; }
        public int getCriticalCircuitBreakers() { return criticalCircuitBreakers; }
        public double getOverallHealthScore() { return overallHealthScore; }
        public long getTotalExecutions() { return totalExecutions; }
        public double getOverallSuccessRate() { return overallSuccessRate; }
        public int getActiveAlerts() { return activeAlerts; }
        public Instant getLastUpdate() { return lastUpdate; }
    }

    /**
     * Circuit breaker summary for dashboard display.
     */
    public static class CircuitBreakerSummary {
        private final String name;
        private final CircuitBreakerMonitoringService.HealthLevel healthLevel;
        private final double healthScore;
        private final long totalExecutions;
        private final double successRate;
        private final double averageResponseTime;
        private final String currentState;
        private final int recentAlerts;
        private final Instant lastExecution;

        public CircuitBreakerSummary(String name, CircuitBreakerMonitoringService.HealthLevel healthLevel,
                                   double healthScore, long totalExecutions, double successRate,
                                   double averageResponseTime, String currentState, int recentAlerts,
                                   Instant lastExecution) {
            this.name = name;
            this.healthLevel = healthLevel;
            this.healthScore = healthScore;
            this.totalExecutions = totalExecutions;
            this.successRate = successRate;
            this.averageResponseTime = averageResponseTime;
            this.currentState = currentState;
            this.recentAlerts = recentAlerts;
            this.lastExecution = lastExecution;
        }

        // Getters
        public String getName() { return name; }
        public CircuitBreakerMonitoringService.HealthLevel getHealthLevel() { return healthLevel; }
        public double getHealthScore() { return healthScore; }
        public long getTotalExecutions() { return totalExecutions; }
        public double getSuccessRate() { return successRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public String getCurrentState() { return currentState; }
        public int getRecentAlerts() { return recentAlerts; }
        public Instant getLastExecution() { return lastExecution; }
    }

    /**
     * Gets dashboard overview data.
     */
    public DashboardOverview getDashboardOverview() {
        Map<String, CircuitBreakerMetricsCollector.CircuitBreakerStats> allStats = 
            metricsCollector.getAllCircuitBreakerStats();
        Map<String, CircuitBreakerMonitoringService.CircuitBreakerHealthStatus> allHealthStatuses = 
            monitoringService.getAllHealthStatuses();

        int totalCircuitBreakers = allStats.size();
        int healthyCircuitBreakers = 0;
        int warningCircuitBreakers = 0;
        int criticalCircuitBreakers = 0;

        long totalExecutions = 0;
        long totalSuccessfulExecutions = 0;
        double totalHealthScore = 0;

        for (Map.Entry<String, CircuitBreakerMetricsCollector.CircuitBreakerStats> entry : allStats.entrySet()) {
            String name = entry.getKey();
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = entry.getValue();
            CircuitBreakerMonitoringService.CircuitBreakerHealthStatus healthStatus = allHealthStatuses.get(name);

            totalExecutions += stats.getTotalExecutions();
            totalSuccessfulExecutions += stats.getSuccessfulExecutions();
            totalHealthScore += stats.getHealthScore();

            if (healthStatus != null) {
                switch (healthStatus.getCurrentHealthLevel()) {
                    case HEALTHY:
                        healthyCircuitBreakers++;
                        break;
                    case WARNING:
                        warningCircuitBreakers++;
                        break;
                    case CRITICAL:
                        criticalCircuitBreakers++;
                        break;
                }
            } else {
                healthyCircuitBreakers++; // Default to healthy if no health status
            }
        }

        double overallHealthScore = totalCircuitBreakers > 0 ? totalHealthScore / totalCircuitBreakers : 1.0;
        double overallSuccessRate = totalExecutions > 0 ? (double) totalSuccessfulExecutions / totalExecutions : 1.0;

        List<CircuitBreakerMonitoringService.CircuitBreakerAlert> recentAlerts = 
            monitoringService.getRecentAlerts(Duration.ofHours(1));
        int activeAlerts = (int) recentAlerts.stream().filter(alert -> !alert.isAcknowledged()).count();

        return new DashboardOverview(totalCircuitBreakers, healthyCircuitBreakers, warningCircuitBreakers,
                                   criticalCircuitBreakers, overallHealthScore, totalExecutions,
                                   overallSuccessRate, activeAlerts, Instant.now());
    }

    /**
     * Gets circuit breaker summaries for dashboard display.
     */
    public List<CircuitBreakerSummary> getCircuitBreakerSummaries() {
        Map<String, CircuitBreakerMetricsCollector.CircuitBreakerStats> allStats = 
            metricsCollector.getAllCircuitBreakerStats();
        Map<String, CircuitBreakerMonitoringService.CircuitBreakerHealthStatus> allHealthStatuses = 
            monitoringService.getAllHealthStatuses();

        return allStats.entrySet().stream()
            .map(entry -> {
                String name = entry.getKey();
                CircuitBreakerMetricsCollector.CircuitBreakerStats stats = entry.getValue();
                CircuitBreakerMonitoringService.CircuitBreakerHealthStatus healthStatus = allHealthStatuses.get(name);

                CircuitBreakerMonitoringService.HealthLevel healthLevel = healthStatus != null ? 
                    healthStatus.getCurrentHealthLevel() : CircuitBreakerMonitoringService.HealthLevel.HEALTHY;

                int recentAlerts = healthStatus != null ? 
                    (int) healthStatus.getAlertHistory().stream()
                        .filter(alert -> alert.getTimestamp().isAfter(Instant.now().minus(Duration.ofHours(1))))
                        .count() : 0;

                return new CircuitBreakerSummary(
                    name,
                    healthLevel,
                    stats.getHealthScore(),
                    stats.getTotalExecutions(),
                    stats.getSuccessRate(),
                    stats.getAverageExecutionTimeMs(),
                    stats.getCurrentState().name(),
                    recentAlerts,
                    stats.getLastExecution()
                );
            })
            .sorted(Comparator.comparing((CircuitBreakerSummary summary) -> summary.getHealthLevel().ordinal())
                              .reversed()
                              .thenComparing(summary -> summary.getHealthScore()))
            .collect(Collectors.toList());
    }

    /**
     * Gets recent alerts for dashboard display.
     */
    public List<CircuitBreakerMonitoringService.CircuitBreakerAlert> getRecentAlerts() {
        return monitoringService.getRecentAlerts(configuration.getDefaultTimeRange()).stream()
            .limit(configuration.getMaxAlertDisplayCount())
            .collect(Collectors.toList());
    }

    /**
     * Generates HTML dashboard.
     */
    public String generateHtmlDashboard() {
        DashboardOverview overview = getDashboardOverview();
        List<CircuitBreakerSummary> summaries = getCircuitBreakerSummaries();
        List<CircuitBreakerMonitoringService.CircuitBreakerAlert> recentAlerts = getRecentAlerts();

        StringBuilder html = new StringBuilder();
        html.append(generateHtmlHeader());
        html.append(generateHtmlBody(overview, summaries, recentAlerts));
        html.append("</html>");

        return html.toString();
    }

    private String generateHtmlHeader() {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                %s
                %s
            </head>
            """, configuration.getTitle(), generateCSS(), generateJavaScript());
    }

    private String generateCSS() {
        return String.format("""
            <style>
            :root {
                --primary-color: %s;
                --success-color: #10b981;
                --warning-color: #f59e0b;
                --error-color: #ef4444;
                --background-color: #f8fafc;
                --card-background: #ffffff;
                --text-primary: #1f2937;
                --text-secondary: #6b7280;
                --border-color: #e5e7eb;
            }

            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background-color: var(--background-color);
                color: var(--text-primary);
                line-height: 1.6;
            }

            .dashboard-container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
            }

            .dashboard-header {
                background: linear-gradient(135deg, var(--primary-color), #3b82f6);
                color: white;
                padding: 30px;
                border-radius: 12px;
                margin-bottom: 30px;
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            }

            .dashboard-title {
                font-size: 2.5rem;
                font-weight: 700;
                margin-bottom: 8px;
            }

            .dashboard-subtitle {
                font-size: 1.1rem;
                opacity: 0.9;
            }

            .overview-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 20px;
                margin-bottom: 30px;
            }

            .overview-card {
                background: var(--card-background);
                padding: 24px;
                border-radius: 12px;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                border: 1px solid var(--border-color);
                text-align: center;
                transition: transform 0.2s, box-shadow 0.2s;
            }

            .overview-card:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            }

            .overview-card-value {
                font-size: 2.5rem;
                font-weight: 700;
                margin-bottom: 8px;
            }

            .overview-card-label {
                font-size: 0.9rem;
                color: var(--text-secondary);
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            .health-indicator {
                display: inline-block;
                width: 12px;
                height: 12px;
                border-radius: 50%%;
                margin-right: 8px;
            }

            .health-healthy { background-color: var(--success-color); }
            .health-warning { background-color: var(--warning-color); }
            .health-critical { background-color: var(--error-color); }
            .health-unknown { background-color: var(--text-secondary); }

            .section {
                background: var(--card-background);
                border-radius: 12px;
                padding: 24px;
                margin-bottom: 24px;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                border: 1px solid var(--border-color);
            }

            .section-title {
                font-size: 1.5rem;
                font-weight: 600;
                margin-bottom: 20px;
                color: var(--text-primary);
            }

            .circuit-breaker-grid {
                display: grid;
                grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
                gap: 20px;
            }

            .circuit-breaker-card {
                border: 1px solid var(--border-color);
                border-radius: 8px;
                padding: 20px;
                transition: border-color 0.2s;
            }

            .circuit-breaker-card:hover {
                border-color: var(--primary-color);
            }

            .circuit-breaker-header {
                display: flex;
                justify-content: between;
                align-items: center;
                margin-bottom: 16px;
            }

            .circuit-breaker-name {
                font-weight: 600;
                font-size: 1.1rem;
            }

            .circuit-breaker-state {
                padding: 4px 8px;
                border-radius: 4px;
                font-size: 0.8rem;
                font-weight: 500;
                text-transform: uppercase;
            }

            .state-closed { background-color: #dcfce7; color: #166534; }
            .state-half_open { background-color: #fef3c7; color: #92400e; }
            .state-open { background-color: #fecaca; color: #991b1b; }

            .circuit-breaker-metrics {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 12px;
                margin-top: 16px;
            }

            .metric {
                text-align: center;
            }

            .metric-value {
                font-size: 1.4rem;
                font-weight: 600;
                color: var(--primary-color);
            }

            .metric-label {
                font-size: 0.8rem;
                color: var(--text-secondary);
                margin-top: 4px;
            }

            .health-score {
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 12px 0;
            }

            .health-score-bar {
                width: 100px;
                height: 8px;
                background-color: #e5e7eb;
                border-radius: 4px;
                overflow: hidden;
                margin-left: 8px;
            }

            .health-score-fill {
                height: 100%%;
                border-radius: 4px;
                transition: width 0.3s;
            }

            .alerts-list {
                max-height: 400px;
                overflow-y: auto;
            }

            .alert-item {
                border-left: 4px solid;
                padding: 16px;
                margin-bottom: 12px;
                border-radius: 0 8px 8px 0;
                background-color: #f9fafb;
            }

            .alert-critical { border-left-color: var(--error-color); }
            .alert-warning { border-left-color: var(--warning-color); }
            .alert-info { border-left-color: var(--primary-color); }

            .alert-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 8px;
            }

            .alert-title {
                font-weight: 600;
                font-size: 1rem;
            }

            .alert-time {
                font-size: 0.85rem;
                color: var(--text-secondary);
            }

            .alert-description {
                color: var(--text-secondary);
                font-size: 0.9rem;
            }

            .last-updated {
                text-align: center;
                color: var(--text-secondary);
                font-size: 0.9rem;
                margin-top: 20px;
                padding-top: 20px;
                border-top: 1px solid var(--border-color);
            }

            @media (max-width: 768px) {
                .dashboard-container {
                    padding: 16px;
                }

                .overview-grid {
                    grid-template-columns: repeat(2, 1fr);
                }

                .circuit-breaker-grid {
                    grid-template-columns: 1fr;
                }

                .dashboard-title {
                    font-size: 2rem;
                }
            }
            </style>
            """, configuration.getThemeColor());
    }

    private String generateJavaScript() {
        if (!configuration.isEnableRealTimeUpdates()) {
            return "";
        }

        return """
            <script>
            function refreshDashboard() {
                window.location.reload();
            }

            // Auto-refresh every 30 seconds
            setInterval(refreshDashboard, 30000);

            // Add real-time timestamp updates
            function updateTimestamps() {
                document.querySelectorAll('[data-timestamp]').forEach(element => {
                    const timestamp = new Date(element.dataset.timestamp);
                    element.textContent = formatRelativeTime(timestamp);
                });
            }

            function formatRelativeTime(date) {
                const now = new Date();
                const diff = now - date;
                const seconds = Math.floor(diff / 1000);
                const minutes = Math.floor(seconds / 60);
                const hours = Math.floor(minutes / 60);
                const days = Math.floor(hours / 24);

                if (days > 0) return `${days}d ago`;
                if (hours > 0) return `${hours}h ago`;
                if (minutes > 0) return `${minutes}m ago`;
                return `${seconds}s ago`;
            }

            setInterval(updateTimestamps, 1000);
            </script>
            """;
    }

    private String generateHtmlBody(DashboardOverview overview, List<CircuitBreakerSummary> summaries,
                                  List<CircuitBreakerMonitoringService.CircuitBreakerAlert> alerts) {
        StringBuilder html = new StringBuilder();
        html.append("<body>");
        html.append("<div class=\"dashboard-container\">");

        // Header
        html.append(generateHeader(overview));

        // Overview cards
        html.append(generateOverviewCards(overview));

        // Circuit breakers section
        html.append(generateCircuitBreakersSection(summaries));

        // Alerts section
        html.append(generateAlertsSection(alerts));

        // Footer
        html.append(generateFooter(overview));

        html.append("</div>");
        html.append("</body>");

        return html.toString();
    }

    private String generateHeader(DashboardOverview overview) {
        return String.format("""
            <div class="dashboard-header">
                <h1 class="dashboard-title">%s</h1>
                <p class="dashboard-subtitle">Real-time monitoring of circuit breaker health and performance</p>
            </div>
            """, configuration.getTitle());
    }

    private String generateOverviewCards(DashboardOverview overview) {
        return String.format("""
            <div class="overview-grid">
                <div class="overview-card">
                    <div class="overview-card-value">%d</div>
                    <div class="overview-card-label">Total Circuit Breakers</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value" style="color: var(--success-color);">%.1f%%</div>
                    <div class="overview-card-label">Overall Health Score</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value" style="color: var(--primary-color);">%.1f%%</div>
                    <div class="overview-card-label">Success Rate</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value" style="color: %s;">%d</div>
                    <div class="overview-card-label">Active Alerts</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value" style="color: var(--success-color);">%d</div>
                    <div class="overview-card-label">Healthy</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value" style="color: var(--warning-color);">%d</div>
                    <div class="overview-card-label">Warning</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value" style="color: var(--error-color);">%d</div>
                    <div class="overview-card-label">Critical</div>
                </div>
                <div class="overview-card">
                    <div class="overview-card-value">%s</div>
                    <div class="overview-card-label">Total Executions</div>
                </div>
            </div>
            """,
            overview.getTotalCircuitBreakers(),
            overview.getOverallHealthScore() * 100,
            overview.getOverallSuccessRate() * 100,
            overview.getActiveAlerts() > 0 ? "var(--error-color)" : "var(--success-color)",
            overview.getActiveAlerts(),
            overview.getHealthyCircuitBreakers(),
            overview.getWarningCircuitBreakers(),
            overview.getCriticalCircuitBreakers(),
            formatNumber(overview.getTotalExecutions())
        );
    }

    private String generateCircuitBreakersSection(List<CircuitBreakerSummary> summaries) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">");
        html.append("<h2 class=\"section-title\">Circuit Breakers</h2>");
        html.append("<div class=\"circuit-breaker-grid\">");

        for (CircuitBreakerSummary summary : summaries) {
            html.append(generateCircuitBreakerCard(summary));
        }

        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    private String generateCircuitBreakerCard(CircuitBreakerSummary summary) {
        String healthClass = "health-" + summary.getHealthLevel().name().toLowerCase();
        String stateClass = "state-" + summary.getCurrentState().toLowerCase();
        String healthScoreColor = getHealthScoreColor(summary.getHealthScore());

        return String.format("""
            <div class="circuit-breaker-card">
                <div class="circuit-breaker-header">
                    <div>
                        <span class="health-indicator %s"></span>
                        <span class="circuit-breaker-name">%s</span>
                    </div>
                    <span class="circuit-breaker-state %s">%s</span>
                </div>
                
                <div class="health-score">
                    Health Score:
                    <div class="health-score-bar">
                        <div class="health-score-fill" style="width: %.1f%%; background-color: %s;"></div>
                    </div>
                    <span style="margin-left: 8px; font-weight: 600;">%.1f%%</span>
                </div>
                
                <div class="circuit-breaker-metrics">
                    <div class="metric">
                        <div class="metric-value">%s</div>
                        <div class="metric-label">Total Executions</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">%.1f%%</div>
                        <div class="metric-label">Success Rate</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">%.0fms</div>
                        <div class="metric-label">Avg Response</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Recent Alerts</div>
                    </div>
                </div>
            </div>
            """,
            healthClass,
            summary.getName(),
            stateClass,
            summary.getCurrentState(),
            summary.getHealthScore() * 100,
            healthScoreColor,
            summary.getHealthScore() * 100,
            formatNumber(summary.getTotalExecutions()),
            summary.getSuccessRate() * 100,
            summary.getAverageResponseTime(),
            summary.getRecentAlerts()
        );
    }

    private String generateAlertsSection(List<CircuitBreakerMonitoringService.CircuitBreakerAlert> alerts) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">");
        html.append("<h2 class=\"section-title\">Recent Alerts</h2>");

        if (alerts.isEmpty()) {
            html.append("<p style=\"text-align: center; color: var(--text-secondary); padding: 40px;\">No recent alerts</p>");
        } else {
            html.append("<div class=\"alerts-list\">");
            for (CircuitBreakerMonitoringService.CircuitBreakerAlert alert : alerts) {
                html.append(generateAlertItem(alert));
            }
            html.append("</div>");
        }

        html.append("</div>");

        return html.toString();
    }

    private String generateAlertItem(CircuitBreakerMonitoringService.CircuitBreakerAlert alert) {
        String alertClass = "alert-" + alert.getSeverity().name().toLowerCase();
        String timeAgo = formatRelativeTime(alert.getTimestamp());

        return String.format("""
            <div class="alert-item %s">
                <div class="alert-header">
                    <span class="alert-title">[%s] %s</span>
                    <span class="alert-time" data-timestamp="%s">%s</span>
                </div>
                <div class="alert-description">%s</div>
            </div>
            """,
            alertClass,
            alert.getCircuitBreakerName(),
            alert.getTitle(),
            alert.getTimestamp().toString(),
            timeAgo,
            alert.getDescription()
        );
    }

    private String generateFooter(DashboardOverview overview) {
        return String.format("""
            <div class="last-updated">
                Last updated: %s | Monitoring: %s
            </div>
            """,
            formatTimestamp(overview.getLastUpdate()),
            monitoringService.isMonitoringActive() ? "Active" : "Inactive"
        );
    }

    // Helper methods

    private String getHealthScoreColor(double healthScore) {
        if (healthScore >= 0.8) return "var(--success-color)";
        if (healthScore >= 0.6) return "var(--warning-color)";
        return "var(--error-color)";
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }

    private String formatTimestamp(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault())
            .format(instant);
    }

    private String formatRelativeTime(Instant instant) {
        Duration diff = Duration.between(instant, Instant.now());
        long seconds = diff.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return seconds + "s ago";
    }

    // Public API methods

    public DashboardConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Generates JSON data for API endpoints.
     */
    public String generateJsonData() {
        DashboardOverview overview = getDashboardOverview();
        List<CircuitBreakerSummary> summaries = getCircuitBreakerSummaries();
        List<CircuitBreakerMonitoringService.CircuitBreakerAlert> alerts = getRecentAlerts();

        // In a real implementation, this would use a proper JSON library like Jackson
        return String.format("""
            {
                "overview": {
                    "totalCircuitBreakers": %d,
                    "healthyCircuitBreakers": %d,
                    "warningCircuitBreakers": %d,
                    "criticalCircuitBreakers": %d,
                    "overallHealthScore": %.3f,
                    "totalExecutions": %d,
                    "overallSuccessRate": %.3f,
                    "activeAlerts": %d,
                    "lastUpdate": "%s"
                },
                "circuitBreakersCount": %d,
                "alertsCount": %d
            }
            """,
            overview.getTotalCircuitBreakers(),
            overview.getHealthyCircuitBreakers(),
            overview.getWarningCircuitBreakers(),
            overview.getCriticalCircuitBreakers(),
            overview.getOverallHealthScore(),
            overview.getTotalExecutions(),
            overview.getOverallSuccessRate(),
            overview.getActiveAlerts(),
            overview.getLastUpdate().toString(),
            summaries.size(),
            alerts.size()
        );
    }
}