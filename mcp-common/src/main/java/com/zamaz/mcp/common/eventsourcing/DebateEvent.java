package com.zamaz.mcp.common.eventsourcing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for debate-related events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class DebateEvent extends BaseEvent {
    
    public static final String AGGREGATE_TYPE = "debate";
    
    // Event types
    public static final String DEBATE_CREATED = "debate.created";
    public static final String DEBATE_UPDATED = "debate.updated";
    public static final String DEBATE_DELETED = "debate.deleted";
    public static final String DEBATE_STARTED = "debate.started";
    public static final String DEBATE_PAUSED = "debate.paused";
    public static final String DEBATE_RESUMED = "debate.resumed";
    public static final String DEBATE_COMPLETED = "debate.completed";
    public static final String DEBATE_CANCELLED = "debate.cancelled";
    public static final String DEBATE_PARTICIPANT_JOINED = "debate.participant.joined";
    public static final String DEBATE_PARTICIPANT_LEFT = "debate.participant.left";
    public static final String DEBATE_RULES_UPDATED = "debate.rules.updated";
    public static final String DEBATE_SETTINGS_UPDATED = "debate.settings.updated";
    
    /**
     * Create a debate created event
     */
    public static DebateEvent created(String debateId, String organizationId, String userId, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_CREATED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(1L)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate updated event
     */
    public static DebateEvent updated(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_UPDATED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate deleted event
     */
    public static DebateEvent deleted(String debateId, String organizationId, String userId, long version) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_DELETED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .build();
    }
    
    /**
     * Create a debate started event
     */
    public static DebateEvent started(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_STARTED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate paused event
     */
    public static DebateEvent paused(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_PAUSED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate resumed event
     */
    public static DebateEvent resumed(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_RESUMED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate completed event
     */
    public static DebateEvent completed(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_COMPLETED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate cancelled event
     */
    public static DebateEvent cancelled(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_CANCELLED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a participant joined event
     */
    public static DebateEvent participantJoined(String debateId, String organizationId, String userId, 
                                               String participantId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_PARTICIPANT_JOINED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("participantId", participantId))
            .build();
    }
    
    /**
     * Create a participant left event
     */
    public static DebateEvent participantLeft(String debateId, String organizationId, String userId, 
                                            String participantId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_PARTICIPANT_LEFT)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("participantId", participantId))
            .build();
    }
    
    /**
     * Create a debate rules updated event
     */
    public static DebateEvent rulesUpdated(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_RULES_UPDATED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
    
    /**
     * Create a debate settings updated event
     */
    public static DebateEvent settingsUpdated(String debateId, String organizationId, String userId, long version, Object payload) {
        return DebateEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(DEBATE_SETTINGS_UPDATED)
            .aggregateId(debateId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .build();
    }
}