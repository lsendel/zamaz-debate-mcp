package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.query.GetDebateQuery;
import com.zamaz.mcp.controller.domain.model.Debate;
import java.util.Optional;

/**
 * Use case for getting a debate by ID.
 */
public interface GetDebateUseCase {
    
    /**
     * Gets a debate by ID.
     * 
     * @param query the get debate query
     * @return the debate if found
     */
    Optional<Debate> execute(GetDebateQuery query);
}