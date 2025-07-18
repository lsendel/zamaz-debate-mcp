package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the type of a debate participant.
 */
public enum ParticipantType implements ValueObject {
    HUMAN("human", "Human participant"),
    AI("ai", "AI/LLM participant");
    
    private final String value;
    private final String description;
    
    ParticipantType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isHuman() {
        return this == HUMAN;
    }
    
    public boolean isAI() {
        return this == AI;
    }
    
    public static ParticipantType fromValue(String value) {
        for (ParticipantType type : ParticipantType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid participant type: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}