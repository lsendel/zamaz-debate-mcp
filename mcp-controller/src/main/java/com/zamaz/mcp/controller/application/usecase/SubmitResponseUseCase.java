package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.SubmitResponseCommand;
import com.zamaz.mcp.controller.domain.model.ResponseId;

/**
 * Use case for submitting a response to a debate round.
 */
public interface SubmitResponseUseCase {
    
    /**
     * Submits a response to the current round of a debate.
     * 
     * @param command the submit response command
     * @return the ID of the created response
     */
    ResponseId execute(SubmitResponseCommand command);
}