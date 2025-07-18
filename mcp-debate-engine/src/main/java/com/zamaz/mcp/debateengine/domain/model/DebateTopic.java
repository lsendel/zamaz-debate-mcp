package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;

/**
 * Value object representing a debate topic.
 */
public record DebateTopic(String value) implements ValueObject {
    
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 500;
    
    public DebateTopic {
        Objects.requireNonNull(value, "Topic cannot be null");
        
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Topic must be at least %d characters", MIN_LENGTH)
            );
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Topic cannot exceed %d characters", MAX_LENGTH)
            );
        }
        
        value = trimmed;
    }
    
    /**
     * Create from string.
     */
    public static DebateTopic of(String value) {
        return new DebateTopic(value);
    }
    
    /**
     * Get a summary of the topic (first 100 characters).
     */
    public String getSummary() {
        if (value.length() <= 100) {
            return value;
        }
        return value.substring(0, 97) + "...";
    }
    
    @Override
    public String toString() {
        return value;
    }
}