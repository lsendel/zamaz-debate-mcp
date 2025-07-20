package com.zamaz.mcp.common.domain.agentic.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
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
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;

@ExtendWith(MockitoExtension.class)
class InternalMonologueFlowServiceTest {
    @Mock
    private LlmServicePort llmService;

    @InjectMocks
    private InternalMonologueFlowService internalMonologueFlowService;

    private PromptContext context;
    private AgenticFlowConfiguration configuration;

    @BeforeEach
    void setUp() {
        context = new PromptContext("debate-123", "participant-456");

        Map<String, Object> params = new HashMap<>();
        params.put("prefix", "Let me think step by step.");
        configuration = new AgenticFlowConfiguration(params);
    }

    @Test
    void shouldReturnCorrectFlowType() {
        assertEquals(AgenticFlowType.INTERNAL_MONOLOGUE, internalMonologueFlowService.getFlowType());
    }

    @Test
    void shouldProcessPromptWithInternalMonologue() {
        // Given
        String prompt = "What is the capital of France?";
        String fullResponse = "Let me think about this question.\n\n" +
                "France is a country in Western Europe.\n" +
                "The capital of France is Paris.\n\n" +
                "Final Answer: Paris";

        LlmResponse llmResponse = LlmResponse.builder()
                .text(fullResponse)
                .processingTime(Duration.ofMillis(500))
                .build();

        when(llmService.generateWithInternalMonologue(anyString(), anyMap()))
                .thenReturn(llmResponse);

        // When
        AgenticFlowResult result = internalMonologueFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(prompt, result.getOriginalPrompt());
        assertTrue(result.getEnhancedPrompt().contains(prompt));
        assertTrue(result.getEnhancedPrompt().contains("Let me think step by step."));
        assertEquals(fullResponse, result.getFullResponse());
        assertEquals("Paris", result.getFinalResponse());
        assertEquals("Let me think about this question.\n\n" +
                "France is a country in Western Europe.\n" +
                "The capital of France is Paris.", result.getReasoning());
        assertEquals(1, result.getProcessingSteps().size());
        assertEquals("internal_monologue", result.getProcessingSteps().get(0).getStepType());
        assertTrue(result.isResponseChanged());
        assertTrue((Boolean) result.getMetrics().get("has_reasoning"));
    }

    @Test
    void shouldHandleResponseWithoutFinalAnswerMarker() {
        // Given
        String prompt = "What is the capital of France?";
        String fullResponse = "Let me think about this question.\n\n" +
                "France is a country in Western Europe.\n" +
                "The capital of France is Paris.";

        LlmResponse llmResponse = LlmResponse.builder()
                .text(fullResponse)
                .processingTime(Duration.ofMillis(500))
                .build();

        when(llmService.generateWithInternalMonologue(anyString(), anyMap()))
                .thenReturn(llmResponse);

        // When
        AgenticFlowResult result = internalMonologueFlowService.process(prompt, configuration, context);

        // Then
        assertNotNull(result);
        assertEquals(fullResponse, result.getFullResponse());
        assertTrue(result.getReasoning().contains("Let me think about this question"));
        assertTrue(result.getFinalResponse().contains("Paris"));
    }

    @Test
    void shouldValidateConfiguration() {
        // Given
        Map<String, Object> validParams = new HashMap<>();
        validParams.put("prefix", "Let me think step by step.");
        AgenticFlowConfiguration validConfig = new AgenticFlowConfiguration(validParams);

        Map<String, Object> invalidParams = new HashMap<>();
        invalidParams.put("prefix", 123); // Should be a string
        AgenticFlowConfiguration invalidConfig = new AgenticFlowConfiguration(invalidParams);

        // Then
        assertTrue(internalMonologueFlowService.validateConfiguration(validConfig));
        assertTrue(internalMonologueFlowService.validateConfiguration(new AgenticFlowConfiguration()));
    }
}