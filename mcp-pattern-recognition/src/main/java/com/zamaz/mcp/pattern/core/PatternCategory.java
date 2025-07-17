package com.zamaz.mcp.pattern.core;

/**
 * Categories of patterns for organization and reporting.
 */
public enum PatternCategory {
    
    /**
     * Gang of Four design patterns
     */
    DESIGN_PATTERN("Design Patterns", 
        "Classic GoF design patterns including creational, structural, and behavioral patterns"),
    
    /**
     * Enterprise application patterns
     */
    ENTERPRISE_PATTERN("Enterprise Patterns", 
        "Patterns commonly used in enterprise applications including MVC, Repository, Service Layer"),
    
    /**
     * Architectural patterns
     */
    ARCHITECTURAL_PATTERN("Architectural Patterns", 
        "High-level structural patterns for application architecture"),
    
    /**
     * Code smells and quality issues
     */
    CODE_SMELL("Code Smells", 
        "Indicators of potential code quality issues and maintainability problems"),
    
    /**
     * Anti-patterns and bad practices
     */
    ANTI_PATTERN("Anti-patterns", 
        "Common design mistakes and practices that should be avoided"),
    
    /**
     * Framework-specific patterns
     */
    FRAMEWORK_PATTERN("Framework Patterns", 
        "Patterns specific to frameworks like Spring, JPA, etc."),
    
    /**
     * Testing patterns
     */
    TESTING_PATTERN("Testing Patterns", 
        "Patterns related to unit testing, integration testing, and test organization"),
    
    /**
     * Performance-related patterns
     */
    PERFORMANCE_PATTERN("Performance Patterns", 
        "Patterns that impact application performance and scalability"),
    
    /**
     * Security patterns
     */
    SECURITY_PATTERN("Security Patterns", 
        "Patterns related to application security and data protection"),
    
    /**
     * Concurrency patterns
     */
    CONCURRENCY_PATTERN("Concurrency Patterns", 
        "Patterns for managing concurrent execution and thread safety"),
    
    /**
     * Team-specific custom patterns
     */
    TEAM_PATTERN("Team Patterns", 
        "Custom patterns specific to team coding standards and practices"),
    
    /**
     * Unclassified patterns
     */
    UNKNOWN("Unknown", 
        "Patterns that don't fit into other categories");
    
    private final String displayName;
    private final String description;
    
    PatternCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the severity level for this pattern category.
     * Used for prioritizing pattern detection results.
     */
    public PatternSeverity getDefaultSeverity() {
        return switch (this) {
            case CODE_SMELL -> PatternSeverity.MEDIUM;
            case ANTI_PATTERN -> PatternSeverity.HIGH;
            case SECURITY_PATTERN -> PatternSeverity.HIGH;
            case PERFORMANCE_PATTERN -> PatternSeverity.MEDIUM;
            case DESIGN_PATTERN, ENTERPRISE_PATTERN, ARCHITECTURAL_PATTERN -> PatternSeverity.LOW;
            case FRAMEWORK_PATTERN, TESTING_PATTERN, CONCURRENCY_PATTERN -> PatternSeverity.LOW;
            case TEAM_PATTERN -> PatternSeverity.LOW;
            case UNKNOWN -> PatternSeverity.LOW;
        };
    }
}