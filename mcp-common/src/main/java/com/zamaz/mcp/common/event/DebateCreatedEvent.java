package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event published when a new debate is created
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DebateCreatedEvent extends DomainEvent {
    
    private String debateId;
    private String debateName;
    private String description;
    private String format;
    private String status;
    private List<String> participantIds = new ArrayList<>();
    private String createdBy;
    private Instant scheduledStartTime;
    private Integer maxRounds;
    private Integer timePerRound;
    
    public DebateCreatedEvent(String debateId, String debateName, String organizationId, String createdBy) {
        super("DEBATE_CREATED", debateId, "DEBATE");
        this.debateId = debateId;
        this.debateName = debateName;
        this.createdBy = createdBy;
        this.setOrganizationId(organizationId);
        this.setUserId(createdBy);
        this.setSourceService("mcp-controller");
    }
    
    // Defensive getters and setters
    public List<String> getParticipantIds() {
        return participantIds == null ? null : new ArrayList<>(participantIds);
    }
    
    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds == null ? null : new ArrayList<>(participantIds);
    }
    
    // Generate other getters/setters
    public String getDebateId() {
        return debateId;
    }
    
    public void setDebateId(String debateId) {
        this.debateId = debateId;
    }
    
    public String getDebateName() {
        return debateName;
    }
    
    public void setDebateName(String debateName) {
        this.debateName = debateName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Instant getScheduledStartTime() {
        return scheduledStartTime;
    }
    
    public void setScheduledStartTime(Instant scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }
    
    public Integer getMaxRounds() {
        return maxRounds;
    }
    
    public void setMaxRounds(Integer maxRounds) {
        this.maxRounds = maxRounds;
    }
    
    public Integer getTimePerRound() {
        return timePerRound;
    }
    
    public void setTimePerRound(Integer timePerRound) {
        this.timePerRound = timePerRound;
    }
}