package com.zamaz.mcp.common.linting.impl;

import com.zamaz.mcp.common.linting.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of LintingResult.
 */
public class DefaultLintingResult implements LintingResult {
    
    private final List<LintingIssue> issues;
    private final Map<String, Object> metrics;
    private final LocalDateTime timestamp;
    private final int filesProcessed;
    private final long durationMs;
    private final boolean successful;
    
    public DefaultLintingResult(List<LintingIssue> issues, Map<String, Object> metrics, 
                               LocalDateTime timestamp, int filesProcessed, 
                               long durationMs, boolean successful) {
        this.issues = issues;
        this.metrics = metrics;
        this.timestamp = timestamp;
        this.filesProcessed = filesProcessed;
        this.durationMs = durationMs;
        this.successful = successful;
    }
    
    @Override
    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == LintingSeverity.ERROR);
    }
    
    @Override
    public boolean hasWarnings() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == LintingSeverity.WARNING);
    }
    
    @Override
    public List<LintingIssue> getIssues() {
        return issues;
    }
    
    @Override
    public List<LintingIssue> getIssuesBySeverity(LintingSeverity severity) {
        return issues.stream()
            .filter(issue -> issue.getSeverity() == severity)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    @Override
    public String generateReport(ReportFormat format) {
        switch (format) {
            case CONSOLE:
                return generateConsoleReport();
            case JSON:
                return generateJsonReport();
            case HTML:
                return generateHtmlReport();
            case XML:
                return generateXmlReport();
            case MARKDOWN:
                return generateMarkdownReport();
            default:
                return generateConsoleReport();
        }
    }
    
    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public int getFilesProcessed() {
        return filesProcessed;
    }
    
    @Override
    public long getDurationMs() {
        return durationMs;
    }
    
    @Override
    public boolean isSuccessful() {
        return successful;
    }
    
    private String generateConsoleReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Linting Report ===\n");
        report.append("Timestamp: ").append(timestamp).append("\n");
        report.append("Files processed: ").append(filesProcessed).append("\n");
        report.append("Duration: ").append(durationMs).append("ms\n");
        report.append("Total issues: ").append(issues.size()).append("\n");
        report.append("Errors: ").append(getIssuesBySeverity(LintingSeverity.ERROR).size()).append("\n");
        report.append("Warnings: ").append(getIssuesBySeverity(LintingSeverity.WARNING).size()).append("\n");
        report.append("Status: ").append(successful ? "SUCCESS" : "FAILED").append("\n");
        
        if (!issues.isEmpty()) {
            report.append("\n=== Issues ===\n");
            for (LintingIssue issue : issues) {
                report.append(String.format("[%s] %s:%d:%d - %s (%s)\n",
                    issue.getSeverity(),
                    issue.getFile(),
                    issue.getLine(),
                    issue.getColumn(),
                    issue.getMessage(),
                    issue.getRule()
                ));
            }
        }
        
        return report.toString();
    }
    
    private String generateJsonReport() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate JSON report: " + e.getMessage() + "\"}";
        }
    }
    
    private String generateHtmlReport() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head><title>Linting Report</title></head><body>\n");
        html.append("<h1>Linting Report</h1>\n");
        html.append("<p><strong>Timestamp:</strong> ").append(timestamp).append("</p>\n");
        html.append("<p><strong>Files processed:</strong> ").append(filesProcessed).append("</p>\n");
        html.append("<p><strong>Duration:</strong> ").append(durationMs).append("ms</p>\n");
        html.append("<p><strong>Total issues:</strong> ").append(issues.size()).append("</p>\n");
        html.append("<p><strong>Status:</strong> ").append(successful ? "SUCCESS" : "FAILED").append("</p>\n");
        
        if (!issues.isEmpty()) {
            html.append("<h2>Issues</h2>\n");
            html.append("<table border='1'>\n");
            html.append("<tr><th>Severity</th><th>File</th><th>Line</th><th>Message</th><th>Rule</th></tr>\n");
            for (LintingIssue issue : issues) {
                html.append("<tr>")
                    .append("<td>").append(issue.getSeverity()).append("</td>")
                    .append("<td>").append(issue.getFile()).append("</td>")
                    .append("<td>").append(issue.getLine()).append("</td>")
                    .append("<td>").append(issue.getMessage()).append("</td>")
                    .append("<td>").append(issue.getRule()).append("</td>")
                    .append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    private String generateXmlReport() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        xml.append("<linting-report>\n");
        xml.append("  <timestamp>").append(timestamp).append("</timestamp>\n");
        xml.append("  <files-processed>").append(filesProcessed).append("</files-processed>\n");
        xml.append("  <duration-ms>").append(durationMs).append("</duration-ms>\n");
        xml.append("  <total-issues>").append(issues.size()).append("</total-issues>\n");
        xml.append("  <successful>").append(successful).append("</successful>\n");
        
        if (!issues.isEmpty()) {
            xml.append("  <issues>\n");
            for (LintingIssue issue : issues) {
                xml.append("    <issue>\n");
                xml.append("      <severity>").append(issue.getSeverity()).append("</severity>\n");
                xml.append("      <file>").append(issue.getFile()).append("</file>\n");
                xml.append("      <line>").append(issue.getLine()).append("</line>\n");
                xml.append("      <column>").append(issue.getColumn()).append("</column>\n");
                xml.append("      <message>").append(issue.getMessage()).append("</message>\n");
                xml.append("      <rule>").append(issue.getRule()).append("</rule>\n");
                xml.append("      <linter>").append(issue.getLinter()).append("</linter>\n");
                xml.append("    </issue>\n");
            }
            xml.append("  </issues>\n");
        }
        
        xml.append("</linting-report>");
        return xml.toString();
    }
    
    private String generateMarkdownReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Linting Report\n\n");
        md.append("- **Timestamp:** ").append(timestamp).append("\n");
        md.append("- **Files processed:** ").append(filesProcessed).append("\n");
        md.append("- **Duration:** ").append(durationMs).append("ms\n");
        md.append("- **Total issues:** ").append(issues.size()).append("\n");
        md.append("- **Status:** ").append(successful ? "✅ SUCCESS" : "❌ FAILED").append("\n\n");
        
        if (!issues.isEmpty()) {
            md.append("## Issues\n\n");
            md.append("| Severity | File | Line | Message | Rule |\n");
            md.append("|----------|------|------|---------|------|\n");
            for (LintingIssue issue : issues) {
                md.append("| ").append(issue.getSeverity())
                  .append(" | ").append(issue.getFile())
                  .append(" | ").append(issue.getLine())
                  .append(" | ").append(issue.getMessage())
                  .append(" | ").append(issue.getRule())
                  .append(" |\n");
            }
        }
        
        return md.toString();
    }
}