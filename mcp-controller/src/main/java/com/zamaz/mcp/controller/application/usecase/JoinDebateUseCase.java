package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.JoinDebateCommand;
import com.zamaz.mcp.controller.domain.model.ParticipantId;

/**
 * Use case for joining a debate as a participant.
 */
public interface JoinDebateUseCase {
    
    /**
     * Adds a participant to a debate.
     * 
     * @param command the join debate command
     * @return the ID of the created participant
     */
    ParticipantId execute(JoinDebateCommand command);
}