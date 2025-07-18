package com.zamaz.mcp.controller.adapter.persistence.entity;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for Debate.
 */
@Entity
@Table(name = "debates")
public class DebateEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "topic", nullable = false, length = 1000)
    private String topic;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "result", length = 5000)
    private String result;
    
    // Configuration fields
    @Column(name = "min_participants", nullable = false)
    private Integer minParticipants;
    
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;
    
    @Column(name = "max_rounds", nullable = false)
    private Integer maxRounds;
    
    @Column(name = "round_time_limit_minutes")
    private Integer roundTimeLimitMinutes;
    
    @Column(name = "max_debate_duration_hours")
    private Integer maxDebateDurationHours;
    
    @Column(name = "require_balanced_positions", nullable = false)
    private Boolean requireBalancedPositions;
    
    @Column(name = "auto_advance_rounds", nullable = false)
    private Boolean autoAdvanceRounds;
    
    @Column(name = "allow_spectators", nullable = false)
    private Boolean allowSpectators;
    
    @Column(name = "max_response_length", nullable = false)
    private Integer maxResponseLength;
    
    @Column(name = "enable_quality_assessment", nullable = false)
    private Boolean enableQualityAssessment;
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ParticipantEntity> participants = new ArrayList<>();
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<RoundEntity> rounds = new ArrayList<>();
    
    @Version
    private Long version;
    
    protected DebateEntity() {
        // JPA constructor
    }
    
    public DebateEntity(UUID id, String topic, String status, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public Integer getMinParticipants() {
        return minParticipants;
    }
    
    public void setMinParticipants(Integer minParticipants) {
        this.minParticipants = minParticipants;
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
    
    public Integer getRoundTimeLimitMinutes() {
        return roundTimeLimitMinutes;
    }
    
    public void setRoundTimeLimitMinutes(Integer roundTimeLimitMinutes) {
        this.roundTimeLimitMinutes = roundTimeLimitMinutes;
    }
    
    public Integer getMaxDebateDurationHours() {
        return maxDebateDurationHours;
    }
    
    public void setMaxDebateDurationHours(Integer maxDebateDurationHours) {
        this.maxDebateDurationHours = maxDebateDurationHours;
    }
    
    public Boolean getRequireBalancedPositions() {
        return requireBalancedPositions;
    }
    
    public void setRequireBalancedPositions(Boolean requireBalancedPositions) {
        this.requireBalancedPositions = requireBalancedPositions;
    }
    
    public Boolean getAutoAdvanceRounds() {
        return autoAdvanceRounds;
    }
    
    public void setAutoAdvanceRounds(Boolean autoAdvanceRounds) {
        this.autoAdvanceRounds = autoAdvanceRounds;
    }
    
    public Boolean getAllowSpectators() {
        return allowSpectators;
    }
    
    public void setAllowSpectators(Boolean allowSpectators) {
        this.allowSpectators = allowSpectators;
    }
    
    public Integer getMaxResponseLength() {
        return maxResponseLength;
    }
    
    public void setMaxResponseLength(Integer maxResponseLength) {
        this.maxResponseLength = maxResponseLength;
    }
    
    public Boolean getEnableQualityAssessment() {
        return enableQualityAssessment;
    }
    
    public void setEnableQualityAssessment(Boolean enableQualityAssessment) {
        this.enableQualityAssessment = enableQualityAssessment;
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
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    // Helper methods
    public void addParticipant(ParticipantEntity participant) {
        participants.add(participant);
        participant.setDebate(this);
    }
    
    public void removeParticipant(ParticipantEntity participant) {
        participants.remove(participant);
        participant.setDebate(null);
    }
    
    public void addRound(RoundEntity round) {
        rounds.add(round);
        round.setDebate(this);
    }
    
    public void removeRound(RoundEntity round) {
        rounds.remove(round);
        round.setDebate(null);
    }
}