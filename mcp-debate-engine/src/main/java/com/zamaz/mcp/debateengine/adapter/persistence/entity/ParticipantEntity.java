package com.zamaz.mcp.debateengine.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for participants.
 */
@Entity
@Table(name = "participants", indexes = {
    @Index(name = "idx_participant_debate", columnList = "debate_id"),
    @Index(name = "idx_participant_user", columnList = "user_id")
})
public class ParticipantEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private DebateEntity debate;
    
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;
    
    @Column(name = "participant_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ParticipantTypeEnum participantType;
    
    @Column(name = "position", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PositionEnum position;
    
    @Column(name = "model_provider", length = 50)
    private String modelProvider;
    
    @Column(name = "model_name", length = 100)
    private String modelName;
    
    @Column(name = "model_config", columnDefinition = "jsonb")
    private String modelConfig;
    
    @Column(name = "total_responses")
    private Integer totalResponses;
    
    @Column(name = "avg_response_time")
    private Long avgResponseTime;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (totalResponses == null) {
            totalResponses = 0;
        }
    }
    
    // Constructors
    public ParticipantEntity() {}
    
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
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public ParticipantTypeEnum getParticipantType() {
        return participantType;
    }
    
    public void setParticipantType(ParticipantTypeEnum participantType) {
        this.participantType = participantType;
    }
    
    public PositionEnum getPosition() {
        return position;
    }
    
    public void setPosition(PositionEnum position) {
        this.position = position;
    }
    
    public String getModelProvider() {
        return modelProvider;
    }
    
    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getModelConfig() {
        return modelConfig;
    }
    
    public void setModelConfig(String modelConfig) {
        this.modelConfig = modelConfig;
    }
    
    public Integer getTotalResponses() {
        return totalResponses;
    }
    
    public void setTotalResponses(Integer totalResponses) {
        this.totalResponses = totalResponses;
    }
    
    public Long getAvgResponseTime() {
        return avgResponseTime;
    }
    
    public void setAvgResponseTime(Long avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public LocalDateTime getLeftAt() {
        return leftAt;
    }
    
    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }
    
    /**
     * Participant type enum for JPA.
     */
    public enum ParticipantTypeEnum {
        HUMAN,
        AI
    }
    
    /**
     * Position enum for JPA.
     */
    public enum PositionEnum {
        PRO,
        CON,
        MODERATOR,
        JUDGE,
        OBSERVER
    }
}