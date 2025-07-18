package com.zamaz.mcp.context.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

/**
 * JPA entity for persisting Context aggregate.
 */
@Entity
@Table(name = "contexts", indexes = {
    @Index(name = "idx_context_org_id", columnList = "organization_id"),
    @Index(name = "idx_context_org_user", columnList = "organization_id, user_id"),
    @Index(name = "idx_context_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_context_updated", columnList = "updated_at")
})
public class ContextEntity {
    
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
    
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ContextStatusEntity status;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens = 0;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<MessageEntity> messages = new ArrayList<>();
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Constructors
    public ContextEntity() {
    }
    
    public ContextEntity(String id, String organizationId, String userId, String name) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.name = name;
        this.status = ContextStatusEntity.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ContextStatusEntity getStatus() {
        return status;
    }
    
    public void setStatus(ContextStatusEntity status) {
        this.status = status;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Integer getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<MessageEntity> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageEntity> messages) {
        this.messages = messages;
    }
    
    public void addMessage(MessageEntity message) {
        messages.add(message);
        message.setContext(this);
    }
    
    public void removeMessage(MessageEntity message) {
        messages.remove(message);
        message.setContext(null);
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}