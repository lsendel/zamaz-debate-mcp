package com.zamaz.mcp.github.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import java.util.Map;

/**
 * Entity for storing personalized suggestions for developers
 */
@Entity
@Table(name = "personalized_suggestions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "json", typeClass = JsonBinaryType.class)
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class PersonalizedSuggestions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "suggestion_type", nullable = false)
    private String suggestionType;

    @Column(name = "suggestion_title", nullable = false)
    private String suggestionTitle;

    @Column(name = "suggestion_content", nullable = false, columnDefinition = "TEXT")
    private String suggestionContent;

    @Type(type = "json")
    @Column(name = "context_data", columnDefinition = "jsonb")
    private Map<String, Object> contextData;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "priority_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private PriorityLevel priorityLevel;

    @Type(type = "list-array")
    @Column(name = "trigger_conditions", columnDefinition = "text[]")
    private List<String> triggerConditions;

    @Type(type = "json")
    @Column(name = "success_metrics", columnDefinition = "jsonb")
    private Map<String, Object> successMetrics;

    @Column(name = "is_accepted")
    private Boolean isAccepted;

    @Column(name = "acceptance_date")
    private LocalDateTime acceptanceDate;

    @Column(name = "effectiveness_rating")
    private Integer effectivenessRating;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PriorityLevel {
        HIGH("high"),
        MEDIUM("medium"),
        LOW("low");

        private final String value;

        PriorityLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}