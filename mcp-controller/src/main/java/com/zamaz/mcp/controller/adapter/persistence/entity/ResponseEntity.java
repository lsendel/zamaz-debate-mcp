package com.zamaz.mcp.controller.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Response.
 */
@Entity
@Table(name = "responses")
public class ResponseEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "participant_id", nullable = false, columnDefinition = "uuid")
    private UUID participantId;
    
    @Column(name = "position", nullable = false, length = 1000)
    private String position;
    
    @Column(name = "content", nullable = false, length = 50000)
    private String content;
    
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    
    @Column(name = "response_time_seconds")
    private Long responseTimeSeconds;
    
    @Column(name = "flagged", nullable = false)
    private Boolean flagged;
    
    @Column(name = "flag_reason", length = 1000)
    private String flagReason;
    
    // Quality metrics
    @Column(name = "quality_logical_strength", precision = 4, scale = 2)
    private BigDecimal qualityLogicalStrength;
    
    @Column(name = "quality_evidence_quality", precision = 4, scale = 2)
    private BigDecimal qualityEvidenceQuality;
    
    @Column(name = "quality_clarity", precision = 4, scale = 2)
    private BigDecimal qualityClarity;
    
    @Column(name = "quality_relevance", precision = 4, scale = 2)
    private BigDecimal qualityRelevance;
    
    @Column(name = "quality_originality", precision = 4, scale = 2)
    private BigDecimal qualityOriginality;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private RoundEntity round;
    
    @Version
    private Long version;
    
    protected ResponseEntity() {
        // JPA constructor
    }
    
    public ResponseEntity(UUID id, UUID participantId, String position, String content, Instant submittedAt) {
        this.id = id;
        this.participantId = participantId;
        this.position = position;
        this.content = content;
        this.submittedAt = submittedAt;
        this.flagged = false;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getParticipantId() {
        return participantId;
    }
    
    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Instant getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public Long getResponseTimeSeconds() {
        return responseTimeSeconds;
    }
    
    public void setResponseTimeSeconds(Long responseTimeSeconds) {
        this.responseTimeSeconds = responseTimeSeconds;
    }
    
    public Boolean getFlagged() {
        return flagged;
    }
    
    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
    }
    
    public String getFlagReason() {
        return flagReason;
    }
    
    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }
    
    public BigDecimal getQualityLogicalStrength() {
        return qualityLogicalStrength;
    }
    
    public void setQualityLogicalStrength(BigDecimal qualityLogicalStrength) {
        this.qualityLogicalStrength = qualityLogicalStrength;
    }
    
    public BigDecimal getQualityEvidenceQuality() {
        return qualityEvidenceQuality;
    }
    
    public void setQualityEvidenceQuality(BigDecimal qualityEvidenceQuality) {
        this.qualityEvidenceQuality = qualityEvidenceQuality;
    }
    
    public BigDecimal getQualityClarity() {
        return qualityClarity;
    }
    
    public void setQualityClarity(BigDecimal qualityClarity) {
        this.qualityClarity = qualityClarity;
    }
    
    public BigDecimal getQualityRelevance() {
        return qualityRelevance;
    }
    
    public void setQualityRelevance(BigDecimal qualityRelevance) {
        this.qualityRelevance = qualityRelevance;
    }
    
    public BigDecimal getQualityOriginality() {
        return qualityOriginality;
    }
    
    public void setQualityOriginality(BigDecimal qualityOriginality) {
        this.qualityOriginality = qualityOriginality;
    }
    
    public RoundEntity getRound() {
        return round;
    }
    
    public void setRound(RoundEntity round) {
        this.round = round;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}