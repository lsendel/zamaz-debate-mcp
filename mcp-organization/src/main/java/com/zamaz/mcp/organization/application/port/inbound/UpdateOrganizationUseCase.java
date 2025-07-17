package com.zamaz.mcp.organization.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.organization.application.command.UpdateOrganizationCommand;

/**
 * Use case for updating organization details.
 * This is an inbound port that defines the contract for organization updates.
 */
public interface UpdateOrganizationUseCase extends VoidUseCase<UpdateOrganizationCommand> {
    /**
     * Updates an organization's details.
     * 
     * @param command the command containing update details
     */
    @Override
    void execute(UpdateOrganizationCommand command);
}