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
 * Implementation of the Constitutional Prompting agentic flow processor.
 * Applies constitutional guardrails to AI agent responses.
 */
@Service
public class ConstitutionalPromptingFlowService implements AgenticFlowProcessor {
    
    private static final List<String> DEFAULT_PRINCIPLES = List.of(
        "Be helpful and harmless",
        "Provide accurate and truthful information",
        "Respect user privacy and confidentiality",
        "Avoid generating harmful, biased, or discriminatory content",
        "Be transparent about limitations and uncertainties"
    );
    
    private final LlmServicePort llmService;

    /**
     * Creates a new ConstitutionalPromptingFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public ConstitutionalPromptingFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.CONSTITUTIONAL_PROMPTING;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get constitutional principles from configuration
            @SuppressWarnings("unchecked")
            List<String> principles = (List<String>) configuration.getParameter("principles", DEFAULT_PRINCIPLES);
            boolean enforceRevision = (Boolean) configuration.getParameter("enforce_revision", true);
            
            // Step 1: Generate initial response
            String initialPrompt = buildInitialPrompt(prompt, principles);
            LlmResponse initialResponse = llmService.generate(initialPrompt, configuration.getParameters());
            
            steps.add(new ProcessingStep(
                "initial_response",
                initialPrompt,
                initialResponse.getText(),
                createResponseMetadata("initial", principles.size())
            ));

            // Step 2: Evaluate response against constitutional principles
            List<ViolationReport> violations = evaluateResponse(
                initialResponse.getText(), principles, configuration
            );
            
            steps.add(new ProcessingStep(
                "constitutional_evaluation",
                buildEvaluationPrompt(initialResponse.getText(), principles),
                formatViolationReport(violations),
                createEvaluationMetadata(violations)
            ));

            // If no violations, return the initial response
            if (violations.isEmpty()) {
                return buildCompliantResult(
                    prompt, initialPrompt, initialResponse.getText(),
                    steps, initialResponse.getProcessingTime()
                );
            }

            // Step 3: Request revision if violations found and revision is enforced
            if (!enforceRevision) {
                // Return with violations noted but not revised
                return buildViolationResult(
                    prompt, initialPrompt, initialResponse.getText(),
                    violations, steps, initialResponse.getProcessingTime(), false
                );
            }

            // Generate revised response
            String revisionPrompt = buildRevisionPrompt(prompt, initialResponse.getText(), violations, principles);
            LlmResponse revisedResponse = llmService.generate(revisionPrompt, configuration.getParameters());
            
            steps.add(new ProcessingStep(
                "revised_response",
                revisionPrompt,
                revisedResponse.getText(),
                createRevisionMetadata(violations.size())
            ));

            // Step 4: Re-evaluate revised response
            List<ViolationReport> remainingViolations = evaluateResponse(
                revisedResponse.getText(), principles, configuration
            );
            
            steps.add(new ProcessingStep(
                "final_evaluation",
                buildEvaluationPrompt(revisedResponse.getText(), principles),
                formatViolationReport(remainingViolations),
                createEvaluationMetadata(remainingViolations)
            ));

            // Build final result
            return buildRevisedResult(
                prompt, initialPrompt, initialResponse.getText(),
                revisedResponse.getText(), violations, remainingViolations,
                steps, revisedResponse.getProcessingTime()
            );

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate principles parameter
        Object principles = configuration.getParameter("principles");
        if (principles != null) {
            if (!(principles instanceof List)) {
                return false;
            }
            List<?> principlesList = (List<?>) principles;
            if (principlesList.isEmpty()) {
                return false;
            }
            // Check all elements are strings
            for (Object principle : principlesList) {
                if (!(principle instanceof String)) {
                    return false;
                }
            }
        }

        // Validate enforce_revision parameter
        Object enforceRevision = configuration.getParameter("enforce_revision");
        if (enforceRevision != null && !(enforceRevision instanceof Boolean)) {
            return false;
        }

        return true;
    }

    /**
     * Builds the initial prompt with constitutional guidance.
     */
    private String buildInitialPrompt(String prompt, List<String> principles) {
        StringBuilder enhancedPrompt = new StringBuilder();
        
        enhancedPrompt.append("Please respond to the following request while adhering to these principles:\n\n");
        
        for (int i = 0; i < principles.size(); i++) {
            enhancedPrompt.append(String.format("%d. %s\n", i + 1, principles.get(i)));
        }
        
        enhancedPrompt.append("\nRequest: ").append(prompt);
        
        return enhancedPrompt.toString();
    }

