package com.zamaz.mcp.common.linting.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zamaz.mcp.common.linting.LintingIssue;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.LintingSeverity;
import com.zamaz.mcp.common.linting.impl.DefaultLintingResult;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for LintingMetricsCollector.
 */
class LintingMetricsCollectorTest {

    private LintingMetricsCollector metricsCollector;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new LintingMetricsCollector(meterRegistry);
    }

    @Test
    void testRecordLintingResult() {
        // Given
        LintingResult result = createTestLintingResult();
        String serviceName = "test-service";

        // When
        metricsCollector.recordLintingResult(result, serviceName);

        // Then
        // Verify counters were incremented
        assertEquals(1.0, meterRegistry.counter("linting.runs.total").count());
        assertEquals(3.0, meterRegistry.counter("linting.issues.total").count());
        assertEquals(1.0, meterRegistry.counter("linting.issues.errors").count());
        assertEquals(2.0, meterRegistry.counter("linting.issues.warnings").count());

        // Verify metrics were recorded
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();
        assertEquals(1, metrics.getTotalRuns());
        assertEquals(5, metrics.getTotalFilesProcessed());
        assertTrue(metrics.getCurrentQualityScore() > 0);
    }

    @Test
    void testRecordMultipleLintingResults() {
        // Given
        LintingResult result1 = createTestLintingResult();
        LintingResult result2 = createTestLintingResult();

        // When
        metricsCollector.recordLintingResult(result1, "service-1");
        metricsCollector.recordLintingResult(result2, "service-2");

        // Then
        assertEquals(2.0, meterRegistry.counter("linting.runs.total").count());
        assertEquals(6.0, meterRegistry.counter("linting.issues.total").count());

        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();
        assertEquals(2, metrics.getTotalRuns());
        assertEquals(10, metrics.getTotalFilesProcessed());
    }

    @Test
    void testRecordAutoFixApplied() {
        // Given
        int fixedIssuesCount = 5;
        String serviceName = "test-service";

        // When
        metricsCollector.recordAutoFixApplied(fixedIssuesCount, serviceName);

        // Then
        // Verify the counter was created and incremented
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().equals("linting.autofix.applied")));
    }

    @Test
    void testRecordConfigurationChange() {
        // Given
        String configurationType = "checkstyle";
        String serviceName = "test-service";

        // When
        metricsCollector.recordConfigurationChange(configurationType, serviceName);

        // Then
        // Verify the counter was created
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().equals("linting.config.changes")));
    }

    @Test
    void testGetCurrentQualityMetrics() {
        // Given
        LintingResult result = createTestLintingResult();
        metricsCollector.recordLintingResult(result, "test-service");

        // When
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();

        // Then
        assertNotNull(metrics);
        assertEquals(1, metrics.getTotalRuns());
        assertEquals(5, metrics.getTotalFilesProcessed());
        assertTrue(metrics.getCurrentQualityScore() >= 0);
        assertTrue(metrics.getCurrentQualityScore() <= 100);
        assertNotNull(metrics.getTimestamp());

        // Check service-specific metrics
        assertNotNull(metrics.getIssueCountsByService());
        assertTrue(metrics.getIssueCountsByService().containsKey("test-service"));

        // Check severity metrics
        assertNotNull(metrics.getIssueCountsBySeverity());
        assertTrue(metrics.getIssueCountsBySeverity().containsKey("ERROR"));
        assertTrue(metrics.getIssueCountsBySeverity().containsKey("WARNING"));

        // Check linter metrics
        assertNotNull(metrics.getIssueCountsByLinter());
        assertTrue(metrics.getIssueCountsByLinter().containsKey("test-linter"));
    }

    @Test
    void testResetMetrics() {
        // Given
        LintingResult result = createTestLintingResult();
        metricsCollector.recordLintingResult(result, "test-service");

        // Verify metrics are recorded
        LintingMetricsCollector.QualityMetrics beforeReset = metricsCollector.getCurrentQualityMetrics();
        assertEquals(1, beforeReset.getTotalRuns());

        // When
        metricsCollector.resetMetrics();

        // Then
        LintingMetricsCollector.QualityMetrics afterReset = metricsCollector.getCurrentQualityMetrics();
        assertEquals(0, afterReset.getTotalRuns());
        assertEquals(0, afterReset.getTotalFilesProcessed());
        assertEquals(100, afterReset.getCurrentQualityScore()); // Should reset to default
        assertTrue(afterReset.getIssueCountsByService().isEmpty());
        assertTrue(afterReset.getIssueCountsBySeverity().isEmpty());
        assertTrue(afterReset.getIssueCountsByLinter().isEmpty());
    }

    @Test
    void testMetricsAggregation() {
        // Given
        LintingResult result1 = createTestLintingResult("service-1", 2, 1, 1);
        LintingResult result2 = createTestLintingResult("service-2", 3, 2, 1);
        LintingResult result3 = createTestLintingResult("service-1", 1, 0, 1);

        // When
        metricsCollector.recordLintingResult(result1, "service-1");
        metricsCollector.recordLintingResult(result2, "service-2");
        metricsCollector.recordLintingResult(result3, "service-1");

        // Then
        LintingMetricsCollector.QualityMetrics metrics = metricsCollector.getCurrentQualityMetrics();

        // Total metrics
        assertEquals(3, metrics.getTotalRuns());

        // Service-specific metrics
        assertEquals(3, metrics.getIssueCountsByService().get("service-1").get()); // 2 + 1
        assertEquals(3, metrics.getIssueCountsByService().get("service-2").get());

        // Severity metrics
        assertEquals(3, metrics.getIssueCountsBySeverity().get("ERROR").get()); // 1 + 2 + 0
        assertEquals(3, metrics.getIssueCountsBySeverity().get("WARNING").get()); // 1 + 1 + 1
    }

    @Test
    void testTimerMetrics() {
        // Given
        LintingResult result = createTestLintingResult();

        // When
        metricsCollector.recordLintingResult(result, "test-service");

        // Then
        // Verify timer was recorded
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().equals("linting.duration")));
    }

    @Test
    void testGaugeMetrics() {
        // Given
        LintingResult result = createTestLintingResult();

        // When
        metricsCollector.recordLintingResult(result, "test-service");

        // Then
        // Verify gauges exist
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().equals("linting.quality.score")));
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().equals("linting.files.processed.total")));
    }

    private LintingResult createTestLintingResult() {
        return createTestLintingResult("unknown", 3, 1, 2);
    }

    private LintingResult createTestLintingResult(String serviceName, int totalIssues, int errors, int warnings) {
        List<LintingIssue> issues = new ArrayList<>();

        // Add error issues
        for (int i = 0; i < errors; i++) {
            issues.add(createTestIssue("ERR-" + i, LintingSeverity.ERROR));
        }

        // Add warning issues
        for (int i = 0; i < warnings; i++) {
            issues.add(createTestIssue("WARN-" + i, LintingSeverity.WARNING));
        }

        // Add remaining issues as INFO
        int remaining = totalIssues - errors - warnings;
        for (int i = 0; i < remaining; i++) {
            issues.add(createTestIssue("INFO-" + i, LintingSeverity.INFO));
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalFiles", 5);
        metrics.put("totalIssues", totalIssues);
        metrics.put("errorCount", errors);
        metrics.put("warningCount", warnings);
        metrics.put("qualityScore", 85.0);

        return new DefaultLintingResult(
                issues,
                metrics,
                LocalDateTime.now(),
                5,
                1500L,
                errors == 0);
    }

    private LintingIssue createTestIssue(String id, LintingSeverity severity) {
        return LintingIssue.builder()
                .id(id)
                .severity(severity)
                .message("Test message")
                .file("TestFile.java")
                .line(1)
                .column(1)
                .rule("test-rule")
                .linter("test-linter")
                .autoFixable(severity != LintingSeverity.ERROR)
                .build();
    }
}
