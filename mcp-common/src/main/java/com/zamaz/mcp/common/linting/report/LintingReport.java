package com.zamaz.mcp.common.linting.report;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.zamaz.mcp.common.linting.LintingIssue;

/**
 * Comprehensive linting report with detailed metrics and analysis.
 */
public class LintingReport {

    private final String projectName;
    private final LocalDateTime timestamp;
    private final LintingSummary summary;
    private final List<LintingIssue> issues;
    private final Map<String, ServiceReport> serviceReports;
    private final QualityMetrics metrics;
    private final List<String> recommendations;

    private LintingReport(Builder builder) {
        this.projectName = builder.projectName;
        this.timestamp = builder.timestamp;
        this.summary = builder.summary;
        this.issues = builder.issues;
        this.serviceReports = builder.serviceReports;
        this.metrics = builder.metrics;
        this.recommendations = builder.recommendations;
    }

    public String getProjectName() {
        return projectName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LintingSummary getSummary() {
        return summary;
    }

    public List<LintingIssue> getIssues() {
        return issues;
    }

    public Map<String, ServiceReport> getServiceReports() {
        return serviceReports;
    }

    public QualityMetrics getMetrics() {
        return metrics;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String projectName;
        private LocalDateTime timestamp;
        private LintingSummary summary;
        private List<LintingIssue> issues;
        private Map<String, ServiceReport> serviceReports;
        private QualityMetrics metrics;
        private List<String> recommendations;

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder summary(LintingSummary summary) {
            this.summary = summary;
            return this;
        }

        public Builder issues(List<LintingIssue> issues) {
            this.issues = issues;
            return this;
        }

        public Builder serviceReports(Map<String, ServiceReport> serviceReports) {
            this.serviceReports = serviceReports;
            return this;
        }

        public Builder metrics(QualityMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public LintingReport build() {
            return new LintingReport(this);
        }
    }

    public static class LintingSummary {
        private final int totalFiles;
        private final int totalIssues;
        private final int errorCount;
        private final int warningCount;
        private final int infoCount;
        private final int suggestionCount;
        private final double qualityScore;
        private final boolean passed;

        public LintingSummary(int totalFiles, int totalIssues, int errorCount,
                int warningCount, int infoCount, int suggestionCount,
                double qualityScore, boolean passed) {
            this.totalFiles = totalFiles;
            this.totalIssues = totalIssues;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.infoCount = infoCount;
            this.suggestionCount = suggestionCount;
            this.qualityScore = qualityScore;
            this.passed = passed;
        }

        // Getters
        public int getTotalFiles() {
            return totalFiles;
        }

        public int getTotalIssues() {
            return totalIssues;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public int getInfoCount() {
            return infoCount;
        }

        public int getSuggestionCount() {
            return suggestionCount;
        }

        public double getQualityScore() {
            return qualityScore;
        }

        public boolean isPassed() {
            return passed;
        }
    }

    public static class ServiceReport {
        private final String serviceName;
        private final int fileCount;
        private final List<LintingIssue> issues;
        private final Map<String, Integer> issuesByLinter;
        private final double qualityScore;

        public ServiceReport(String serviceName, int fileCount, List<LintingIssue> issues,
                Map<String, Integer> issuesByLinter, double qualityScore) {
            this.serviceName = serviceName;
            this.fileCount = fileCount;
            this.issues = issues;
            this.issuesByLinter = issuesByLinter;
            this.qualityScore = qualityScore;
        }

        // Getters
        public String getServiceName() {
            return serviceName;
        }

        public int getFileCount() {
            return fileCount;
        }

        public List<LintingIssue> getIssues() {
            return issues;
        }

        public Map<String, Integer> getIssuesByLinter() {
            return issuesByLinter;
        }

        public double getQualityScore() {
            return qualityScore;
        }
    }

    public static class QualityMetrics {
        private final double codeQualityScore;
        private final Map<String, Double> scoresByService;
        private final Map<String, Integer> issuesByType;
        private final Map<String, Integer> issuesByLinter;
        private final Map<String, Double> coverageByModule;
        private final int autoFixableCount;
        private final List<String> topIssues;

        public QualityMetrics(double codeQualityScore, Map<String, Double> scoresByService,
                Map<String, Integer> issuesByType, Map<String, Integer> issuesByLinter,
                Map<String, Double> coverageByModule, int autoFixableCount,
                List<String> topIssues) {
            this.codeQualityScore = codeQualityScore;
            this.scoresByService = scoresByService;
            this.issuesByType = issuesByType;
            this.issuesByLinter = issuesByLinter;
            this.coverageByModule = coverageByModule;
            this.autoFixableCount = autoFixableCount;
            this.topIssues = topIssues;
        }

        // Getters
        public double getCodeQualityScore() {
            return codeQualityScore;
        }

        public Map<String, Double> getScoresByService() {
            return scoresByService;
        }

        public Map<String, Integer> getIssuesByType() {
            return issuesByType;
        }

        public Map<String, Integer> getIssuesByLinter() {
            return issuesByLinter;
        }

        public Map<String, Double> getCoverageByModule() {
            return coverageByModule;
        }

        public int getAutoFixableCount() {
            return autoFixableCount;
        }

        public List<String> getTopIssues() {
            return topIssues;
        }
    }
}