    /**
     * Builds the evaluation prompt for checking violations.
     */
    private String buildEvaluationPrompt(String response, List<String> principles) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Evaluate the following response against these constitutional principles:\n\n");
        
        for (int i = 0; i < principles.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, principles.get(i)));
        }
        
        prompt.append("\nResponse to evaluate:\n").append(response);
        prompt.append("\n\nFor each principle, indicate if there are any violations.");
        
        return prompt.toString();
    }

    /**
     * Builds the revision prompt to address violations.
     */
    private String buildRevisionPrompt(String originalPrompt, String response, 
                                     List<ViolationReport> violations, List<String> principles) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("The following response violates some constitutional principles:\n\n");
        prompt.append("Original request: ").append(originalPrompt).append("\n\n");
        prompt.append("Initial response: ").append(response).append("\n\n");
        prompt.append("Violations detected:\n");
        
        for (ViolationReport violation : violations) {
            prompt.append("- ").append(violation.getPrinciple()).append(": ");
            prompt.append(violation.getDescription()).append("\n");
        }
        
        prompt.append("\nPlease provide a revised response that addresses these violations ");
        prompt.append("while still being helpful and answering the original request.");
        
        return prompt.toString();
    }

    /**
     * Evaluates a response against constitutional principles.
     */
    private List<ViolationReport> evaluateResponse(String response, List<String> principles,
                                                  AgenticFlowConfiguration configuration) {
        List<ViolationReport> violations = new ArrayList<>();
        
        // Build evaluation prompt
        StringBuilder evalPrompt = new StringBuilder();
        evalPrompt.append("Analyze this response for violations of constitutional principles.\n\n");
        evalPrompt.append("Response: ").append(response).append("\n\n");
        evalPrompt.append("Principles to check:\n");
        
        for (int i = 0; i < principles.size(); i++) {
            evalPrompt.append(String.format("%d. %s\n", i + 1, principles.get(i)));
        }
        
        evalPrompt.append("\nFor each principle that is violated, respond with:\n");
        evalPrompt.append("VIOLATION [number]: [description of violation]\n");
        evalPrompt.append("If no violations are found, respond with: NO VIOLATIONS");
        
        // Get evaluation from LLM
        LlmResponse evalResponse = llmService.generate(evalPrompt.toString(), configuration.getParameters());
        
        // Parse violations from response
        String evalText = evalResponse.getText();
        if (!evalText.contains("NO VIOLATIONS")) {
            String[] lines = evalText.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("VIOLATION")) {
                    ViolationReport violation = parseViolation(line, principles);
                    if (violation != null) {
                        violations.add(violation);
                    }
                }
            }
        }
        
        return violations;
    }

    /**
     * Parses a violation report from evaluation text.
     */
    private ViolationReport parseViolation(String line, List<String> principles) {
        try {
            // Extract principle number and description
            String[] parts = line.split(":", 2);
            if (parts.length < 2) return null;
            
            String numberPart = parts[0].trim();
            String description = parts[1].trim();
            
            // Extract principle number
            String numberStr = numberPart.replaceAll("[^0-9]", "");
            int principleIndex = Integer.parseInt(numberStr) - 1;
            
            if (principleIndex >= 0 && principleIndex < principles.size()) {
                return new ViolationReport(
                    principles.get(principleIndex),
                    description,
                    principleIndex + 1
                );
            }
        } catch (Exception e) {
            // Skip malformed violations
        }
        
        return null;
    }

    /**
     * Formats violation report for display.
     */
    private String formatViolationReport(List<ViolationReport> violations) {
        if (violations.isEmpty()) {
            return "No constitutional violations detected.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Constitutional violations detected:\n\n");
        
        for (ViolationReport violation : violations) {
            report.append(String.format("• Principle %d (%s): %s\n",
                violation.getPrincipleNumber(),
                violation.getPrinciple(),
                violation.getDescription()
            ));
        }
        
        return report.toString();
    }

    /**
     * Creates metadata for response steps.
     */
    private Map<String, Object> createResponseMetadata(String stage, int principleCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("stage", stage);
        metadata.put("principle_count", principleCount);
        metadata.put("visualization_type", "constitutional_response");
        return metadata;
    }

    /**
     * Creates metadata for evaluation steps.
     */
    private Map<String, Object> createEvaluationMetadata(List<ViolationReport> violations) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("violation_count", violations.size());
        metadata.put("has_violations", !violations.isEmpty());
        metadata.put("violation_principles", violations.stream()
                .map(ViolationReport::getPrinciple)
                .toList());
        metadata.put("visualization_type", "violation_report");
        return metadata;
    }

    /**
     * Creates metadata for revision steps.
     */
    private Map<String, Object> createRevisionMetadata(int violationCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("addressed_violations", violationCount);
        metadata.put("revision_type", "constitutional");
        metadata.put("visualization_type", "revised_response");
        return metadata;
    }

    /**
     * Builds result for compliant response.
     */
    private AgenticFlowResult buildCompliantResult(String originalPrompt, String enhancedPrompt,
                                                 String response, List<ProcessingStep> steps,
                                                 long processingTime) {
        return AgenticFlowResult.builder()
                .originalPrompt(originalPrompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(response)
                .finalResponse(response)
                .reasoning("Response complies with all constitutional principles.")
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(false)
                .addMetric("violations_found", 0)
                .addMetric("violations_resolved", 0)
                .addMetric("compliant", true)
                .addMetric("visualization_type", "constitutional_prompting")
                .build();
    }

    /**
     * Builds result for response with violations (no revision).
     */
    private AgenticFlowResult buildViolationResult(String originalPrompt, String enhancedPrompt,
                                                 String response, List<ViolationReport> violations,
                                                 List<ProcessingStep> steps, long processingTime,
                                                 boolean revised) {
        return AgenticFlowResult.builder()
                .originalPrompt(originalPrompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(response)
                .finalResponse(response)
                .reasoning(String.format("Found %d constitutional violations. %s",
                    violations.size(), revised ? "Revision attempted." : "Revision not enforced."))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(revised)
                .addMetric("violations_found", violations.size())
                .addMetric("violations_resolved", 0)
                .addMetric("compliant", false)
                .addMetric("revision_enforced", revised)
                .addMetric("visualization_type", "constitutional_prompting")
                .build();
    }

    /**
     * Builds result for revised response.
     */
    private AgenticFlowResult buildRevisedResult(String originalPrompt, String enhancedPrompt,
                                               String initialResponse, String revisedResponse,
                                               List<ViolationReport> initialViolations,
                                               List<ViolationReport> remainingViolations,
                                               List<ProcessingStep> steps, long processingTime) {
        int resolvedCount = initialViolations.size() - remainingViolations.size();
        
        return AgenticFlowResult.builder()
                .originalPrompt(originalPrompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(initialResponse)
                .finalResponse(revisedResponse)
                .reasoning(String.format(
                    "Found %d constitutional violations. Resolved %d through revision. %d remain.",
                    initialViolations.size(), resolvedCount, remainingViolations.size()))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(true)
                .addMetric("violations_found", initialViolations.size())
                .addMetric("violations_resolved", resolvedCount)
                .addMetric("violations_remaining", remainingViolations.size())
                .addMetric("compliant", remainingViolations.isEmpty())
                .addMetric("revision_enforced", true)
                .addMetric("visualization_type", "constitutional_prompting")
                .build();
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in constitutional prompting: " + e.getMessage())
                .finalResponse("Error in constitutional prompting: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }

    /**
     * Inner class representing a constitutional violation report.
     */
    private static class ViolationReport {
        private final String principle;
        private final String description;
        private final int principleNumber;

        public ViolationReport(String principle, String description, int principleNumber) {
            this.principle = principle;
            this.description = description;
            this.principleNumber = principleNumber;
        }

        public String getPrinciple() {
            return principle;
        }

        public String getDescription() {
            return description;
        }

        public int getPrincipleNumber() {
            return principleNumber;
        }
    }
}