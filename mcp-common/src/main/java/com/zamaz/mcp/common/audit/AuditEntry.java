package com.zamaz.mcp.common.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an audit entry for event sourcing-based audit trail
 */
@Data
@Builder
public class AuditEntry {
    
    /**
     * Unique event identifier
     */
    private UUID eventId;
    
    /**
     * Type of the event
     */
    private String eventType;
    
    /**
     * ID of the aggregate that was affected
     */
    private String aggregateId;
    
    /**
     * Type of the aggregate (e.g., user, debate, organization)
     */
    private String aggregateType;
    
    /**
     * Version of the aggregate after this event
     */
    private long version;
    
    /**
     * When the event occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * User who performed the action
     */
    private String userId;
    
    /**
     * Organization context
     */
    private String organizationId;
    
    /**
     * Correlation ID for related events
     */
    private String correlationId;
    
    /**
     * Event payload (the actual data)
     */
    private Object payload;
    
    /**
     * Additional metadata
     */
    private Object metadata;
    
    /**
     * Human-readable action extracted from event type
     */
    private String action;
    
    /**
     * Human-readable description of what happened
     */
    private String description;
}