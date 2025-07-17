package com.zamaz.mcp.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pull_request_review")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "pr_title", nullable = false)
    private String prTitle;

    @Column(name = "pr_author", nullable = false)
    private String prAuthor;

    @Column(name = "base_branch", nullable = false)
    private String baseBranch;

    @Column(name = "head_branch", nullable = false)
    private String headBranch;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "files_reviewed")
    private Integer filesReviewed;

    @Column(name = "lines_reviewed")
    private Integer linesReviewed;

    @Column(name = "critical_issues")
    private Integer criticalIssues;

    @Column(name = "major_issues")
    private Integer majorIssues;

    @Column(name = "minor_issues")
    private Integer minorIssues;

    @Column(name = "suggestions")
    private Integer suggestions;

    @Column(name = "auto_fixable")
    private Integer autoFixable;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewIssue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewComment> comments = new ArrayList<>();

    public enum ReviewStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}