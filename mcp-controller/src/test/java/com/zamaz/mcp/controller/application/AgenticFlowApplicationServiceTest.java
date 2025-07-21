package com.zamaz.mcp.controller.application;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.port.out.AgenticFlowRepository;
import com.zamaz.mcp.controller.port.out.LlmServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgenticFlowApplicationService.
 */
@ExtendWith(MockitoExtension.class)
class AgenticFlowApplicationServiceTest {
    
    @Mock
    private AgenticFlowRepository flowRepository;
    
    @Mock
    private LlmServicePort llmService;
    
    @Mock
    private AgenticFlowDomainService domainService;
    
    private AgenticFlowApplicationService service;
    
    @BeforeEach
    void setUp() {
        service = new AgenticFlowApplicationService(
            flowRepository, 
            llmService, 
            domainService
        );
    }
    
    @Test
    @DisplayName("Should create new agentic flow successfully")
    void shouldCreateNewAgenticFlow() {
        // Given
        OrganizationId orgId = new OrganizationId(UUID.randomUUID().toString());
        Map<String, Object> config = new HashMap<>();
        config.put("prefix", "Think step by step");
        
        AgenticFlow flow = createTestFlow(orgId, config);
        
        when(flowRepository.save(any(AgenticFlow.class))).thenReturn(flow);
        
        // When
        AgenticFlow result = service.createFlow(
            orgId,
            AgenticFlowType.INTERNAL_MONOLOGUE,
            "Test Flow",
            "Description",
            config
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Flow");
        assertThat(result.getFlowType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        verify(flowRepository).save(any(AgenticFlow.class));
    }
    
    @Test
    @DisplayName("Should execute agentic flow successfully")
    void shouldExecuteAgenticFlow() throws Exception {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        String prompt = "What is the capital of France?";
        
        AgenticFlow flow = createTestFlow();
        AgenticFlowResult expectedResult = createSuccessfulResult(flowId);
        
        when(flowRepository.findById(flowId)).thenReturn(Optional.of(flow));
        when(domainService.execute(eq(flow), eq(prompt), any())).thenReturn(expectedResult);
        
        // When
        CompletableFuture<AgenticFlowResult> futureResult = service.executeFlow(
            flowId.getValue(),
            prompt,
            UUID.randomUUID().toString(),
            null
        );
        
        AgenticFlowResult result = futureResult.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AgenticFlowStatus.SUCCESS);
        assertThat(result.getFinalAnswer()).isEqualTo("Paris is the capital of France");
        verify(domainService).execute(eq(flow), eq(prompt), any());
    }
    
    @Test
    @DisplayName("Should handle flow execution failure")
    void shouldHandleFlowExecutionFailure() throws Exception {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        String prompt = "Test prompt";
        
        AgenticFlow flow = createTestFlow();
        
        when(flowRepository.findById(flowId)).thenReturn(Optional.of(flow));
        when(domainService.execute(eq(flow), eq(prompt), any()))
            .thenThrow(new RuntimeException("LLM service error"));
        
        // When
        CompletableFuture<AgenticFlowResult> futureResult = service.executeFlow(
            flowId.getValue(),
            prompt,
            UUID.randomUUID().toString(),
            null
        );
        
        // Then
        assertThatThrownBy(() -> futureResult.get())
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("LLM service error");
    }
    
    @Test
    @DisplayName("Should update flow configuration")
    void shouldUpdateFlowConfiguration() {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        AgenticFlow existingFlow = createTestFlow();
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("prefix", "Updated prefix");
        newConfig.put("temperature", 0.9);
        
        when(flowRepository.findById(flowId)).thenReturn(Optional.of(existingFlow));
        when(flowRepository.save(any(AgenticFlow.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        AgenticFlow result = service.updateFlowConfiguration(flowId, newConfig);
        
        // Then
        assertThat(result.getConfiguration()).isEqualTo(newConfig);
        assertThat(result.getVersion()).isEqualTo(2L);
        verify(flowRepository).save(existingFlow);
    }
    
    @Test
    @DisplayName("Should deactivate flow")
    void shouldDeactivateFlow() {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        AgenticFlow flow = createTestFlow();
        
        when(flowRepository.findById(flowId)).thenReturn(Optional.of(flow));
        when(flowRepository.save(any(AgenticFlow.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        service.deactivateFlow(flowId);
        
        // Then
        assertThat(flow.getStatus()).isEqualTo(AgenticFlowStatus.INACTIVE);
        verify(flowRepository).save(flow);
    }
    
    @Test
    @DisplayName("Should list flows by organization")
    void shouldListFlowsByOrganization() {
        // Given
        OrganizationId orgId = new OrganizationId(UUID.randomUUID().toString());
        List<AgenticFlow> flows = Arrays.asList(
            createTestFlow(orgId, AgenticFlowType.INTERNAL_MONOLOGUE),
            createTestFlow(orgId, AgenticFlowType.SELF_CRITIQUE_LOOP),
            createTestFlow(orgId, AgenticFlowType.MULTI_AGENT_RED_TEAM)
        );
        
        when(flowRepository.findByOrganizationId(orgId)).thenReturn(flows);
        
        // When
        List<AgenticFlow> result = service.listFlowsByOrganization(orgId);
        
        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(AgenticFlow::getFlowType)
            .containsExactly(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                AgenticFlowType.MULTI_AGENT_RED_TEAM
            );
    }
    
    @Test
    @DisplayName("Should throw exception when flow not found")
    void shouldThrowExceptionWhenFlowNotFound() {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        
        when(flowRepository.findById(flowId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> service.executeFlow(
            flowId.getValue(),
            "prompt",
            UUID.randomUUID().toString(),
            null
        ).get())
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Flow not found");
    }
    
    private AgenticFlow createTestFlow() {
        return createTestFlow(
            new OrganizationId(UUID.randomUUID().toString()),
            new HashMap<>()
        );
    }
    
    private AgenticFlow createTestFlow(OrganizationId orgId, Map<String, Object> config) {
        return createTestFlow(orgId, AgenticFlowType.INTERNAL_MONOLOGUE, config);
    }
    
    private AgenticFlow createTestFlow(OrganizationId orgId, AgenticFlowType flowType) {
        return createTestFlow(orgId, flowType, new HashMap<>());
    }
    
    private AgenticFlow createTestFlow(
            OrganizationId orgId, 
            AgenticFlowType flowType,
            Map<String, Object> config) {
        
        return AgenticFlow.builder()
            .id(new AgenticFlowId(UUID.randomUUID().toString()))
            .organizationId(orgId)
            .flowType(flowType)
            .name("Test Flow")
            .description("Test description")
            .configuration(config)
            .status(AgenticFlowStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1L)
            .build();
    }
    
    private AgenticFlowResult createSuccessfulResult(AgenticFlowId flowId) {
        return AgenticFlowResult.builder()
            .flowId(flowId)
            .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Paris is the capital of France")
            .reasoning("Step 1: Recall geography knowledge\nStep 2: France's capital is Paris")
            .status(AgenticFlowStatus.SUCCESS)
            .confidence(95.0)
            .timestamp(Instant.now())
            .duration(1500L)
            .build();
    }
}