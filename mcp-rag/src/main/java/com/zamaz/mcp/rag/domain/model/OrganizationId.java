package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for an Organization.
 */
public record OrganizationId(UUID value) implements ValueObject {
    
    public OrganizationId {
        Objects.requireNonNull(value, "Organization ID cannot be null");
    }
    
    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID());
    }
    
    public static OrganizationId from(String value) {
        Objects.requireNonNull(value, "Organization ID string cannot be null");
        try {
            return new OrganizationId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Organization ID format: " + value, e);
        }
    }
    
    public static OrganizationId from(UUID value) {
        return new OrganizationId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}