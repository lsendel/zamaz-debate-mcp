package com.zamaz.mcp.context.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Objects;

/**
 * Command to archive a context.
 */
public record ArchiveContextCommand(
    String contextId,
    String organizationId
) implements Command {
    
    public ArchiveContextCommand {
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