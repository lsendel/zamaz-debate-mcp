package com.zamaz.mcp.controller.domain.agentic;

import com.zamaz.mcp.common.domain.agentic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AgenticFlowResult domain entity.
 */
class AgenticFlowResultTest {
    
    @Test
    @DisplayName("Should create valid AgenticFlowResult with basic fields")
    void shouldCreateValidAgenticFlowResult() {
        // Given
        AgenticFlowId flowId = new AgenticFlowId(UUID.randomUUID().toString());
        String finalAnswer = "This is the final answer";
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(flowId)
            .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer(finalAnswer)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(Instant.now())
            .duration(1500L)
            .build();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFlowId()).isEqualTo(flowId);
        assertThat(result.getFlowType()).isEqualTo(AgenticFlowType.INTERNAL_MONOLOGUE);
        assertThat(result.getFinalAnswer()).isEqualTo(finalAnswer);
        assertThat(result.getStatus()).isEqualTo(AgenticFlowStatus.SUCCESS);
        assertThat(result.getDuration()).isEqualTo(1500L);
    }
    
    @Test
    @DisplayName("Should create result with reasoning for Internal Monologue")
    void shouldCreateResultWithReasoning() {
        // Given
        String reasoning = "Step 1: Analyze the problem\nStep 2: Consider options\nStep 3: Formulate answer";
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.INTERNAL_MONOLOGUE)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Final answer")
            .reasoning(reasoning)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(Instant.now())
            .build();
        
        // Then
        assertThat(result.getReasoning()).isEqualTo(reasoning);
    }
    
    @Test
    @DisplayName("Should create result with iterations for Self-Critique Loop")
    void shouldCreateResultWithIterations() {
        // Given
        List<AgenticFlowResult.Iteration> iterations = Arrays.asList(
            AgenticFlowResult.Iteration.builder()
                .iteration(1)
                .content("Initial response")
                .critique("Could be more detailed")
                .revision("More detailed response")
                .build(),
            AgenticFlowResult.Iteration.builder()
                .iteration(2)
                .content("More detailed response")
                .critique("Good, but needs example")
                .revision("Response with example")
                .build()
        );
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.SELF_CRITIQUE_LOOP)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Response with example")
            .iterations(iterations)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(Instant.now())
            .build();
        
        // Then
        assertThat(result.getIterations()).hasSize(2);
        assertThat(result.getIterations().get(0).getIteration()).isEqualTo(1);
        assertThat(result.getIterations().get(1).getRevision()).isEqualTo("Response with example");
    }
    
    @Test
    @DisplayName("Should create result with confidence score")
    void shouldCreateResultWithConfidence() {
        // Given
        Double confidence = 85.5;
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.CONFIDENCE_SCORING)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Answer with confidence")
            .confidence(confidence)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(Instant.now())
            .build();
        
        // Then
        assertThat(result.getConfidence()).isEqualTo(confidence);
    }
    
    @Test
    @DisplayName("Should create result with tool calls")
    void shouldCreateResultWithToolCalls() {
        // Given
        List<AgenticFlowResult.ToolCall> toolCalls = Arrays.asList(
            AgenticFlowResult.ToolCall.builder()
                .tool("web_search")
                .query("latest AI developments")
                .result("Found 10 relevant articles")
                .timestamp(Instant.now().toString())
                .build(),
            AgenticFlowResult.ToolCall.builder()
                .tool("calculator")
                .query("sqrt(144)")
                .result("12")
                .timestamp(Instant.now().toString())
                .build()
        );
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.TOOL_CALLING_VERIFICATION)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Based on verification...")
            .toolCalls(toolCalls)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(Instant.now())
            .build();
        
        // Then
        assertThat(result.getToolCalls()).hasSize(2);
        assertThat(result.getToolCalls().get(0).getTool()).isEqualTo("web_search");
        assertThat(result.getToolCalls().get(1).getResult()).isEqualTo("12");
    }
    
    @Test
    @DisplayName("Should handle failed execution")
    void shouldHandleFailedExecution() {
        // Given
        String errorMessage = "LLM service unavailable";
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.TREE_OF_THOUGHTS)
            .executionId(UUID.randomUUID().toString())
            .status(AgenticFlowStatus.FAILED)
            .error(errorMessage)
            .timestamp(Instant.now())
            .duration(500L)
            .build();
        
        // Then
        assertThat(result.getStatus()).isEqualTo(AgenticFlowStatus.FAILED);
        assertThat(result.getError()).isEqualTo(errorMessage);
        assertThat(result.getFinalAnswer()).isNull();
    }
    
    @Test
    @DisplayName("Should create result with perspectives for Multi-Agent Red Team")
    void shouldCreateResultWithPerspectives() {
        // Given
        AgenticFlowResult.Perspectives perspectives = AgenticFlowResult.Perspectives.builder()
            .architect("Here's my solution...")
            .skeptic("I see potential issues...")
            .judge("After considering both perspectives...")
            .build();
        
        // When
        AgenticFlowResult result = AgenticFlowResult.builder()
            .flowId(new AgenticFlowId(UUID.randomUUID().toString()))
            .flowType(AgenticFlowType.MULTI_AGENT_RED_TEAM)
            .executionId(UUID.randomUUID().toString())
            .finalAnswer("Judge's final decision")
            .perspectives(perspectives)
            .status(AgenticFlowStatus.SUCCESS)
            .timestamp(Instant.now())
            .build();
        
        // Then
        assertThat(result.getPerspectives()).isNotNull();
        assertThat(result.getPerspectives().getArchitect()).startsWith("Here's my solution");
        assertThat(result.getPerspectives().getSkeptic()).startsWith("I see potential issues");
        assertThat(result.getPerspectives().getJudge()).startsWith("After considering");
    }
}