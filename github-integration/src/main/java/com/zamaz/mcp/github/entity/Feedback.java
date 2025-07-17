package com.zamaz.mcp.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_issue_id", nullable = false)
    private ReviewIssue reviewIssue;

    @Column(name = "feedback_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum FeedbackType {
        HELPFUL, NOT_HELPFUL, INCORRECT, SPAM, OTHER
    }
}