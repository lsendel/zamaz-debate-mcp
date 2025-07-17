package com.zamaz.mcp.common.eventsourcing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL implementation of EventStore
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class PostgreSQLEventStore implements EventStore {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, EventSubscription> subscriptions = new ConcurrentHashMap<>();
    
    private static final String INSERT_EVENT_SQL = """
        INSERT INTO events (event_id, event_type, aggregate_id, aggregate_type, version, 
                           timestamp, user_id, organization_id, correlation_id, payload, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
        """;
    
    private static final String SELECT_EVENTS_BY_AGGREGATE_SQL = """
        SELECT event_id, event_type, aggregate_id, aggregate_type, version, 
               timestamp, user_id, organization_id, correlation_id, payload, metadata
        FROM events 
        WHERE aggregate_id = ? AND version >= ? AND version <= ?
        ORDER BY version ASC
        """;
    
    private static final String SELECT_EVENTS_BY_TYPE_SQL = """
        SELECT event_id, event_type, aggregate_id, aggregate_type, version, 
               timestamp, user_id, organization_id, correlation_id, payload, metadata
        FROM events 
        WHERE event_type = ? AND timestamp >= ? AND timestamp <= ?
        ORDER BY timestamp ASC
        LIMIT ?
        """;
    
    private static final String SELECT_EVENTS_BY_ORGANIZATION_SQL = """
        SELECT event_id, event_type, aggregate_id, aggregate_type, version, 
               timestamp, user_id, organization_id, correlation_id, payload, metadata
        FROM events 
        WHERE organization_id = ? AND timestamp >= ? AND timestamp <= ?
        ORDER BY timestamp ASC
        LIMIT ?
        """;
    
    private static final String SELECT_EVENTS_BY_USER_SQL = """
        SELECT event_id, event_type, aggregate_id, aggregate_type, version, 
               timestamp, user_id, organization_id, correlation_id, payload, metadata
        FROM events 
        WHERE user_id = ? AND timestamp >= ? AND timestamp <= ?
        ORDER BY timestamp ASC
        LIMIT ?
        """;
    
    private static final String SELECT_EVENTS_BY_CORRELATION_SQL = """
        SELECT event_id, event_type, aggregate_id, aggregate_type, version, 
               timestamp, user_id, organization_id, correlation_id, payload, metadata
        FROM events 
        WHERE correlation_id = ?
        ORDER BY timestamp ASC
        """;
    
    private static final String SELECT_CURRENT_VERSION_SQL = """
        SELECT COALESCE(MAX(version), 0) FROM events WHERE aggregate_id = ?
        """;
    
    private static final String INSERT_SNAPSHOT_SQL = """
        INSERT INTO snapshots (aggregate_id, version, timestamp, data)
        VALUES (?, ?, ?, ?::jsonb)
        ON CONFLICT (aggregate_id, version) DO UPDATE SET
        timestamp = EXCLUDED.timestamp,
        data = EXCLUDED.data
        """;
    
    private static final String SELECT_SNAPSHOT_SQL = """
        SELECT aggregate_id, version, timestamp, data
        FROM snapshots
        WHERE aggregate_id = ? AND version <= ?
        ORDER BY version DESC
        LIMIT 1
        """;
    
    @Override
    public CompletableFuture<Void> appendEvents(String aggregateId, long expectedVersion, List<Event> events) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Appending {} events for aggregate {} at version {}", 
                    events.size(), aggregateId, expectedVersion);
                
                // Check current version for optimistic concurrency control
                Long currentVersion = getCurrentVersion(aggregateId).get();
                if (!currentVersion.equals(expectedVersion)) {
                    throw new ConcurrencyException(
                        String.format("Expected version %d but current version is %d for aggregate %s",
                            expectedVersion, currentVersion, aggregateId));
                }
                
                // Insert all events in a batch
                for (Event event : events) {
                    jdbcTemplate.update(INSERT_EVENT_SQL,
                        event.getEventId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getAggregateType(),
                        event.getVersion(),
                        event.getTimestamp(),
                        event.getUserId(),
                        event.getOrganizationId(),
                        event.getCorrelationId(),
                        serializePayload(event.getPayload()),
                        serializePayload(event.getMetadata())
                    );
                }
                
                log.info("Successfully appended {} events for aggregate {}", events.size(), aggregateId);
                
                // Notify subscribers
                notifySubscribers(events);
                
            } catch (DataIntegrityViolationException e) {
                log.error("Concurrency conflict when appending events for aggregate {}", aggregateId, e);
                throw new ConcurrencyException("Concurrency conflict when appending events", e);
            } catch (Exception e) {
                log.error("Error appending events for aggregate {}", aggregateId, e);
                throw new EventStoreException("Error appending events", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Event>> getEvents(String aggregateId, long fromVersion, Long toVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting events for aggregate {} from version {} to {}", 
                    aggregateId, fromVersion, toVersion);
                
                long endVersion = toVersion != null ? toVersion : Long.MAX_VALUE;
                
                List<Event> events = jdbcTemplate.query(SELECT_EVENTS_BY_AGGREGATE_SQL,
                    new EventRowMapper(), aggregateId, fromVersion, endVersion);
                
                log.debug("Retrieved {} events for aggregate {}", events.size(), aggregateId);
                return events;
                
            } catch (Exception e) {
                log.error("Error retrieving events for aggregate {}", aggregateId, e);
                throw new EventStoreException("Error retrieving events", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Event>> getEvents(String aggregateId) {
        return getEvents(aggregateId, 1, null);
    }
    
    @Override
    public CompletableFuture<List<Event>> getEventsByType(String eventType, LocalDateTime fromTimestamp, 
                                                         LocalDateTime toTimestamp, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting events by type {} from {} to {} with limit {}", 
                    eventType, fromTimestamp, toTimestamp, limit);
                
                List<Event> events = jdbcTemplate.query(SELECT_EVENTS_BY_TYPE_SQL,
                    new EventRowMapper(), eventType, fromTimestamp, toTimestamp, limit);
                
                log.debug("Retrieved {} events by type {}", events.size(), eventType);
                return events;
                
            } catch (Exception e) {
                log.error("Error retrieving events by type {}", eventType, e);
                throw new EventStoreException("Error retrieving events by type", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Event>> getEventsByOrganization(String organizationId, LocalDateTime fromTimestamp,
                                                                 LocalDateTime toTimestamp, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting events by organization {} from {} to {} with limit {}", 
                    organizationId, fromTimestamp, toTimestamp, limit);
                
                List<Event> events = jdbcTemplate.query(SELECT_EVENTS_BY_ORGANIZATION_SQL,
                    new EventRowMapper(), organizationId, fromTimestamp, toTimestamp, limit);
                
                log.debug("Retrieved {} events by organization {}", events.size(), organizationId);
                return events;
                
            } catch (Exception e) {
                log.error("Error retrieving events by organization {}", organizationId, e);
                throw new EventStoreException("Error retrieving events by organization", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Event>> getEventsByUser(String userId, LocalDateTime fromTimestamp,
                                                         LocalDateTime toTimestamp, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting events by user {} from {} to {} with limit {}", 
                    userId, fromTimestamp, toTimestamp, limit);
                
                List<Event> events = jdbcTemplate.query(SELECT_EVENTS_BY_USER_SQL,
                    new EventRowMapper(), userId, fromTimestamp, toTimestamp, limit);
                
                log.debug("Retrieved {} events by user {}", events.size(), userId);
                return events;
                
            } catch (Exception e) {
                log.error("Error retrieving events by user {}", userId, e);
                throw new EventStoreException("Error retrieving events by user", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Event>> getEventsByCorrelationId(String correlationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting events by correlation ID {}", correlationId);
                
                List<Event> events = jdbcTemplate.query(SELECT_EVENTS_BY_CORRELATION_SQL,
                    new EventRowMapper(), correlationId);
                
                log.debug("Retrieved {} events by correlation ID {}", events.size(), correlationId);
                return events;
                
            } catch (Exception e) {
                log.error("Error retrieving events by correlation ID {}", correlationId, e);
                throw new EventStoreException("Error retrieving events by correlation ID", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> getCurrentVersion(String aggregateId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long version = jdbcTemplate.queryForObject(SELECT_CURRENT_VERSION_SQL, Long.class, aggregateId);
                return version != null ? version : 0L;
            } catch (Exception e) {
                log.error("Error getting current version for aggregate {}", aggregateId, e);
                throw new EventStoreException("Error getting current version", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<Snapshot>> getSnapshot(String aggregateId, long version) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting snapshot for aggregate {} at version {}", aggregateId, version);
                
                List<Snapshot> snapshots = jdbcTemplate.query(SELECT_SNAPSHOT_SQL,
                    new SnapshotRowMapper(), aggregateId, version);
                
                Optional<Snapshot> snapshot = snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.get(0));
                log.debug("Retrieved snapshot for aggregate {}: {}", aggregateId, snapshot.isPresent());
                return snapshot;
                
            } catch (Exception e) {
                log.error("Error retrieving snapshot for aggregate {}", aggregateId, e);
                throw new EventStoreException("Error retrieving snapshot", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> saveSnapshot(Snapshot snapshot) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Saving snapshot for aggregate {} at version {}", 
                    snapshot.getAggregateId(), snapshot.getVersion());
                
                jdbcTemplate.update(INSERT_SNAPSHOT_SQL,
                    snapshot.getAggregateId(),
                    snapshot.getVersion(),
                    snapshot.getTimestamp(),
                    serializePayload(snapshot.getData())
                );
                
                log.info("Successfully saved snapshot for aggregate {} at version {}", 
                    snapshot.getAggregateId(), snapshot.getVersion());
                
            } catch (Exception e) {
                log.error("Error saving snapshot for aggregate {}", snapshot.getAggregateId(), e);
                throw new EventStoreException("Error saving snapshot", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<EventSubscription> subscribe(String subscriptionId, List<String> eventTypes,
                                                         LocalDateTime fromTimestamp, EventHandler handler) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Creating subscription {} for event types {} from {}", 
                subscriptionId, eventTypes, fromTimestamp);
            
            EventSubscription subscription = new EventSubscription(subscriptionId, eventTypes, fromTimestamp, handler);
            subscriptions.put(subscriptionId, subscription);
            
            log.info("Successfully created subscription {}", subscriptionId);
            return subscription;
        });
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String subscriptionId) {
        return CompletableFuture.runAsync(() -> {
            log.info("Cancelling subscription {}", subscriptionId);
            subscriptions.remove(subscriptionId);
            log.info("Successfully cancelled subscription {}", subscriptionId);
        });
    }
    
    private void notifySubscribers(List<Event> events) {
        subscriptions.values().forEach(subscription -> {
            try {
                subscription.getHandler().handle(events);
            } catch (Exception e) {
                log.error("Error notifying subscription {}", subscription.getSubscriptionId(), e);
            }
        });
    }
    
    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializing payload", e);
            throw new EventStoreException("Error serializing payload", e);
        }
    }
    
    private Object deserializePayload(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, Object.class);
        } catch (Exception e) {
            log.error("Error deserializing payload", e);
            throw new EventStoreException("Error deserializing payload", e);
        }
    }
    
    private class EventRowMapper implements RowMapper<Event> {
        @Override
        public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            return BaseEvent.builder()
                .eventId(UUID.fromString(rs.getString("event_id")))
                .eventType(rs.getString("event_type"))
                .aggregateId(rs.getString("aggregate_id"))
                .aggregateType(rs.getString("aggregate_type"))
                .version(rs.getLong("version"))
                .timestamp(rs.getTimestamp("timestamp").toLocalDateTime())
                .userId(rs.getString("user_id"))
                .organizationId(rs.getString("organization_id"))
                .correlationId(rs.getString("correlation_id"))
                .payload(deserializePayload(rs.getString("payload")))
                .metadata(deserializePayload(rs.getString("metadata")))
                .build();
        }
    }
    
    private class SnapshotRowMapper implements RowMapper<Snapshot> {
        @Override
        public Snapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Snapshot.builder()
                .aggregateId(rs.getString("aggregate_id"))
                .version(rs.getLong("version"))
                .timestamp(rs.getTimestamp("timestamp").toLocalDateTime())
                .data(deserializePayload(rs.getString("data")))
                .build();
        }
    }
    
    public static class EventStoreException extends RuntimeException {
        public EventStoreException(String message) {
            super(message);
        }
        
        public EventStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) {
            super(message);
        }
        
        public ConcurrencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}