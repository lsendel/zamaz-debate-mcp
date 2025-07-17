package com.zamaz.mcp.controller.ai;

import com.zamaz.mcp.controller.dto.DebateAnalysisDto;
import com.zamaz.mcp.controller.entity.Debate;
import com.zamaz.mcp.controller.entity.Response;
import com.zamaz.mcp.controller.integration.LlmServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI-powered debate quality scoring service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebateQualityScorer {
    
    private final LlmServiceClient llmServiceClient;
    private final ArgumentAnalyzer argumentAnalyzer;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final CoherenceAnalyzer coherenceAnalyzer;
    private final FactualityChecker factualityChecker;
    
    @Value("${ai.quality-scoring.enabled:true}")
    private boolean qualityScoringEnabled;
    
    @Value("${ai.quality-scoring.model:claude-3-opus-20240229}")
    private String qualityScoringModel;
    
    @Value("${ai.quality-scoring.provider:anthropic}")
    private String qualityScoringProvider;
    
    /**
     * Comprehensive debate quality analysis
     */
    @Async
    public CompletableFuture<DebateAnalysisDto> analyzeDebateQuality(Debate debate) {
        if (!qualityScoringEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            log.info("Starting quality analysis for debate: {}", debate.getId());
            
            // Collect all responses
            List<Response> allResponses = debate.getRounds().stream()
                .flatMap(round -> round.getResponses().stream())
                .collect(Collectors.toList());
            
            if (allResponses.isEmpty()) {
                return CompletableFuture.completedFuture(createEmptyAnalysis(debate));
            }
            
            // Parallel analysis of different quality aspects
            CompletableFuture<Map<String, Double>> argumentScores = analyzeArgumentQuality(allResponses);
            CompletableFuture<Map<String, Double>> coherenceScores = analyzeCoherence(allResponses);
            CompletableFuture<Map<String, Object>> sentimentAnalysis = analyzeSentiment(allResponses);
            CompletableFuture<Map<String, Double>> factualityScores = analyzeFactuality(allResponses);
            CompletableFuture<DebateStructureAnalysis> structureAnalysis = analyzeDebateStructure(debate);
            CompletableFuture<Map<String, Object>> llmAnalysis = performLLMAnalysis(debate, allResponses);
            
            // Combine all analyses
            return CompletableFuture.allOf(
                argumentScores, coherenceScores, sentimentAnalysis, 
                factualityScores, structureAnalysis, llmAnalysis
            ).thenApply(v -> {
                try {
                    return buildComprehensiveAnalysis(
                        debate,
                        argumentScores.get(),
                        coherenceScores.get(),
                        sentimentAnalysis.get(),
                        factualityScores.get(),
                        structureAnalysis.get(),
                        llmAnalysis.get()
                    );
                } catch (Exception e) {
                    log.error("Error combining analysis results", e);
                    return createErrorAnalysis(debate, e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error in debate quality analysis", e);
            return CompletableFuture.completedFuture(createErrorAnalysis(debate, e));
        }
    }
    
    /**
     * Analyze argument quality using AI
     */
    private CompletableFuture<Map<String, Double>> analyzeArgumentQuality(List<Response> responses) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> scores = new HashMap<>();
            
            for (Response response : responses) {
                try {
                    ArgumentQualityMetrics metrics = argumentAnalyzer.analyzeArgument(response.getContent());
                    
                    double overallScore = calculateArgumentScore(metrics);
                    scores.put(response.getId().toString(), overallScore);
                    
                    log.debug("Argument quality score for response {}: {}", 
                        response.getId(), overallScore);
                        
                } catch (Exception e) {
                    log.warn("Failed to analyze argument quality for response: {}", 
                        response.getId(), e);
                    scores.put(response.getId().toString(), 0.5); // Default neutral score
                }
            }
            
            return scores;
        });
    }
    
    /**
     * Analyze coherence and logical flow
     */
    private CompletableFuture<Map<String, Double>> analyzeCoherence(List<Response> responses) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> scores = new HashMap<>();
            
            for (Response response : responses) {
                try {
                    CoherenceMetrics metrics = coherenceAnalyzer.analyzeCoherence(response.getContent());
                    double coherenceScore = calculateCoherenceScore(metrics);
                    scores.put(response.getId().toString(), coherenceScore);
                    
                } catch (Exception e) {
                    log.warn("Failed to analyze coherence for response: {}", response.getId(), e);
                    scores.put(response.getId().toString(), 0.5);
                }
            }
            
            return scores;
        });
    }
    
    /**
     * Analyze sentiment and emotional tone
     */
    private CompletableFuture<Map<String, Object>> analyzeSentiment(List<Response> responses) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> sentimentData = new HashMap<>();
            
            for (Response response : responses) {
                try {
                    SentimentMetrics metrics = sentimentAnalyzer.analyzeSentiment(response.getContent());
                    
                    Map<String, Object> responseMetrics = new HashMap<>();
                    responseMetrics.put("sentiment", metrics.getSentiment());
                    responseMetrics.put("confidence", metrics.getConfidence());
                    responseMetrics.put("emotions", metrics.getEmotions());
                    responseMetrics.put("toxicity", metrics.getToxicityScore());
                    responseMetrics.put("professionalism", metrics.getProfessionalismScore());
                    
                    sentimentData.put(response.getId().toString(), responseMetrics);
                    
                } catch (Exception e) {
                    log.warn("Failed to analyze sentiment for response: {}", response.getId(), e);
                }
            }
            
            return sentimentData;
        });
    }
    
    /**
     * Analyze factual accuracy and evidence quality
     */
    private CompletableFuture<Map<String, Double>> analyzeFactuality(List<Response> responses) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> scores = new HashMap<>();
            
            for (Response response : responses) {
                try {
                    FactualityMetrics metrics = factualityChecker.checkFactuality(response.getContent());
                    double factualityScore = calculateFactualityScore(metrics);
                    scores.put(response.getId().toString(), factualityScore);
                    
                } catch (Exception e) {
                    log.warn("Failed to analyze factuality for response: {}", response.getId(), e);
                    scores.put(response.getId().toString(), 0.5);
                }
            }
            
            return scores;
        });
    }
    
    /**
     * Analyze overall debate structure and flow
     */
    private CompletableFuture<DebateStructureAnalysis> analyzeDebateStructure(Debate debate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeStructure(debate);
            } catch (Exception e) {
                log.warn("Failed to analyze debate structure", e);
                return new DebateStructureAnalysis(); // Return empty analysis
            }
        });
    }
    
    /**
     * Perform comprehensive LLM-based analysis
     */
    private CompletableFuture<Map<String, Object>> performLLMAnalysis(Debate debate, List<Response> responses) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performAdvancedLLMAnalysis(debate, responses);
            } catch (Exception e) {
                log.warn("Failed to perform LLM analysis", e);
                return new HashMap<>();
            }
        });
    }
    
    /**
     * Advanced LLM analysis using external model
     */
    private Map<String, Object> performAdvancedLLMAnalysis(Debate debate, List<Response> responses) {
        // Prepare context for LLM analysis
        String debateContext = buildDebateContext(debate, responses);
        
        Map<String, Object> analysisRequest = Map.of(
            "provider", qualityScoringProvider,
            "model", qualityScoringModel,
            "messages", List.of(
                Map.of("role", "system", "content", getAnalysisSystemPrompt()),
                Map.of("role", "user", "content", debateContext)
            ),
            "maxTokens", 2000,
            "temperature", 0.3
        );
        
        try {
            Map<String, Object> response = llmServiceClient.generateCompletion(analysisRequest);
            return parseAnalysisResponse(response);
        } catch (Exception e) {
            log.error("Failed to get LLM analysis", e);
            return Map.of("error", "LLM analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Calculate overall argument quality score
     */
    private double calculateArgumentScore(ArgumentQualityMetrics metrics) {
        return (metrics.getLogicalStrength() * 0.3 +
                metrics.getEvidenceQuality() * 0.25 +
                metrics.getClarityScore() * 0.2 +
                metrics.getRelevanceScore() * 0.15 +
                metrics.getOriginalityScore() * 0.1);
    }
    
    /**
     * Calculate coherence score
     */
    private double calculateCoherenceScore(CoherenceMetrics metrics) {
        return (metrics.getLogicalFlow() * 0.4 +
                metrics.getStructuralCoherence() * 0.3 +
                metrics.getTransitionQuality() * 0.3);
    }
    
    /**
     * Calculate factuality score
     */
    private double calculateFactualityScore(FactualityMetrics metrics) {
        return (metrics.getFactualAccuracy() * 0.5 +
                metrics.getSourceReliability() * 0.3 +
                metrics.getEvidenceStrength() * 0.2);
    }
    
    /**
     * Build comprehensive analysis result
     */
    private DebateAnalysisDto buildComprehensiveAnalysis(
            Debate debate,
            Map<String, Double> argumentScores,
            Map<String, Double> coherenceScores,
            Map<String, Object> sentimentAnalysis,
            Map<String, Double> factualityScores,
            DebateStructureAnalysis structureAnalysis,
            Map<String, Object> llmAnalysis) {
        
        // Calculate overall scores
        double overallQuality = calculateOverallQuality(
            argumentScores, coherenceScores, factualityScores);
        
        return DebateAnalysisDto.builder()
            .debateId(debate.getId().toString())
            .overallQualityScore(overallQuality)
            .argumentQualityScores(argumentScores)
            .coherenceScores(coherenceScores)
            .sentimentAnalysis(sentimentAnalysis)
            .factualityScores(factualityScores)
            .structureAnalysis(structureAnalysis)
            .llmInsights(llmAnalysis)
            .qualityGrade(determineQualityGrade(overallQuality))
            .recommendations(generateRecommendations(overallQuality, structureAnalysis))
            .analysisTimestamp(java.time.LocalDateTime.now())
            .build();
    }
    
    /**
     * Calculate overall debate quality score
     */
    private double calculateOverallQuality(Map<String, Double> argumentScores,
                                          Map<String, Double> coherenceScores,
                                          Map<String, Double> factualityScores) {
        double avgArgument = argumentScores.values().stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.5);
        double avgCoherence = coherenceScores.values().stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.5);
        double avgFactuality = factualityScores.values().stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.5);
            
        return (avgArgument * 0.4 + avgCoherence * 0.3 + avgFactuality * 0.3);
    }
    
    /**
     * Determine quality grade based on score
     */
    private String determineQualityGrade(double score) {
        if (score >= 0.9) return "A+";
        if (score >= 0.8) return "A";
        if (score >= 0.7) return "B+";
        if (score >= 0.6) return "B";
        if (score >= 0.5) return "C+";
        if (score >= 0.4) return "C";
        if (score >= 0.3) return "D";
        return "F";
    }
    
    /**
     * Generate improvement recommendations
     */
    private List<String> generateRecommendations(double overallQuality, DebateStructureAnalysis structureAnalysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (overallQuality < 0.6) {
            recommendations.add("Focus on providing stronger evidence to support arguments");
            recommendations.add("Improve logical structure and flow of arguments");
        }
        
        if (structureAnalysis.getBalanceScore() < 0.5) {
            recommendations.add("Ensure more balanced participation from all sides");
        }
        
        if (structureAnalysis.getEngagementScore() < 0.6) {
            recommendations.add("Encourage more direct engagement between participants");
        }
        
        return recommendations;
    }
    
    // Helper methods for creating default/error responses
    private DebateAnalysisDto createEmptyAnalysis(Debate debate) {
        return DebateAnalysisDto.builder()
            .debateId(debate.getId().toString())
            .overallQualityScore(0.0)
            .qualityGrade("N/A")
            .analysisTimestamp(java.time.LocalDateTime.now())
            .build();
    }
    
    private DebateAnalysisDto createErrorAnalysis(Debate debate, Exception error) {
        return DebateAnalysisDto.builder()
            .debateId(debate.getId().toString())
            .overallQualityScore(0.0)
            .qualityGrade("ERROR")
            .llmInsights(Map.of("error", error.getMessage()))
            .analysisTimestamp(java.time.LocalDateTime.now())
            .build();
    }
    
    // Additional helper methods would be implemented here:
    // - buildDebateContext()
    // - getAnalysisSystemPrompt()
    // - parseAnalysisResponse()
    // - analyzeStructure()
    
    /**
     * Get system prompt for LLM analysis
     */
    private String getAnalysisSystemPrompt() {
        return """You are an expert debate analyst. Analyze the provided debate and provide insights on:
        1. Argument strength and logical validity
        2. Use of evidence and factual accuracy
        3. Rhetorical techniques and persuasiveness
        4. Areas for improvement
        5. Overall debate quality assessment
        
        Provide your analysis in JSON format with specific scores and detailed explanations.""";
    }
    
    /**
     * Build context string for LLM analysis
     */
    private String buildDebateContext(Debate debate, List<Response> responses) {
        StringBuilder context = new StringBuilder();
        context.append("Debate Topic: ").append(debate.getTopic()).append("\n\n");
        
        Map<Integer, List<Response>> responsesByRound = responses.stream()
            .collect(Collectors.groupingBy(r -> r.getRound().getRoundNumber()));
        
        responsesByRound.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                context.append("Round ").append(entry.getKey()).append(":\n");
                entry.getValue().forEach(response -> {
                    context.append(response.getParticipant().getName())
                           .append(" (")
                           .append(response.getParticipant().getPosition())
                           .append("): ")
                           .append(response.getContent())
                           .append("\n\n");
                });
            });
        
        return context.toString();
    }
    
    /**
     * Parse LLM analysis response
     */
    private Map<String, Object> parseAnalysisResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                
                // Try to parse as JSON, fallback to text analysis
                try {
                    return new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(content, java.util.HashMap.class);
                } catch (Exception e) {
                    return Map.of("analysis", content);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse LLM analysis response", e);
        }
        
        return Map.of("error", "Failed to parse analysis response");
    }
    
    /**
     * Analyze debate structure
     */
    private DebateStructureAnalysis analyzeStructure(Debate debate) {
        // Calculate various structural metrics
        List<Response> allResponses = debate.getRounds().stream()
            .flatMap(round -> round.getResponses().stream())
            .collect(Collectors.toList());
        
        double balanceScore = calculateParticipationBalance(allResponses);
        double engagementScore = calculateEngagementLevel(allResponses);
        double progressionScore = calculateProgressionQuality(debate);
        
        return DebateStructureAnalysis.builder()
            .balanceScore(balanceScore)
            .engagementScore(engagementScore)
            .progressionScore(progressionScore)
            .totalRounds(debate.getRounds().size())
            .totalResponses(allResponses.size())
            .averageResponseLength(calculateAverageResponseLength(allResponses))
            .build();
    }
    
    // Additional structural analysis helper methods would be implemented here
    private double calculateParticipationBalance(List<Response> responses) {
        if (responses.isEmpty()) return 0.0;
        
        Map<String, Long> participantCounts = responses.stream()
            .collect(Collectors.groupingBy(
                r -> r.getParticipant().getName(),
                Collectors.counting()
            ));
        
        double mean = participantCounts.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        double variance = participantCounts.values().stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average()
            .orElse(0.0);
        
        // Lower variance = better balance
        return Math.max(0.0, 1.0 - (variance / (mean * mean)));
    }
    
    private double calculateEngagementLevel(List<Response> responses) {
        // This would analyze cross-references, direct responses, etc.
        // Simplified implementation
        return Math.min(1.0, responses.size() / 10.0);
    }
    
    private double calculateProgressionQuality(Debate debate) {
        // Analyze how arguments build upon each other
        // Simplified implementation
        return Math.min(1.0, debate.getRounds().size() / (double) debate.getMaxRounds());
    }
    
    private double calculateAverageResponseLength(List<Response> responses) {
        return responses.stream()
            .mapToInt(r -> r.getContent().length())
            .average()
            .orElse(0.0);
    }
}