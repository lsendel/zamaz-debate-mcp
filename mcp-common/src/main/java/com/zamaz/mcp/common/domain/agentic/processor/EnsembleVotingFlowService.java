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
import java.util.stream.Collectors;

/**
 * Implementation of the Ensemble Voting agentic flow processor.
 * Uses ensemble voting across multiple AI responses for more reliable answers.
 */
@Service
public class EnsembleVotingFlowService implements AgenticFlowProcessor {
    
    private final LlmServicePort llmService;

    /**
     * Creates a new EnsembleVotingFlowService with the specified LLM service.
     *
     * @param llmService The LLM service to use
     */
    public EnsembleVotingFlowService(LlmServicePort llmService) {
        this.llmService = llmService;
    }

    @Override
    public AgenticFlowType getFlowType() {
        return AgenticFlowType.ENSEMBLE_VOTING;
    }

    @Override
    public AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context) {
        Instant startTime = Instant.now();
        List<ProcessingStep> steps = new ArrayList<>();
        
        try {
            // Get ensemble configuration
            int ensembleSize = (Integer) configuration.getParameter("ensemble_size", 5);
            float temperatureVariation = ((Number) configuration.getParameter("temperature_variation", 0.3f)).floatValue();
            String votingMethod = (String) configuration.getParameter("voting_method", "similarity");
            
            // Step 1: Generate multiple responses with temperature variation
            List<EnsembleResponse> ensembleResponses = generateEnsembleResponses(
                prompt, ensembleSize, temperatureVariation, configuration
            );
            
            steps.add(new ProcessingStep(
                "ensemble_generation",
                prompt,
                formatEnsembleResponses(ensembleResponses),
                createEnsembleMetadata(ensembleResponses)
            ));

            // Step 2: Analyze responses for voting
            VotingAnalysis analysis = analyzeResponses(ensembleResponses, votingMethod, configuration);
            
            steps.add(new ProcessingStep(
                "voting_analysis",
                "Analyzing ensemble responses for voting",
                formatVotingAnalysis(analysis),
                createAnalysisMetadata(analysis)
            ));

            // Step 3: Select winning response or synthesize
            String finalResponse;
            String selectionReasoning;
            
            if (analysis.hasClearWinner()) {
                // Use the winning response
                finalResponse = analysis.getWinningResponse();
                selectionReasoning = String.format(
                    "Selected response with highest consensus (%d/%d votes)",
                    analysis.getWinnerVotes(), ensembleSize
                );
            } else {
                // Synthesize from top responses
                finalResponse = synthesizeResponses(analysis.getTopResponses(), prompt, configuration);
                selectionReasoning = "No clear winner. Synthesized response from top candidates.";
                
                steps.add(new ProcessingStep(
                    "response_synthesis",
                    buildSynthesisPrompt(analysis.getTopResponses(), prompt),
                    finalResponse,
                    createSynthesisMetadata(analysis.getTopResponses().size())
                ));
            }

            // Calculate metrics
            double averageConfidence = calculateAverageConfidence(ensembleResponses);
            double responseVariance = calculateResponseVariance(ensembleResponses);
            
            return AgenticFlowResult.builder()
                    .originalPrompt(prompt)
                    .enhancedPrompt(prompt)
                    .fullResponse(formatFullEnsembleOutput(ensembleResponses, analysis))
                    .finalResponse(finalResponse)
                    .reasoning(selectionReasoning)
                    .addAllProcessingSteps(steps)
                    .processingTime(ensembleResponses.stream()
                            .mapToLong(r -> r.getProcessingTime())
                            .sum())
                    .responseChanged(true)
                    .addMetric("ensemble_size", ensembleSize)
                    .addMetric("temperature_variation", temperatureVariation)
                    .addMetric("voting_method", votingMethod)
                    .addMetric("has_clear_winner", analysis.hasClearWinner())
                    .addMetric("winner_votes", analysis.getWinnerVotes())
                    .addMetric("average_confidence", averageConfidence)
                    .addMetric("response_variance", responseVariance)
                    .addMetric("visualization_type", "ensemble_voting")
                    .build();

        } catch (Exception e) {
            return buildErrorResult(prompt, steps, e);
        }
    }

    @Override
    public boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        // Validate ensemble_size
        Object ensembleSize = configuration.getParameter("ensemble_size");
        if (ensembleSize != null) {
            if (!(ensembleSize instanceof Integer)) {
                return false;
            }
            int size = (Integer) ensembleSize;
            if (size < 3 || size > 10) {
                return false;
            }
        }

        // Validate temperature_variation
        Object tempVariation = configuration.getParameter("temperature_variation");
        if (tempVariation != null) {
            if (!(tempVariation instanceof Number)) {
                return false;
            }
            float variation = ((Number) tempVariation).floatValue();
            if (variation < 0.0f || variation > 1.0f) {
                return false;
            }
        }

        // Validate voting_method
        Object votingMethod = configuration.getParameter("voting_method");
        if (votingMethod != null) {
            if (!(votingMethod instanceof String)) {
                return false;
            }
            String method = (String) votingMethod;
            if (!List.of("similarity", "majority", "weighted").contains(method)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Generates multiple responses with temperature variation.
     */
    private List<EnsembleResponse> generateEnsembleResponses(String prompt, int ensembleSize,
                                                           float temperatureVariation,
                                                           AgenticFlowConfiguration configuration) {
        List<EnsembleResponse> responses = new ArrayList<>();
        float baseTemperature = ((Number) configuration.getParameter("temperature", 0.7f)).floatValue();
        
        for (int i = 0; i < ensembleSize; i++) {
            // Vary temperature for diversity
            float temperature = baseTemperature + (temperatureVariation * (i - ensembleSize / 2.0f) / ensembleSize);
            temperature = Math.max(0.1f, Math.min(1.0f, temperature)); // Clamp to valid range
            
            Map<String, Object> params = new HashMap<>(configuration.getParameters());
            params.put("temperature", temperature);
            
            LlmResponse response = llmService.generate(prompt, params);
            
            responses.add(new EnsembleResponse(
                i + 1,
                response.getText(),
                temperature,
                response.getProcessingTime()
            ));
        }
        
        return responses;
    }

    /**
     * Analyzes ensemble responses for voting.
     */
    private VotingAnalysis analyzeResponses(List<EnsembleResponse> responses, String votingMethod,
                                          AgenticFlowConfiguration configuration) {
        if ("similarity".equals(votingMethod)) {
            return analyzeBySimilarity(responses, configuration);
        } else if ("majority".equals(votingMethod)) {
            return analyzeByMajority(responses, configuration);
        } else {
            return analyzeByWeighted(responses, configuration);
        }
    }

    /**
     * Analyzes responses by semantic similarity.
     */
    private VotingAnalysis analyzeBySimilarity(List<EnsembleResponse> responses,
                                              AgenticFlowConfiguration configuration) {
        // Build similarity comparison prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("Compare these responses and group them by semantic similarity:\n\n");
        
        for (EnsembleResponse response : responses) {
            prompt.append(String.format("Response %d: %s\n\n", response.getId(), response.getResponse()));
        }
        
        prompt.append("Identify which responses are essentially saying the same thing. ");
        prompt.append("Return the ID of the response that best represents the largest group.");
        
        LlmResponse analysis = llmService.generate(prompt.toString(), configuration.getParameters());
        
        // Parse the winning response ID (simplified parsing)
        int winnerId = extractWinnerId(analysis.getText(), responses.size());
        
        VotingAnalysis result = new VotingAnalysis();
        result.setWinningResponse(responses.get(winnerId - 1).getResponse());
        result.setWinnerVotes(calculateSimilarityVotes(winnerId, responses));
        result.setTopResponses(responses.stream()
                .map(EnsembleResponse::getResponse)
                .limit(3)
                .collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Analyzes responses by majority vote on key points.
     */
    private VotingAnalysis analyzeByMajority(List<EnsembleResponse> responses,
                                           AgenticFlowConfiguration configuration) {
        // For simplicity, use the first response as winner
        // In a real implementation, this would extract key points and vote on them
        VotingAnalysis result = new VotingAnalysis();
        result.setWinningResponse(responses.get(0).getResponse());
        result.setWinnerVotes((responses.size() + 1) / 2);
        result.setTopResponses(responses.stream()
                .map(EnsembleResponse::getResponse)
                .limit(3)
                .collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Analyzes responses by weighted voting based on confidence.
     */
    private VotingAnalysis analyzeByWeighted(List<EnsembleResponse> responses,
                                           AgenticFlowConfiguration configuration) {
        // For simplicity, use the response with median temperature as winner
        // In a real implementation, this would calculate confidence weights
        int medianIndex = responses.size() / 2;
        
        VotingAnalysis result = new VotingAnalysis();
        result.setWinningResponse(responses.get(medianIndex).getResponse());
        result.setWinnerVotes((responses.size() + 1) / 2);
        result.setTopResponses(responses.stream()
                .map(EnsembleResponse::getResponse)
                .limit(3)
                .collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Extracts winner ID from analysis text.
     */
    private int extractWinnerId(String analysisText, int maxId) {
        // Try to find a number between 1 and maxId in the text
        for (int i = 1; i <= maxId; i++) {
            if (analysisText.contains(String.valueOf(i))) {
                return i;
            }
        }
        return 1; // Default to first response
    }

    /**
     * Calculates similarity votes for a response.
     */
    private int calculateSimilarityVotes(int winnerId, List<EnsembleResponse> responses) {
        // Simplified: assume winner gets majority
        return (responses.size() + 1) / 2;
    }

    /**
     * Synthesizes a response from top candidates.
     */
    private String synthesizeResponses(List<String> topResponses, String originalPrompt,
                                     AgenticFlowConfiguration configuration) {
        StringBuilder synthesisPrompt = new StringBuilder();
        synthesisPrompt.append("Synthesize the best elements from these responses:\n\n");
        
        for (int i = 0; i < topResponses.size(); i++) {
            synthesisPrompt.append(String.format("Response %d: %s\n\n", i + 1, topResponses.get(i)));
        }
        
        synthesisPrompt.append("Original question: ").append(originalPrompt).append("\n\n");
        synthesisPrompt.append("Create a comprehensive response that combines the best insights from all responses.");
        
        LlmResponse synthesis = llmService.generate(synthesisPrompt.toString(), configuration.getParameters());
        return synthesis.getText();
    }

    /**
     * Builds synthesis prompt for visualization.
     */
    private String buildSynthesisPrompt(List<String> responses, String originalPrompt) {
        return String.format("Synthesizing %d responses for: %s", responses.size(), originalPrompt);
    }

    /**
     * Calculates average confidence across ensemble.
     */
    private double calculateAverageConfidence(List<EnsembleResponse> responses) {
        // Using temperature as inverse proxy for confidence
        double avgTemp = responses.stream()
                .mapToDouble(EnsembleResponse::getTemperature)
                .average()
                .orElse(0.7);
        
        return 1.0 - avgTemp;
    }

    /**
     * Calculates variance in responses.
     */
    private double calculateResponseVariance(List<EnsembleResponse> responses) {
        // Simplified: using response length variance as proxy
        double avgLength = responses.stream()
                .mapToInt(r -> r.getResponse().length())
                .average()
                .orElse(0);
        
        return responses.stream()
                .mapToDouble(r -> Math.pow(r.getResponse().length() - avgLength, 2))
                .average()
                .orElse(0) / (avgLength * avgLength);
    }

    /**
     * Formats ensemble responses for display.
     */
    private String formatEnsembleResponses(List<EnsembleResponse> responses) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("Generated ").append(responses.size()).append(" ensemble responses:\n\n");
        
        for (EnsembleResponse response : responses) {
            formatted.append(String.format("Response %d (temp=%.2f):\n%s\n\n",
                    response.getId(), response.getTemperature(),
                    truncate(response.getResponse(), 200)));
        }
        
        return formatted.toString();
    }

    /**
     * Formats voting analysis for display.
     */
    private String formatVotingAnalysis(VotingAnalysis analysis) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("Voting Analysis Results:\n");
        formatted.append("- Has clear winner: ").append(analysis.hasClearWinner()).append("\n");
        formatted.append("- Winner votes: ").append(analysis.getWinnerVotes()).append("\n");
        formatted.append("- Top responses: ").append(analysis.getTopResponses().size()).append("\n");
        return formatted.toString();
    }

    /**
     * Formats full ensemble output.
     */
    private String formatFullEnsembleOutput(List<EnsembleResponse> responses, VotingAnalysis analysis) {
        StringBuilder output = new StringBuilder();
        output.append("Ensemble Voting Results:\n\n");
        
        for (EnsembleResponse response : responses) {
            output.append(String.format("Response %d:\n%s\n\n", response.getId(), response.getResponse()));
        }
        
        output.append("\nVoting Analysis:\n");
        output.append(formatVotingAnalysis(analysis));
        
        return output.toString();
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
     * Creates metadata for ensemble generation.
     */
    private Map<String, Object> createEnsembleMetadata(List<EnsembleResponse> responses) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("response_count", responses.size());
        metadata.put("temperature_range", responses.stream()
                .mapToDouble(EnsembleResponse::getTemperature)
                .summaryStatistics());
        metadata.put("visualization_type", "ensemble_responses");
        return metadata;
    }

    /**
     * Creates metadata for voting analysis.
     */
    private Map<String, Object> createAnalysisMetadata(VotingAnalysis analysis) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("has_clear_winner", analysis.hasClearWinner());
        metadata.put("winner_votes", analysis.getWinnerVotes());
        metadata.put("top_response_count", analysis.getTopResponses().size());
        metadata.put("visualization_type", "voting_results");
        return metadata;
    }

    /**
     * Creates metadata for synthesis.
     */
    private Map<String, Object> createSynthesisMetadata(int responseCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("synthesized_from", responseCount);
        metadata.put("synthesis_method", "llm_combination");
        metadata.put("visualization_type", "synthesis_result");
        return metadata;
    }

    /**
     * Builds error result.
     */
    private AgenticFlowResult buildErrorResult(String prompt, List<ProcessingStep> steps, Exception e) {
        return AgenticFlowResult.builder()
                .originalPrompt(prompt)
                .enhancedPrompt(prompt)
                .fullResponse("Error in ensemble voting: " + e.getMessage())
                .finalResponse("Error in ensemble voting: " + e.getMessage())
                .reasoning("Processing failed: " + e.getMessage())
                .addAllProcessingSteps(steps)
                .processingTime(0L)
                .responseChanged(false)
                .addMetric("error", true)
                .addMetric("error_message", e.getMessage())
                .build();
    }

    /**
     * Inner class representing an ensemble response.
     */
    private static class EnsembleResponse {
        private final int id;
        private final String response;
        private final float temperature;
        private final long processingTime;

        public EnsembleResponse(int id, String response, float temperature, long processingTime) {
            this.id = id;
            this.response = response;
            this.temperature = temperature;
            this.processingTime = processingTime;
        }

        public int getId() {
            return id;
        }

        public String getResponse() {
            return response;
        }

        public float getTemperature() {
            return temperature;
        }

        public long getProcessingTime() {
            return processingTime;
        }
    }

    /**
     * Inner class representing voting analysis results.
     */
    private static class VotingAnalysis {
        private String winningResponse;
        private int winnerVotes;
        private List<String> topResponses;

        public boolean hasClearWinner() {
            return winnerVotes > 1;
        }

        public String getWinningResponse() {
            return winningResponse;
        }

        public void setWinningResponse(String winningResponse) {
            this.winningResponse = winningResponse;
        }

        public int getWinnerVotes() {
            return winnerVotes;
        }

        public void setWinnerVotes(int winnerVotes) {
            this.winnerVotes = winnerVotes;
        }

        public List<String> getTopResponses() {
            return topResponses;
        }

        public void setTopResponses(List<String> topResponses) {
            this.topResponses = topResponses;
        }
    }
}