package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a debate is started
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DebateStartedEvent extends DomainEvent {
    
    private String debateId;
    private String debateName;
    private List<String> participantIds;
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
}