package com.zamaz.mcp.controller.application;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.controller.domain.analytics.*;
import com.zamaz.mcp.controller.port.out.AgenticFlowAnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgenticFlowAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class AgenticFlowAnalyticsServiceTest {
    
    @Mock
    private AgenticFlowAnalyticsRepository analyticsRepository;
    
    private AgenticFlowAnalyticsService service;
    
    @BeforeEach
    void setUp() {
        service = new AgenticFlowAnalyticsService(analyticsRepository);
    }
    
    @Test
    @DisplayName("Should record flow execution successfully")
    void shouldRecordFlowExecution() throws Exception {
        // Given
        UUID flowId = UUID.randomUUID();
        UUID debateId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Duration executionTime = Duration.ofMillis(1500);
        
        AgenticFlowResult result = createFlowResult();
        AgenticFlowExecution savedExecution = createFlowExecution(flowId, debateId, organizationId);
        
        when(analyticsRepository.save(any(AgenticFlowExecution.class)))
            .thenReturn(savedExecution);
        
        // When
        CompletableFuture<AgenticFlowExecution> future = service.recordExecution(
            flowId, debateId, organizationId, result, executionTime
        );
        
        AgenticFlowExecution execution = future.get();
        
        // Then
        assertThat(execution).isNotNull();
        assertThat(execution.getFlowId()).isEqualTo(flowId);
        assertThat(execution.getDebateId()).isEqualTo(debateId);
        assertThat(execution.getFlowType()).isEqualTo(result.getFlowType());
        assertThat(execution.getConfidence()).isEqualTo(result.getConfidence());
        verify(analyticsRepository).save(any(AgenticFlowExecution.class));
    }
    
    @Test
    @DisplayName("Should record performance metrics")
    void shouldRecordPerformanceMetrics() throws Exception {
        // Given
        UUID executionId = UUID.randomUUID();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("llmResponseTime", 1200L);
        metrics.put("toolCallTime", 300L);
        metrics.put("totalTokens", 500);
        metrics.put("promptTokens", 200);
        metrics.put("completionTokens", 300);
        
        // When
        CompletableFuture<Void> future = service.recordPerformanceMetrics(executionId, metrics);
        future.get();
        
        // Then
        verify(analyticsRepository).savePerformanceMetrics(any(AgenticFlowPerformanceMetrics.class));
    }
    
    @Test
    @DisplayName("Should get flow type analytics")
    void shouldGetFlowTypeAnalytics() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        AgenticFlowType flowType = AgenticFlowType.SELF_CRITIQUE_LOOP;
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<AgenticFlowExecution> executions = Arrays.asList(
            createFlowExecution(flowType, 90.0, AgenticFlowStatus.SUCCESS, 1500L),
            createFlowExecution(flowType, 85.0, AgenticFlowStatus.SUCCESS, 2000L),
            createFlowExecution(flowType, 75.0, AgenticFlowStatus.FAILED, 1000L)
        );
        
        when(analyticsRepository.findByOrganizationAndFlowTypeAndDateRange(
            organizationId, flowType, startDate, endDate
        )).thenReturn(executions);
        
        // When
        CompletableFuture<AgenticFlowAnalyticsSummary> future = 
            service.getFlowTypeAnalytics(organizationId, flowType, startDate, endDate);
        
        AgenticFlowAnalyticsSummary summary = future.get();
        
        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getFlowType()).isEqualTo(flowType);
        assertThat(summary.getExecutionCount()).isEqualTo(3);
        assertThat(summary.getAverageConfidence()).isEqualTo(83.33, within(0.01));
        assertThat(summary.getSuccessRate()).isEqualTo(0.67, within(0.01));
        assertThat(summary.getAverageExecutionTime().toMillis()).isEqualTo(1500L);
    }
    
    @Test
    @DisplayName("Should get debate analytics")
    void shouldGetDebateAnalytics() throws Exception {
        // Given
        UUID debateId = UUID.randomUUID();
        
        List<AgenticFlowExecution> executions = Arrays.asList(
            createFlowExecution(AgenticFlowType.INTERNAL_MONOLOGUE, 85.0, AgenticFlowStatus.SUCCESS, 1000L),
            createFlowExecution(AgenticFlowType.SELF_CRITIQUE_LOOP, 90.0, AgenticFlowStatus.SUCCESS, 2000L),
            createFlowExecution(AgenticFlowType.TOOL_CALLING_VERIFICATION, 80.0, AgenticFlowStatus.SUCCESS, 3000L)
        );
        
        when(analyticsRepository.findByDebateId(debateId)).thenReturn(executions);
        
        // When
        CompletableFuture<DebateAgenticFlowAnalytics> future = 
            service.getDebateAnalytics(debateId);
        
        DebateAgenticFlowAnalytics analytics = future.get();
        
        // Then
        assertThat(analytics).isNotNull();
        assertThat(analytics.getDebateId()).isEqualTo(debateId);
        assertThat(analytics.getTotalExecutions()).isEqualTo(3);
        assertThat(analytics.getAverageConfidence()).isEqualTo(85.0);
        assertThat(analytics.getSuccessRate()).isEqualTo(1.0);
        assertThat(analytics.getFlowTypeSummaries()).hasSize(3);
    }
    
    @Test
    @DisplayName("Should get trending flow types")
    void shouldGetTrendingFlowTypes() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        int limit = 5;
        
        List<AgenticFlowExecution> recentExecutions = generateMixedExecutions();
        
        when(analyticsRepository.findByOrganizationAndDateRange(
            eq(organizationId), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(recentExecutions);
        
        // When
        CompletableFuture<List<TrendingFlowType>> future = 
            service.getTrendingFlowTypes(organizationId, limit);
        
        List<TrendingFlowType> trending = future.get();
        
        // Then
        assertThat(trending).hasSize(Math.min(limit, 3)); // We have 3 flow types
        assertThat(trending.get(0).getTrendScore()).isGreaterThan(0.0);
        assertThat(trending).isSortedAccordingTo(
            Comparator.comparing(TrendingFlowType::getTrendScore).reversed()
        );
    }
    
    @Test
    @DisplayName("Should compare flow types")
    void shouldCompareFlowTypes() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        Set<AgenticFlowType> flowTypes = new HashSet<>(Arrays.asList(
            AgenticFlowType.INTERNAL_MONOLOGUE,
            AgenticFlowType.SELF_CRITIQUE_LOOP
        ));
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(analyticsRepository.findByOrganizationAndFlowTypeAndDateRange(
            organizationId, AgenticFlowType.INTERNAL_MONOLOGUE, startDate, endDate
        )).thenReturn(Arrays.asList(
            createFlowExecution(AgenticFlowType.INTERNAL_MONOLOGUE, 80.0, AgenticFlowStatus.SUCCESS, 1000L),
            createFlowExecution(AgenticFlowType.INTERNAL_MONOLOGUE, 85.0, AgenticFlowStatus.SUCCESS, 1200L)
        ));
        
        when(analyticsRepository.findByOrganizationAndFlowTypeAndDateRange(
            organizationId, AgenticFlowType.SELF_CRITIQUE_LOOP, startDate, endDate
        )).thenReturn(Arrays.asList(
            createFlowExecution(AgenticFlowType.SELF_CRITIQUE_LOOP, 90.0, AgenticFlowStatus.SUCCESS, 2000L),
            createFlowExecution(AgenticFlowType.SELF_CRITIQUE_LOOP, 95.0, AgenticFlowStatus.SUCCESS, 2200L)
        ));
        
        // When
        CompletableFuture<FlowTypeComparison> future = 
            service.compareFlowTypes(organizationId, flowTypes, startDate, endDate);
        
        FlowTypeComparison comparison = future.get();
        
        // Then
        assertThat(comparison).isNotNull();
        assertThat(comparison.getFlowTypes()).isEqualTo(flowTypes);
        assertThat(comparison.getBestByConfidence()).isEqualTo(AgenticFlowType.SELF_CRITIQUE_LOOP);
        assertThat(comparison.getFastest()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(comparison.getRecommendation()).isNotEmpty();
    }
    
    private AgenticFlowResult createFlowResult() {
        return AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Test answer")
            .confidence(90.0)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(java.time.Instant.now())
            .duration(1500L)
            .iterations(Arrays.asList(
                AgenticFlowResult.Iteration.builder()
                    .iteration(1)
                    .content("Initial response")
                    .critique("Needs improvement")
                    .revision("Improved response")
                    .build()
            ))
            .build();
    }
    
    private AgenticFlowExecution createFlowExecution(
            UUID flowId, UUID debateId, UUID organizationId) {
        
        return AgenticFlowExecution.builder()
            .id(UUID.randomUUID())
            .flowId(flowId)
            .debateId(debateId)
            .organizationId(organizationId)
            .flowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
            .executionTime(Duration.ofMillis(1500))
            .status(AgenticFlowStatus.SUCCESS)
            .confidence(90.0)
            .timestamp(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }
    
    private AgenticFlowExecution createFlowExecution(
            AgenticFlowType flowType, 
            Double confidence, 
            AgenticFlowStatus status, 
            Long executionTimeMs) {
        
        return AgenticFlowExecution.builder()
            .id(UUID.randomUUID())
            .flowId(UUID.randomUUID())
            .debateId(UUID.randomUUID())
            .organizationId(UUID.randomUUID())
            .flowType(flowType)
            .executionTime(Duration.ofMillis(executionTimeMs))
            .status(status)
            .confidence(confidence)
            .timestamp(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }
    
    private List<AgenticFlowExecution> generateMixedExecutions() {
        List<AgenticFlowExecution> executions = new ArrayList<>();
        
        // Generate executions for different flow types
        for (int i = 0; i < 10; i++) {
            executions.add(createFlowExecution(
                AgenticFlowType.INTERNAL_MONOLOGUE, 
                80.0 + i, 
                AgenticFlowStatus.SUCCESS, 
                1000L + i * 100
            ));
        }
        
        for (int i = 0; i < 8; i++) {
            executions.add(createFlowExecution(
                AgenticFlowType.SELF_CRITIQUE_LOOP, 
                85.0 + i, 
                i < 7 ? AgenticFlowStatus.SUCCESS : AgenticFlowStatus.FAILED, 
                2000L + i * 100
            ));
        }
        
        for (int i = 0; i < 5; i++) {
            executions.add(createFlowExecution(
                AgenticFlowType.TOOL_CALLING_VERIFICATION, 
                90.0 + i, 
                AgenticFlowStatus.SUCCESS, 
                3000L + i * 100
            ));
        }
        
        return executions;
    }
}