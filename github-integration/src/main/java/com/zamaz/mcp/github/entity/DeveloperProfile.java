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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Developer profile entity for tracking individual developer characteristics,
 * skills, and preferences to enable personalized recommendations
 */
@Entity
@Table(name = "developer_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "json", typeClass = JsonBinaryType.class)
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class DeveloperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_username", nullable = false, unique = true)
    private String githubUsername;

    @Column(name = "github_user_id", nullable = false, unique = true)
    private Long githubUserId;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "experience_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Type(type = "list-array")
    @Column(name = "primary_languages", columnDefinition = "text[]")
    private List<String> primaryLanguages;

    @Type(type = "list-array")
    @Column(name = "domain_expertise", columnDefinition = "text[]")
    private List<String> domainExpertise;

    @Column(name = "communication_style", nullable = false)
    @Enumerated(EnumType.STRING)
    private CommunicationStyle communicationStyle;

    @Type(type = "json")
    @Column(name = "learning_preferences", columnDefinition = "jsonb")
    private Map<String, Object> learningPreferences;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (experienceLevel == null) {
            experienceLevel = ExperienceLevel.INTERMEDIATE;
        }
        if (communicationStyle == null) {
            communicationStyle = CommunicationStyle.STANDARD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ExperienceLevel {
        BEGINNER("beginner"),
        INTERMEDIATE("intermediate"),
        ADVANCED("advanced"),
        EXPERT("expert");

        private final String value;

        ExperienceLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum CommunicationStyle {
        CONCISE("concise"),
        DETAILED("detailed"),
        EDUCATIONAL("educational"),
        STANDARD("standard");

        private final String value;

        CommunicationStyle(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}