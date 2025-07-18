package com.zamaz.mcp.debateengine.application.command;

import com.zamaz.mcp.common.application.Command;
import com.zamaz.mcp.debateengine.domain.model.DebateConfiguration;
import com.zamaz.mcp.debateengine.domain.model.OrganizationId;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to create a new debate.
 */
public record CreateDebateCommand(
    OrganizationId organizationId,
    UUID userId,
    String topic,
    String description,
    DebateConfiguration configuration
) implements Command {
    
    public CreateDebateCommand {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(topic, "Topic cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");
    }
    
    /**
     * Create command with default configuration.
     */
    public static CreateDebateCommand withDefaults(
            OrganizationId organizationId,
            UUID userId,
            String topic,
            String description) {
        return new CreateDebateCommand(
            organizationId,
            userId,
            topic,
            description,
            DebateConfiguration.defaults()
        );
    }
}