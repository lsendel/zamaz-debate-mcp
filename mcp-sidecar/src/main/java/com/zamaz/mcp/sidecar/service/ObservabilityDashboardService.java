package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Comprehensive Observability Dashboard Service for MCP Sidecar
 * 
 * Features:
 * - Real-time metrics aggregation
 * - Service health monitoring
 * - Performance analytics
 * - Custom dashboard widgets
 * - Historical data trends
 * - Alerting integration
 * - Multi-dimensional monitoring
 * - Business metrics tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObservabilityDashboardService {

    private final MetricsCollectorService metricsCollectorService;
    private final AdvancedRateLimitingService rateLimitingService;
    private final DistributedCircuitBreakerService circuitBreakerService;
    private final AuditLoggingService auditLoggingService;
    private final AlertingService alertingService;
    private final AdvancedRequestRoutingService routingService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${app.dashboard.enabled:true}")
    private boolean dashboardEnabled;

    @Value("${app.dashboard.refresh-interval:30s}")
    private Duration refreshInterval;

    @Value("${app.dashboard.data-retention:7d}")
    private Duration dataRetention;

    @Value("${app.dashboard.max-data-points:1000}")
    private int maxDataPoints;

    // Dashboard state
    private final Map<String, DashboardWidget> widgets = new ConcurrentHashMap<>();
    private final Map<String, TimeSeriesData> timeSeriesData = new ConcurrentHashMap<>();
    private final Map<String, AlertSummary> alertSummaries = new ConcurrentHashMap<>();
    private volatile DashboardSnapshot lastSnapshot;
    private volatile Instant lastRefresh = Instant.now();

    /**
     * Dashboard widget types
     */
    public enum WidgetType {
        METRIC_CARD, TIME_SERIES_CHART, STATUS_INDICATOR, GAUGE, PIE_CHART, 
        TABLE, HEATMAP, HISTOGRAM, ALERT_LIST, LOG_STREAM
    }

    /**
     * Dashboard widget definition
     */
    public static class DashboardWidget {
        private final String id;
        private final String title;
        private final String description;
        private final WidgetType type;
        private final Map<String, Object> configuration;
        private final Map<String, String> queries;
        private volatile Object currentValue;
        private volatile Instant lastUpdated;
        private final List<DataPoint> history;

        public DashboardWidget(String id, String title, String description, WidgetType type,
                             Map<String, Object> configuration, Map<String, String> queries) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.configuration = configuration != null ? new HashMap<>(configuration) : new HashMap<>();
            this.queries = queries != null ? new HashMap<>(queries) : new HashMap<>();
            this.lastUpdated = Instant.now();
            this.history = new ArrayList<>();
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public WidgetType getType() { return type; }
        public Map<String, Object> getConfiguration() { return configuration; }
        public Map<String, String> getQueries() { return queries; }
        public Object getCurrentValue() { return currentValue; }
        public Instant getLastUpdated() { return lastUpdated; }
        public List<DataPoint> getHistory() { return history; }

        public void updateValue(Object value) {
            this.currentValue = value;
            this.lastUpdated = Instant.now();
            addHistoryPoint(value);
        }

        private void addHistoryPoint(Object value) {
            if (value instanceof Number) {
                history.add(new DataPoint(Instant.now(), ((Number) value).doubleValue()));
                
                // Limit history size
                if (history.size() > 1000) {
                    history.remove(0);
                }
            }
        }
    }

    /**
     * Time series data point
     */
    public static class DataPoint {
        private final Instant timestamp;
        private final double value;
        private final Map<String, String> tags;

        public DataPoint(Instant timestamp, double value) {
            this(timestamp, value, new HashMap<>());
        }

        public DataPoint(Instant timestamp, double value, Map<String, String> tags) {
            this.timestamp = timestamp;
            this.value = value;
            this.tags = tags != null ? new HashMap<>(tags) : new HashMap<>();
        }

        public Instant getTimestamp() { return timestamp; }
        public double getValue() { return value; }
        public Map<String, String> getTags() { return tags; }
    }

    /**
     * Time series data container
     */
    public static class TimeSeriesData {
        private final String metricName;
        private final List<DataPoint> dataPoints;
        private final Map<String, String> labels;

        public TimeSeriesData(String metricName, Map<String, String> labels) {
            this.metricName = metricName;
            this.labels = labels != null ? new HashMap<>(labels) : new HashMap<>();
            this.dataPoints = new ArrayList<>();
        }

        public String getMetricName() { return metricName; }
        public List<DataPoint> getDataPoints() { return dataPoints; }
        public Map<String, String> getLabels() { return labels; }

        public void addDataPoint(double value) {
            addDataPoint(Instant.now(), value);
        }

        public void addDataPoint(Instant timestamp, double value) {
            dataPoints.add(new DataPoint(timestamp, value));
            
            // Remove old data points
            Instant cutoff = Instant.now().minus(Duration.ofHours(24));
            dataPoints.removeIf(point -> point.getTimestamp().isBefore(cutoff));
        }
    }

    /**
     * Alert summary
     */
    public static class AlertSummary {
        private final String ruleId;
        private final String ruleName;
        private final AlertingService.AlertSeverity severity;
        private final int activeCount;
        private final int totalCount;
        private final Instant lastFired;

        public AlertSummary(String ruleId, String ruleName, AlertingService.AlertSeverity severity,
                          int activeCount, int totalCount, Instant lastFired) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.severity = severity;
            this.activeCount = activeCount;
            this.totalCount = totalCount;
            this.lastFired = lastFired;
        }

        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public AlertingService.AlertSeverity getSeverity() { return severity; }
        public int getActiveCount() { return activeCount; }
        public int getTotalCount() { return totalCount; }
        public Instant getLastFired() { return lastFired; }
    }

    /**
     * Complete dashboard snapshot
     */
    public static class DashboardSnapshot {
        private final Instant timestamp;
        private final Map<String, Object> systemMetrics;
        private final Map<String, Object> serviceMetrics;
        private final Map<String, Object> securityMetrics;
        private final Map<String, Object> businessMetrics;
        private final List<AlertSummary> activeAlerts;
        private final Map<String, DashboardWidget> widgets;

        public DashboardSnapshot(Instant timestamp, Map<String, Object> systemMetrics,
                               Map<String, Object> serviceMetrics, Map<String, Object> securityMetrics,
                               Map<String, Object> businessMetrics, List<AlertSummary> activeAlerts,
                               Map<String, DashboardWidget> widgets) {
            this.timestamp = timestamp;
            this.systemMetrics = systemMetrics != null ? new HashMap<>(systemMetrics) : new HashMap<>();
            this.serviceMetrics = serviceMetrics != null ? new HashMap<>(serviceMetrics) : new HashMap<>();
            this.securityMetrics = securityMetrics != null ? new HashMap<>(securityMetrics) : new HashMap<>();
            this.businessMetrics = businessMetrics != null ? new HashMap<>(businessMetrics) : new HashMap<>();
            this.activeAlerts = activeAlerts != null ? new ArrayList<>(activeAlerts) : new ArrayList<>();
            this.widgets = widgets != null ? new HashMap<>(widgets) : new HashMap<>();
        }

        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getSystemMetrics() { return systemMetrics; }
        public Map<String, Object> getServiceMetrics() { return serviceMetrics; }
        public Map<String, Object> getSecurityMetrics() { return securityMetrics; }
        public Map<String, Object> getBusinessMetrics() { return businessMetrics; }
        public List<AlertSummary> getActiveAlerts() { return activeAlerts; }
        public Map<String, DashboardWidget> getWidgets() { return widgets; }
    }

    /**
     * Initialize default dashboard widgets
     */
    public void initializeDefaultWidgets() {
        // System metrics widgets
        createWidget("cpu-usage", "CPU Usage", "Current CPU utilization", WidgetType.GAUGE,
                Map.of("unit", "%", "min", 0, "max", 100, "threshold", 80),
                Map.of("query", "system.cpu.usage"));

        createWidget("memory-usage", "Memory Usage", "Current memory utilization", WidgetType.GAUGE,
                Map.of("unit", "MB", "threshold", 800),
                Map.of("query", "system.memory.used"));

        createWidget("request-rate", "Request Rate", "Requests per second", WidgetType.TIME_SERIES_CHART,
                Map.of("unit", "req/s", "aggregation", "sum"),
                Map.of("query", "rate(sidecar_requests_total[5m])"));

        createWidget("response-time", "Response Time", "95th percentile response time", WidgetType.TIME_SERIES_CHART,
                Map.of("unit", "ms", "aggregation", "p95"),
                Map.of("query", "histogram_quantile(0.95, rate(sidecar_request_duration_seconds_bucket[5m]))"));

        createWidget("error-rate", "Error Rate", "Request error rate", WidgetType.METRIC_CARD,
                Map.of("unit", "%", "threshold", 5.0),
                Map.of("query", "rate(sidecar_requests_total{status=~\"5..\"}[5m])"));

        // Service health widgets
        createWidget("service-health", "Service Health", "Overall service health status", WidgetType.STATUS_INDICATOR,
                Map.of("states", List.of("UP", "DEGRADED", "DOWN")),
                Map.of("query", "service.health.status"));

        createWidget("circuit-breakers", "Circuit Breakers", "Circuit breaker states", WidgetType.PIE_CHART,
                Map.of("states", List.of("CLOSED", "OPEN", "HALF_OPEN")),
                Map.of("query", "circuit_breaker.states"));

        createWidget("rate-limits", "Rate Limiting", "Rate limiting statistics", WidgetType.TABLE,
                Map.of("columns", List.of("User Tier", "Active Users", "Hit Rate")),
                Map.of("query", "rate_limiting.stats"));

        // Security widgets
        createWidget("security-threats", "Security Threats", "Threats detected by type", WidgetType.PIE_CHART,
                Map.of("colors", Map.of("LOW", "green", "MEDIUM", "yellow", "HIGH", "orange", "CRITICAL", "red")),
                Map.of("query", "security.threats.by_level"));

        createWidget("blocked-ips", "Blocked IPs", "Number of blocked IP addresses", WidgetType.METRIC_CARD,
                Map.of("unit", "count", "threshold", 10),
                Map.of("query", "security.blocked_ips.count"));

        // Business metrics widgets
        createWidget("active-users", "Active Users", "Currently active users", WidgetType.METRIC_CARD,
                Map.of("unit", "users"),
                Map.of("query", "business.active_users"));

        createWidget("api-usage", "API Usage", "API calls by endpoint", WidgetType.HEATMAP,
                Map.of("dimensions", List.of("endpoint", "method")),
                Map.of("query", "api.usage.by_endpoint"));

        log.info("Initialized {} dashboard widgets", widgets.size());
    }

    /**
     * Create a dashboard widget
     */
    public void createWidget(String id, String title, String description, WidgetType type,
                           Map<String, Object> configuration, Map<String, String> queries) {
        DashboardWidget widget = new DashboardWidget(id, title, description, type, configuration, queries);
        widgets.put(id, widget);
        log.debug("Created dashboard widget: {}", id);
    }

    /**
     * Refresh all dashboard data
     */
    @Scheduled(fixedDelayString = "${app.dashboard.refresh-interval:30s}")
    public void refreshDashboard() {
        if (!dashboardEnabled) {
            return;
        }

        try {
            log.debug("Refreshing dashboard data");
            
            // Collect all metrics
            Map<String, Object> systemMetrics = collectSystemMetrics().block();
            Map<String, Object> serviceMetrics = collectServiceMetrics().block();
            Map<String, Object> securityMetrics = collectSecurityMetrics().block();
            Map<String, Object> businessMetrics = collectBusinessMetrics().block();
            List<AlertSummary> activeAlerts = collectActiveAlerts().block();
            
            // Update widgets
            updateWidgets(systemMetrics, serviceMetrics, securityMetrics, businessMetrics);
            
            // Create snapshot
            lastSnapshot = new DashboardSnapshot(
                Instant.now(),
                systemMetrics,
                serviceMetrics,
                securityMetrics,
                businessMetrics,
                activeAlerts,
                new HashMap<>(widgets)
            );
            
            lastRefresh = Instant.now();
            
            log.debug("Dashboard refresh completed");
            
        } catch (Exception e) {
            log.error("Error refreshing dashboard", e);
        }
    }

    /**
     * Collect system metrics
     */
    private Mono<Map<String, Object>> collectSystemMetrics() {
        return metricsCollectorService.getMetricsReport()
                .map(report -> {
                    Map<String, Object> systemMetrics = new HashMap<>();
                    
                    if (report.containsKey("system")) {
                        Map<String, Object> system = (Map<String, Object>) report.get("system");
                        systemMetrics.put("memoryUsage", system.get("memoryUsage"));
                        systemMetrics.put("activeConnections", system.get("activeConnections"));
                        systemMetrics.put("cacheHitRate", system.get("cacheHitRate"));
                    }
                    
                    if (report.containsKey("requests")) {
                        Map<String, Object> requests = (Map<String, Object>) report.get("requests");
                        systemMetrics.put("requestRate", requests.get("requestsPerSecond"));
                        systemMetrics.put("averageResponseTime", requests.get("averageResponseTime"));
                        systemMetrics.put("errorRate", requests.get("errorRate"));
                    }
                    
                    return systemMetrics;
                });
    }

    /**
     * Collect service metrics
     */
    private Mono<Map<String, Object>> collectServiceMetrics() {
        return Mono.zip(
                circuitBreakerService.getAllCircuitBreakerStatuses(),
                routingService.getAllClustersStatus(),
                rateLimitingService.getRateLimitingStatistics()
        ).map(tuple -> {
            Map<String, Object> serviceMetrics = new HashMap<>();
            
            // Circuit breaker metrics
            Map<String, Object> cbStats = tuple.getT1();
            serviceMetrics.put("circuitBreakers", cbStats);
            
            // Routing metrics
            Map<String, Object> routingStats = tuple.getT2();
            serviceMetrics.put("routing", routingStats);
            
            // Rate limiting metrics
            Map<String, Object> rateLimitStats = tuple.getT3();
            serviceMetrics.put("rateLimiting", rateLimitStats);
            
            return serviceMetrics;
        });
    }

    /**
     * Collect security metrics
     */
    private Mono<Map<String, Object>> collectSecurityMetrics() {
        return auditLoggingService.getAuditStatistics()
                .map(auditStats -> {
                    Map<String, Object> securityMetrics = new HashMap<>();
                    securityMetrics.put("audit", auditStats);
                    
                    // Add security-specific metrics
                    securityMetrics.put("threatLevel", "LOW"); // This would come from security service
                    securityMetrics.put("blockedRequests", 0);
                    securityMetrics.put("suspiciousActivity", 0);
                    
                    return securityMetrics;
                });
    }

    /**
     * Collect business metrics
     */
    private Mono<Map<String, Object>> collectBusinessMetrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> businessMetrics = new HashMap<>();
            
            // Simulate business metrics
            businessMetrics.put("activeUsers", 150);
            businessMetrics.put("totalRequests", 10500);
            businessMetrics.put("successfulRequests", 10245);
            businessMetrics.put("averageLatency", 85.6);
            businessMetrics.put("peakResponseTime", 234.5);
            businessMetrics.put("apiCalls", Map.of(
                "/api/v1/debates", 4500,
                "/api/v1/llm", 3200,
                "/api/v1/organizations", 1800,
                "/api/v1/rag", 1000
            ));
            
            return businessMetrics;
        });
    }

    /**
     * Collect active alerts
     */
    private Mono<List<AlertSummary>> collectActiveAlerts() {
        return alertingService.getActiveAlerts()
                .map(alerts -> alerts.stream()
                        .collect(Collectors.groupingBy(AlertingService.Alert::getRuleId))
                        .entrySet().stream()
                        .map(entry -> {
                            String ruleId = entry.getKey();
                            List<AlertingService.Alert> ruleAlerts = entry.getValue();
                            AlertingService.Alert firstAlert = ruleAlerts.get(0);
                            
                            return new AlertSummary(
                                ruleId,
                                firstAlert.getRuleName(),
                                firstAlert.getSeverity(),
                                ruleAlerts.size(),
                                ruleAlerts.size(),
                                firstAlert.getStartsAt()
                            );
                        })
                        .collect(Collectors.toList())
                );
    }

    /**
     * Update widget values
     */
    private void updateWidgets(Map<String, Object> systemMetrics, Map<String, Object> serviceMetrics,
                             Map<String, Object> securityMetrics, Map<String, Object> businessMetrics) {
        
        // Update system metric widgets
        updateWidgetValue("memory-usage", systemMetrics.get("memoryUsage"));
        updateWidgetValue("request-rate", systemMetrics.get("requestRate"));
        updateWidgetValue("response-time", systemMetrics.get("averageResponseTime"));
        updateWidgetValue("error-rate", systemMetrics.get("errorRate"));
        
        // Update service metric widgets
        if (serviceMetrics.containsKey("circuitBreakers")) {
            updateWidgetValue("circuit-breakers", serviceMetrics.get("circuitBreakers"));
        }
        
        if (serviceMetrics.containsKey("rateLimiting")) {
            updateWidgetValue("rate-limits", serviceMetrics.get("rateLimiting"));
        }
        
        // Update security metric widgets
        if (securityMetrics.containsKey("audit")) {
            Map<String, Object> auditStats = (Map<String, Object>) securityMetrics.get("audit");
            updateWidgetValue("security-threats", auditStats.get("totalEvents"));
        }
        
        // Update business metric widgets
        updateWidgetValue("active-users", businessMetrics.get("activeUsers"));
        updateWidgetValue("api-usage", businessMetrics.get("apiCalls"));
    }

    /**
     * Update individual widget value
     */
    private void updateWidgetValue(String widgetId, Object value) {
        DashboardWidget widget = widgets.get(widgetId);
        if (widget != null && value != null) {
            widget.updateValue(value);
            
            // Store time series data
            if (value instanceof Number) {
                TimeSeriesData timeSeries = timeSeriesData.computeIfAbsent(
                    widgetId, 
                    k -> new TimeSeriesData(widgetId, Map.of("widget", widgetId))
                );
                timeSeries.addDataPoint(((Number) value).doubleValue());
            }
        }
    }

    /**
     * Get current dashboard snapshot
     */
    public Mono<DashboardSnapshot> getDashboardSnapshot() {
        if (lastSnapshot == null) {
            refreshDashboard();
        }
        return Mono.justOrEmpty(lastSnapshot);
    }

    /**
     * Get widget by ID
     */
    public Mono<DashboardWidget> getWidget(String widgetId) {
        return Mono.justOrEmpty(widgets.get(widgetId));
    }

    /**
     * Get all widgets
     */
    public Mono<Map<String, DashboardWidget>> getAllWidgets() {
        return Mono.just(new HashMap<>(widgets));
    }

    /**
     * Get time series data for widget
     */
    public Mono<TimeSeriesData> getTimeSeriesData(String widgetId, Duration timeRange) {
        return Mono.fromCallable(() -> {
            TimeSeriesData data = timeSeriesData.get(widgetId);
            if (data == null) {
                return null;
            }
            
            // Filter data by time range
            Instant cutoff = Instant.now().minus(timeRange);
            TimeSeriesData filteredData = new TimeSeriesData(data.getMetricName(), data.getLabels());
            
            data.getDataPoints().stream()
                    .filter(point -> point.getTimestamp().isAfter(cutoff))
                    .forEach(point -> filteredData.getDataPoints().add(point));
            
            return filteredData;
        });
    }

    /**
     * Get dashboard statistics
     */
    public Mono<Map<String, Object>> getDashboardStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            stats.put("totalWidgets", widgets.size());
            stats.put("lastRefresh", lastRefresh);
            stats.put("refreshInterval", refreshInterval.toString());
            stats.put("dashboardEnabled", dashboardEnabled);
            
            // Widget type distribution
            Map<WidgetType, Long> widgetTypeDistribution = widgets.values().stream()
                    .collect(Collectors.groupingBy(
                        DashboardWidget::getType,
                        Collectors.counting()
                    ));
            stats.put("widgetTypeDistribution", widgetTypeDistribution);
            
            // Time series data points
            long totalDataPoints = timeSeriesData.values().stream()
                    .mapToLong(data -> data.getDataPoints().size())
                    .sum();
            stats.put("totalDataPoints", totalDataPoints);
            
            return stats;
        });
    }

    /**
     * Export dashboard configuration
     */
    public Mono<Map<String, Object>> exportDashboardConfig() {
        return Mono.fromCallable(() -> {
            Map<String, Object> config = new HashMap<>();
            
            config.put("dashboardEnabled", dashboardEnabled);
            config.put("refreshInterval", refreshInterval.toString());
            config.put("dataRetention", dataRetention.toString());
            config.put("maxDataPoints", maxDataPoints);
            
            // Export widget configurations
            Map<String, Object> widgetConfigs = widgets.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            DashboardWidget widget = entry.getValue();
                            return Map.of(
                                "title", widget.getTitle(),
                                "description", widget.getDescription(),
                                "type", widget.getType().name(),
                                "configuration", widget.getConfiguration(),
                                "queries", widget.getQueries()
                            );
                        }
                    ));
            config.put("widgets", widgetConfigs);
            
            return config;
        });
    }

    /**
     * Get dashboard health
     */
    public Mono<Map<String, Object>> getDashboardHealth() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = new HashMap<>();
            
            boolean isHealthy = dashboardEnabled && 
                               Duration.between(lastRefresh, Instant.now()).compareTo(refreshInterval.multipliedBy(2)) < 0;
            
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("lastRefresh", lastRefresh);
            health.put("secondsSinceLastRefresh", Duration.between(lastRefresh, Instant.now()).getSeconds());
            health.put("dashboardEnabled", dashboardEnabled);
            health.put("widgetCount", widgets.size());
            
            return health;
        });
    }
}