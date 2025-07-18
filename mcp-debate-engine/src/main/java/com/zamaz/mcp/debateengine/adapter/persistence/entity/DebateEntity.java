package com.zamaz.mcp.debateengine.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for debates.
 */
@Entity
@Table(name = "debates", indexes = {
    @Index(name = "idx_debate_org", columnList = "organization_id"),
    @Index(name = "idx_debate_status", columnList = "status"),
    @Index(name = "idx_debate_created", columnList = "created_at")
})
public class DebateEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;
    
    @Column(name = "created_by_user_id", nullable = false, columnDefinition = "uuid")
    private UUID createdByUserId;
    
    @Column(name = "topic", nullable = false, length = 500)
    private String topic;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;
    
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;
    
    @Column(name = "max_rounds", nullable = false)
    private Integer maxRounds;
    
    @Column(name = "round_timeout_ms", nullable = false)
    private Long roundTimeoutMs;
    
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DebateStatusEnum status;
    
    @Column(name = "current_round")
    private Integer currentRound;
    
    @Column(name = "context_id", columnDefinition = "uuid")
    private UUID contextId;
    
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    @Column(name = "message_count")
    private Integer messageCount;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipantEntity> participants = new ArrayList<>();
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundEntity> rounds = new ArrayList<>();
    
    @OneToOne(mappedBy = "debate", cascade = CascadeType.ALL, orphanRemoval = true)
    private ContextEntity context;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (currentRound == null) {
            currentRound = 0;
        }
        if (totalTokens == null) {
            totalTokens = 0;
        }
        if (messageCount == null) {
            messageCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public DebateEntity() {}
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
    
    public UUID getCreatedByUserId() {
        return createdByUserId;
    }
    
    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
    
    public Integer getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public Integer getMaxRounds() {
        return maxRounds;
    }
    
    public void setMaxRounds(Integer maxRounds) {
        this.maxRounds = maxRounds;
    }
    
    public Long getRoundTimeoutMs() {
        return roundTimeoutMs;
    }
    
    public void setRoundTimeoutMs(Long roundTimeoutMs) {
        this.roundTimeoutMs = roundTimeoutMs;
    }
    
    public String getSettings() {
        return settings;
    }
    
    public void setSettings(String settings) {
        this.settings = settings;
    }
    
    public DebateStatusEnum getStatus() {
        return status;
    }
    
    public void setStatus(DebateStatusEnum status) {
        this.status = status;
    }
    
    public Integer getCurrentRound() {
        return currentRound;
    }
    
    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }
    
    public UUID getContextId() {
        return contextId;
    }
    
    public void setContextId(UUID contextId) {
        this.contextId = contextId;
    }
    
    public Integer getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
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
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public List<ParticipantEntity> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<ParticipantEntity> participants) {
        this.participants = participants;
    }
    
    public List<RoundEntity> getRounds() {
        return rounds;
    }
    
    public void setRounds(List<RoundEntity> rounds) {
        this.rounds = rounds;
    }
    
    public ContextEntity getContext() {
        return context;
    }
    
    public void setContext(ContextEntity context) {
        this.context = context;
    }
    
    /**
     * Status enum for JPA.
     */
    public enum DebateStatusEnum {
        DRAFT,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
}