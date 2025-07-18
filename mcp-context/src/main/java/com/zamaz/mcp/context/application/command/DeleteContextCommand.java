package com.zamaz.mcp.context.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Objects;

/**
 * Command to delete an existing context.
 */
public record DeleteContextCommand(
    String contextId,
    String organizationId
) implements Command {
    
    public DeleteContextCommand {
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        if (contextId.trim().isEmpty()) {
            throw new IllegalArgumentException("Context ID cannot be empty");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
    }
}