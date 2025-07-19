package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.ObservabilityDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Dashboard Controller for MCP Sidecar
 * 
 * Provides REST endpoints for the observability dashboard
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final ObservabilityDashboardService dashboardService;

    /**
     * Get complete dashboard snapshot
     */
    @GetMapping("/snapshot")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<ObservabilityDashboardService.DashboardSnapshot>> getDashboardSnapshot() {
        return dashboardService.getDashboardSnapshot()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.debug("Dashboard snapshot requested"))
                .onErrorResume(error -> {
                    log.error("Error getting dashboard snapshot: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get all dashboard widgets
     */
    @GetMapping("/widgets")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, ObservabilityDashboardService.DashboardWidget>>> getAllWidgets() {
        return dashboardService.getAllWidgets()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("All dashboard widgets requested"))
                .onErrorResume(error -> {
                    log.error("Error getting dashboard widgets: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get specific widget
     */
    @GetMapping("/widgets/{widgetId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<ObservabilityDashboardService.DashboardWidget>> getWidget(@PathVariable String widgetId) {
        return dashboardService.getWidget(widgetId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.debug("Widget requested: {}", widgetId))
                .onErrorResume(error -> {
                    log.error("Error getting widget {}: {}", widgetId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get time series data for widget
     */
    @GetMapping("/widgets/{widgetId}/timeseries")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<ObservabilityDashboardService.TimeSeriesData>> getTimeSeriesData(
            @PathVariable String widgetId,
            @RequestParam(defaultValue = "1h") String timeRange) {
        
        try {
            Duration duration = Duration.parse("PT" + timeRange.toUpperCase());
            return dashboardService.getTimeSeriesData(widgetId, duration)
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build())
                    .doOnSuccess(response -> log.debug("Time series data requested for widget: {} with range: {}", widgetId, timeRange))
                    .onErrorResume(error -> {
                        log.error("Error getting time series data for widget {}: {}", widgetId, error.getMessage());
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        } catch (Exception e) {
            log.warn("Invalid time range format: {}", timeRange);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * Create custom widget
     */
    @PostMapping("/widgets")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> createWidget(@RequestBody CreateWidgetRequest request) {
        return Mono.fromRunnable(() -> {
            try {
                ObservabilityDashboardService.WidgetType type = 
                    ObservabilityDashboardService.WidgetType.valueOf(request.getType().toUpperCase());
                
                dashboardService.createWidget(
                    request.getId(),
                    request.getTitle(),
                    request.getDescription(),
                    type,
                    request.getConfiguration(),
                    request.getQueries()
                );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid widget type: " + request.getType());
            }
        })
        .then(Mono.just(ResponseEntity.ok(Map.of(
            "status", "Widget created",
            "widgetId", request.getId()
        ))))
        .doOnSuccess(response -> log.info("Custom widget created: {}", request.getId()))
        .onErrorResume(error -> {
            log.error("Error creating widget: {}", error.getMessage());
            if (error.getMessage().contains("Invalid widget type")) {
                return Mono.just(ResponseEntity.badRequest().build());
            }
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardStatistics() {
        return dashboardService.getDashboardStatistics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Dashboard statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting dashboard statistics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Force dashboard refresh
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> refreshDashboard() {
        return Mono.fromRunnable(() -> dashboardService.refreshDashboard())
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Dashboard refreshed",
                    "timestamp", java.time.Instant.now().toString()
                ))))
                .doOnSuccess(response -> log.info("Dashboard refresh triggered via API"))
                .onErrorResume(error -> {
                    log.error("Error refreshing dashboard: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get dashboard configuration
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardConfig() {
        return dashboardService.exportDashboardConfig()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Dashboard configuration requested"))
                .onErrorResume(error -> {
                    log.error("Error getting dashboard configuration: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get dashboard health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardHealth() {
        return dashboardService.getDashboardHealth()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Dashboard health check requested"))
                .onErrorResume(error -> {
                    log.error("Error getting dashboard health: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get available widget types
     */
    @GetMapping("/widget-types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public ResponseEntity<java.util.List<String>> getAvailableWidgetTypes() {
        java.util.List<String> widgetTypes = java.util.Arrays.stream(
                ObservabilityDashboardService.WidgetType.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(widgetTypes);
    }

    /**
     * Get dashboard overview
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<DashboardOverview>> getDashboardOverview() {
        return Mono.zip(
                dashboardService.getDashboardStatistics(),
                dashboardService.getDashboardHealth(),
                dashboardService.getAllWidgets()
        ).map(tuple -> {
            Map<String, Object> statistics = tuple.getT1();
            Map<String, Object> health = tuple.getT2();
            Map<String, ObservabilityDashboardService.DashboardWidget> widgets = tuple.getT3();
            
            // Count widgets by type
            Map<String, Long> widgetCounts = widgets.values().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        widget -> widget.getType().name(),
                        java.util.stream.Collectors.counting()
                    ));
            
            // Get recent data points
            long recentDataPoints = widgets.values().stream()
                    .mapToLong(widget -> Math.min(widget.getHistory().size(), 10))
                    .sum();
            
            DashboardOverview overview = new DashboardOverview(
                (String) health.get("status"),
                (Integer) statistics.get("totalWidgets"),
                widgetCounts,
                (Long) statistics.get("totalDataPoints"),
                recentDataPoints,
                (java.time.Instant) health.get("lastRefresh"),
                (Boolean) health.get("dashboardEnabled")
            );
            
            return ResponseEntity.ok(overview);
        })
        .doOnSuccess(response -> log.debug("Dashboard overview requested"))
        .onErrorResume(error -> {
            log.error("Error getting dashboard overview: {}", error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Request DTOs
     */
    public static class CreateWidgetRequest {
        private String id;
        private String title;
        private String description;
        private String type;
        private Map<String, Object> configuration = new java.util.HashMap<>();
        private Map<String, String> queries = new java.util.HashMap<>();
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
        public Map<String, String> getQueries() { return queries; }
        public void setQueries(Map<String, String> queries) { this.queries = queries; }
    }

    /**
     * Response DTOs
     */
    public static class DashboardOverview {
        private final String status;
        private final int totalWidgets;
        private final Map<String, Long> widgetTypeDistribution;
        private final long totalDataPoints;
        private final long recentDataPoints;
        private final java.time.Instant lastRefresh;
        private final boolean dashboardEnabled;
        
        public DashboardOverview(String status, int totalWidgets, Map<String, Long> widgetTypeDistribution,
                               long totalDataPoints, long recentDataPoints, java.time.Instant lastRefresh,
                               boolean dashboardEnabled) {
            this.status = status;
            this.totalWidgets = totalWidgets;
            this.widgetTypeDistribution = widgetTypeDistribution;
            this.totalDataPoints = totalDataPoints;
            this.recentDataPoints = recentDataPoints;
            this.lastRefresh = lastRefresh;
            this.dashboardEnabled = dashboardEnabled;
        }
        
        public String getStatus() { return status; }
        public int getTotalWidgets() { return totalWidgets; }
        public Map<String, Long> getWidgetTypeDistribution() { return widgetTypeDistribution; }
        public long getTotalDataPoints() { return totalDataPoints; }
        public long getRecentDataPoints() { return recentDataPoints; }
        public java.time.Instant getLastRefresh() { return lastRefresh; }
        public boolean isDashboardEnabled() { return dashboardEnabled; }
    }
}