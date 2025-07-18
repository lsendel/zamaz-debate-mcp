package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;

/**
 * Value object representing debate status.
 */
public enum DebateStatus implements ValueObject {
    DRAFT("Draft"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    DebateStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if debate can be started.
     */
    public boolean canStart() {
        return this == DRAFT;
    }
    
    /**
     * Check if debate can be cancelled.
     */
    public boolean canCancel() {
        return this == DRAFT || this == ACTIVE;
    }
    
    /**
     * Check if debate is finished.
     */
    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED;
    }
}