package com.zamaz.mcp.debateengine.application.command;

import com.zamaz.mcp.common.application.Command;
import com.zamaz.mcp.debateengine.domain.model.DebateId;
import com.zamaz.mcp.debateengine.domain.model.ParticipantId;
import com.zamaz.mcp.debateengine.domain.model.RoundId;

import java.util.Objects;

/**
 * Command to submit a response in a debate round.
 */
public record SubmitResponseCommand(
    DebateId debateId,
    RoundId roundId,
    ParticipantId participantId,
    String content,
    long responseTimeMs,
    int tokenCount
) implements Command {
    
    public SubmitResponseCommand {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        Objects.requireNonNull(roundId, "Round ID cannot be null");
        Objects.requireNonNull(participantId, "Participant ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        
        if (content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (responseTimeMs < 0) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
        if (tokenCount < 0) {
            throw new IllegalArgumentException("Token count cannot be negative");
        }
    }
}