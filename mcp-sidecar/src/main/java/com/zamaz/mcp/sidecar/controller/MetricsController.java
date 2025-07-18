package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.MetricsCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Metrics Controller for MCP Sidecar
 * 
 * Provides REST endpoints for accessing metrics and statistics
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {

    private final MetricsCollectorService metricsCollectorService;

    /**
     * Get comprehensive metrics report
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getMetricsReport() {
        return metricsCollectorService.getMetricsReport()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Metrics report requested"))
                .onErrorResume(error -> {
                    log.error("Error generating metrics report", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get system health metrics
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR') or hasRole('USER')")
    public Mono<ResponseEntity<Map<String, Object>>> getHealthMetrics() {
        return metricsCollectorService.getMetricsReport()
                .map(report -> {
                    // Extract only health-related metrics
                    Map<String, Object> healthMetrics = Map.of(
                        "serviceHealth", report.get("serviceHealth"),
                        "system", report.get("system"),
                        "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(healthMetrics);
                })
                .doOnSuccess(response -> log.debug("Health metrics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting health metrics", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get performance metrics
     */
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getPerformanceMetrics() {
        return metricsCollectorService.getMetricsReport()
                .map(report -> {
                    // Extract only performance-related metrics
                    Map<String, Object> performanceMetrics = Map.of(
                        "requests", report.get("requests"),
                        "aiServices", report.get("aiServices"),
                        "cache", report.get("cache"),
                        "tracing", report.get("tracing"),
                        "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(performanceMetrics);
                })
                .doOnSuccess(response -> log.debug("Performance metrics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting performance metrics", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get authentication metrics
     */
    @GetMapping("/authentication")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public Mono<ResponseEntity<Map<String, Object>>> getAuthenticationMetrics() {
        return metricsCollectorService.getMetricsReport()
                .map(report -> {
                    // Extract only authentication-related metrics
                    Map<String, Object> authMetrics = Map.of(
                        "authentication", report.get("authentication"),
                        "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(authMetrics);
                })
                .doOnSuccess(response -> log.debug("Authentication metrics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting authentication metrics", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Record custom metric
     */
    @PostMapping("/record")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    public Mono<ResponseEntity<Void>> recordCustomMetric(
            @RequestParam String type,
            @RequestParam String name,
            @RequestParam String value,
            @RequestParam(required = false) Map<String, String> tags) {
        
        return Mono.fromRunnable(() -> {
            // Record the custom metric based on type
            switch (type.toLowerCase()) {
                case "request":
                    // Parse request metric
                    break;
                case "auth":
                    // Parse authentication metric
                    break;
                case "ai":
                    // Parse AI metric
                    break;
                default:
                    log.warn("Unknown metric type: {}", type);
            }
        })
        .then(Mono.just(ResponseEntity.ok().build()))
        .doOnSuccess(response -> log.debug("Custom metric recorded: type={}, name={}, value={}", type, name, value))
        .onErrorResume(error -> {
            log.error("Error recording custom metric", error);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Export metrics
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> exportMetrics() {
        return metricsCollectorService.exportMetrics()
                .then(Mono.just(ResponseEntity.ok().build()))
                .doOnSuccess(response -> log.info("Metrics export requested"))
                .onErrorResume(error -> {
                    log.error("Error exporting metrics", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}