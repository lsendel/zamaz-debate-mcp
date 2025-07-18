package com.zamaz.mcp.common.linting.metrics;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.zamaz.mcp.common.linting.LintingIssue;
import com.zamaz.mcp.common.linting.LintingResult;
import com.zamaz.mcp.common.linting.LintingSeverity;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Collects and exposes linting metrics for monitoring and dashboards.
 */
@Component
public class LintingMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> issueCountsByService = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> issueCountsBySeverity = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> issueCountsByLinter = new ConcurrentHashMap<>();
    private final AtomicLong totalLintingRuns = new AtomicLong(0);
    private final AtomicLong totalFilesProcessed = new AtomicLong(0);
    private final AtomicInteger currentQualityScore = new AtomicInteger(100);

    // Counters
    private final Counter lintingRunsCounter;
    private final Counter totalIssuesCounter;
    private final Counter errorIssuesCounter;
    private final Counter warningIssuesCounter;
    private final Counter autoFixedIssuesCounter;

    // Timers
    private final Timer lintingDurationTimer;

    public LintingMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.lintingRunsCounter = Counter.builder("linting.runs.total")
                .description("Total number of linting runs")
                .register(meterRegistry);

        this.totalIssuesCounter = Counter.builder("linting.issues.total")
                .description("Total number of linting issues found")
                .register(meterRegistry);

        this.errorIssuesCounter = Counter.builder("linting.issues.errors")
                .description("Total number of error-level linting issues")
                .register(meterRegistry);

        this.warningIssuesCounter = Counter.builder("linting.issues.warnings")
                .description("Total number of warning-level linting issues")
                .register(meterRegistry);

        this.autoFixedIssuesCounter = Counter.builder("linting.issues.autofixed")
                .description("Total number of auto-fixed linting issues")
                .register(meterRegistry);

        // Initialize timers
        this.lintingDurationTimer = Timer.builder("linting.duration")
                .description("Duration of linting operations")
                .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("linting.quality.score")
                .description("Current overall quality score (0-100)")
                .register(meterRegistry, this, LintingMetricsCollector::getCurrentQualityScore);

        Gauge.builder("linting.files.processed.total")
                .description("Total number of files processed by linting")
                .register(meterRegistry, this, LintingMetricsCollector::getTotalFilesProcessed);
    }

    /**
     * Record metrics from a linting result.
     */
    public void recordLintingResult(LintingResult result, String serviceName) {
        // Increment run counter
        lintingRunsCounter.increment();
        totalLintingRuns.incrementAndGet();

        // Record duration
        lintingDurationTimer.record(result.getDurationMs(), java.util.concurrent.TimeUnit.MILLISECONDS);

        // Record files processed
        totalFilesProcessed.addAndGet(result.getFilesProcessed());

        // Record issues by severity
        long errorCount = result.getIssuesBySeverity(LintingSeverity.ERROR).size();
        long warningCount = result.getIssuesBySeverity(LintingSeverity.WARNING).size();
        long infoCount = result.getIssuesBySeverity(LintingSeverity.INFO).size();
        long suggestionCount = result.getIssuesBySeverity(LintingSeverity.SUGGESTION).size();

        totalIssuesCounter.increment(result.getIssues().size());
        errorIssuesCounter.increment(errorCount);
        warningIssuesCounter.increment(warningCount);

        // Update severity counters
        issueCountsBySeverity.computeIfAbsent("ERROR", k -> new AtomicInteger(0)).addAndGet((int) errorCount);
        issueCountsBySeverity.computeIfAbsent("WARNING", k -> new AtomicInteger(0)).addAndGet((int) warningCount);
        issueCountsBySeverity.computeIfAbsent("INFO", k -> new AtomicInteger(0)).addAndGet((int) infoCount);
        issueCountsBySeverity.computeIfAbsent("SUGGESTION", k -> new AtomicInteger(0)).addAndGet((int) suggestionCount);

        // Update service-specific metrics
        if (serviceName != null) {
            issueCountsByService.computeIfAbsent(serviceName, k -> new AtomicInteger(0))
                    .addAndGet(result.getIssues().size());

            // Create service-specific gauges if they don't exist
            createServiceGaugeIfNeeded(serviceName);
        }

        // Record issues by linter
        for (LintingIssue issue : result.getIssues()) {
            String linter = issue.getLinter();
            issueCountsByLinter.computeIfAbsent(linter, k -> new AtomicInteger(0)).incrementAndGet();
            createLinterGaugeIfNeeded(linter);
        }

        // Record auto-fixed issues
        long autoFixableCount = result.getIssues().stream()
                .filter(LintingIssue::isAutoFixable)
                .count();
        autoFixedIssuesCounter.increment(autoFixableCount);

        // Update quality score
        Object qualityScore = result.getMetrics().get("qualityScore");
        if (qualityScore instanceof Number) {
            currentQualityScore.set(((Number) qualityScore).intValue());
        }

        // Record custom metrics
        recordCustomMetrics(result, serviceName);
    }

    /**
     * Record metrics for a successful auto-fix operation.
     */
    public void recordAutoFixApplied(int fixedIssuesCount, String serviceName) {
        Counter.builder("linting.autofix.applied")
                .description("Number of issues automatically fixed")
                .tag("service", serviceName != null ? serviceName : "unknown")
                .register(meterRegistry)
                .increment(fixedIssuesCount);
    }

    /**
     * Record metrics for linting configuration changes.
     */
    public void recordConfigurationChange(String configurationType, String serviceName) {
        Counter.builder("linting.config.changes")
                .description("Number of linting configuration changes")
                .tag("type", configurationType)
                .tag("service", serviceName != null ? serviceName : "global")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Get current quality metrics for dashboard.
     */
    public QualityMetrics getCurrentQualityMetrics() {
        return QualityMetrics.builder()
                .totalRuns(totalLintingRuns.get())
                .totalFilesProcessed(totalFilesProcessed.get())
                .currentQualityScore(currentQualityScore.get())
                .issueCountsByService(Map.copyOf(issueCountsByService))
                .issueCountsBySeverity(Map.copyOf(issueCountsBySeverity))
                .issueCountsByLinter(Map.copyOf(issueCountsByLinter))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Reset metrics (useful for testing or periodic resets).
     */
    public void resetMetrics() {
        issueCountsByService.clear();
        issueCountsBySeverity.clear();
        issueCountsByLinter.clear();
        totalLintingRuns.set(0);
        totalFilesProcessed.set(0);
        currentQualityScore.set(100);
    }

    private void recordCustomMetrics(LintingResult result, String serviceName) {
        // Record trend metrics
        String timeWindow = getCurrentTimeWindow();

        Counter.builder("linting.issues.by.timewindow")
                .description("Issues found by time window")
                .tag("timewindow", timeWindow)
                .tag("service", serviceName != null ? serviceName : "all")
                .register(meterRegistry)
                .increment(result.getIssues().size());

        // Record success rate
        Gauge.builder("linting.success.rate")
                .description("Percentage of successful linting runs")
                .tag("service", serviceName != null ? serviceName : "all")
                .register(meterRegistry, this, collector -> {
                    long totalRuns = collector.totalLintingRuns.get();
                    if (totalRuns == 0)
                        return 100.0;
                    // This is simplified - in reality you'd track failed runs
                    return result.isSuccessful() ? 100.0 : 0.0;
                });
    }

    private void createServiceGaugeIfNeeded(String serviceName) {
        String gaugeName = "linting.issues.by.service." + serviceName.replace("-", "_");

        // Check if gauge already exists (simplified check)
        try {
            Gauge.builder("linting.issues.by.service")
                    .description("Number of issues by service")
                    .tag("service", serviceName)
                    .register(meterRegistry, issueCountsByService.get(serviceName), AtomicInteger::get);
        } catch (Exception e) {
            // Gauge might already exist, ignore
        }
    }

    private void createLinterGaugeIfNeeded(String linter) {
        try {
            Gauge.builder("linting.issues.by.linter")
                    .description("Number of issues by linter")
                    .tag("linter", linter)
                    .register(meterRegistry, issueCountsByLinter.get(linter), AtomicInteger::get);
        } catch (Exception e) {
            // Gauge might already exist, ignore
        }
    }

    private String getCurrentTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d-%02d-%02d-%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }

    // Getter methods for gauges
    private double getCurrentQualityScore() {
        return currentQualityScore.get();
    }

    private double getTotalFilesProcessed() {
        return totalFilesProcessed.get();
    }

    /**
     * Quality metrics data class for dashboard consumption.
     */
    public static class QualityMetrics {
        private final long totalRuns;
        private final long totalFilesProcessed;
        private final int currentQualityScore;
        private final Map<String, AtomicInteger> issueCountsByService;
        private final Map<String, AtomicInteger> issueCountsBySeverity;
        private final Map<String, AtomicInteger> issueCountsByLinter;
        private final LocalDateTime timestamp;

        private QualityMetrics(Builder builder) {
            this.totalRuns = builder.totalRuns;
            this.totalFilesProcessed = builder.totalFilesProcessed;
            this.currentQualityScore = builder.currentQualityScore;
            this.issueCountsByService = builder.issueCountsByService;
            this.issueCountsBySeverity = builder.issueCountsBySeverity;
            this.issueCountsByLinter = builder.issueCountsByLinter;
            this.timestamp = builder.timestamp;
        }

        // Getters
        public long getTotalRuns() {
            return totalRuns;
        }

        public long getTotalFilesProcessed() {
            return totalFilesProcessed;
        }

        public int getCurrentQualityScore() {
            return currentQualityScore;
        }

        public Map<String, AtomicInteger> getIssueCountsByService() {
            return issueCountsByService;
        }

        public Map<String, AtomicInteger> getIssueCountsBySeverity() {
            return issueCountsBySeverity;
        }

        public Map<String, AtomicInteger> getIssueCountsByLinter() {
            return issueCountsByLinter;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long totalRuns;
            private long totalFilesProcessed;
            private int currentQualityScore;
            private Map<String, AtomicInteger> issueCountsByService;
            private Map<String, AtomicInteger> issueCountsBySeverity;
            private Map<String, AtomicInteger> issueCountsByLinter;
            private LocalDateTime timestamp;

            public Builder totalRuns(long totalRuns) {
                this.totalRuns = totalRuns;
                return this;
            }

            public Builder totalFilesProcessed(long totalFilesProcessed) {
                this.totalFilesProcessed = totalFilesProcessed;
                return this;
            }

            public Builder currentQualityScore(int currentQualityScore) {
                this.currentQualityScore = currentQualityScore;
                return this;
            }

            public Builder issueCountsByService(Map<String, AtomicInteger> issueCountsByService) {
                this.issueCountsByService = issueCountsByService;
                return this;
            }

            public Builder issueCountsBySeverity(Map<String, AtomicInteger> issueCountsBySeverity) {
                this.issueCountsBySeverity = issueCountsBySeverity;
                return this;
            }

            public Builder issueCountsByLinter(Map<String, AtomicInteger> issueCountsByLinter) {
                this.issueCountsByLinter = issueCountsByLinter;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public QualityMetrics build() {
                return new QualityMetrics(this);
            }
        }
    }
}
