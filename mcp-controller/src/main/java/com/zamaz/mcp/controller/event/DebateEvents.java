package com.zamaz.mcp.controller.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Debate-related event classes
 */
public class DebateEvents {
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DebateStartedEvent {
        private String debateId;
        private String organizationId;
        private long timestamp;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoundCompletedEvent {
        private String debateId;
        private int roundNumber;
        private List<Map<String, Object>> responses;
        private long timestamp;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewResponseEvent {
        private String debateId;
        private String responseId;
        private String participantId;
        private String participantName;
        private String position;
        private String content;
        private int roundNumber;
        private long timestamp;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DebateCompletedEvent {
        private String debateId;
        private String summary;
        private String winner;
        private Map<String, Object> finalScores;
        private long timestamp;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VoteUpdateEvent {
        private String debateId;
        private String responseId;
        private String voteType;
        private String voterId;
        private Map<String, Integer> currentVotes;
        private long timestamp;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParticipantJoinedEvent {
        private String debateId;
        private String participantId;
        private String participantName;
        private String participantType;
        private String position;
        private int totalParticipants;
        private long timestamp;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DebateErrorEvent {
        private String debateId;
        private String error;
        private String errorType;
        private boolean recoverable;
        private long timestamp;
    }
}