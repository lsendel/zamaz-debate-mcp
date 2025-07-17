package com.zamaz.mcp.pattern.model;

import lombok.Builder;
import lombok.Data;

/**
 * Code metrics for pattern analysis.
 */
@Data
@Builder
public class CodeMetrics {
    
    /**
     * Lines of code
     */
    private final int linesOfCode;
    
    /**
     * Non-comment lines of code
     */
    private final int nonCommentLinesOfCode;
    
    /**
     * Comment lines
     */
    private final int commentLines;
    
    /**
     * Blank lines
     */
    private final int blankLines;
    
    /**
     * Cyclomatic complexity
     */
    private final int cyclomaticComplexity;
    
    /**
     * Number of classes
     */
    private final int classCount;
    
    /**
     * Number of methods
     */
    private final int methodCount;
    
    /**
     * Number of fields
     */
    private final int fieldCount;
    
    /**
     * Number of imports
     */
    private final int importCount;
    
    /**
     * Number of annotations
     */
    private final int annotationCount;
    
    /**
     * Halstead metrics
     */
    private final HalsteadMetrics halsteadMetrics;
    
    /**
     * Maintainability index
     */
    private final double maintainabilityIndex;
    
    /**
     * Coupling between objects
     */
    private final int couplingBetweenObjects;
    
    /**
     * Depth of inheritance tree
     */
    private final int depthOfInheritanceTree;
    
    /**
     * Number of children (subclasses)
     */
    private final int numberOfChildren;
    
    /**
     * Lack of cohesion in methods
     */
    private final double lackOfCohesionInMethods;
    
    /**
     * Weighted methods per class
     */
    private final int weightedMethodsPerClass;
    
    /**
     * Response for class
     */
    private final int responseForClass;
    
    /**
     * Calculate comment density
     */
    public double getCommentDensity() {
        return linesOfCode > 0 ? (double) commentLines / linesOfCode : 0.0;
    }
    
    /**
     * Calculate code density (non-comment lines vs total lines)
     */
    public double getCodeDensity() {
        return linesOfCode > 0 ? (double) nonCommentLinesOfCode / linesOfCode : 0.0;
    }
    
    /**
     * Calculate average methods per class
     */
    public double getAverageMethodsPerClass() {
        return classCount > 0 ? (double) methodCount / classCount : 0.0;
    }
    
    /**
     * Calculate average fields per class
     */
    public double getAverageFieldsPerClass() {
        return classCount > 0 ? (double) fieldCount / classCount : 0.0;
    }
    
    /**
     * Calculate average complexity per method
     */
    public double getAverageComplexityPerMethod() {
        return methodCount > 0 ? (double) cyclomaticComplexity / methodCount : 0.0;
    }
    
    /**
     * Check if the code is well-documented
     */
    public boolean isWellDocumented() {
        return getCommentDensity() >= 0.15; // At least 15% comments
    }
    
    /**
     * Check if the code is too complex
     */
    public boolean isTooComplex() {
        return cyclomaticComplexity > 15 || getAverageComplexityPerMethod() > 8;
    }
    
    /**
     * Check if the file is too large
     */
    public boolean isTooLarge() {
        return linesOfCode > 500;
    }
    
    /**
     * Check if the class has too many methods
     */
    public boolean hasTooManyMethods() {
        return getAverageMethodsPerClass() > 20;
    }
    
    /**
     * Check if the class has too many fields
     */
    public boolean hasTooManyFields() {
        return getAverageFieldsPerClass() > 10;
    }
    
    /**
     * Check if the coupling is too high
     */
    public boolean hasHighCoupling() {
        return couplingBetweenObjects > 10;
    }
    
    /**
     * Check if the inheritance tree is too deep
     */
    public boolean hasDeepInheritance() {
        return depthOfInheritanceTree > 5;
    }
    
    /**
     * Check if the cohesion is too low
     */
    public boolean hasLowCohesion() {
        return lackOfCohesionInMethods > 0.8;
    }
    
    /**
     * Get the overall quality score (0-100)
     */
    public double getQualityScore() {
        double score = 100.0;
        
        // Deduct points for various issues
        if (isTooComplex()) score -= 20;
        if (isTooLarge()) score -= 15;
        if (hasTooManyMethods()) score -= 10;
        if (hasTooManyFields()) score -= 10;
        if (hasHighCoupling()) score -= 15;
        if (hasDeepInheritance()) score -= 10;
        if (hasLowCohesion()) score -= 15;
        if (!isWellDocumented()) score -= 5;
        
        return Math.max(0, score);
    }
    
    /**
     * Get the quality rating based on score
     */
    public QualityRating getQualityRating() {
        double score = getQualityScore();
        if (score >= 90) return QualityRating.EXCELLENT;
        if (score >= 80) return QualityRating.GOOD;
        if (score >= 70) return QualityRating.FAIR;
        if (score >= 60) return QualityRating.POOR;
        return QualityRating.VERY_POOR;
    }
    
    /**
     * Quality rating enumeration
     */
    public enum QualityRating {
        EXCELLENT, GOOD, FAIR, POOR, VERY_POOR
    }
}