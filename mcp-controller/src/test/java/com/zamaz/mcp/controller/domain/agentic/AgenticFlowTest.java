package com.zamaz.mcp.controller.domain.agentic;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AgenticFlow domain entity.
 */
class AgenticFlowTest {
    
    @Test
    @DisplayName("Should create valid AgenticFlow with all required fields")
    void shouldCreateValidAgenticFlow() {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        OrganizationId orgId = new OrganizationId(UUID.randomUUID().toString());
        AgenticFlowType flowType = AgenticFlowType.INTERNAL_MONOLOGUE;
        String name = "Test Flow";
        String description = "Test flow description";
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("prefix", "Think step by step");
        configuration.put("temperature", 0.7);
        
        // When
        AgenticFlow flow = AgenticFlow.builder()
            .id(flowId)
            .organizationId(orgId)
            .flowType(flowType)
            .name(name)
            .description(description)
            .configuration(configuration)
            .status(AgenticFlowStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1L)
            .build();
        
        // Then
        assertThat(flow).isNotNull();
        assertThat(flow.getId()).isEqualTo(flowId);
        assertThat(flow.getOrganizationId()).isEqualTo(orgId);
        assertThat(flow.getFlowType()).isEqualTo(flowType);
        assertThat(flow.getName()).isEqualTo(name);
        assertThat(flow.getDescription()).isEqualTo(description);
        assertThat(flow.getConfiguration()).isEqualTo(configuration);
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.ACTIVE);
        assertThat(flow.getVersion()).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("Should validate flow configuration based on flow type")
    void shouldValidateFlowConfiguration() {
        // Given
        Map<String, Object> invalidConfig = new HashMap<>();
        // Missing required "prefix" for INTERNAL_MONOLOGUE
        
        // When/Then
        assertThatThrownBy(() -> {
            AgenticFlow.builder()
                .id(new AgenticFlowId(UUID.randomUUID().toString()))
                .organizationId(new OrganizationId(UUID.randomUUID().toString()))
                .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
                .name("Invalid Flow")
                .configuration(invalidConfig)
                .status(AgenticFlowStatus.ACTIVE)
                .build()
                .validate(); // Assuming validate method exists
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("configuration");
    }
    
    @Test
    @DisplayName("Should handle flow deactivation")
    void shouldHandleFlowDeactivation() {
        // Given
        AgenticFlow flow = createTestFlow();
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.ACTIVE);
        
        // When
        flow.deactivate();
        
        // Then
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.INACTIVE);
        assertThat(flow.getUpdatedAt()).isAfterOrEqualTo(flow.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should update configuration correctly")
    void shouldUpdateConfiguration() {
        // Given
        AgenticFlow flow = createTestFlow();
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("prefix", "New thinking prompt");
        newConfig.put("temperature", 0.9);
        
        // When
        flow.updateConfiguration(newConfig);
        
        // Then
        assertThat(flow.getConfiguration()).isEqualTo(newConfig);
        assertThat(flow.getVersion()).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should check if flow is active")
    void shouldCheckIfFlowIsActive() {
        // Given
        AgenticFlow activeFlow = createTestFlow();
        AgenticFlow inactiveFlow = createTestFlow();
        inactiveFlow.deactivate();
        
        // Then
        assertThat(activeFlow.isActive()).isTrue();
        assertThat(inactiveFlow.isActive()).isFalse();
    }
    
    @Test
    @DisplayName("Should validate flow name")
    void shouldValidateFlowName() {
        // When/Then
        assertThatThrownBy(() -> {
            AgenticFlow.builder()
                .id(new AgenticFlowId(UUID.randomUUID().toString()))
                .organizationId(new OrganizationId(UUID.randomUUID().toString()))
                .flowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
                .name("") // Empty name
                .configuration(new HashMap<>())
                .status(AgenticFlowStatus.ACTIVE)
                .build();
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name");
    }
    
    private AgenticFlow createTestFlow() {
        Map<String, Object> config = new HashMap<>();
        config.put("prefix", "Test prefix");
        config.put("temperature", 0.7);
        
        return AgenticFlow.builder()
            .id(new AgenticFlowId(UUID.randomUUID().toString()))
            .organizationId(new OrganizationId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
            .name("Test Flow")
            .description("Test description")
            .configuration(config)
            .status(AgenticFlowStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1L)
            .build();
    }
}