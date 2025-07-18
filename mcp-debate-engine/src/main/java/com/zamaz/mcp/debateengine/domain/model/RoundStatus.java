package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;

/**
 * Value object representing round status.
 */
public enum RoundStatus implements ValueObject {
    PENDING("Waiting to start"),
    ACTIVE("Currently in progress"),
    COMPLETED("Successfully completed"),
    TIMEOUT("Timed out");
    
    private final String description;
    
    RoundStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if round can be started.
     */
    public boolean canStart() {
        return this == PENDING;
    }
    
    /**
     * Check if round is active.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Check if round is finished.
     */
    public boolean isFinished() {
        return this == COMPLETED || this == TIMEOUT;
    }
}