package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;

/**
 * Value object representing participant type.
 */
public enum ParticipantType implements ValueObject {
    HUMAN("Human participant"),
    AI("AI participant");
    
    private final String description;
    
    ParticipantType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if participant is AI.
     */
    public boolean isAI() {
        return this == AI;
    }
    
    /**
     * Check if participant is human.
     */
    public boolean isHuman() {
        return this == HUMAN;
    }
}