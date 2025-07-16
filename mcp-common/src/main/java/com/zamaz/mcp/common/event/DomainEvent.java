package com.zamaz.mcp.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DebateCreatedEvent.class, name = "DEBATE_CREATED"),
    @JsonSubTypes.Type(value = DebateStartedEvent.class, name = "DEBATE_STARTED"),
    @JsonSubTypes.Type(value = MessageAddedEvent.class, name = "MESSAGE_ADDED"),
    @JsonSubTypes.Type(value = ParticipantJoinedEvent.class, name = "PARTICIPANT_JOINED"),
    @JsonSubTypes.Type(value = DebateCompletedEvent.class, name = "DEBATE_COMPLETED")
})
public abstract class DomainEvent {
    
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private String aggregateId;
    private String aggregateType;
    private String organizationId;
    private String userId;
    private Map<String, Object> metadata;
    
    protected DomainEvent(String eventType, String aggregateId, String aggregateType) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
    }
}