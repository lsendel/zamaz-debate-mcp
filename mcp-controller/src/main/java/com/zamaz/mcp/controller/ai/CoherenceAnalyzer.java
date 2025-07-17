package com.zamaz.mcp.controller.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyzer for logical flow and structural coherence in debate responses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoherenceAnalyzer {
    
    // Transition words and phrases for logical flow
    private static final Map<String, List<Pattern>> TRANSITION_PATTERNS = Map.of(
        "addition", Arrays.asList(
            Pattern.compile("\\b(furthermore|moreover|additionally|also|besides|in addition)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(similarly|likewise|equally important)\\b", Pattern.CASE_INSENSITIVE)
        ),
        "contrast", Arrays.asList(
            Pattern.compile("\\b(however|nevertheless|nonetheless|on the other hand)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(although|though|despite|in contrast|conversely)\\b", Pattern.CASE_INSENSITIVE)
        ),
        "causation", Arrays.asList(
            Pattern.compile("\\b(because|since|as a result|therefore|thus|consequently)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(due to|owing to|leads to|causes|results in)\\b", Pattern.CASE_INSENSITIVE)
        ),
        "sequence", Arrays.asList(
            Pattern.compile("\\b(first|second|third|finally|lastly|next|then)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(initially|subsequently|meanwhile|eventually)\\b", Pattern.CASE_INSENSITIVE)
        ),
        "emphasis", Arrays.asList(
            Pattern.compile("\\b(indeed|certainly|undoubtedly|clearly|obviously)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(most importantly|above all|especially|particularly)\\b", Pattern.CASE_INSENSITIVE)
        ),
        "conclusion", Arrays.asList(
            Pattern.compile("\\b(in conclusion|to summarize|in summary|overall)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(ultimately|in the end|to conclude)\\b", Pattern.CASE_INSENSITIVE)
        )
    );
    
    // Structural coherence indicators
    private static final List<Pattern> STRUCTURE_PATTERNS = Arrays.asList(
        Pattern.compile("\\b(let me explain|here's why|the reason is)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(for example|for instance|consider|imagine)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(my point is|what I mean is|in other words)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(this shows|this demonstrates|this proves)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    // Coherence disruption indicators
    private static final List<Pattern> DISRUPTION_PATTERNS = Arrays.asList(
        Pattern.compile("\\b(by the way|speaking of|off topic|unrelated)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(wait|hold on|let me think|um|uh)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(anyway|whatever|never mind|forget that)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Analyze coherence and logical flow of text
     */
    public CoherenceMetrics analyzeCoherence(String text) {
        try {
            return CoherenceMetrics.builder()
                .logicalFlow(analyzeLogicalFlow(text))
                .structuralCoherence(analyzeStructuralCoherence(text))
                .transitionQuality(analyzeTransitionQuality(text))
                .topicConsistency(analyzeTopicConsistency(text))
                .argumentProgression(analyzeArgumentProgression(text))
                .cohesionScore(analyzeCohesion(text))
                .clarityScore(analyzeClarity(text))
                .build();
        } catch (Exception e) {
            log.error("Error analyzing coherence: {}", text, e);
            return getDefaultCoherenceMetrics();
        }
    }
    
    /**
     * Analyze logical flow between ideas
     */
    private double analyzeLogicalFlow(String text) {
        double score = 0.5; // Base score
        
        String[] sentences = splitIntoSentences(text);
        if (sentences.length < 2) {
            return score; // Single sentence, neutral score
        }
        
        // Count logical transitions between sentences
        int logicalTransitions = 0;
        int totalTransitions = sentences.length - 1;
        
        for (int i = 1; i < sentences.length; i++) {
            String currentSentence = sentences[i];
            String previousSentence = sentences[i - 1];
            
            if (hasLogicalConnection(previousSentence, currentSentence)) {
                logicalTransitions++;
            }
        }
        
        if (totalTransitions > 0) {
            double transitionRatio = (double) logicalTransitions / totalTransitions;
            score += (transitionRatio - 0.5) * 0.4; // Adjust from base score
        }
        
        // Bonus for explicit transition words
        score += Math.min(0.2, countAllTransitions(text) * 0.05);
        
        // Penalty for disruptions
        score -= Math.min(0.3, countDisruptions(text) * 0.1);
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze structural coherence
     */
    private double analyzeStructuralCoherence(String text) {
        double score = 0.5; // Base score
        
        // Check for clear introduction
        if (hasIntroduction(text)) {
            score += 0.15;
        }
        
        // Check for clear conclusion
        if (hasConclusion(text)) {
            score += 0.15;
        }
        
        // Check for organized structure (lists, numbered points)
        if (hasOrganizedStructure(text)) {
            score += 0.1;
        }
        
        // Check for topic sentences
        if (hasTopicSentences(text)) {
            score += 0.1;
        }
        
        // Check for supporting details
        if (hasSupportingDetails(text)) {
            score += 0.1;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze quality of transitions
     */
    private double analyzeTransitionQuality(String text) {
        double score = 0.3; // Base score
        
        Map<String, Integer> transitionCounts = new HashMap<>();
        
        // Count different types of transitions
        for (Map.Entry<String, List<Pattern>> entry : TRANSITION_PATTERNS.entrySet()) {
            String transitionType = entry.getKey();
            int count = 0;
            
            for (Pattern pattern : entry.getValue()) {
                count += countMatches(pattern, text);
            }
            
            transitionCounts.put(transitionType, count);
        }
        
        // Variety bonus (using different types of transitions)
        long transitionTypes = transitionCounts.values().stream()
            .mapToLong(count -> count > 0 ? 1 : 0)
            .sum();
        
        score += Math.min(0.3, transitionTypes * 0.05);
        
        // Total transitions bonus
        int totalTransitions = transitionCounts.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
        
        score += Math.min(0.4, totalTransitions * 0.1);
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze topic consistency throughout text
     */
    private double analyzeTopicConsistency(String text) {
        double score = 0.7; // Assume generally consistent unless proven otherwise
        
        String[] sentences = splitIntoSentences(text);
        if (sentences.length < 3) {
            return score; // Too short to assess consistency
        }
        
        // Check for topic drift indicators
        for (Pattern pattern : DISRUPTION_PATTERNS) {
            int disruptions = countMatches(pattern, text);
            score -= Math.min(0.3, disruptions * 0.1);
        }
        
        // Check for repetitive topic reinforcement
        if (hasTopicReinforcement(text)) {
            score += 0.1;
        }
        
        // Check for off-topic tangents
        if (hasOffTopicContent(text)) {
            score -= 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze argument progression and development
     */
    private double analyzeArgumentProgression(String text) {
        double score = 0.5; // Base score
        
        // Check for claim-evidence-reasoning structure
        if (hasClaimEvidenceReasoning(text)) {
            score += 0.2;
        }
        
        // Check for building complexity
        if (hasBuildingComplexity(text)) {
            score += 0.15;
        }
        
        // Check for counterargument consideration
        if (hasCounterargumentConsideration(text)) {
            score += 0.15;
        }
        
        // Check for synthesis and conclusion
        if (hasSynthesis(text)) {
            score += 0.1;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze lexical and semantic cohesion
     */
    private double analyzeCohesion(String text) {
        double score = 0.5; // Base score
        
        // Check for pronoun reference consistency
        score += analyzePronounConsistency(text) * 0.2;
        
        // Check for keyword repetition and variation
        score += analyzeKeywordCohesion(text) * 0.3;
        
        // Check for semantic field consistency
        score += analyzeSemanticConsistency(text) * 0.3;
        
        // Check for temporal consistency
        score += analyzeTemporalConsistency(text) * 0.2;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze overall clarity of expression
     */
    private double analyzeClarity(String text) {
        double score = 0.5; // Base score
        
        // Check sentence length distribution
        double avgSentenceLength = calculateAverageSentenceLength(text);
        if (avgSentenceLength >= 10 && avgSentenceLength <= 25) {
            score += 0.2; // Optimal range
        } else if (avgSentenceLength > 40) {
            score -= 0.3; // Too long, reduces clarity
        }
        
        // Check for clear subject-verb relationships
        if (hasClearSubjectVerb(text)) {
            score += 0.1;
        }
        
        // Check for active vs passive voice
        if (hasActiveVoice(text)) {
            score += 0.1;
        }
        
        // Check for concrete vs abstract language
        if (hasConcreteLanguage(text)) {
            score += 0.1;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    // Helper methods
    
    private String[] splitIntoSentences(String text) {
        return text.split("[.!?]+");
    }
    
    private boolean hasLogicalConnection(String sentence1, String sentence2) {
        // Check if sentence2 starts with a transition word or refers back to sentence1
        for (List<Pattern> patterns : TRANSITION_PATTERNS.values()) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(sentence2.trim()).find()) {
                    return true;
                }
            }
        }
        
        // Check for pronoun references
        return Pattern.compile("\\b(this|that|these|those|it|they)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(sentence2.trim()).find();
    }
    
    private int countAllTransitions(String text) {
        return TRANSITION_PATTERNS.values().stream()
            .flatMap(List::stream)
            .mapToInt(pattern -> countMatches(pattern, text))
            .sum();
    }
    
    private int countDisruptions(String text) {
        return DISRUPTION_PATTERNS.stream()
            .mapToInt(pattern -> countMatches(pattern, text))
            .sum();
    }
    
    private int countMatches(Pattern pattern, String text) {
        return (int) pattern.matcher(text).results().count();
    }
    
    private boolean hasIntroduction(String text) {
        String firstSentence = splitIntoSentences(text)[0].trim();
        return Pattern.compile("\\b(first|initially|to begin|let me start|I will argue)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(firstSentence).find();
    }
    
    private boolean hasConclusion(String text) {
        String[] sentences = splitIntoSentences(text);
        if (sentences.length == 0) return false;
        
        String lastSentence = sentences[sentences.length - 1].trim();
        return TRANSITION_PATTERNS.get("conclusion").stream()
            .anyMatch(pattern -> pattern.matcher(lastSentence).find());
    }
    
    private boolean hasOrganizedStructure(String text) {
        return Pattern.compile("\\b(first|second|third|1\\.|2\\.|3\\.|•|\\*)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
    }
    
    private boolean hasTopicSentences(String text) {
        String[] sentences = splitIntoSentences(text);
        return sentences.length > 1 && sentences[0].trim().length() > 30;
    }
    
    private boolean hasSupportingDetails(String text) {
        return STRUCTURE_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(text).find());
    }
    
    private boolean hasTopicReinforcement(String text) {
        // This would ideally analyze keyword repetition
        // Simplified: check for repeating key concepts
        return true; // Placeholder implementation
    }
    
    private boolean hasOffTopicContent(String text) {
        return DISRUPTION_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(text).find());
    }
    
    private boolean hasClaimEvidenceReasoning(String text) {
        boolean hasClaim = Pattern.compile("\\b(I argue|I claim|my position)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
        boolean hasEvidence = Pattern.compile("\\b(evidence|data|research|studies)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
        boolean hasReasoning = Pattern.compile("\\b(because|therefore|this shows)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
        
        return hasClaim && hasEvidence && hasReasoning;
    }
    
    private boolean hasBuildingComplexity(String text) {
        String[] sentences = splitIntoSentences(text);
        if (sentences.length < 3) return false;
        
        // Check if later sentences are generally longer (indicating development)
        double firstHalfAvg = Arrays.stream(sentences, 0, sentences.length / 2)
            .mapToInt(String::length)
            .average()
            .orElse(0.0);
        
        double secondHalfAvg = Arrays.stream(sentences, sentences.length / 2, sentences.length)
            .mapToInt(String::length)
            .average()
            .orElse(0.0);
        
        return secondHalfAvg > firstHalfAvg * 1.1;
    }
    
    private boolean hasCounterargumentConsideration(String text) {
        return Pattern.compile("\\b(however|although|some might argue|critics claim|on the other hand)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
    }
    
    private boolean hasSynthesis(String text) {
        return TRANSITION_PATTERNS.get("conclusion").stream()
            .anyMatch(pattern -> pattern.matcher(text).find());
    }
    
    private double analyzePronounConsistency(String text) {
        // Simplified: check for consistent use of pronouns
        return 0.5; // Placeholder
    }
    
    private double analyzeKeywordCohesion(String text) {
        // Simplified: would analyze keyword repetition and semantic variation
        return 0.5; // Placeholder
    }
    
    private double analyzeSemanticConsistency(String text) {
        // Simplified: would check for consistent semantic fields
        return 0.5; // Placeholder
    }
    
    private double analyzeTemporalConsistency(String text) {
        // Check for consistent tense usage
        return 0.5; // Placeholder
    }
    
    private double calculateAverageSentenceLength(String text) {
        String[] sentences = splitIntoSentences(text);
        return Arrays.stream(sentences)
            .mapToInt(s -> s.trim().split("\\s+").length)
            .average()
            .orElse(0.0);
    }
    
    private boolean hasClearSubjectVerb(String text) {
        // Simplified: assume most text has clear subject-verb relationships
        return true; // Placeholder
    }
    
    private boolean hasActiveVoice(String text) {
        // Count passive voice indicators vs total verbs
        long passiveIndicators = Pattern.compile("\\b(was|were|been|being)\\s+\\w+ed\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).results().count();
        
        String[] words = text.split("\\s+");
        return passiveIndicators < words.length * 0.1; // Less than 10% passive
    }
    
    private boolean hasConcreteLanguage(String text) {
        // Check for concrete nouns vs abstract concepts
        long concreteWords = Pattern.compile("\\b(people|person|data|study|example|case|number|percent)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).results().count();
        
        long abstractWords = Pattern.compile("\\b(concept|idea|notion|theory|principle|belief)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).results().count();
        
        return concreteWords >= abstractWords;
    }
    
    private CoherenceMetrics getDefaultCoherenceMetrics() {
        return CoherenceMetrics.builder()
            .logicalFlow(0.5)
            .structuralCoherence(0.5)
            .transitionQuality(0.5)
            .topicConsistency(0.5)
            .argumentProgression(0.5)
            .cohesionScore(0.5)
            .clarityScore(0.5)
            .build();
    }
}