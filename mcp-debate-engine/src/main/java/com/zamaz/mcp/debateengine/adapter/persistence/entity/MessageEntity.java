package com.zamaz.mcp.debateengine.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for messages.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_context", columnList = "context_id"),
    @Index(name = "idx_message_round", columnList = "round_id"),
    @Index(name = "idx_message_participant", columnList = "participant_id")
})
public class MessageEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_id", nullable = false)
    private ContextEntity context;
    
    @Column(name = "role", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MessageRoleEnum role;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private RoundEntity round;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private ParticipantEntity participant;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (isHidden == null) {
            isHidden = false;
        }
    }
    
    // Constructors
    public MessageEntity() {}
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public ContextEntity getContext() {
        return context;
    }
    
    public void setContext(ContextEntity context) {
        this.context = context;
    }
    
    public MessageRoleEnum getRole() {
        return role;
    }
    
    public void setRole(MessageRoleEnum role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public Boolean getIsHidden() {
        return isHidden;
    }
    
    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }
    
    public RoundEntity getRound() {
        return round;
    }
    
    public void setRound(RoundEntity round) {
        this.round = round;
    }
    
    public ParticipantEntity getParticipant() {
        return participant;
    }
    
    public void setParticipant(ParticipantEntity participant) {
        this.participant = participant;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Message role enum for JPA.
     */
    public enum MessageRoleEnum {
        SYSTEM,
        USER,
        ASSISTANT,
        MODERATOR
    }
}