package com.zamaz.mcp.common.domain.agentic.processor;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowProcessor;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.ProcessingStep;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.common.domain.llm.CritiqueIteration;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Self-Critique Loop agentic flow processor.
 * Uses a Generate-Critique-Revise pattern to improve responses through
 * self-critique.
 */
@Service
public class SelfCritiqueLoopFlowService implements AgenticFlowProcessor {
    private final LlmServicePort llmService;

    private static final String STEP_TYPE = "self_critique_loop";
    private static final String ITERATIONS_PARAM = "iterations";
    private static final int DEFAULT_ITERATIONS = 1;
    private static final int MAX_ITERATIONS = 3;

    /**
     * Creates a new SelfCritiqueLoopFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public SelfCritiqueLoopFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.SELF_CRITIQUE_LOOP;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        // Get the number of iterations from configuration (default: 1, max: 3)
        int iterations = getIterationCount(configuration);

        // Record start time for processing duration
        Instant startTime = Instant.now();

        // Generate response with self-critique loop
        LlmResponse response = llmService.generateWithSelfCritique(prompt, configuration.getParameters(), iterations);

        // Create processing steps for visualization
        List<ProcessingStep> processingSteps = createProcessingSteps(prompt, response);

        // Track changes between iterations
        Map<String, Object> changeMetrics = calculateChangeMetrics(response);

        // Build and return the result
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt) // No enhancement for the initial prompt
                .fullResponse(response.getText())
                .finalResponse(response.getText())
                .processingSteps(processingSteps)
                .processingTime(response.getProcessingTime())
                .responseChanged(!response.getIterations().isEmpty())
                .addMetric("iterations_count", iterations)
                .addMetric("actual_iterations", response.getIterations().size())
                .addMetric("visualization_type", STEP_TYPE)
                .addMetric("changes_detected", changeMetrics.get("changes_detected"))
                .addMetric("change_percentage", changeMetrics.get("change_percentage"))
                .build();
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate that iterations is an integer if present
        if (configuration.getParameter(ITERATIONS_PARAM) != null) {
            if (!(configuration.getParameter(ITERATIONS_PARAM) instanceof Integer)) {
                return false;
            }

            // Validate that iterations is between 1 and 3
            int iterations = (Integer) configuration.getParameter(ITERATIONS_PARAM);
            if (iterations < 1 || iterations > MAX_ITERATIONS) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the iteration count from the configuration.
     * 
     * @param configuration The flow configuration
     * @return The iteration count (1-3)
     */
    private int getIterationCount(AgenticFlowConfiguration configuration) {
        Object iterationsObj = configuration.getParameter(ITERATIONS_PARAM, DEFAULT_ITERATIONS);

        if (iterationsObj instanceof Integer) {
            int iterations = (Integer) iterationsObj;
            return Math.min(Math.max(iterations, 1), MAX_ITERATIONS);
        }

        return DEFAULT_ITERATIONS;
    }

    /**
     * Creates processing steps for visualization from the LLM response.
     * 
     * @param prompt   The original prompt
     * @param response The LLM response
     * @return A list of processing steps
     */
    private List<ProcessingStep> createProcessingSteps(String prompt, LlmResponse response) {
        List<ProcessingStep> steps = new ArrayList<>();

        // Initial generation step
        steps.add(new ProcessingStep(
                STEP_TYPE + "_initial",
                prompt,
                response.getIterations().isEmpty() ? response.getText()
                        : response.getIterations().get(0).getRevisedResponse(),
                createInitialStepMetadata()));

        // Critique and revision steps
        for (int i = 0; i < response.getIterations().size(); i++) {
            CritiqueIteration iteration = response.getIterations().get(i);

            // Critique step
            String previousResponse = i == 0 ? response.getIterations().get(0).getRevisedResponse()
                    : response.getIterations().get(i - 1).getRevisedResponse();

            steps.add(new ProcessingStep(
                    STEP_TYPE + "_critique_" + (i + 1),
                    previousResponse,
                    iteration.getCritique(),
                    createCritiqueStepMetadata(i + 1)));

            // Revision step
            steps.add(new ProcessingStep(
                    STEP_TYPE + "_revision_" + (i + 1),
                    iteration.getCritique(),
                    iteration.getRevisedResponse(),
                    createRevisionStepMetadata(i + 1)));
        }

        return steps;
    }

    /**
     * Creates metadata for the initial generation step.
     * 
     * @return A map of metadata
     */
    private Map<String, Object> createInitialStepMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("visualization_type", STEP_TYPE);
        metadata.put("step_name", "Initial Response");
        metadata.put("step_description", "Initial response generated by the model");
        metadata.put("step_order", 1);
        return metadata;
    }

    /**
     * Creates metadata for a critique step.
     * 
     * @param iterationNumber The iteration number
     * @return A map of metadata
     */
    private Map<String, Object> createCritiqueStepMetadata(int iterationNumber) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("visualization_type", STEP_TYPE);
        metadata.put("step_name", "Critique " + iterationNumber);
        metadata.put("step_description", "Self-critique of the previous response");
        metadata.put("step_order", iterationNumber * 2);
        metadata.put("iteration", iterationNumber);
        return metadata;
    }

    /**
     * Creates metadata for a revision step.
     * 
     * @param iterationNumber The iteration number
     * @return A map of metadata
     */
    private Map<String, Object> createRevisionStepMetadata(int iterationNumber) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("visualization_type", STEP_TYPE);
        metadata.put("step_name", "Revision " + iterationNumber);
        metadata.put("step_description", "Revised response based on self-critique");
        metadata.put("step_order", iterationNumber * 2 + 1);
        metadata.put("iteration", iterationNumber);
        return metadata;
    }

    /**
     * Calculates metrics about changes between iterations.
     * 
     * @param response The LLM response
     * @return A map of change metrics
     */
    private Map<String, Object> calculateChangeMetrics(LlmResponse response) {
        Map<String, Object> metrics = new HashMap<>();

        if (response.getIterations().isEmpty()) {
            metrics.put("changes_detected", false);
            metrics.put("change_percentage", 0.0);
            return metrics;
        }

        // Get the initial and final responses
        String initialResponse = response.getIterations().get(0).getRevisedResponse();
        String finalResponse = response.getText();

        // Calculate a simple change metric based on length difference
        double lengthDifference = Math.abs(finalResponse.length() - initialResponse.length());
        double changePercentage = (lengthDifference / Math.max(initialResponse.length(), 1)) * 100.0;

        metrics.put("changes_detected", changePercentage > 5.0);
        metrics.put("change_percentage", changePercentage);

        return metrics;
    }
}