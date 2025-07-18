package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Document.
 */
public record DocumentId(UUID value) implements ValueObject {
    
    public DocumentId {
        Objects.requireNonNull(value, "Document ID cannot be null");
    }
    
    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }
    
    public static DocumentId from(String value) {
        Objects.requireNonNull(value, "Document ID string cannot be null");
        try {
            return new DocumentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Document ID format: " + value, e);
        }
    }
    
    public static DocumentId from(UUID value) {
        return new DocumentId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}