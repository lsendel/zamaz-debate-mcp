package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a participant joins a debate
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ParticipantJoinedEvent extends DomainEvent {
    
    private String debateId;
    private String participantId;
    private String participantName;
    private String participantType; // HUMAN, AI, MODERATOR
    private String role; // PROPONENT, OPPONENT, MODERATOR, OBSERVER
    private Instant joinedAt;
    
    public ParticipantJoinedEvent(String debateId, String participantId, String organizationId) {
        super("PARTICIPANT_JOINED", debateId, "DEBATE");
        this.debateId = debateId;
        this.participantId = participantId;
        this.joinedAt = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(participantId);
        this.setSourceService("mcp-controller");
    }
}