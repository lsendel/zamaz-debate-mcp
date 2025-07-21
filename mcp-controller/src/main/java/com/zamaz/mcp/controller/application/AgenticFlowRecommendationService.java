package com.zamaz.mcp.controller.application;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.controller.domain.analytics.*;
import com.zamaz.mcp.controller.domain.recommendation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for recommending agentic flows based on context and historical performance.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowRecommendationService {
    
    private final AgenticFlowAnalyticsService analyticsService;
    
    /**
     * Gets flow recommendations based on debate context.
     */
    public CompletableFuture<FlowRecommendation> recommendFlowsForDebate(
            UUID organizationId,
            DebateContext context) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating flow recommendations for debate context: {}", context);
            
            // Get historical performance data
            List<TrendingFlowType> trendingFlows = analyticsService
                .getTrendingFlowTypes(organizationId, 20)
                .join();
            
            // Get flow type statistics for the last 90 days
            LocalDateTime startDate = LocalDateTime.now().minusDays(90);
            LocalDateTime endDate = LocalDateTime.now();
            
            Map<AgenticFlowType, AgenticFlowAnalyticsSummary> flowStats = new HashMap<>();
            for (AgenticFlowType flowType : AgenticFlowType.values()) {
                AgenticFlowAnalyticsSummary summary = analyticsService
                    .getFlowTypeAnalytics(organizationId, flowType, startDate, endDate)
                    .join();
                if (summary.hasSignificantData()) {
                    flowStats.put(flowType, summary);
                }
            }
            
            // Generate recommendations based on context
            List<FlowTypeRecommendation> recommendations = generateRecommendations(
                context, trendingFlows, flowStats);
            
            // Build recommendation response
            return FlowRecommendation.builder()
                .context(context)
                .recommendations(recommendations)
                .reasoning(generateReasoningExplanation(context, recommendations))
                .timestamp(LocalDateTime.now())
                .build();
        });
    }
    
    /**
     * Gets flow recommendations for a specific participant.
     */
    public CompletableFuture<FlowRecommendation> recommendFlowsForParticipant(
            UUID organizationId,
            ParticipantContext participantContext,
            DebateContext debateContext) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating flow recommendations for participant: {}", participantContext);
            
            // Get base recommendations for debate
            FlowRecommendation baseRecommendation = recommendFlowsForDebate(
                organizationId, debateContext).join();
            
            // Adjust recommendations based on participant context
            List<FlowTypeRecommendation> adjustedRecommendations = 
                adjustRecommendationsForParticipant(
                    baseRecommendation.getRecommendations(),
                    participantContext);
            
            return FlowRecommendation.builder()
                .context(debateContext)
                .participantContext(participantContext)
                .recommendations(adjustedRecommendations)
                .reasoning(generateParticipantReasoningExplanation(
                    participantContext, adjustedRecommendations))
                .timestamp(LocalDateTime.now())
                .build();
        });
    }
    
    /**
     * Gets adaptive recommendations based on real-time performance.
     */
    public CompletableFuture<AdaptiveFlowRecommendation> getAdaptiveRecommendations(
            UUID debateId,
            UUID participantId,
            PerformanceContext currentPerformance) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating adaptive recommendations for participant {} in debate {}", 
                participantId, debateId);
            
            // Analyze current performance trends
            PerformanceAnalysis analysis = analyzePerformance(currentPerformance);
            
            // Get recommendations based on performance issues
            List<FlowTypeRecommendation> adaptiveRecommendations = 
                generateAdaptiveRecommendations(analysis);
            
            return AdaptiveFlowRecommendation.builder()
                .debateId(debateId)
                .participantId(participantId)
                .currentFlowType(currentPerformance.getCurrentFlowType())
                .performanceAnalysis(analysis)
                .recommendations(adaptiveRecommendations)
                .shouldSwitch(analysis.hasSignificantIssues())
                .switchUrgency(calculateSwitchUrgency(analysis))
                .timestamp(LocalDateTime.now())
                .build();
        });
    }
    
    // Private helper methods
    
    private List<FlowTypeRecommendation> generateRecommendations(
            DebateContext context,
            List<TrendingFlowType> trendingFlows,
            Map<AgenticFlowType, AgenticFlowAnalyticsSummary> flowStats) {
        
        List<FlowTypeRecommendation> recommendations = new ArrayList<>();
        
        // Score each flow type based on context and performance
        for (AgenticFlowType flowType : AgenticFlowType.values()) {
            double contextScore = calculateContextScore(flowType, context);
            double performanceScore = calculatePerformanceScore(flowType, flowStats);
            double trendScore = calculateTrendScore(flowType, trendingFlows);
            
            // Weighted combination of scores
            double totalScore = contextScore * 0.4 + performanceScore * 0.4 + trendScore * 0.2;
            
            if (totalScore > 0.3) { // Threshold for recommendation
                recommendations.add(FlowTypeRecommendation.builder()
                    .flowType(flowType)
                    .score(totalScore)
                    .reasons(generateReasons(flowType, context, flowStats))
                    .expectedBenefits(getExpectedBenefits(flowType, context))
                    .potentialDrawbacks(getPotentialDrawbacks(flowType, context))
                    .build());
            }
        }
        
        // Sort by score and return top recommendations
        return recommendations.stream()
            .sorted(Comparator.comparing(FlowTypeRecommendation::getScore).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private double calculateContextScore(AgenticFlowType flowType, DebateContext context) {
        double score = 0.0;
        
        // Match flow type to debate characteristics
        switch (flowType) {
            case INTERNAL_MONOLOGUE:
                if (context.requiresDeepReasoning() || context.isPhilosophical()) {
                    score += 0.8;
                }
                break;
                
            case SELF_CRITIQUE_LOOP:
                if (context.requiresAccuracy() || context.isHighStakes()) {
                    score += 0.9;
                }
                break;
                
            case MULTI_AGENT_RED_TEAM:
                if (context.isControversial() || context.requiresMultiplePerspectives()) {
                    score += 0.85;
                }
                break;
                
            case TOOL_CALLING_VERIFICATION:
                if (context.requiresFactChecking() || context.hasFactualClaims()) {
                    score += 0.95;
                }
                break;
                
            case RAG_WITH_RERANKING:
                if (context.requiresResearch() || context.hasComplexQuestions()) {
                    score += 0.9;
                }
                break;
                
            case CONFIDENCE_SCORING:
                if (context.requiresTransparency() || context.isEducational()) {
                    score += 0.7;
                }
                break;
                
            case CONSTITUTIONAL_PROMPTING:
                if (context.hasSensitiveTopics() || context.requiresEthicalConsiderations()) {
                    score += 0.85;
                }
                break;
                
            case ENSEMBLE_VOTING:
                if (context.requiresConsensus() || context.isHighStakes()) {
                    score += 0.8;
                }
                break;
                
            case POST_PROCESSING_RULES:
                if (context.requiresFormatting() || context.hasSpecificRequirements()) {
                    score += 0.7;
                }
                break;
                
            case TREE_OF_THOUGHTS:
                if (context.requiresProblemSolving() || context.hasComplexDecisions()) {
                    score += 0.85;
                }
                break;
                
            case STEP_BACK_PROMPTING:
                if (context.requiresAbstraction() || context.isTheoretical()) {
                    score += 0.75;
                }
                break;
                
            case PROMPT_CHAINING:
                if (context.hasMultiStepTasks() || context.requiresSequentialReasoning()) {
                    score += 0.8;
                }
                break;
        }
        
        // Adjust based on debate format
        if (context.getFormat() == DebateFormat.OXFORD && 
            (flowType == AgenticFlowType.MULTI_AGENT_RED_TEAM || 
             flowType == AgenticFlowType.SELF_CRITIQUE_LOOP)) {
            score += 0.1;
        }
        
        return Math.min(1.0, score);
    }
    
    private double calculatePerformanceScore(
            AgenticFlowType flowType,
            Map<AgenticFlowType, AgenticFlowAnalyticsSummary> flowStats) {
        
        AgenticFlowAnalyticsSummary stats = flowStats.get(flowType);
        if (stats == null || stats.getExecutionCount() < 10) {
            return 0.5; // Neutral score for insufficient data
        }
        
        // Combine multiple performance metrics
        double confidenceScore = stats.getAverageConfidence() / 100.0;
        double successScore = stats.getSuccessRate();
        double speedScore = Math.max(0, 1.0 - (stats.getAverageExecutionTime().toMillis() / 10000.0));
        
        return (confidenceScore * 0.4 + successScore * 0.4 + speedScore * 0.2);
    }
    
    private double calculateTrendScore(
            AgenticFlowType flowType,
            List<TrendingFlowType> trendingFlows) {
        
        return trendingFlows.stream()
            .filter(t -> t.getFlowType() == flowType)
            .findFirst()
            .map(TrendingFlowType::getTrendScore)
            .orElse(0.3); // Low score if not trending
    }
    
    private List<String> generateReasons(
            AgenticFlowType flowType,
            DebateContext context,
            Map<AgenticFlowType, AgenticFlowAnalyticsSummary> flowStats) {
        
        List<String> reasons = new ArrayList<>();
        
        // Add context-based reasons
        if (context.requiresFactChecking() && flowType == AgenticFlowType.TOOL_CALLING_VERIFICATION) {
            reasons.add("Debate topic requires fact verification");
        }
        
        // Add performance-based reasons
        AgenticFlowAnalyticsSummary stats = flowStats.get(flowType);
        if (stats != null && stats.getAverageConfidence() > 85) {
            reasons.add(String.format("High average confidence (%.1f%%)", stats.getAverageConfidence()));
        }
        
        if (stats != null && stats.getSuccessRate() > 0.9) {
            reasons.add(String.format("Excellent success rate (%.1f%%)", stats.getSuccessRate() * 100));
        }
        
        return reasons;
    }
    
    private List<String> getExpectedBenefits(AgenticFlowType flowType, DebateContext context) {
        List<String> benefits = new ArrayList<>();
        
        switch (flowType) {
            case INTERNAL_MONOLOGUE:
                benefits.add("Clear reasoning process");
                benefits.add("Step-by-step logic");
                break;
            case SELF_CRITIQUE_LOOP:
                benefits.add("Self-correcting responses");
                benefits.add("Higher accuracy");
                break;
            case MULTI_AGENT_RED_TEAM:
                benefits.add("Multiple perspectives");
                benefits.add("Balanced arguments");
                break;
            // Add more cases...
        }
        
        return benefits;
    }
    
    private List<String> getPotentialDrawbacks(AgenticFlowType flowType, DebateContext context) {
        List<String> drawbacks = new ArrayList<>();
        
        switch (flowType) {
            case INTERNAL_MONOLOGUE:
                drawbacks.add("Longer response times");
                break;
            case SELF_CRITIQUE_LOOP:
                drawbacks.add("May overthink simple questions");
                break;
            case MULTI_AGENT_RED_TEAM:
                drawbacks.add("Complex processing");
                break;
            // Add more cases...
        }
        
        return drawbacks;
    }
    
    private String generateReasoningExplanation(
            DebateContext context,
            List<FlowTypeRecommendation> recommendations) {
        
        if (recommendations.isEmpty()) {
            return "No specific flow types recommended for this debate context.";
        }
        
        FlowTypeRecommendation topRecommendation = recommendations.get(0);
        return String.format(
            "Based on the debate topic '%s' and format '%s', %s is recommended as it %s. " +
            "This flow type has shown %s in similar contexts.",
            context.getTopic(),
            context.getFormat(),
            topRecommendation.getFlowType(),
            String.join(" and ", topRecommendation.getReasons()),
            topRecommendation.getScore() > 0.8 ? "excellent performance" : "good performance"
        );
    }
    
    private List<FlowTypeRecommendation> adjustRecommendationsForParticipant(
            List<FlowTypeRecommendation> baseRecommendations,
            ParticipantContext participantContext) {
        
        // Adjust scores based on participant characteristics
        return baseRecommendations.stream()
            .map(rec -> {
                double adjustedScore = rec.getScore();
                
                // Adjust based on LLM model capabilities
                if (participantContext.hasLargeModel() && 
                    (rec.getFlowType() == AgenticFlowType.TREE_OF_THOUGHTS ||
                     rec.getFlowType() == AgenticFlowType.MULTI_AGENT_RED_TEAM)) {
                    adjustedScore *= 1.2;
                }
                
                // Adjust based on participant role
                if (participantContext.getRole() == ParticipantRole.EXPERT &&
                    rec.getFlowType() == AgenticFlowType.CONFIDENCE_SCORING) {
                    adjustedScore *= 1.1;
                }
                
                return FlowTypeRecommendation.builder()
                    .flowType(rec.getFlowType())
                    .score(Math.min(1.0, adjustedScore))
                    .reasons(rec.getReasons())
                    .expectedBenefits(rec.getExpectedBenefits())
                    .potentialDrawbacks(rec.getPotentialDrawbacks())
                    .build();
            })
            .sorted(Comparator.comparing(FlowTypeRecommendation::getScore).reversed())
            .collect(Collectors.toList());
    }
    
    private String generateParticipantReasoningExplanation(
            ParticipantContext participantContext,
            List<FlowTypeRecommendation> recommendations) {
        
        return String.format(
            "For participant '%s' using %s model, adjusted recommendations based on " +
            "model capabilities and participant role.",
            participantContext.getName(),
            participantContext.getModelName()
        );
    }
    
    private PerformanceAnalysis analyzePerformance(PerformanceContext context) {
        List<String> issues = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        
        // Analyze confidence trends
        if (context.getAverageConfidence() < 60) {
            issues.add("Low confidence scores");
        } else if (context.getAverageConfidence() > 85) {
            strengths.add("High confidence scores");
        }
        
        // Analyze response quality
        if (context.getResponseChangeRate() > 0.3) {
            issues.add("High response revision rate");
        }
        
        // Analyze execution time
        if (context.getAverageExecutionTime() > 5000) {
            issues.add("Slow execution times");
        }
        
        return PerformanceAnalysis.builder()
            .issues(issues)
            .strengths(strengths)
            .overallScore(calculateOverallScore(context))
            .hasSignificantIssues(issues.size() >= 2)
            .build();
    }
    
    private List<FlowTypeRecommendation> generateAdaptiveRecommendations(
            PerformanceAnalysis analysis) {
        
        List<FlowTypeRecommendation> recommendations = new ArrayList<>();
        
        // Recommend based on identified issues
        if (analysis.getIssues().contains("Low confidence scores")) {
            recommendations.add(createQuickRecommendation(
                AgenticFlowType.CONFIDENCE_SCORING,
                "Improve confidence through explicit scoring"));
        }
        
        if (analysis.getIssues().contains("High response revision rate")) {
            recommendations.add(createQuickRecommendation(
                AgenticFlowType.SELF_CRITIQUE_LOOP,
                "Reduce revisions through upfront critique"));
        }
        
        return recommendations;
    }
    
    private FlowTypeRecommendation createQuickRecommendation(
            AgenticFlowType flowType,
            String reason) {
        
        return FlowTypeRecommendation.builder()
            .flowType(flowType)
            .score(0.8)
            .reasons(List.of(reason))
            .expectedBenefits(List.of("Address current performance issues"))
            .potentialDrawbacks(List.of("May require adjustment period"))
            .build();
    }
    
    private double calculateOverallScore(PerformanceContext context) {
        double confidenceScore = context.getAverageConfidence() / 100.0;
        double qualityScore = 1.0 - context.getResponseChangeRate();
        double speedScore = Math.max(0, 1.0 - (context.getAverageExecutionTime() / 10000.0));
        
        return (confidenceScore * 0.4 + qualityScore * 0.4 + speedScore * 0.2);
    }
    
    private SwitchUrgency calculateSwitchUrgency(PerformanceAnalysis analysis) {
        if (analysis.getOverallScore() < 0.3 || analysis.getIssues().size() >= 3) {
            return SwitchUrgency.HIGH;
        } else if (analysis.getOverallScore() < 0.5 || analysis.getIssues().size() >= 2) {
            return SwitchUrgency.MEDIUM;
        } else if (analysis.getIssues().size() >= 1) {
            return SwitchUrgency.LOW;
        }
        return SwitchUrgency.NONE;
    }
}