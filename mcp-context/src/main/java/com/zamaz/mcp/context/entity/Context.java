package com.zamaz.mcp.context.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a conversation context for an organization.
 * Supports multi-tenant isolation and context versioning.
 */
@Entity
@Table(name = "contexts", indexes = {
    @Index(name = "idx_context_org_id", columnList = "organization_id"),
    @Index(name = "idx_context_user_id", columnList = "user_id"),
    @Index(name = "idx_context_created_at", columnList = "created_at"),
    @Index(name = "idx_context_last_accessed", columnList = "last_accessed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Context {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ContextStatus status = ContextStatus.ACTIVE;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
    
    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    
    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("version DESC")
    @Builder.Default
    private List<ContextVersion> versions = new ArrayList<>();
    
    @Column(name = "total_tokens")
    @Builder.Default
    private Integer totalTokens = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Column(name = "last_accessed_at", nullable = false)
    @Builder.Default
    private Instant lastAccessedAt = Instant.now();
    
    @Version
    private Long version;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        lastAccessedAt = Instant.now();
    }
    
    public void addMessage(Message message) {
        messages.add(message);
        message.setContext(this);
        totalTokens += message.getTokenCount();
    }
    
    public enum ContextStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}