package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a tenant/organization identifier.
 * This is used across all services for multi-tenant isolation.
 */
public record TenantId(UUID value) implements ValueObject {
    
    public TenantId {
        Objects.requireNonNull(value, "Tenant ID cannot be null");
    }
    
    /**
     * Creates a new random TenantId.
     * 
     * @return a new TenantId
     */
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }
    
    /**
     * Creates a TenantId from a string representation.
     * 
     * @param value the string UUID
     * @return a TenantId
     */
    public static TenantId from(String value) {
        return new TenantId(UUID.fromString(value));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}