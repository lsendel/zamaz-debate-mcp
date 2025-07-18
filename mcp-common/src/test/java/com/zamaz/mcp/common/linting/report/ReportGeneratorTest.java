package com.zamaz.mcp.common.linting.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zamaz.mcp.common.linting.LintingIssue;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.LintingSeverity;
import com.zamaz.mcp.common.linting.ReportFormat;
import com.zamaz.mcp.common.linting.impl.DefaultLintingResult;

/**
 * Unit tests for ReportGenerator.
 */
class ReportGeneratorTest {

    private ReportGenerator reportGenerator;
    private LintingResult testResult;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();
        testResult = createTestLintingResult();
    }

    @Test
    void testGenerateConsoleReport() {
        // When
        String report = reportGenerator.generateReport(testResult, ReportFormat.CONSOLE, "Test Project");

        // Then
        assertNotNull(report);
        assertTrue(report.contains("CODE QUALITY REPORT"));
        assertTrue(report.contains("Test Project"));
        assertTrue(report.contains("Files Processed: 5"));
        assertTrue(report.contains("Total Issues: 3"));
        assertTrue(report.contains("Errors: 1"));
        assertTrue(report.contains("Warnings: 2"));
    }

    @Test
    void testGenerateJsonReport() {
        // When
        String report = reportGenerator.generateReport(testResult, ReportFormat.JSON, "Test Project");

        // Then
        assertNotNull(report);
        assertTrue(report.startsWith("{"));
        assertTrue(report.endsWith("}"));
        assertTrue(report.contains("\"projectName\":\"Test Project\""));
        assertTrue(report.contains("\"totalFiles\":5"));
        assertTrue(report.contains("\"totalIssues\":3"));
    }

    @Test
    void testGenerateHtmlReport() {
        // When
        String report = reportGenerator.generateReport(testResult, ReportFormat.HTML, "Test Project");

        // Then
        assertNotNull(report);
        assertTrue(report.startsWith("<!DOCTYPE html>"));
        assertTrue(report.contains("<html"));
        assertTrue(report.contains("</html>"));
        assertTrue(report.contains("Test Project"));
        assertTrue(report.contains("Files Processed"));
        assertTrue(report.contains("Total Issues"));
        assertTrue(report.contains("<table"));
    }

    @Test
    void testGenerateMarkdownReport() {
        // When
        String report = reportGenerator.generateReport(testResult, ReportFormat.MARKDOWN, "Test Project");

        // Then
        assertNotNull(report);
        assertTrue(report.contains("# Code Quality Report"));
        assertTrue(report.contains("**Project:** Test Project"));
        assertTrue(report.contains("| Metric | Value |"));
        assertTrue(report.contains("| Files Processed | 5 |"));
        assertTrue(report.contains("| Total Issues | 3 |"));
        assertTrue(report.contains("## Issues"));
    }

    @Test
    void testGenerateXmlReport() {
        // When
        String report = reportGenerator.generateReport(testResult, ReportFormat.XML, "Test Project");

        // Then
        assertNotNull(report);
        assertTrue(report.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(report.contains("<linting-report>"));
        assertTrue(report.contains("</linting-report>"));
        assertTrue(report.contains("<project-name>Test Project</project-name>"));
        assertTrue(report.contains("<total-files>5</total-files>"));
        assertTrue(report.contains("<total-issues>3</total-issues>"));
    }

    @Test
    void testReportWithNoIssues() {
        // Given
        LintingResult emptyResult = new DefaultLintingResult(
                Collections.emptyList(),
                Map.of("totalFiles", 10, "totalIssues", 0),
                LocalDateTime.now(),
                10,
                1000L,
                true);

        // When
        String consoleReport = reportGenerator.generateReport(emptyResult, ReportFormat.CONSOLE, "Clean Project");
        String htmlReport = reportGenerator.generateReport(emptyResult, ReportFormat.HTML, "Clean Project");

        // Then
        assertTrue(consoleReport.contains("Total Issues: 0"));
        assertTrue(consoleReport.contains("Status: SUCCESS"));
        assertTrue(htmlReport.contains("Clean Project"));
        assertFalse(htmlReport.contains("<table")); // No issues table should be present
    }

    @Test
    void testReportWithAllSeverityLevels() {
        // Given
        List<LintingIssue> issues = Arrays.asList(
                createTestIssue("ERR-001", LintingSeverity.ERROR, "Critical error"),
                createTestIssue("WARN-001", LintingSeverity.WARNING, "Warning message"),
                createTestIssue("INFO-001", LintingSeverity.INFO, "Info message"),
                createTestIssue("SUGG-001", LintingSeverity.SUGGESTION, "Suggestion message"));

        LintingResult result = new DefaultLintingResult(
                issues,
                Map.of("totalFiles", 1, "totalIssues", 4),
                LocalDateTime.now(),
                1,
                500L,
                false);

        // When
        String markdownReport = reportGenerator.generateReport(result, ReportFormat.MARKDOWN, "Multi-Severity Project");

        // Then
        assertTrue(markdownReport.contains("| ERROR |"));
        assertTrue(markdownReport.contains("| WARNING |"));
        assertTrue(markdownReport.contains("| INFO |"));
        assertTrue(markdownReport.contains("| SUGGESTION |"));
        assertTrue(markdownReport.contains("❌ FAILED")); // Should show failed status
    }

    @Test
    void testReportEscaping() {
        // Given
        LintingIssue issueWithSpecialChars = LintingIssue.builder()
                .id("SPECIAL-001")
                .severity(LintingSeverity.ERROR)
                .message("Error with <special> & \"quoted\" characters")
                .file("Test&File.java")
                .line(1)
                .column(1)
                .rule("special-rule")
                .linter("test-linter")
                .build();

        LintingResult result = new DefaultLintingResult(
                Arrays.asList(issueWithSpecialChars),
                Map.of("totalFiles", 1, "totalIssues", 1),
                LocalDateTime.now(),
                1,
                100L,
                false);

        // When
        String htmlReport = reportGenerator.generateReport(result, ReportFormat.HTML, "Escaping Test");
        String xmlReport = reportGenerator.generateReport(result, ReportFormat.XML, "Escaping Test");

        // Then
        // HTML should escape special characters
        assertTrue(htmlReport.contains("&lt;special&gt;"));
        assertTrue(htmlReport.contains("&amp;"));
        assertTrue(htmlReport.contains("&quot;"));

        // XML should escape special characters
        assertTrue(xmlReport.contains("&lt;special&gt;"));
        assertTrue(xmlReport.contains("&amp;"));
        assertTrue(xmlReport.contains("&quot;"));
    }

    private LintingResult createTestLintingResult() {
        List<LintingIssue> issues = Arrays.asList(
                createTestIssue("TEST-001", LintingSeverity.ERROR, "Test error message"),
                createTestIssue("TEST-002", LintingSeverity.WARNING, "Test warning message"),
                createTestIssue("TEST-003", LintingSeverity.WARNING, "Another warning message"));

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalFiles", 5);
        metrics.put("totalIssues", 3);
        metrics.put("errorCount", 1);
        metrics.put("warningCount", 2);
        metrics.put("qualityScore", 85.5);

        return new DefaultLintingResult(
                issues,
                metrics,
                LocalDateTime.now(),
                5,
                2500L,
                true);
    }

    private LintingIssue createTestIssue(String id, LintingSeverity severity, String message) {
        return LintingIssue.builder()
                .id(id)
                .severity(severity)
                .message(message)
                .file("src/main/java/TestClass.java")
                .line(10)
                .column(5)
                .rule("test-rule")
                .linter("test-linter")
                .autoFixable(false)
                .build();
    }
}
