package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Alerting and Notification Service for MCP Sidecar
 * 
 * Provides comprehensive alerting capabilities:
 * - Real-time threshold monitoring
 * - Multi-channel notifications (Slack, Email, Webhook)
 * - Alert aggregation and deduplication
 * - Escalation policies
 * - Alert suppression and maintenance windows
 * - Historical alert tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${app.alerting.enabled:true}")
    private boolean alertingEnabled;

    @Value("${app.alerting.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${app.alerting.email.smtp-server:}")
    private String smtpServer;

    @Value("${app.alerting.email.from:}")
    private String emailFrom;

    @Value("${app.alerting.webhook.url:}")
    private String webhookUrl;

    @Value("${app.alerting.evaluation-interval:30s}")
    private Duration evaluationInterval;

    // Alert storage and tracking
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final Map<String, AlertSuppression> suppressions = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> alertCounts = new ConcurrentHashMap<>();

    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        INFO(1),
        WARNING(2),
        ERROR(3),
        CRITICAL(4);

        private final int level;

        AlertSeverity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * Alert status
     */
    public enum AlertStatus {
        FIRING, RESOLVED, SUPPRESSED, ACKNOWLEDGED
    }

    /**
     * Notification channels
     */
    public enum NotificationChannel {
        SLACK, EMAIL, WEBHOOK, SMS
    }

    /**
     * Alert rule definition
     */
    public static class AlertRule {
        private final String id;
        private final String name;
        private final String description;
        private final String query;
        private final AlertSeverity severity;
        private final Duration duration;
        private final Map<String, String> labels;
        private final List<NotificationChannel> channels;
        private final Set<String> recipients;
        private volatile boolean enabled;

        public AlertRule(String id, String name, String description, String query, 
                        AlertSeverity severity, Duration duration, 
                        Map<String, String> labels, List<NotificationChannel> channels, 
                        Set<String> recipients) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.query = query;
            this.severity = severity;
            this.duration = duration;
            this.labels = labels != null ? new HashMap<>(labels) : new HashMap<>();
            this.channels = new ArrayList<>(channels);
            this.recipients = new HashSet<>(recipients);
            this.enabled = true;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getQuery() { return query; }
        public AlertSeverity getSeverity() { return severity; }
        public Duration getDuration() { return duration; }
        public Map<String, String> getLabels() { return labels; }
        public List<NotificationChannel> getChannels() { return channels; }
        public Set<String> getRecipients() { return recipients; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * Alert instance
     */
    public static class Alert {
        private final String id;
        private final String ruleId;
        private final String ruleName;
        private final AlertSeverity severity;
        private final String message;
        private final Map<String, String> labels;
        private final Instant startsAt;
        private volatile Instant endsAt;
        private volatile AlertStatus status;
        private volatile String acknowledgedBy;
        private volatile Instant acknowledgedAt;
        private final AtomicLong notificationCount;

        public Alert(String id, String ruleId, String ruleName, AlertSeverity severity, 
                    String message, Map<String, String> labels) {
            this.id = id;
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.severity = severity;
            this.message = message;
            this.labels = labels != null ? new HashMap<>(labels) : new HashMap<>();
            this.startsAt = Instant.now();
            this.status = AlertStatus.FIRING;
            this.notificationCount = new AtomicLong(0);
        }

        // Getters and setters
        public String getId() { return id; }
        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public AlertSeverity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public Map<String, String> getLabels() { return labels; }
        public Instant getStartsAt() { return startsAt; }
        public Instant getEndsAt() { return endsAt; }
        public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
        public AlertStatus getStatus() { return status; }
        public void setStatus(AlertStatus status) { this.status = status; }
        public String getAcknowledgedBy() { return acknowledgedBy; }
        public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
        public Instant getAcknowledgedAt() { return acknowledgedAt; }
        public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
        public long getNotificationCount() { return notificationCount.get(); }
        public void incrementNotificationCount() { notificationCount.incrementAndGet(); }

        public Duration getDuration() {
            Instant end = endsAt != null ? endsAt : Instant.now();
            return Duration.between(startsAt, end);
        }
    }

    /**
     * Alert suppression rule
     */
    public static class AlertSuppression {
        private final String id;
        private final String rulePattern;
        private final Map<String, String> labelMatchers;
        private final Instant startsAt;
        private final Instant endsAt;
        private final String reason;
        private final String createdBy;

        public AlertSuppression(String id, String rulePattern, Map<String, String> labelMatchers,
                               Instant startsAt, Instant endsAt, String reason, String createdBy) {
            this.id = id;
            this.rulePattern = rulePattern;
            this.labelMatchers = labelMatchers != null ? new HashMap<>(labelMatchers) : new HashMap<>();
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.reason = reason;
            this.createdBy = createdBy;
        }

        public String getId() { return id; }
        public String getRulePattern() { return rulePattern; }
        public Map<String, String> getLabelMatchers() { return labelMatchers; }
        public Instant getStartsAt() { return startsAt; }
        public Instant getEndsAt() { return endsAt; }
        public String getReason() { return reason; }
        public String getCreatedBy() { return createdBy; }

        public boolean isActive() {
            Instant now = Instant.now();
            return now.isAfter(startsAt) && now.isBefore(endsAt);
        }

        public boolean matches(Alert alert) {
            if (!isActive()) return false;
            
            // Check rule pattern
            if (rulePattern != null && !alert.getRuleName().matches(rulePattern)) {
                return false;
            }
            
            // Check label matchers
            for (Map.Entry<String, String> matcher : labelMatchers.entrySet()) {
                String alertValue = alert.getLabels().get(matcher.getKey());
                if (alertValue == null || !alertValue.equals(matcher.getValue())) {
                    return false;
                }
            }
            
            return true;
        }
    }

    /**
     * Initialize default alert rules
     */
    public void initializeDefaultRules() {
        // High error rate alert
        addAlertRule(new AlertRule(
            "high-error-rate",
            "High Error Rate",
            "Error rate is above 5% for 5 minutes",
            "rate(sidecar_requests_total{status=~\"5..\"}[5m]) / rate(sidecar_requests_total[5m]) > 0.05",
            AlertSeverity.ERROR,
            Duration.ofMinutes(5),
            Map.of("service", "sidecar", "type", "error-rate"),
            List.of(NotificationChannel.SLACK, NotificationChannel.EMAIL),
            Set.of("ops-team", "dev-team")
        ));

        // High response time alert
        addAlertRule(new AlertRule(
            "high-response-time",
            "High Response Time",
            "95th percentile response time is above 500ms for 5 minutes",
            "histogram_quantile(0.95, rate(sidecar_request_duration_seconds_bucket[5m])) > 0.5",
            AlertSeverity.WARNING,
            Duration.ofMinutes(5),
            Map.of("service", "sidecar", "type", "performance"),
            List.of(NotificationChannel.SLACK),
            Set.of("ops-team")
        ));

        // High memory usage alert
        addAlertRule(new AlertRule(
            "high-memory-usage",
            "High Memory Usage",
            "Memory usage is above 80% for 10 minutes",
            "sidecar_memory_used / sidecar_memory_total > 0.8",
            AlertSeverity.WARNING,
            Duration.ofMinutes(10),
            Map.of("service", "sidecar", "type", "resource"),
            List.of(NotificationChannel.SLACK),
            Set.of("ops-team")
        ));

        // Circuit breaker trips alert
        addAlertRule(new AlertRule(
            "circuit-breaker-trips",
            "Circuit Breaker Trips",
            "Circuit breaker is tripping frequently",
            "rate(sidecar_circuit_breaker_trips[5m]) > 5",
            AlertSeverity.CRITICAL,
            Duration.ofMinutes(2),
            Map.of("service", "sidecar", "type", "circuit-breaker"),
            List.of(NotificationChannel.SLACK, NotificationChannel.EMAIL, NotificationChannel.WEBHOOK),
            Set.of("ops-team", "dev-team", "on-call")
        ));

        // Security threats alert
        addAlertRule(new AlertRule(
            "security-threats",
            "Security Threats Detected",
            "High number of security threats detected",
            "rate(security_threats_detected_total[5m]) > 10",
            AlertSeverity.CRITICAL,
            Duration.ofMinutes(1),
            Map.of("service", "sidecar", "type", "security"),
            List.of(NotificationChannel.SLACK, NotificationChannel.EMAIL, NotificationChannel.WEBHOOK),
            Set.of("security-team", "ops-team", "on-call")
        ));

        // Rate limiting hits alert
        addAlertRule(new AlertRule(
            "rate-limit-hits",
            "High Rate Limit Hits",
            "Rate limiting is being hit frequently",
            "rate(sidecar_rate_limit_hits[5m]) > 50",
            AlertSeverity.WARNING,
            Duration.ofMinutes(5),
            Map.of("service", "sidecar", "type", "rate-limiting"),
            List.of(NotificationChannel.SLACK),
            Set.of("ops-team")
        ));

        log.info("Initialized {} default alert rules", alertRules.size());
    }

    /**
     * Add alert rule
     */
    public void addAlertRule(AlertRule rule) {
        alertRules.put(rule.getId(), rule);
        log.debug("Added alert rule: {}", rule.getName());
    }

    /**
     * Remove alert rule
     */
    public void removeAlertRule(String ruleId) {
        AlertRule removed = alertRules.remove(ruleId);
        if (removed != null) {
            log.debug("Removed alert rule: {}", removed.getName());
        }
    }

    /**
     * Fire alert
     */
    public Mono<Void> fireAlert(String ruleId, String message, Map<String, String> labels) {
        return Mono.fromRunnable(() -> {
            AlertRule rule = alertRules.get(ruleId);
            if (rule == null || !rule.isEnabled()) {
                return;
            }

            String alertId = generateAlertId(ruleId, labels);
            Alert existingAlert = activeAlerts.get(alertId);

            if (existingAlert != null && existingAlert.getStatus() == AlertStatus.FIRING) {
                // Alert already firing, update notification count
                existingAlert.incrementNotificationCount();
                return;
            }

            Alert alert = new Alert(alertId, ruleId, rule.getName(), rule.getSeverity(), message, labels);
            
            // Check if alert should be suppressed
            if (isAlertSuppressed(alert)) {
                alert.setStatus(AlertStatus.SUPPRESSED);
                log.debug("Alert suppressed: {}", alert.getId());
                return;
            }

            activeAlerts.put(alertId, alert);
            alertCounts.computeIfAbsent(ruleId, k -> new AtomicInteger(0)).incrementAndGet();

            // Send notifications
            sendNotifications(alert, rule);

            log.warn("Alert fired: {} - {}", alert.getRuleName(), alert.getMessage());
        });
    }

    /**
     * Resolve alert
     */
    public Mono<Void> resolveAlert(String ruleId, Map<String, String> labels) {
        return Mono.fromRunnable(() -> {
            String alertId = generateAlertId(ruleId, labels);
            Alert alert = activeAlerts.get(alertId);

            if (alert != null && alert.getStatus() == AlertStatus.FIRING) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setEndsAt(Instant.now());

                AlertRule rule = alertRules.get(ruleId);
                if (rule != null) {
                    sendResolutionNotifications(alert, rule);
                }

                log.info("Alert resolved: {} - Duration: {}", alert.getRuleName(), alert.getDuration());
            }
        });
    }

    /**
     * Acknowledge alert
     */
    public Mono<Void> acknowledgeAlert(String alertId, String acknowledgedBy) {
        return Mono.fromRunnable(() -> {
            Alert alert = activeAlerts.get(alertId);
            if (alert != null && alert.getStatus() == AlertStatus.FIRING) {
                alert.setStatus(AlertStatus.ACKNOWLEDGED);
                alert.setAcknowledgedBy(acknowledgedBy);
                alert.setAcknowledgedAt(Instant.now());

                log.info("Alert acknowledged: {} by {}", alert.getRuleName(), acknowledgedBy);
            }
        });
    }

    /**
     * Add alert suppression
     */
    public void addSuppression(AlertSuppression suppression) {
        suppressions.put(suppression.getId(), suppression);
        log.info("Added alert suppression: {} - {}", suppression.getId(), suppression.getReason());
    }

    /**
     * Remove alert suppression
     */
    public void removeSuppression(String suppressionId) {
        AlertSuppression removed = suppressions.remove(suppressionId);
        if (removed != null) {
            log.info("Removed alert suppression: {}", suppressionId);
        }
    }

    /**
     * Check if alert should be suppressed
     */
    private boolean isAlertSuppressed(Alert alert) {
        return suppressions.values().stream()
                .anyMatch(suppression -> suppression.matches(alert));
    }

    /**
     * Generate alert ID
     */
    private String generateAlertId(String ruleId, Map<String, String> labels) {
        StringBuilder sb = new StringBuilder(ruleId);
        if (labels != null) {
            labels.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sb.append(":").append(entry.getKey()).append("=").append(entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * Send notifications for alert
     */
    private void sendNotifications(Alert alert, AlertRule rule) {
        if (!alertingEnabled) {
            return;
        }

        for (NotificationChannel channel : rule.getChannels()) {
            try {
                switch (channel) {
                    case SLACK -> sendSlackNotification(alert, rule);
                    case EMAIL -> sendEmailNotification(alert, rule);
                    case WEBHOOK -> sendWebhookNotification(alert, rule);
                    case SMS -> sendSmsNotification(alert, rule);
                }
            } catch (Exception e) {
                log.error("Failed to send notification via {}: {}", channel, e.getMessage());
            }
        }

        alert.incrementNotificationCount();
    }

    /**
     * Send resolution notifications
     */
    private void sendResolutionNotifications(Alert alert, AlertRule rule) {
        if (!alertingEnabled) {
            return;
        }

        for (NotificationChannel channel : rule.getChannels()) {
            try {
                switch (channel) {
                    case SLACK -> sendSlackResolutionNotification(alert, rule);
                    case EMAIL -> sendEmailResolutionNotification(alert, rule);
                    case WEBHOOK -> sendWebhookResolutionNotification(alert, rule);
                }
            } catch (Exception e) {
                log.error("Failed to send resolution notification via {}: {}", channel, e.getMessage());
            }
        }
    }

    /**
     * Send Slack notification
     */
    private void sendSlackNotification(Alert alert, AlertRule rule) {
        if (slackWebhookUrl.isEmpty()) {
            return;
        }

        String color = switch (alert.getSeverity()) {
            case INFO -> "good";
            case WARNING -> "warning";
            case ERROR -> "danger";
            case CRITICAL -> "danger";
        };

        Map<String, Object> payload = Map.of(
            "text", "🚨 Alert: " + alert.getRuleName(),
            "attachments", List.of(Map.of(
                "color", color,
                "fields", List.of(
                    Map.of("title", "Alert", "value", alert.getRuleName(), "short", true),
                    Map.of("title", "Severity", "value", alert.getSeverity().name(), "short", true),
                    Map.of("title", "Message", "value", alert.getMessage(), "short", false),
                    Map.of("title", "Started", "value", alert.getStartsAt().toString(), "short", true)
                )
            ))
        );

        webClientBuilder.build()
                .post()
                .uri(slackWebhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> log.debug("Slack notification sent for alert: {}", alert.getId()),
                    error -> log.error("Failed to send Slack notification: {}", error.getMessage())
                );
    }

    /**
     * Send Slack resolution notification
     */
    private void sendSlackResolutionNotification(Alert alert, AlertRule rule) {
        if (slackWebhookUrl.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
            "text", "✅ Alert Resolved: " + alert.getRuleName(),
            "attachments", List.of(Map.of(
                "color", "good",
                "fields", List.of(
                    Map.of("title", "Alert", "value", alert.getRuleName(), "short", true),
                    Map.of("title", "Duration", "value", alert.getDuration().toString(), "short", true),
                    Map.of("title", "Resolved", "value", alert.getEndsAt().toString(), "short", true)
                )
            ))
        );

        webClientBuilder.build()
                .post()
                .uri(slackWebhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> log.debug("Slack resolution notification sent for alert: {}", alert.getId()),
                    error -> log.error("Failed to send Slack resolution notification: {}", error.getMessage())
                );
    }

    /**
     * Send email notification
     */
    private void sendEmailNotification(Alert alert, AlertRule rule) {
        // Email implementation would go here
        log.debug("Email notification would be sent for alert: {}", alert.getId());
    }

    /**
     * Send email resolution notification
     */
    private void sendEmailResolutionNotification(Alert alert, AlertRule rule) {
        // Email resolution implementation would go here
        log.debug("Email resolution notification would be sent for alert: {}", alert.getId());
    }

    /**
     * Send webhook notification
     */
    private void sendWebhookNotification(Alert alert, AlertRule rule) {
        if (webhookUrl.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
            "alertId", alert.getId(),
            "ruleName", alert.getRuleName(),
            "severity", alert.getSeverity().name(),
            "message", alert.getMessage(),
            "startsAt", alert.getStartsAt().toString(),
            "status", alert.getStatus().name(),
            "labels", alert.getLabels()
        );

        webClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> log.debug("Webhook notification sent for alert: {}", alert.getId()),
                    error -> log.error("Failed to send webhook notification: {}", error.getMessage())
                );
    }

    /**
     * Send webhook resolution notification
     */
    private void sendWebhookResolutionNotification(Alert alert, AlertRule rule) {
        if (webhookUrl.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
            "alertId", alert.getId(),
            "ruleName", alert.getRuleName(),
            "severity", alert.getSeverity().name(),
            "message", alert.getMessage(),
            "startsAt", alert.getStartsAt().toString(),
            "endsAt", alert.getEndsAt().toString(),
            "duration", alert.getDuration().toString(),
            "status", alert.getStatus().name(),
            "labels", alert.getLabels()
        );

        webClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> log.debug("Webhook resolution notification sent for alert: {}", alert.getId()),
                    error -> log.error("Failed to send webhook resolution notification: {}", error.getMessage())
                );
    }

    /**
     * Send SMS notification
     */
    private void sendSmsNotification(Alert alert, AlertRule rule) {
        // SMS implementation would go here
        log.debug("SMS notification would be sent for alert: {}", alert.getId());
    }

    /**
     * Evaluate alert rules
     */
    @Scheduled(fixedDelayString = "${app.alerting.evaluation-interval:30s}")
    public void evaluateAlertRules() {
        if (!alertingEnabled) {
            return;
        }

        log.debug("Evaluating {} alert rules", alertRules.size());

        for (AlertRule rule : alertRules.values()) {
            if (!rule.isEnabled()) {
                continue;
            }

            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("Error evaluating alert rule {}: {}", rule.getName(), e.getMessage());
            }
        }
    }

    /**
     * Evaluate individual alert rule
     */
    private void evaluateRule(AlertRule rule) {
        // This would integrate with actual metrics system
        // For now, simulate evaluation based on collected metrics
        
        Map<String, Object> metrics = metricsCollectorService.getMetricsReport().block();
        if (metrics == null) {
            return;
        }

        boolean shouldFire = false;
        String message = "";
        Map<String, String> labels = new HashMap<>(rule.getLabels());

        // Simple rule evaluation logic
        switch (rule.getId()) {
            case "high-error-rate":
                Map<String, Object> requestMetrics = (Map<String, Object>) metrics.get("requests");
                if (requestMetrics != null) {
                    Double errorRate = (Double) requestMetrics.get("errorRate");
                    if (errorRate != null && errorRate > 0.05) {
                        shouldFire = true;
                        message = String.format("Error rate is %.2f%% (threshold: 5%%)", errorRate * 100);
                    }
                }
                break;
                
            case "high-memory-usage":
                Map<String, Object> systemMetrics = (Map<String, Object>) metrics.get("system");
                if (systemMetrics != null) {
                    Double memoryUsage = (Double) systemMetrics.get("memoryUsage");
                    if (memoryUsage != null && memoryUsage > 800 * 1024 * 1024) { // 800MB
                        shouldFire = true;
                        message = String.format("Memory usage is %.0f MB (threshold: 800 MB)", memoryUsage / 1024 / 1024);
                    }
                }
                break;
        }

        if (shouldFire) {
            fireAlert(rule.getId(), message, labels).subscribe();
        } else {
            resolveAlert(rule.getId(), labels).subscribe();
        }
    }

    /**
     * Get active alerts
     */
    public Mono<List<Alert>> getActiveAlerts() {
        return Mono.fromCallable(() -> 
            activeAlerts.values().stream()
                .filter(alert -> alert.getStatus() == AlertStatus.FIRING || alert.getStatus() == AlertStatus.ACKNOWLEDGED)
                .sorted((a1, a2) -> {
                    int severityCompare = Integer.compare(a2.getSeverity().getLevel(), a1.getSeverity().getLevel());
                    if (severityCompare != 0) return severityCompare;
                    return a1.getStartsAt().compareTo(a2.getStartsAt());
                })
                .toList()
        );
    }

    /**
     * Get alert statistics
     */
    public Mono<Map<String, Object>> getAlertStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // Count alerts by status
            Map<AlertStatus, Long> statusCounts = activeAlerts.values().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        Alert::getStatus,
                        java.util.stream.Collectors.counting()
                    ));
            
            stats.put("statusCounts", statusCounts);
            
            // Count alerts by severity
            Map<AlertSeverity, Long> severityCounts = activeAlerts.values().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        Alert::getSeverity,
                        java.util.stream.Collectors.counting()
                    ));
            
            stats.put("severityCounts", severityCounts);
            
            // Total counts
            stats.put("totalRules", alertRules.size());
            stats.put("activeAlerts", activeAlerts.size());
            stats.put("suppressions", suppressions.size());
            
            // Rule statistics
            Map<String, Object> ruleStats = new HashMap<>();
            alertCounts.forEach((ruleId, count) -> {
                AlertRule rule = alertRules.get(ruleId);
                if (rule != null) {
                    ruleStats.put(rule.getName(), count.get());
                }
            });
            stats.put("ruleStats", ruleStats);
            
            return stats;
        });
    }

    /**
     * Clean up resolved alerts
     */
    @Scheduled(fixedDelayString = "${app.alerting.cleanup-interval:1h}")
    public void cleanupResolvedAlerts() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        
        activeAlerts.entrySet().removeIf(entry -> {
            Alert alert = entry.getValue();
            return alert.getStatus() == AlertStatus.RESOLVED && 
                   alert.getEndsAt() != null && 
                   alert.getEndsAt().isBefore(cutoff);
        });
        
        log.debug("Cleaned up old resolved alerts");
    }
}