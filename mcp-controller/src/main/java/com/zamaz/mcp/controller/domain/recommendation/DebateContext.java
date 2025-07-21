package com.zamaz.mcp.controller.domain.recommendation;

import lombok.*;
import java.util.Set;

/**
 * Context information about a debate used for flow recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateContext {
    private String topic;
    private DebateFormat format;
    private Set<String> topicCategories;
    private int expectedRounds;
    private boolean isHighStakes;
    private boolean requiresFactChecking;
    private boolean requiresDeepReasoning;
    private boolean isControversial;
    private boolean hasSensitiveTopics;
    
    // Derived properties
    public boolean isPhilosophical() {
        return topicCategories != null && 
            (topicCategories.contains("philosophy") || 
             topicCategories.contains("ethics") ||
             topicCategories.contains("metaphysics"));
    }
    
    public boolean requiresAccuracy() {
        return isHighStakes || requiresFactChecking;
    }
    
    public boolean requiresMultiplePerspectives() {
        return isControversial || format == DebateFormat.PANEL;
    }
    
    public boolean hasFactualClaims() {
        return topicCategories != null && 
            (topicCategories.contains("science") || 
             topicCategories.contains("history") ||
             topicCategories.contains("statistics"));
    }
    
    public boolean requiresResearch() {
        return topicCategories != null && 
            (topicCategories.contains("academic") || 
             topicCategories.contains("research") ||
             topicCategories.contains("technical"));
    }
    
    public boolean hasComplexQuestions() {
        return requiresDeepReasoning || isPhilosophical();
    }
    
    public boolean requiresTransparency() {
        return isHighStakes || hasSensitiveTopics;
    }
    
    public boolean isEducational() {
        return topicCategories != null && topicCategories.contains("educational");
    }
    
    public boolean requiresEthicalConsiderations() {
        return hasSensitiveTopics || 
            (topicCategories != null && topicCategories.contains("ethics"));
    }
    
    public boolean requiresConsensus() {
        return format == DebateFormat.CONSENSUS_BUILDING;
    }
    
    public boolean requiresFormatting() {
        return format == DebateFormat.FORMAL;
    }
    
    public boolean hasSpecificRequirements() {
        return isHighStakes || format == DebateFormat.FORMAL;
    }
    
    public boolean requiresProblemSolving() {
        return topicCategories != null && 
            (topicCategories.contains("problem-solving") || 
             topicCategories.contains("strategy"));
    }
    
    public boolean hasComplexDecisions() {
        return requiresProblemSolving() || isHighStakes;
    }
    
    public boolean requiresAbstraction() {
        return isPhilosophical() || isTheoretical();
    }
    
    public boolean isTheoretical() {
        return topicCategories != null && 
            (topicCategories.contains("theoretical") || 
             topicCategories.contains("abstract"));
    }
    
    public boolean hasMultiStepTasks() {
        return topicCategories != null && topicCategories.contains("procedural");
    }
    
    public boolean requiresSequentialReasoning() {
        return hasMultiStepTasks() || requiresProblemSolving();
    }
}