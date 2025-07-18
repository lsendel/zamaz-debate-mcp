package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a participant identifier.
 */
public record ParticipantId(UUID value) implements ValueObject {
    
    public ParticipantId {
        Objects.requireNonNull(value, "Participant ID cannot be null");
    }
    
    /**
     * Create from string representation.
     */
    public static ParticipantId from(String value) {
        return new ParticipantId(UUID.fromString(value));
    }
    
    /**
     * Generate new random ID.
     */
    public static ParticipantId generate() {
        return new ParticipantId(UUID.randomUUID());
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}