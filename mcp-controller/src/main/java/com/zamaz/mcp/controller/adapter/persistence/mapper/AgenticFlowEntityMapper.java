package com.zamaz.mcp.controller.adapter.persistence.mapper;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Mapper between AgenticFlow domain model and AgenticFlowEntity.
 */
@Component
public class AgenticFlowEntityMapper {

    /**
     * Converts a domain model to an entity.
     *
     * @param domain The domain model
     * @return The entity
     */
    public AgenticFlowEntity toEntity(AgenticFlow domain) {
        Objects.requireNonNull(domain, "Domain object cannot be null");

        return AgenticFlowEntity.builder()
                .id(domain.getId() != null ? UUID.fromString(domain.getId().getValue()) : null)
                .flowType(domain.getType().name())
                .name(domain.getName())
                .description(domain.getDescription())
                .configuration(domain.getConfiguration().getParameters())
                .organizationId(UUID.fromString(domain.getOrganizationId().getValue()))
                .status(domain.getStatus().name())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    /**
     * Converts an entity to a domain model.
     *
     * @param entity The entity
     * @return The domain model
     */
    public AgenticFlow toDomain(AgenticFlowEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");

        return AgenticFlow.builder()
                .id(new AgenticFlowId(entity.getId().toString()))
                .type(AgenticFlowType.valueOf(entity.getFlowType()))
                .name(entity.getName())
                .description(entity.getDescription())
                .configuration(new AgenticFlowConfiguration(entity.getConfiguration()))
                .organizationId(new OrganizationId(entity.getOrganizationId().toString()))
                .status(AgenticFlowStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}