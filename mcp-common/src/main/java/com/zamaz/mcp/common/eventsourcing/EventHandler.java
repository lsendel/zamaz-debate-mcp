package com.zamaz.mcp.common.eventsourcing;

import java.util.List;

/**
 * Interface for handling events from event subscriptions
 */
@FunctionalInterface
public interface EventHandler {
    
    /**
     * Handle a list of events
     * 
     * @param events The events to handle
     */
    void handle(List<Event> events);
}