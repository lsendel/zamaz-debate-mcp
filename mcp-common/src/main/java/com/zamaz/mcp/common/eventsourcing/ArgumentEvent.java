package com.zamaz.mcp.common.eventsourcing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for argument-related events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ArgumentEvent extends BaseEvent {
    
    public static final String AGGREGATE_TYPE = "argument";
    
    // Event types
    public static final String ARGUMENT_CREATED = "argument.created";
    public static final String ARGUMENT_UPDATED = "argument.updated";
    public static final String ARGUMENT_DELETED = "argument.deleted";
    public static final String ARGUMENT_PUBLISHED = "argument.published";
    public static final String ARGUMENT_REPLY_ADDED = "argument.reply.added";
    public static final String ARGUMENT_ATTACHMENT_ADDED = "argument.attachment.added";
    public static final String ARGUMENT_ATTACHMENT_REMOVED = "argument.attachment.removed";
    public static final String ARGUMENT_FLAGGED = "argument.flagged";
    public static final String ARGUMENT_UNFLAGGED = "argument.unflagged";
    
    /**
     * Create an argument created event
     */
    public static ArgumentEvent created(String argumentId, String debateId, String organizationId, String userId, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_CREATED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(1L)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("debateId", debateId))
            .build();
    }
    
    /**
     * Create an argument updated event
     */
    public static ArgumentEvent updated(String argumentId, String debateId, String organizationId, String userId, long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_UPDATED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("debateId", debateId))
            .build();
    }
    
    /**
     * Create an argument deleted event
     */
    public static ArgumentEvent deleted(String argumentId, String debateId, String organizationId, String userId, long version) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_DELETED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .metadata(java.util.Map.of("debateId", debateId))
            .build();
    }
    
    /**
     * Create an argument published event
     */
    public static ArgumentEvent published(String argumentId, String debateId, String organizationId, String userId, long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_PUBLISHED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("debateId", debateId))
            .build();
    }
    
    /**
     * Create an argument reply added event
     */
    public static ArgumentEvent replyAdded(String argumentId, String debateId, String organizationId, String userId, 
                                         String replyId, long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_REPLY_ADDED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "debateId", debateId,
                "replyId", replyId
            ))
            .build();
    }
    
    /**
     * Create an argument attachment added event
     */
    public static ArgumentEvent attachmentAdded(String argumentId, String debateId, String organizationId, String userId, 
                                              String attachmentId, long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_ATTACHMENT_ADDED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "debateId", debateId,
                "attachmentId", attachmentId
            ))
            .build();
    }
    
    /**
     * Create an argument attachment removed event
     */
    public static ArgumentEvent attachmentRemoved(String argumentId, String debateId, String organizationId, String userId, 
                                                String attachmentId, long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_ATTACHMENT_REMOVED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "debateId", debateId,
                "attachmentId", attachmentId
            ))
            .build();
    }
    
    /**
     * Create an argument flagged event
     */
    public static ArgumentEvent flagged(String argumentId, String debateId, String organizationId, String userId, 
                                      String reason, long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_FLAGGED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "debateId", debateId,
                "reason", reason
            ))
            .build();
    }
    
    /**
     * Create an argument unflagged event
     */
    public static ArgumentEvent unflagged(String argumentId, String debateId, String organizationId, String userId, 
                                        long version, Object payload) {
        return ArgumentEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(ARGUMENT_UNFLAGGED)
            .aggregateId(argumentId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of("debateId", debateId))
            .build();
    }
}