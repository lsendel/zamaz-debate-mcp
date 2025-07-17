package com.zamaz.mcp.pattern.model;

import com.zamaz.mcp.pattern.core.PatternCategory;
import com.zamaz.mcp.pattern.core.PatternSeverity;
import com.zamaz.mcp.pattern.core.PatternType;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result of pattern detection containing all information about a detected pattern.
 */
@Data
@Builder
public class PatternDetectionResult {
    
    /**
     * Type of pattern detected
     */
    private final PatternType patternType;
    
    /**
     * Category of the pattern
     */
    private final PatternCategory patternCategory;
    
    /**
     * Severity of the detected pattern
     */
    private final PatternSeverity severity;
    
    /**
     * Confidence score (0.0 to 1.0) of the detection
     */
    private final double confidence;
    
    /**
     * File where the pattern was detected
     */
    private final Path filePath;
    
    /**
     * Line number where the pattern starts
     */
    private final int startLine;
    
    /**
     * Line number where the pattern ends
     */
    private final int endLine;
    
    /**
     * Column number where the pattern starts
     */
    private final int startColumn;
    
    /**
     * Column number where the pattern ends
     */
    private final int endColumn;
    
    /**
     * Description of the detected pattern
     */
    private final String description;
    
    /**
     * Detailed explanation of why this pattern was detected
     */
    private final String explanation;
    
    /**
     * Suggestions for improvement or refactoring
     */
    @Singular
    private final List<String> suggestions;
    
    /**
     * Examples of how to fix or improve the pattern
     */
    @Singular
    private final List<CodeExample> codeExamples;
    
    /**
     * Related patterns or issues
     */
    @Singular
    private final List<PatternType> relatedPatterns;
    
    /**
     * Additional metadata about the detection
     */
    @Singular
    private final Map<String, Object> metadata;
    
    /**
     * Timestamp when the pattern was detected
     */
    @Builder.Default
    private final LocalDateTime detectedAt = LocalDateTime.now();
    
    /**
     * Name of the detector that found this pattern
     */
    private final String detectorName;
    
    /**
     * Version of the detector
     */
    private final String detectorVersion;
    
    /**
     * Impact assessment of the pattern
     */
    private final PatternImpact impact;
    
    /**
     * Effort required to fix the pattern
     */
    private final RefactoringEffort refactoringEffort;
    
    /**
     * Priority for addressing this pattern
     */
    private final PatternPriority priority;
    
    /**
     * Code snippet where the pattern was detected
     */
    private final String codeSnippet;
    
    /**
     * Context information about the pattern location
     */
    private final LocationContext locationContext;
    
    /**
     * Check if this result represents a positive pattern (good practice)
     */
    public boolean isPositivePattern() {
        return patternCategory == PatternCategory.DESIGN_PATTERN || 
               patternCategory == PatternCategory.ENTERPRISE_PATTERN || 
               patternCategory == PatternCategory.ARCHITECTURAL_PATTERN ||
               patternCategory == PatternCategory.FRAMEWORK_PATTERN ||
               patternCategory == PatternCategory.TESTING_PATTERN ||
               patternCategory == PatternCategory.PERFORMANCE_PATTERN ||
               patternCategory == PatternCategory.SECURITY_PATTERN ||
               patternCategory == PatternCategory.CONCURRENCY_PATTERN;
    }
    
    /**
     * Check if this result represents a negative pattern (issue)
     */
    public boolean isNegativePattern() {
        return patternCategory == PatternCategory.CODE_SMELL || 
               patternCategory == PatternCategory.ANTI_PATTERN;
    }
    
    /**
     * Get the display name for this pattern result
     */
    public String getDisplayName() {
        return patternType.name().toLowerCase().replace("_", " ");
    }
    
    /**
     * Get a specific metadata value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Check if this pattern has high priority
     */
    public boolean isHighPriority() {
        return priority == PatternPriority.HIGH || priority == PatternPriority.CRITICAL;
    }
    
    /**
     * Get the urgency level based on severity and impact
     */
    public UrgencyLevel getUrgencyLevel() {
        if (severity == PatternSeverity.CRITICAL || 
            (severity == PatternSeverity.HIGH && impact == PatternImpact.HIGH)) {
            return UrgencyLevel.IMMEDIATE;
        } else if (severity == PatternSeverity.HIGH || 
                   (severity == PatternSeverity.MEDIUM && impact == PatternImpact.HIGH)) {
            return UrgencyLevel.SOON;
        } else if (severity == PatternSeverity.MEDIUM) {
            return UrgencyLevel.PLANNED;
        } else {
            return UrgencyLevel.OPTIONAL;
        }
    }
    
    /**
     * Urgency levels for pattern addressing
     */
    public enum UrgencyLevel {
        IMMEDIATE, SOON, PLANNED, OPTIONAL
    }
}