package com.zamaz.mcp.controller.domain.recommendation;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for flow recommendation domain models.
 */
class FlowRecommendationTest {
    
    @Test
    @DisplayName("Should create valid FlowRecommendation")
    void shouldCreateValidFlowRecommendation() {
        // Given
        DebateContext context = createDebateContext();
        List<FlowTypeRecommendation> recommendations = Arrays.asList(
            createRecommendation(AgenticFlowType.TOOL_CALLING_VERIFICATION, 0.95),
            createRecommendation(AgenticFlowType.SELF_CRITIQUE_LOOP, 0.82),
            createRecommendation(AgenticFlowType.INTERNAL_MONOLOGUE, 0.75)
        );
        
        // When
        FlowRecommendation flowRec = FlowRecommendation.builder()
            .context(context)
            .recommendations(recommendations)
            .reasoning("Based on debate requirements...")
            .timestamp(LocalDateTime.now())
            .build();
        
        // Then
        assertThat(flowRec.getTopRecommendation()).isNotNull();
        assertThat(flowRec.getTopRecommendation().getFlowType())
            .isEqualTo(AgenticFlowType.TOOL_CALLING_VERIFICATION);
        assertThat(flowRec.hasHighConfidenceRecommendations()).isTrue();
    }
    
    @Test
    @DisplayName("Should identify debate context characteristics")
    void shouldIdentifyDebateContextCharacteristics() {
        // Given
        Set<String> philosophicalCategories = new HashSet<>(Arrays.asList("philosophy", "ethics"));
        Set<String> factualCategories = new HashSet<>(Arrays.asList("science", "statistics"));
        
        DebateContext philosophicalDebate = DebateContext.builder()
            .topic("Is consciousness an emergent property?")
            .topicCategories(philosophicalCategories)
            .format(DebateFormat.SOCRATIC)
            .build();
        
        DebateContext factualDebate = DebateContext.builder()
            .topic("Climate change statistics")
            .topicCategories(factualCategories)
            .requiresFactChecking(true)
            .format(DebateFormat.OXFORD)
            .build();
        
        // Then
        assertThat(philosophicalDebate.isPhilosophical()).isTrue();
        assertThat(philosophicalDebate.requiresDeepReasoning()).isFalse();
        assertThat(philosophicalDebate.hasFactualClaims()).isFalse();
        
        assertThat(factualDebate.hasFactualClaims()).isTrue();
        assertThat(factualDebate.requiresFactChecking()).isTrue();
        assertThat(factualDebate.isPhilosophical()).isFalse();
    }
    
    @Test
    @DisplayName("Should evaluate FlowTypeRecommendation levels")
    void shouldEvaluateFlowTypeRecommendationLevels() {
        // Given
        FlowTypeRecommendation highlyRecommended = createRecommendation(
            AgenticFlowType.RAG_WITH_RERANKING, 0.85
        );
        FlowTypeRecommendation recommended = createRecommendation(
            AgenticFlowType.CONFIDENCE_SCORING, 0.65
        );
        FlowTypeRecommendation suitable = createRecommendation(
            AgenticFlowType.ENSEMBLE_VOTING, 0.45
        );
        FlowTypeRecommendation notRecommended = createRecommendation(
            AgenticFlowType.TREE_OF_THOUGHTS, 0.3
        );
        
        // Then
        assertThat(highlyRecommended.isHighlyRecommended()).isTrue();
        assertThat(highlyRecommended.getRecommendationLevel()).isEqualTo("Highly Recommended");
        
        assertThat(recommended.isRecommended()).isTrue();
        assertThat(recommended.getRecommendationLevel()).isEqualTo("Recommended");
        
        assertThat(suitable.getRecommendationLevel()).isEqualTo("Suitable");
        
        assertThat(notRecommended.getRecommendationLevel()).isEqualTo("Not Recommended");
    }
    
