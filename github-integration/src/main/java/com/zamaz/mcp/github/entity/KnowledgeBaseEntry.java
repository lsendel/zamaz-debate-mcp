package com.zamaz.mcp.github.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Knowledge base entry for storing team learning and best practices
 */
@Entity
@Table(name = "knowledge_base_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class KnowledgeBaseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Type(type = "list-array")
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "frequency_count")
    private Integer frequencyCount;

    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    @Type(type = "list-array")
    @Column(name = "source_review_ids", columnDefinition = "bigint[]")
    private List<Long> sourceReviewIds;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "language")
    private String language;

    @Column(name = "framework")
    private String framework;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "github_user_id", insertable = false, updatable = false)
    private DeveloperProfile createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", referencedColumnName = "github_user_id", insertable = false, updatable = false)
    private DeveloperProfile approvedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (frequencyCount == null) frequencyCount = 1;
        if (isApproved == null) isApproved = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Severity {
        CRITICAL("critical"),
        HIGH("high"),
        MEDIUM("medium"),
        LOW("low");

        private final String value;

        Severity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}