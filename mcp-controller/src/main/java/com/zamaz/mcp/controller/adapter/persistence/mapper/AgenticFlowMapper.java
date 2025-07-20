package com.zamaz.mcp.controller.adapter.persistence.mapper;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowExecution;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for converting between agentic flow domain objects and persistence
 * entities.
 */
@Component
public class AgenticFlowMapper {

    /**
     * Converts an AgenticFlow domain object to an AgenticFlowEntity.
     *
     * @param flow The domain object
     * @return The entity
     */
    public AgenticFlowEntity toEntity(AgenticFlow flow) {
        if (flow == null) {
            return null;
        }

        return AgenticFlowEntity.builder()
                .id(UUID.fromString(flow.getId().getValue()))
                .flowType(flow.getType().name())
                .name(generateFlowName(flow))
                .description(generateFlowDescription(flow))
                .configuration(flow.getConfiguration().getParameters())
                .organizationId(UUID.fromString(flow.getOrganizationId().getValue()))
                .status(flow.getStatus().name())
                .createdAt(flow.getCreatedAt())
                .updatedAt(flow.getUpdatedAt())
                .version(1L) // Default version for new entities
                .build();
    }

    /**
     * Converts an AgenticFlowEntity to an AgenticFlow domain object.
     *
     * @param entity The entity
     * @return The domain object
     */
    public AgenticFlow toDomain(AgenticFlowEntity entity) {
        if (entity == null) {
            return null;
        }

        return new AgenticFlow(
                new AgenticFlowId(entity.getId().toString()),
                AgenticFlowType.valueOf(entity.getFlowType()),
                new AgenticFlowConfiguration(entity.getConfiguration()),
                AgenticFlowStatus.valueOf(entity.getStatus()),
                new OrganizationId(entity.getOrganizationId().toString()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /**
     * Converts an AgenticFlowExecution domain object to an
     * AgenticFlowExecutionEntity.
     *
     * @param execution  The domain object
     * @param flowEntity The associated flow entity
     * @return The entity
     */
    public AgenticFlowExecutionEntity toExecutionEntity(AgenticFlowExecution execution, AgenticFlowEntity flowEntity) {
        if (execution == null) {
            return null;
        }

        return AgenticFlowExecutionEntity.builder()
                .id(execution.getId())
                .flow(flowEntity)
                .debateId(execution.getDebateId())
                .participantId(execution.getParticipantId())
                .prompt(execution.getPrompt())
                .result(execution.getResult())
                .processingTimeMs(execution.getProcessingTimeMs())
                .responseChanged(execution.isResponseChanged())
                .errorMessage(execution.getErrorMessage())
                .createdAt(execution.getCreatedAt())
                .build();
    }

    /**
     * Converts an AgenticFlowExecutionEntity to an AgenticFlowExecution domain
     * object.
     *
     * @param entity The entity
     * @return The domain object
     */
    public AgenticFlowExecution toExecutionDomain(AgenticFlowExecutionEntity entity) {
        if (entity == null) {
            return null;
        }

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
                entity.getCreatedAt());
    }

    /**
     * Updates an existing entity with data from a domain object.
     *
     * @param entity The entity to update
     * @param flow   The domain object with new data
     */
    public void updateEntity(AgenticFlowEntity entity, AgenticFlow flow) {
        if (entity == null || flow == null) {
            return;
        }

        // Only update mutable fields
        entity.setStatus(flow.getStatus().name());
        entity.setUpdatedAt(flow.getUpdatedAt());

        // Update name and description if they might have changed
        entity.setName(generateFlowName(flow));
        entity.setDescription(generateFlowDescription(flow));
    }

    /**
     * Generates a human-readable name for the flow.
     *
     * @param flow The flow
     * @return The generated name
     */
    private String generateFlowName(AgenticFlow flow) {
        String baseName = flow.getType().getDisplayName();

        // Add configuration-specific suffix if available
        var config = flow.getConfiguration().getParameters();
        if (config.containsKey("name")) {
            return baseName + " - " + config.get("name");
        }

        return baseName;
    }

    /**
     * Generates a description for the flow.
     *
     * @param flow The flow
     * @return The generated description
     */
    private String generateFlowDescription(AgenticFlow flow) {
        StringBuilder description = new StringBuilder();
        description.append("Agentic flow of type: ").append(flow.getType().getDisplayName());

        var config = flow.getConfiguration().getParameters();

        // Add configuration details to description
        if (config.containsKey("description")) {
            description.append(". ").append(config.get("description"));
        } else {
            // Add type-specific default descriptions
            switch (flow.getType()) {
                case INTERNAL_MONOLOGUE:
                    description.append(". Uses step-by-step reasoning through internal monologue.");
                    break;
                case SELF_CRITIQUE_LOOP:
                    description.append(". Implements Generate-Critique-Revise pattern for self-improvement.");
                    break;
                case MULTI_AGENT_RED_TEAM:
                    description.append(". Simulates internal debates between different perspectives.");
                    break;
                case TOOL_CALLING_VERIFICATION:
                    description.append(". Enables external tool usage for fact verification.");
                    break;
                case RAG_WITH_RERANKING:
                    description.append(". Enhanced RAG with document re-ranking.");
                    break;
                case CONFIDENCE_SCORING:
                    description.append(". Provides confidence scores and improves low-confidence answers.");
                    break;
                case CONSTITUTIONAL_PROMPTING:
                    description.append(". Applies constitutional guardrails to responses.");
                    break;
                case ENSEMBLE_VOTING:
                    description.append(". Uses ensemble voting for more reliable answers.");
                    break;
                case POST_PROCESSING_RULES:
                    description.append(". Applies deterministic validation checks.");
                    break;
                case TREE_OF_THOUGHTS:
                    description.append(". Explores multiple reasoning paths like a decision tree.");
                    break;
                case STEP_BACK_PROMPTING:
                    description.append(". Generalizes from specific questions to underlying principles.");
                    break;
                case PROMPT_CHAINING:
                    description.append(". Decomposes complex tasks into interconnected prompts.");
                    break;
            }
        }

        // Add key configuration parameters
        if (config.containsKey("iterations")) {
            description.append(" Iterations: ").append(config.get("iterations"));
        }
        if (config.containsKey("temperature")) {
            description.append(" Temperature: ").append(config.get("temperature"));
        }
        if (config.containsKey("threshold")) {
            description.append(" Threshold: ").append(config.get("threshold"));
        }

        return description.toString();
    }
}