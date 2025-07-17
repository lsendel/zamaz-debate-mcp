package com.zamaz.mcp.pattern.core;

/**
 * Severity levels for pattern detection results.
 * Used for prioritizing and filtering pattern analysis results.
 */
public enum PatternSeverity {
    
    /**
     * Low severity - informational patterns, good practices
     */
    LOW("Low", 1, "Informational patterns and good practices"),
    
    /**
     * Medium severity - potential issues, code smells
     */
    MEDIUM("Medium", 2, "Potential issues and code smells that should be reviewed"),
    
    /**
     * High severity - serious issues, anti-patterns
     */
    HIGH("High", 3, "Serious issues and anti-patterns that should be addressed"),
    
    /**
     * Critical severity - security issues, major architectural problems
     */
    CRITICAL("Critical", 4, "Critical issues requiring immediate attention");
    
    private final String displayName;
    private final int level;
    private final String description;
    
    PatternSeverity(String displayName, int level, String description) {
        this.displayName = displayName;
        this.level = level;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this severity is higher than another severity.
     * 
     * @param other The other severity to compare with
     * @return true if this severity is higher
     */
    public boolean isHigherThan(PatternSeverity other) {
        return this.level > other.level;
    }
    
    /**
     * Check if this severity is at least as high as another severity.
     * 
     * @param other The other severity to compare with
     * @return true if this severity is at least as high
     */
    public boolean isAtLeast(PatternSeverity other) {
        return this.level >= other.level;
    }
    
    /**
     * Get the color code for UI display.
     * 
     * @return Color code as hex string
     */
    public String getColorCode() {
        return switch (this) {
            case LOW -> "#28a745";      // Green
            case MEDIUM -> "#ffc107";   // Yellow
            case HIGH -> "#fd7e14";     // Orange
            case CRITICAL -> "#dc3545"; // Red
        };
    }
}