package com.zamaz.mcp.common.domain.agentic.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowProcessor;
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
import com.zamaz.mcp.common.exception.ToolCallException;
import com.zamaz.mcp.common.exception.UnsupportedToolException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the Tool-Calling Verification agentic flow processor.
 * Enables the model to use external tools to verify facts and retrieve
 * up-to-date information.
 */
@Service
public class ToolCallingVerificationFlowService implements AgenticFlowProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ToolCallingVerificationFlowService.class);
    private static final String TOOL_CALL_PATTERN = "\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"query\"\\s*:\\s*\"([^\"]+)\"\\s*\\}";
    private static final Pattern JSON_PATTERN = Pattern.compile(TOOL_CALL_PATTERN);

    private final LlmServicePort llmService;
    private final Map<String, ExternalToolPort> toolAdapters;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new ToolCallingVerificationFlowService with the specified LLM
     * service and tool adapters.
     *
     * @param llmService   The LLM service to use
     * @param toolAdapters The tool adapters to use
     * @param objectMapper The object mapper to use for JSON processing
     */
    public ToolCallingVerificationFlowService(LlmServicePort llmService, List<ExternalToolPort> toolAdapters,
            ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.toolAdapters = new HashMap<>();
        for (ExternalToolPort adapter : toolAdapters) {
            this.toolAdapters.put(adapter.getToolName(), adapter);
        }
        this.objectMapper = objectMapper;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.TOOL_CALLING_VERIFICATION;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        // Record start time for processing duration
        Instant startTime = Instant.now();

        // Get available tools from configuration or use defaults
        List<ToolDefinition> availableTools = getAvailableTools(configuration);

        // Build enhanced prompt with tool instructions
        String enhancedPrompt = buildEnhancedPrompt(prompt, availableTools);

        // Generate initial response with potential tool calls
        LlmResponse initialResponse = llmService.generateWithToolCalling(enhancedPrompt, configuration.getParameters(),
                availableTools);

        // Extract and execute tool calls
        List<ProcessingStep> processingSteps = new ArrayList<>();
        processingSteps.add(new ProcessingStep(
                "initial_response",
                enhancedPrompt,
                initialResponse.getText(),
                createVisualizationMetadata("initial_response", null, initialResponse.getText())));

        // Extract tool calls from the response
        List<ToolCall> toolCalls = extractToolCalls(initialResponse.getText());

        // If no tool calls were found, return the initial response
        if (toolCalls.isEmpty()) {
            logger.info("No tool calls found in response");
            return buildResult(prompt, enhancedPrompt, initialResponse.getText(), initialResponse.getText(),
                    processingSteps, Duration.between(startTime, Instant.now()), false);
        }

        // Execute tool calls and collect results
        List<ToolResponse> toolResponses = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            try {
                ToolResponse toolResponse = executeToolCall(toolCall);
                toolResponses.add(toolResponse);

                // Add tool call processing step
                processingSteps.add(new ProcessingStep(
                        "tool_call",
                        objectMapper.writeValueAsString(toolCall),
                        objectMapper.writeValueAsString(toolResponse),
                        createVisualizationMetadata("tool_call", toolCall, toolResponse)));

            } catch (Exception e) {
                logger.error("Error executing tool call: {}", toolCall, e);
                ToolResponse errorResponse = ToolResponse.builder()
                        .toolName(toolCall.getTool())
                        .success(false)
                        .errorMessage("Error executing tool call: " + e.getMessage())
                        .build();
                toolResponses.add(errorResponse);

                try {
                    // Add error processing step
                    processingSteps.add(new ProcessingStep(
                            "tool_call_error",
                            objectMapper.writeValueAsString(toolCall),
                            e.getMessage(),
                            createVisualizationMetadata("tool_call_error", toolCall, errorResponse)));
                } catch (JsonProcessingException jsonException) {
                    logger.error("Error serializing tool call", jsonException);
                }
            }
        }

        // Generate revised response with tool results
        String toolResultsPrompt = buildToolResultsPrompt(prompt, initialResponse.getText(), toolResponses);
        LlmResponse revisedResponse = llmService.generate(toolResultsPrompt, configuration.getParameters());

        // Add final response processing step
        processingSteps.add(new ProcessingStep(
                "revised_response",
                toolResultsPrompt,
                revisedResponse.getText(),
                createVisualizationMetadata("revised_response", null, revisedResponse.getText())));

        // Calculate processing time
        Duration processingTime = Duration.between(startTime, Instant.now());

        // Build and return the result
        return buildResult(prompt, enhancedPrompt, initialResponse.getText(), revisedResponse.getText(),
                processingSteps, processingTime, true);
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate that the tools parameter is a list if present
        if (configuration.getParameter("tools") != null && !(configuration.getParameter("tools") instanceof List)) {
            return false;
        }

        // Validate that the tool_instructions parameter is a string if present
        if (configuration.getParameter("tool_instructions") != null &&
                !(configuration.getParameter("tool_instructions") instanceof String)) {
            return false;
        }

        return true;
    }

    /**
     * Gets the available tools from the configuration or uses defaults.
     *
     * @param configuration The flow configuration
     * @return The list of available tools
     */
    @SuppressWarnings("unchecked")
    private List<ToolDefinition> getAvailableTools(AgenticFlowConfiguration configuration) {
        List<ToolDefinition> availableTools = new ArrayList<>();

        // Get tools from configuration if present
        if (configuration.getParameter("tools") instanceof List) {
            List<String> configuredTools = (List<String>) configuration.getParameter("tools");
            for (String toolName : configuredTools) {
                if (toolAdapters.containsKey(toolName)) {
                    availableTools.add(createToolDefinition(toolName));
                } else {
                    logger.warn("Tool not available: {}", toolName);
                }
            }
        } else {
            // Use all available tools
            for (String toolName : toolAdapters.keySet()) {
                availableTools.add(createToolDefinition(toolName));
            }
        }

        return availableTools;
    }

    /**
     * Creates a tool definition for the specified tool name.
     *
     * @param toolName The tool name
     * @return The tool definition
     */
    private ToolDefinition createToolDefinition(String toolName) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("query"));

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> queryProperty = new HashMap<>();
        queryProperty.put("type", "string");
        queryProperty.put("description", "The query to execute");
        properties.put("query", queryProperty);
        schema.put("properties", properties);

        String description = switch (toolName) {
            case "web_search" -> "Search the web for information";
            case "calculator" -> "Perform mathematical calculations";
            case "database" -> "Query a database for information";
            case "weather" -> "Get current weather information";
            default -> "Execute a " + toolName + " operation";
        };

        return new ToolDefinition(toolName, description, schema);
    }

    /**
     * Builds an enhanced prompt with tool instructions.
     *
     * @param prompt The original prompt
     * @param tools  The available tools
     * @return The enhanced prompt
     */
    private String buildEnhancedPrompt(String prompt, List<ToolDefinition> tools) {
        StringBuilder enhancedPrompt = new StringBuilder();

        // Add tool instructions
        enhancedPrompt.append("You have access to the following tools:\n\n");

        for (ToolDefinition tool : tools) {
            enhancedPrompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }

        enhancedPrompt.append("\nWhen you need to use a tool, output a JSON object in the following format:\n");
        enhancedPrompt.append("{ \"tool\": \"tool_name\", \"query\": \"your query\" }\n\n");
        enhancedPrompt.append("After receiving the tool result, incorporate it into your response.\n\n");
        enhancedPrompt.append("Original prompt: ").append(prompt);

        return enhancedPrompt.toString();
    }

    /**
     * Extracts tool calls from the response text.
     *
     * @param responseText The response text
     * @return The list of tool calls
     */
    private List<ToolCall> extractToolCalls(String responseText) {
        List<ToolCall> toolCalls = new ArrayList<>();

        // Try to extract JSON objects using regex
        Matcher matcher = JSON_PATTERN.matcher(responseText);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String query = matcher.group(2);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("query", query);

            toolCalls.add(new ToolCall(toolName, parameters));
        }

        // If no matches were found with regex, try to parse as JSON
        if (toolCalls.isEmpty()) {
            try {
                // Try to find JSON objects in the text
                int startIndex = responseText.indexOf('{');
                int endIndex = responseText.lastIndexOf('}');

                if (startIndex >= 0 && endIndex > startIndex) {
                    String jsonText = responseText.substring(startIndex, endIndex + 1);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonObject = objectMapper.readValue(jsonText, Map.class);

                    if (jsonObject.containsKey("tool") && jsonObject.containsKey("query")) {
                        String toolName = jsonObject.get("tool").toString();
                        String query = jsonObject.get("query").toString();

                        Map<String, Object> parameters = new HashMap<>();
                        parameters.put("query", query);

                        toolCalls.add(new ToolCall(toolName, parameters));
                    }
                }
            } catch (Exception e) {
                logger.debug("Error parsing JSON from response", e);
            }
        }

        return toolCalls;
    }

    /**
     * Executes a tool call using the appropriate tool adapter.
     *
     * @param toolCall The tool call to execute
     * @return The tool response
     * @throws UnsupportedToolException if the tool is not supported
     * @throws ToolCallException        if the tool call fails
     */
    private ToolResponse executeToolCall(ToolCall toolCall) {
        ExternalToolPort toolAdapter = toolAdapters.get(toolCall.getTool());

        if (toolAdapter == null) {
            throw new UnsupportedToolException(toolCall.getTool());
        }

        return toolAdapter.executeToolCall(toolCall);
    }

    /**
     * Builds a prompt for generating a revised response with tool results.
     *
     * @param originalPrompt  The original prompt
     * @param initialResponse The initial response
     * @param toolResponses   The tool responses
     * @return The tool results prompt
     */
    private String buildToolResultsPrompt(String originalPrompt, String initialResponse,
            List<ToolResponse> toolResponses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Original prompt: ").append(originalPrompt).append("\n\n");
        prompt.append("Your initial response: ").append(initialResponse).append("\n\n");
        prompt.append("Tool results:\n\n");

        for (ToolResponse toolResponse : toolResponses) {
            prompt.append("Tool: ").append(toolResponse.getToolName()).append("\n");
            prompt.append("Success: ").append(toolResponse.isSuccess()).append("\n");

            if (toolResponse.isSuccess()) {
                prompt.append("Result: ").append(toolResponse.getResult()).append("\n");
            } else {
                prompt.append("Error: ").append(toolResponse.getErrorMessage()).append("\n");
            }

            prompt.append("\n");
        }

        prompt.append(
                "Please provide a revised response incorporating the tool results. Make sure to cite the tool results when appropriate.");

        return prompt.toString();
    }

    /**
     * Creates metadata for visualization of the tool calling process.
     *
     * @param stepType The type of processing step
     * @param toolCall The tool call, or null if not applicable
     * @param result   The result of the step
     * @return A map of visualization metadata
     */
    private Map<String, Object> createVisualizationMetadata(String stepType, Object toolCall, Object result) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("visualization_type", "tool_calling");
        metadata.put("step_type", stepType);

        if (toolCall != null) {
            metadata.put("tool_call", toolCall);
        }

        metadata.put("result", result);
        metadata.put("show_tool_calls", true);

        return metadata;
    }

    /**
     * Builds the final result object.
     *
     * @param originalPrompt  The original prompt
     * @param enhancedPrompt  The enhanced prompt
     * @param initialResponse The initial response
     * @param finalResponse   The final response
     * @param processingSteps The processing steps
     * @param processingTime  The processing time
     * @param responseChanged Whether the response was changed
     * @return The agentic flow result
     */
    private AgenticFlowResult buildResult(String originalPrompt, String enhancedPrompt, String initialResponse,
            String finalResponse, List<ProcessingStep> processingSteps, Duration processingTime,
            boolean responseChanged) {

        return AgenticFlowResult.builder()
                .originalPrompt(originalPrompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(initialResponse)
                .finalResponse(finalResponse)
                .processingSteps(processingSteps)
                .processingTime(processingTime)
                .responseChanged(responseChanged)
                .addMetric("tool_calls_count", processingSteps.stream()
                        .filter(step -> "tool_call".equals(step.getStepType()))
                        .count())
                .addMetric("tool_call_errors_count", processingSteps.stream()
                        .filter(step -> "tool_call_error".equals(step.getStepType()))
                        .count())
                .addMetric("visualization_type", "tool_calling")
                .build();
    }
}