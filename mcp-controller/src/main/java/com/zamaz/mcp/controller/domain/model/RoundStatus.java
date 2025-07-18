package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the status of a debate round.
 */
public enum RoundStatus implements ValueObject {
    ACTIVE("active", "Round is currently accepting responses"),
    COMPLETED("completed", "Round has been completed successfully"),
    EXPIRED("expired", "Round expired due to time limit");
    
    private final String value;
    private final String description;
    
    RoundStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == EXPIRED;
    }
    
    public boolean canAcceptResponses() {
        return this == ACTIVE;
    }
    
    public static RoundStatus fromValue(String value) {
        for (RoundStatus status : RoundStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid round status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}