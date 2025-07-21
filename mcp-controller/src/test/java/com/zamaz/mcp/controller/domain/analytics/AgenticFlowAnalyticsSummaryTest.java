package com.zamaz.mcp.controller.domain.analytics;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AgenticFlowAnalyticsSummary.
 */
class AgenticFlowAnalyticsSummaryTest {
    
    @Test
    @DisplayName("Should create empty analytics summary")
    void shouldCreateEmptyAnalyticsSummary() {
        // When
        AgenticFlowAnalyticsSummary summary = AgenticFlowAnalyticsSummary.empty(
            AgenticFlowType.INTERNAL_MONOLOGUE
        );
        
        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getFlowType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(summary.getExecutionCount()).isEqualTo(0);
        assertThat(summary.getAverageConfidence()).isEqualTo(0.0);
        assertThat(summary.getSuccessRate()).isEqualTo(0.0);
        assertThat(summary.getAverageExecutionTime()).isEqualTo(Duration.ZERO);
        assertThat(summary.getMetrics()).isEmpty();
        assertThat(summary.hasSignificantData()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate performance grade correctly")
    void shouldCalculatePerformanceGrade() {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("test", "data");
        
        // Test cases
        testPerformanceGrade(95.0, 0.95, "A");
        testPerformanceGrade(85.0, 0.85, "B");
        testPerformanceGrade(75.0, 0.75, "C");
        testPerformanceGrade(65.0, 0.65, "D");
        testPerformanceGrade(50.0, 0.50, "F");
    }
    
    @Test
    @DisplayName("Should identify significant data threshold")
    void shouldIdentifySignificantDataThreshold() {
        // Given
        AgenticFlowAnalyticsSummary lowDataSummary = createSummary(5, 80.0, 0.8);
        AgenticFlowAnalyticsSummary significantDataSummary = createSummary(15, 80.0, 0.8);
        
        // Then
        assertThat(lowDataSummary.hasSignificantData()).isFalse();
        assertThat(significantDataSummary.hasSignificantData()).isTrue();
    }
    
    @Test
    @DisplayName("Should store and retrieve metrics")
    void shouldStoreAndRetrieveMetrics() {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalExecutions", 100);
        metrics.put("averageConfidence", 85.5);
        metrics.put("successRate", 0.92);
        metrics.put("averageExecutionTimeMs", 1500L);
        
        // When
        AgenticFlowAnalyticsSummary summary = AgenticFlowAnalyticsSummary.builder()
            .flowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
            .executionCount(100)
            .averageConfidence(85.5)
            .successRate(0.92)
            .averageExecutionTime(Duration.ofMillis(1500))
            .metrics(metrics)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Then
        assertThat(summary.getMetrics()).containsAllEntriesOf(metrics);
        assertThat(summary.getMetrics().get("totalExecutions")).isEqualTo(100);
        assertThat(summary.getMetrics().get("averageConfidence")).isEqualTo(85.5);
    }
    
    private void testPerformanceGrade(double confidence, double successRate, String expectedGrade) {
        AgenticFlowAnalyticsSummary summary = createSummary(20, confidence, successRate);
        assertThat(summary.getPerformanceGrade()).isEqualTo(expectedGrade);
    }
    
    private AgenticFlowAnalyticsSummary createSummary(
            int executionCount,
            double averageConfidence,
            double successRate) {
        
        return AgenticFlowAnalyticsSummary.builder()
            .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
            .executionCount(executionCount)
            .averageConfidence(averageConfidence)
            .successRate(successRate)
            .averageExecutionTime(Duration.ofMillis(1000))
            .metrics(new HashMap<>())
            .timestamp(LocalDateTime.now())
            .build();
    }
}