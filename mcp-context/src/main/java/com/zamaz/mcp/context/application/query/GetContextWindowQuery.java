package com.zamaz.mcp.context.application.query;

import com.zamaz.mcp.common.application.query.Query;
import java.util.Objects;
import java.util.Optional;

/**
 * Query to retrieve a context window with token and message limits.
 */
public record GetContextWindowQuery(
    String contextId,
    String organizationId,
    int maxTokens,
    Optional<Integer> maxMessages
) implements Query {
    
    public GetContextWindowQuery {
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(maxMessages, "Max messages optional cannot be null");
        
        if (contextId.trim().isEmpty()) {
            throw new IllegalArgumentException("Context ID cannot be empty");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
        
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }
        
        if (maxMessages.isPresent() && maxMessages.get() <= 0) {
            throw new IllegalArgumentException("Max messages must be positive if provided");
        }
    }
    
    public static GetContextWindowQuery of(
            String contextId,
            String organizationId,
            int maxTokens
    ) {
        return new GetContextWindowQuery(contextId, organizationId, maxTokens, Optional.empty());
    }
    
    public static GetContextWindowQuery of(
            String contextId,
            String organizationId,
            int maxTokens,
            int maxMessages
    ) {
        return new GetContextWindowQuery(contextId, organizationId, maxTokens, Optional.of(maxMessages));
    }
}