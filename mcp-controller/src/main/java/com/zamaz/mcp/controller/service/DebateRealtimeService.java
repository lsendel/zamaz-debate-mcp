package com.zamaz.mcp.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateRealtimeService {

    private final Map<String, Sinks.Many<Map<String, Object>>> debateEventSinks = new ConcurrentHashMap<>();

    public Flux<Map<String, Object>> subscribeToDebateEvents(String debateId) {
        log.debug("Creating event subscription for debate: {}", debateId);

        Sinks.Many<Map<String, Object>> sink = debateEventSinks.computeIfAbsent(
            debateId, 
            k -> Sinks.many().multicast().onBackpressureBuffer()
        );

        return sink.asFlux()
            .doOnSubscribe(subscription -> 
                log.info("New subscription created for debate: {}", debateId))
            .doOnCancel(() -> 
                log.info("Subscription cancelled for debate: {}", debateId))
            .timeout(Duration.ofHours(1)) // Auto-cleanup after 1 hour
            .doOnError(error -> 
                log.error("Error in debate event stream for {}: {}", debateId, error.getMessage()));
    }

    public void publishDebateEvent(String debateId, Map<String, Object> event) {
        Sinks.Many<Map<String, Object>> sink = debateEventSinks.get(debateId);
        if (sink != null) {
            sink.tryEmitNext(event);
            log.debug("Published event to debate {}: {}", debateId, event.get("type"));
        }
    }

    public void cleanupDebateEvents(String debateId) {
        Sinks.Many<Map<String, Object>> sink = debateEventSinks.remove(debateId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("Cleaned up event subscription for debate: {}", debateId);
        }
    }

    public int getActiveSubscriptionCount() {
        return debateEventSinks.size();
    }
}
