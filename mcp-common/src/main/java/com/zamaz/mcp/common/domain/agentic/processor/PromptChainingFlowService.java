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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Prompt Chaining agentic flow processor.
 * Decomposes complex tasks into a sequence of smaller, interconnected prompts.
 */
@Service
public class PromptChainingFlowService implements AgenticFlowProcessor {
    
    private final LlmServicePort llmService;

    /**
     * Creates a new PromptChainingFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public PromptChainingFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.PROMPT_CHAINING;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get configuration parameters
            @SuppressWarnings("unchecked")
            List<String> chainSteps = (List<String>) configuration.getParameter("chain_steps");
            boolean autoDecompose = (Boolean) configuration.getParameter("auto_decompose", chainSteps == null);
            boolean passFullContext = (Boolean) configuration.getParameter("pass_full_context", false);
            int maxChainLength = (Integer) configuration.getParameter("max_chain_length", 5);
            
            // Step 1: Determine the chain of prompts
            PromptChain chain;
            if (chainSteps != null && !chainSteps.isEmpty()) {
                // Use predefined chain steps
                chain = buildPredefinedChain(prompt, chainSteps);
            } else if (autoDecompose) {
                // Auto-decompose the task
                chain = decomposeTask(prompt, maxChainLength, configuration);
                
                steps.add(new ProcessingStep(
                    "task_decomposition",
                    buildDecompositionPrompt(prompt, maxChainLength),
                    formatChain(chain),
                    createDecompositionMetadata(chain)
                ));
            } else {
                // Single step chain
                chain = new PromptChain(prompt);
                chain.addStep(new ChainStep(1, "Direct Answer", prompt));
            }

            // Step 2: Execute the chain
            List<ChainResult> chainResults = new ArrayList<>();
            String previousOutput = null;
            
            for (ChainStep step : chain.getSteps()) {
                String stepPrompt = buildStepPrompt(step, previousOutput, passFullContext, chainResults);
                
                LlmResponse stepResponse = llmService.generate(stepPrompt, configuration.getParameters());
                
                ChainResult result = new ChainResult(step, stepResponse.getText(), stepPrompt);
                chainResults.add(result);
                
                steps.add(new ProcessingStep(
                    String.format("chain_step_%d", step.getStepNumber()),
                    stepPrompt,
                    stepResponse.getText(),
                    createStepMetadata(step, chainResults.size())
                ));
                
                previousOutput = stepResponse.getText();
            }

            // Step 3: Synthesize final result if needed
            String finalResponse;
            if (chainResults.size() > 1 && (Boolean) configuration.getParameter("synthesize_final", true)) {
                finalResponse = synthesizeFinalResponse(prompt, chainResults, configuration);
                
                steps.add(new ProcessingStep(
                    "final_synthesis",
                    buildSynthesisPrompt(prompt, chainResults),
                    finalResponse,
                    createSynthesisMetadata(chainResults.size())
                ));
            } else {
                // Use the last step's output as final response
                finalResponse = chainResults.get(chainResults.size() - 1).getOutput();
            }

            // Calculate total processing time
            long totalProcessingTime = System.currentTimeMillis() - startTime.toEpochMilli();
            
            return AgenticFlowResult.builder()
                    .originalPrompt(prompt)
                    .enhancedPrompt(chain.getSteps().get(0).getPrompt())
                    .fullResponse(formatFullChainOutput(chainResults))
                    .finalResponse(finalResponse)
                    .reasoning(buildChainReasoning(chain, chainResults))
                    .addAllProcessingSteps(steps)
                    .processingTime(totalProcessingTime)
                    .responseChanged(true)
                    .addMetric("chain_length", chain.getSteps().size())
                    .addMetric("auto_decomposed", autoDecompose)
                    .addMetric("pass_full_context", passFullContext)
                    .addMetric("total_tokens", calculateTotalTokens(chainResults))
                    .addMetric("visualization_type", "prompt_chaining")
                    .build();

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate chain_steps
        Object chainSteps = configuration.getParameter("chain_steps");
        if (chainSteps != null) {
            if (!(chainSteps instanceof List)) {
                return false;
            }
            List<?> stepsList = (List<?>) chainSteps;
            for (Object step : stepsList) {
                if (!(step instanceof String)) {
                    return false;
                }
            }
        }

        // Validate auto_decompose
        Object autoDecompose = configuration.getParameter("auto_decompose");
        if (autoDecompose != null && !(autoDecompose instanceof Boolean)) {
            return false;
        }

        // Validate pass_full_context
        Object passFullContext = configuration.getParameter("pass_full_context");
        if (passFullContext != null && !(passFullContext instanceof Boolean)) {
            return false;
        }

        // Validate max_chain_length
        Object maxLength = configuration.getParameter("max_chain_length");
        if (maxLength != null) {
            if (!(maxLength instanceof Integer)) {
                return false;
            }
            int length = (Integer) maxLength;
            if (length < 1 || length > 10) {
                return false;
            }
        }

        // Validate synthesize_final
        Object synthesizeFinal = configuration.getParameter("synthesize_final");
        if (synthesizeFinal != null && !(synthesizeFinal instanceof Boolean)) {
            return false;
        }

        return true;
    }

    /**
     * Builds a predefined chain from configuration.
     */
    private PromptChain buildPredefinedChain(String originalPrompt, List<String> chainSteps) {
        PromptChain chain = new PromptChain(originalPrompt);
        
        for (int i = 0; i < chainSteps.size(); i++) {
            String stepTemplate = chainSteps.get(i);
            String stepPrompt = stepTemplate.replace("{original_prompt}", originalPrompt);
            chain.addStep(new ChainStep(i + 1, "Step " + (i + 1), stepPrompt));
        }
        
        return chain;
    }

