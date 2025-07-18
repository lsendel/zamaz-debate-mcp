package com.zamaz.mcp.debateengine.application.usecase;

import com.zamaz.mcp.debateengine.application.command.CreateDebateCommand;
import com.zamaz.mcp.debateengine.domain.model.DebateId;

/**
 * Use case for creating a debate.
 */
public interface CreateDebateUseCase {
    
    /**
     * Create a new debate.
     * 
     * @param command the create debate command
     * @return the ID of the created debate
     */
    DebateId execute(CreateDebateCommand command);
}