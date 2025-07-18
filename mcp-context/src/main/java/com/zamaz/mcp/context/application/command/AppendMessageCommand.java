package com.zamaz.mcp.context.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Objects;

/**
 * Command to append a message to an existing context.
 */
public record AppendMessageCommand(
    String contextId,
    String organizationId,
    String role,
    String content,
    String model
) implements Command {
    
    public AppendMessageCommand {
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        
        if (contextId.trim().isEmpty()) {
            throw new IllegalArgumentException("Context ID cannot be empty");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
        
        if (role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }
        
        // Model can be null, will use default
    }
    
    public static AppendMessageCommand of(
            String contextId,
            String organizationId,
            String role,
            String content
    ) {
        return new AppendMessageCommand(contextId, organizationId, role, content, null);
    }
}