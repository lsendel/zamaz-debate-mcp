package com.zamaz.mcp.controller.application.usecase;

import com.zamaz.mcp.controller.application.command.StartDebateCommand;

/**
 * Use case for starting a debate.
 */
public interface StartDebateUseCase {
    
    /**
     * Starts a debate, transitioning it to IN_PROGRESS status.
     * 
     * @param command the start debate command
     */
    void execute(StartDebateCommand command);
}