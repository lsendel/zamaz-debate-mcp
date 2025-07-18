package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a context version snapshot.
 */
public record ContextVersion(
    UUID id,
    ContextId contextId,
    int version,
    Map<String, Object> snapshot,
    String changeSummary,
    UUID createdBy,
    LocalDateTime createdAt
) implements ValueObject {
    
    public ContextVersion {
        Objects.requireNonNull(id, "Version ID cannot be null");
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        Objects.requireNonNull(createdBy, "Created by cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        
        if (version < 1) {
            throw new IllegalArgumentException("Version must be positive");
        }
        
        snapshot = Map.copyOf(snapshot); // Make immutable
    }
    
    /**
     * Create a new context version.
     */
    public static ContextVersion create(
            ContextId contextId,
            int version,
            Map<String, Object> snapshot,
            String changeSummary,
            UUID createdBy) {
        return new ContextVersion(
            UUID.randomUUID(),
            contextId,
            version,
            snapshot,
            changeSummary,
            createdBy,
            LocalDateTime.now()
        );
    }
}