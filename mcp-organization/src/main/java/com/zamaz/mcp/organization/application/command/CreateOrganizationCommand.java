package com.zamaz.mcp.organization.application.command;

import com.zamaz.mcp.common.application.command.Command;
import com.zamaz.mcp.organization.domain.model.OrganizationDescription;
import com.zamaz.mcp.organization.domain.model.OrganizationName;
import com.zamaz.mcp.organization.domain.model.UserId;

import java.util.Map;
import java.util.Objects;

/**
 * Command for creating a new organization.
 * Contains all the necessary information to create an organization.
 */
public record CreateOrganizationCommand(
    OrganizationName name,
    OrganizationDescription description,
    UserId creatorUserId,
    Map<String, Object> initialSettings
) implements Command {
    
    public CreateOrganizationCommand {
        Objects.requireNonNull(name, "Organization name is required");
        Objects.requireNonNull(creatorUserId, "Creator user ID is required");
        // Description can be null/empty
        // Initial settings can be null (will use defaults)
    }
    
    /**
     * Creates a command with minimal required information.
     */
    public static CreateOrganizationCommand of(String name, String creatorUserId) {
        return new CreateOrganizationCommand(
            OrganizationName.from(name),
            OrganizationDescription.empty(),
            UserId.from(creatorUserId),
            null
        );
    }
    
    /**
     * Creates a command with name and description.
     */
    public static CreateOrganizationCommand of(String name, String description, String creatorUserId) {
        return new CreateOrganizationCommand(
            OrganizationName.from(name),
            OrganizationDescription.from(description),
            UserId.from(creatorUserId),
            null
        );
    }
}