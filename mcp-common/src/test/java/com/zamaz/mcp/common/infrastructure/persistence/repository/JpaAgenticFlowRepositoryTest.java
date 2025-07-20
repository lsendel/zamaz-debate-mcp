package com.zamaz.mcp.common.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.common.infrastructure.persistence.mapper.AgenticFlowPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JpaAgenticFlowRepository.
 */
@ExtendWith(MockitoExtension.class)
class JpaAgenticFlowRepositoryTest {

    @Mock
    private SpringDataAgenticFlowRepository jpaRepository;

    private AgenticFlowPersistenceMapper mapper;
    private JpaAgenticFlowRepository repository;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        mapper = new AgenticFlowPersistenceMapper(objectMapper);
        repository = new JpaAgenticFlowRepository(jpaRepository, mapper);
    }

    @Test
    void testCreateAgenticFlow() {
        // Given
        Map<String, Object> configParams = new HashMap<>();
        configParams.put("temperature", 0.7);
        configParams.put("maxTokens", 1000);

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(configParams);
        OrganizationId organizationId = new OrganizationId();

        AgenticFlow flow = new AgenticFlow(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                configuration,
                organizationId);

        // Then
        assertThat(flow).isNotNull();
        assertThat(flow.getId()).isNotNull();
        assertThat(flow.getType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(flow.getConfiguration().getParameters()).containsEntry("temperature", 0.7);
        assertThat(flow.getConfiguration().getParameters()).containsEntry("maxTokens", 1000);
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.ACTIVE);
        assertThat(flow.getOrganizationId()).isEqualTo(organizationId);
        assertThat(flow.getCreatedAt()).isNotNull();
        assertThat(flow.getUpdatedAt()).isNotNull();
    }

    @Test
    void testAgenticFlowStatusTransitions() {
        // Given
        AgenticFlow flow = createTestFlow();

        // When - activate
        flow.activate();

        // Then
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.ACTIVE);

        // When - deactivate
        flow.deactivate();

        // Then
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.INACTIVE);
    }

    @Test
    void testAgenticFlowConfiguration() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("iterations", 3);
        params.put("threshold", 0.8);
        params.put("enableLogging", true);

        AgenticFlowConfiguration config = new AgenticFlowConfiguration(params);

        // Then
        assertThat(config.getParameter("iterations")).isEqualTo(3);
        assertThat(config.getParameter("threshold")).isEqualTo(0.8);
        assertThat(config.getParameter("enableLogging")).isEqualTo(true);
        assertThat(config.getParameter("nonExistent")).isNull();
        assertThat(config.getParameter("nonExistent", "default")).isEqualTo("default");

        // Test immutability
        AgenticFlowConfiguration newConfig = config.withParameter("newParam", "newValue");
        assertThat(config.getParameter("newParam")).isNull();
        assertThat(newConfig.getParameter("newParam")).isEqualTo("newValue");
    }

    @Test
    void testAllAgenticFlowTypes() {
        // Test that all flow types can be created
        AgenticFlowType[] types = AgenticFlowType.values();

        for (AgenticFlowType type : types) {
            AgenticFlow flow = new AgenticFlow(
                    type,
                    new AgenticFlowConfiguration(),
                    new OrganizationId());

            assertThat(flow.getType()).isEqualTo(type);
        }
    }

    @Test
    void testAgenticFlowEquality() {
        // Given
        AgenticFlow flow1 = createTestFlow();
        AgenticFlow flow2 = new AgenticFlow(
                flow1.getId(),
                flow1.getType(),
                flow1.getConfiguration(),
                flow1.getStatus(),
                flow1.getOrganizationId(),
                flow1.getCreatedAt(),
                flow1.getUpdatedAt());
        AgenticFlow flow3 = createTestFlow();

        // Then
        assertThat(flow1).isEqualTo(flow2);
        assertThat(flow1).isNotEqualTo(flow3);
        assertThat(flow1.hashCode()).isEqualTo(flow2.hashCode());
    }

    @Test
    void testAgenticFlowToString() {
        // Given
        AgenticFlow flow = createTestFlow();

        // When
        String toString = flow.toString();

        // Then
        assertThat(toString).contains("AgenticFlow");
        assertThat(toString).contains(flow.getId().toString());
        assertThat(toString).contains(flow.getType().toString());
        assertThat(toString).contains(flow.getStatus().toString());
        assertThat(toString).contains(flow.getOrganizationId().toString());
    }

    private AgenticFlow createTestFlow() {
        Map<String, Object> configParams = new HashMap<>();
        configParams.put("temperature", 0.7);

        return new AgenticFlow(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                new AgenticFlowConfiguration(configParams),
                new OrganizationId());
    }
}