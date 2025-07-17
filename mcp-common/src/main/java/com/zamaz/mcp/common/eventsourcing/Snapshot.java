package com.zamaz.mcp.common.eventsourcing;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a snapshot of an aggregate at a specific point in time
 */
@Data
@Builder
public class Snapshot {
    
    /**
     * The aggregate identifier
     */
    private final String aggregateId;
    
    /**
     * The version of the aggregate when this snapshot was taken
     */
    private final long version;
    
    /**
     * When the snapshot was created
     */
    private final LocalDateTime timestamp;
    
    /**
     * The serialized state of the aggregate
     */
    private final Object data;
}