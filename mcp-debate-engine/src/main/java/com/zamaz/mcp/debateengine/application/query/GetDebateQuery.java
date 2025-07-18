package com.zamaz.mcp.debateengine.application.query;

import com.zamaz.mcp.common.application.Query;
import com.zamaz.mcp.debateengine.domain.model.DebateId;
import com.zamaz.mcp.debateengine.domain.model.OrganizationId;

import java.util.Objects;

/**
 * Query to get a debate by ID.
 */
public record GetDebateQuery(
    DebateId debateId,
    OrganizationId organizationId
) implements Query {
    
    public GetDebateQuery {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
    }
    
    /**
     * Create from string IDs.
     */
    public static GetDebateQuery of(String debateId, String organizationId) {
        return new GetDebateQuery(
            DebateId.from(debateId),
            OrganizationId.from(organizationId)
        );
    }
}