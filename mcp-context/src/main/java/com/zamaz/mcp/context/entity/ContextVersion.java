package com.zamaz.mcp.context.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a versioned snapshot of a context.
 * Used for maintaining history and enabling rollback.
 */
@Entity
@Table(name = "context_versions", indexes = {
    @Index(name = "idx_version_context_id", columnList = "context_id"),
    @Index(name = "idx_version_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ContextVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_id", nullable = false)
    private Context context;
    
    @Column(nullable = false)
    private Integer version;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<MessageSnapshot> messages;
    
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    
    /**
     * Lightweight representation of a message for versioning.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageSnapshot {
        private UUID id;
        private String role;
        private String content;
        private Integer tokenCount;
        private Instant timestamp;
        private Map<String, Object> metadata;
    }
}