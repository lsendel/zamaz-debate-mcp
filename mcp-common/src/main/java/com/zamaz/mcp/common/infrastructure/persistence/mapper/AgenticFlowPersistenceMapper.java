package com.zamaz.mcp.common.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.common.architecture.adapter.persistence.PersistenceAdapter;
import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.common.infrastructure.persistence.entity.AgenticFlowEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for converting between agentic flow domain objects and persistence
 * entities.
 * This is part of the persistence adapter layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowPersistenceMapper implements DomainMapper<AgenticFlow, AgenticFlowEntity>, PersistenceAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public AgenticFlow toDomain(AgenticFlowEntity entity) {
        if (entity == null) {
            return null;
        }

        try {
            // Map configuration from JSON
            Map<String, Object> configurationMap = jsonNodeToMap(entity.getConfiguration());
            AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(configurationMap);

            // Convert entity enums to domain enums
            AgenticFlowType type = mapTypeToDomain(entity.getType());
            AgenticFlowStatus status = mapStatusToDomain(entity.getStatus());

            // Create domain object
            return new AgenticFlow(
                    new AgenticFlowId(entity.getId().toString()),
                    type,
                    configuration,
                    status,
                    new OrganizationId(entity.getOrganizationId().toString()),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt());

        } catch (Exception e) {
            log.error("Failed to map AgenticFlowEntity to domain object", e);
            throw new RuntimeException("Failed to map entity to domain", e);
        }
    }

    @Override
    public AgenticFlowEntity fromDomain(AgenticFlow domain) {
        if (domain == null) {
            return null;
        }

        try {
            // Convert configuration to JSON
            JsonNode configurationJson = objectMapper.valueToTree(domain.getConfiguration().getParameters());

            // Convert domain enums to entity enums
            AgenticFlowEntity.AgenticFlowTypeEntity type = mapTypeToEntity(domain.getType());
            AgenticFlowEntity.AgenticFlowStatusEntity status = mapStatusToEntity(domain.getStatus());

            // Create entity
            return AgenticFlowEntity.builder()
                    .id(UUID.fromString(domain.getId().getValue()))
                    .type(type)
                    .configuration(configurationJson)
                    .status(status)
                    .organizationId(UUID.fromString(domain.getOrganizationId().getValue()))
                    .createdAt(domain.getCreatedAt())
                    .updatedAt(domain.getUpdatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Failed to map AgenticFlow domain object to entity", e);
            throw new RuntimeException("Failed to map domain to entity", e);
        }
    }

    /**
     * Maps entity type enum to domain type enum.
     */
    private AgenticFlowType mapTypeToDomain(AgenticFlowEntity.AgenticFlowTypeEntity entityType) {
        return switch (entityType) {
            case INTERNAL_MONOLOGUE -> AgenticFlowType.INTERNAL_MONOLOGUE;
            case SELF_CRITIQUE_LOOP -> AgenticFlowType.SELF_CRITIQUE_LOOP;
            case MULTI_AGENT_RED_TEAM -> AgenticFlowType.MULTI_AGENT_RED_TEAM;
            case TOOL_CALLING_VERIFICATION -> AgenticFlowType.TOOL_CALLING_VERIFICATION;
            case RAG_WITH_RERANKING -> AgenticFlowType.RAG_WITH_RERANKING;
            case CONFIDENCE_SCORING -> AgenticFlowType.CONFIDENCE_SCORING;
            case CONSTITUTIONAL_PROMPTING -> AgenticFlowType.CONSTITUTIONAL_PROMPTING;
            case ENSEMBLE_VOTING -> AgenticFlowType.ENSEMBLE_VOTING;
            case POST_PROCESSING_RULES -> AgenticFlowType.POST_PROCESSING_RULES;
            case TREE_OF_THOUGHTS -> AgenticFlowType.TREE_OF_THOUGHTS;
            case STEP_BACK_PROMPTING -> AgenticFlowType.STEP_BACK_PROMPTING;
            case PROMPT_CHAINING -> AgenticFlowType.PROMPT_CHAINING;
        };
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

    /**
     * Maps entity status enum to domain status enum.
     */
    private AgenticFlowStatus mapStatusToDomain(AgenticFlowEntity.AgenticFlowStatusEntity entityStatus) {
        return switch (entityStatus) {
            case ACTIVE -> AgenticFlowStatus.ACTIVE;
            case INACTIVE -> AgenticFlowStatus.INACTIVE;
            case DRAFT -> AgenticFlowStatus.DRAFT;
        };
    }

    /**
     * Maps domain status enum to entity status enum.
     */
    private AgenticFlowEntity.AgenticFlowStatusEntity mapStatusToEntity(AgenticFlowStatus domainStatus) {
        return switch (domainStatus) {
            case ACTIVE -> AgenticFlowEntity.AgenticFlowStatusEntity.ACTIVE;
            case INACTIVE -> AgenticFlowEntity.AgenticFlowStatusEntity.INACTIVE;
            case DRAFT -> AgenticFlowEntity.AgenticFlowStatusEntity.DRAFT;
        };
    }

    /**
     * Converts JsonNode to Map.
     */
    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to convert JsonNode to Map, returning empty map", e);
            return new HashMap<>();
        }
    }
}