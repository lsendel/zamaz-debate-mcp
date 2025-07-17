package com.zamaz.mcp.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "repository_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false, unique = true)
    private Long repositoryId;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "review_depth", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewDepth reviewDepth;

    @Column(name = "auto_fix_enabled", nullable = false)
    private boolean autoFixEnabled;

    @Column(name = "comment_style", nullable = false)
    @Enumerated(EnumType.STRING)
    private CommentStyle commentStyle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ReviewDepth {
        BASIC, STANDARD, THOROUGH
    }

    public enum CommentStyle {
        CONCISE, EDUCATIONAL, DETAILED
    }
}