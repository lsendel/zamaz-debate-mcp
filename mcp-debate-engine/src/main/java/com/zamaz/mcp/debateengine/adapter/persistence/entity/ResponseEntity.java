package com.zamaz.mcp.debateengine.adapter.persistence.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for responses.
 */
@Entity
@Table(name = "responses", indexes = {
    @Index(name = "idx_response_round", columnList = "round_id"),
    @Index(name = "idx_response_participant", columnList = "participant_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"round_id", "participant_id"})
})
public class ResponseEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private RoundEntity round;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ParticipantEntity participant;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "response_order", nullable = false)
    private Integer responseOrder;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @Column(name = "quality_score", precision = 3, scale = 2)
    private BigDecimal qualityScore;
    
    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private BigDecimal sentimentScore;
    
    @Column(name = "coherence_score", precision = 3, scale = 2)
    private BigDecimal coherenceScore;
    
    @Column(name = "factuality_score", precision = 3, scale = 2)
    private BigDecimal factualityScore;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Constructors
    public ResponseEntity() {}
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getResponseOrder() {
        return responseOrder;
    }
    
    public void setResponseOrder(Integer responseOrder) {
        this.responseOrder = responseOrder;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public BigDecimal getQualityScore() {
        return qualityScore;
    }
    
    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }
    
    public BigDecimal getSentimentScore() {
        return sentimentScore;
    }
    
    public void setSentimentScore(BigDecimal sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
    
    public BigDecimal getCoherenceScore() {
        return coherenceScore;
    }
    
    public void setCoherenceScore(BigDecimal coherenceScore) {
        this.coherenceScore = coherenceScore;
    }
    
    public BigDecimal getFactualityScore() {
        return factualityScore;
    }
    
    public void setFactualityScore(BigDecimal factualityScore) {
        this.factualityScore = factualityScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}