package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing the content of a debate argument.
 */
public record ArgumentContent(String value) implements ValueObject {
    
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 50_000;
    
    public ArgumentContent {
        Objects.requireNonNull(value, "Argument content cannot be null");
        if (value.trim().length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Argument content must be at least " + MIN_LENGTH + " characters"
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Argument content cannot exceed " + MAX_LENGTH + " characters"
            );
        }
    }
    
    public static ArgumentContent of(String value) {
        return new ArgumentContent(value.trim());
    }
    
    public int length() {
        return value.length();
    }
    
    public int wordCount() {
        return value.trim().split("\\s+").length;
    }
    
    public ArgumentContent truncate(int maxLength) {
        if (maxLength < MIN_LENGTH) {
            throw new IllegalArgumentException("Max length cannot be less than " + MIN_LENGTH);
        }
        if (value.length() <= maxLength) {
            return this;
        }
        
        String truncated = value.substring(0, maxLength);
        // Try to break at word boundary
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxLength / 2) {
            truncated = truncated.substring(0, lastSpace);
        }
        
        return new ArgumentContent(truncated + "...");
    }
    
    public ArgumentContent append(String additionalContent) {
        Objects.requireNonNull(additionalContent, "Additional content cannot be null");
        return new ArgumentContent(value + additionalContent);
    }
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
    
    public boolean isNotEmpty() {
        return !isEmpty();
    }
    
    public boolean contains(String substring) {
        return value.toLowerCase().contains(substring.toLowerCase());
    }
    
    public ArgumentContent withPrefix(String prefix) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        return new ArgumentContent(prefix + value);
    }
    
    public ArgumentContent withSuffix(String suffix) {
        Objects.requireNonNull(suffix, "Suffix cannot be null");
        return new ArgumentContent(value + suffix);
    }
    
    /**
     * Estimates reading time in seconds based on average reading speed.
     */
    public int estimateReadingTimeSeconds() {
        // Average reading speed: 200-250 words per minute
        return Math.max(5, (wordCount() * 60) / 225);
    }
    
    @Override
    public String toString() {
        if (value.length() <= 100) {
            return value;
        }
        return value.substring(0, 97) + "...";
    }
}