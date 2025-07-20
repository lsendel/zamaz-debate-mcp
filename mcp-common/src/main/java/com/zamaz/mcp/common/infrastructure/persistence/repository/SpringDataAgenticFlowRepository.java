package com.zamaz.mcp.common.infrastructure.persistence.repository;

import com.zamaz.mcp.common.infrastructure.persistence.entity.AgenticFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AgenticFlowEntity.
 * This is the low-level persistence interface used by the adapter.
 */
@Repository
public interface SpringDataAgenticFlowRepository extends JpaRepository<AgenticFlowEntity, UUID> {

    /**
     * Finds all agentic flows for the specified organization.
     */
    List<AgenticFlowEntity> findByOrganizationId(UUID organizationId);

    /**
     * Finds all agentic flows of the specified type.
     */
    List<AgenticFlowEntity> findByType(AgenticFlowEntity.AgenticFlowTypeEntity type);

    /**
     * Finds all agentic flows of the specified type for the specified organization.
     */
    List<AgenticFlowEntity> findByOrganizationIdAndType(
            UUID organizationId,
            AgenticFlowEntity.AgenticFlowTypeEntity type);

    /**
     * Finds all active agentic flows for the specified organization.
     */
    @Query("SELECT af FROM AgenticFlowEntity af WHERE af.organizationId = :organizationId AND af.status = 'ACTIVE'")
    List<AgenticFlowEntity> findActiveByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Counts agentic flows by organization.
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Counts agentic flows by type.
     */
    long countByType(AgenticFlowEntity.AgenticFlowTypeEntity type);

    /**
     * Checks if an agentic flow exists for the organization and type.
     */
    boolean existsByOrganizationIdAndType(
            UUID organizationId,
            AgenticFlowEntity.AgenticFlowTypeEntity type);
}