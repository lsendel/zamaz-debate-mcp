package com.zamaz.mcp.debateengine.application.command;

import com.zamaz.mcp.common.application.Command;
import com.zamaz.mcp.debateengine.domain.model.AIModel;
import com.zamaz.mcp.debateengine.domain.model.DebateId;
import com.zamaz.mcp.debateengine.domain.model.Position;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to add a participant to a debate.
 */
public record AddParticipantCommand(
    DebateId debateId,
    UUID userId,
    AIModel aiModel,
    Position position
) implements Command {
    
    public AddParticipantCommand {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");
        
        if (userId == null && aiModel == null) {
            throw new IllegalArgumentException("Either userId or aiModel must be provided");
        }
        if (userId != null && aiModel != null) {
            throw new IllegalArgumentException("Cannot provide both userId and aiModel");
        }
    }
    
    /**
     * Create command for human participant.
     */
    public static AddParticipantCommand forHuman(
            DebateId debateId,
            UUID userId,
            Position position) {
        return new AddParticipantCommand(debateId, userId, null, position);
    }
    
    /**
     * Create command for AI participant.
     */
    public static AddParticipantCommand forAI(
            DebateId debateId,
            AIModel aiModel,
            Position position) {
        return new AddParticipantCommand(debateId, null, aiModel, position);
    }
    
    /**
     * Check if this is for an AI participant.
     */
    public boolean isAI() {
        return aiModel != null;
    }
}