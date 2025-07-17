package com.zamaz.mcp.github.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historical metrics for pull requests to track patterns, trends, and performance
 */
@Entity
@Table(name = "pr_historical_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PRHistoricalMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "pr_author_id", nullable = false)
    private Long prAuthorId;

    @Column(name = "pr_size", nullable = false)
    @Enumerated(EnumType.STRING)
    private PRSize prSize;

    @Column(name = "complexity_score", precision = 5, scale = 2)
    private BigDecimal complexityScore;

    @Column(name = "test_coverage_change", precision = 5, scale = 2)
    private BigDecimal testCoverageChange;

    @Column(name = "code_quality_score", precision = 5, scale = 2)
    private BigDecimal codeQualityScore;

    @Column(name = "review_turnaround_hours")
    private Integer reviewTurnaroundHours;

    @Column(name = "merge_time_hours")
    private Integer mergeTimeHours;

    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "approval_count")
    private Integer approvalCount;

    @Column(name = "change_request_count")
    private Integer changeRequestCount;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "lines_added")
    private Integer linesAdded;

    @Column(name = "lines_deleted")
    private Integer linesDeleted;

    @Column(name = "commit_count")
    private Integer commitCount;

    @Column(name = "is_hotfix")
    private Boolean isHotfix;

    @Column(name = "is_feature")
    private Boolean isFeature;

    @Column(name = "is_refactor")
    private Boolean isRefactor;

    @Column(name = "is_bugfix")
    private Boolean isBugfix;

    @Column(name = "merge_conflicts")
    private Boolean mergeConflicts;

    @Column(name = "ci_failures")
    private Integer ciFailures;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_author_id", referencedColumnName = "github_user_id", insertable = false, updatable = false)
    private DeveloperProfile author;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (commentCount == null) commentCount = 0;
        if (approvalCount == null) approvalCount = 0;
        if (changeRequestCount == null) changeRequestCount = 0;
        if (filesChanged == null) filesChanged = 0;
        if (linesAdded == null) linesAdded = 0;
        if (linesDeleted == null) linesDeleted = 0;
        if (commitCount == null) commitCount = 0;
        if (isHotfix == null) isHotfix = false;
        if (isFeature == null) isFeature = false;
        if (isRefactor == null) isRefactor = false;
        if (isBugfix == null) isBugfix = false;
        if (mergeConflicts == null) mergeConflicts = false;
        if (ciFailures == null) ciFailures = 0;
    }

    public enum PRSize {
        XS("xs"),
        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large"),
        XL("xl");

        private final String value;

        PRSize(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static PRSize fromLinesChanged(int linesChanged) {
            if (linesChanged <= 10) return XS;
            if (linesChanged <= 50) return SMALL;
            if (linesChanged <= 200) return MEDIUM;
            if (linesChanged <= 500) return LARGE;
            return XL;
        }
    }
}