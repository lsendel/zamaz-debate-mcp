package com.zamaz.mcp.common.linting;

/**
 * Enumeration of linting issue severity levels.
 */
public enum LintingSeverity {
    /**
     * Critical errors that must be fixed.
     */
    ERROR,
    
    /**
     * Warnings that should be addressed.
     */
    WARNING,
    
    /**
     * Informational messages.
     */
    INFO,
    
    /**
     * Suggestions for improvement.
     */
    SUGGESTION
}