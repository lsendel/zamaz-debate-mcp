package com.zamaz.mcp.organization.application.port.inbound;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.organization.application.query.GetOrganizationQuery;
import com.zamaz.mcp.organization.application.query.OrganizationView;

/**
 * Use case for retrieving organization details.
 * This is an inbound port that defines the contract for organization retrieval.
 */
public interface GetOrganizationUseCase extends UseCase<GetOrganizationQuery, OrganizationView> {
    /**
     * Retrieves an organization by its ID.
     * 
     * @param query the query containing the organization ID
     * @return the organization details
     */
    @Override
    OrganizationView execute(GetOrganizationQuery query);
}