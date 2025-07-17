package com.zamaz.mcp.controller.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Analyzer for argument quality and logical strength
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArgumentAnalyzer {
    
    // Patterns for detecting logical structures
    private static final List<Pattern> EVIDENCE_PATTERNS = Arrays.asList(
        Pattern.compile("according to", Pattern.CASE_INSENSITIVE),
        Pattern.compile("studies show", Pattern.CASE_INSENSITIVE),
        Pattern.compile("research indicates", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data suggests", Pattern.CASE_INSENSITIVE),
        Pattern.compile("statistics reveal", Pattern.CASE_INSENSITIVE),
        Pattern.compile("evidence shows", Pattern.CASE_INSENSITIVE),
        Pattern.compile("for example", Pattern.CASE_INSENSITIVE),
        Pattern.compile("for instance", Pattern.CASE_INSENSITIVE)
    );
    
    private static final List<Pattern> LOGICAL_CONNECTORS = Arrays.asList(
        Pattern.compile("therefore", Pattern.CASE_INSENSITIVE),
        Pattern.compile("because", Pattern.CASE_INSENSITIVE),
        Pattern.compile("since", Pattern.CASE_INSENSITIVE),
        Pattern.compile("as a result", Pattern.CASE_INSENSITIVE),
        Pattern.compile("consequently", Pattern.CASE_INSENSITIVE),
        Pattern.compile("hence", Pattern.CASE_INSENSITIVE),
        Pattern.compile("thus", Pattern.CASE_INSENSITIVE),
        Pattern.compile("however", Pattern.CASE_INSENSITIVE),
        Pattern.compile("nevertheless", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on the other hand", Pattern.CASE_INSENSITIVE)
    );
    
    private static final List<Pattern> FALLACY_PATTERNS = Arrays.asList(
        Pattern.compile("always|never|all|none|everyone|no one", Pattern.CASE_INSENSITIVE), // Overgeneralization
        Pattern.compile("you are wrong because", Pattern.CASE_INSENSITIVE), // Ad hominem
        Pattern.compile("either .* or .*", Pattern.CASE_INSENSITIVE), // False dichotomy
        Pattern.compile("if we .*, then .*", Pattern.CASE_INSENSITIVE) // Slippery slope
    );
    
    /**
     * Analyze argument quality
     */
    public ArgumentQualityMetrics analyzeArgument(String argumentText) {
        try {
            return ArgumentQualityMetrics.builder()
                .logicalStrength(analyzeLogicalStrength(argumentText))
                .evidenceQuality(analyzeEvidenceQuality(argumentText))
                .clarityScore(analyzeClarityScore(argumentText))
                .relevanceScore(analyzeRelevanceScore(argumentText))
                .originalityScore(analyzeOriginalityScore(argumentText))
                .logicalFallacies(detectLogicalFallacies(argumentText))
                .argumentStructure(analyzeArgumentStructure(argumentText))
                .build();
        } catch (Exception e) {
            log.error("Error analyzing argument: {}", argumentText, e);
            return getDefaultMetrics();
        }
    }
    
    /**
     * Analyze logical strength of argument
     */
    private double analyzeLogicalStrength(String text) {
        double score = 0.5; // Base score
        
        // Count logical connectors
        long connectorCount = LOGICAL_CONNECTORS.stream()
            .mapToLong(pattern -> countMatches(pattern, text))
            .sum();
        
        // Bonus for logical structure
        score += Math.min(0.3, connectorCount * 0.1);
        
        // Check for premise-conclusion structure
        if (hasPremiseConclusionStructure(text)) {
            score += 0.2;
        }
        
        // Penalty for logical fallacies
        long fallacyCount = FALLACY_PATTERNS.stream()
            .mapToLong(pattern -> countMatches(pattern, text))
            .sum();
        score -= fallacyCount * 0.1;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze evidence quality
     */
    private double analyzeEvidenceQuality(String text) {
        double score = 0.3; // Base score for any text
        
        // Count evidence indicators
        long evidenceCount = EVIDENCE_PATTERNS.stream()
            .mapToLong(pattern -> countMatches(pattern, text))
            .sum();
        
        score += Math.min(0.4, evidenceCount * 0.15);
        
        // Check for specific types of evidence
        if (containsNumericData(text)) {
            score += 0.15;
        }
        
        if (containsCitations(text)) {
            score += 0.15;
        }
        
        if (containsExamples(text)) {
            score += 0.1;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze clarity and readability
     */
    private double analyzeClarityScore(String text) {
        double score = 0.5;
        
        // Sentence length analysis
        String[] sentences = text.split("[.!?]+");
        double avgSentenceLength = Arrays.stream(sentences)
            .mapToInt(s -> s.trim().split("\\s+").length)
            .average()
            .orElse(0.0);
        
        // Optimal sentence length is 15-20 words
        if (avgSentenceLength >= 15 && avgSentenceLength <= 20) {
            score += 0.2;
        } else if (avgSentenceLength > 30) {
            score -= 0.2; // Penalty for overly long sentences
        }
        
        // Check for clear structure
        if (hasListStructure(text)) {
            score += 0.1;
        }
        
        if (hasTopicSentences(text)) {
            score += 0.1;
        }
        
        // Check for transition words
        long transitionCount = LOGICAL_CONNECTORS.stream()
            .mapToLong(pattern -> countMatches(pattern, text))
            .sum();
        score += Math.min(0.1, transitionCount * 0.02);
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze relevance to debate topic
     */
    private double analyzeRelevanceScore(String text) {
        // This would ideally compare against the debate topic
        // For now, use heuristics
        double score = 0.6; // Assume generally relevant
        
        // Check for off-topic indicators
        if (containsOffTopicPhrases(text)) {
            score -= 0.2;
        }
        
        // Check for topic-specific language
        if (containsTopicKeywords(text)) {
            score += 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze originality and uniqueness
     */
    private double analyzeOriginalityScore(String text) {
        double score = 0.5; // Base score
        
        // Check for clichés or common phrases
        if (containsCliches(text)) {
            score -= 0.2;
        }
        
        // Check for unique insights or perspectives
        if (containsUniqueInsights(text)) {
            score += 0.3;
        }
        
        // Check for creative analogies or metaphors
        if (containsAnalogies(text)) {
            score += 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Detect logical fallacies
     */
    private List<String> detectLogicalFallacies(String text) {
        List<String> fallacies = new java.util.ArrayList<>();
        
        if (FALLACY_PATTERNS.get(0).matcher(text).find()) {
            fallacies.add("Overgeneralization");
        }
        
        if (FALLACY_PATTERNS.get(1).matcher(text).find()) {
            fallacies.add("Ad Hominem");
        }
        
        if (FALLACY_PATTERNS.get(2).matcher(text).find()) {
            fallacies.add("False Dichotomy");
        }
        
        if (FALLACY_PATTERNS.get(3).matcher(text).find()) {
            fallacies.add("Slippery Slope");
        }
        
        return fallacies;
    }
    
    /**
     * Analyze argument structure
     */
    private ArgumentStructure analyzeArgumentStructure(String text) {
        boolean hasClaim = containsClaim(text);
        boolean hasEvidence = EVIDENCE_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(text).find());
        boolean hasWarrant = containsWarrant(text);
        boolean hasCounterargument = containsCounterargument(text);
        boolean hasRebuttal = containsRebuttal(text);
        
        return ArgumentStructure.builder()
            .hasClaim(hasClaim)
            .hasEvidence(hasEvidence)
            .hasWarrant(hasWarrant)
            .hasCounterargument(hasCounterargument)
            .hasRebuttal(hasRebuttal)
            .structureScore(calculateStructureScore(hasClaim, hasEvidence, hasWarrant, hasCounterargument, hasRebuttal))
            .build();
    }
    
    // Helper methods
    
    private long countMatches(Pattern pattern, String text) {
        return pattern.matcher(text).results().count();
    }
    
    private boolean hasPremiseConclusionStructure(String text) {
        return text.toLowerCase().contains("because") && 
               (text.toLowerCase().contains("therefore") || text.toLowerCase().contains("thus"));
    }
    
    private boolean containsNumericData(String text) {
        return Pattern.compile("\\d+%|\\d+\\.\\d+|\\$\\d+|\\d+ (million|billion|thousand)").matcher(text).find();
    }
    
    private boolean containsCitations(String text) {
        return Pattern.compile("\\(\\d{4}\\)|et al\\.|\\[\\d+\\]").matcher(text).find();
    }
    
    private boolean containsExamples(String text) {
        return Pattern.compile("for example|for instance|such as|including", Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean hasListStructure(String text) {
        return Pattern.compile("first|second|third|finally|lastly", Pattern.CASE_INSENSITIVE).matcher(text).find() ||
               Pattern.compile("\\d+\\.|•|\\*").matcher(text).find();
    }
    
    private boolean hasTopicSentences(String text) {
        String[] sentences = text.split("[.!?]+");
        return sentences.length > 1 && sentences[0].trim().length() > 20;
    }
    
    private boolean containsOffTopicPhrases(String text) {
        return Pattern.compile("by the way|speaking of|off topic|unrelated", Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsTopicKeywords(String text) {
        // This would ideally use the actual debate topic keywords
        // For now, assume any substantial text is relevant
        return text.length() > 100;
    }
    
    private boolean containsCliches(String text) {
        return Pattern.compile("at the end of the day|think outside the box|low hanging fruit|paradigm shift", 
                             Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsUniqueInsights(String text) {
        return Pattern.compile("surprisingly|unexpectedly|contrary to|interestingly|novel approach", 
                             Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsAnalogies(String text) {
        return Pattern.compile("like|as if|similar to|comparable to|just as", Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsClaim(String text) {
        return Pattern.compile("I argue|I claim|I believe|I contend|my position", Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsWarrant(String text) {
        return Pattern.compile("this is important because|this matters because|the significance", 
                             Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsCounterargument(String text) {
        return Pattern.compile("some might argue|critics claim|opponents say|however|on the other hand", 
                             Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private boolean containsRebuttal(String text) {
        return Pattern.compile("but this overlooks|however this ignores|this argument fails", 
                             Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    
    private double calculateStructureScore(boolean hasClaim, boolean hasEvidence, boolean hasWarrant, 
                                         boolean hasCounterargument, boolean hasRebuttal) {
        double score = 0.0;
        if (hasClaim) score += 0.3;
        if (hasEvidence) score += 0.3;
        if (hasWarrant) score += 0.2;
        if (hasCounterargument) score += 0.1;
        if (hasRebuttal) score += 0.1;
        return score;
    }
    
    private ArgumentQualityMetrics getDefaultMetrics() {
        return ArgumentQualityMetrics.builder()
            .logicalStrength(0.5)
            .evidenceQuality(0.5)
            .clarityScore(0.5)
            .relevanceScore(0.5)
            .originalityScore(0.5)
            .logicalFallacies(List.of())
            .argumentStructure(ArgumentStructure.builder()
                .structureScore(0.5)
                .build())
            .build();
    }
}