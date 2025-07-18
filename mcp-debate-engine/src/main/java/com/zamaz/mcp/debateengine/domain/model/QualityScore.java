package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;

/**
 * Value object representing quality scores for responses.
 */
public record QualityScore(
    double overall,
    double sentiment,
    double coherence,
    double factuality
) implements ValueObject {
    
    public QualityScore {
        validateScore(overall, "Overall score");
        validateScore(coherence, "Coherence score");
        validateScore(factuality, "Factuality score");
        
        // Sentiment can be negative
        if (sentiment < -1.0 || sentiment > 1.0) {
            throw new IllegalArgumentException("Sentiment score must be between -1.0 and 1.0");
        }
    }
    
    private static void validateScore(double score, String name) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Create quality score.
     */
    public static QualityScore of(
            double overall,
            double sentiment,
            double coherence,
            double factuality) {
        return new QualityScore(overall, sentiment, coherence, factuality);
    }
    
    /**
     * Create empty quality score.
     */
    public static QualityScore empty() {
        return new QualityScore(0.0, 0.0, 0.0, 0.0);
    }
    
    /**
     * Check if scores have been calculated.
     */
    public boolean isCalculated() {
        return overall > 0.0;
    }
    
    /**
     * Get average of all positive scores.
     */
    public double getAverage() {
        return (overall + Math.abs(sentiment) + coherence + factuality) / 4.0;
    }
    
    /**
     * Get sentiment as a category.
     */
    public SentimentCategory getSentimentCategory() {
        if (sentiment < -0.3) return SentimentCategory.NEGATIVE;
        if (sentiment > 0.3) return SentimentCategory.POSITIVE;
        return SentimentCategory.NEUTRAL;
    }
    
    /**
     * Sentiment categories.
     */
    public enum SentimentCategory {
        POSITIVE("Positive sentiment"),
        NEUTRAL("Neutral sentiment"),
        NEGATIVE("Negative sentiment");
        
        private final String description;
        
        SentimentCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}