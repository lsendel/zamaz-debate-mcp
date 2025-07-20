package com.zamaz.mcp.controller.adapter.persistence.mapper;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowExecution;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Mapper between AgenticFlowExecution domain model and AgenticFlowExecutionEntity.
 */
@Component
public class AgenticFlowExecutionEntityMapper {

    /**
     * Converts a domain model to an entity.
     *
     * @param domain The domain model
     * @param flowEntity The associated flow entity
     * @return The entity
     */
    public AgenticFlowExecutionEntity toEntity(AgenticFlowExecution domain, AgenticFlowEntity flowEntity) {
        Objects.requireNonNull(domain, "Domain object cannot be null");
        Objects.requireNonNull(flowEntity, "Flow entity cannot be null");

        return AgenticFlowExecutionEntity.builder()
                .id(domain.getId())
                .flow(flowEntity)
                .debateId(domain.getDebateId())
                .participantId(domain.getParticipantId())
                .prompt(domain.getPrompt())
                .result(domain.getResult())
                .processingTimeMs(domain.getProcessingTimeMs())
                .responseChanged(domain.isResponseChanged())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * Converts an entity to a domain model.
     *
     * @param entity The entity
     * @return The domain model
     */
    public AgenticFlowExecution toDomain(AgenticFlowExecutionEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");

        return new AgenticFlowExecution(
                entity.getId(),
                new AgenticFlowId(entity.getFlow().getId().toString()),
                entity.getDebateId(),
                entity.getParticipantId(),
                entity.getPrompt(),
                entity.getResult(),
                entity.getProcessingTimeMs(),
                entity.getResponseChanged(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }
}