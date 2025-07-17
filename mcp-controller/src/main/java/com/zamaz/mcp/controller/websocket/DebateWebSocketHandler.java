package com.zamaz.mcp.controller.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.security.SecurityContext;
import com.zamaz.mcp.controller.service.DebateService;
import com.zamaz.mcp.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time debate updates
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DebateWebSocketHandler implements WebSocketHandler {
    
    private final DebateService debateService;
    private final ObjectMapper objectMapper;
    private final SecurityContext securityContext;
    private final JwtService jwtService;
    
    // Map of debate ID to connected sessions
    private final Map<String, Map<String, WebSocketSession>> debateSessions = new ConcurrentHashMap<>();
    
    // Map of session ID to sink for sending messages
    private final Map<String, Sinks.Many<WebSocketMessage>> sessionSinks = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        String path = session.getHandshakeInfo().getUri().getPath();
        String debateId = extractDebateId(path);
        
        if (debateId == null) {
            log.error("Invalid WebSocket path: {}", path);
            return session.close();
        }
        
        // Validate authorization
        String token = session.getHandshakeInfo().getHeaders().getFirst("X-Auth-Token");
        String organizationId = session.getHandshakeInfo().getHeaders().getFirst("X-Organization-ID");
        
        if (!validateAccess(token, organizationId, debateId)) {
            log.warn("Unauthorized WebSocket connection attempt for debate: {}", debateId);
            return session.close();
        }
        
        log.info("WebSocket connection established - Session: {}, Debate: {}", sessionId, debateId);
        
        // Create sink for this session
        Sinks.Many<WebSocketMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionSinks.put(sessionId, sink);
        
        // Add session to debate room
        debateSessions.computeIfAbsent(debateId, k -> new ConcurrentHashMap<>())
            .put(sessionId, session);
        
        // Send welcome message
        sendMessage(session, Map.of(
            "type", "connection",
            "status", "connected",
            "debateId", debateId,
            "sessionId", sessionId
        ));
        
        // Subscribe to debate events
        Flux<WebSocketMessage> debateUpdates = subscribeToDebateUpdates(debateId, session);
        
        // Handle incoming messages
        Flux<WebSocketMessage> output = session.receive()
            .doOnNext(message -> handleIncomingMessage(session, debateId, message))
            .thenMany(Flux.merge(
                sink.asFlux(),
                debateUpdates,
                createHeartbeat(session)
            ))
            .doOnError(error -> log.error("WebSocket error - Session: {}", sessionId, error))
            .doFinally(signal -> {
                log.info("WebSocket disconnected - Session: {}, Signal: {}", sessionId, signal);
                cleanupSession(sessionId, debateId);
            });
        
        return session.send(output);
    }
    
    /**
     * Handle incoming messages from client
     */
    private void handleIncomingMessage(WebSocketSession session, String debateId, WebSocketMessage message) {
        try {
            String payload = message.getPayloadAsText();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            
            log.debug("Received WebSocket message - Type: {}, Debate: {}", type, debateId);
            
            switch (type) {
                case "ping":
                    sendMessage(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
                    break;
                    
                case "subscribe":
                    // Client subscribing to specific events
                    String eventType = (String) data.get("eventType");
                    log.info("Client subscribed to events - Type: {}, Debate: {}", eventType, debateId);
                    break;
                    
                case "vote":
                    // Handle real-time voting
                    handleVote(session, debateId, data);
                    break;
                    
                case "comment":
                    // Handle real-time comments
                    handleComment(session, debateId, data);
                    break;
                    
                default:
                    log.warn("Unknown message type: {}", type);
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }
    
    /**
     * Subscribe to debate updates
     */
    private Flux<WebSocketMessage> subscribeToDebateUpdates(String debateId, WebSocketSession session) {
        return debateService.subscribeToDebateEvents(debateId)
            .map(event -> {
                try {
                    String json = objectMapper.writeValueAsString(event);
                    return session.textMessage(json);
                } catch (Exception e) {
                    log.error("Error serializing debate event", e);
                    return null;
                }
            })
            .filter(msg -> msg != null);
    }
    
    /**
     * Create heartbeat messages
     */
    private Flux<WebSocketMessage> createHeartbeat(WebSocketSession session) {
        return Flux.interval(Duration.ofSeconds(30))
            .map(tick -> session.textMessage(
                "{\"type\":\"heartbeat\",\"timestamp\":" + System.currentTimeMillis() + "}"
            ));
    }
    
    /**
     * Broadcast message to all sessions in a debate
     */
    public void broadcastToDebate(String debateId, Map<String, Object> message) {
        Map<String, WebSocketSession> sessions = debateSessions.get(debateId);
        if (sessions != null) {
            sessions.forEach((sessionId, session) -> {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            });
        }
    }
    
    /**
     * Send message to specific session
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Sinks.Many<WebSocketMessage> sink = sessionSinks.get(session.getId());
            if (sink != null) {
                sink.tryEmitNext(session.textMessage(json));
            }
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
        }
    }
    
    /**
     * Send error message
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, Map.of(
            "type", "error",
            "error", error,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Handle vote from WebSocket
     */
    private void handleVote(WebSocketSession session, String debateId, Map<String, Object> data) {
        // TODO: Implement vote handling
        log.info("Vote received via WebSocket - Debate: {}, Data: {}", debateId, data);
        
        // Broadcast vote update to all participants
        broadcastToDebate(debateId, Map.of(
            "type", "vote_update",
            "responseId", data.get("responseId"),
            "voteType", data.get("voteType"),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Handle comment from WebSocket
     */
    private void handleComment(WebSocketSession session, String debateId, Map<String, Object> data) {
        // TODO: Implement comment handling
        log.info("Comment received via WebSocket - Debate: {}, Data: {}", debateId, data);
        
        // Broadcast comment to all participants
        broadcastToDebate(debateId, Map.of(
            "type", "new_comment",
            "comment", data.get("comment"),
            "author", data.get("author"),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Extract debate ID from path
     */
    private String extractDebateId(String path) {
        // Path format: /api/v1/debates/{debateId}/ws
        String[] parts = path.split("/");
        if (parts.length >= 5 && "debates".equals(parts[3]) && "ws".equals(parts[5])) {
            return parts[4];
        }
        return null;
    }
    
    /**
     * Validate access to debate
     */
    private boolean validateAccess(String token, String organizationId, String debateId) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Missing authentication token for WebSocket connection");
            return false;
        }
        
        if (organizationId == null || organizationId.trim().isEmpty()) {
            log.warn("Missing organization ID for WebSocket connection");
            return false;
        }
        
        try {
            // Validate JWT token
            if (!jwtService.isTokenValid(token)) {
                log.warn("Invalid JWT token for WebSocket connection");
                return false;
            }
            
            // Extract organization from token
            String tokenOrgId = jwtService.extractOrganizationId(token);
            if (tokenOrgId == null || !tokenOrgId.equals(organizationId)) {
                log.warn("Organization ID mismatch: token={}, header={}", tokenOrgId, organizationId);
                return false;
            }
            
            // Validate user has access to this debate
            String userId = jwtService.extractUserId(token);
            if (userId == null) {
                log.warn("Unable to extract user ID from token");
                return false;
            }
            
            // Verify user has access to the debate
            try {
                var debate = debateService.getDebate(UUID.fromString(debateId));
                if (!debate.getOrganizationId().toString().equals(organizationId)) {
                    log.warn("Debate organization mismatch: debate belongs to {} but user is in {}", 
                        debate.getOrganizationId(), organizationId);
                    return false;
                }
                
                // Additional permission checks can be added here based on debate settings
                // For example: check if debate is public, if user is participant, etc.
                
            } catch (Exception e) {
                log.warn("Unable to verify debate access", e);
                return false;
            }
            
            log.debug("WebSocket access validated for user {} in organization {} for debate {}", 
                userId, organizationId, debateId);
            return true;
            
        } catch (Exception e) {
            log.error("Error validating WebSocket access", e);
            return false;
        }
    }
    
    /**
     * Cleanup session resources
     */
    private void cleanupSession(String sessionId, String debateId) {
        // Remove from debate sessions
        Map<String, WebSocketSession> sessions = debateSessions.get(debateId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                debateSessions.remove(debateId);
            }
        }
        
        // Remove sink
        Sinks.Many<WebSocketMessage> sink = sessionSinks.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        
        // Notify other participants
        broadcastToDebate(debateId, Map.of(
            "type", "participant_left",
            "sessionId", sessionId,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Get active session count for a debate
     */
    public int getActiveSessionCount(String debateId) {
        Map<String, WebSocketSession> sessions = debateSessions.get(debateId);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * Get total active sessions
     */
    public int getTotalActiveSessions() {
        return debateSessions.values().stream()
            .mapToInt(Map::size)
            .sum();
    }
}