package com.zamaz.mcp.controller.application.command;

import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.LlmProvider;
import com.zamaz.mcp.controller.domain.model.ParticipantType;
import com.zamaz.mcp.controller.domain.model.Position;
import com.zamaz.mcp.controller.domain.model.ProviderConfig;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to join a debate as a participant.
 */
public record JoinDebateCommand(
    DebateId debateId,
    String participantName,
    ParticipantType participantType,
    Position position,
    LlmProvider provider,
    ProviderConfig providerConfig
) {
    
    public JoinDebateCommand {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        Objects.requireNonNull(participantName, "Participant name cannot be null");
        Objects.requireNonNull(participantType, "Participant type cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");
        
        if (participantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Participant name cannot be empty");
        }
        
        if (participantName.length() > 255) {
            throw new IllegalArgumentException("Participant name cannot exceed 255 characters");
        }
        
        // Validate AI participants have provider info
        if (participantType == ParticipantType.AI) {
            Objects.requireNonNull(provider, "AI participants must have a provider");
            Objects.requireNonNull(providerConfig, "AI participants must have provider config");
        }
        
        // Human participants should not have provider info
        if (participantType == ParticipantType.HUMAN) {
            if (provider != null) {
                throw new IllegalArgumentException("Human participants cannot have a provider");
            }
            if (providerConfig != null) {
                throw new IllegalArgumentException("Human participants cannot have provider config");
            }
        }
    }
    
    public static JoinDebateCommand human(DebateId debateId, String participantName, Position position) {
        return new JoinDebateCommand(
            debateId,
            participantName.trim(),
            ParticipantType.HUMAN,
            position,
            null,
            null
        );
    }
    
    public static JoinDebateCommand ai(DebateId debateId, String participantName, Position position,
                                     LlmProvider provider, ProviderConfig config) {
        return new JoinDebateCommand(
            debateId,
            participantName.trim(),
            ParticipantType.AI,
            position,
            provider,
            config
        );
    }
    
    public static JoinDebateCommand aiWithDefaults(DebateId debateId, String participantName, 
                                                  Position position, LlmProvider provider, String topic) {
        ProviderConfig config = ProviderConfig.debateConfig(provider, position, topic);
        return ai(debateId, participantName, position, provider, config);
    }
    
    public boolean isHuman() {
        return participantType == ParticipantType.HUMAN;
    }
    
    public boolean isAI() {
        return participantType == ParticipantType.AI;
    }
    
    public Optional<LlmProvider> getProvider() {
        return Optional.ofNullable(provider);
    }
    
    public Optional<ProviderConfig> getProviderConfig() {
        return Optional.ofNullable(providerConfig);
    }
}