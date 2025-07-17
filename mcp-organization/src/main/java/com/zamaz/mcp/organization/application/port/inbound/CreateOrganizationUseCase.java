package com.zamaz.mcp.organization.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.organization.application.command.CreateOrganizationCommand;
import com.zamaz.mcp.organization.domain.model.OrganizationId;

/**
 * Use case for creating a new organization.
 * This is an inbound port that defines the contract for organization creation.
 */
public interface CreateOrganizationUseCase extends UseCase<CreateOrganizationCommand, OrganizationId> {
    /**
     * Creates a new organization with the provided details.
     * 
     * @param command the command containing organization details
     * @return the ID of the created organization
     */
    @Override
    OrganizationId execute(CreateOrganizationCommand command);
}