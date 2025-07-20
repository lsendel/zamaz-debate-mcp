package com.zamaz.mcp.common.domain.agentic;

import com.zamaz.mcp.common.domain.organization.OrganizationId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for agentic flow persistence operations.
 */
public interface AgenticFlowRepository {

    /**
     * Saves an agentic flow.
     *
     * @param flow The flow to save
     * @return The saved flow
     */
    AgenticFlow save(AgenticFlow flow);

    /**
     * Finds an agentic flow by its ID.
     *
     * @param id The flow ID
     * @return An Optional containing the flow, or empty if not found
     */
    Optional<AgenticFlow> findById(AgenticFlowId id);

    /**
     * Finds all agentic flows for the specified organization.
     *
     * @param organizationId The organization ID
     * @return A list of flows for the organization
     */
    List<AgenticFlow> findByOrganization(OrganizationId organizationId);

    /**
     * Finds all agentic flows of the specified type.
     *
     * @param type The flow type
     * @return A list of flows of the specified type
     */
    List<AgenticFlow> findByType(AgenticFlowType type);

    /**
     * Finds all agentic flows of the specified type for the specified organization.
     *
     * @param organizationId The organization ID
     * @param type           The flow type
     * @return A list of flows of the specified type for the organization
     */
    List<AgenticFlow> findByOrganizationAndType(OrganizationId organizationId, AgenticFlowType type);

    /**
     * Deletes an agentic flow.
     *
     * @param id The flow ID
     * @return True if the flow was deleted, false if it was not found
     */
    boolean delete(AgenticFlowId id);
}