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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AgenticFlowMapper.
 */
class AgenticFlowMapperTest {

    private AgenticFlowMapper mapper;
    private AgenticFlow testFlow;
    private AgenticFlowEntity testEntity;
    private AgenticFlowExecution testExecution;
    private AgenticFlowExecutionEntity testExecutionEntity;

    @BeforeEach
    void setUp() {
        mapper = new AgenticFlowMapper();

        OrganizationId organizationId = new OrganizationId(UUID.randomUUID().toString());
        AgenticFlowId flowId = new AgenticFlowId();
        Instant now = Instant.now();

        Map<String, Object> config = Map.of(
                "temperature", 0.7,
                "iterations", 3,
                "name", "Custom Flow");

        testFlow = new AgenticFlow(
                flowId,
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(config),
                AgenticFlowStatus.ACTIVE,
                organizationId,
                now,
                now);

        testEntity = AgenticFlowEntity.builder()
                .id(UUID.fromString(flowId.getValue()))
                .flowType(AgenticFlowType.SELF_CRITIQUE_LOOP.name())
                .name("Self-Critique Loop - Custom Flow")
                .description("Agentic flow of type: Self-Critique Loop. Custom Flow")
                .configuration(config)
                .organizationId(UUID.fromString(organizationId.getValue()))
                .status(AgenticFlowStatus.ACTIVE.name())
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .build();

        Map<String, Object> result = Map.of(
                "finalAnswer", "Test answer",
                "reasoning", "Test reasoning");

        testExecution = new AgenticFlowExecution(
                UUID.randomUUID(),
                flowId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test prompt",
                result,
                1500L,
                true,
                null,
                now);

        testExecutionEntity = AgenticFlowExecutionEntity.builder()
                .id(testExecution.getId())
                .flow(testEntity)
                .debateId(testExecution.getDebateId())
                .participantId(testExecution.getParticipantId())
                .prompt("Test prompt")
                .result(result)
                .processingTimeMs(1500L)
                .responseChanged(true)
                .errorMessage(null)
                .createdAt(now)
                .build();
    }

