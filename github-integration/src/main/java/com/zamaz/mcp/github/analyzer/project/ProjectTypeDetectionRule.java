package com.zamaz.mcp.github.analyzer.project;

import com.zamaz.mcp.github.analyzer.model.RepositoryStructure;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * Rule for detecting a specific project type
 */
@Data
@Builder
public class ProjectTypeDetectionRule {
    
    /**
     * Files that must be present for this project type
     */
    private List<String> requiredFiles;
    
    /**
     * Files that may be present and increase confidence
     */
    private List<String> optionalFiles;
    
    /**
     * Directory patterns that indicate this project type
     */
    private List<String> directoryPatterns;
    
    /**
     * File patterns that indicate this project type
     */
    private List<String> filePatterns;
    
    /**
     * Minimum confidence required to report this project type
     */
    private double minConfidence;
    
    /**
     * Custom logic for project type detection
     */
    private Function<RepositoryStructure, Double> customLogic;
    
    /**
     * Weight for this rule (higher weight = more important)
     */
    @Builder.Default
    private double weight = 1.0;
    
    /**
     * Whether this rule is exclusive (only one project of this type per repository)
     */
    @Builder.Default
    private boolean exclusive = false;
    
    /**
     * Dependencies on other project types
     */
    private List<String> dependencies;
    
    /**
     * Project types that conflict with this one
     */
    private List<String> conflicts;
}