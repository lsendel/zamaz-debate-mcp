package com.zamaz.mcp.common.domain.agentic.processor;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowProcessor;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.ProcessingStep;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the Internal Monologue agentic flow processor.
 * Uses chain-of-thought prompting to make the model's reasoning process
 * explicit.
 */
@Service
public class InternalMonologueFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;

    /**
     * Creates a new InternalMonologueFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public InternalMonologueFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.INTERNAL_MONOLOGUE;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        // Record start time for processing duration
        Instant startTime = Instant.now();

        // Add chain-of-thought instructions to the prompt
        String enhancedPrompt = buildEnhancedPrompt(prompt, configuration);

        // Generate response with internal monologue
        LlmResponse response = llmService.generateWithInternalMonologue(enhancedPrompt, configuration.getParameters());

        // Extract reasoning and final answer
        String reasoning = extractReasoning(response.getText());
        String finalAnswer = extractFinalAnswer(response.getText());

        // Create processing step for visualization
        ProcessingStep step = new ProcessingStep(
                "internal_monologue",
                enhancedPrompt,
                response.getText(),
                createVisualizationMetadata(reasoning, finalAnswer));

        // Calculate processing time
        Instant endTime = Instant.now();

        // Build and return the result
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(response.getText())
                .finalResponse(finalAnswer)
                .reasoning(reasoning)
                .addProcessingStep(step)
                .processingTime(response.getProcessingTime())
                .responseChanged(!finalAnswer.equals(response.getText()))
                .addMetric("has_reasoning", reasoning != null && !reasoning.isEmpty())
                .addMetric("reasoning_length", reasoning != null ? reasoning.length() : 0)
                .addMetric("visualization_type", "internal_monologue")
                .build();
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate that the prefix is a string if present
        if (configuration.getParameter("prefix") != null && !(configuration.getParameter("prefix") instanceof String)) {
            return false;
        }

        // Validate that the final_answer_marker is a string if present
        if (configuration.getParameter("final_answer_marker") != null
                && !(configuration.getParameter("final_answer_marker") instanceof String)) {
            return false;
        }

        return true;
    }

    /**
     * Builds an enhanced prompt with chain-of-thought instructions.
     *
     * @param prompt        The original prompt
     * @param configuration The flow configuration
     * @return The enhanced prompt
     */
    private String buildEnhancedPrompt(String prompt, AgenticFlowConfiguration configuration) {
        String prefix = (String) configuration.getParameter("prefix",
                "Take a deep breath, think step by step, and show your work. After you've thought through the problem, provide your final answer.");

        return prefix + "\n\n" + prompt
                + "\n\nWhen you've completed your reasoning, please provide your final answer after the text 'Final Answer:'.";
    }

    /**
     * Extracts the reasoning part from the full response.
     *
     * @param fullResponse The full response from the LLM
     * @return The extracted reasoning
     */
    private String extractReasoning(String fullResponse) {
        String finalAnswerMarker = "Final Answer:";
        int finalAnswerIndex = fullResponse.lastIndexOf(finalAnswerMarker);

        if (finalAnswerIndex >= 0) {
            return fullResponse.substring(0, finalAnswerIndex).trim();
        }

        // If no final answer marker is found, try to use regex to find a conclusion
        // section
        Pattern pattern = Pattern
                .compile("(?i)(conclusion:|in conclusion:|therefore,|thus,|to summarize:|in summary:)");
        Matcher matcher = pattern.matcher(fullResponse);

        if (matcher.find()) {
            return fullResponse.substring(0, matcher.start()).trim();
        }

        // If no clear separation, return the first 80% of the response as reasoning
        int reasoningEndIndex = (int) (fullResponse.length() * 0.8);
        return fullResponse.substring(0, reasoningEndIndex).trim();
    }

    /**
     * Extracts the final answer from the full response.
     *
     * @param fullResponse The full response from the LLM
     * @return The extracted final answer
     */
    private String extractFinalAnswer(String fullResponse) {
        String finalAnswerMarker = "Final Answer:";
        int finalAnswerIndex = fullResponse.lastIndexOf(finalAnswerMarker);

        if (finalAnswerIndex >= 0) {
            return fullResponse.substring(finalAnswerIndex + finalAnswerMarker.length()).trim();
        }

        // If no final answer marker is found, try to use regex to find a conclusion
        // section
        Pattern pattern = Pattern
                .compile("(?i)(conclusion:|in conclusion:|therefore,|thus,|to summarize:|in summary:)");
        Matcher matcher = pattern.matcher(fullResponse);

        if (matcher.find()) {
            return fullResponse.substring(matcher.start()).trim();
        }

        // If no clear separation, return the last 20% of the response as the final
        // answer
        int finalAnswerStartIndex = (int) (fullResponse.length() * 0.8);
        return fullResponse.substring(finalAnswerStartIndex).trim();
    }

    /**
     * Creates metadata for visualization of the internal monologue.
     *
     * @param reasoning   The extracted reasoning
     * @param finalAnswer The extracted final answer
     * @return A map of visualization metadata
     */
    private Map<String, Object> createVisualizationMetadata(String reasoning, String finalAnswer) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("visualization_type", "internal_monologue");
        metadata.put("reasoning_section", reasoning);
        metadata.put("final_answer_section", finalAnswer);
        metadata.put("show_sections", true);
        return metadata;
    }
}