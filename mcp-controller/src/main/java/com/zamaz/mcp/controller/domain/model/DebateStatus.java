package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the lifecycle status of a Debate.
 */
public enum DebateStatus implements ValueObject {
    CREATED("created", "Debate created but not yet initialized"),
    INITIALIZED("initialized", "Debate initialized with participants"),
    IN_PROGRESS("in_progress", "Debate is actively running"),
    COMPLETED("completed", "Debate has finished successfully"),
    ARCHIVED("archived", "Debate has been archived"),
    CANCELLED("cancelled", "Debate was cancelled before completion");
    
    private final String value;
    private final String description;
    
    DebateStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canTransitionTo(DebateStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == INITIALIZED || newStatus == CANCELLED;
            case INITIALIZED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED -> newStatus == ARCHIVED;
            case ARCHIVED, CANCELLED -> false; // Terminal states
        };
    }
    
    public boolean isActive() {
        return this == IN_PROGRESS;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == ARCHIVED || this == CANCELLED;
    }
    
    public boolean canAcceptParticipants() {
        return this == CREATED || this == INITIALIZED;
    }
    
    public boolean canAcceptResponses() {
        return this == IN_PROGRESS;
    }
    
    public static DebateStatus fromValue(String value) {
        for (DebateStatus status : DebateStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid debate status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}