package com.zamaz.mcp.organization.application.command;

import com.zamaz.mcp.common.application.command.Command;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.Role;
import com.zamaz.mcp.organization.domain.model.UserId;

import java.util.Objects;

/**
 * Command for adding a user to an organization.
 */
public record AddUserToOrganizationCommand(
    OrganizationId organizationId,
    UserId userToAdd,
    Role role,
    UserId addedBy
) implements Command {
    
    public AddUserToOrganizationCommand {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(userToAdd, "User ID to add is required");
        Objects.requireNonNull(role, "Role is required");
        Objects.requireNonNull(addedBy, "Adding user ID is required");
    }
    
    /**
     * Creates a command from string values.
     */
    public static AddUserToOrganizationCommand of(
            String organizationId,
            String userToAdd,
            String role,
            String addedBy) {
        return new AddUserToOrganizationCommand(
            OrganizationId.from(organizationId),
            UserId.from(userToAdd),
            Role.fromString(role),
            UserId.from(addedBy)
        );
    }
}