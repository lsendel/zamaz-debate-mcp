package com.zamaz.mcp.organization.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.organization.application.command.RemoveUserFromOrganizationCommand;

/**
 * Use case for removing a user from an organization.
 * This is an inbound port that defines the contract for user removal.
 */
public interface RemoveUserFromOrganizationUseCase extends VoidUseCase<RemoveUserFromOrganizationCommand> {
    /**
     * Removes a user from an organization.
     * 
     * @param command the command containing user removal details
     */
    @Override
    void execute(RemoveUserFromOrganizationCommand command);
}