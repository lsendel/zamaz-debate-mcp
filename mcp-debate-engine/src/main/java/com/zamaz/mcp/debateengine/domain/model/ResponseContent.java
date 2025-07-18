package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;

/**
 * Value object representing participant response content.
 */
public record ResponseContent(String value) implements ValueObject {
    
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 10000;
    
    public ResponseContent {
        Objects.requireNonNull(value, "Response content cannot be null");
        
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Response content cannot be empty");
        }
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Response must be at least %d characters", MIN_LENGTH)
            );
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Response cannot exceed %d characters", MAX_LENGTH)
            );
        }
        
        value = trimmed;
    }
    
    /**
     * Create from string.
     */
    public static ResponseContent of(String value) {
        return new ResponseContent(value);
    }
    
    /**
     * Get word count.
     */
    public int getWordCount() {
        return value.split("\\s+").length;
    }
    
    /**
     * Get character count.
     */
    public int getCharacterCount() {
        return value.length();
    }
    
    /**
     * Get summary (first 200 characters).
     */
    public String getSummary() {
        if (value.length() <= 200) {
            return value;
        }
        return value.substring(0, 197) + "...";
    }
    
    @Override
    public String toString() {
        return value;
    }
}