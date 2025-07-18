package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing the content of a message.
 * Immutable and self-validating.
 */
public record MessageContent(String value) implements ValueObject {
    
    private static final int MAX_LENGTH = 1_000_000; // 1M characters
    
    public MessageContent {
        Objects.requireNonNull(value, "Message content cannot be null");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Message content exceeds maximum length of " + MAX_LENGTH + " characters"
            );
        }
    }
    
    public static MessageContent empty() {
        return new MessageContent("");
    }
    
    public static MessageContent of(String value) {
        return new MessageContent(value);
    }
    
    public boolean isEmpty() {
        return value.isEmpty();
    }
    
    public boolean isNotEmpty() {
        return !value.isEmpty();
    }
    
    public int length() {
        return value.length();
    }
    
    public MessageContent truncate(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length cannot be negative");
        }
        if (value.length() <= maxLength) {
            return this;
        }
        return new MessageContent(value.substring(0, maxLength));
    }
    
    @Override
    public String toString() {
        return value;
    }
}