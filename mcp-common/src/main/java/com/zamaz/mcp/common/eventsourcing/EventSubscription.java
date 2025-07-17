package com.zamaz.mcp.common.eventsourcing;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a subscription to events in the event store
 */
@Data
@RequiredArgsConstructor
public class EventSubscription {
    
    /**
     * Unique identifier for the subscription
     */
    private final String subscriptionId;
    
    /**
     * Types of events to subscribe to (null for all)
     */
    private final List<String> eventTypes;
    
    /**
     * Starting timestamp for events
     */
    private final LocalDateTime fromTimestamp;
    
    /**
     * Handler for processing events
     */
    private final EventHandler handler;
}