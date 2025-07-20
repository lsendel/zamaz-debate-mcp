package com.zamaz.mcp.controller.adapter.persistence.repository;

import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for agentic flow entities.
 */
@Repository
public interface SpringDataAgenticFlowRepository extends JpaRepository<AgenticFlowEntity, UUID> {

    /**
     * Finds all flows for a given organization.
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
     * Finds all flows for a given organization and type.
     *
     * @param organizationId The organization ID
     * @param flowType       The flow type
     * @return List of flows
     */
    List<AgenticFlowEntity> findByOrganizationIdAndFlowType(UUID organizationId, String flowType);

    /**
     * Finds all flows for a given organization and status.
     *
     * @param organizationId The organization ID
     * @param status         The flow status
     * @return List of flows
     */
    List<AgenticFlowEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    /**
     * Counts flows by organization and type.
     *
     * @param organizationId The organization ID
     * @param flowType       The flow type
     * @return Count of flows
     */
    long countByOrganizationIdAndFlowType(UUID organizationId, String flowType);

    /**
     * Finds active flows (status = ACTIVE) for an organization.
     *
     * @param organizationId The organization ID
     * @return List of active flows
     */
    @Query("SELECT f FROM AgenticFlowEntity f WHERE f.organizationId = :orgId AND f.status = 'ACTIVE'")
    List<AgenticFlowEntity> findActiveFlowsByOrganization(@Param("orgId") UUID organizationId);

    /**
     * Finds flows by name pattern for an organization.
     *
     * @param organizationId The organization ID
     * @param namePattern    The name pattern
     * @return List of flows
     */
    @Query("SELECT f FROM AgenticFlowEntity f WHERE f.organizationId = :orgId AND LOWER(f.name) LIKE LOWER(:pattern)")
    List<AgenticFlowEntity> findByOrganizationAndNamePattern(
            @Param("orgId") UUID organizationId,
            @Param("pattern") String namePattern
    );

    /**
     * Finds flows with specific configuration parameter.
     *
     * @param organizationId The organization ID
     * @param parameterKey   The configuration parameter key
     * @param parameterValue The configuration parameter value
     * @return List of flows
     */
    @Query(value = "SELECT * FROM agentic_flows f " +
            "WHERE f.organization_id = :orgId " +
            "AND f.configuration @> jsonb_build_object(:key, :value)",
            nativeQuery = true)
    List<AgenticFlowEntity> findByOrganizationAndConfigParameter(
            @Param("orgId") UUID organizationId,
            @Param("key") String parameterKey,
            @Param("value") String parameterValue
    );

    /**
     * Checks if a flow with the given name exists for an organization.
     *
     * @param organizationId The organization ID
     * @param name           The flow name
     * @return True if exists
     */
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);

    /**
     * Deletes all flows for an organization.
     *
     * @param organizationId The organization ID
     */
    void deleteByOrganizationId(UUID organizationId);
}