package com.zamaz.mcp.common.linting;

/**
 * Enumeration of supported report formats.
 */
public enum ReportFormat {
    /**
     * Console output format.
     */
    CONSOLE,
    
    /**
     * JSON format for programmatic consumption.
     */
    JSON,
    
    /**
     * HTML format for web viewing.
     */
    HTML,
    
    /**
     * XML format for tool integration.
     */
    XML,
    
    /**
     * Markdown format for documentation.
     */
    MARKDOWN
}