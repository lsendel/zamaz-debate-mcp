package com.zamaz.mcp.common.eventsourcing;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for event store operations
 */
public interface EventStore {
    
    /**
     * Append events to the event store
     * 
     * @param aggregateId The aggregate identifier
     * @param expectedVersion The expected version for optimistic concurrency control
     * @param events The events to append
     * @return CompletableFuture that completes when events are stored
     */
    CompletableFuture<Void> appendEvents(String aggregateId, long expectedVersion, List<Event> events);
    
    /**
     * Get events for a specific aggregate
     * 
     * @param aggregateId The aggregate identifier
     * @param fromVersion The version to start from (inclusive)
     * @param toVersion The version to end at (inclusive), null for all
     * @return CompletableFuture containing the events
     */
    CompletableFuture<List<Event>> getEvents(String aggregateId, long fromVersion, Long toVersion);
    
    /**
     * Get all events for a specific aggregate
     * 
     * @param aggregateId The aggregate identifier
     * @return CompletableFuture containing all events
     */
    CompletableFuture<List<Event>> getEvents(String aggregateId);
    
    /**
     * Get events by type within a time range
     * 
     * @param eventType The event type to filter by
     * @param fromTimestamp The start timestamp (inclusive)
     * @param toTimestamp The end timestamp (inclusive)
     * @param limit Maximum number of events to return
     * @return CompletableFuture containing the events
     */
    CompletableFuture<List<Event>> getEventsByType(String eventType, java.time.LocalDateTime fromTimestamp, 
                                                   java.time.LocalDateTime toTimestamp, int limit);
    
    /**
     * Get events for a specific organization
     * 
     * @param organizationId The organization identifier
     * @param fromTimestamp The start timestamp (inclusive)
     * @param toTimestamp The end timestamp (inclusive)
     * @param limit Maximum number of events to return
     * @return CompletableFuture containing the events
     */
    CompletableFuture<List<Event>> getEventsByOrganization(String organizationId, java.time.LocalDateTime fromTimestamp,
                                                           java.time.LocalDateTime toTimestamp, int limit);
    
    /**
     * Get events by user
     * 
     * @param userId The user identifier
     * @param fromTimestamp The start timestamp (inclusive)
     * @param toTimestamp The end timestamp (inclusive)
     * @param limit Maximum number of events to return
     * @return CompletableFuture containing the events
     */
    CompletableFuture<List<Event>> getEventsByUser(String userId, java.time.LocalDateTime fromTimestamp,
                                                   java.time.LocalDateTime toTimestamp, int limit);
    
    /**
     * Get events by correlation ID
     * 
     * @param correlationId The correlation identifier
     * @return CompletableFuture containing related events
     */
    CompletableFuture<List<Event>> getEventsByCorrelationId(String correlationId);
    
    /**
     * Get the current version of an aggregate
     * 
     * @param aggregateId The aggregate identifier
     * @return CompletableFuture containing the current version
     */
    CompletableFuture<Long> getCurrentVersion(String aggregateId);
    
    /**
     * Get a snapshot of an aggregate at a specific version
     * 
     * @param aggregateId The aggregate identifier
     * @param version The version to get snapshot for
     * @return CompletableFuture containing the snapshot if available
     */
    CompletableFuture<Optional<Snapshot>> getSnapshot(String aggregateId, long version);
    
    /**
     * Save a snapshot of an aggregate
     * 
     * @param snapshot The snapshot to save
     * @return CompletableFuture that completes when snapshot is saved
     */
    CompletableFuture<Void> saveSnapshot(Snapshot snapshot);
    
    /**
     * Create a subscription to events
     * 
     * @param subscriptionId Unique identifier for the subscription
     * @param eventTypes Types of events to subscribe to (null for all)
     * @param fromTimestamp Start timestamp for events
     * @param handler Handler for processing events
     * @return CompletableFuture containing the subscription
     */
    CompletableFuture<EventSubscription> subscribe(String subscriptionId, List<String> eventTypes,
                                                  java.time.LocalDateTime fromTimestamp, EventHandler handler);
    
    /**
     * Cancel a subscription
     * 
     * @param subscriptionId The subscription identifier
     * @return CompletableFuture that completes when subscription is cancelled
     */
    CompletableFuture<Void> unsubscribe(String subscriptionId);
}