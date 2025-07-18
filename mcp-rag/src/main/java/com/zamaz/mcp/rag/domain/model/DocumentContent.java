package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing the content of a document.
 */
public record DocumentContent(String value) implements ValueObject {
    
    private static final int MAX_LENGTH = 10_000_000; // 10MB text limit
    
    public DocumentContent {
        Objects.requireNonNull(value, "Document content cannot be null");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Document content cannot exceed " + MAX_LENGTH + " characters"
            );
        }
    }
    
    public static DocumentContent of(String value) {
        return new DocumentContent(value != null ? value : "");
    }
    
    public static DocumentContent empty() {
        return new DocumentContent("");
    }
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
    
    public boolean isNotEmpty() {
        return !isEmpty();
    }
    
    public int length() {
        return value.length();
    }
    
    public int wordCount() {
        if (isEmpty()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }
    
    public DocumentContent truncate(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length cannot be negative");
        }
        if (value.length() <= maxLength) {
            return this;
        }
        return new DocumentContent(value.substring(0, maxLength));
    }
    
    public DocumentContent concat(DocumentContent other) {
        Objects.requireNonNull(other, "Other content cannot be null");
        return new DocumentContent(this.value + other.value);
    }
    
    public boolean contains(String substring) {
        Objects.requireNonNull(substring, "Substring cannot be null");
        return value.toLowerCase().contains(substring.toLowerCase());
    }
    
    /**
     * Extract a preview of the content (first N characters).
     */
    public String getPreview(int maxChars) {
        if (maxChars <= 0) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }
    
    /**
     * Estimate reading time in minutes based on average reading speed.
     */
    public int getEstimatedReadingTimeMinutes() {
        // Average reading speed: 200-250 words per minute
        return Math.max(1, wordCount() / 225);
    }
    
    @Override
    public String toString() {
        return getPreview(100);
    }
}