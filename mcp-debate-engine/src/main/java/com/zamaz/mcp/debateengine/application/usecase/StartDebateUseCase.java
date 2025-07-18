package com.zamaz.mcp.debateengine.application.usecase;

import com.zamaz.mcp.debateengine.application.command.StartDebateCommand;

/**
 * Use case for starting a debate.
 */
public interface StartDebateUseCase {
    
    /**
     * Start a debate.
     * 
     * @param command the start debate command
     */
    void execute(StartDebateCommand command);
}