package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a participant leaves a debate
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ParticipantLeftEvent extends DomainEvent {
    
    private String debateId;
    private String participantId;
    private String participantName;
    private String reason; // COMPLETED, DISCONNECTED, KICKED, ERROR
    private Instant leftAt;
    
    public ParticipantLeftEvent(String debateId, String participantId, String organizationId, String reason) {
        super("PARTICIPANT_LEFT", debateId, "DEBATE");
        this.debateId = debateId;
        this.participantId = participantId;
        this.reason = reason;
        this.leftAt = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(participantId);
        this.setSourceService("mcp-controller");
    }
}