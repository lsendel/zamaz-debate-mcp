package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.model.valueobject.TenantId;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an organization identifier.
 * In a multi-tenant system, OrganizationId is equivalent to TenantId.
 */
public record OrganizationId(UUID value) implements ValueObject {
    
    public OrganizationId {
        Objects.requireNonNull(value, "Organization ID cannot be null");
    }
    
    /**
     * Creates a new random OrganizationId.
     * 
     * @return a new OrganizationId
     */
    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID());
    }
    
    /**
     * Creates an OrganizationId from a string representation.
     * 
     * @param value the string UUID
     * @return an OrganizationId
     */
    public static OrganizationId from(String value) {
        return new OrganizationId(UUID.fromString(value));
    }
    
    /**
     * Creates an OrganizationId from a TenantId.
     * 
     * @param tenantId the tenant ID
     * @return an OrganizationId
     */
    public static OrganizationId from(TenantId tenantId) {
        return new OrganizationId(tenantId.value());
    }
    
    /**
     * Converts this OrganizationId to a TenantId.
     * 
     * @return a TenantId with the same value
     */
    public TenantId toTenantId() {
        return new TenantId(value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}