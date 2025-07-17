package com.zamaz.mcp.controller.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Analyzer for sentiment and emotional tone in debate responses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalyzer {
    
    // Positive sentiment indicators
    private static final Map<Pattern, Double> POSITIVE_PATTERNS = Map.of(
        Pattern.compile("\\b(excellent|outstanding|brilliant|effective|successful|beneficial|positive|good|great|wonderful|amazing)\\b", Pattern.CASE_INSENSITIVE), 0.8,
        Pattern.compile("\\b(support|agree|endorse|favor|approve|appreciate|commend|praise)\\b", Pattern.CASE_INSENSITIVE), 0.6,
        Pattern.compile("\\b(improvement|progress|success|achievement|solution|opportunity)\\b", Pattern.CASE_INSENSITIVE), 0.5
    );
    
    // Negative sentiment indicators  
    private static final Map<Pattern, Double> NEGATIVE_PATTERNS = Map.of(
        Pattern.compile("\\b(terrible|awful|horrible|disastrous|failed|wrong|bad|poor|weak|flawed)\\b", Pattern.CASE_INSENSITIVE), -0.8,
        Pattern.compile("\\b(oppose|disagree|reject|deny|refuse|condemn|criticize|attack)\\b", Pattern.CASE_INSENSITIVE), -0.6,
        Pattern.compile("\\b(problem|issue|concern|threat|risk|danger|failure|mistake)\\b", Pattern.CASE_INSENSITIVE), -0.4
    );
    
    // Emotional tone indicators
    private static final Map<String, Pattern> EMOTION_PATTERNS = Map.of(
        "anger", Pattern.compile("\\b(angry|furious|outraged|irritated|frustrated|annoyed)\\b", Pattern.CASE_INSENSITIVE),
        "fear", Pattern.compile("\\b(afraid|scared|worried|concerned|anxious|nervous)\\b", Pattern.CASE_INSENSITIVE),
        "joy", Pattern.compile("\\b(happy|excited|pleased|delighted|thrilled|optimistic)\\b", Pattern.CASE_INSENSITIVE),
        "sadness", Pattern.compile("\\b(sad|disappointed|discouraged|hopeless|depressed)\\b", Pattern.CASE_INSENSITIVE),
        "surprise", Pattern.compile("\\b(surprised|shocked|amazed|astonished|unexpected)\\b", Pattern.CASE_INSENSITIVE),
        "confidence", Pattern.compile("\\b(confident|certain|sure|convinced|determined)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    // Toxicity indicators
    private static final List<Pattern> TOXICITY_PATTERNS = Arrays.asList(
        Pattern.compile("\\b(stupid|idiot|moron|fool|ridiculous|absurd)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\byou (are|'re) (wrong|stupid|an idiot)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(shut up|get lost|go away)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(hate|despise|loathe)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    // Professionalism indicators
    private static final List<Pattern> PROFESSIONAL_PATTERNS = Arrays.asList(
        Pattern.compile("\\b(research shows|studies indicate|data suggests|evidence demonstrates)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(respectfully|humbly|politely|courteously)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(thank you|please|may I suggest|I would argue)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(furthermore|moreover|additionally|in conclusion)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Analyze sentiment and emotional tone of text
     */
    public SentimentMetrics analyzeSentiment(String text) {
        try {
            return SentimentMetrics.builder()
                .sentiment(calculateSentimentScore(text))
                .confidence(calculateConfidence(text))
                .emotions(analyzeEmotions(text))
                .toxicityScore(calculateToxicityScore(text))
                .professionalismScore(calculateProfessionalismScore(text))
                .polarityScore(calculatePolarityScore(text))
                .subjectivityScore(calculateSubjectivityScore(text))
                .build();
        } catch (Exception e) {
            log.error("Error analyzing sentiment: {}", text, e);
            return getDefaultSentimentMetrics();
        }
    }
    
    /**
     * Calculate overall sentiment score (-1 to 1)
     */
    private double calculateSentimentScore(String text) {
        double score = 0.0;
        int totalMatches = 0;
        
        // Count positive patterns
        for (Map.Entry<Pattern, Double> entry : POSITIVE_PATTERNS.entrySet()) {
            long matches = countMatches(entry.getKey(), text);
            score += matches * entry.getValue();
            totalMatches += matches;
        }
        
        // Count negative patterns  
        for (Map.Entry<Pattern, Double> entry : NEGATIVE_PATTERNS.entrySet()) {
            long matches = countMatches(entry.getKey(), text);
            score += matches * entry.getValue();
            totalMatches += matches;
        }
        
        // Normalize based on text length and number of matches
        if (totalMatches > 0) {
            score = score / Math.max(1, totalMatches);
        }
        
        return Math.max(-1.0, Math.min(1.0, score));
    }
    
    /**
     * Calculate confidence in sentiment analysis
     */
    private double calculateConfidence(String text) {
        int strongIndicators = 0;
        int weakIndicators = 0;
        
        // Count strong sentiment indicators
        for (Map.Entry<Pattern, Double> entry : POSITIVE_PATTERNS.entrySet()) {
            if (Math.abs(entry.getValue()) >= 0.7) {
                strongIndicators += countMatches(entry.getKey(), text);
            } else {
                weakIndicators += countMatches(entry.getKey(), text);
            }
        }
        
        for (Map.Entry<Pattern, Double> entry : NEGATIVE_PATTERNS.entrySet()) {
            if (Math.abs(entry.getValue()) >= 0.7) {
                strongIndicators += countMatches(entry.getKey(), text);
            } else {
                weakIndicators += countMatches(entry.getKey(), text);
            }
        }
        
        double confidence = (strongIndicators * 0.8 + weakIndicators * 0.4) / Math.max(1, text.split("\\s+").length / 10.0);
        return Math.min(1.0, confidence);
    }
    
    /**
     * Analyze emotional content
     */
    private Map<String, Double> analyzeEmotions(String text) {
        Map<String, Double> emotions = new HashMap<>();
        
        for (Map.Entry<String, Pattern> entry : EMOTION_PATTERNS.entrySet()) {
            long matches = countMatches(entry.getValue(), text);
            double intensity = Math.min(1.0, matches / 3.0); // Max 3 matches = full intensity
            emotions.put(entry.getKey(), intensity);
        }
        
        return emotions;
    }
    
    /**
     * Calculate toxicity score (0 to 1, higher = more toxic)
     */
    private double calculateToxicityScore(String text) {
        double toxicityScore = 0.0;
        
        for (Pattern pattern : TOXICITY_PATTERNS) {
            long matches = countMatches(pattern, text);
            toxicityScore += matches * 0.25; // Each match adds 0.25 to toxicity
        }
        
        // Check for ALL CAPS (indicates shouting)
        long capsWords = Arrays.stream(text.split("\\s+"))
            .mapToLong(word -> word.length() > 3 && word.equals(word.toUpperCase()) ? 1 : 0)
            .sum();
        
        if (capsWords > 0) {
            toxicityScore += Math.min(0.3, capsWords * 0.1);
        }
        
        // Check for excessive punctuation (indicates aggression)
        long excessivePunctuation = Pattern.compile("[!]{2,}|[?]{2,}")
            .matcher(text)
            .results()
            .count();
        
        toxicityScore += Math.min(0.2, excessivePunctuation * 0.1);
        
        return Math.min(1.0, toxicityScore);
    }
    
    /**
     * Calculate professionalism score (0 to 1)
     */
    private double calculateProfessionalismScore(String text) {
        double professionalismScore = 0.5; // Base score
        
        // Add points for professional language
        for (Pattern pattern : PROFESSIONAL_PATTERNS) {
            long matches = countMatches(pattern, text);
            professionalismScore += Math.min(0.3, matches * 0.1);
        }
        
        // Check for proper grammar indicators
        if (hasProperCapitalization(text)) {
            professionalismScore += 0.1;
        }
        
        if (hasProperPunctuation(text)) {
            professionalismScore += 0.1;
        }
        
        // Subtract points for unprofessional elements
        professionalismScore -= calculateToxicityScore(text) * 0.5;
        
        // Check for informal language
        if (containsInformalLanguage(text)) {
            professionalismScore -= 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, professionalismScore));
    }
    
    /**
     * Calculate polarity score (how extreme the sentiment is)
     */
    private double calculatePolarityScore(String text) {
        double sentimentScore = calculateSentimentScore(text);
        return Math.abs(sentimentScore); // Polarity is absolute value of sentiment
    }
    
    /**
     * Calculate subjectivity score (0 = objective, 1 = subjective)
     */
    private double calculateSubjectivityScore(String text) {
        double subjectivityScore = 0.5; // Base score
        
        // Subjective indicators
        List<Pattern> subjectivePatterns = Arrays.asList(
            Pattern.compile("\\b(I think|I believe|I feel|in my opinion|personally|I would say)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(seems|appears|might|could|probably|possibly)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(beautiful|ugly|amazing|terrible|wonderful|awful)\\b", Pattern.CASE_INSENSITIVE)
        );
        
        for (Pattern pattern : subjectivePatterns) {
            long matches = countMatches(pattern, text);
            subjectivityScore += Math.min(0.3, matches * 0.1);
        }
        
        // Objective indicators
        List<Pattern> objectivePatterns = Arrays.asList(
            Pattern.compile("\\b(data shows|research indicates|according to|statistics reveal)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(fact|evidence|study|research|analysis)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+%|\\d+\\.\\d+|\\$\\d+", Pattern.CASE_INSENSITIVE)
        );
        
        for (Pattern pattern : objectivePatterns) {
            long matches = countMatches(pattern, text);
            subjectivityScore -= Math.min(0.2, matches * 0.05);
        }
        
        return Math.max(0.0, Math.min(1.0, subjectivityScore));
    }
    
    // Helper methods
    
    private long countMatches(Pattern pattern, String text) {
        return pattern.matcher(text).results().count();
    }
    
    private boolean hasProperCapitalization(String text) {
        String[] sentences = text.split("[.!?]+");
        return Arrays.stream(sentences)
            .allMatch(sentence -> {
                String trimmed = sentence.trim();
                return trimmed.isEmpty() || Character.isUpperCase(trimmed.charAt(0));
            });
    }
    
    private boolean hasProperPunctuation(String text) {
        return text.matches(".*[.!?]\\s*$"); // Ends with proper punctuation
    }
    
    private boolean containsInformalLanguage(String text) {
        List<Pattern> informalPatterns = Arrays.asList(
            Pattern.compile("\\b(gonna|wanna|kinda|sorta|yeah|nope|ok|lol|omg)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(dude|bro|guys|folks)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("&|\\band\\s*&|\\bu\\b", Pattern.CASE_INSENSITIVE) // Text speak
        );
        
        return informalPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(text).find());
    }
    
    private SentimentMetrics getDefaultSentimentMetrics() {
        return SentimentMetrics.builder()
            .sentiment(0.0)
            .confidence(0.0)
            .emotions(Map.of())
            .toxicityScore(0.0)
            .professionalismScore(0.5)
            .polarityScore(0.0)
            .subjectivityScore(0.5)
            .build();
    }
}