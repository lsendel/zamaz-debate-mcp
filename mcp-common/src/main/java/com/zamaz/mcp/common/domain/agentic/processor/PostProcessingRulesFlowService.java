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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the Post-processing Rules agentic flow processor.
 * Applies deterministic checks to validate AI outputs.
 */
@Service
public class PostProcessingRulesFlowService implements AgenticFlowProcessor {
    
    private final LlmServicePort llmService;
    private final Map<String, ValidationRule> ruleRegistry;

    /**
     * Creates a new PostProcessingRulesFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public PostProcessingRulesFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
        this.ruleRegistry = initializeRuleRegistry();
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.POST_PROCESSING_RULES;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get configuration parameters
            @SuppressWarnings("unchecked")
            List<String> enabledRules = (List<String>) configuration.getParameter("enabled_rules", 
                new ArrayList<>(ruleRegistry.keySet()));
            boolean autoCorrect = (Boolean) configuration.getParameter("auto_correct", true);
            int maxCorrectionAttempts = (Integer) configuration.getParameter("max_correction_attempts", 2);
            
            // Step 1: Generate initial response
            LlmResponse initialResponse = llmService.generate(prompt, configuration.getParameters());
            
            steps.add(new ProcessingStep(
                "initial_response",
                prompt,
                initialResponse.getText(),
                createResponseMetadata("initial")
            ));

            // Step 2: Apply validation rules
            List<RuleViolation> violations = validateResponse(
                initialResponse.getText(), enabledRules, configuration
            );
            
            steps.add(new ProcessingStep(
                "rule_validation",
                "Applying " + enabledRules.size() + " validation rules",
                formatViolationReport(violations),
                createValidationMetadata(violations, enabledRules.size())
            ));

            // If no violations, return the initial response
            if (violations.isEmpty()) {
                return buildCompliantResult(
                    prompt, initialResponse.getText(), steps, 
                    initialResponse.getProcessingTime(), enabledRules.size()
                );
            }

            // If auto-correction is disabled, return with violations noted
            if (!autoCorrect) {
                return buildViolationResult(
                    prompt, initialResponse.getText(), violations, steps,
                    initialResponse.getProcessingTime(), false
                );
            }

            // Step 3: Attempt auto-correction
            String currentResponse = initialResponse.getText();
            List<RuleViolation> currentViolations = violations;
            
            for (int attempt = 1; attempt <= maxCorrectionAttempts; attempt++) {
                String correctedResponse = attemptCorrection(
                    prompt, currentResponse, currentViolations, configuration
                );
                
                steps.add(new ProcessingStep(
                    String.format("correction_attempt_%d", attempt),
                    buildCorrectionPrompt(prompt, currentResponse, currentViolations),
                    correctedResponse,
                    createCorrectionMetadata(attempt, currentViolations.size())
                ));

                // Re-validate corrected response
                List<RuleViolation> newViolations = validateResponse(
                    correctedResponse, enabledRules, configuration
                );
                
                steps.add(new ProcessingStep(
                    String.format("revalidation_%d", attempt),
                    "Re-validating corrected response",
                    formatViolationReport(newViolations),
                    createValidationMetadata(newViolations, enabledRules.size())
                ));

                currentResponse = correctedResponse;
                currentViolations = newViolations;
                
                // If all violations resolved, break
                if (newViolations.isEmpty()) {
                    break;
                }
            }

            // Build final result
            return buildCorrectedResult(
                prompt, initialResponse.getText(), currentResponse,
                violations, currentViolations, steps,
                initialResponse.getProcessingTime()
            );

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate enabled_rules
        Object enabledRules = configuration.getParameter("enabled_rules");
        if (enabledRules != null) {
            if (!(enabledRules instanceof List)) {
                return false;
            }
            List<?> rulesList = (List<?>) enabledRules;
            for (Object rule : rulesList) {
                if (!(rule instanceof String)) {
                    return false;
                }
            }
        }

        // Validate auto_correct
        Object autoCorrect = configuration.getParameter("auto_correct");
        if (autoCorrect != null && !(autoCorrect instanceof Boolean)) {
            return false;
        }

        // Validate max_correction_attempts
        Object maxAttempts = configuration.getParameter("max_correction_attempts");
        if (maxAttempts != null) {
            if (!(maxAttempts instanceof Integer)) {
                return false;
            }
            int attempts = (Integer) maxAttempts;
            if (attempts < 1 || attempts > 5) {
                return false;
            }
        }

        return true;
    }

    /**
     * Initializes the rule registry with common validation rules.
     */
    private Map<String, ValidationRule> initializeRuleRegistry() {
        Map<String, ValidationRule> registry = new HashMap<>();
        
        // Format validation rules
        registry.put("no_html_tags", new ValidationRule(
            "No HTML tags",
            response -> !response.matches(".*<[^>]+>.*"),
            "Response contains HTML tags"
        ));
        
        registry.put("no_markdown_code_blocks", new ValidationRule(
            "No markdown code blocks",
            response -> !response.contains("```"),
            "Response contains markdown code blocks"
        ));
        
        registry.put("proper_sentence_ending", new ValidationRule(
            "Proper sentence ending",
            response -> response.trim().matches(".*[.!?]$"),
            "Response does not end with proper punctuation"
        ));
        
        // Content validation rules
        registry.put("no_urls", new ValidationRule(
            "No URLs",
            response -> !response.matches(".*https?://.*"),
            "Response contains URLs"
        ));
        
        registry.put("no_email_addresses", new ValidationRule(
            "No email addresses",
            response -> !response.matches(".*[\\w.-]+@[\\w.-]+\\.[\\w]+.*"),
            "Response contains email addresses"
        ));
        
        registry.put("no_phone_numbers", new ValidationRule(
            "No phone numbers",
            response -> !response.matches(".*\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b.*"),
            "Response contains phone numbers"
        ));
        
        // Length validation rules
        registry.put("max_length_500", new ValidationRule(
            "Maximum length 500 characters",
            response -> response.length() <= 500,
            "Response exceeds 500 characters"
        ));
        
        registry.put("min_length_50", new ValidationRule(
            "Minimum length 50 characters",
            response -> response.length() >= 50,
            "Response is less than 50 characters"
        ));
        
        // Structure validation rules
        registry.put("no_bullet_points", new ValidationRule(
            "No bullet points",
            response -> !response.matches(".*^\\s*[•\\-*]\\s+.*", Pattern.MULTILINE),
            "Response contains bullet points"
        ));
        
        registry.put("no_numbered_lists", new ValidationRule(
            "No numbered lists",
            response -> !response.matches(".*^\\s*\\d+[.)\\s]+.*", Pattern.MULTILINE),
            "Response contains numbered lists"
        ));
        
        // Language validation rules
        registry.put("english_only", new ValidationRule(
            "English only",
            response -> !containsNonEnglish(response),
            "Response contains non-English characters"
        ));
        
        registry.put("no_profanity", new ValidationRule(
            "No profanity",
            response -> !containsProfanity(response),
            "Response contains inappropriate language"
        ));
        
        // Consistency rules
        registry.put("consistent_tense", new ValidationRule(
            "Consistent verb tense",
            response -> hasConsistentTense(response),
            "Response has inconsistent verb tenses"
        ));
        
        registry.put("no_first_person", new ValidationRule(
            "No first person pronouns",
            response -> !response.matches("(?i).*\\b(I|me|my|mine|myself)\\b.*"),
            "Response contains first person pronouns"
        ));
        
        return registry;
    }

