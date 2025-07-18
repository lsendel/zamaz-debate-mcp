package com.zamaz.mcp.common.linting.metrics;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * REST controller for exposing linting metrics and quality dashboard data.
 */
@RestController
@RequestMapping("/api/v1/linting/metrics")
@CrossOrigin(origins = "*")
public class LintingMetricsController {

    private final LintingMetricsCollector metricsCollector;
    private final MeterRegistry meterRegistry;

    public LintingMetricsController(LintingMetricsCollector metricsCollector, MeterRegistry meterRegistry) {
        this.metricsCollector = metricsCollector;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Get current quality metrics for dashboard.
     */
    @GetMapping("/quality")
    public ResponseEntity<LintingMetricsCollector.QualityMetrics> getQualityMetrics() {
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get quality dashboard data in a format suitable for visualization.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();

        Map<String, Object> dashboardData = new HashMap<>();

        // Summary metrics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRuns", metrics.getTotalRuns());
        summary.put("totalFilesProcessed", metrics.getTotalFilesProcessed());
        summary.put("currentQualityScore", metrics.getCurrentQualityScore());
        summary.put("timestamp", metrics.getTimestamp());
        dashboardData.put("summary", summary);

        // Issues by service (convert AtomicInteger to Integer for JSON)
        Map<String, Integer> serviceIssues = new HashMap<>();
        metrics.getIssueCountsByService().forEach((service, count) -> serviceIssues.put(service, count.get()));
        dashboardData.put("issuesByService", serviceIssues);

        // Issues by severity
        Map<String, Integer> severityIssues = new HashMap<>();
        metrics.getIssueCountsBySeverity().forEach((severity, count) -> severityIssues.put(severity, count.get()));
        dashboardData.put("issuesBySeverity", severityIssues);

        // Issues by linter
        Map<String, Integer> linterIssues = new HashMap<>();
        metrics.getIssueCountsByLinter().forEach((linter, count) -> linterIssues.put(linter, count.get()));
        dashboardData.put("issuesByLinter", linterIssues);

        // Quality trends (simplified - in real implementation would query historical
        // data)
        dashboardData.put("qualityTrend", generateQualityTrend());

        return ResponseEntity.ok(dashboardData);
    }

    /**
     * Get metrics for a specific service.
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<Map<String, Object>> getServiceMetrics(@PathVariable String serviceName) {
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();

        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("serviceName", serviceName);
        serviceData.put("issueCount", metrics.getIssueCountsByService().getOrDefault(serviceName,
                new java.util.concurrent.atomic.AtomicInteger(0)).get());
        serviceData.put("timestamp", metrics.getTimestamp());

        return ResponseEntity.ok(serviceData);
    }

    /**
     * Get Prometheus-compatible metrics.
     */
    @GetMapping("/prometheus")
    public ResponseEntity<String> getPrometheusMetrics() {
        StringBuilder prometheus = new StringBuilder();

        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();

        // Quality score metric
        prometheus.append("# HELP linting_quality_score Current overall quality score (0-100)\n");
        prometheus.append("# TYPE linting_quality_score gauge\n");
        prometheus.append("linting_quality_score ").append(metrics.getCurrentQualityScore()).append("\n");

        // Total runs metric
        prometheus.append("# HELP linting_runs_total Total number of linting runs\n");
        prometheus.append("# TYPE linting_runs_total counter\n");
        prometheus.append("linting_runs_total ").append(metrics.getTotalRuns()).append("\n");

        // Files processed metric
        prometheus.append("# HELP linting_files_processed_total Total number of files processed\n");
        prometheus.append("# TYPE linting_files_processed_total counter\n");
        prometheus.append("linting_files_processed_total ").append(metrics.getTotalFilesProcessed()).append("\n");

        // Issues by service
        prometheus.append("# HELP linting_issues_by_service Number of issues by service\n");
        prometheus.append("# TYPE linting_issues_by_service gauge\n");
        metrics.getIssueCountsByService().forEach((service, count) -> {
            prometheus.append("linting_issues_by_service{service=\"").append(service).append("\"} ")
                    .append(count.get()).append("\n");
        });

        // Issues by severity
        prometheus.append("# HELP linting_issues_by_severity Number of issues by severity\n");
        prometheus.append("# TYPE linting_issues_by_severity gauge\n");
        metrics.getIssueCountsBySeverity().forEach((severity, count) -> {
            prometheus.append("linting_issues_by_severity{severity=\"").append(severity).append("\"} ")
                    .append(count.get()).append("\n");
        });

        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                .body(prometheus.toString());
    }

    /**
     * Reset metrics (admin endpoint).
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        metricsCollector.resetMetrics();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Metrics have been reset");

        return ResponseEntity.ok(response);
    }

    /**
     * Get health status of linting system.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();

        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();

        // Determine health status based on quality score
        int qualityScore = metrics.getCurrentQualityScore();
        String status;
        if (qualityScore >= 80) {
            status = "healthy";
        } else if (qualityScore >= 60) {
            status = "warning";
        } else {
            status = "critical";
        }

        health.put("status", status);
        health.put("qualityScore", qualityScore);
        health.put("totalRuns", metrics.getTotalRuns());
        health.put("lastUpdate", metrics.getTimestamp());

        // Add recommendations based on status
        if ("critical".equals(status)) {
            health.put("recommendation", "Immediate attention required - quality score is below 60%");
        } else if ("warning".equals(status)) {
            health.put("recommendation", "Consider addressing linting issues to improve quality score");
        } else {
            health.put("recommendation", "Quality score is good - maintain current standards");
        }

        return ResponseEntity.ok(health);
    }

    private Map<String, Object> generateQualityTrend() {
        // This is a simplified implementation
        // In a real system, you would query historical data from a time-series database

        Map<String, Object> trend = new HashMap<>();
        trend.put("period", "last_7_days");
        trend.put("direction", "stable"); // could be "improving", "declining", "stable"
        trend.put("change", 0); // percentage change

        return trend;
    }
}
