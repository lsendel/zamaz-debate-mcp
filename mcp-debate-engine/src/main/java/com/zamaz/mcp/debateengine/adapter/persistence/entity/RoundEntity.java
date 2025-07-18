package com.zamaz.mcp.debateengine.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for rounds.
 */
@Entity
@Table(name = "rounds", indexes = {
    @Index(name = "idx_round_debate", columnList = "debate_id"),
    @Index(name = "idx_round_number", columnList = "round_number")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"debate_id", "round_number"})
})
public class RoundEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private DebateEntity debate;
    
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RoundStatusEnum status;
    
    @Column(name = "timeout_ms", nullable = false)
    private Long timeoutMs;
    
    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseEntity> responses = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Constructors
    public RoundEntity() {}
    
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
    
    public Integer getRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    public RoundStatusEnum getStatus() {
        return status;
    }
    
    public void setStatus(RoundStatusEnum status) {
        this.status = status;
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public String getPromptTemplate() {
        return promptTemplate;
    }
    
    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
    
    public List<ResponseEntity> getResponses() {
        return responses;
    }
    
    public void setResponses(List<ResponseEntity> responses) {
        this.responses = responses;
    }
    
    /**
     * Round status enum for JPA.
     */
    public enum RoundStatusEnum {
        PENDING,
        ACTIVE,
        COMPLETED,
        TIMEOUT
    }
}