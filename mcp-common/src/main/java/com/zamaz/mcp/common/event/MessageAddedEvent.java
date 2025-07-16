package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a message is added to a debate
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MessageAddedEvent extends DomainEvent {
    
    private String messageId;
    private String debateId;
    private String participantId;
    private String participantName;
    private String content;
    private String messageType; // ARGUMENT, REBUTTAL, CONCLUSION, MODERATOR
    private Integer roundNumber;
    private Integer turnNumber;
    private Instant messageTime;
    
    public MessageAddedEvent(String messageId, String debateId, String organizationId, String participantId) {
        super("MESSAGE_ADDED", debateId, "DEBATE");
        this.messageId = messageId;
        this.debateId = debateId;
        this.participantId = participantId;
        this.messageTime = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(participantId);
        this.setSourceService("mcp-controller");
    }
}