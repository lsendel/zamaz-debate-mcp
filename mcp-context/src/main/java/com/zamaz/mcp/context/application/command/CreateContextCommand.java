package com.zamaz.mcp.context.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Map;
import java.util.Objects;

/**
 * Command to create a new context.
 */
public record CreateContextCommand(
    String organizationId,
    String userId,
    String name,
    Map<String, Object> metadata
) implements Command {
    
    public CreateContextCommand {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        
        // Metadata can be null, convert to empty map
        if (metadata == null) {
            metadata = Map.of();
        }
    }
    
    public static CreateContextCommand of(String organizationId, String userId, String name) {
        return new CreateContextCommand(organizationId, userId, name, Map.of());
    }
}