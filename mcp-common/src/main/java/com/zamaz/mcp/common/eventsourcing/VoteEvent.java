package com.zamaz.mcp.common.eventsourcing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for vote-related events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class VoteEvent extends BaseEvent {
    
    public static final String AGGREGATE_TYPE = "vote";
    
    // Event types
    public static final String VOTE_CAST = "vote.cast";
    public static final String VOTE_CHANGED = "vote.changed";
    public static final String VOTE_RETRACTED = "vote.retracted";
    public static final String VOTE_COMMENT_ADDED = "vote.comment.added";
    public static final String VOTE_COMMENT_UPDATED = "vote.comment.updated";
    public static final String VOTE_COMMENT_REMOVED = "vote.comment.removed";
    
    /**
     * Create a vote cast event
     */
    public static VoteEvent cast(String voteId, String argumentId, String debateId, String organizationId, String userId, Object payload) {
        return VoteEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(VOTE_CAST)
            .aggregateId(voteId)
            .aggregateType(AGGREGATE_TYPE)
            .version(1L)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "argumentId", argumentId,
                "debateId", debateId
            ))
            .build();
    }
    
    /**
     * Create a vote changed event
     */
    public static VoteEvent changed(String voteId, String argumentId, String debateId, String organizationId, String userId, 
                                   String oldVoteType, String newVoteType, long version, Object payload) {
        return VoteEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(VOTE_CHANGED)
            .aggregateId(voteId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "argumentId", argumentId,
                "debateId", debateId,
                "oldVoteType", oldVoteType,
                "newVoteType", newVoteType
            ))
            .build();
    }
    
    /**
     * Create a vote retracted event
     */
    public static VoteEvent retracted(String voteId, String argumentId, String debateId, String organizationId, String userId, 
                                    long version, Object payload) {
        return VoteEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(VOTE_RETRACTED)
            .aggregateId(voteId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "argumentId", argumentId,
                "debateId", debateId
            ))
            .build();
    }
    
    /**
     * Create a vote comment added event
     */
    public static VoteEvent commentAdded(String voteId, String argumentId, String debateId, String organizationId, String userId, 
                                       long version, Object payload) {
        return VoteEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(VOTE_COMMENT_ADDED)
            .aggregateId(voteId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "argumentId", argumentId,
                "debateId", debateId
            ))
            .build();
    }
    
    /**
     * Create a vote comment updated event
     */
    public static VoteEvent commentUpdated(String voteId, String argumentId, String debateId, String organizationId, String userId, 
                                         long version, Object payload) {
        return VoteEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(VOTE_COMMENT_UPDATED)
            .aggregateId(voteId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "argumentId", argumentId,
                "debateId", debateId
            ))
            .build();
    }
    
    /**
     * Create a vote comment removed event
     */
    public static VoteEvent commentRemoved(String voteId, String argumentId, String debateId, String organizationId, String userId, 
                                         long version, Object payload) {
        return VoteEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(VOTE_COMMENT_REMOVED)
            .aggregateId(voteId)
            .aggregateType(AGGREGATE_TYPE)
            .version(version)
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .organizationId(organizationId)
            .payload(payload)
            .metadata(java.util.Map.of(
                "argumentId", argumentId,
                "debateId", debateId
            ))
            .build();
    }
}