package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Debate Participant.
 */
public record ParticipantId(UUID value) implements ValueObject {
    
    public ParticipantId {
        Objects.requireNonNull(value, "Participant ID cannot be null");
    }
    
    public static ParticipantId generate() {
        return new ParticipantId(UUID.randomUUID());
    }
    
    public static ParticipantId from(String value) {
        Objects.requireNonNull(value, "Participant ID string cannot be null");
        try {
            return new ParticipantId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Participant ID format: " + value, e);
        }
    }
    
    public static ParticipantId from(UUID value) {
        return new ParticipantId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}