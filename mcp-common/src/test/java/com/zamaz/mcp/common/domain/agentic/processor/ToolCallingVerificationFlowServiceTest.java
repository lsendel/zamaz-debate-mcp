package com.zamaz.mcp.common.domain.agentic.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.ProcessingStep;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;
import com.zamaz.mcp.common.domain.llm.ToolDefinition;
import com.zamaz.mcp.common.domain.tool.ExternalToolPort;
import com.zamaz.mcp.common.domain.tool.ToolCall;
import com.zamaz.mcp.common.domain.tool.ToolResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ToolCallingVerificationFlowServiceTest {

    @Mock
    private LlmServicePort llmService;

    @Mock
    private ExternalToolPort webSearchTool;

    @Mock
    private ExternalToolPort calculatorTool;

    private ObjectMapper objectMapper;
    private ToolCallingVerificationFlowService flowService;
    private AgenticFlowConfiguration configuration;
    private PromptContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up tool adapters
        when(webSearchTool.getToolName()).thenReturn("web_search");
        when(calculatorTool.getToolName()).thenReturn("calculator");

        // Set up object mapper
        objectMapper = new ObjectMapper();

        // Create flow service with mocked dependencies
        flowService = new ToolCallingVerificationFlowService(llmService, List.of(webSearchTool, calculatorTool),
                objectMapper);

        // Set up configuration and context
        configuration = new AgenticFlowConfiguration(new HashMap<>());
        context = new PromptContext("debate-123", "participant-456");
    }

    @Test
    void getFlowType_shouldReturnToolCallingVerification() {
        assertEquals(AgenticFlowType.TOOL_CALLING_VERIFICATION, flowService.getFlowType());
    }

    @Test
    void process_withNoToolCalls_shouldReturnOriginalResponse() {
        // Arrange
        String prompt = "What is the capital of France?";
        String response = "The capital of France is Paris.";

        when(llmService.generateWithToolCalling(anyString(), anyMap(), anyList()))
                .thenReturn(LlmResponse.builder()
                        .text(response)
                        .processingTime(Duration.ofMillis(100))
                        .build());

        // Act
        AgenticFlowResult result = flowService.process(prompt, configuration, context);

        // Assert
        assertEquals(prompt, result.getOriginalPrompt());
        assertEquals(response, result.getFinalResponse());
        assertEquals(response, result.getFullResponse());
        assertFalse(result.isResponseChanged());
        assertEquals(1, result.getProcessingSteps().size());
        assertEquals("initial_response", result.getProcessingSteps().get(0).getStepType());

        // Verify LLM service was called with tool definitions
        ArgumentCaptor<List<ToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmService).generateWithToolCalling(anyString(), anyMap(), toolsCaptor.capture());

        List<ToolDefinition> tools = toolsCaptor.getValue();
        assertEquals(2, tools.size());
        assertTrue(tools.stream().anyMatch(tool -> "web_search".equals(tool.getName())));
        assertTrue(tools.stream().anyMatch(tool -> "calculator".equals(tool.getName())));
    }

    @Test
    void process_withToolCall_shouldExecuteToolAndReviseResponse() {
        // Arrange
        String prompt = "Who is the current CEO of Twitter?";
        String initialResponse = "I need to check this information. { \"tool\": \"web_search\", \"query\": \"current CEO of Twitter 2024\" }";
        String revisedResponse = "According to the latest information, Linda Yaccarino is the current CEO of Twitter (now called X), appointed in June 2023.";

        // Mock LLM service responses
        when(llmService.generateWithToolCalling(anyString(), anyMap(), anyList()))
                .thenReturn(LlmResponse.builder()
                        .text(initialResponse)
                        .processingTime(Duration.ofMillis(100))
                        .build());

        when(llmService.generate(anyString(), anyMap()))
                .thenReturn(LlmResponse.builder()
                        .text(revisedResponse)
                        .processingTime(Duration.ofMillis(100))
                        .build());

        // Mock web search tool response
        Map<String, String> searchResult = new HashMap<>();
        searchResult.put("title", "Linda Yaccarino is the CEO of Twitter (X)");
        searchResult.put("snippet",
                "Linda Yaccarino is the current CEO of Twitter (now called X), appointed in June 2023.");

        when(webSearchTool.executeToolCall(any(ToolCall.class)))
                .thenReturn(ToolResponse.builder()
                        .toolName("web_search")
                        .result(List.of(searchResult))
                        .success(true)
                        .timestamp(Instant.now())
                        .build());

        // Act
        AgenticFlowResult result = flowService.process(prompt, configuration, context);

        // Assert
        assertEquals(prompt, result.getOriginalPrompt());
        assertEquals(revisedResponse, result.getFinalResponse());
        assertEquals(initialResponse, result.getFullResponse());
        assertTrue(result.isResponseChanged());
        assertEquals(3, result.getProcessingSteps().size());
        assertEquals("initial_response", result.getProcessingSteps().get(0).getStepType());
        assertEquals("tool_call", result.getProcessingSteps().get(1).getStepType());
        assertEquals("revised_response", result.getProcessingSteps().get(2).getStepType());

        // Verify tool was called
        ArgumentCaptor<ToolCall> toolCallCaptor = ArgumentCaptor.forClass(ToolCall.class);
        verify(webSearchTool).executeToolCall(toolCallCaptor.capture());

        ToolCall toolCall = toolCallCaptor.getValue();
        assertEquals("web_search", toolCall.getTool());
        assertEquals("current CEO of Twitter 2024", toolCall.getParameters().get("query"));
    }

    @Test
    void validateConfiguration_withValidConfiguration_shouldReturnTrue() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("tools", List.of("web_search", "calculator"));
        params.put("tool_instructions", "Use tools when needed");
        AgenticFlowConfiguration config = new AgenticFlowConfiguration(params);

        // Act & Assert
        assertTrue(flowService.validateConfiguration(config));
    }

    @Test
    void validateConfiguration_withInvalidToolsParameter_shouldReturnFalse() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("tools", "web_search"); // Should be a list, not a string
        AgenticFlowConfiguration config = new AgenticFlowConfiguration(params);

        // Act & Assert
        assertFalse(flowService.validateConfiguration(config));
    }
}