package com.zamaz.mcp.controller.application.query;

import com.zamaz.mcp.controller.domain.model.DebateId;
import java.util.Objects;

/**
 * Query to get a debate by ID.
 */
public record GetDebateQuery(DebateId debateId) {
    
    public GetDebateQuery {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
    }
    
    public static GetDebateQuery of(DebateId debateId) {
        return new GetDebateQuery(debateId);
    }
}