package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an organization identifier.
 */
public record OrganizationId(UUID value) implements ValueObject {
    
    public OrganizationId {
        Objects.requireNonNull(value, "Organization ID cannot be null");
    }
    
    /**
     * Create from string representation.
     */
    public static OrganizationId from(String value) {
        return new OrganizationId(UUID.fromString(value));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}