    @Test
    @DisplayName("Should handle participant context")
    void shouldHandleParticipantContext() {
        // Given
        ParticipantContext participant = ParticipantContext.builder()
            .name("Expert Debater")
            .modelProvider("OpenAI")
            .modelName("gpt-4-turbo")
            .role(ParticipantRole.EXPERT)
            .temperature(0.7)
            .maxTokens(3000)
            .build();
        
        // Then
        assertThat(participant.hasLargeModel()).isTrue();
        assertThat(participant.isHighTemperature()).isFalse();
        assertThat(participant.isLowTemperature()).isFalse();
        assertThat(participant.hasHighTokenLimit()).isTrue();
    }
    
    @Test
    @DisplayName("Should evaluate performance context")
    void shouldEvaluatePerformanceContext() {
        // Given
        PerformanceContext goodPerformance = PerformanceContext.builder()
            .currentFlowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
            .executionCount(50)
            .averageConfidence(85.0)
            .responseChangeRate(0.05)
            .averageExecutionTime(2000L)
            .recentErrors(0)
            .recentSuccessRate(0.95)
            .build();
        
        PerformanceContext poorPerformance = PerformanceContext.builder()
            .currentFlowType(AgenticFlowType.TREE_OF_THOUGHTS)
            .executionCount(30)
            .averageConfidence(55.0)
            .responseChangeRate(0.4)
            .averageExecutionTime(8000L)
            .recentErrors(5)
            .recentSuccessRate(0.6)
            .build();
        
        // Then
        assertThat(goodPerformance.hasPerformanceIssues()).isFalse();
        assertThat(goodPerformance.isPerformingWell()).isTrue();
        
        assertThat(poorPerformance.hasPerformanceIssues()).isTrue();
        assertThat(poorPerformance.isPerformingWell()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle adaptive recommendations")
    void shouldHandleAdaptiveRecommendations() {
        // Given
        PerformanceAnalysis analysis = PerformanceAnalysis.builder()
            .issues(Arrays.asList("Low confidence", "High latency"))
            .strengths(Arrays.asList("Good accuracy"))
            .overallScore(0.35)
            .hasSignificantIssues(true)
            .build();
        
        AdaptiveFlowRecommendation adaptive = AdaptiveFlowRecommendation.builder()
            .currentFlowType(AgenticFlowType.TREE_OF_THOUGHTS)
            .performanceAnalysis(analysis)
            .shouldSwitch(true)
            .switchUrgency(SwitchUrgency.HIGH)
            .recommendations(Arrays.asList(
                createRecommendation(AgenticFlowType.CONFIDENCE_SCORING, 0.8)
            ))
            .timestamp(LocalDateTime.now())
            .build();
        
        // Then
        assertThat(adaptive.requiresImmediateAction()).isTrue();
        assertThat(adaptive.getBestAlternative()).isNotNull();
        assertThat(adaptive.getBestAlternative().getFlowType())
            .isEqualTo(AgenticFlowType.CONFIDENCE_SCORING);
    }
    
    @Test
    @DisplayName("Should evaluate switch urgency")
    void shouldEvaluateSwitchUrgency() {
        // Then
        assertThat(SwitchUrgency.HIGH.requiresAction()).isTrue();
        assertThat(SwitchUrgency.HIGH.isUrgent()).isTrue();
        assertThat(SwitchUrgency.MEDIUM.isUrgent()).isTrue();
        assertThat(SwitchUrgency.LOW.isUrgent()).isFalse();
        assertThat(SwitchUrgency.NONE.requiresAction()).isFalse();
    }
    
    private DebateContext createDebateContext() {
        Set<String> categories = new HashSet<>(Arrays.asList("science", "factual"));
        
        return DebateContext.builder()
            .topic("Climate change facts")
            .format(DebateFormat.OXFORD)
            .topicCategories(categories)
            .expectedRounds(5)
            .isHighStakes(true)
            .requiresFactChecking(true)
            .build();
    }
    
    private FlowTypeRecommendation createRecommendation(AgenticFlowType type, double score) {
        return FlowTypeRecommendation.builder()
            .flowType(type)
            .score(score)
            .reasons(Arrays.asList("Good for this context"))
            .expectedBenefits(Arrays.asList("Higher accuracy"))
            .potentialDrawbacks(Arrays.asList("Slower execution"))
            .build();
    }
}