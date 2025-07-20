package com.zamaz.mcp.common.domain.agentic.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.CritiqueIteration;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;

@ExtendWith(MockitoExtension.class)
class SelfCritiqueLoopFlowServiceTest {
    @Mock
    private LlmServicePort llmService;

    @InjectMocks
    private SelfCritiqueLoopFlowService selfCritiqueLoopFlowService;

    private PromptContext context;
    private AgenticFlowConfiguration configuration;

    @BeforeEach
    void setUp() {
        context = new PromptContext("debate-123", "participant-456");

        Map<String, Object> params = new HashMap<>();
        params.put("iterations", 2);
        configuration = new AgenticFlowConfiguration(params);
    }

    @Test
    void shouldReturnCorrectFlowType() {
        assertEquals(AgenticFlowType.SELF_CRITIQUE_LOOP, selfCritiqueLoopFlowService.getFlowType());
    }

    @Test
    void shouldProcessPromptWithSelfCritiqueLoop() {
        // Given
        String prompt = "What is the capital of France?";
        String initialResponse = "The capital of France is Paris.";
        String critique = "This response is correct but lacks detail. It would be better to provide some context about Paris.";
        String revisedResponse = "The capital of France is Paris, which is also the largest city in the country and a major European cultural center.";

        List<CritiqueIteration> iterations = new ArrayList<>();
        iterations.add(new CritiqueIteration(critique, revisedResponse));

        LlmResponse llmResponse = LlmResponse.builder()
                .text(revisedResponse)
                .processingTime(Duration.ofMillis(1000))
                .iterations(iterations)
                .build();

        when(llmService.generateWithSelfCritique(anyString(), anyMap(), anyInt()))
                .thenReturn(llmResponse);

        // When
        AgenticFlowResult result = selfCritiqueLoopFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(prompt, result.getOriginalPrompt());
        assertEquals(revisedResponse, result.getFullResponse());
        assertEquals(revisedResponse, result.getFinalResponse());
        assertEquals(3, result.getProcessingSteps().size()); // Initial + Critique + Revision
        assertTrue(result.isResponseChanged());
        assertEquals(2, result.getMetrics().get("iterations_count"));
        assertEquals(1, result.getMetrics().get("actual_iterations"));
        assertEquals("self_critique_loop", result.getMetrics().get("visualization_type"));
        assertTrue((Boolean) result.getMetrics().get("changes_detected"));
    }

    @Test
    void shouldHandleNoIterations() {
        // Given
        String prompt = "What is the capital of France?";
        String response = "The capital of France is Paris.";

        LlmResponse llmResponse = LlmResponse.builder()
                .text(response)
                .processingTime(Duration.ofMillis(500))
                .iterations(new ArrayList<>())
                .build();

        when(llmService.generateWithSelfCritique(anyString(), anyMap(), anyInt()))
                .thenReturn(llmResponse);

        // When
        AgenticFlowResult result = selfCritiqueLoopFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(prompt, result.getOriginalPrompt());
        assertEquals(response, result.getFullResponse());
        assertEquals(response, result.getFinalResponse());
        assertEquals(1, result.getProcessingSteps().size()); // Only initial step
        assertFalse(result.isResponseChanged());
        assertFalse((Boolean) result.getMetrics().get("changes_detected"));
    }

    @Test
    void shouldLimitIterationsToMaximum() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("iterations", 5); // More than maximum (3)
        AgenticFlowConfiguration config = new AgenticFlowConfiguration(params);

        String prompt = "What is the capital of France?";
        String response = "The capital of France is Paris.";

        LlmResponse llmResponse = LlmResponse.builder()
                .text(response)
                .processingTime(Duration.ofMillis(500))
                .build();

        when(llmService.generateWithSelfCritique(anyString(), anyMap(), eq(3))) // Should be limited to 3
                .thenReturn(llmResponse);

        // When
        AgenticFlowResult result = selfCritiqueLoopFlowService.process(prompt, config, context);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getMetrics().get("iterations_count")); // Limited to 3
    }

    @Test
    void shouldValidateConfiguration() {
        // Given
        Map<String, Object> validParams = new HashMap<>();
        validParams.put("iterations", 2);
        AgenticFlowConfiguration validConfig = new AgenticFlowConfiguration(validParams);

        Map<String, Object> invalidTypeParams = new HashMap<>();
        invalidTypeParams.put("iterations", "two"); // Should be an integer
        AgenticFlowConfiguration invalidTypeConfig = new AgenticFlowConfiguration(invalidTypeParams);

        Map<String, Object> invalidValueParams = new HashMap<>();
        invalidValueParams.put("iterations", 0); // Should be at least 1
        AgenticFlowConfiguration invalidValueConfig = new AgenticFlowConfiguration(invalidValueParams);

        Map<String, Object> invalidMaxParams = new HashMap<>();
        invalidMaxParams.put("iterations", 4); // Should be at most 3
        AgenticFlowConfiguration invalidMaxConfig = new AgenticFlowConfiguration(invalidMaxParams);

        // Then
        assertTrue(selfCritiqueLoopFlowService.validateConfiguration(validConfig));
        assertTrue(selfCritiqueLoopFlowService.validateConfiguration(new AgenticFlowConfiguration()));
        assertFalse(selfCritiqueLoopFlowService.validateConfiguration(invalidTypeConfig));
        assertFalse(selfCritiqueLoopFlowService.validateConfiguration(invalidValueConfig));
        assertFalse(selfCritiqueLoopFlowService.validateConfiguration(invalidMaxConfig));
    }

    @Test
    void shouldTrackChangesBetweenIterations() {
        // Given
        String prompt = "What is the capital of France?";
        String initialResponse = "The capital of France is Lyon."; // Incorrect
        String critique = "This response is incorrect. The capital of France is Paris, not Lyon.";
        String revisedResponse = "The capital of France is Paris, not Lyon as I incorrectly stated.";

        List<CritiqueIteration> iterations = new ArrayList<>();
        iterations.add(new CritiqueIteration(critique, revisedResponse));

        LlmResponse llmResponse = LlmResponse.builder()
                .text(revisedResponse)
                .processingTime(Duration.ofMillis(1000))
                .iterations(iterations)
                .build();

        when(llmService.generateWithSelfCritique(anyString(), anyMap(), anyInt()))
                .thenReturn(llmResponse);

        // When
        AgenticFlowResult result = selfCritiqueLoopFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertTrue(result.isResponseChanged());
        assertTrue((Boolean) result.getMetrics().get("changes_detected"));
        assertTrue((Double) result.getMetrics().get("change_percentage") > 0.0);
    }
}