    @Test
    void toEntity_ValidFlow_ShouldMapCorrectly() {
        // When
        AgenticFlowEntity result = mapper.toEntity(testFlow);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(UUID.fromString(testFlow.getId().getValue()));
        assertThat(result.getFlowType()).isEqualTo(testFlow.getType().name());
        assertThat(result.getName()).isEqualTo("Self-Critique Loop - Custom Flow");
        assertThat(result.getDescription()).contains("Self-Critique Loop");
        assertThat(result.getConfiguration()).isEqualTo(testFlow.getConfiguration().getParameters());
        assertThat(result.getOrganizationId()).isEqualTo(UUID.fromString(testFlow.getOrganizationId().getValue()));
        assertThat(result.getStatus()).isEqualTo(testFlow.getStatus().name());
        assertThat(result.getCreatedAt()).isEqualTo(testFlow.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(testFlow.getUpdatedAt());
        assertThat(result.getVersion()).isEqualTo(1L);
    }

    @Test
    void toEntity_NullFlow_ShouldReturnNull() {
        // When
        AgenticFlowEntity result = mapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toDomain_ValidEntity_ShouldMapCorrectly() {
        // When
        AgenticFlow result = mapper.toDomain(testEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId().getValue()).isEqualTo(testEntity.getId().toString());
        assertThat(result.getType()).isEqualTo(AgenticFlowType.valueOf(testEntity.getFlowType()));
        assertThat(result.getConfiguration().getParameters()).isEqualTo(testEntity.getConfiguration());
        assertThat(result.getStatus()).isEqualTo(AgenticFlowStatus.valueOf(testEntity.getStatus()));
        assertThat(result.getOrganizationId().getValue()).isEqualTo(testEntity.getOrganizationId().toString());
        assertThat(result.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
    }

    @Test
    void toDomain_NullEntity_ShouldReturnNull() {
        // When
        AgenticFlow result = mapper.toDomain(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toExecutionEntity_ValidExecution_ShouldMapCorrectly() {
        // When
        AgenticFlowExecutionEntity result = mapper.toExecutionEntity(testExecution, testEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testExecution.getId());
        assertThat(result.getFlow()).isEqualTo(testEntity);
        assertThat(result.getDebateId()).isEqualTo(testExecution.getDebateId());
        assertThat(result.getParticipantId()).isEqualTo(testExecution.getParticipantId());
        assertThat(result.getPrompt()).isEqualTo(testExecution.getPrompt());
        assertThat(result.getResult()).isEqualTo(testExecution.getResult());
        assertThat(result.getProcessingTimeMs()).isEqualTo(testExecution.getProcessingTimeMs());
        assertThat(result.getResponseChanged()).isEqualTo(testExecution.isResponseChanged());
        assertThat(result.getErrorMessage()).isEqualTo(testExecution.getErrorMessage());
        assertThat(result.getCreatedAt()).isEqualTo(testExecution.getCreatedAt());
    }

    @Test
    void toExecutionEntity_NullExecution_ShouldReturnNull() {
        // When
        AgenticFlowExecutionEntity result = mapper.toExecutionEntity(null, testEntity);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toExecutionDomain_ValidEntity_ShouldMapCorrectly() {
        // When
        AgenticFlowExecution result = mapper.toExecutionDomain(testExecutionEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testExecutionEntity.getId());
        assertThat(result.getFlowId().getValue()).isEqualTo(testExecutionEntity.getFlow().getId().toString());
        assertThat(result.getDebateId()).isEqualTo(testExecutionEntity.getDebateId());
        assertThat(result.getParticipantId()).isEqualTo(testExecutionEntity.getParticipantId());
        assertThat(result.getPrompt()).isEqualTo(testExecutionEntity.getPrompt());
        assertThat(result.getResult()).isEqualTo(testExecutionEntity.getResult());
        assertThat(result.getProcessingTimeMs()).isEqualTo(testExecutionEntity.getProcessingTimeMs());
        assertThat(result.isResponseChanged()).isEqualTo(testExecutionEntity.getResponseChanged());
        assertThat(result.getErrorMessage()).isEqualTo(testExecutionEntity.getErrorMessage());
        assertThat(result.getCreatedAt()).isEqualTo(testExecutionEntity.getCreatedAt());
    }

    @Test
    void toExecutionDomain_NullEntity_ShouldReturnNull() {
        // When
        AgenticFlowExecution result = mapper.toExecutionDomain(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void updateEntity_ValidFlowAndEntity_ShouldUpdateMutableFields() {
        // Given
        AgenticFlowEntity entity = AgenticFlowEntity.builder()
                .id(UUID.fromString(testFlow.getId().getValue()))
                .flowType("OLD_TYPE")
                .name("Old Name")
                .description("Old Description")
                .status("INACTIVE")
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        // When
        mapper.updateEntity(entity, testFlow);

        // Then
        assertThat(entity.getStatus()).isEqualTo(testFlow.getStatus().name());
        assertThat(entity.getUpdatedAt()).isEqualTo(testFlow.getUpdatedAt());
        assertThat(entity.getName()).isEqualTo("Self-Critique Loop - Custom Flow");
        assertThat(entity.getDescription()).contains("Self-Critique Loop");

        // Immutable fields should not change
        assertThat(entity.getFlowType()).isEqualTo("OLD_TYPE");
    }

    @Test
    void updateEntity_NullInputs_ShouldNotThrow() {
        // When & Then - should not throw
        mapper.updateEntity(null, testFlow);
        mapper.updateEntity(testEntity, null);
        mapper.updateEntity(null, null);
    }

    @Test
    void generateFlowName_FlowWithCustomName_ShouldIncludeCustomName() {
        // Given
        Map<String, Object> config = Map.of("name", "My Custom Flow");
        AgenticFlow flow = new AgenticFlow(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                new AgenticFlowConfiguration(config),
                new OrganizationId(UUID.randomUUID().toString()));

        // When
        AgenticFlowEntity result = mapper.toEntity(flow);

        // Then
        assertThat(result.getName()).isEqualTo("Internal Monologue - My Custom Flow");
    }

    @Test
    void generateFlowName_FlowWithoutCustomName_ShouldUseDisplayName() {
        // Given
        Map<String, Object> config = Map.of("temperature", 0.5);
        AgenticFlow flow = new AgenticFlow(
                AgenticFlowType.ENSEMBLE_VOTING,
                new AgenticFlowConfiguration(config),
                new OrganizationId(UUID.randomUUID().toString()));

        // When
        AgenticFlowEntity result = mapper.toEntity(flow);

        // Then
        assertThat(result.getName()).isEqualTo("Ensemble Voting");
    }

    @Test
    void generateFlowDescription_DifferentFlowTypes_ShouldGenerateAppropriateDescriptions() {
        // Test different flow types
        AgenticFlowType[] types = {
                AgenticFlowType.INTERNAL_MONOLOGUE,
                AgenticFlowType.TOOL_CALLING_VERIFICATION,
                AgenticFlowType.RAG_WITH_RERANKING,
                AgenticFlowType.CONSTITUTIONAL_PROMPTING
        };

        for (AgenticFlowType type : types) {
            // Given
            AgenticFlow flow = new AgenticFlow(
                    type,
                    new AgenticFlowConfiguration(Map.of()),
                    new OrganizationId(UUID.randomUUID().toString()));

            // When
            AgenticFlowEntity result = mapper.toEntity(flow);

            // Then
            assertThat(result.getDescription())
                    .startsWith("Agentic flow of type: " + type.getDisplayName())
                    .contains(type.getDisplayName());
        }
    }

    @Test
    void generateFlowDescription_WithConfigurationParameters_ShouldIncludeParameters() {
        // Given
        Map<String, Object> config = Map.of(
                "iterations", 5,
                "temperature", 0.8,
                "threshold", 0.75);
        AgenticFlow flow = new AgenticFlow(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(config),
                new OrganizationId(UUID.randomUUID().toString()));

        // When
        AgenticFlowEntity result = mapper.toEntity(flow);

        // Then
        assertThat(result.getDescription())
                .contains("Iterations: 5")
                .contains("Temperature: 0.8")
                .contains("Threshold: 0.75");
    }
}