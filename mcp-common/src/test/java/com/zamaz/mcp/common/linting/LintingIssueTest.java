package com.zamaz.mcp.common.linting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for LintingIssue.
 */
class LintingIssueTest {

    @Test
    void testLintingIssueBuilder() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "style");
        metadata.put("fixable", true);

        // When
        LintingIssue issue = LintingIssue.builder()
                .id("TEST-001")
                .severity(LintingSeverity.WARNING)
                .message("Test warning message")
                .file("src/main/java/TestClass.java")
                .line(42)
                .column(10)
                .rule("test-rule")
                .linter("test-linter")
                .autoFixable(true)
                .suggestion("Fix suggestion")
                .metadata(metadata)
                .build();

        // Then
        assertEquals("TEST-001", issue.getId());
        assertEquals(LintingSeverity.WARNING, issue.getSeverity());
        assertEquals("Test warning message", issue.getMessage());
        assertEquals("src/main/java/TestClass.java", issue.getFile());
        assertEquals(42, issue.getLine());
        assertEquals(10, issue.getColumn());
        assertEquals("test-rule", issue.getRule());
        assertEquals("test-linter", issue.getLinter());
        assertTrue(issue.isAutoFixable());
        assertEquals("Fix suggestion", issue.getSuggestion());
        assertEquals(metadata, issue.getMetadata());
    }

    @Test
    void testLintingIssueDefaults() {
        // When
        LintingIssue issue = LintingIssue.builder()
                .id("TEST-002")
                .severity(LintingSeverity.ERROR)
                .message("Test error message")
                .file("TestFile.java")
                .line(1)
                .column(1)
                .rule("error-rule")
                .linter("error-linter")
                .build();

        // Then
        assertNotNull(issue.getId());
        assertEquals(LintingSeverity.ERROR, issue.getSeverity());
        assertNotNull(issue.getMessage());
        assertNotNull(issue.getFile());
        assertEquals(1, issue.getLine());
        assertEquals(1, issue.getColumn());
        assertNotNull(issue.getRule());
        assertNotNull(issue.getLinter());
        assertFalse(issue.isAutoFixable()); // Default should be false
        assertNull(issue.getSuggestion());
        assertNull(issue.getMetadata());
    }

    @Test
    void testSeverityLevels() {
        // Test all severity levels
        LintingSeverity[] severities = {
                LintingSeverity.ERROR,
                LintingSeverity.WARNING,
                LintingSeverity.INFO,
                LintingSeverity.SUGGESTION
        };

        for (LintingSeverity severity : severities) {
            LintingIssue issue = LintingIssue.builder()
                    .id("TEST-" + severity.name())
                    .severity(severity)
                    .message("Test message")
                    .file("TestFile.java")
                    .line(1)
                    .column(1)
                    .rule("test-rule")
                    .linter("test-linter")
                    .build();

            assertEquals(severity, issue.getSeverity());
        }
    }

    @Test
    void testMetadataHandling() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "performance");
        metadata.put("impact", "high");
        metadata.put("effort", "low");
        metadata.put("tags", new String[] { "optimization", "memory" });

        // When
        LintingIssue issue = LintingIssue.builder()
                .id("PERF-001")
                .severity(LintingSeverity.WARNING)
                .message("Performance issue detected")
                .file("SlowClass.java")
                .line(100)
                .column(5)
                .rule("performance-rule")
                .linter("performance-linter")
                .metadata(metadata)
                .build();

        // Then
        assertNotNull(issue.getMetadata());
        assertEquals("performance", issue.getMetadata().get("category"));
        assertEquals("high", issue.getMetadata().get("impact"));
        assertEquals("low", issue.getMetadata().get("effort"));
        assertArrayEquals(new String[] { "optimization", "memory" },
                (String[]) issue.getMetadata().get("tags"));
    }
}
