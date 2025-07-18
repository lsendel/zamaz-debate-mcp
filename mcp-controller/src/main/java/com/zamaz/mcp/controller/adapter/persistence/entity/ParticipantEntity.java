package com.zamaz.mcp.controller.adapter.persistence.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for Participant.
 */
@Entity
@Table(name = "participants")
public class ParticipantEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "type", nullable = false, length = 50)
    private String type;
    
    @Column(name = "position", nullable = false, length = 1000)
    private String position;
    
    @Column(name = "provider", length = 50)
    private String provider;
    
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    
    @Column(name = "active", nullable = false)
    private Boolean active;
    
    @Column(name = "response_count", nullable = false)
    private Integer responseCount;
    
    // Provider configuration fields
    @Column(name = "provider_model", length = 255)
    private String providerModel;
    
    @Column(name = "provider_max_tokens")
    private Integer providerMaxTokens;
    
    @Column(name = "provider_temperature", precision = 3, scale = 2)
    private BigDecimal providerTemperature;
    
    @Column(name = "provider_top_p", precision = 3, scale = 2)
    private BigDecimal providerTopP;
    
    @Column(name = "provider_system_prompt", length = 10000)
    private String providerSystemPrompt;
    
    // Quality metrics
    @Column(name = "avg_logical_strength", precision = 4, scale = 2)
    private BigDecimal avgLogicalStrength;
    
    @Column(name = "avg_evidence_quality", precision = 4, scale = 2)
    private BigDecimal avgEvidenceQuality;
    
    @Column(name = "avg_clarity", precision = 4, scale = 2)
    private BigDecimal avgClarity;
    
    @Column(name = "avg_relevance", precision = 4, scale = 2)
    private BigDecimal avgRelevance;
    
    @Column(name = "avg_originality", precision = 4, scale = 2)
    private BigDecimal avgOriginality;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private DebateEntity debate;
    
    @Version
    private Long version;
    
    protected ParticipantEntity() {
        // JPA constructor
    }
    
    public ParticipantEntity(UUID id, String name, String type, String position, Instant joinedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.position = position;
        this.joinedAt = joinedAt;
        this.active = true;
        this.responseCount = 0;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public Instant getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Integer getResponseCount() {
        return responseCount;
    }
    
    public void setResponseCount(Integer responseCount) {
        this.responseCount = responseCount;
    }
    
    public String getProviderModel() {
        return providerModel;
    }
    
    public void setProviderModel(String providerModel) {
        this.providerModel = providerModel;
    }
    
    public Integer getProviderMaxTokens() {
        return providerMaxTokens;
    }
    
    public void setProviderMaxTokens(Integer providerMaxTokens) {
        this.providerMaxTokens = providerMaxTokens;
    }
    
    public BigDecimal getProviderTemperature() {
        return providerTemperature;
    }
    
    public void setProviderTemperature(BigDecimal providerTemperature) {
        this.providerTemperature = providerTemperature;
    }
    
    public BigDecimal getProviderTopP() {
        return providerTopP;
    }
    
    public void setProviderTopP(BigDecimal providerTopP) {
        this.providerTopP = providerTopP;
    }
    
    public String getProviderSystemPrompt() {
        return providerSystemPrompt;
    }
    
    public void setProviderSystemPrompt(String providerSystemPrompt) {
        this.providerSystemPrompt = providerSystemPrompt;
    }
    
    public BigDecimal getAvgLogicalStrength() {
        return avgLogicalStrength;
    }
    
    public void setAvgLogicalStrength(BigDecimal avgLogicalStrength) {
        this.avgLogicalStrength = avgLogicalStrength;
    }
    
    public BigDecimal getAvgEvidenceQuality() {
        return avgEvidenceQuality;
    }
    
    public void setAvgEvidenceQuality(BigDecimal avgEvidenceQuality) {
        this.avgEvidenceQuality = avgEvidenceQuality;
    }
    
    public BigDecimal getAvgClarity() {
        return avgClarity;
    }
    
    public void setAvgClarity(BigDecimal avgClarity) {
        this.avgClarity = avgClarity;
    }
    
    public BigDecimal getAvgRelevance() {
        return avgRelevance;
    }
    
    public void setAvgRelevance(BigDecimal avgRelevance) {
        this.avgRelevance = avgRelevance;
    }
    
    public BigDecimal getAvgOriginality() {
        return avgOriginality;
    }
    
    public void setAvgOriginality(BigDecimal avgOriginality) {
        this.avgOriginality = avgOriginality;
    }
    
    public DebateEntity getDebate() {
        return debate;
    }
    
    public void setDebate(DebateEntity debate) {
        this.debate = debate;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}