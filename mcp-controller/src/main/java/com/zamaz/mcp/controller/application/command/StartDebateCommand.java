package com.zamaz.mcp.controller.application.command;

import com.zamaz.mcp.controller.domain.model.DebateId;
import java.util.Objects;

/**
 * Command to start a debate.
 */
public record StartDebateCommand(DebateId debateId) {
    
    public StartDebateCommand {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
    }
    
    public static StartDebateCommand of(DebateId debateId) {
        return new StartDebateCommand(debateId);
    }
}