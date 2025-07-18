package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing the quality metrics of a debate argument.
 */
public record ArgumentQuality(
    BigDecimal logicalStrength,
    BigDecimal evidenceQuality,
    BigDecimal clarity,
    BigDecimal relevance,
    BigDecimal originality
) implements ValueObject {
    
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(10.0);
    private static final int SCALE = 2;
    
    public ArgumentQuality {
        Objects.requireNonNull(logicalStrength, "Logical strength cannot be null");
        Objects.requireNonNull(evidenceQuality, "Evidence quality cannot be null");
        Objects.requireNonNull(clarity, "Clarity cannot be null");
        Objects.requireNonNull(relevance, "Relevance cannot be null");
        Objects.requireNonNull(originality, "Originality cannot be null");
        
        validateScore(logicalStrength, "Logical strength");
        validateScore(evidenceQuality, "Evidence quality");
        validateScore(clarity, "Clarity");
        validateScore(relevance, "Relevance");
        validateScore(originality, "Originality");
    }
    
    public static ArgumentQuality of(
            double logicalStrength,
            double evidenceQuality,
            double clarity,
            double relevance,
            double originality
    ) {
        return new ArgumentQuality(
            BigDecimal.valueOf(logicalStrength).setScale(SCALE, RoundingMode.HALF_UP),
            BigDecimal.valueOf(evidenceQuality).setScale(SCALE, RoundingMode.HALF_UP),
            BigDecimal.valueOf(clarity).setScale(SCALE, RoundingMode.HALF_UP),
            BigDecimal.valueOf(relevance).setScale(SCALE, RoundingMode.HALF_UP),
            BigDecimal.valueOf(originality).setScale(SCALE, RoundingMode.HALF_UP)
        );
    }
    
    public static ArgumentQuality excellent() {
        return of(9.0, 9.0, 9.0, 9.0, 8.0);
    }
    
    public static ArgumentQuality good() {
        return of(7.0, 7.0, 7.5, 8.0, 6.0);
    }
    
    public static ArgumentQuality average() {
        return of(5.0, 5.0, 5.5, 6.0, 4.0);
    }
    
    public static ArgumentQuality poor() {
        return of(3.0, 3.0, 4.0, 4.0, 2.0);
    }
    
    public static ArgumentQuality unknown() {
        return of(0.0, 0.0, 0.0, 0.0, 0.0);
    }
    
    /**
     * Calculate overall quality score as weighted average.
     */
    public BigDecimal overallScore() {
        // Weighted calculation: logic=30%, evidence=25%, clarity=20%, relevance=15%, originality=10%
        BigDecimal weighted = logicalStrength.multiply(BigDecimal.valueOf(0.30))
            .add(evidenceQuality.multiply(BigDecimal.valueOf(0.25)))
            .add(clarity.multiply(BigDecimal.valueOf(0.20)))
            .add(relevance.multiply(BigDecimal.valueOf(0.15)))
            .add(originality.multiply(BigDecimal.valueOf(0.10)));
        
        return weighted.setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Get quality category based on overall score.
     */
    public QualityCategory getCategory() {
        BigDecimal score = overallScore();
        if (score.compareTo(BigDecimal.valueOf(8.0)) >= 0) {
            return QualityCategory.EXCELLENT;
        } else if (score.compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            return QualityCategory.GOOD;
        } else if (score.compareTo(BigDecimal.valueOf(4.0)) >= 0) {
            return QualityCategory.AVERAGE;
        } else if (score.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            return QualityCategory.POOR;
        } else {
            return QualityCategory.UNKNOWN;
        }
    }
    
    /**
     * Compare this quality with another.
     */
    public int compareTo(ArgumentQuality other) {
        Objects.requireNonNull(other, "Other argument quality cannot be null");
        return this.overallScore().compareTo(other.overallScore());
    }
    
    public boolean isBetterThan(ArgumentQuality other) {
        return compareTo(other) > 0;
    }
    
    public boolean isWorseThan(ArgumentQuality other) {
        return compareTo(other) < 0;
    }
    
    /**
     * Create a new quality with adjusted scores.
     */
    public ArgumentQuality withLogicalStrength(double newScore) {
        return new ArgumentQuality(
            BigDecimal.valueOf(newScore).setScale(SCALE, RoundingMode.HALF_UP),
            evidenceQuality, clarity, relevance, originality
        );
    }
    
    public ArgumentQuality withEvidenceQuality(double newScore) {
        return new ArgumentQuality(
            logicalStrength,
            BigDecimal.valueOf(newScore).setScale(SCALE, RoundingMode.HALF_UP),
            clarity, relevance, originality
        );
    }
    
    public ArgumentQuality withClarity(double newScore) {
        return new ArgumentQuality(
            logicalStrength, evidenceQuality,
            BigDecimal.valueOf(newScore).setScale(SCALE, RoundingMode.HALF_UP),
            relevance, originality
        );
    }
    
    private static void validateScore(BigDecimal score, String scoreName) {
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException(
                scoreName + " must be between " + MIN_SCORE + " and " + MAX_SCORE
            );
        }
    }
    
    public enum QualityCategory {
        EXCELLENT("Excellent"),
        GOOD("Good"),
        AVERAGE("Average"),
        POOR("Poor"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        QualityCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Override
    public String toString() {
        return String.format("ArgumentQuality{overall=%.2f, logic=%.2f, evidence=%.2f, clarity=%.2f, relevance=%.2f, originality=%.2f}",
            overallScore(), logicalStrength, evidenceQuality, clarity, relevance, originality);
    }
}