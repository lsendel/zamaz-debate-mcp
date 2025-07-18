package com.zamaz.mcp.debateengine.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for contexts.
 */
@Entity
@Table(name = "contexts", indexes = {
    @Index(name = "idx_context_org", columnList = "organization_id"),
    @Index(name = "idx_context_user", columnList = "user_id"),
    @Index(name = "idx_context_status", columnList = "status")
})
public class ContextEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", unique = true)
    private DebateEntity debate;
    
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;
    
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ContextStatusEnum status;
    
    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;
    
    @Column(name = "max_tokens", nullable = false)
    private Integer maxTokens;
    
    @Column(name = "message_count", nullable = false)
    private Integer messageCount;
    
    @Column(name = "window_size", nullable = false)
    private Integer windowSize;
    
    @Column(name = "version", nullable = false)
    private Integer version;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<MessageEntity> messages = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = LocalDateTime.now();
        }
        if (totalTokens == null) {
            totalTokens = 0;
        }
        if (messageCount == null) {
            messageCount = 0;
        }
        if (version == null) {
            version = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public ContextEntity() {}
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public DebateEntity getDebate() {
        return debate;
    }
    
    public void setDebate(DebateEntity debate) {
        this.debate = debate;
    }
    
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ContextStatusEnum getStatus() {
        return status;
    }
    
    public void setStatus(ContextStatusEnum status) {
        this.status = status;
    }
    
    public Integer getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public Integer getWindowSize() {
        return windowSize;
    }
    
    public void setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    public List<MessageEntity> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageEntity> messages) {
        this.messages = messages;
    }
    
    /**
     * Context status enum for JPA.
     */
    public enum ContextStatusEnum {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}