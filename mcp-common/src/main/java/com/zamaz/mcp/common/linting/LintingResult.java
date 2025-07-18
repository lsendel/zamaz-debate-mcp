package com.zamaz.mcp.common.linting;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents the aggregated results from linting operations.
 */
public interface LintingResult {
    
    /**
     * Check if there are any errors in the linting results.
     *
     * @return true if errors are present
     */
    boolean hasErrors();
    
    /**
     * Check if there are any warnings in the linting results.
     *
     * @return true if warnings are present
     */
    boolean hasWarnings();
    
    /**
     * Get all linting issues found.
     *
     * @return list of linting issues
     */
    List<LintingIssue> getIssues();
    
    /**
     * Get issues filtered by severity.
     *
     * @param severity the severity level to filter by
     * @return list of issues with the specified severity
     */
    List<LintingIssue> getIssuesBySeverity(LintingSeverity severity);
    
    /**
     * Get metrics about the linting operation.
     *
     * @return map of metric names to values
     */
    Map<String, Object> getMetrics();
    
    /**
     * Generate a report in the specified format.
     *
     * @param format the report format
     * @return formatted report as string
     */
    String generateReport(ReportFormat format);
    
    /**
     * Get the timestamp when linting was performed.
     *
     * @return linting timestamp
     */
    LocalDateTime getTimestamp();
    
    /**
     * Get the total number of files that were linted.
     *
     * @return number of files processed
     */
    int getFilesProcessed();
    
    /**
     * Get the duration of the linting operation in milliseconds.
     *
     * @return duration in milliseconds
     */
    long getDurationMs();
    
    /**
     * Check if the linting operation was successful (no critical errors).
     *
     * @return true if linting completed successfully
     */
    boolean isSuccessful();
}