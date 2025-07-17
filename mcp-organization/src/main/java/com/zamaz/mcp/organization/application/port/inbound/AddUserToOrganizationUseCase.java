package com.zamaz.mcp.organization.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.organization.application.command.AddUserToOrganizationCommand;

/**
 * Use case for adding a user to an organization.
 * This is an inbound port that defines the contract for user addition.
 */
public interface AddUserToOrganizationUseCase extends VoidUseCase<AddUserToOrganizationCommand> {
    /**
     * Adds a user to an organization with a specified role.
     * 
     * @param command the command containing user and role details
     */
    @Override
    void execute(AddUserToOrganizationCommand command);
}