    /**
     * Validates response against enabled rules.
     */
    private List<RuleViolation> validateResponse(String response, List<String> enabledRules,
                                               AgenticFlowConfiguration configuration) {
        List<RuleViolation> violations = new ArrayList<>();
        
        // Check custom rules from configuration
        @SuppressWarnings("unchecked")
        List<Map<String, String>> customRules = (List<Map<String, String>>) 
            configuration.getParameter("custom_rules", new ArrayList<>());
        
        // Apply built-in rules
        for (String ruleName : enabledRules) {
            ValidationRule rule = ruleRegistry.get(ruleName);
            if (rule != null && !rule.validate(response)) {
                violations.add(new RuleViolation(ruleName, rule.getDescription(), rule.getErrorMessage()));
            }
        }
        
        // Apply custom rules
        for (Map<String, String> customRule : customRules) {
            String ruleName = customRule.get("name");
            String pattern = customRule.get("pattern");
            String errorMessage = customRule.get("error_message");
            
            if (ruleName != null && pattern != null) {
                try {
                    if (response.matches(pattern)) {
                        violations.add(new RuleViolation(
                            ruleName, 
                            "Custom rule: " + ruleName,
                            errorMessage != null ? errorMessage : "Custom rule violation"
                        ));
                    }
                } catch (Exception e) {
                    // Skip invalid patterns
                }
            }
        }
        
        return violations;
    }