    /**
     * Decomposes a task into a chain of prompts.
     */
    private PromptChain decomposeTask(String prompt, int maxChainLength, 
                                    AgenticFlowConfiguration configuration) {
        String decompositionPrompt = buildDecompositionPrompt(prompt, maxChainLength);
        
        LlmResponse response = llmService.generate(decompositionPrompt, configuration.getParameters());
        
        return parseDecomposition(prompt, response.getText(), maxChainLength);
    }

    /**
     * Builds prompt for task decomposition.
     */
    private String buildDecompositionPrompt(String prompt, int maxChainLength) {
        StringBuilder decompositionPrompt = new StringBuilder();
        
        decompositionPrompt.append("Decompose the following complex task into a sequence of simpler sub-tasks.\n");
        decompositionPrompt.append("Each sub-task should build upon the previous ones.\n\n");
        decompositionPrompt.append("Task: ").append(prompt).append("\n\n");
        decompositionPrompt.append("Create up to ").append(maxChainLength).append(" sequential steps.\n");
        decompositionPrompt.append("Format each step as:\n");
        decompositionPrompt.append("STEP [number]: [description]\n");
        decompositionPrompt.append("PROMPT: [specific prompt for this step]\n\n");
        decompositionPrompt.append("Make sure each step is focused and builds on previous results.");
        
        return decompositionPrompt.toString();
    }

