package com.zamaz.mcp.common.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.common.infrastructure.persistence.config.AgenticFlowPersistenceConfig;
import com.zamaz.mcp.common.infrastructure.persistence.mapper.AgenticFlowPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JpaAgenticFlowRepository using an in-memory database.
 */
@DataJpaTest
@Import({ AgenticFlowPersistenceConfig.class, AgenticFlowPersistenceMapper.class, ObjectMapper.class })
@ActiveProfiles("test")
class JpaAgenticFlowRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SpringDataAgenticFlowRepository springDataRepository;

    @Autowired
    private AgenticFlowPersistenceMapper mapper;

    private JpaAgenticFlowRepository repository;

    @Test
    void testSaveAndFindById() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);

        Map<String, Object> configParams = new HashMap<>();
        configParams.put("temperature", 0.8);
        configParams.put("maxTokens", 2000);

        AgenticFlow originalFlow = new AgenticFlow(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                new AgenticFlowConfiguration(configParams),
                new OrganizationId());

        // When
        AgenticFlow savedFlow = repository.save(originalFlow);
        entityManager.flush();
        entityManager.clear();

        Optional<AgenticFlow> foundFlow = repository.findById(savedFlow.getId());

        // Then
        assertThat(foundFlow).isPresent();
        assertThat(foundFlow.get().getId()).isEqualTo(savedFlow.getId());
        assertThat(foundFlow.get().getType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(foundFlow.get().getConfiguration().getParameter("temperature")).isEqualTo(0.8);
        assertThat(foundFlow.get().getConfiguration().getParameter("maxTokens")).isEqualTo(2000);
        assertThat(foundFlow.get().getStatus()).isEqualTo(AgenticFlowStatus.ACTIVE);
        assertThat(foundFlow.get().getOrganizationId()).isEqualTo(originalFlow.getOrganizationId());
    }

    @Test
    void testFindByOrganization() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);

        OrganizationId org1 = new OrganizationId();
        OrganizationId org2 = new OrganizationId();

        AgenticFlow flow1 = new AgenticFlow(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(),
                org1);

        AgenticFlow flow2 = new AgenticFlow(
                AgenticFlowType.MULTI_AGENT_RED_TEAM,
                new AgenticFlowConfiguration(),
                org1);

        AgenticFlow flow3 = new AgenticFlow(
                AgenticFlowType.TOOL_CALLING_VERIFICATION,
                new AgenticFlowConfiguration(),
                org2);

        // When
        repository.save(flow1);
        repository.save(flow2);
        repository.save(flow3);
        entityManager.flush();
        entityManager.clear();

        List<AgenticFlow> org1Flows = repository.findByOrganization(org1);
        List<AgenticFlow> org2Flows = repository.findByOrganization(org2);

        // Then
        assertThat(org1Flows).hasSize(2);
        assertThat(org1Flows).extracting(AgenticFlow::getType)
                .containsExactlyInAnyOrder(AgenticFlowType.SELF_CRITIQUE_LOOP, AgenticFlowType.MULTI_AGENT_RED_TEAM);

        assertThat(org2Flows).hasSize(1);
        assertThat(org2Flows.get(0).getType()).isEqualTo(AgenticFlowType.TOOL_CALLING_VERIFICATION);
    }

    @Test
    void testFindByType() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);

        OrganizationId org1 = new OrganizationId();
        OrganizationId org2 = new OrganizationId();

        AgenticFlow flow1 = new AgenticFlow(
                AgenticFlowType.RAG_WITH_RERANKING,
                new AgenticFlowConfiguration(),
                org1);

        AgenticFlow flow2 = new AgenticFlow(
                AgenticFlowType.RAG_WITH_RERANKING,
                new AgenticFlowConfiguration(),
                org2);

        AgenticFlow flow3 = new AgenticFlow(
                AgenticFlowType.CONFIDENCE_SCORING,
                new AgenticFlowConfiguration(),
                org1);

        // When
        repository.save(flow1);
        repository.save(flow2);
        repository.save(flow3);
        entityManager.flush();
        entityManager.clear();

        List<AgenticFlow> ragFlows = repository.findByType(AgenticFlowType.RAG_WITH_RERANKING);
        List<AgenticFlow> confidenceFlows = repository.findByType(AgenticFlowType.CONFIDENCE_SCORING);

        // Then
        assertThat(ragFlows).hasSize(2);
        assertThat(ragFlows).allMatch(flow -> flow.getType() == AgenticFlowType.RAG_WITH_RERANKING);

        assertThat(confidenceFlows).hasSize(1);
        assertThat(confidenceFlows.get(0).getType()).isEqualTo(AgenticFlowType.CONFIDENCE_SCORING);
    }

    @Test
    void testFindByOrganizationAndType() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);

        OrganizationId org1 = new OrganizationId();
        OrganizationId org2 = new OrganizationId();

        AgenticFlow flow1 = new AgenticFlow(
                AgenticFlowType.CONSTITUTIONAL_PROMPTING,
                new AgenticFlowConfiguration(),
                org1);

        AgenticFlow flow2 = new AgenticFlow(
                AgenticFlowType.CONSTITUTIONAL_PROMPTING,
                new AgenticFlowConfiguration(),
                org2);

        AgenticFlow flow3 = new AgenticFlow(
                AgenticFlowType.ENSEMBLE_VOTING,
                new AgenticFlowConfiguration(),
                org1);

        // When
        repository.save(flow1);
        repository.save(flow2);
        repository.save(flow3);
        entityManager.flush();
        entityManager.clear();

        List<AgenticFlow> org1ConstitutionalFlows = repository.findByOrganizationAndType(
                org1, AgenticFlowType.CONSTITUTIONAL_PROMPTING);
        List<AgenticFlow> org2ConstitutionalFlows = repository.findByOrganizationAndType(
                org2, AgenticFlowType.CONSTITUTIONAL_PROMPTING);
        List<AgenticFlow> org1EnsembleFlows = repository.findByOrganizationAndType(
                org1, AgenticFlowType.ENSEMBLE_VOTING);

        // Then
        assertThat(org1ConstitutionalFlows).hasSize(1);
        assertThat(org1ConstitutionalFlows.get(0).getOrganizationId()).isEqualTo(org1);
        assertThat(org1ConstitutionalFlows.get(0).getType()).isEqualTo(AgenticFlowType.CONSTITUTIONAL_PROMPTING);

        assertThat(org2ConstitutionalFlows).hasSize(1);
        assertThat(org2ConstitutionalFlows.get(0).getOrganizationId()).isEqualTo(org2);

        assertThat(org1EnsembleFlows).hasSize(1);
        assertThat(org1EnsembleFlows.get(0).getType()).isEqualTo(AgenticFlowType.ENSEMBLE_VOTING);
    }

    @Test
    void testDelete() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);

        AgenticFlow flow = new AgenticFlow(
                AgenticFlowType.POST_PROCESSING_RULES,
                new AgenticFlowConfiguration(),
                new OrganizationId());

        // When
        AgenticFlow savedFlow = repository.save(flow);
        entityManager.flush();
        entityManager.clear();

        boolean deleted = repository.delete(savedFlow.getId());
        entityManager.flush();
        entityManager.clear();

        Optional<AgenticFlow> foundFlow = repository.findById(savedFlow.getId());

        // Then
        assertThat(deleted).isTrue();
        assertThat(foundFlow).isEmpty();
    }

    @Test
    void testDeleteNonExistent() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);
        AgenticFlowId nonExistentId = new AgenticFlowId();

        // When
        boolean deleted = repository.delete(nonExistentId);

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void testUpdateFlow() {
        // Given
        repository = new JpaAgenticFlowRepository(springDataRepository, mapper);

        Map<String, Object> originalConfig = new HashMap<>();
        originalConfig.put("param1", "value1");

        AgenticFlow originalFlow = new AgenticFlow(
                AgenticFlowType.TREE_OF_THOUGHTS,
                new AgenticFlowConfiguration(originalConfig),
                new OrganizationId());

        // When
        AgenticFlow savedFlow = repository.save(originalFlow);
        entityManager.flush();
        entityManager.clear();

        // Modify the flow
        savedFlow.deactivate();
        AgenticFlow updatedFlow = repository.save(savedFlow);
        entityManager.flush();
        entityManager.clear();

        Optional<AgenticFlow> foundFlow = repository.findById(savedFlow.getId());

        // Then
        assertThat(foundFlow).isPresent();
        assertThat(foundFlow.get().getStatus()).isEqualTo(AgenticFlowStatus.INACTIVE);
        assertThat(foundFlow.get().getUpdatedAt()).isAfter(foundFlow.get().getCreatedAt());
    }
}