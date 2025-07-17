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
 * Developer skill assessment entity for tracking individual skills and learning progress
 */
@Entity
@Table(name = "developer_skill_assessment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class DeveloperSkillAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "skill_category", nullable = false)
    private String skillCategory;

    @Column(name = "skill_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private SkillLevel skillLevel;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "evidence_count")
    private Integer evidenceCount;

    @Column(name = "last_demonstration_date")
    private LocalDateTime lastDemonstrationDate;

    @Column(name = "improvement_trend")
    @Enumerated(EnumType.STRING)
    private ImprovementTrend improvementTrend;

    @Type(type = "list-array")
    @Column(name = "learning_goals", columnDefinition = "text[]")
    private List<String> learningGoals;

    @Type(type = "list-array")
    @Column(name = "recommended_resources", columnDefinition = "text[]")
    private List<String> recommendedResources;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", insertable = false, updatable = false)
    private DeveloperProfile developer;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (evidenceCount == null) evidenceCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SkillLevel {
        NOVICE("novice"),
        COMPETENT("competent"),
        PROFICIENT("proficient"),
        EXPERT("expert");

        private final String value;

        SkillLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ImprovementTrend {
        IMPROVING("improving"),
        STABLE("stable"),
        DECLINING("declining");

        private final String value;

        ImprovementTrend(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}