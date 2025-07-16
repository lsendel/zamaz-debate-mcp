package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a debate is completed
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DebateCompletedEvent extends DomainEvent {
    
    private String debateId;
    private String debateName;
    private String winnerId;
    private String completionReason; // NORMAL, TIMEOUT, CANCELLED, ERROR
    private Integer totalRounds;
    private Integer completedRounds;
    private Instant startTime;
    private Instant endTime;
    private Map<String, Object> statistics; // participant stats, scores, etc.
    
    public DebateCompletedEvent(String debateId, String debateName, String organizationId, String completedBy) {
        super("DEBATE_COMPLETED", debateId, "DEBATE");
        this.debateId = debateId;
        this.debateName = debateName;
        this.endTime = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(completedBy);
        this.setSourceService("mcp-controller");
    }
}