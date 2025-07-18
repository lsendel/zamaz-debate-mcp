package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.CreateDebateCommand;
import com.zamaz.mcp.controller.domain.model.DebateId;

/**
 * Use case for creating a new debate.
 */
public interface CreateDebateUseCase {
    
    /**
     * Creates a new debate with the given parameters.
     * 
     * @param command the create debate command
     * @return the ID of the created debate
     */
    DebateId execute(CreateDebateCommand command);
}