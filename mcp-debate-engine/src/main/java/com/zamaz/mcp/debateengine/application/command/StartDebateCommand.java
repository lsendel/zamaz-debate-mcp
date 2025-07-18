package com.zamaz.mcp.debateengine.application.command;

import com.zamaz.mcp.common.application.Command;
import com.zamaz.mcp.debateengine.domain.model.DebateId;

import java.util.Objects;

/**
 * Command to start a debate.
 */
public record StartDebateCommand(
    DebateId debateId
) implements Command {
    
    public StartDebateCommand {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
    }
    
    /**
     * Create from debate ID string.
     */
    public static StartDebateCommand of(String debateId) {
        return new StartDebateCommand(DebateId.from(debateId));
    }
}