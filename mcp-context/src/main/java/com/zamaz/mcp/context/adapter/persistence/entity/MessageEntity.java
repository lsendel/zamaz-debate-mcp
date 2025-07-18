package com.zamaz.mcp.context.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for persisting Message entity.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_context", columnList = "context_id"),
    @Index(name = "idx_message_timestamp", columnList = "timestamp"),
    @Index(name = "idx_message_hidden", columnList = "hidden")
})
public class MessageEntity {
    
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_id", nullable = false)
    private ContextEntity context;
    
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageRoleEntity role;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "hidden", nullable = false)
    private Boolean hidden = false;
    
    // Constructors
    public MessageEntity() {
    }
    
    public MessageEntity(String id, MessageRoleEntity role, String content, Integer tokenCount) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
        this.timestamp = Instant.now();
        this.hidden = false;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public ContextEntity getContext() {
        return context;
    }
    
    public void setContext(ContextEntity context) {
        this.context = context;
    }
    
    public MessageRoleEntity getRole() {
        return role;
    }
    
    public void setRole(MessageRoleEntity role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getHidden() {
        return hidden;
    }
    
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (hidden == null) {
            hidden = false;
        }
    }
}