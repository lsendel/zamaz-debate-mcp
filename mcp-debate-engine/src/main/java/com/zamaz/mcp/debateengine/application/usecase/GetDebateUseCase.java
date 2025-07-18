package com.zamaz.mcp.debateengine.application.usecase;

import com.zamaz.mcp.debateengine.application.query.GetDebateQuery;
import com.zamaz.mcp.debateengine.domain.model.Debate;

import java.util.Optional;

/**
 * Use case for retrieving a debate.
 */
public interface GetDebateUseCase {
    
    /**
     * Get a debate by ID.
     * 
     * @param query the get debate query
     * @return the debate if found
     */
    Optional<Debate> execute(GetDebateQuery query);
}