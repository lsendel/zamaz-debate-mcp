package com.zamaz.mcp.rag.domain.model.document;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Chunk's unique identifier.
 */
public record ChunkId(String value) {
    
    public ChunkId {
        Objects.requireNonNull(value, "ChunkId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ChunkId value cannot be blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ChunkId must be a valid UUID format", e);
        }
    }
    
    /**
     * Factory method to generate a new ChunkId
     */
    public static ChunkId generate() {
        return new ChunkId(UUID.randomUUID().toString());
    }
    
    /**
     * Factory method to create from existing value
     */
    public static ChunkId of(String value) {
        return new ChunkId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}