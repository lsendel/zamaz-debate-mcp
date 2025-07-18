package com.zamaz.mcp.context.application.command;

import com.zamaz.mcp.common.application.command.Command;
import java.util.Map;
import java.util.Objects;

/**
 * Command to update metadata of an existing context.
 */
public record UpdateContextMetadataCommand(
    String contextId,
    String organizationId,
    Map<String, Object> metadata
) implements Command {
    
    public UpdateContextMetadataCommand {
        Objects.requireNonNull(contextId, "Context ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        
        if (contextId.trim().isEmpty()) {
            throw new IllegalArgumentException("Context ID cannot be empty");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
    }
    
    public static UpdateContextMetadataCommand of(
            String contextId,
            String organizationId,
            Map<String, Object> metadata
    ) {
        return new UpdateContextMetadataCommand(contextId, organizationId, metadata);
    }
}