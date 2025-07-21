package com.zamaz.mcp.controller.application;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.controller.domain.analytics.TrendingFlowType;
import com.zamaz.mcp.controller.domain.recommendation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgenticFlowRecommendationService.
 */
@ExtendWith(MockitoExtension.class)
class AgenticFlowRecommendationServiceTest {
    
    @Mock
    private AgenticFlowAnalyticsService analyticsService;
    
    private AgenticFlowRecommendationService service;
    
    @BeforeEach
    void setUp() {
        service = new AgenticFlowRecommendationService(analyticsService);
    }
    
    @Test
    @DisplayName("Should recommend flows for fact-checking debate")
    void shouldRecommendFlowsForFactCheckingDebate() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        DebateContext context = DebateContext.builder()
            .topic("Climate change statistics and facts")
            .format(DebateFormat.OXFORD)
            .topicCategories(new HashSet<>(Arrays.asList("science", "factual")))
            .requiresFactChecking(true)
            .isHighStakes(true)
            .build();
        
        List<TrendingFlowType> trendingFlows = Arrays.asList(
            createTrendingFlow(AgenticFlowType.TOOL_CALLING_VERIFICATION, 0.9),
            createTrendingFlow(AgenticFlowType.RAG_WITH_RERANKING, 0.8)
        );
        
