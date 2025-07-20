package com.zamaz.mcp.rag.domain.model.document;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Document's unique identifier.
 * Immutable and self-validating.
 */
public record DocumentId(String value) {
    
    public DocumentId {
        Objects.requireNonNull(value, "DocumentId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("DocumentId value cannot be blank");
        }
        // Validate UUID format if that's what we're using
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("DocumentId must be a valid UUID format", e);
        }
    }
    
    /**
     * Factory method to generate a new DocumentId
     */
    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID().toString());
    }
    
    /**
     * Factory method to create from existing value
     */
    public static DocumentId of(String value) {
        return new DocumentId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}