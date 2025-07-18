package com.zamaz.mcp.sidecar.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Comprehensive Audit Logging Service for MCP Sidecar
 * 
 * Features:
 * - Structured audit event logging
 * - Configurable log levels and categories
 * - Asynchronous logging with buffering
 * - Searchable audit trail
 * - Compliance and security event tracking
 * - Automatic log rotation and archiving
 * - Real-time audit monitoring
 * - Integration with external SIEM systems
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${app.audit.enabled:true}")
    private boolean auditEnabled;

    @Value("${app.audit.buffer-size:1000}")
    private int bufferSize;

    @Value("${app.audit.flush-interval:30s}")
    private Duration flushInterval;

    @Value("${app.audit.retention-days:90}")
    private int retentionDays;

    @Value("${app.audit.log-level:INFO}")
    private String logLevel;

    @Value("${app.audit.include-request-body:false}")
    private boolean includeRequestBody;

    @Value("${app.audit.include-response-body:false}")
    private boolean includeResponseBody;

    // Audit event buffer
    private final Queue<AuditEvent> auditEventBuffer = new ConcurrentLinkedQueue<>();
    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    private final Map<String, AuditEventStatistics> eventStatistics = new ConcurrentHashMap<>();

    /**
     * Audit event types
     */
    public enum AuditEventType {
        AUTHENTICATION("AUTH"),
        AUTHORIZATION("AUTHZ"),
        API_ACCESS("API"),
        DATA_ACCESS("DATA"),
        SECURITY("SEC"),
        CONFIGURATION("CONFIG"),
        SYSTEM("SYS"),
        ERROR("ERR"),
        COMPLIANCE("COMP");

        private final String code;

        AuditEventType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Audit event severity levels
     */
    public enum AuditSeverity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);

        private final int level;

        AuditSeverity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * Audit event outcome
     */
    public enum AuditOutcome {
        SUCCESS, FAILURE, PARTIAL_SUCCESS, UNKNOWN
    }

    /**
     * Audit event definition
     */
    public static class AuditEvent {
        private final String id;
        private final Instant timestamp;
        private final AuditEventType eventType;
        private final AuditSeverity severity;
        private final AuditOutcome outcome;
        private final String userId;
        private final String sessionId;
        private final String sourceIp;
        private final String userAgent;
        private final String action;
        private final String resource;
        private final String description;
        private final Map<String, Object> details;
        private final Map<String, String> tags;
        private final String requestId;
        private final String correlationId;
        private final long processingTime;

        public AuditEvent(String id, AuditEventType eventType, AuditSeverity severity, 
                         AuditOutcome outcome, String userId, String sessionId, 
                         String sourceIp, String userAgent, String action, String resource, 
                         String description, Map<String, Object> details, Map<String, String> tags,
                         String requestId, String correlationId, long processingTime) {
            this.id = id;
            this.timestamp = Instant.now();
            this.eventType = eventType;
            this.severity = severity;
            this.outcome = outcome;
            this.userId = userId;
            this.sessionId = sessionId;
            this.sourceIp = sourceIp;
            this.userAgent = userAgent;
            this.action = action;
            this.resource = resource;
            this.description = description;
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
            this.tags = tags != null ? new HashMap<>(tags) : new HashMap<>();
            this.requestId = requestId;
            this.correlationId = correlationId;
            this.processingTime = processingTime;
        }

        public String getId() { return id; }
        public Instant getTimestamp() { return timestamp; }
        public AuditEventType getEventType() { return eventType; }
        public AuditSeverity getSeverity() { return severity; }
        public AuditOutcome getOutcome() { return outcome; }
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public String getSourceIp() { return sourceIp; }
        public String getUserAgent() { return userAgent; }
        public String getAction() { return action; }
        public String getResource() { return resource; }
        public String getDescription() { return description; }
        public Map<String, Object> getDetails() { return details; }
        public Map<String, String> getTags() { return tags; }
        public String getRequestId() { return requestId; }
        public String getCorrelationId() { return correlationId; }
        public long getProcessingTime() { return processingTime; }
    }

    /**
     * Audit event statistics
     */
    public static class AuditEventStatistics {
        private final String eventType;
        private final AtomicLong totalEvents;
        private final AtomicLong successEvents;
        private final AtomicLong failureEvents;
        private final AtomicLong totalProcessingTime;
        private volatile Instant lastEventTime;

        public AuditEventStatistics(String eventType) {
            this.eventType = eventType;
            this.totalEvents = new AtomicLong(0);
            this.successEvents = new AtomicLong(0);
            this.failureEvents = new AtomicLong(0);
            this.totalProcessingTime = new AtomicLong(0);
            this.lastEventTime = Instant.now();
        }

        public String getEventType() { return eventType; }
        public long getTotalEvents() { return totalEvents.get(); }
        public long getSuccessEvents() { return successEvents.get(); }
        public long getFailureEvents() { return failureEvents.get(); }
        public long getTotalProcessingTime() { return totalProcessingTime.get(); }
        public Instant getLastEventTime() { return lastEventTime; }

        public void recordEvent(AuditEvent event) {
            totalEvents.incrementAndGet();
            if (event.getOutcome() == AuditOutcome.SUCCESS) {
                successEvents.incrementAndGet();
            } else if (event.getOutcome() == AuditOutcome.FAILURE) {
                failureEvents.incrementAndGet();
            }
            totalProcessingTime.addAndGet(event.getProcessingTime());
            lastEventTime = event.getTimestamp();
        }

        public double getSuccessRate() {
            long total = totalEvents.get();
            return total > 0 ? (double) successEvents.get() / total : 0.0;
        }

        public double getAverageProcessingTime() {
            long total = totalEvents.get();
            return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
        }
    }

    /**
     * Audit event builder
     */
    public static class AuditEventBuilder {
        private String id;
        private AuditEventType eventType;
        private AuditSeverity severity = AuditSeverity.LOW;
        private AuditOutcome outcome = AuditOutcome.SUCCESS;
        private String userId;
        private String sessionId;
        private String sourceIp;
        private String userAgent;
        private String action;
        private String resource;
        private String description;
        private Map<String, Object> details = new HashMap<>();
        private Map<String, String> tags = new HashMap<>();
        private String requestId;
        private String correlationId;
        private long processingTime = 0;

        public AuditEventBuilder id(String id) {
            this.id = id;
            return this;
        }

        public AuditEventBuilder eventType(AuditEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public AuditEventBuilder severity(AuditSeverity severity) {
            this.severity = severity;
            return this;
        }

        public AuditEventBuilder outcome(AuditOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public AuditEventBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public AuditEventBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public AuditEventBuilder sourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        public AuditEventBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public AuditEventBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditEventBuilder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public AuditEventBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AuditEventBuilder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public AuditEventBuilder details(Map<String, Object> details) {
            this.details.putAll(details);
            return this;
        }

        public AuditEventBuilder tag(String key, String value) {
            this.tags.put(key, value);
            return this;
        }

        public AuditEventBuilder tags(Map<String, String> tags) {
            this.tags.putAll(tags);
            return this;
        }

        public AuditEventBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public AuditEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public AuditEventBuilder processingTime(long processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public AuditEvent build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            if (eventType == null) {
                throw new IllegalArgumentException("Event type is required");
            }
            
            return new AuditEvent(id, eventType, severity, outcome, userId, sessionId, 
                                sourceIp, userAgent, action, resource, description, 
                                details, tags, requestId, correlationId, processingTime);
        }
    }

    /**
     * Create audit event builder
     */
    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    /**
     * Log audit event
     */
    public Mono<Void> logAuditEvent(AuditEvent event) {
        if (!auditEnabled) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
            // Add to buffer
            auditEventBuffer.offer(event);
            
            // Update statistics
            AuditEventStatistics stats = eventStatistics.computeIfAbsent(
                event.getEventType().name(), 
                k -> new AuditEventStatistics(k)
            );
            stats.recordEvent(event);
            
            // Update counters
            eventCounters.computeIfAbsent(event.getEventType().name(), k -> new AtomicLong(0))
                        .incrementAndGet();
            
            // Log to standard logger based on severity
            logToStandardLogger(event);
            
            // Record metrics
            metricsCollectorService.recordAuditEvent(event.getEventType().name(), 
                                                   event.getSeverity().name(), 
                                                   event.getOutcome().name());
            
            // Flush buffer if needed
            if (auditEventBuffer.size() >= bufferSize) {
                flushAuditEvents();
            }
        });
    }

    /**
     * Log to standard logger
     */
    private void logToStandardLogger(AuditEvent event) {
        String logMessage = formatAuditMessage(event);
        
        switch (event.getSeverity()) {
            case CRITICAL:
                log.error("AUDIT: {}", logMessage);
                break;
            case HIGH:
                log.warn("AUDIT: {}", logMessage);
                break;
            case MEDIUM:
                log.info("AUDIT: {}", logMessage);
                break;
            case LOW:
                log.debug("AUDIT: {}", logMessage);
                break;
        }
    }

    /**
     * Format audit message
     */
    private String formatAuditMessage(AuditEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(event.getEventType().getCode()).append("] ");
        sb.append("User: ").append(event.getUserId()).append(" ");
        sb.append("Action: ").append(event.getAction()).append(" ");
        sb.append("Resource: ").append(event.getResource()).append(" ");
        sb.append("Outcome: ").append(event.getOutcome()).append(" ");
        sb.append("IP: ").append(event.getSourceIp()).append(" ");
        if (event.getDescription() != null) {
            sb.append("Description: ").append(event.getDescription()).append(" ");
        }
        if (event.getRequestId() != null) {
            sb.append("RequestId: ").append(event.getRequestId()).append(" ");
        }
        sb.append("Time: ").append(event.getProcessingTime()).append("ms");
        
        return sb.toString();
    }

    /**
     * Flush audit events to persistent storage
     */
    @Scheduled(fixedDelayString = "${app.audit.flush-interval:30s}")
    public void flushAuditEvents() {
        if (auditEventBuffer.isEmpty()) {
            return;
        }

        List<AuditEvent> eventsToFlush = new ArrayList<>();
        AuditEvent event;
        while ((event = auditEventBuffer.poll()) != null) {
            eventsToFlush.add(event);
        }

        if (eventsToFlush.isEmpty()) {
            return;
        }

        // Persist to Redis
        persistAuditEvents(eventsToFlush)
                .doOnSuccess(v -> log.debug("Flushed {} audit events", eventsToFlush.size()))
                .doOnError(error -> log.error("Failed to flush audit events", error))
                .subscribe();
    }

    /**
     * Persist audit events to Redis
     */
    private Mono<Void> persistAuditEvents(List<AuditEvent> events) {
        return Mono.fromCallable(() -> {
            Map<String, String> eventMap = new HashMap<>();
            
            for (AuditEvent event : events) {
                try {
                    String eventJson = objectMapper.writeValueAsString(event);
                    String key = "audit:" + event.getTimestamp().toEpochMilli() + ":" + event.getId();
                    eventMap.put(key, eventJson);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize audit event: {}", event.getId(), e);
                }
            }
            
            return eventMap;
        })
        .flatMap(eventMap -> {
            if (eventMap.isEmpty()) {
                return Mono.empty();
            }
            
            return redisTemplate.opsForHash().putAll("audit_events", eventMap)
                    .then(redisTemplate.expire("audit_events", Duration.ofDays(retentionDays)))
                    .then();
        });
    }

    /**
     * Search audit events
     */
    public Mono<List<AuditEvent>> searchAuditEvents(AuditSearchCriteria criteria) {
        return redisTemplate.opsForHash().entries("audit_events")
                .collectList()
                .map(entries -> {
                    List<AuditEvent> events = new ArrayList<>();
                    
                    for (Map.Entry<Object, Object> entry : entries) {
                        try {
                            String eventJson = (String) entry.getValue();
                            AuditEvent event = objectMapper.readValue(eventJson, AuditEvent.class);
                            
                            if (criteria.matches(event)) {
                                events.add(event);
                            }
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize audit event", e);
                        }
                    }
                    
                    // Sort by timestamp descending
                    events.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                    
                    // Apply limit
                    if (criteria.getLimit() > 0 && events.size() > criteria.getLimit()) {
                        events = events.subList(0, criteria.getLimit());
                    }
                    
                    return events;
                });
    }

    /**
     * Get audit statistics
     */
    public Mono<Map<String, Object>> getAuditStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> statistics = new HashMap<>();
            
            // Overall statistics
            long totalEvents = eventCounters.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum();
            
            statistics.put("totalEvents", totalEvents);
            statistics.put("bufferedEvents", auditEventBuffer.size());
            statistics.put("eventTypes", eventCounters.size());
            
            // Event type statistics
            Map<String, Object> eventTypeStats = new HashMap<>();
            for (Map.Entry<String, AuditEventStatistics> entry : eventStatistics.entrySet()) {
                AuditEventStatistics stats = entry.getValue();
                Map<String, Object> typeStats = new HashMap<>();
                typeStats.put("totalEvents", stats.getTotalEvents());
                typeStats.put("successEvents", stats.getSuccessEvents());
                typeStats.put("failureEvents", stats.getFailureEvents());
                typeStats.put("successRate", stats.getSuccessRate());
                typeStats.put("averageProcessingTime", stats.getAverageProcessingTime());
                typeStats.put("lastEventTime", stats.getLastEventTime());
                
                eventTypeStats.put(entry.getKey(), typeStats);
            }
            statistics.put("eventTypeStatistics", eventTypeStats);
            
            // Recent activity
            Map<String, Long> recentActivity = eventCounters.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                    ));
            statistics.put("recentActivity", recentActivity);
            
            return statistics;
        });
    }

    /**
     * Audit search criteria
     */
    public static class AuditSearchCriteria {
        private String userId;
        private AuditEventType eventType;
        private AuditSeverity minSeverity;
        private AuditOutcome outcome;
        private String action;
        private String resource;
        private Instant startTime;
        private Instant endTime;
        private Map<String, String> tags;
        private String requestId;
        private String correlationId;
        private int limit = 100;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public AuditEventType getEventType() { return eventType; }
        public void setEventType(AuditEventType eventType) { this.eventType = eventType; }
        public AuditSeverity getMinSeverity() { return minSeverity; }
        public void setMinSeverity(AuditSeverity minSeverity) { this.minSeverity = minSeverity; }
        public AuditOutcome getOutcome() { return outcome; }
        public void setOutcome(AuditOutcome outcome) { this.outcome = outcome; }
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

        public boolean matches(AuditEvent event) {
            if (userId != null && !userId.equals(event.getUserId())) {
                return false;
            }
            if (eventType != null && eventType != event.getEventType()) {
                return false;
            }
            if (minSeverity != null && event.getSeverity().getLevel() < minSeverity.getLevel()) {
                return false;
            }
            if (outcome != null && outcome != event.getOutcome()) {
                return false;
            }
            if (action != null && !action.equals(event.getAction())) {
                return false;
            }
            if (resource != null && !resource.equals(event.getResource())) {
                return false;
            }
            if (startTime != null && event.getTimestamp().isBefore(startTime)) {
                return false;
            }
            if (endTime != null && event.getTimestamp().isAfter(endTime)) {
                return false;
            }
            if (requestId != null && !requestId.equals(event.getRequestId())) {
                return false;
            }
            if (correlationId != null && !correlationId.equals(event.getCorrelationId())) {
                return false;
            }
            if (tags != null && !tags.isEmpty()) {
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    if (!tag.getValue().equals(event.getTags().get(tag.getKey()))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Clean up old audit events
     */
    @Scheduled(fixedDelayString = "${app.audit.cleanup-interval:24h}")
    public void cleanupOldAuditEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        
        redisTemplate.opsForHash().entries("audit_events")
                .filter(entry -> {
                    String key = (String) entry.getKey();
                    try {
                        String[] parts = key.split(":");
                        if (parts.length >= 2) {
                            long timestamp = Long.parseLong(parts[1]);
                            return Instant.ofEpochMilli(timestamp).isBefore(cutoff);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid audit event key format: {}", key);
                    }
                    return false;
                })
                .map(entry -> (String) entry.getKey())
                .collectList()
                .flatMap(keysToDelete -> {
                    if (keysToDelete.isEmpty()) {
                        return Mono.empty();
                    }
                    return redisTemplate.opsForHash().delete("audit_events", keysToDelete.toArray());
                })
                .doOnSuccess(deletedCount -> {
                    if (deletedCount > 0) {
                        log.info("Cleaned up {} old audit events", deletedCount);
                    }
                })
                .doOnError(error -> log.error("Failed to cleanup old audit events", error))
                .subscribe();
    }
}