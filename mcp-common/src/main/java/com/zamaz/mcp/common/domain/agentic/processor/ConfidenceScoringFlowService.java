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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the Confidence Scoring agentic flow processor.
 * Provides confidence scores with responses and improves low-confidence answers.
 */
@Service
public class ConfidenceScoringFlowService implements AgenticFlowProcessor {
    
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile(
        "(?i)confidence\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?|\\d+%)");
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d+)%");
    
    private final LlmServicePort llmService;

    /**
     * Creates a new ConfidenceScoringFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public ConfidenceScoringFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.CONFIDENCE_SCORING;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get confidence threshold from configuration
            float confidenceThreshold = ((Number) configuration.getParameter("confidence_threshold", 0.7f)).floatValue();
            int maxImprovementAttempts = (Integer) configuration.getParameter("max_improvement_attempts", 2);
            
            // Step 1: Generate initial response with confidence score
            String enhancedPrompt = buildEnhancedPrompt(prompt, configuration);
            LlmResponse initialResponse = llmService.generate(enhancedPrompt, configuration.getParameters());
            
            // Extract confidence score and response
            float confidenceScore = extractConfidenceScore(initialResponse.getText());
            String cleanResponse = extractCleanResponse(initialResponse.getText());
            
            steps.add(new ProcessingStep(
                "initial_response_with_confidence",
                enhancedPrompt,
                initialResponse.getText(),
                createConfidenceMetadata(confidenceScore, "initial")
            ));

            // Check if improvement is needed
            if (confidenceScore >= confidenceThreshold) {
                // High confidence - return as is
                return buildHighConfidenceResult(
                    prompt, enhancedPrompt, initialResponse.getText(), 
                    cleanResponse, confidenceScore, steps, initialResponse.getProcessingTime()
                );
            }

            // Step 2: Low confidence - attempt improvements
            String currentResponse = cleanResponse;
            float currentConfidence = confidenceScore;
            
            for (int attempt = 1; attempt <= maxImprovementAttempts; attempt++) {
                String improvementPrompt = buildImprovementPrompt(
                    prompt, currentResponse, currentConfidence, configuration
                );
                
                LlmResponse improvedResponse = llmService.generate(
                    improvementPrompt, configuration.getParameters()
                );
                
                float improvedConfidence = extractConfidenceScore(improvedResponse.getText());
                String improvedCleanResponse = extractCleanResponse(improvedResponse.getText());
                
                steps.add(new ProcessingStep(
                    String.format("improvement_attempt_%d", attempt),
                    improvementPrompt,
                    improvedResponse.getText(),
                    createImprovementMetadata(currentConfidence, improvedConfidence, attempt)
                ));
                
                // Update current response and confidence
                currentResponse = improvedCleanResponse;
                currentConfidence = improvedConfidence;
                
                // Check if we've reached the threshold
                if (improvedConfidence >= confidenceThreshold) {
                    break;
                }
            }
            
            // Build final result
            return buildImprovedResult(
                prompt, enhancedPrompt, initialResponse.getText(),
                currentResponse, confidenceScore, currentConfidence,
                steps, initialResponse.getProcessingTime()
            );

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate confidence_threshold
        Object threshold = configuration.getParameter("confidence_threshold");
        if (threshold != null) {
            if (!(threshold instanceof Number)) {
                return false;
            }
            float value = ((Number) threshold).floatValue();
            if (value < 0.0f || value > 1.0f) {
                return false;
            }
        }

        // Validate max_improvement_attempts
        Object maxAttempts = configuration.getParameter("max_improvement_attempts");
        if (maxAttempts != null) {
            if (!(maxAttempts instanceof Integer)) {
                return false;
            }
            int value = (Integer) maxAttempts;
            if (value < 1 || value > 5) {
                return false;
            }
        }

        // Validate confidence_format
        Object format = configuration.getParameter("confidence_format");
        if (format != null && !(format instanceof String)) {
            return false;
        }

        return true;
    }

    /**
     * Builds an enhanced prompt that requests confidence scoring.
     */
    private String buildEnhancedPrompt(String prompt, AgenticFlowConfiguration configuration) {
        String confidenceFormat = (String) configuration.getParameter("confidence_format", "percentage");
        
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append(prompt).append("\n\n");
        enhancedPrompt.append("After providing your answer, please indicate your confidence level ");
        
        if ("percentage".equals(confidenceFormat)) {
            enhancedPrompt.append("as a percentage (0-100%)");
        } else {
            enhancedPrompt.append("on a scale of 0.0 to 1.0");
        }
        
        enhancedPrompt.append(". Format: 'Confidence: [score]'");
        
        return enhancedPrompt.toString();
    }

    /**
     * Builds a prompt for improving a low-confidence response.
     */
    private String buildImprovementPrompt(String originalPrompt, String currentResponse, 
                                        float currentConfidence, AgenticFlowConfiguration configuration) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("The following response was given with low confidence (");
        prompt.append(String.format("%.1f%%", currentConfidence * 100));
        prompt.append("):\n\n");
        prompt.append("Original question: ").append(originalPrompt).append("\n\n");
        prompt.append("Current response: ").append(currentResponse).append("\n\n");
        prompt.append("Please provide an improved response with higher confidence. ");
        prompt.append("Consider:\n");
        prompt.append("- Adding more specific details\n");
        prompt.append("- Providing examples or evidence\n");
        prompt.append("- Clarifying any ambiguous points\n");
        prompt.append("- Correcting any potential errors\n\n");
        prompt.append("Include your confidence level at the end.");
        
        return prompt.toString();
    }

    /**
     * Extracts confidence score from the response text.
     */
    private float extractConfidenceScore(String response) {
        Matcher matcher = CONFIDENCE_PATTERN.matcher(response);
        
        if (matcher.find()) {
            String scoreStr = matcher.group(1);
            
            // Check if it's a percentage
            Matcher percentMatcher = PERCENTAGE_PATTERN.matcher(scoreStr);
            if (percentMatcher.find()) {
                return Float.parseFloat(percentMatcher.group(1)) / 100.0f;
            }
            
            // Otherwise, parse as decimal
            return Float.parseFloat(scoreStr);
        }
        
        // Default to medium confidence if not found
        return 0.5f;
    }

    /**
     * Extracts the clean response without confidence notation.
     */
    private String extractCleanResponse(String fullResponse) {
        // Remove confidence statements
        String cleaned = fullResponse.replaceAll(
            "(?i)\\s*confidence\\s*[:=]?\\s*\\d+(?:\\.\\d+)?%?\\s*", " "
        );
        
        // Also remove common confidence phrases
        cleaned = cleaned.replaceAll(
            "(?i)\\s*(my confidence (level|score) is|confidence:|confidence score:).*", ""
        );
        
        return cleaned.trim();
    }

    /**
     * Creates metadata for confidence visualization.
     */
    private Map<String, Object> createConfidenceMetadata(float confidence, String stage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("confidence_score", confidence);
        metadata.put("confidence_percentage", String.format("%.1f%%", confidence * 100));
        metadata.put("confidence_level", getConfidenceLevel(confidence));
        metadata.put("stage", stage);
        metadata.put("visualization_type", "confidence_gauge");
        return metadata;
    }

    /**
     * Creates metadata for improvement attempts.
     */
    private Map<String, Object> createImprovementMetadata(float previousConfidence, 
                                                         float newConfidence, int attempt) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previous_confidence", previousConfidence);
        metadata.put("new_confidence", newConfidence);
        metadata.put("confidence_improvement", newConfidence - previousConfidence);
        metadata.put("attempt_number", attempt);
        metadata.put("improvement_percentage", 
            String.format("%.1f%%", (newConfidence - previousConfidence) * 100));
        metadata.put("visualization_type", "confidence_improvement");
        return metadata;
    }

    /**
     * Determines confidence level category.
     */
    private String getConfidenceLevel(float confidence) {
        if (confidence >= 0.9f) return "very_high";
        if (confidence >= 0.7f) return "high";
        if (confidence >= 0.5f) return "medium";
        if (confidence >= 0.3f) return "low";
        return "very_low";
    }

    /**
     * Builds result for high confidence response.
     */
    private AgenticFlowResult buildHighConfidenceResult(String originalPrompt, String enhancedPrompt,
                                                      String fullResponse, String cleanResponse,
                                                      float confidence, List<ProcessingStep> steps,
                                                      long processingTime) {
        return AgenticFlowResult.builder()
                .originalPrompt(originalPrompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(fullResponse)
                .finalResponse(cleanResponse)
                .reasoning(String.format("High confidence response (%.1f%%). No improvement needed.", 
                    confidence * 100))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(false)
                .addMetric("initial_confidence", confidence)
                .addMetric("final_confidence", confidence)
                .addMetric("confidence_level", getConfidenceLevel(confidence))
                .addMetric("improvement_attempts", 0)
                .addMetric("visualization_type", "confidence_scoring")
                .build();
    }

    /**
     * Builds result for improved response.
     */
    private AgenticFlowResult buildImprovedResult(String originalPrompt, String enhancedPrompt,
                                                String initialResponse, String finalResponse,
                                                float initialConfidence, float finalConfidence,
                                                List<ProcessingStep> steps, long processingTime) {
        int attempts = steps.size() - 1; // Subtract initial response
        
        return AgenticFlowResult.builder()
                .originalPrompt(originalPrompt)
                .enhancedPrompt(enhancedPrompt)
                .fullResponse(initialResponse)
                .finalResponse(finalResponse)
                .reasoning(String.format(
                    "Initial confidence was low (%.1f%%). After %d improvement attempt(s), " +
                    "achieved confidence of %.1f%%.", 
                    initialConfidence * 100, attempts, finalConfidence * 100))
                .addAllProcessingSteps(steps)
                .processingTime(processingTime)
                .responseChanged(true)
                .addMetric("initial_confidence", initialConfidence)
                .addMetric("final_confidence", finalConfidence)
                .addMetric("confidence_improvement", finalConfidence - initialConfidence)
                .addMetric("confidence_level", getConfidenceLevel(finalConfidence))
                .addMetric("improvement_attempts", attempts)
                .addMetric("visualization_type", "confidence_scoring")
                .build();
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in confidence scoring: " + e.getMessage())
                .finalResponse("Error in confidence scoring: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }
}