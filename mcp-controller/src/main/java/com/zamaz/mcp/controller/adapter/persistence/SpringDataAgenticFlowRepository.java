package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AgenticFlowEntity.
 */
@Repository
public interface SpringDataAgenticFlowRepository extends JpaRepository<AgenticFlowEntity, UUID> {

    /**
     * Finds all flows for a specific organization.
     *
     * @param organizationId The organization ID
     * @return List of flows
     */
    List<AgenticFlowEntity> findByOrganizationId(UUID organizationId);

    /**
     * Finds all flows of a specific type.
     *
     * @param flowType The flow type
     * @return List of flows
     */
    List<AgenticFlowEntity> findByFlowType(String flowType);

    /**
     * Finds all flows for a specific organization and type.
     *
     * @param organizationId The organization ID
     * @param flowType The flow type
     * @return List of flows
     */
    List<AgenticFlowEntity> findByOrganizationIdAndFlowType(UUID organizationId, String flowType);

    /**
     * Finds all flows for a specific organization with a specific status.
     *
     * @param organizationId The organization ID
     * @param status The flow status
     * @return List of flows
     */
    List<AgenticFlowEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    /**
     * Counts flows for a specific organization.
     *
     * @param organizationId The organization ID
     * @return The count
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Checks if a flow with the given name exists for the organization.
     *
     * @param organizationId The organization ID
     * @param name The flow name
     * @return True if exists
     */
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);

    /**
     * Finds flows matching a name pattern for an organization.
     *
     * @param organizationId The organization ID
     * @param namePattern The name pattern
     * @return List of flows
     */
    @Query("SELECT f FROM AgenticFlowEntity f WHERE f.organizationId = :orgId AND f.name LIKE %:pattern%")
    List<AgenticFlowEntity> findByOrganizationIdAndNameContaining(
            @Param("orgId") UUID organizationId,
            @Param("pattern") String namePattern
    );
}