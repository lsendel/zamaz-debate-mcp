package com.zamaz.mcp.pattern.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context information about the entire project being analyzed.
 */
@Data
@Builder
public class ProjectContext {
    
    /**
     * Root path of the project
     */
    private final Path projectRoot;
    
    /**
     * Project name
     */
    private final String projectName;
    
    /**
     * Project version
     */
    private final String projectVersion;
    
    /**
     * Programming language(s) used
     */
    @Singular
    private final Set<String> languages;
    
    /**
     * Frameworks and libraries used
     */
    @Singular
    private final Set<String> frameworks;
    
    /**
     * All dependencies in the project
     */
    @Singular
    private final List<Dependency> dependencies;
    
    /**
     * Build system information
     */
    private final BuildSystemInfo buildSystemInfo;
    
    /**
     * Testing framework information
     */
    @Singular
    private final Set<String> testFrameworks;
    
    /**
     * Code quality tools configured
     */
    @Singular
    private final Set<String> qualityTools;
    
    /**
     * Package structure information
     */
    @Singular
    private final Map<String, PackageInfo> packageStructure;
    
    /**
     * Total number of files in the project
     */
    private final int totalFiles;
    
    /**
     * Total lines of code
     */
    private final int totalLinesOfCode;
    
    /**
     * Total number of classes
     */
    private final int totalClasses;
    
    /**
     * Total number of methods
     */
    private final int totalMethods;
    
    /**
     * Git repository information
     */
    private final GitRepositoryInfo gitInfo;
    
    /**
     * Team-specific coding standards
     */
    private final TeamCodingStandards teamStandards;
    
    /**
     * Historical pattern analysis results
     */
    @Singular
    private final List<HistoricalPatternData> historicalData;
    
    /**
     * Custom pattern definitions
     */
    @Singular
    private final List<CustomPatternDefinition> customPatterns;
    
    /**
     * Configuration settings for pattern detection
     */
    private final PatternDetectionConfig detectionConfig;
    
    /**
     * Check if the project uses a specific framework
     */
    public boolean usesFramework(String frameworkName) {
        return frameworks.contains(frameworkName) || 
               frameworks.stream().anyMatch(f -> f.toLowerCase().contains(frameworkName.toLowerCase()));
    }
    
    /**
     * Check if the project uses Spring framework
     */
    public boolean usesSpring() {
        return usesFramework("spring");
    }
    
    /**
     * Check if the project uses JPA/Hibernate
     */
    public boolean usesJpa() {
        return usesFramework("jpa") || usesFramework("hibernate");
    }
    
    /**
     * Check if the project uses a specific testing framework
     */
    public boolean usesTestFramework(String testFrameworkName) {
        return testFrameworks.contains(testFrameworkName) || 
               testFrameworks.stream().anyMatch(f -> f.toLowerCase().contains(testFrameworkName.toLowerCase()));
    }
    
    /**
     * Get the primary programming language
     */
    public String getPrimaryLanguage() {
        return languages.isEmpty() ? "java" : languages.iterator().next();
    }
    
    /**
     * Check if this is a multi-module project
     */
    public boolean isMultiModule() {
        return packageStructure.size() > 5; // Simple heuristic
    }
    
    /**
     * Get the complexity level of the project
     */
    public ProjectComplexity getComplexity() {
        if (totalClasses > 1000) {
            return ProjectComplexity.VERY_HIGH;
        } else if (totalClasses > 500) {
            return ProjectComplexity.HIGH;
        } else if (totalClasses > 100) {
            return ProjectComplexity.MEDIUM;
        } else if (totalClasses > 20) {
            return ProjectComplexity.LOW;
        } else {
            return ProjectComplexity.VERY_LOW;
        }
    }
    
    /**
     * Project complexity levels
     */
    public enum ProjectComplexity {
        VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
    }
}