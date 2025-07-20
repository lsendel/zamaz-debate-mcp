package com.zamaz.mcp.rag.domain.model.document;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing an Organization's unique identifier.
 * This creates a bounded context boundary with the Organization service.
 */
public record OrganizationId(String value) {
    
    public OrganizationId {
        Objects.requireNonNull(value, "OrganizationId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("OrganizationId value cannot be blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("OrganizationId must be a valid UUID format", e);
        }
    }
    
    /**
     * Factory method to create from existing value
     */
    public static OrganizationId of(String value) {
        return new OrganizationId(value);
    }
    
    /**
     * Factory method to generate a new OrganizationId (mainly for testing)
     */
    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID().toString());
    }
    
    @Override
    public String toString() {
        return value;
    }
}