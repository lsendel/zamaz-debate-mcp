package com.zamaz.mcp.rag.domain.model.embedding;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing an Embedding identifier.
 */
public record EmbeddingId(String value) {
    
    public EmbeddingId {
        Objects.requireNonNull(value, "Embedding ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Embedding ID cannot be empty");
        }
        
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Embedding ID must be a valid UUID: " + value);
        }
    }
    
    /**
     * Generate a new embedding ID
     */
    public static EmbeddingId generate() {
        return new EmbeddingId(UUID.randomUUID().toString());
    }
    
    /**
     * Create an embedding ID from a string value
     */
    public static EmbeddingId of(String value) {
        return new EmbeddingId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}