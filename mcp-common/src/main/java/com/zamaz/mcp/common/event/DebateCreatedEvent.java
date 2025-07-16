package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a new debate is created
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DebateCreatedEvent extends DomainEvent {
    
    private String debateId;
    private String debateName;
    private String description;
    private String format;
    private String status;
    private List<String> participantIds;
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
}