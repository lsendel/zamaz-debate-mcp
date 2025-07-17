package com.zamaz.mcp.controller.event;

import com.zamaz.mcp.controller.websocket.DebateWebSocketHandler;
import com.zamaz.mcp.controller.event.DebateEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes debate events to WebSocket clients
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventPublisher {
    
    private final DebateWebSocketHandler webSocketHandler;
    
    /**
     * Handle debate started event
     */
    @EventListener
    @Async
    public void handleDebateStarted(DebateStartedEvent event) {
        log.info("Publishing debate started event via WebSocket: {}", event.getDebateId());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "debate_started",
            "debateId", event.getDebateId(),
            "status", "IN_PROGRESS",
            "currentRound", 1,
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Handle round completed event
     */
    @EventListener
    @Async
    public void handleRoundCompleted(RoundCompletedEvent event) {
        log.info("Publishing round completed event via WebSocket: Debate {}, Round {}", 
            event.getDebateId(), event.getRoundNumber());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "round_completed",
            "debateId", event.getDebateId(),
            "roundNumber", event.getRoundNumber(),
            "responses", event.getResponses(),
            "nextRound", event.getRoundNumber() + 1,
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Handle new response event
     */
    @EventListener
    @Async
    public void handleNewResponse(NewResponseEvent event) {
        log.info("Publishing new response event via WebSocket: Debate {}, Participant {}", 
            event.getDebateId(), event.getParticipantName());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "new_response",
            "debateId", event.getDebateId(),
            "responseId", event.getResponseId(),
            "participantId", event.getParticipantId(),
            "participantName", event.getParticipantName(),
            "position", event.getPosition(),
            "content", event.getContent(),
            "roundNumber", event.getRoundNumber(),
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Handle debate completed event
     */
    @EventListener
    @Async
    public void handleDebateCompleted(DebateCompletedEvent event) {
        log.info("Publishing debate completed event via WebSocket: {}", event.getDebateId());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "debate_completed",
            "debateId", event.getDebateId(),
            "status", "COMPLETED",
            "summary", event.getSummary(),
            "winner", event.getWinner(),
            "finalScores", event.getFinalScores(),
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Handle vote update event
     */
    @EventListener
    @Async
    public void handleVoteUpdate(VoteUpdateEvent event) {
        log.debug("Publishing vote update event via WebSocket: Response {}", event.getResponseId());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "vote_update",
            "debateId", event.getDebateId(),
            "responseId", event.getResponseId(),
            "voteType", event.getVoteType(),
            "voterId", event.getVoterId(),
            "currentVotes", event.getCurrentVotes(),
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Handle participant joined event
     */
    @EventListener
    @Async
    public void handleParticipantJoined(ParticipantJoinedEvent event) {
        log.info("Publishing participant joined event via WebSocket: {} joined debate {}", 
            event.getParticipantName(), event.getDebateId());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "participant_joined",
            "debateId", event.getDebateId(),
            "participantId", event.getParticipantId(),
            "participantName", event.getParticipantName(),
            "participantType", event.getParticipantType(),
            "position", event.getPosition(),
            "totalParticipants", event.getTotalParticipants(),
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Handle error event
     */
    @EventListener
    @Async
    public void handleDebateError(DebateErrorEvent event) {
        log.error("Publishing debate error event via WebSocket: {}", event.getError());
        
        webSocketHandler.broadcastToDebate(event.getDebateId(), Map.of(
            "type", "debate_error",
            "debateId", event.getDebateId(),
            "error", event.getError(),
            "errorType", event.getErrorType(),
            "recoverable", event.isRecoverable(),
            "timestamp", event.getTimestamp()
        ));
    }
    
    /**
     * Send system announcement
     */
    public void sendAnnouncement(String debateId, String message, String level) {
        log.info("Sending system announcement to debate {}: {}", debateId, message);
        
        webSocketHandler.broadcastToDebate(debateId, Map.of(
            "type", "announcement",
            "message", message,
            "level", level, // info, warning, error
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Send debate statistics update
     */
    public void sendStatisticsUpdate(String debateId, Map<String, Object> statistics) {
        log.debug("Sending statistics update to debate {}", debateId);
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "statistics_update");
        message.put("debateId", debateId);
        message.put("statistics", statistics);
        message.put("timestamp", System.currentTimeMillis());
        
        webSocketHandler.broadcastToDebate(debateId, message);
    }
}