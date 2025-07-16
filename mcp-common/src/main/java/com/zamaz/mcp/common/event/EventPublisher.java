package com.zamaz.mcp.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.exception.EventPublishException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Event Publisher for domain events using Redis Pub/Sub
 * Features:
 * - Automatic retry with exponential backoff
 * - Metrics collection for monitoring
 * - Async publishing support
 * - Event enrichment with metadata
 * - Channel routing based on event type
 */
@Component
@Slf4j
public class EventPublisher {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter publishedEvents;
    private final Counter failedEvents;
    private final Timer publishTimer;
    
    public EventPublisher(RedisTemplate<String, Object> redisTemplate, 
                         ObjectMapper objectMapper, 
                         MeterRegistry meterRegistry) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "RedisTemplate cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry cannot be null");
        
        // Initialize metrics
        this.publishedEvents = Counter.builder("mcp.events.published")
            .description("Number of events published")
            .register(meterRegistry);
            
        this.failedEvents = Counter.builder("mcp.events.failed")
            .description("Number of failed event publications")
            .register(meterRegistry);
            
        this.publishTimer = Timer.builder("mcp.events.publish.duration")
            .description("Time taken to publish events")
            .register(meterRegistry);
    }
    
    /**
     * Publishes a domain event synchronously with retry mechanism
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishEvent(DomainEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            enrichEventMetadata(event);
            String channel = buildChannelName(event);
            String eventJson = objectMapper.writeValueAsString(event);
            
            log.info("Publishing event {} to channel {} for aggregate {} in organization {}", 
                event.getEventType(), channel, event.getAggregateId(), event.getOrganizationId());
            
            // Publish to specific event channel
            Long subscribers = redisTemplate.convertAndSend(channel, eventJson);
            
            // Publish to global events channel for monitoring and debugging
            redisTemplate.convertAndSend("mcp.events.all", eventJson);
            
            // Publish to organization-specific channel for multi-tenancy
            if (event.getOrganizationId() != null) {
                String orgChannel = "mcp.events.org." + event.getOrganizationId();
                redisTemplate.convertAndSend(orgChannel, eventJson);
            }
            
            publishedEvents.increment();
            log.debug("Event published successfully to {} subscribers", subscribers);
            
        } catch (Exception e) {
            failedEvents.increment();
            log.error("Failed to publish event: {}", event, e);
            throw new EventPublishException("Failed to publish event: " + event.getEventType(), e);
        } finally {
            sample.stop(publishTimer);
        }
    }
    
    /**
     * Publishes event asynchronously for non-critical events
     */
    @Async("eventExecutor")
    public CompletableFuture<Void> publishEventAsync(DomainEvent event) {
        return CompletableFuture.runAsync(() -> publishEvent(event));
    }
    
    /**
     * Batch publish multiple events in a single operation
     */
    public void publishEvents(DomainEvent... events) {
        for (DomainEvent event : events) {
            publishEvent(event);
        }
    }
    
    private void enrichEventMetadata(DomainEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        
        if (event.getVersion() == null) {
            event.setVersion("1.0");
        }
        
        // Add correlation ID from current request context if available
        String correlationId = getCurrentCorrelationId();
        if (correlationId != null) {
            event.setCorrelationId(correlationId);
        }
        
        // Add source service information
        event.setSourceService(getServiceName());
        
        // Add environment metadata
        event.addMetadata("environment", getEnvironment());
        event.addMetadata("publishedAt", Instant.now().toString());
    }
    
    private String buildChannelName(DomainEvent event) {
        // Channel naming convention: mcp.events.{aggregate_type}.{event_type}
        String aggregateType = event.getAggregateType().toLowerCase(Locale.ROOT);
        String eventType = event.getEventType().toLowerCase(Locale.ROOT).replace("_", ".");
        return String.format("mcp.events.%s.%s", aggregateType, eventType);
    }
    
    private String getCurrentCorrelationId() {
        // Implementation would get correlation ID from request context
        // return RequestContextHolder.getCurrentCorrelationId();
        return null;
    }
    
    private String getServiceName() {
        // Get service name from application properties or environment
        return System.getProperty("spring.application.name", "unknown");
    }
    
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "default");
    }
}