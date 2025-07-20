package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowAnalyticsRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowExecution;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for agentic flow database adapters.
 * Uses an in-memory H2 database for testing.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        PostgresAgenticFlowRepository.class,
        PostgresAgenticFlowAnalyticsRepository.class,
        com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowMapper.class
})
@Transactional
class AgenticFlowRepositoryIntegrationTest {

    @Autowired
    private AgenticFlowRepository flowRepository;

    @Autowired
    private AgenticFlowAnalyticsRepository analyticsRepository;

    private OrganizationId organizationId;
    private AgenticFlow testFlow;

    @BeforeEach
    void setUp() {
        organizationId = new OrganizationId(UUID.randomUUID().toString());

        Map<String, Object> config = Map.of(
                "temperature", 0.7,
                "iterations", 3,
                "name", "Test Flow");

        testFlow = new AgenticFlow(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                new AgenticFlowConfiguration(config),
                organizationId);
    }

    @Test
    void saveAndFindFlow_ShouldPersistAndRetrieveCorrectly() {
        // When
        AgenticFlow savedFlow = flowRepository.save(testFlow);
        Optional<AgenticFlow> foundFlow = flowRepository.findById(savedFlow.getId());

        // Then
        assertThat(foundFlow).isPresent();
        assertThat(foundFlow.get().getId()).isEqualTo(savedFlow.getId());
        assertThat(foundFlow.get().getType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(foundFlow.get().getStatus()).isEqualTo(AgenticFlowStatus.ACTIVE);
        assertThat(foundFlow.get().getOrganizationId()).isEqualTo(organizationId);
        assertThat(foundFlow.get().getConfiguration().getParameters())
                .containsEntry("temperature", 0.7)
                .containsEntry("iterations", 3)
                .containsEntry("name", "Test Flow");
    }

    @Test
    void updateFlow_ShouldUpdateMutableFields() {
        // Given
        AgenticFlow savedFlow = flowRepository.save(testFlow);

        // When
        savedFlow.deactivate();
        AgenticFlow updatedFlow = flowRepository.save(savedFlow);
        Optional<AgenticFlow> foundFlow = flowRepository.findById(savedFlow.getId());

        // Then
        assertThat(foundFlow).isPresent();
        assertThat(foundFlow.get().getStatus()).isEqualTo(AgenticFlowStatus.INACTIVE);
        assertThat(foundFlow.get().getUpdatedAt()).isAfter(foundFlow.get().getCreatedAt());
    }

    @Test
    void findByOrganization_ShouldReturnFlowsForOrganization() {
        // Given
        OrganizationId otherOrgId = new OrganizationId(UUID.randomUUID().toString());
        AgenticFlow otherFlow = new AgenticFlow(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(Map.of()),
                otherOrgId);

        flowRepository.save(testFlow);
        flowRepository.save(otherFlow);

        // When
        List<AgenticFlow> flows = flowRepository.findByOrganization(organizationId);
        List<AgenticFlow> otherFlows = flowRepository.findByOrganization(otherOrgId);

        // Then
        assertThat(flows).hasSize(1);
        assertThat(flows.get(0).getOrganizationId()).isEqualTo(organizationId);
        assertThat(otherFlows).hasSize(1);
        assertThat(otherFlows.get(0).getOrganizationId()).isEqualTo(otherOrgId);
    }

    @Test
    void findByType_ShouldReturnFlowsOfSpecificType() {
        // Given
        AgenticFlow selfCritiqueFlow = new AgenticFlow(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(Map.of()),
                organizationId);

        flowRepository.save(testFlow);
        flowRepository.save(selfCritiqueFlow);

        // When
        List<AgenticFlow> internalMonologueFlows = flowRepository.findByType(AgenticFlowType.INTERNAL_MONOLOGUE);
        List<AgenticFlow> selfCritiqueFlows = flowRepository.findByType(AgenticFlowType.SELF_CRITIQUE_LOOP);

        // Then
        assertThat(internalMonologueFlows).hasSize(1);
        assertThat(internalMonologueFlows.get(0).getType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(selfCritiqueFlows).hasSize(1);
        assertThat(selfCritiqueFlows.get(0).getType()).isEqualTo(AgenticFlowType.SELF_CRITIQUE_LOOP);
    }

    @Test
    void findByOrganizationAndType_ShouldReturnFilteredFlows() {
        // Given
        OrganizationId otherOrgId = new OrganizationId(UUID.randomUUID().toString());
        AgenticFlow sameTypeOtherOrg = new AgenticFlow(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                new AgenticFlowConfiguration(Map.of()),
                otherOrgId);
        AgenticFlow differentTypeSameOrg = new AgenticFlow(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(Map.of()),
                organizationId);

        flowRepository.save(testFlow);
        flowRepository.save(sameTypeOtherOrg);
        flowRepository.save(differentTypeSameOrg);

        // When
        List<AgenticFlow> flows = flowRepository.findByOrganizationAndType(
                organizationId, AgenticFlowType.INTERNAL_MONOLOGUE);

        // Then
        assertThat(flows).hasSize(1);
        assertThat(flows.get(0).getOrganizationId()).isEqualTo(organizationId);
        assertThat(flows.get(0).getType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
    }

    @Test
    void deleteFlow_ShouldRemoveFromDatabase() {
        // Given
        AgenticFlow savedFlow = flowRepository.save(testFlow);

        // When
        boolean deleted = flowRepository.delete(savedFlow.getId());
        Optional<AgenticFlow> foundFlow = flowRepository.findById(savedFlow.getId());

        // Then
        assertThat(deleted).isTrue();
        assertThat(foundFlow).isEmpty();
    }

    @Test
    void deleteNonExistentFlow_ShouldReturnFalse() {
        // Given
        AgenticFlowId nonExistentId = new AgenticFlowId();

        // When
        boolean deleted = flowRepository.delete(nonExistentId);

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void saveAndFindExecution_ShouldPersistAndRetrieveCorrectly() {
        // Given
        AgenticFlow savedFlow = flowRepository.save(testFlow);

        Map<String, Object> result = Map.of(
                "finalAnswer", "Test answer",
                "reasoning", "Step 1: ..., Step 2: ...",
                "confidence", 0.85);

        AgenticFlowExecution execution = AgenticFlowExecution.successful(
                savedFlow.getId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "What is the capital of France?",
                result,
                1500L,
                true);

        // When
        AgenticFlowExecution savedExecution = analyticsRepository.save(execution);
        Optional<AgenticFlowExecution> foundExecution = analyticsRepository.findById(savedExecution.getId());

        // Then
        assertThat(foundExecution).isPresent();
        assertThat(foundExecution.get().getFlowId()).isEqualTo(savedFlow.getId());
        assertThat(foundExecution.get().getPrompt()).isEqualTo("What is the capital of France?");
        assertThat(foundExecution.get().getProcessingTimeMs()).isEqualTo(1500L);
        assertThat(foundExecution.get().isResponseChanged()).isTrue();
        assertThat(foundExecution.get().getResult())
                .containsEntry("finalAnswer", "Test answer")
                .containsEntry("confidence", 0.85);
    }

    @Test
    void analyticsOperations_ShouldCalculateMetricsCorrectly() {
        // Given
        AgenticFlow savedFlow = flowRepository.save(testFlow);

        // Create multiple executions
        for (int i = 0; i < 5; i++) {
            AgenticFlowExecution execution = AgenticFlowExecution.successful(
                    savedFlow.getId(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Test prompt " + i,
                    Map.of("answer", "Answer " + i),
                    1000L + (i * 100L), // Processing times: 1000, 1100, 1200, 1300, 1400
                    i % 2 == 0 // Response changed for even indices
            );
            analyticsRepository.save(execution);
        }

        // When
        long totalExecutions = analyticsRepository.countExecutions(savedFlow.getId());
        long responseChanges = analyticsRepository.countResponseChanges(savedFlow.getId());
        Double avgProcessingTime = analyticsRepository.calculateAverageProcessingTime(savedFlow.getId());
        Double changeRate = analyticsRepository.calculateResponseChangeRate(savedFlow.getId());

        // Then
        assertThat(totalExecutions).isEqualTo(5);
        assertThat(responseChanges).isEqualTo(3); // Indices 0, 2, 4
        assertThat(avgProcessingTime).isEqualTo(1200.0); // Average of 1000, 1100, 1200, 1300, 1400
        assertThat(changeRate).isEqualTo(0.6); // 3/5 = 0.6
    }

    @Test
    void findExecutionsByDebateId_ShouldReturnCorrectExecutions() {
        // Given
        AgenticFlow savedFlow = flowRepository.save(testFlow);
        UUID debateId1 = UUID.randomUUID();
        UUID debateId2 = UUID.randomUUID();

        // Create executions for different debates
        AgenticFlowExecution execution1 = AgenticFlowExecution.successful(
                savedFlow.getId(), debateId1, UUID.randomUUID(),
                "Prompt 1", Map.of("answer", "Answer 1"), 1000L, false);
        AgenticFlowExecution execution2 = AgenticFlowExecution.successful(
                savedFlow.getId(), debateId1, UUID.randomUUID(),
                "Prompt 2", Map.of("answer", "Answer 2"), 1100L, true);
        AgenticFlowExecution execution3 = AgenticFlowExecution.successful(
                savedFlow.getId(), debateId2, UUID.randomUUID(),
                "Prompt 3", Map.of("answer", "Answer 3"), 1200L, false);

        analyticsRepository.save(execution1);
        analyticsRepository.save(execution2);
        analyticsRepository.save(execution3);

        // When
        List<AgenticFlowExecution> debate1Executions = analyticsRepository.findByDebateId(debateId1);
        List<AgenticFlowExecution> debate2Executions = analyticsRepository.findByDebateId(debateId2);

        // Then
        assertThat(debate1Executions).hasSize(2);
        assertThat(debate2Executions).hasSize(1);
        assertThat(debate1Executions).allMatch(exec -> exec.getDebateId().equals(debateId1));
        assertThat(debate2Executions).allMatch(exec -> exec.getDebateId().equals(debateId2));
    }

    @Test
    void findSlowestExecutions_ShouldReturnOrderedResults() {
        // Given
        AgenticFlow savedFlow = flowRepository.save(testFlow);

        // Create executions with different processing times
        long[] processingTimes = { 1000L, 3000L, 1500L, 2000L, 500L };
        for (long time : processingTimes) {
            AgenticFlowExecution execution = AgenticFlowExecution.successful(
                    savedFlow.getId(), UUID.randomUUID(), UUID.randomUUID(),
                    "Prompt", Map.of("answer", "Answer"), time, false);
            analyticsRepository.save(execution);
        }

        // When
        List<AgenticFlowExecution> slowest = analyticsRepository.findSlowestExecutions(savedFlow.getId(), 3);

        // Then
        assertThat(slowest).hasSize(3);
        assertThat(slowest.get(0).getProcessingTimeMs()).isEqualTo(3000L);
        assertThat(slowest.get(1).getProcessingTimeMs()).isEqualTo(2000L);
        assertThat(slowest.get(2).getProcessingTimeMs()).isEqualTo(1500L);
    }
}