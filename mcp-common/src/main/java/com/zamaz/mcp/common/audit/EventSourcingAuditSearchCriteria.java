package com.zamaz.mcp.common.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Search criteria for event sourcing-based audit trail
 */
@Data
@Builder
public class EventSourcingAuditSearchCriteria {
    
    /**
     * Organization ID to filter by
     */
    private String organizationId;
    
    /**
     * User ID to filter by
     */
    private String userId;
    
    /**
     * Specific entity ID to filter by
     */
    private String entityId;
    
    /**
     * Entity type to filter by
     */
    private String entityType;
    
    /**
     * Event type to filter by
     */
    private String eventType;
    
    /**
     * List of event types to filter by
     */
    private List<String> eventTypes;
    
    /**
     * List of aggregate types to filter by
     */
    private List<String> aggregateTypes;
    
    /**
     * Correlation ID for related events
     */
    private String correlationId;
    
    /**
     * Start date for filtering
     */
    private LocalDateTime fromDate;
    
    /**
     * End date for filtering
     */
    private LocalDateTime toDate;
    
    /**
     * Maximum number of results to return
     */
    private int limit;
    
    /**
     * Offset for pagination
     */
    private int offset;
    
    /**
     * Sort field
     */
    private String sortBy;
    
    /**
     * Sort direction
     */
    private SortDirection sortDirection;
    
    /**
     * Include payload in results
     */
    private boolean includePayload;
    
    /**
     * Include metadata in results
     */
    private boolean includeMetadata;
    
    /**
     * Sort direction enum
     */
    public enum SortDirection {
        ASC, DESC
    }
    
    /**
     * Builder with default values
     */
    public static EventSourcingAuditSearchCriteriaBuilder builder() {
        return new EventSourcingAuditSearchCriteriaBuilder()
            .limit(1000)
            .offset(0)
            .sortBy("timestamp")
            .sortDirection(SortDirection.DESC)
            .includePayload(false)
            .includeMetadata(false);
    }
}