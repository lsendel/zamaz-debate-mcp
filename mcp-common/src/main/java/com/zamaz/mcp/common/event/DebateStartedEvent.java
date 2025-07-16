package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Event published when a debate is started
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DebateStartedEvent extends DomainEvent {
    
    private String debateId;
    private String debateName;
    private List<String> participantIds = new ArrayList<>();
    private String startedBy;
    private Instant actualStartTime;
    private Integer plannedRounds;
    
    public DebateStartedEvent(String debateId, String debateName, String organizationId, String startedBy) {
        super("DEBATE_STARTED", debateId, "DEBATE");
        this.debateId = debateId;
        this.debateName = debateName;
        this.startedBy = startedBy;
        this.actualStartTime = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(startedBy);
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
    
    public String getStartedBy() {
        return startedBy;
    }
    
    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }
    
    public Instant getActualStartTime() {
        return actualStartTime;
    }
    
    public void setActualStartTime(Instant actualStartTime) {
        this.actualStartTime = actualStartTime;
    }
    
    public Integer getPlannedRounds() {
        return plannedRounds;
    }
    
    public void setPlannedRounds(Integer plannedRounds) {
        this.plannedRounds = plannedRounds;
    }
}