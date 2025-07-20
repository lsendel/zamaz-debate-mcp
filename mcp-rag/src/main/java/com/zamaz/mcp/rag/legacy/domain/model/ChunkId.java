package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Document Chunk.
 */
public record ChunkId(UUID value) implements ValueObject {
    
    public ChunkId {
        Objects.requireNonNull(value, "Chunk ID cannot be null");
    }
    
    public static ChunkId generate() {
        return new ChunkId(UUID.randomUUID());
    }
    
    public static ChunkId from(String value) {
        Objects.requireNonNull(value, "Chunk ID string cannot be null");
        try {
            return new ChunkId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Chunk ID format: " + value, e);
        }
    }
    
    public static ChunkId from(UUID value) {
        return new ChunkId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}