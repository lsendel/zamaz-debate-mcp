package com.zamaz.mcp.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private PullRequestReview review;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "line_start", nullable = false)
    private Integer lineStart;

    @Column(name = "line_end", nullable = false)
    private Integer lineEnd;

    @Column(name = "issue_type", nullable = false)
    private String issueType;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueSeverity severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "auto_fixable", nullable = false)
    private boolean autoFixable;

    @Column(name = "fix_description", columnDefinition = "TEXT")
    private String fixDescription;

    @Column(name = "comment_id")
    private Long commentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "reviewIssue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Feedback> feedback = new ArrayList<>();

    public enum IssueSeverity {
        CRITICAL, MAJOR, MINOR, SUGGESTION
    }
}