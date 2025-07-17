package com.zamaz.mcp.organization.application.query;

import com.zamaz.mcp.common.application.query.Query;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.UserId;

import java.util.Objects;

/**
 * Query for retrieving organization details.
 * Includes the requesting user for permission checks.
 */
public record GetOrganizationQuery(
    OrganizationId organizationId,
    UserId requestingUserId
) implements Query {
    
    public GetOrganizationQuery {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(requestingUserId, "Requesting user ID is required");
    }
    
    /**
     * Creates a query from string IDs.
     */
    public static GetOrganizationQuery of(String organizationId, String requestingUserId) {
        return new GetOrganizationQuery(
            OrganizationId.from(organizationId),
            UserId.from(requestingUserId)
        );
    }
}