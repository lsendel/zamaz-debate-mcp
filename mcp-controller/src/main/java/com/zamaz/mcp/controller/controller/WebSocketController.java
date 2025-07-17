package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.service.DebateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * Controller for WebSocket and Server-Sent Events endpoints
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    
    private final DebateService debateService;
    
    /**
     * Server-Sent Events endpoint for debate updates (fallback for WebSocket)
     */
    @GetMapping(value = "/debates/{debateId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> debateEvents(
            @PathVariable String debateId,
            @RequestHeader("X-Organization-ID") String organizationId) {
        
        log.info("SSE connection established for debate: {} by organization: {}", debateId, organizationId);
        
        return debateService.subscribeToDebateEvents(debateId)
            .map(event -> {
                try {
                    // Format as SSE
                    String eventType = (String) event.get("type");
                    String data = com.fasterxml.jackson.databind.ObjectMapper.builder().build()
                        .writeValueAsString(event);
                    
                    return String.format("event: %s\ndata: %s\n\n", eventType, data);
                } catch (Exception e) {
                    log.error("Error formatting SSE event", e);
                    return "event: error\ndata: {\"error\":\"Internal error\"}\n\n";
                }
            })
            .mergeWith(
                // Send heartbeat every 30 seconds
                Flux.interval(Duration.ofSeconds(30))
                    .map(tick -> "event: heartbeat\ndata: {\"timestamp\":" + System.currentTimeMillis() + "}\n\n")
            )
            .doOnSubscribe(subscription -> 
                log.info("SSE subscription started for debate: {}", debateId))
            .doOnCancel(() -> 
                log.info("SSE subscription cancelled for debate: {}", debateId))
            .doOnError(error -> 
                log.error("SSE error for debate {}: {}", debateId, error.getMessage()));
    }
    
    /**
     * Get WebSocket connection statistics
     */
    @GetMapping("/websocket/stats")
    public ResponseEntity<Map<String, Object>> getWebSocketStats() {
        int activeSubscriptions = debateService.getActiveSubscriptionCount();
        
        Map<String, Object> stats = Map.of(
            "activeSubscriptions", activeSubscriptions,
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Trigger a test event for debugging
     */
    @PostMapping("/debates/{debateId}/test-event")
    public ResponseEntity<String> triggerTestEvent(
            @PathVariable String debateId,
            @RequestBody Map<String, Object> eventData) {
        
        log.info("Triggering test event for debate: {}", debateId);
        
        Map<String, Object> testEvent = Map.of(
            "type", "test_event",
            "debateId", debateId,
            "data", eventData,
            "timestamp", System.currentTimeMillis()
        );
        
        debateService.publishDebateEvent(debateId, testEvent);
        
        return ResponseEntity.ok("Test event published");
    }
}