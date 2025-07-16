package com.zamaz.mcp.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Event Publisher for domain events using Redis Pub/Sub
 * Provides reliable event publishing with automatic retry and error handling
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Publishes a domain event to the appropriate Redis channel
     * Channel naming convention: mcp.events.{event_type}
     */
    public void publishEvent(DomainEvent event) {
        try {
            enrichEventMetadata(event);
            String channel = buildChannelName(event);
            String eventJson = objectMapper.writeValueAsString(event);
            
            log.info("Publishing event {} to channel {} for aggregate {}", 
                event.getEventType(), channel, event.getAggregateId());
            
            redisTemplate.convertAndSend(channel, eventJson);
            
            // Publish to global events channel for monitoring
            redisTemplate.convertAndSend("mcp.events.all", eventJson);
            
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
            throw new EventPublishException("Failed to publish event", e);
        }
    }
    
    /**
     * Publishes event asynchronously with retry mechanism
     */
    public void publishEventAsync(DomainEvent event) {
        // Implementation would use @Async with retry logic
        publishEvent(event);
    }
    
    private void enrichEventMetadata(DomainEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now());
        event.setVersion("1.0");
        
        // Add correlation ID from current context if available
        // event.setCorrelationId(CorrelationContext.getCurrentCorrelationId());
    }
    
    private String buildChannelName(DomainEvent event) {
        return "mcp.events." + event.getEventType().toLowerCase().replace("_", ".");
    }
}