    /**
     * Attempts to correct violations in the response.
     */
    private String attemptCorrection(String originalPrompt, String response,
                                   List<RuleViolation> violations,
                                   AgenticFlowConfiguration configuration) {
        String correctionPrompt = buildCorrectionPrompt(originalPrompt, response, violations);
        LlmResponse correctedResponse = llmService.generate(correctionPrompt, configuration.getParameters());
        return correctedResponse.getText();
    }

    /**
     * Builds a prompt for correcting violations.
     */
    private String buildCorrectionPrompt(String originalPrompt, String response,
                                       List<RuleViolation> violations) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Please correct the following response to fix these validation issues:\n\n");
        prompt.append("Original request: ").append(originalPrompt).append("\n\n");
        prompt.append("Current response: ").append(response).append("\n\n");
        prompt.append("Validation issues to fix:\n");
        
        for (RuleViolation violation : violations) {
            prompt.append("- ").append(violation.getErrorMessage()).append("\n");
        }
        
        prompt.append("\nProvide a corrected response that maintains the original meaning ");
        prompt.append("while fixing all validation issues.");
        
        return prompt.toString();
    }

    /**
     * Formats violation report for display.
     */
    private String formatViolationReport(List<RuleViolation> violations) {
        if (violations.isEmpty()) {
            return "All validation rules passed.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Validation violations detected:\n\n");
        
        for (RuleViolation violation : violations) {
            report.append(String.format("• Rule: %s\n  %s\n\n",
                violation.getRuleName(), violation.getErrorMessage()));
        }
        
        return report.toString();
    }

    /**
     * Checks if text contains non-English characters.
     */
    private boolean containsNonEnglish(String text) {
        // Simple check for non-ASCII characters (excluding common punctuation)
        return text.matches(".*[^\\x00-\\x7F].*");
    }

    /**
     * Checks if text contains profanity.
     */
    private boolean containsProfanity(String text) {
        // Simplified profanity check - in production, use a comprehensive list
        String[] profanityList = {"damn", "hell"}; // Mild examples
        String lowerText = text.toLowerCase();
        
        for (String word : profanityList) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks if text has consistent verb tense.
     */
    private boolean hasConsistentTense(String text) {
        // Simplified tense consistency check
        // Count past and present tense indicators
        int pastCount = 0;
        int presentCount = 0;
        
        // Past tense patterns
        if (text.matches(".*\\b\\w+ed\\b.*")) pastCount++;
        if (text.matches(".*\\b(was|were|had|did)\\b.*")) pastCount++;
        
        // Present tense patterns
        if (text.matches(".*\\b(is|are|am|has|have|do|does)\\b.*")) presentCount++;
        if (text.matches(".*\\b\\w+ing\\b.*")) presentCount++;
        
        // Allow some mixing but not excessive
        return Math.abs(pastCount - presentCount) <= 2;
    }

    /**
     * Creates metadata for response steps.
     */
    private Map<String, Object> createResponseMetadata(String stage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("stage", stage);
        metadata.put("visualization_type", "response_generation");
        return metadata;
    }

    /**
     * Creates metadata for validation steps.
     */
    private Map<String, Object> createValidationMetadata(List<RuleViolation> violations, int totalRules) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("total_rules", totalRules);
        metadata.put("violations_found", violations.size());
        metadata.put("rules_passed", totalRules - violations.size());
        metadata.put("violation_rules", violations.stream()
                .map(RuleViolation::getRuleName)
                .toList());
        metadata.put("visualization_type", "validation_report");
        return metadata;
    }

    /**
     * Creates metadata for correction steps.
     */
    private Map<String, Object> createCorrectionMetadata(int attempt, int violationCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("attempt_number", attempt);
        metadata.put("violations_to_fix", violationCount);
        metadata.put("visualization_type", "correction_attempt");
        return metadata;
    }

    /**
     * Builds result for compliant response.
     */
    private AgenticFlowResult buildCompliantResult(String prompt, String response,
                                                 List<ProcessingStep> steps, long processingTime,
                                                 int totalRules) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse(response)
                .finalResponse(response)
                .reasoning(String.format("Response passed all %d validation rules.", totalRules))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(false)
                .addMetric("total_rules", totalRules)
                .addMetric("violations_found", 0)
                .addMetric("corrections_made", 0)
                .addMetric("compliant", true)
                .addMetric("visualization_type", "post_processing_rules")
                .build();
    }

    /**
     * Builds result for response with violations (no correction).
     */
    private AgenticFlowResult buildViolationResult(String prompt, String response,
                                                 List<RuleViolation> violations,
                                                 List<ProcessingStep> steps, long processingTime,
                                                 boolean corrected) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse(response)
                .finalResponse(response)
                .reasoning(String.format("Found %d rule violations. %s",
                    violations.size(), corrected ? "Correction attempted." : "Auto-correction disabled."))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(corrected)
                .addMetric("violations_found", violations.size())
                .addMetric("corrections_made", 0)
                .addMetric("compliant", false)
                .addMetric("auto_correct_enabled", corrected)
                .addMetric("visualization_type", "post_processing_rules")
                .build();
    }

    /**
     * Builds result for corrected response.
     */
    private AgenticFlowResult buildCorrectedResult(String prompt, String initialResponse,
                                                 String correctedResponse,
                                                 List<RuleViolation> initialViolations,
                                                 List<RuleViolation> remainingViolations,
                                                 List<ProcessingStep> steps, long processingTime) {
        int resolvedCount = initialViolations.size() - remainingViolations.size();
        
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse(initialResponse)
                .finalResponse(correctedResponse)
                .reasoning(String.format(
                    "Found %d rule violations. Resolved %d through correction. %d remain.",
                    initialViolations.size(), resolvedCount, remainingViolations.size()))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(true)
                .addMetric("violations_found", initialViolations.size())
                .addMetric("violations_resolved", resolvedCount)
                .addMetric("violations_remaining", remainingViolations.size())
                .addMetric("compliant", remainingViolations.isEmpty())
                .addMetric("corrections_made", (steps.size() - 2) / 2) // Correction attempts
                .addMetric("visualization_type", "post_processing_rules")
                .build();
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in post-processing rules: " + e.getMessage())
                .finalResponse("Error in post-processing rules: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }

    /**
     * Inner class representing a validation rule.
     */
    private static class ValidationRule {
        private final String description;
        private final Function<String, Boolean> validator;
        private final String errorMessage;

        public ValidationRule(String description, Function<String, Boolean> validator, String errorMessage) {
            this.description = description;
            this.validator = validator;
            this.errorMessage = errorMessage;
        }

        public boolean validate(String response) {
            return validator.apply(response);
        }

        public String getDescription() {
            return description;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Inner class representing a rule violation.
     */
    private static class RuleViolation {
        private final String ruleName;
        private final String ruleDescription;
        private final String errorMessage;

        public RuleViolation(String ruleName, String ruleDescription, String errorMessage) {
            this.ruleName = ruleName;
            this.ruleDescription = ruleDescription;
            this.errorMessage = errorMessage;
        }

        public String getRuleName() {
            return ruleName;
        }

        public String getRuleDescription() {
            return ruleDescription;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}