package com.zamaz.mcp.debateengine.application.query;

import com.zamaz.mcp.common.application.Query;
import com.zamaz.mcp.debateengine.domain.model.DebateStatus;
import com.zamaz.mcp.debateengine.domain.model.OrganizationId;

import java.util.Objects;
import java.util.UUID;

/**
 * Query to list debates with filtering.
 */
public record ListDebatesQuery(
    OrganizationId organizationId,
    UUID userId,
    DebateStatus status,
    Integer limit,
    Integer offset
) implements Query {
    
    public ListDebatesQuery {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
    }
    
    /**
     * Create query for organization's debates.
     */
    public static ListDebatesQuery forOrganization(OrganizationId organizationId) {
        return new ListDebatesQuery(organizationId, null, null, null, null);
    }
    
    /**
     * Create query for user's debates.
     */
    public static ListDebatesQuery forUser(OrganizationId organizationId, UUID userId) {
        return new ListDebatesQuery(organizationId, userId, null, null, null);
    }
    
    /**
     * Create query with pagination.
     */
    public static ListDebatesQuery withPagination(
            OrganizationId organizationId,
            int limit,
            int offset) {
        return new ListDebatesQuery(organizationId, null, null, limit, offset);
    }
}