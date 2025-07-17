package com.zamaz.mcp.common.audit;

import com.zamaz.mcp.common.eventsourcing.Event;
import com.zamaz.mcp.common.eventsourcing.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for audit trail operations using event sourcing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSourcingAuditService {
    
    private final EventStore eventStore;
    
    /**
     * Get audit trail for a specific entity
     */
    public CompletableFuture<List<AuditEntry>> getAuditTrail(String entityId, String entityType, 
                                                           LocalDateTime fromDate, LocalDateTime toDate) {
        log.debug("Getting audit trail for entity {} of type {} from {} to {}", 
            entityId, entityType, fromDate, toDate);
        
        return eventStore.getEvents(entityId)
            .thenApply(events -> events.stream()
                .filter(event -> isWithinDateRange(event, fromDate, toDate))
                .map(this::eventToAuditEntry)
                .collect(Collectors.toList()))
            .thenApply(auditEntries -> {
                log.debug("Retrieved {} audit entries for entity {}", auditEntries.size(), entityId);
                return auditEntries;
            });
    }
    
    /**
     * Get audit trail for an organization
     */
    public CompletableFuture<List<AuditEntry>> getOrganizationAuditTrail(String organizationId, 
                                                                        LocalDateTime fromDate, LocalDateTime toDate, 
                                                                        int limit) {
        log.debug("Getting organization audit trail for {} from {} to {} with limit {}", 
            organizationId, fromDate, toDate, limit);
        
        return eventStore.getEventsByOrganization(organizationId, fromDate, toDate, limit)
            .thenApply(events -> events.stream()
                .map(this::eventToAuditEntry)
                .collect(Collectors.toList()))
            .thenApply(auditEntries -> {
                log.debug("Retrieved {} audit entries for organization {}", auditEntries.size(), organizationId);
                return auditEntries;
            });
    }
    
    /**
     * Get audit trail for a specific user
     */
    public CompletableFuture<List<AuditEntry>> getUserAuditTrail(String userId, 
                                                               LocalDateTime fromDate, LocalDateTime toDate, 
                                                               int limit) {
        log.debug("Getting user audit trail for {} from {} to {} with limit {}", 
            userId, fromDate, toDate, limit);
        
        return eventStore.getEventsByUser(userId, fromDate, toDate, limit)
            .thenApply(events -> events.stream()
                .map(this::eventToAuditEntry)
                .collect(Collectors.toList()))
            .thenApply(auditEntries -> {
                log.debug("Retrieved {} audit entries for user {}", auditEntries.size(), userId);
                return auditEntries;
            });
    }
    
    /**
     * Get audit trail by event type
     */
    public CompletableFuture<List<AuditEntry>> getAuditTrailByEventType(String eventType, 
                                                                       LocalDateTime fromDate, LocalDateTime toDate, 
                                                                       int limit) {
        log.debug("Getting audit trail by event type {} from {} to {} with limit {}", 
            eventType, fromDate, toDate, limit);
        
        return eventStore.getEventsByType(eventType, fromDate, toDate, limit)
            .thenApply(events -> events.stream()
                .map(this::eventToAuditEntry)
                .collect(Collectors.toList()))
            .thenApply(auditEntries -> {
                log.debug("Retrieved {} audit entries for event type {}", auditEntries.size(), eventType);
                return auditEntries;
            });
    }
    
    /**
     * Get related events by correlation ID
     */
    public CompletableFuture<List<AuditEntry>> getRelatedEvents(String correlationId) {
        log.debug("Getting related events for correlation ID {}", correlationId);
        
        return eventStore.getEventsByCorrelationId(correlationId)
            .thenApply(events -> events.stream()
                .map(this::eventToAuditEntry)
                .collect(Collectors.toList()))
            .thenApply(auditEntries -> {
                log.debug("Retrieved {} related audit entries for correlation ID {}", 
                    auditEntries.size(), correlationId);
                return auditEntries;
            });
    }
    
    /**
     * Search audit trail with filters
     */
    public CompletableFuture<List<AuditEntry>> searchAuditTrail(EventSourcingAuditSearchCriteria criteria) {
        log.debug("Searching audit trail with criteria: {}", criteria);
        
        CompletableFuture<List<Event>> eventsFuture;
        
        if (criteria.getEntityId() != null) {
            eventsFuture = eventStore.getEvents(criteria.getEntityId());
        } else if (criteria.getOrganizationId() != null) {
            eventsFuture = eventStore.getEventsByOrganization(
                criteria.getOrganizationId(), 
                criteria.getFromDate(), 
                criteria.getToDate(), 
                criteria.getLimit()
            );
        } else if (criteria.getUserId() != null) {
            eventsFuture = eventStore.getEventsByUser(
                criteria.getUserId(), 
                criteria.getFromDate(), 
                criteria.getToDate(), 
                criteria.getLimit()
            );
        } else if (criteria.getEventType() != null) {
            eventsFuture = eventStore.getEventsByType(
                criteria.getEventType(), 
                criteria.getFromDate(), 
                criteria.getToDate(), 
                criteria.getLimit()
            );
        } else if (criteria.getCorrelationId() != null) {
            eventsFuture = eventStore.getEventsByCorrelationId(criteria.getCorrelationId());
        } else {
            // Default to organization-wide search if no specific criteria
            eventsFuture = CompletableFuture.completedFuture(List.of());
        }
        
        return eventsFuture
            .thenApply(events -> events.stream()
                .filter(event -> matchesCriteria(event, criteria))
                .skip(criteria.getOffset())
                .limit(criteria.getLimit())
                .map(event -> eventToAuditEntry(event, criteria))
                .collect(Collectors.toList()))
            .thenApply(auditEntries -> {
                log.debug("Retrieved {} audit entries matching criteria", auditEntries.size());
                return auditEntries;
            });
    }
    
    /**
     * Get audit statistics
     */
    public CompletableFuture<EventSourcingAuditStatistics> getAuditStatistics(String organizationId, 
                                                                             LocalDateTime fromDate, 
                                                                             LocalDateTime toDate) {
        log.debug("Getting audit statistics for organization {} from {} to {}", 
            organizationId, fromDate, toDate);
        
        return eventStore.getEventsByOrganization(organizationId, fromDate, toDate, Integer.MAX_VALUE)
            .thenApply(events -> {
                EventSourcingAuditStatistics.EventSourcingAuditStatisticsBuilder statsBuilder = 
                    EventSourcingAuditStatistics.builder()
                        .totalEvents(events.size())
                        .fromDate(fromDate)
                        .toDate(toDate)
                        .organizationId(organizationId);
                
                // Calculate statistics
                Map<String, Integer> eventsByType = events.stream()
                    .collect(Collectors.groupingBy(Event::getEventType, 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
                
                Map<String, Integer> eventsByUser = events.stream()
                    .filter(event -> event.getUserId() != null)
                    .collect(Collectors.groupingBy(Event::getUserId, 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
                
                Map<String, Integer> eventsByAggregateType = events.stream()
                    .collect(Collectors.groupingBy(Event::getAggregateType, 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
                
                Map<Integer, Integer> eventsByHour = events.stream()
                    .collect(Collectors.groupingBy(event -> event.getTimestamp().getHour(), 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
                
                Map<Integer, Integer> eventsByDayOfWeek = events.stream()
                    .collect(Collectors.groupingBy(event -> event.getTimestamp().getDayOfWeek().getValue(), 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
                
                // Top users (limit to 10)
                Map<String, Integer> topUsers = eventsByUser.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                    ));
                
                // Top event types (limit to 10)
                Map<String, Integer> topEventTypes = eventsByType.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                    ));
                
                // Calculate additional statistics
                int uniqueUsers = eventsByUser.size();
                int uniqueCorrelationIds = (int) events.stream()
                    .map(Event::getCorrelationId)
                    .filter(id -> id != null)
                    .distinct()
                    .count();
                
                double averageEventsPerDay = calculateAverageEventsPerDay(events.size(), fromDate, toDate);
                int peakEventsPerHour = eventsByHour.values().stream()
                    .max(Integer::compareTo)
                    .orElse(0);
                
                EventSourcingAuditStatistics stats = statsBuilder
                    .eventsByType(eventsByType)
                    .eventsByUser(eventsByUser)
                    .eventsByAggregateType(eventsByAggregateType)
                    .eventsByHour(eventsByHour)
                    .eventsByDayOfWeek(eventsByDayOfWeek)
                    .topUsers(topUsers)
                    .topEventTypes(topEventTypes)
                    .uniqueUsers(uniqueUsers)
                    .uniqueCorrelationIds(uniqueCorrelationIds)
                    .averageEventsPerDay(averageEventsPerDay)
                    .peakEventsPerHour(peakEventsPerHour)
                    .build();
                
                log.debug("Generated audit statistics: {} total events, {} event types, {} users", 
                    stats.getTotalEvents(), stats.getEventsByType().size(), stats.getUniqueUsers());
                
                return stats;
            });
    }
    
    /**
     * Export audit trail to different formats
     */
    public CompletableFuture<String> exportAuditTrail(AuditExportRequest request) {
        log.debug("Exporting audit trail with format {} for criteria: {}", 
            request.getFormat(), request.getCriteria());
        
        // Convert regular criteria to event sourcing criteria
        EventSourcingAuditSearchCriteria eventSourcingCriteria = EventSourcingAuditSearchCriteria.builder()
            .organizationId(request.getCriteria().getOrganizationId())
            .userId(request.getCriteria().getUserId())
            .eventType(request.getCriteria().getEventType() != null ? 
                request.getCriteria().getEventType().name() : null)
            .fromDate(request.getCriteria().getFromDate())
            .toDate(request.getCriteria().getToDate())
            .includePayload(request.isIncludeSensitiveData())
            .includeMetadata(request.isIncludeSensitiveData())
            .build();
        
        return searchAuditTrail(eventSourcingCriteria)
            .thenApply(auditEntries -> {
                switch (request.getFormat()) {
                    case CSV:
                        return exportToCsv(auditEntries);
                    case JSON:
                        return exportToJson(auditEntries);
                    case XML:
                        return exportToXml(auditEntries);
                    default:
                        throw new IllegalArgumentException("Unsupported export format: " + request.getFormat());
                }
            })
            .thenApply(exportData -> {
                log.info("Exported {} audit entries to {} format", 
                    exportData.split("\n").length, request.getFormat());
                return exportData;
            });
    }
    
    private AuditEntry eventToAuditEntry(Event event) {
        return eventToAuditEntry(event, null);
    }
    
    private AuditEntry eventToAuditEntry(Event event, EventSourcingAuditSearchCriteria criteria) {
        AuditEntry.AuditEntryBuilder builder = AuditEntry.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .aggregateId(event.getAggregateId())
            .aggregateType(event.getAggregateType())
            .version(event.getVersion())
            .timestamp(event.getTimestamp())
            .userId(event.getUserId())
            .organizationId(event.getOrganizationId())
            .correlationId(event.getCorrelationId())
            .action(extractAction(event.getEventType()))
            .description(generateDescription(event));
        
        // Include payload and metadata based on criteria
        if (criteria == null || criteria.isIncludePayload()) {
            builder.payload(event.getPayload());
        }
        
        if (criteria == null || criteria.isIncludeMetadata()) {
            builder.metadata(event.getMetadata());
        }
        
        return builder.build();
    }
    
    private boolean isWithinDateRange(Event event, LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime eventTime = event.getTimestamp();
        return (fromDate == null || eventTime.isAfter(fromDate) || eventTime.isEqual(fromDate)) &&
               (toDate == null || eventTime.isBefore(toDate) || eventTime.isEqual(toDate));
    }
    
    private boolean matchesCriteria(Event event, EventSourcingAuditSearchCriteria criteria) {
        if (criteria.getEventTypes() != null && !criteria.getEventTypes().isEmpty()) {
            if (!criteria.getEventTypes().contains(event.getEventType())) {
                return false;
            }
        }
        
        if (criteria.getAggregateTypes() != null && !criteria.getAggregateTypes().isEmpty()) {
            if (!criteria.getAggregateTypes().contains(event.getAggregateType())) {
                return false;
            }
        }
        
        if (criteria.getEntityType() != null && !criteria.getEntityType().equals(event.getAggregateType())) {
            return false;
        }
        
        return isWithinDateRange(event, criteria.getFromDate(), criteria.getToDate());
    }
    
    private String extractAction(String eventType) {
        String[] parts = eventType.split("\\.");
        return parts.length > 1 ? parts[1] : eventType;
    }
    
    private String generateDescription(Event event) {
        String action = extractAction(event.getEventType());
        return String.format("%s %s %s", 
            event.getAggregateType(), 
            event.getAggregateId(), 
            action);
    }
    
    private double calculateAverageEventsPerDay(int totalEvents, LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate == null || toDate == null || totalEvents == 0) {
            return 0.0;
        }
        
        long days = java.time.Duration.between(fromDate, toDate).toDays();
        if (days == 0) {
            return totalEvents;
        }
        
        return (double) totalEvents / days;
    }
    
    private String exportToCsv(List<AuditEntry> auditEntries) {
        StringBuilder csv = new StringBuilder();
        csv.append("Event ID,Event Type,Aggregate ID,Aggregate Type,Version,Timestamp,User ID,Organization ID,Correlation ID,Action,Description\n");
        
        for (AuditEntry entry : auditEntries) {
            csv.append(String.format("%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,\"%s\"\n",
                entry.getEventId(),
                entry.getEventType(),
                entry.getAggregateId(),
                entry.getAggregateType(),
                entry.getVersion(),
                entry.getTimestamp(),
                entry.getUserId(),
                entry.getOrganizationId(),
                entry.getCorrelationId(),
                entry.getAction(),
                entry.getDescription()));
        }
        
        return csv.toString();
    }
    
    private String exportToJson(List<AuditEntry> auditEntries) {
        // Simple JSON export - in a real implementation, use ObjectMapper
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < auditEntries.size(); i++) {
            AuditEntry entry = auditEntries.get(i);
            json.append("  {\n");
            json.append(String.format("    \"eventId\": \"%s\",\n", entry.getEventId()));
            json.append(String.format("    \"eventType\": \"%s\",\n", entry.getEventType()));
            json.append(String.format("    \"aggregateId\": \"%s\",\n", entry.getAggregateId()));
            json.append(String.format("    \"aggregateType\": \"%s\",\n", entry.getAggregateType()));
            json.append(String.format("    \"version\": %d,\n", entry.getVersion()));
            json.append(String.format("    \"timestamp\": \"%s\",\n", entry.getTimestamp()));
            json.append(String.format("    \"userId\": \"%s\",\n", entry.getUserId()));
            json.append(String.format("    \"organizationId\": \"%s\",\n", entry.getOrganizationId()));
            json.append(String.format("    \"correlationId\": \"%s\",\n", entry.getCorrelationId()));
            json.append(String.format("    \"action\": \"%s\",\n", entry.getAction()));
            json.append(String.format("    \"description\": \"%s\"\n", entry.getDescription()));
            json.append("  }");
            
            if (i < auditEntries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        return json.toString();
    }
    
    private String exportToXml(List<AuditEntry> auditEntries) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<auditTrail>\n");
        
        for (AuditEntry entry : auditEntries) {
            xml.append("  <auditEntry>\n");
            xml.append(String.format("    <eventId>%s</eventId>\n", entry.getEventId()));
            xml.append(String.format("    <eventType>%s</eventType>\n", entry.getEventType()));
            xml.append(String.format("    <aggregateId>%s</aggregateId>\n", entry.getAggregateId()));
            xml.append(String.format("    <aggregateType>%s</aggregateType>\n", entry.getAggregateType()));
            xml.append(String.format("    <version>%d</version>\n", entry.getVersion()));
            xml.append(String.format("    <timestamp>%s</timestamp>\n", entry.getTimestamp()));
            xml.append(String.format("    <userId>%s</userId>\n", entry.getUserId()));
            xml.append(String.format("    <organizationId>%s</organizationId>\n", entry.getOrganizationId()));
            xml.append(String.format("    <correlationId>%s</correlationId>\n", entry.getCorrelationId()));
            xml.append(String.format("    <action>%s</action>\n", entry.getAction()));
            xml.append(String.format("    <description>%s</description>\n", entry.getDescription()));
            xml.append("  </auditEntry>\n");
        }
        
        xml.append("</auditTrail>");
        return xml.toString();
    }
}