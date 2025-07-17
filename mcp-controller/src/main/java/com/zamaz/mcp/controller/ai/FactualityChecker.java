package com.zamaz.mcp.controller.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Checker for factual accuracy and evidence quality in debate responses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FactualityChecker {
    
    // Patterns for different types of evidence
    private static final Map<String, List<Pattern>> EVIDENCE_PATTERNS = Map.of(
        "statistics", Arrays.asList(
            Pattern.compile("\\d+%|\\d+\\.\\d+%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+\\s*(million|billion|thousand|percent)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\$\\d+(\\.\\d+)?(\\s*(million|billion|thousand))?", Pattern.CASE_INSENSITIVE)
        ),
        "citations", Arrays.asList(
            Pattern.compile("\\(\\d{4}\\)", Pattern.CASE_INSENSITIVE), // (2023)
            Pattern.compile("\\w+\\s+et al\\.?", Pattern.CASE_INSENSITIVE), // Smith et al.
            Pattern.compile("\\[\\d+\\]", Pattern.CASE_INSENSITIVE), // [1]
            Pattern.compile("\\bhttps?://\\S+", Pattern.CASE_INSENSITIVE) // URLs
        ),
        "studies", Arrays.asList(
            Pattern.compile("\\b(study|research|investigation|survey|analysis)\\s+(shows|indicates|suggests|demonstrates|reveals)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(according to|based on)\\s+(study|research|data|analysis)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(peer-reviewed|published|academic)\\s+(study|research|paper)", Pattern.CASE_INSENSITIVE)
        ),
        "authoritative_sources", Arrays.asList(
            Pattern.compile("\\b(WHO|CDC|FDA|NASA|UNESCO|UN|government|ministry|department)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(university|college|institute|foundation)\\s+(study|research)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(Harvard|MIT|Stanford|Oxford|Cambridge)\\b", Pattern.CASE_INSENSITIVE)
        )
    );
    
    // Reliability indicators for sources
    private static final Map<Pattern, Double> SOURCE_RELIABILITY = Map.of(
        Pattern.compile("\\b(peer-reviewed|academic|scientific|scholarly)\\b", Pattern.CASE_INSENSITIVE), 0.9,
        Pattern.compile("\\b(government|official|ministry|department)\\b", Pattern.CASE_INSENSITIVE), 0.8,
        Pattern.compile("\\b(university|institute|research center)\\b", Pattern.CASE_INSENSITIVE), 0.8,
        Pattern.compile("\\b(WHO|CDC|FDA|NASA|UNESCO)\\b", Pattern.CASE_INSENSITIVE), 0.9,
        Pattern.compile("\\b(news|newspaper|journalist|reporter)\\b", Pattern.CASE_INSENSITIVE), 0.6,
        Pattern.compile("\\b(blog|opinion|editorial|personal)\\b", Pattern.CASE_INSENSITIVE), 0.3
    );
    
    // Certainty language patterns
    private static final Map<Pattern, Double> CERTAINTY_PATTERNS = Map.of(
        Pattern.compile("\\b(definitely|certainly|absolutely|undoubtedly|without question)\\b", Pattern.CASE_INSENSITIVE), 1.0,
        Pattern.compile("\\b(clearly|obviously|evidently|unquestionably)\\b", Pattern.CASE_INSENSITIVE), 0.9,
        Pattern.compile("\\b(likely|probably|generally|typically|usually)\\b", Pattern.CASE_INSENSITIVE), 0.7,
        Pattern.compile("\\b(possibly|might|could|may|perhaps|potentially)\\b", Pattern.CASE_INSENSITIVE), 0.5,
        Pattern.compile("\\b(allegedly|reportedly|supposedly|claimed)\\b", Pattern.CASE_INSENSITIVE), 0.3
    );
    
    // Red flag patterns that suggest unreliable claims
    private static final List<Pattern> RED_FLAG_PATTERNS = Arrays.asList(
        Pattern.compile("\\b(secret|conspiracy|cover-up|hidden truth)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(miracle cure|instant solution|guaranteed|100% effective)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(everyone knows|it's obvious|common sense|no need to prove)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(fake news|mainstream media lies|propaganda)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(I heard|someone told me|my friend said|rumors suggest)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    // Logical fallacy patterns that affect factuality
    private static final Map<String, Pattern> FALLACY_PATTERNS = Map.of(
        "hasty_generalization", Pattern.compile("\\b(all|every|always|never|none|everyone|no one)\\s+\\w+\\s+(are|is|do|don't)", Pattern.CASE_INSENSITIVE),
        "false_cause", Pattern.compile("\\b(caused by|leads to|results in)\\b.*\\bbecause\\b", Pattern.CASE_INSENSITIVE),
        "appeal_to_popularity", Pattern.compile("\\b(everyone|most people|popular opinion|widely believed)\\b", Pattern.CASE_INSENSITIVE),
        "appeal_to_authority", Pattern.compile("\\b(expert says|authority claims)\\b(?!.*\\b(peer-reviewed|study|research)\\b)", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Check factual accuracy and evidence quality
     */
    public FactualityMetrics checkFactuality(String text) {
        try {
            return FactualityMetrics.builder()
                .factualAccuracy(assessFactualAccuracy(text))
                .sourceReliability(assessSourceReliability(text))
                .evidenceStrength(assessEvidenceStrength(text))
                .claimCertainty(assessClaimCertainty(text))
                .logicalSoundness(assessLogicalSoundness(text))
                .verifiability(assessVerifiability(text))
                .evidenceTypes(identifyEvidenceTypes(text))
                .redFlags(identifyRedFlags(text))
                .factualClaims(extractFactualClaims(text))
                .build();
        } catch (Exception e) {
            log.error("Error checking factuality: {}", text, e);
            return getDefaultFactualityMetrics();
        }
    }
    
    /**
     * Assess overall factual accuracy based on evidence and claims
     */
    private double assessFactualAccuracy(String text) {
        double score = 0.5; // Start neutral
        
        // Bonus for providing evidence
        score += assessEvidenceStrength(text) * 0.3;
        
        // Bonus for reliable sources
        score += assessSourceReliability(text) * 0.2;
        
        // Penalty for red flags
        int redFlags = identifyRedFlags(text).size();
        score -= Math.min(0.4, redFlags * 0.1);
        
        // Bonus for appropriate certainty language
        score += assessClaimCertainty(text) * 0.2;
        
        // Penalty for logical fallacies
        score -= countLogicalFallacies(text) * 0.05;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Assess reliability of cited sources
     */
    private double assessSourceReliability(String text) {
        double totalReliability = 0.0;
        int sourceCount = 0;
        
        for (Map.Entry<Pattern, Double> entry : SOURCE_RELIABILITY.entrySet()) {
            int matches = countMatches(entry.getKey(), text);
            if (matches > 0) {
                totalReliability += entry.getValue() * matches;
                sourceCount += matches;
            }
        }
        
        if (sourceCount == 0) {
            return 0.3; // No explicit sources = low reliability
        }
        
        return Math.min(1.0, totalReliability / sourceCount);
    }
    
    /**
     * Assess strength of evidence presented
     */
    private double assessEvidenceStrength(String text) {
        double score = 0.0;
        int evidenceCount = 0;
        
        // Different types of evidence have different weights
        Map<String, Double> evidenceWeights = Map.of(
            "statistics", 0.8,
            "citations", 0.9,
            "studies", 0.85,
            "authoritative_sources", 0.9
        );
        
        for (Map.Entry<String, List<Pattern>> entry : EVIDENCE_PATTERNS.entrySet()) {
            String evidenceType = entry.getKey();
            double weight = evidenceWeights.getOrDefault(evidenceType, 0.5);
            
            for (Pattern pattern : entry.getValue()) {
                int matches = countMatches(pattern, text);
                if (matches > 0) {
                    score += weight * Math.min(3, matches); // Cap at 3 matches per type
                    evidenceCount += matches;
                }
            }
        }
        
        if (evidenceCount == 0) {
            return 0.2; // No evidence = very low score
        }
        
        // Normalize by evidence count but give bonus for multiple types
        Set<String> evidenceTypesFound = identifyEvidenceTypes(text);
        double diversityBonus = Math.min(0.2, evidenceTypesFound.size() * 0.05);
        
        return Math.min(1.0, (score / evidenceCount) + diversityBonus);
    }
    
    /**
     * Assess appropriateness of certainty in claims
     */
    private double assessClaimCertainty(String text) {
        double totalCertainty = 0.0;
        int certaintyIndicators = 0;
        
        for (Map.Entry<Pattern, Double> entry : CERTAINTY_PATTERNS.entrySet()) {
            int matches = countMatches(entry.getKey(), text);
            if (matches > 0) {
                totalCertainty += entry.getValue() * matches;
                certaintyIndicators += matches;
            }
        }
        
        if (certaintyIndicators == 0) {
            return 0.6; // No explicit certainty = moderate score
        }
        
        double avgCertainty = totalCertainty / certaintyIndicators;
        
        // Adjust based on evidence strength
        double evidenceStrength = assessEvidenceStrength(text);
        
        // High certainty with low evidence = problematic
        if (avgCertainty > 0.8 && evidenceStrength < 0.4) {
            return 0.3; // Overconfident without evidence
        }
        
        // Appropriate certainty for evidence level
        if (Math.abs(avgCertainty - evidenceStrength) < 0.2) {
            return 0.8; // Well-calibrated
        }
        
        return avgCertainty * 0.6; // Partial credit for reasonable certainty
    }
    
    /**
     * Assess logical soundness of arguments
     */
    private double assessLogicalSoundness(String text) {
        double score = 0.8; // Start with high score, deduct for problems
        
        // Count logical fallacies
        int fallacyCount = countLogicalFallacies(text);
        score -= Math.min(0.5, fallacyCount * 0.1);
        
        // Check for proper logical structure
        if (hasProperLogicalStructure(text)) {
            score += 0.1;
        }
        
        // Check for contradictions
        if (hasInternalContradictions(text)) {
            score -= 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Assess how verifiable the claims are
     */
    private double assessVerifiability(String text) {
        double score = 0.3; // Base score
        
        // Bonus for specific, measurable claims
        if (hasSpecificClaims(text)) {
            score += 0.3;
        }
        
        // Bonus for citations and sources
        score += Math.min(0.3, countCitations(text) * 0.1);
        
        // Bonus for recent timeframes (more verifiable)
        if (hasRecentTimeframes(text)) {
            score += 0.1;
        }
        
        // Penalty for vague or untestable claims
        if (hasVagueClaims(text)) {
            score -= 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Identify types of evidence present
     */
    private Set<String> identifyEvidenceTypes(String text) {
        Set<String> evidenceTypes = new HashSet<>();
        
        for (Map.Entry<String, List<Pattern>> entry : EVIDENCE_PATTERNS.entrySet()) {
            String evidenceType = entry.getKey();
            
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(text).find()) {
                    evidenceTypes.add(evidenceType);
                    break; // Found this type, move to next
                }
            }
        }
        
        return evidenceTypes;
    }
    
    /**
     * Identify red flags that suggest unreliable information
     */
    private List<String> identifyRedFlags(String text) {
        List<String> redFlags = new ArrayList<>();
        
        for (int i = 0; i < RED_FLAG_PATTERNS.size(); i++) {
            Pattern pattern = RED_FLAG_PATTERNS.get(i);
            if (pattern.matcher(text).find()) {
                switch (i) {
                    case 0: redFlags.add("Conspiracy language"); break;
                    case 1: redFlags.add("Unrealistic claims"); break;
                    case 2: redFlags.add("Appeal to common sense"); break;
                    case 3: redFlags.add("Media distrust"); break;
                    case 4: redFlags.add("Hearsay evidence"); break;
                }
            }
        }
        
        return redFlags;
    }
    
    /**
     * Extract specific factual claims from text
     */
    private List<String> extractFactualClaims(String text) {
        List<String> claims = new ArrayList<>();
        
        // Find sentences with statistical claims
        String[] sentences = text.split("[.!?]+");
        for (String sentence : sentences) {
            sentence = sentence.trim();
            
            // Look for sentences with numbers, percentages, or strong claims
            if (Pattern.compile("\\d+%|\\d+\\.\\d+|\\$\\d+").matcher(sentence).find() ||
                Pattern.compile("\\b(increases?|decreases?|reduces?|improves?)\\b", Pattern.CASE_INSENSITIVE).matcher(sentence).find()) {
                
                if (sentence.length() > 20 && sentence.length() < 200) {
                    claims.add(sentence);
                }
            }
        }
        
        return claims;
    }
    
    // Helper methods
    
    private int countMatches(Pattern pattern, String text) {
        return (int) pattern.matcher(text).results().count();
    }
    
    private int countLogicalFallacies(String text) {
        return FALLACY_PATTERNS.values().stream()
            .mapToInt(pattern -> countMatches(pattern, text))
            .sum();
    }
    
    private boolean hasProperLogicalStructure(String text) {
        // Check for premise-conclusion structure
        boolean hasPremise = Pattern.compile("\\b(because|since|given that|due to)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
        boolean hasConclusion = Pattern.compile("\\b(therefore|thus|consequently|as a result)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
        
        return hasPremise && hasConclusion;
    }
    
    private boolean hasInternalContradictions(String text) {
        // Simplified check for obvious contradictions
        return Pattern.compile("\\b(yes.*no|true.*false|always.*never)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
    }
    
    private boolean hasSpecificClaims(String text) {
        return Pattern.compile("\\d+(\\.\\d+)?\\s*(percent|%|times|fold|million|billion)", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
    }
    
    private int countCitations(String text) {
        return EVIDENCE_PATTERNS.get("citations").stream()
            .mapToInt(pattern -> countMatches(pattern, text))
            .sum();
    }
    
    private boolean hasRecentTimeframes(String text) {
        return Pattern.compile("\\b(202[0-9]|2019|recently|latest|current|new study)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
    }
    
    private boolean hasVagueClaims(String text) {
        return Pattern.compile("\\b(many|some|often|sometimes|usually|generally|typically)\\b.*\\b(better|worse|more|less)\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).find();
    }
    
    private FactualityMetrics getDefaultFactualityMetrics() {
        return FactualityMetrics.builder()
            .factualAccuracy(0.5)
            .sourceReliability(0.3)
            .evidenceStrength(0.3)
            .claimCertainty(0.5)
            .logicalSoundness(0.7)
            .verifiability(0.3)
            .evidenceTypes(Set.of())
            .redFlags(List.of())
            .factualClaims(List.of())
            .build();
    }
}