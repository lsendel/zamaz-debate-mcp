package com.zamaz.mcp.controller.adapter.persistence.entity;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for Round.
 */
@Entity
@Table(name = "rounds")
public class RoundEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private DebateEntity debate;
    
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ResponseEntity> responses = new ArrayList<>();
    
    @Version
    private Long version;
    
    protected RoundEntity() {
        // JPA constructor
    }
    
    public RoundEntity(UUID id, Integer roundNumber, String status, Instant startedAt) {
        this.id = id;
        this.roundNumber = roundNumber;
        this.status = status;
        this.startedAt = startedAt;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Integer getRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }
    
    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }
    
    public DebateEntity getDebate() {
        return debate;
    }
    
    public void setDebate(DebateEntity debate) {
        this.debate = debate;
    }
    
    public List<ResponseEntity> getResponses() {
        return responses;
    }
    
    public void setResponses(List<ResponseEntity> responses) {
        this.responses = responses;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    // Helper methods
    public void addResponse(ResponseEntity response) {
        responses.add(response);
        response.setRound(this);
    }
    
    public void removeResponse(ResponseEntity response) {
        responses.remove(response);
        response.setRound(null);
    }
}