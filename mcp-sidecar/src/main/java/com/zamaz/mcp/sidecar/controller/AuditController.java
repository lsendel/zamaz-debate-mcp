package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.AuditLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Audit Logging Controller for MCP Sidecar
 * 
 * Provides REST endpoints for audit logging and monitoring
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditLoggingService auditLoggingService;

    /**
     * Create audit event
     */
    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> createAuditEvent(
            @RequestBody CreateAuditEventRequest request) {
        
        try {
            AuditLoggingService.AuditEvent event = AuditLoggingService.builder()
                    .eventType(AuditLoggingService.AuditEventType.valueOf(request.getEventType().toUpperCase()))
                    .severity(AuditLoggingService.AuditSeverity.valueOf(request.getSeverity().toUpperCase()))
                    .outcome(AuditLoggingService.AuditOutcome.valueOf(request.getOutcome().toUpperCase()))
                    .userId(request.getUserId())
                    .sessionId(request.getSessionId())
                    .sourceIp(request.getSourceIp())
                    .userAgent(request.getUserAgent())
                    .action(request.getAction())
                    .resource(request.getResource())
                    .description(request.getDescription())
                    .details(request.getDetails())
                    .tags(request.getTags())
                    .requestId(request.getRequestId())
                    .correlationId(request.getCorrelationId())
                    .processingTime(request.getProcessingTime())
                    .build();
            
            return auditLoggingService.logAuditEvent(event)
                    .then(Mono.just(ResponseEntity.ok(Map.of(
                        "status", "Audit event created",
                        "eventId", event.getId(),
                        "timestamp", event.getTimestamp().toString()
                    ))))
                    .doOnSuccess(response -> log.info("Audit event created via API: {}", event.getId()))
                    .onErrorResume(error -> {
                        log.error("Error creating audit event: {}", error.getMessage());
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid audit event parameters: {}", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * Search audit events
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDIT') or hasRole('COMPLIANCE')")
    public Mono<ResponseEntity<List<AuditLoggingService.AuditEvent>>> searchAuditEvents(
            @RequestBody AuditSearchRequest request) {
        
        AuditLoggingService.AuditSearchCriteria criteria = new AuditLoggingService.AuditSearchCriteria();
        criteria.setUserId(request.getUserId());
        
        if (request.getEventType() != null) {
            try {
                criteria.setEventType(AuditLoggingService.AuditEventType.valueOf(request.getEventType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid event type: {}", request.getEventType());
                return Mono.just(ResponseEntity.badRequest().build());
            }
        }
        
        if (request.getMinSeverity() != null) {
            try {
                criteria.setMinSeverity(AuditLoggingService.AuditSeverity.valueOf(request.getMinSeverity().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid severity: {}", request.getMinSeverity());
                return Mono.just(ResponseEntity.badRequest().build());
            }
        }
        
        if (request.getOutcome() != null) {
            try {
                criteria.setOutcome(AuditLoggingService.AuditOutcome.valueOf(request.getOutcome().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid outcome: {}", request.getOutcome());
                return Mono.just(ResponseEntity.badRequest().build());
            }
        }
        
        criteria.setAction(request.getAction());
        criteria.setResource(request.getResource());
        criteria.setStartTime(request.getStartTime());
        criteria.setEndTime(request.getEndTime());
        criteria.setTags(request.getTags());
        criteria.setRequestId(request.getRequestId());
        criteria.setCorrelationId(request.getCorrelationId());
        criteria.setLimit(request.getLimit() > 0 ? request.getLimit() : 100);
        
        return auditLoggingService.searchAuditEvents(criteria)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Audit events search performed"))
                .onErrorResume(error -> {
                    log.error("Error searching audit events: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get audit statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDIT') or hasRole('COMPLIANCE') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getAuditStatistics() {
        return auditLoggingService.getAuditStatistics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Audit statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting audit statistics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get audit event types
     */
    @GetMapping("/event-types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDIT') or hasRole('COMPLIANCE') or hasRole('MONITOR')")
    public ResponseEntity<List<String>> getAuditEventTypes() {
        List<String> eventTypes = java.util.Arrays.stream(AuditLoggingService.AuditEventType.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(eventTypes);
    }

    /**
     * Get audit severities
     */
    @GetMapping("/severities")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDIT') or hasRole('COMPLIANCE') or hasRole('MONITOR')")
    public ResponseEntity<List<String>> getAuditSeverities() {
        List<String> severities = java.util.Arrays.stream(AuditLoggingService.AuditSeverity.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(severities);
    }

    /**
     * Get audit outcomes
     */
    @GetMapping("/outcomes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDIT') or hasRole('COMPLIANCE') or hasRole('MONITOR')")
    public ResponseEntity<List<String>> getAuditOutcomes() {
        List<String> outcomes = java.util.Arrays.stream(AuditLoggingService.AuditOutcome.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(outcomes);
    }

    /**
     * Flush audit events
     */
    @PostMapping("/flush")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> flushAuditEvents() {
        return Mono.fromRunnable(() -> auditLoggingService.flushAuditEvents())
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Audit events flushed",
                    "timestamp", Instant.now().toString()
                ))))
                .doOnSuccess(response -> log.info("Audit events flushed via API"))
                .onErrorResume(error -> {
                    log.error("Error flushing audit events: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getAuditHealth() {
        return auditLoggingService.getAuditStatistics()
                .map(statistics -> {
                    long totalEvents = (Long) statistics.get("totalEvents");
                    int bufferedEvents = (Integer) statistics.get("bufferedEvents");
                    
                    Map<String, Object> health = Map.of(
                        "status", "UP",
                        "totalEvents", totalEvents,
                        "bufferedEvents", bufferedEvents,
                        "bufferStatus", bufferedEvents > 1000 ? "HIGH" : "NORMAL"
                    );
                    
                    return ResponseEntity.ok(health);
                })
                .doOnSuccess(response -> log.debug("Audit health check requested"))
                .onErrorResume(error -> {
                    log.error("Error getting audit health: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Request DTOs
     */
    public static class CreateAuditEventRequest {
        private String eventType;
        private String severity = "LOW";
        private String outcome = "SUCCESS";
        private String userId;
        private String sessionId;
        private String sourceIp;
        private String userAgent;
        private String action;
        private String resource;
        private String description;
        private Map<String, Object> details = new java.util.HashMap<>();
        private Map<String, String> tags = new java.util.HashMap<>();
        private String requestId;
        private String correlationId;
        private long processingTime = 0;
        
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getOutcome() { return outcome; }
        public void setOutcome(String outcome) { this.outcome = outcome; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getSourceIp() { return sourceIp; }
        public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        public Map<String, String> getTags() { return tags; }
        public void setTags(Map<String, String> tags) { this.tags = tags; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    }

    public static class AuditSearchRequest {
        private String userId;
        private String eventType;
        private String minSeverity;
        private String outcome;
        private String action;
        private String resource;
        private Instant startTime;
        private Instant endTime;
        private Map<String, String> tags = new java.util.HashMap<>();
        private String requestId;
        private String correlationId;
        private int limit = 100;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getMinSeverity() { return minSeverity; }
        public void setMinSeverity(String minSeverity) { this.minSeverity = minSeverity; }
        public String getOutcome() { return outcome; }
        public void setOutcome(String outcome) { this.outcome = outcome; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public Map<String, String> getTags() { return tags; }
        public void setTags(Map<String, String> tags) { this.tags = tags; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
}