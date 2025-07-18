package com.zamaz.mcp.context.application.query;

import com.zamaz.mcp.common.application.query.Query;
import java.util.Objects;

/**
 * Query to retrieve a context by ID.
 */
public record GetContextQuery(
    String contextId,
    String organizationId
) implements Query {
    
    public GetContextQuery {
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        if (contextId.trim().isEmpty()) {
            throw new IllegalArgumentException("Context ID cannot be empty");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
    }
}