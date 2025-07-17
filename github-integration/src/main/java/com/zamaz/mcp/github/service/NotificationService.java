package com.zamaz.mcp.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for sending notifications to various channels (Slack, email, PagerDuty).
 * This service handles all external notification integrations and provides
 * a unified interface for sending alerts and status updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;
    
    // Configuration values
    @Value("${notification.slack.webhook.url:}")
    private String slackWebhookUrl;
    
    @Value("${notification.slack.channel.default:#general}")
    private String defaultSlackChannel;
    
    @Value("${notification.pagerduty.integration.key:}")
    private String pagerDutyIntegrationKey;
    
    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${notification.email.smtp.host:localhost}")
    private String emailHost;
    
    @Value("${notification.email.smtp.port:587}")
    private int emailPort;
    
    @Value("${notification.email.from:noreply@zamaz-debate-mcp.com}")
    private String emailFrom;
    
    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;
    
    // Thread pool for async notifications
    private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(5);
    
    /**
     * Send a Slack notification
     */
    public CompletableFuture<Boolean> sendSlackNotification(String message, String channel, SlackMessageType type) {
        if (!notificationsEnabled || slackWebhookUrl.isEmpty()) {
            log.warn("Slack notifications are disabled or not configured");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetChannel = channel != null ? channel : defaultSlackChannel;
                
                SlackMessage slackMessage = SlackMessage.builder()
                        .channel(targetChannel)
                        .username("GitHub Integration Bot")
                        .iconEmoji(getEmojiForType(type))
                        .text(message)
                        .build();
                
                // Add attachments based on type
                if (type == SlackMessageType.ALERT) {
                    slackMessage.addAttachment(createAlertAttachment(message));
                } else if (type == SlackMessageType.SLO_VIOLATION) {
                    slackMessage.addAttachment(createSLOAttachment(message));
                }
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<SlackMessage> request = new HttpEntity<>(slackMessage, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(
                        slackWebhookUrl, request, String.class);
                
                boolean success = response.getStatusCode().is2xxSuccessful();
                
                if (success) {
                    log.debug("Slack notification sent successfully to channel: {}", targetChannel);
                    recordNotificationMetric("slack", "success");
                } else {
                    log.error("Failed to send Slack notification. Status: {}", response.getStatusCode());
                    recordNotificationMetric("slack", "error");
                }
                
                return success;
                
            } catch (Exception e) {
                log.error("Error sending Slack notification", e);
                recordNotificationMetric("slack", "error");
                return false;
            }
        }, notificationExecutor);
    }
    
    /**
     * Send a PagerDuty alert
     */
    public CompletableFuture<Boolean> sendPagerDutyAlert(String summary, String source, PagerDutyEventType eventType, String severity) {
        if (!notificationsEnabled || pagerDutyIntegrationKey.isEmpty()) {
            log.warn("PagerDuty notifications are disabled or not configured");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                PagerDutyEvent event = PagerDutyEvent.builder()
                        .integrationKey(pagerDutyIntegrationKey)
                        .eventAction(eventType.toString().toLowerCase())
                        .payload(PagerDutyPayload.builder()
                                .summary(summary)
                                .source(source)
                                .severity(severity)
                                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                .customDetails(Map.of(
                                        "service", "github-integration",
                                        "environment", "production",
                                        "dashboard_url", "http://localhost:3000/d/github-integration-comprehensive"
                                ))
                                .build())
                        .build();
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<PagerDutyEvent> request = new HttpEntity<>(event, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(
                        "https://events.pagerduty.com/v2/enqueue", request, String.class);
                
                boolean success = response.getStatusCode().is2xxSuccessful();
                
                if (success) {
                    log.debug("PagerDuty alert sent successfully for: {}", summary);
                    recordNotificationMetric("pagerduty", "success");
                } else {
                    log.error("Failed to send PagerDuty alert. Status: {}", response.getStatusCode());
                    recordNotificationMetric("pagerduty", "error");
                }
                
                return success;
                
            } catch (Exception e) {
                log.error("Error sending PagerDuty alert", e);
                recordNotificationMetric("pagerduty", "error");
                return false;
            }
        }, notificationExecutor);
    }
    
    /**
     * Send an email notification
     */
    public CompletableFuture<Boolean> sendEmailNotification(String to, String subject, String body, EmailType type) {
        if (!notificationsEnabled || !emailEnabled) {
            log.warn("Email notifications are disabled");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // This would integrate with your email service
                // For now, we'll just log the email content
                log.info("Email notification would be sent:");
                log.info("To: {}", to);
                log.info("Subject: {}", subject);
                log.info("Body: {}", body);
                
                // TODO: Implement actual email sending using JavaMail or similar
                
                recordNotificationMetric("email", "success");
                return true;
                
            } catch (Exception e) {
                log.error("Error sending email notification", e);
                recordNotificationMetric("email", "error");
                return false;
            }
        }, notificationExecutor);
    }
    
    /**
     * Send PR processing status update
     */
    public void sendPRProcessingUpdate(String repository, String prNumber, String status, String details) {
        if (!notificationsEnabled) {
            return;
        }
        
        String message = String.format(
                "📋 PR Processing Update\n" +
                "*Repository:* %s\n" +
                "*PR:* #%s\n" +
                "*Status:* %s\n" +
                "*Details:* %s",
                repository, prNumber, status, details
        );
        
        sendSlackNotification(message, "#github-integration", SlackMessageType.INFO);
    }
    
    /**
     * Send SLO violation notification
     */
    public void sendSLOViolationNotification(String sloType, double currentValue, double threshold) {
        if (!notificationsEnabled) {
            return;
        }
        
        String message = String.format(
                "🚨 SLO Violation Detected\n" +
                "*SLO Type:* %s\n" +
                "*Current Value:* %.2f\n" +
                "*Threshold:* %.2f\n" +
                "*Severity:* %s",
                sloType, currentValue, threshold,
                currentValue > threshold * 1.5 ? "Critical" : "Warning"
        );
        
        sendSlackNotification(message, "#slo-violations", SlackMessageType.SLO_VIOLATION);
        
        // If critical, also send to PagerDuty
        if (currentValue > threshold * 1.5) {
            sendPagerDutyAlert(
                    String.format("SLO Violation: %s", sloType),
                    "github-integration",
                    PagerDutyEventType.TRIGGER,
                    "critical"
            );
        }
    }
    
    /**
     * Send GitHub API rate limit warning
     */
    public void sendGitHubAPIRateLimitWarning(int remaining, int limit) {
        if (!notificationsEnabled) {
            return;
        }
        
        double percentage = (double) remaining / limit * 100;
        String message = String.format(
                "⚠️ GitHub API Rate Limit Warning\n" +
                "*Remaining:* %d/%d requests\n" +
                "*Usage:* %.1f%%\n" +
                "*Time:* %s",
                remaining, limit, 100 - percentage,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        
        sendSlackNotification(message, "#github-integration", SlackMessageType.WARNING);
    }
    
    /**
     * Send system health alert
     */
    public void sendSystemHealthAlert(String component, String status, String details) {
        if (!notificationsEnabled) {
            return;
        }
        
        String emoji = "healthy".equals(status) ? "✅" : "❌";
        String message = String.format(
                "%s System Health Alert\n" +
                "*Component:* %s\n" +
                "*Status:* %s\n" +
                "*Details:* %s",
                emoji, component, status, details
        );
        
        SlackMessageType messageType = "healthy".equals(status) ? SlackMessageType.SUCCESS : SlackMessageType.ALERT;
        sendSlackNotification(message, "#infrastructure", messageType);
    }
    
    /**
     * Helper methods
     */
    private String getEmojiForType(SlackMessageType type) {
        switch (type) {
            case ALERT:
                return ":warning:";
            case SUCCESS:
                return ":white_check_mark:";
            case WARNING:
                return ":warning:";
            case SLO_VIOLATION:
                return ":rotating_light:";
            case INFO:
            default:
                return ":information_source:";
        }
    }
    
    private SlackAttachment createAlertAttachment(String message) {
        return SlackAttachment.builder()
                .color("danger")
                .fallback("Alert notification")
                .fields(SlackField.builder()
                        .title("Alert Details")
                        .value(message)
                        .shortField(false)
                        .build())
                .build();
    }
    
    private SlackAttachment createSLOAttachment(String message) {
        return SlackAttachment.builder()
                .color("warning")
                .fallback("SLO Violation")
                .fields(SlackField.builder()
                        .title("SLO Violation Details")
                        .value(message)
                        .shortField(false)
                        .build())
                .build();
    }
    
    private void recordNotificationMetric(String channel, String status) {
        metricsService.recordSLOMetric(
                String.format("notification_%s_%s", channel, status),
                "success".equals(status) ? 1.0 : 0.0
        );
    }
    
    /**
     * Data classes for notifications
     */
    public enum SlackMessageType {
        ALERT, SUCCESS, WARNING, SLO_VIOLATION, INFO
    }
    
    public enum PagerDutyEventType {
        TRIGGER, RESOLVE
    }
    
    public enum EmailType {
        ALERT, INFO, SUMMARY
    }
    
    // Slack message classes
    public static class SlackMessage {
        private String channel;
        private String username;
        private String iconEmoji;
        private String text;
        private java.util.List<SlackAttachment> attachments;
        
        public static SlackMessageBuilder builder() {
            return new SlackMessageBuilder();
        }
        
        public void addAttachment(SlackAttachment attachment) {
            if (attachments == null) {
                attachments = new java.util.ArrayList<>();
            }
            attachments.add(attachment);
        }
        
        public static class SlackMessageBuilder {
            private String channel;
            private String username;
            private String iconEmoji;
            private String text;
            
            public SlackMessageBuilder channel(String channel) {
                this.channel = channel;
                return this;
            }
            
            public SlackMessageBuilder username(String username) {
                this.username = username;
                return this;
            }
            
            public SlackMessageBuilder iconEmoji(String iconEmoji) {
                this.iconEmoji = iconEmoji;
                return this;
            }
            
            public SlackMessageBuilder text(String text) {
                this.text = text;
                return this;
            }
            
            public SlackMessage build() {
                SlackMessage message = new SlackMessage();
                message.channel = this.channel;
                message.username = this.username;
                message.iconEmoji = this.iconEmoji;
                message.text = this.text;
                return message;
            }
        }
        
        // Getters and setters
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getIconEmoji() { return iconEmoji; }
        public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public java.util.List<SlackAttachment> getAttachments() { return attachments; }
        public void setAttachments(java.util.List<SlackAttachment> attachments) { this.attachments = attachments; }
    }
    
    public static class SlackAttachment {
        private String color;
        private String fallback;
        private java.util.List<SlackField> fields;
        
        public static SlackAttachmentBuilder builder() {
            return new SlackAttachmentBuilder();
        }
        
        public static class SlackAttachmentBuilder {
            private String color;
            private String fallback;
            private java.util.List<SlackField> fields;
            
            public SlackAttachmentBuilder color(String color) {
                this.color = color;
                return this;
            }
            
            public SlackAttachmentBuilder fallback(String fallback) {
                this.fallback = fallback;
                return this;
            }
            
            public SlackAttachmentBuilder fields(SlackField... fields) {
                this.fields = java.util.Arrays.asList(fields);
                return this;
            }
            
            public SlackAttachment build() {
                SlackAttachment attachment = new SlackAttachment();
                attachment.color = this.color;
                attachment.fallback = this.fallback;
                attachment.fields = this.fields;
                return attachment;
            }
        }
        
        // Getters and setters
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getFallback() { return fallback; }
        public void setFallback(String fallback) { this.fallback = fallback; }
        public java.util.List<SlackField> getFields() { return fields; }
        public void setFields(java.util.List<SlackField> fields) { this.fields = fields; }
    }
    
    public static class SlackField {
        private String title;
        private String value;
        private boolean shortField;
        
        public static SlackFieldBuilder builder() {
            return new SlackFieldBuilder();
        }
        
        public static class SlackFieldBuilder {
            private String title;
            private String value;
            private boolean shortField;
            
            public SlackFieldBuilder title(String title) {
                this.title = title;
                return this;
            }
            
            public SlackFieldBuilder value(String value) {
                this.value = value;
                return this;
            }
            
            public SlackFieldBuilder shortField(boolean shortField) {
                this.shortField = shortField;
                return this;
            }
            
            public SlackField build() {
                SlackField field = new SlackField();
                field.title = this.title;
                field.value = this.value;
                field.shortField = this.shortField;
                return field;
            }
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isShortField() { return shortField; }
        public void setShortField(boolean shortField) { this.shortField = shortField; }
    }
    
    // PagerDuty event classes
    public static class PagerDutyEvent {
        private String integrationKey;
        private String eventAction;
        private PagerDutyPayload payload;
        
        public static PagerDutyEventBuilder builder() {
            return new PagerDutyEventBuilder();
        }
        
        public static class PagerDutyEventBuilder {
            private String integrationKey;
            private String eventAction;
            private PagerDutyPayload payload;
            
            public PagerDutyEventBuilder integrationKey(String integrationKey) {
                this.integrationKey = integrationKey;
                return this;
            }
            
            public PagerDutyEventBuilder eventAction(String eventAction) {
                this.eventAction = eventAction;
                return this;
            }
            
            public PagerDutyEventBuilder payload(PagerDutyPayload payload) {
                this.payload = payload;
                return this;
            }
            
            public PagerDutyEvent build() {
                PagerDutyEvent event = new PagerDutyEvent();
                event.integrationKey = this.integrationKey;
                event.eventAction = this.eventAction;
                event.payload = this.payload;
                return event;
            }
        }
        
        // Getters and setters
        public String getIntegrationKey() { return integrationKey; }
        public void setIntegrationKey(String integrationKey) { this.integrationKey = integrationKey; }
        public String getEventAction() { return eventAction; }
        public void setEventAction(String eventAction) { this.eventAction = eventAction; }
        public PagerDutyPayload getPayload() { return payload; }
        public void setPayload(PagerDutyPayload payload) { this.payload = payload; }
    }
    
    public static class PagerDutyPayload {
        private String summary;
        private String source;
        private String severity;
        private String timestamp;
        private Map<String, Object> customDetails;
        
        public static PagerDutyPayloadBuilder builder() {
            return new PagerDutyPayloadBuilder();
        }
        
        public static class PagerDutyPayloadBuilder {
            private String summary;
            private String source;
            private String severity;
            private String timestamp;
            private Map<String, Object> customDetails;
            
            public PagerDutyPayloadBuilder summary(String summary) {
                this.summary = summary;
                return this;
            }
            
            public PagerDutyPayloadBuilder source(String source) {
                this.source = source;
                return this;
            }
            
            public PagerDutyPayloadBuilder severity(String severity) {
                this.severity = severity;
                return this;
            }
            
            public PagerDutyPayloadBuilder timestamp(String timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public PagerDutyPayloadBuilder customDetails(Map<String, Object> customDetails) {
                this.customDetails = customDetails;
                return this;
            }
            
            public PagerDutyPayload build() {
                PagerDutyPayload payload = new PagerDutyPayload();
                payload.summary = this.summary;
                payload.source = this.source;
                payload.severity = this.severity;
                payload.timestamp = this.timestamp;
                payload.customDetails = this.customDetails;
                return payload;
            }
        }
        
        // Getters and setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getCustomDetails() { return customDetails; }
        public void setCustomDetails(Map<String, Object> customDetails) { this.customDetails = customDetails; }
    }
}