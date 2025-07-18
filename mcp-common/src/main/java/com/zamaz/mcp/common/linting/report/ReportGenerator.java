package com.zamaz.mcp.common.linting.report;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zamaz.mcp.common.linting.LintingIssue;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.LintingSeverity;
import com.zamaz.mcp.common.linting.ReportFormat;

/**
 * Generates comprehensive linting reports in various formats.
 */
@Component
public class ReportGenerator {

    private final ObjectMapper objectMapper;

    public ReportGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public String generateReport(LintingResult result, ReportFormat format, String projectName) {
        LintingReport report = createDetailedReport(result, projectName);

        switch (format) {
            case JSON:
                return generateJsonReport(report);
            case HTML:
                return generateHtmlReport(report);
            case XML:
                return generateXmlReport(report);
            case MARKDOWN:
                return generateMarkdownReport(report);
            case CONSOLE:
            default:
                return generateConsoleReport(report);
        }
    }

    private LintingReport createDetailedReport(LintingResult result, String projectName) {
        List<LintingIssue> issues = result.getIssues();

        // Create summary
        LintingReport.LintingSummary summary = new LintingReport.LintingSummary(
                result.getFilesProcessed(),
                issues.size(),
                (int) issues.stream().filter(i -> i.getSeverity() == LintingSeverity.ERROR).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == LintingSeverity.WARNING).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == LintingSeverity.INFO).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == LintingSeverity.SUGGESTION).count(),
                calculateQualityScore(issues, result.getFilesProcessed()),
                result.isSuccessful());

        // Create service reports
        Map<String, LintingReport.ServiceReport> serviceReports = createServiceReports(issues);

        // Create quality metrics
        LintingReport.QualityMetrics metrics = createQualityMetrics(issues, result);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(issues, result);

        return LintingReport.builder()
                .projectName(projectName)
                .timestamp(result.getTimestamp())
                .summary(summary)
                .issues(issues)
                .serviceReports(serviceReports)
                .metrics(metrics)
                .recommendations(recommendations)
                .build();
    }

    private Map<String, LintingReport.ServiceReport> createServiceReports(List<LintingIssue> issues) {
        Map<String, List<LintingIssue>> issuesByService = issues.stream()
                .collect(Collectors.groupingBy(this::extractServiceName));

        Map<String, LintingReport.ServiceReport> serviceReports = new HashMap<>();

        for (Map.Entry<String, List<LintingIssue>> entry : issuesByService.entrySet()) {
            String serviceName = entry.getKey();
            List<LintingIssue> serviceIssues = entry.getValue();

            Map<String, Integer> issuesByLinter = serviceIssues.stream()
                    .collect(Collectors.groupingBy(
                            LintingIssue::getLinter,
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

            double serviceQualityScore = calculateServiceQualityScore(serviceIssues);

            serviceReports.put(serviceName, new LintingReport.ServiceReport(
                    serviceName,
                    getFileCountForService(serviceName, serviceIssues),
                    serviceIssues,
                    issuesByLinter,
                    serviceQualityScore));
        }

        return serviceReports;
    }

    private LintingReport.QualityMetrics createQualityMetrics(List<LintingIssue> issues, LintingResult result) {
        Map<String, Integer> issuesByType = issues.stream()
                .collect(Collectors.groupingBy(
                        issue -> issue.getSeverity().toString(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

        Map<String, Integer> issuesByLinter = issues.stream()
                .collect(Collectors.groupingBy(
                        LintingIssue::getLinter,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

        Map<String, Double> scoresByService = createServiceReports(issues).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getQualityScore()));

        int autoFixableCount = (int) issues.stream()
                .filter(LintingIssue::isAutoFixable)
                .count();

        List<String> topIssues = getTopIssues(issues);

        return new LintingReport.QualityMetrics(
                calculateQualityScore(issues, result.getFilesProcessed()),
                scoresByService,
                issuesByType,
                issuesByLinter,
                new HashMap<>(), // Coverage data would come from separate analysis
                autoFixableCount,
                topIssues);
    }

    private List<String> generateRecommendations(List<LintingIssue> issues, LintingResult result) {
        List<String> recommendations = new ArrayList<>();

        long errorCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.ERROR).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.WARNING).count();
        long autoFixableCount = issues.stream().filter(LintingIssue::isAutoFixable).count();

        if (errorCount > 0) {
            recommendations.add("Fix " + errorCount + " critical errors before proceeding");
        }

        if (warningCount > 10) {
            recommendations.add("Consider addressing " + warningCount + " warnings to improve code quality");
        }

        if (autoFixableCount > 0) {
            recommendations.add("Run auto-fix to resolve " + autoFixableCount + " issues automatically");
        }

        // Analyze common issues
        Map<String, Long> ruleFrequency = issues.stream()
                .collect(Collectors.groupingBy(LintingIssue::getRule, Collectors.counting()));

        ruleFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 5)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> recommendations.add(
                        "Consider reviewing rule '" + entry.getKey() + "' - appears " + entry.getValue() + " times"));

        if (recommendations.isEmpty()) {
            recommendations.add("Great job! No major issues found. Consider running periodic quality checks.");
        }

        return recommendations;
    }

    private String generateJsonReport(LintingReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate JSON report: " + e.getMessage() + "\"}";
        }
    }

    private String generateHtmlReport(LintingReport report) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Linting Report - ").append(report.getProjectName()).append("</title>\n");
        html.append("    <style>\n");
        html.append(getHtmlStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <header class=\"header\">\n");
        html.append("        <h1>Code Quality Report</h1>\n");
        html.append("        <div class=\"project-info\">\n");
        html.append("            <h2>").append(report.getProjectName()).append("</h2>\n");
        html.append("            <p>Generated: ")
                .append(report.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("</p>\n");
        html.append("        </div>\n");
        html.append("    </header>\n");

        // Summary
        html.append("    <section class=\"summary\">\n");
        html.append("        <h2>Summary</h2>\n");
        html.append("        <div class=\"summary-grid\">\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <span class=\"metric-value\">").append(report.getSummary().getTotalFiles())
                .append("</span>\n");
        html.append("                <span class=\"metric-label\">Files Processed</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <span class=\"metric-value\">").append(report.getSummary().getTotalIssues())
                .append("</span>\n");
        html.append("                <span class=\"metric-label\">Total Issues</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric error\">\n");
        html.append("                <span class=\"metric-value\">").append(report.getSummary().getErrorCount())
                .append("</span>\n");
        html.append("                <span class=\"metric-label\">Errors</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric warning\">\n");
        html.append("                <span class=\"metric-value\">").append(report.getSummary().getWarningCount())
                .append("</span>\n");
        html.append("                <span class=\"metric-label\">Warnings</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric score\">\n");
        html.append("                <span class=\"metric-value\">")
                .append(String.format("%.1f", report.getSummary().getQualityScore())).append("</span>\n");
        html.append("                <span class=\"metric-label\">Quality Score</span>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("    </section>\n");

        // Issues table
        if (!report.getIssues().isEmpty()) {
            html.append("    <section class=\"issues\">\n");
            html.append("        <h2>Issues</h2>\n");
            html.append("        <table class=\"issues-table\">\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>Severity</th>\n");
            html.append("                    <th>File</th>\n");
            html.append("                    <th>Line</th>\n");
            html.append("                    <th>Message</th>\n");
            html.append("                    <th>Rule</th>\n");
            html.append("                    <th>Linter</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");

            for (LintingIssue issue : report.getIssues()) {
                html.append("                <tr class=\"").append(issue.getSeverity().toString().toLowerCase())
                        .append("\">\n");
                html.append("                    <td><span class=\"severity-badge\">").append(issue.getSeverity())
                        .append("</span></td>\n");
                html.append("                    <td>").append(issue.getFile()).append("</td>\n");
                html.append("                    <td>").append(issue.getLine()).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(issue.getMessage())).append("</td>\n");
                html.append("                    <td>").append(issue.getRule()).append("</td>\n");
                html.append("                    <td>").append(issue.getLinter()).append("</td>\n");
                html.append("                </tr>\n");
            }

            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </section>\n");
        }

        // Recommendations
        if (!report.getRecommendations().isEmpty()) {
            html.append("    <section class=\"recommendations\">\n");
            html.append("        <h2>Recommendations</h2>\n");
            html.append("        <ul>\n");
            for (String recommendation : report.getRecommendations()) {
                html.append("            <li>").append(escapeHtml(recommendation)).append("</li>\n");
            }
            html.append("        </ul>\n");
            html.append("    </section>\n");
        }

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String generateMarkdownReport(LintingReport report) {
        StringBuilder md = new StringBuilder();

        md.append("# Code Quality Report\n\n");
        md.append("**Project:** ").append(report.getProjectName()).append("\n");
        md.append("**Generated:** ")
                .append(report.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n\n");

        // Summary
        md.append("## Summary\n\n");
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append("| Files Processed | ").append(report.getSummary().getTotalFiles()).append(" |\n");
        md.append("| Total Issues | ").append(report.getSummary().getTotalIssues()).append(" |\n");
        md.append("| Errors | ").append(report.getSummary().getErrorCount()).append(" |\n");
        md.append("| Warnings | ").append(report.getSummary().getWarningCount()).append(" |\n");
        md.append("| Quality Score | ").append(String.format("%.1f/100", report.getSummary().getQualityScore()))
                .append(" |\n");
        md.append("| Status | ").append(report.getSummary().isPassed() ? "✅ PASSED" : "❌ FAILED").append(" |\n\n");

        // Issues
        if (!report.getIssues().isEmpty()) {
            md.append("## Issues\n\n");
            md.append("| Severity | File | Line | Message | Rule |\n");
            md.append("|----------|------|------|---------|------|\n");

            for (LintingIssue issue : report.getIssues()) {
                String severityIcon = getSeverityIcon(issue.getSeverity());
                md.append("| ").append(severityIcon).append(" ").append(issue.getSeverity())
                        .append(" | ").append(issue.getFile())
                        .append(" | ").append(issue.getLine())
                        .append(" | ").append(issue.getMessage().replace("|", "\\|"))
                        .append(" | ").append(issue.getRule())
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Recommendations
        if (!report.getRecommendations().isEmpty()) {
            md.append("## Recommendations\n\n");
            for (String recommendation : report.getRecommendations()) {
                md.append("- ").append(recommendation).append("\n");
            }
            md.append("\n");
        }

        return md.toString();
    }

    private String generateConsoleReport(LintingReport report) {
        StringBuilder console = new StringBuilder();

        console.append("╔══════════════════════════════════════════════════════════════════╗\n");
        console.append("║                    CODE QUALITY REPORT                          ║\n");
        console.append("╚══════════════════════════════════════════════════════════════════╝\n\n");

        console.append("Project: ").append(report.getProjectName()).append("\n");
        console.append("Generated: ")
                .append(report.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n\n");

        // Summary
        console.append("SUMMARY\n");
        console.append("═══════\n");
        console.append("Files Processed: ").append(report.getSummary().getTotalFiles()).append("\n");
        console.append("Total Issues: ").append(report.getSummary().getTotalIssues()).append("\n");
        console.append("Errors: ").append(report.getSummary().getErrorCount()).append("\n");
        console.append("Warnings: ").append(report.getSummary().getWarningCount()).append("\n");
        console.append("Quality Score: ").append(String.format("%.1f/100", report.getSummary().getQualityScore()))
                .append("\n");
        console.append("Status: ").append(report.getSummary().isPassed() ? "PASSED" : "FAILED").append("\n\n");

        // Top issues
        if (!report.getIssues().isEmpty()) {
            console.append("ISSUES\n");
            console.append("══════\n");

            report.getIssues().stream()
                    .limit(20) // Show top 20 issues
                    .forEach(issue -> {
                        console.append(String.format("[%s] %s:%d - %s (%s)\n",
                                issue.getSeverity(),
                                issue.getFile(),
                                issue.getLine(),
                                issue.getMessage(),
                                issue.getRule()));
                    });

            if (report.getIssues().size() > 20) {
                console.append("... and ").append(report.getIssues().size() - 20).append(" more issues\n");
            }
            console.append("\n");
        }

        // Recommendations
        if (!report.getRecommendations().isEmpty()) {
            console.append("RECOMMENDATIONS\n");
            console.append("═══════════════\n");
            for (String recommendation : report.getRecommendations()) {
                console.append("• ").append(recommendation).append("\n");
            }
        }

        return console.toString();
    }

    private String generateXmlReport(LintingReport report) {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<linting-report>\n");
        xml.append("  <project-name>").append(escapeXml(report.getProjectName())).append("</project-name>\n");
        xml.append("  <timestamp>").append(report.getTimestamp()).append("</timestamp>\n");

        // Summary
        xml.append("  <summary>\n");
        xml.append("    <total-files>").append(report.getSummary().getTotalFiles()).append("</total-files>\n");
        xml.append("    <total-issues>").append(report.getSummary().getTotalIssues()).append("</total-issues>\n");
        xml.append("    <error-count>").append(report.getSummary().getErrorCount()).append("</error-count>\n");
        xml.append("    <warning-count>").append(report.getSummary().getWarningCount()).append("</warning-count>\n");
        xml.append("    <quality-score>").append(report.getSummary().getQualityScore()).append("</quality-score>\n");
        xml.append("    <passed>").append(report.getSummary().isPassed()).append("</passed>\n");
        xml.append("  </summary>\n");

        // Issues
        if (!report.getIssues().isEmpty()) {
            xml.append("  <issues>\n");
            for (LintingIssue issue : report.getIssues()) {
                xml.append("    <issue>\n");
                xml.append("      <severity>").append(issue.getSeverity()).append("</severity>\n");
                xml.append("      <file>").append(escapeXml(issue.getFile())).append("</file>\n");
                xml.append("      <line>").append(issue.getLine()).append("</line>\n");
                xml.append("      <column>").append(issue.getColumn()).append("</column>\n");
                xml.append("      <message>").append(escapeXml(issue.getMessage())).append("</message>\n");
                xml.append("      <rule>").append(escapeXml(issue.getRule())).append("</rule>\n");
                xml.append("      <linter>").append(escapeXml(issue.getLinter())).append("</linter>\n");
                xml.append("      <auto-fixable>").append(issue.isAutoFixable()).append("</auto-fixable>\n");
                xml.append("    </issue>\n");
            }
            xml.append("  </issues>\n");
        }

        xml.append("</linting-report>");
        return xml.toString();
    }

    // Helper methods
    private String extractServiceName(LintingIssue issue) {
        String file = issue.getFile();
        if (file.contains("/")) {
            String[] parts = file.split("/");
            for (String part : parts) {
                if (part.startsWith("mcp-") || part.equals("debate-ui") || part.equals("github-integration")) {
                    return part;
                }
            }
        }
        return "unknown";
    }

    private int getFileCountForService(String serviceName, List<LintingIssue> issues) {
        return (int) issues.stream()
                .map(LintingIssue::getFile)
                .distinct()
                .count();
    }

    private double calculateQualityScore(List<LintingIssue> issues, int totalFiles) {
        if (totalFiles == 0)
            return 100.0;

        long errorCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.ERROR).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.WARNING).count();

        double penalty = (errorCount * 10.0 + warningCount * 2.0) / totalFiles;
        return Math.max(0.0, 100.0 - penalty);
    }

    private double calculateServiceQualityScore(List<LintingIssue> issues) {
        if (issues.isEmpty())
            return 100.0;

        long errorCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.ERROR).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == LintingSeverity.WARNING).count();

        double penalty = errorCount * 15.0 + warningCount * 3.0;
        return Math.max(0.0, 100.0 - penalty);
    }

    private List<String> getTopIssues(List<LintingIssue> issues) {
        return issues.stream()
                .collect(Collectors.groupingBy(LintingIssue::getRule, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> entry.getKey() + " (" + entry.getValue() + " occurrences)")
                .collect(Collectors.toList());
    }

    private String getSeverityIcon(LintingSeverity severity) {
        switch (severity) {
            case ERROR:
                return "🔴";
            case WARNING:
                return "🟡";
            case INFO:
                return "🔵";
            case SUGGESTION:
                return "💡";
            default:
                return "⚪";
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String getHtmlStyles() {
        return """
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                .header { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .header h1 { margin: 0; color: #333; }
                .project-info h2 { margin: 10px 0 5px 0; color: #666; }
                .project-info p { margin: 0; color: #999; }
                .summary { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .summary h2 { margin-top: 0; }
                .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; }
                .metric { text-align: center; padding: 15px; border-radius: 6px; background: #f8f9fa; }
                .metric.error { background: #fee; }
                .metric.warning { background: #fff3cd; }
                .metric.score { background: #d4edda; }
                .metric-value { display: block; font-size: 24px; font-weight: bold; color: #333; }
                .metric-label { display: block; font-size: 12px; color: #666; margin-top: 5px; }
                .issues { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .issues h2 { margin-top: 0; }
                .issues-table { width: 100%; border-collapse: collapse; }
                .issues-table th, .issues-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #eee; }
                .issues-table th { background: #f8f9fa; font-weight: 600; }
                .issues-table tr.error { background: #fef2f2; }
                .issues-table tr.warning { background: #fffbf0; }
                .severity-badge { padding: 2px 6px; border-radius: 3px; font-size: 11px; font-weight: bold; }
                .recommendations { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .recommendations h2 { margin-top: 0; }
                .recommendations ul { padding-left: 20px; }
                .recommendations li { margin-bottom: 8px; }
                """;
    }
}
