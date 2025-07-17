package com.zamaz.mcp.pattern.core;

import com.zamaz.mcp.pattern.model.CodeAnalysisContext;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import java.util.List;

/**
 * Interface for pattern detection implementations.
 * Each pattern detector is responsible for identifying specific patterns in code.
 */
public interface PatternDetector {
    
    /**
     * Get the pattern type that this detector identifies.
     * 
     * @return The pattern type
     */
    PatternType getPatternType();
    
    /**
     * Get the category of patterns this detector handles.
     * 
     * @return The pattern category
     */
    PatternCategory getPatternCategory();
    
    /**
     * Detect patterns in the given code analysis context.
     * 
     * @param context The code analysis context containing parsed code, AST, etc.
     * @return List of detected patterns with confidence scores and metadata
     */
    List<PatternDetectionResult> detectPatterns(CodeAnalysisContext context);
    
    /**
     * Get the minimum confidence threshold for pattern detection.
     * Results below this threshold will be filtered out.
     * 
     * @return Confidence threshold between 0.0 and 1.0
     */
    default double getConfidenceThreshold() {
        return 0.7;
    }
    
    /**
     * Check if this detector supports the given file type.
     * 
     * @param fileExtension The file extension (e.g., ".java", ".py", ".js")
     * @return true if the detector supports this file type
     */
    default boolean supportsFileType(String fileExtension) {
        return ".java".equals(fileExtension);
    }
    
    /**
     * Get a brief description of what this detector does.
     * 
     * @return Description of the detector's purpose
     */
    String getDescription();
    
    /**
     * Get the performance impact of running this detector.
     * Used for optimization decisions in large codebases.
     * 
     * @return Performance impact level
     */
    default PerformanceImpact getPerformanceImpact() {
        return PerformanceImpact.MEDIUM;
    }
    
    /**
     * Performance impact levels for optimization.
     */
    enum PerformanceImpact {
        LOW,    // Fast execution, minimal resource usage
        MEDIUM, // Moderate execution time and resource usage
        HIGH    // Slow execution, high resource usage
    }
}