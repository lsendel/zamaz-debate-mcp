package com.zamaz.mcp.organization.application.command;

import com.zamaz.mcp.common.application.command.Command;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.UserId;

import java.util.Objects;

/**
 * Command for removing a user from an organization.
 */
public record RemoveUserFromOrganizationCommand(
    OrganizationId organizationId,
    UserId userToRemove,
    UserId removedBy
) implements Command {
    
    public RemoveUserFromOrganizationCommand {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(userToRemove, "User ID to remove is required");
        Objects.requireNonNull(removedBy, "Removing user ID is required");
    }
    
    /**
     * Creates a command from string values.
     */
    public static RemoveUserFromOrganizationCommand of(
            String organizationId,
            String userToRemove,
            String removedBy) {
        return new RemoveUserFromOrganizationCommand(
            OrganizationId.from(organizationId),
            UserId.from(userToRemove),
            UserId.from(removedBy)
        );
    }
}