package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.AlertingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Alerting Controller for MCP Sidecar
 * 
 * Provides REST endpoints for managing alerts and notifications
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertingController {

    private final AlertingService alertingService;

    /**
     * Get active alerts
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR') or hasRole('OPS')")
    public Mono<ResponseEntity<List<AlertingService.Alert>>> getActiveAlerts() {
        return alertingService.getActiveAlerts()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Active alerts requested"))
                .onErrorResume(error -> {
                    log.error("Error getting active alerts", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get alert statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MONITOR') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, Object>>> getAlertStatistics() {
        return alertingService.getAlertStatistics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Alert statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting alert statistics", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Fire manual alert
     */
    @PostMapping("/fire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> fireAlert(
            @RequestParam String ruleId,
            @RequestParam String message,
            @RequestParam(required = false) Map<String, String> labels) {
        
        return alertingService.fireAlert(ruleId, message, labels)
                .then(Mono.just(ResponseEntity.ok(Map.of("status", "Alert fired", "ruleId", ruleId))))
                .doOnSuccess(response -> log.info("Manual alert fired: ruleId={}, message={}", ruleId, message))
                .onErrorResume(error -> {
                    log.error("Error firing manual alert", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Acknowledge alert
     */
    @PostMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, String>>> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestParam String acknowledgedBy) {
        
        return alertingService.acknowledgeAlert(alertId, acknowledgedBy)
                .then(Mono.just(ResponseEntity.ok(Map.of("status", "Alert acknowledged", "alertId", alertId))))
                .doOnSuccess(response -> log.info("Alert acknowledged: alertId={}, by={}", alertId, acknowledgedBy))
                .onErrorResume(error -> {
                    log.error("Error acknowledging alert", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Create alert rule
     */
    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> createAlertRule(
            @RequestBody CreateAlertRuleRequest request) {
        
        return Mono.fromRunnable(() -> {
            AlertingService.AlertRule rule = new AlertingService.AlertRule(
                request.getId(),
                request.getName(),
                request.getDescription(),
                request.getQuery(),
                request.getSeverity(),
                request.getDuration(),
                request.getLabels(),
                request.getChannels(),
                request.getRecipients()
            );
            
            alertingService.addAlertRule(rule);
        })
        .then(Mono.just(ResponseEntity.ok(Map.of("status", "Alert rule created", "ruleId", request.getId()))))
        .doOnSuccess(response -> log.info("Alert rule created: {}", request.getName()))
        .onErrorResume(error -> {
            log.error("Error creating alert rule", error);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Delete alert rule
     */
    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> deleteAlertRule(@PathVariable String ruleId) {
        return Mono.fromRunnable(() -> alertingService.removeAlertRule(ruleId))
                .then(Mono.just(ResponseEntity.ok(Map.of("status", "Alert rule deleted", "ruleId", ruleId))))
                .doOnSuccess(response -> log.info("Alert rule deleted: {}", ruleId))
                .onErrorResume(error -> {
                    log.error("Error deleting alert rule", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Create alert suppression
     */
    @PostMapping("/suppressions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> createSuppression(
            @RequestBody CreateSuppressionRequest request) {
        
        return Mono.fromRunnable(() -> {
            AlertingService.AlertSuppression suppression = new AlertingService.AlertSuppression(
                request.getId(),
                request.getRulePattern(),
                request.getLabelMatchers(),
                request.getStartsAt(),
                request.getEndsAt(),
                request.getReason(),
                request.getCreatedBy()
            );
            
            alertingService.addSuppression(suppression);
        })
        .then(Mono.just(ResponseEntity.ok(Map.of("status", "Alert suppression created", "suppressionId", request.getId()))))
        .doOnSuccess(response -> log.info("Alert suppression created: {}", request.getId()))
        .onErrorResume(error -> {
            log.error("Error creating alert suppression", error);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Delete alert suppression
     */
    @DeleteMapping("/suppressions/{suppressionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> deleteSuppression(@PathVariable String suppressionId) {
        return Mono.fromRunnable(() -> alertingService.removeSuppression(suppressionId))
                .then(Mono.just(ResponseEntity.ok(Map.of("status", "Alert suppression deleted", "suppressionId", suppressionId))))
                .doOnSuccess(response -> log.info("Alert suppression deleted: {}", suppressionId))
                .onErrorResume(error -> {
                    log.error("Error deleting alert suppression", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Test notifications
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> testNotifications(
            @RequestParam List<AlertingService.NotificationChannel> channels) {
        
        return alertingService.fireAlert(
                "test-alert",
                "This is a test alert notification",
                Map.of("test", "true", "source", "manual")
        )
        .then(Mono.just(ResponseEntity.ok(Map.of("status", "Test notifications sent"))))
        .doOnSuccess(response -> log.info("Test notifications sent to channels: {}", channels))
        .onErrorResume(error -> {
            log.error("Error sending test notifications", error);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Request DTOs
     */
    public static class CreateAlertRuleRequest {
        private String id;
        private String name;
        private String description;
        private String query;
        private AlertingService.AlertSeverity severity;
        private java.time.Duration duration;
        private Map<String, String> labels;
        private List<AlertingService.NotificationChannel> channels;
        private Set<String> recipients;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public AlertingService.AlertSeverity getSeverity() { return severity; }
        public void setSeverity(AlertingService.AlertSeverity severity) { this.severity = severity; }
        public java.time.Duration getDuration() { return duration; }
        public void setDuration(java.time.Duration duration) { this.duration = duration; }
        public Map<String, String> getLabels() { return labels; }
        public void setLabels(Map<String, String> labels) { this.labels = labels; }
        public List<AlertingService.NotificationChannel> getChannels() { return channels; }
        public void setChannels(List<AlertingService.NotificationChannel> channels) { this.channels = channels; }
        public Set<String> getRecipients() { return recipients; }
        public void setRecipients(Set<String> recipients) { this.recipients = recipients; }
    }

    public static class CreateSuppressionRequest {
        private String id;
        private String rulePattern;
        private Map<String, String> labelMatchers;
        private Instant startsAt;
        private Instant endsAt;
        private String reason;
        private String createdBy;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRulePattern() { return rulePattern; }
        public void setRulePattern(String rulePattern) { this.rulePattern = rulePattern; }
        public Map<String, String> getLabelMatchers() { return labelMatchers; }
        public void setLabelMatchers(Map<String, String> labelMatchers) { this.labelMatchers = labelMatchers; }
        public Instant getStartsAt() { return startsAt; }
        public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
        public Instant getEndsAt() { return endsAt; }
        public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    }
}