        when(analyticsService.getTrendingFlowTypes(eq(organizationId), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(trendingFlows));
        
        when(analyticsService.getFlowTypeAnalytics(eq(organizationId), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalyticsSummary()));
        
        // When
        CompletableFuture<FlowRecommendation> future = 
            service.recommendFlowsForDebate(organizationId, context);
        
        FlowRecommendation recommendation = future.get();
        
        // Then
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.getRecommendations()).isNotEmpty();
        assertThat(recommendation.getTopRecommendation().getFlowType())
            .isEqualTo(AgenticFlowType.TOOL_CALLING_VERIFICATION);
        assertThat(recommendation.getReasoning()).contains("fact");
    }
    
    @Test
    @DisplayName("Should recommend flows for philosophical debate")
    void shouldRecommendFlowsForPhilosophicalDebate() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        DebateContext context = DebateContext.builder()
            .topic("The nature of consciousness")
            .format(DebateFormat.SOCRATIC)
            .topicCategories(new HashSet<>(Arrays.asList("philosophy", "ethics")))
            .requiresDeepReasoning(true)
            .build();
        
        when(analyticsService.getTrendingFlowTypes(eq(organizationId), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
        
        when(analyticsService.getFlowTypeAnalytics(eq(organizationId), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalyticsSummary()));
        
        // When
        FlowRecommendation recommendation = 
            service.recommendFlowsForDebate(organizationId, context).get();
        
        // Then
        assertThat(recommendation.getRecommendations())
            .extracting(FlowTypeRecommendation::getFlowType)
            .contains(
                AgenticFlowType.INTERNAL_MONOLOGUE,
                AgenticFlowType.STEP_BACK_PROMPTING
            );
    }
    
    @Test
    @DisplayName("Should adjust recommendations for participant with large model")
    void shouldAdjustRecommendationsForParticipant() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        DebateContext debateContext = createDebateContext();
        ParticipantContext participantContext = ParticipantContext.builder()
            .name("Expert AI")
            .modelProvider("OpenAI")
            .modelName("gpt-4-turbo")
            .role(ParticipantRole.EXPERT)
            .temperature(0.7)
            .maxTokens(4000)
            .build();
        
        List<TrendingFlowType> trendingFlows = Arrays.asList(
            createTrendingFlow(AgenticFlowType.TREE_OF_THOUGHTS, 0.7),
            createTrendingFlow(AgenticFlowType.MULTI_AGENT_RED_TEAM, 0.8)
        );
        
        when(analyticsService.getTrendingFlowTypes(eq(organizationId), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(trendingFlows));
        
        when(analyticsService.getFlowTypeAnalytics(eq(organizationId), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createAnalyticsSummary()));
        
        // When
        FlowRecommendation recommendation = service.recommendFlowsForParticipant(
            organizationId, participantContext, debateContext
        ).get();
        
        // Then
        assertThat(recommendation.getParticipantContext()).isEqualTo(participantContext);
        // Complex flows should be boosted for large models
        assertThat(recommendation.getRecommendations())
            .extracting(FlowTypeRecommendation::getFlowType)
            .containsAnyOf(
                AgenticFlowType.TREE_OF_THOUGHTS,
                AgenticFlowType.MULTI_AGENT_RED_TEAM
            );
    }
    
    @Test
    @DisplayName("Should provide adaptive recommendations for poor performance")
    void shouldProvideAdaptiveRecommendationsForPoorPerformance() throws Exception {
        // Given
        UUID debateId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        PerformanceContext poorPerformance = PerformanceContext.builder()
            .currentFlowType(AgenticFlowType.TREE_OF_THOUGHTS)
            .executionCount(20)
            .averageConfidence(45.0) // Low confidence
            .responseChangeRate(0.4) // High change rate
            .averageExecutionTime(8000L) // Slow
            .recentErrors(5)
            .recentSuccessRate(0.5)
            .build();
        
        // When
        AdaptiveFlowRecommendation adaptive = 
            service.getAdaptiveRecommendations(debateId, participantId, poorPerformance).get();
        
        // Then
        assertThat(adaptive.shouldSwitch()).isTrue();
        assertThat(adaptive.getSwitchUrgency()).isIn(SwitchUrgency.HIGH, SwitchUrgency.MEDIUM);
        assertThat(adaptive.getRecommendations()).isNotEmpty();
        assertThat(adaptive.getPerformanceAnalysis().hasSignificantIssues()).isTrue();
    }
    
    @Test
    @DisplayName("Should not recommend switch for good performance")
    void shouldNotRecommendSwitchForGoodPerformance() throws Exception {
        // Given
        UUID debateId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        PerformanceContext goodPerformance = PerformanceContext.builder()
            .currentFlowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
            .executionCount(50)
            .averageConfidence(88.0)
            .responseChangeRate(0.05)
            .averageExecutionTime(2000L)
            .recentErrors(0)
            .recentSuccessRate(0.98)
            .build();
        
        // When
        AdaptiveFlowRecommendation adaptive = 
            service.getAdaptiveRecommendations(debateId, participantId, goodPerformance).get();
        
        // Then
        assertThat(adaptive.shouldSwitch()).isFalse();
        assertThat(adaptive.getSwitchUrgency()).isEqualTo(SwitchUrgency.NONE);
        assertThat(adaptive.getPerformanceAnalysis().isPerformanceGood()).isTrue();
    }
    
    private DebateContext createDebateContext() {
        return DebateContext.builder()
            .topic("General debate topic")
            .format(DebateFormat.OXFORD)
            .topicCategories(new HashSet<>(Arrays.asList("general")))
            .expectedRounds(5)
            .build();
    }
    
    private TrendingFlowType createTrendingFlow(AgenticFlowType flowType, double trendScore) {
        return TrendingFlowType.builder()
            .flowType(flowType)
            .usageCount(100)
            .averageConfidence(85.0)
            .successRate(0.9)
            .averageExecutionTime(Duration.ofMillis(2000))
            .trendScore(trendScore)
            .build();
    }
    
    private com.zamaz.mcp.controller.domain.analytics.AgenticFlowAnalyticsSummary createAnalyticsSummary() {
        return com.zamaz.mcp.controller.domain.analytics.AgenticFlowAnalyticsSummary.builder()
            .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
            .executionCount(50)
            .averageConfidence(85.0)
            .successRate(0.9)
            .averageExecutionTime(Duration.ofMillis(1500))
            .metrics(new HashMap<>())
            .timestamp(java.time.LocalDateTime.now())
            .build();
    }
}