    /**
     * Parses decomposition response into a chain.
     */
    private PromptChain parseDecomposition(String originalPrompt, String decomposition, int maxChainLength) {
        PromptChain chain = new PromptChain(originalPrompt);
        
        String[] lines = decomposition.split("\n");
        ChainStep currentStep = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.matches("STEP \\d+:.*")) {
                if (currentStep != null && currentStep.getPrompt() != null) {
                    chain.addStep(currentStep);
                }
                
                // Extract step number and description
                int colonIndex = line.indexOf(":");
                String numberPart = line.substring(4, colonIndex).trim();
                String description = line.substring(colonIndex + 1).trim();
                
                try {
                    int stepNumber = Integer.parseInt(numberPart);
                    currentStep = new ChainStep(stepNumber, description, null);
                } catch (NumberFormatException e) {
                    // Skip malformed steps
                }
            } else if (line.startsWith("PROMPT:") && currentStep != null) {
                String promptText = line.substring(7).trim();
                currentStep.setPrompt(promptText);
            }
        }
        
        // Add the last step
        if (currentStep != null && currentStep.getPrompt() != null) {
            chain.addStep(currentStep);
        }
        
        // If parsing failed, create a simple chain
        if (chain.getSteps().isEmpty()) {
            chain.addStep(new ChainStep(1, "Direct Answer", originalPrompt));
        }
        
        // Limit to max chain length
        while (chain.getSteps().size() > maxChainLength) {
            chain.getSteps().remove(chain.getSteps().size() - 1);
        }
        
        return chain;
    }

    /**
     * Builds prompt for a chain step.
     */
    private String buildStepPrompt(ChainStep step, String previousOutput, 
                                 boolean passFullContext, List<ChainResult> previousResults) {
        StringBuilder stepPrompt = new StringBuilder();
        
        if (passFullContext && !previousResults.isEmpty()) {
            stepPrompt.append("Previous steps in the chain:\n\n");
            for (ChainResult result : previousResults) {
                stepPrompt.append(String.format("Step %d (%s):\n", 
                    result.getStep().getStepNumber(),
                    result.getStep().getDescription()));
                stepPrompt.append("Output: ").append(truncate(result.getOutput(), 500)).append("\n\n");
            }
            stepPrompt.append("Current step:\n");
        } else if (previousOutput != null) {
            stepPrompt.append("Previous output:\n");
            stepPrompt.append(previousOutput).append("\n\n");
            stepPrompt.append("Next step:\n");
        }
        
        stepPrompt.append(step.getPrompt());
        
        return stepPrompt.toString();
    }

    /**
     * Synthesizes final response from chain results.
     */
    private String synthesizeFinalResponse(String originalPrompt, List<ChainResult> chainResults,
                                         AgenticFlowConfiguration configuration) {
        StringBuilder synthesisPrompt = new StringBuilder();
        
        synthesisPrompt.append("Synthesize a comprehensive answer to the original question using ");
        synthesisPrompt.append("the results from the following chain of reasoning:\n\n");
        synthesisPrompt.append("Original question: ").append(originalPrompt).append("\n\n");
        synthesisPrompt.append("Chain results:\n");
        
        for (ChainResult result : chainResults) {
            synthesisPrompt.append(String.format("\nStep %d (%s):\n", 
                result.getStep().getStepNumber(),
                result.getStep().getDescription()));
            synthesisPrompt.append(result.getOutput()).append("\n");
        }
        
        synthesisPrompt.append("\nProvide a complete, coherent answer that integrates all the information:");
        
        LlmResponse response = llmService.generate(synthesisPrompt.toString(), configuration.getParameters());
        return response.getText();
    }

    /**
     * Builds synthesis prompt for visualization.
     */
    private String buildSynthesisPrompt(String originalPrompt, List<ChainResult> results) {
        return String.format("Synthesizing %d chain results for: %s", results.size(), originalPrompt);
    }

    /**
     * Formats the chain for display.
     */
    private String formatChain(PromptChain chain) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("Prompt Chain (" + chain.getSteps().size() + " steps):\n\n");
        
        for (ChainStep step : chain.getSteps()) {
            formatted.append(String.format("Step %d: %s\n", 
                step.getStepNumber(), step.getDescription()));
            formatted.append("Prompt: ").append(step.getPrompt()).append("\n\n");
        }
        
        return formatted.toString();
    }

    /**
     * Formats full chain output.
     */
    private String formatFullChainOutput(List<ChainResult> results) {
        StringBuilder output = new StringBuilder();
        output.append("Prompt Chain Execution Results:\n\n");
        
        for (ChainResult result : results) {
            output.append(String.format("=== Step %d: %s ===\n", 
                result.getStep().getStepNumber(),
                result.getStep().getDescription()));
            output.append("Prompt: ").append(result.getPrompt()).append("\n\n");
            output.append("Output: ").append(result.getOutput()).append("\n\n");
        }
        
        return output.toString();
    }

    /**
     * Builds chain reasoning summary.
     */
    private String buildChainReasoning(PromptChain chain, List<ChainResult> results) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Prompt Chaining Process:\n\n");
        
        if (chain.isAutoDecomposed()) {
            reasoning.append("Task was automatically decomposed into ");
            reasoning.append(chain.getSteps().size()).append(" steps.\n\n");
        }
        
        reasoning.append("Chain execution:\n");
        for (int i = 0; i < results.size(); i++) {
            ChainResult result = results.get(i);
            reasoning.append(String.format("%d. %s\n", 
                i + 1, result.getStep().getDescription()));
        }
        
        if (results.size() > 1) {
            reasoning.append("\nFinal response synthesized from all chain results.");
        }
        
        return reasoning.toString();
    }

    /**
     * Calculates total tokens across chain (simplified).
     */
    private int calculateTotalTokens(List<ChainResult> results) {
        // Rough estimation: 4 characters = 1 token
        return results.stream()
                .mapToInt(r -> (r.getPrompt().length() + r.getOutput().length()) / 4)
                .sum();
    }

    /**
     * Truncates text to specified length.
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Creates metadata for decomposition.
     */
    private Map<String, Object> createDecompositionMetadata(PromptChain chain) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chain_length", chain.getSteps().size());
        metadata.put("auto_decomposed", true);
        metadata.put("visualization_type", "task_decomposition");
        return metadata;
    }

    /**
     * Creates metadata for chain steps.
     */
    private Map<String, Object> createStepMetadata(ChainStep step, int totalSteps) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("step_number", step.getStepNumber());
        metadata.put("step_description", step.getDescription());
        metadata.put("total_steps", totalSteps);
        metadata.put("visualization_type", "chain_step");
        return metadata;
    }

    /**
     * Creates metadata for synthesis.
     */
    private Map<String, Object> createSynthesisMetadata(int chainLength) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("synthesized_steps", chainLength);
        metadata.put("synthesis_method", "comprehensive_integration");
        metadata.put("visualization_type", "chain_synthesis");
        return metadata;
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in prompt chaining: " + e.getMessage())
                .finalResponse("Error in prompt chaining: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }

    /**
     * Inner class representing a prompt chain.
     */
    private static class PromptChain {
        private final String originalPrompt;
        private final List<ChainStep> steps;
        private boolean autoDecomposed;

        public PromptChain(String originalPrompt) {
            this.originalPrompt = originalPrompt;
            this.steps = new ArrayList<>();
            this.autoDecomposed = false;
        }

        public void addStep(ChainStep step) {
            steps.add(step);
        }

        public String getOriginalPrompt() {
            return originalPrompt;
        }

        public List<ChainStep> getSteps() {
            return steps;
        }

        public boolean isAutoDecomposed() {
            return autoDecomposed;
        }

        public void setAutoDecomposed(boolean autoDecomposed) {
            this.autoDecomposed = autoDecomposed;
        }
    }

    /**
     * Inner class representing a chain step.
     */
    private static class ChainStep {
        private final int stepNumber;
        private final String description;
        private String prompt;

        public ChainStep(int stepNumber, String description, String prompt) {
            this.stepNumber = stepNumber;
            this.description = description;
            this.prompt = prompt;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public String getDescription() {
            return description;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }

    /**
     * Inner class representing a chain execution result.
     */
    private static class ChainResult {
        private final ChainStep step;
        private final String output;
        private final String prompt;

        public ChainResult(ChainStep step, String output, String prompt) {
            this.step = step;
            this.output = output;
            this.prompt = prompt;
        }

        public ChainStep getStep() {
            return step;
        }

        public String getOutput() {
            return output;
        }

        public String getPrompt() {
            return prompt;
        }
    }
}