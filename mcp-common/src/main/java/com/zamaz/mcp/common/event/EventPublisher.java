package com.zamaz.mcp.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public void publishEvent(DomainEvent event) {
        try {
            // Enrich event with metadata
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(Instant.now());
            event.setVersion("1.0");
            
            String channel = getChannelName(event);
            String eventJson = objectMapper.writeValueAsString(event);
            
            log.info("Publishing event {} to channel {}", event.getEventType(), channel);
            redisTemplate.convertAndSend(channel, eventJson);
            
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
            throw new EventPublishException("Failed to publish event", e);
        }
    }
    
    private String getChannelName(DomainEvent event) {
        return "mcp.events." + event.getEventType().toLowerCase().replace("_", ".");
    }
}