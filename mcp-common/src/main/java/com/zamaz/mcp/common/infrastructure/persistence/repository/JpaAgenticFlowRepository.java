package com.zamaz.mcp.common.infrastructure.persistence.repository;

import com.zamaz.mcp.common.architecture.adapter.persistence.PersistenceAdapter;
import com.zamaz.mcp.common.architecture.exception.PersistenceException;
import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.common.infrastructure.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.common.infrastructure.persistence.mapper.AgenticFlowPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of the AgenticFlowRepository port.
 * This adapter translates between the domain model and JPA entities.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class JpaAgenticFlowRepository implements AgenticFlowRepository, PersistenceAdapter {

    private final SpringDataAgenticFlowRepository jpaRepository;
    private final AgenticFlowPersistenceMapper mapper;

    @Override
    @Transactional
    public AgenticFlow save(AgenticFlow flow) {
        try {
            log.debug("Saving agentic flow: {}", flow.getId());

            // Convert to entity
            AgenticFlowEntity entity = mapper.fromDomain(flow);

            // Save entity
            AgenticFlowEntity saved = jpaRepository.save(entity);

            // Convert back to domain
            AgenticFlow result = mapper.toDomain(saved);

            log.debug("Successfully saved agentic flow: {}", result.getId());
            return result;

        } catch (Exception e) {
            log.error("Failed to save agentic flow: {}", flow.getId(), e);
            throw new PersistenceException("Failed to save agentic flow", e);
        }
    }

    @Override
    public Optional<AgenticFlow> findById(AgenticFlowId id) {
        try {
            log.debug("Finding agentic flow by ID: {}", id);

            UUID uuid = UUID.fromString(id.getValue());
            return jpaRepository.findById(uuid)
                    .map(mapper::toDomain);

        } catch (Exception e) {
            log.error("Failed to find agentic flow by ID: {}", id, e);
            throw new PersistenceException("Failed to find agentic flow by ID", e);
        }
    }

    @Override
    public List<AgenticFlow> findByOrganization(OrganizationId organizationId) {
        try {
            log.debug("Finding agentic flows by organization: {}", organizationId);

            UUID uuid = UUID.fromString(organizationId.getValue());
            return jpaRepository.findByOrganizationId(uuid).stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to find agentic flows by organization: {}", organizationId, e);
            throw new PersistenceException("Failed to find agentic flows by organization", e);
        }
    }

    @Override
    public List<AgenticFlow> findByType(AgenticFlowType type) {
        try {
            log.debug("Finding agentic flows by type: {}", type);

            AgenticFlowEntity.AgenticFlowTypeEntity entityType = mapTypeToEntity(type);
            return jpaRepository.findByType(entityType).stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to find agentic flows by type: {}", type, e);
            throw new PersistenceException("Failed to find agentic flows by type", e);
        }
    }

    @Override
    public List<AgenticFlow> findByOrganizationAndType(OrganizationId organizationId, AgenticFlowType type) {
        try {
            log.debug("Finding agentic flows by organization: {} and type: {}", organizationId, type);

            UUID orgUuid = UUID.fromString(organizationId.getValue());
            AgenticFlowEntity.AgenticFlowTypeEntity entityType = mapTypeToEntity(type);

            return jpaRepository.findByOrganizationIdAndType(orgUuid, entityType).stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to find agentic flows by organization: {} and type: {}", organizationId, type, e);
            throw new PersistenceException("Failed to find agentic flows by organization and type", e);
        }
    }

    @Override
    public boolean delete(AgenticFlowId id) {
        try {
            log.debug("Deleting agentic flow: {}", id);

            UUID uuid = UUID.fromString(id.getValue());

            if (jpaRepository.existsById(uuid)) {
                jpaRepository.deleteById(uuid);
                log.debug("Successfully deleted agentic flow: {}", id);
                return true;
            } else {
                log.debug("Agentic flow not found for deletion: {}", id);
                return false;
            }

        } catch (Exception e) {
            log.error("Failed to delete agentic flow: {}", id, e);
            throw new PersistenceException("Failed to delete agentic flow", e);
        }
    }

    /**
     * Additional utility methods for enhanced functionality
     */

    /**
     * Finds all active agentic flows for the specified organization.
     */
    public List<AgenticFlow> findActiveByOrganization(OrganizationId organizationId) {
        try {
            log.debug("Finding active agentic flows by organization: {}", organizationId);

            UUID uuid = UUID.fromString(organizationId.getValue());
            return jpaRepository.findActiveByOrganizationId(uuid).stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to find active agentic flows by organization: {}", organizationId, e);
            throw new PersistenceException("Failed to find active agentic flows by organization", e);
        }
    }

    /**
     * Counts agentic flows by organization.
     */
    public long countByOrganization(OrganizationId organizationId) {
        try {
            UUID uuid = UUID.fromString(organizationId.getValue());
            return jpaRepository.countByOrganizationId(uuid);
        } catch (Exception e) {
            log.error("Failed to count agentic flows by organization: {}", organizationId, e);
            throw new PersistenceException("Failed to count agentic flows by organization", e);
        }
    }

    /**
     * Counts agentic flows by type.
     */
    public long countByType(AgenticFlowType type) {
        try {
            AgenticFlowEntity.AgenticFlowTypeEntity entityType = mapTypeToEntity(type);
            return jpaRepository.countByType(entityType);
        } catch (Exception e) {
            log.error("Failed to count agentic flows by type: {}", type, e);
            throw new PersistenceException("Failed to count agentic flows by type", e);
        }
    }

    /**
     * Checks if an agentic flow exists for the organization and type.
     */
    public boolean existsByOrganizationAndType(OrganizationId organizationId, AgenticFlowType type) {
        try {
            UUID orgUuid = UUID.fromString(organizationId.getValue());
            AgenticFlowEntity.AgenticFlowTypeEntity entityType = mapTypeToEntity(type);
            return jpaRepository.existsByOrganizationIdAndType(orgUuid, entityType);
        } catch (Exception e) {
            log.error("Failed to check existence by organization: {} and type: {}", organizationId, type, e);
            throw new PersistenceException("Failed to check agentic flow existence", e);
        }
    }

    /**
     * Maps domain type enum to entity type enum.
     */
    private AgenticFlowEntity.AgenticFlowTypeEntity mapTypeToEntity(AgenticFlowType domainType) {
        return switch (domainType) {
            case INTERNAL_MONOLOGUE -> AgenticFlowEntity.AgenticFlowTypeEntity.INTERNAL_MONOLOGUE;
            case SELF_CRITIQUE_LOOP -> AgenticFlowEntity.AgenticFlowTypeEntity.SELF_CRITIQUE_LOOP;
            case MULTI_AGENT_RED_TEAM -> AgenticFlowEntity.AgenticFlowTypeEntity.MULTI_AGENT_RED_TEAM;
            case TOOL_CALLING_VERIFICATION -> AgenticFlowEntity.AgenticFlowTypeEntity.TOOL_CALLING_VERIFICATION;
            case RAG_WITH_RERANKING -> AgenticFlowEntity.AgenticFlowTypeEntity.RAG_WITH_RERANKING;
            case CONFIDENCE_SCORING -> AgenticFlowEntity.AgenticFlowTypeEntity.CONFIDENCE_SCORING;
            case CONSTITUTIONAL_PROMPTING -> AgenticFlowEntity.AgenticFlowTypeEntity.CONSTITUTIONAL_PROMPTING;
            case ENSEMBLE_VOTING -> AgenticFlowEntity.AgenticFlowTypeEntity.ENSEMBLE_VOTING;
            case POST_PROCESSING_RULES -> AgenticFlowEntity.AgenticFlowTypeEntity.POST_PROCESSING_RULES;
            case TREE_OF_THOUGHTS -> AgenticFlowEntity.AgenticFlowTypeEntity.TREE_OF_THOUGHTS;
            case STEP_BACK_PROMPTING -> AgenticFlowEntity.AgenticFlowTypeEntity.STEP_BACK_PROMPTING;
            case PROMPT_CHAINING -> AgenticFlowEntity.AgenticFlowTypeEntity.PROMPT_CHAINING;
        };
    }
}