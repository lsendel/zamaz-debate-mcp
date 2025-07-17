package com.zamaz.mcp.common.eventsourcing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collection for event sourcing operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventSourcingMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter eventsAppended = Counter.builder("eventsourcing.events.appended")
        .description("Total number of events appended to the event store")
        .register(meterRegistry);
    
    private final Counter eventsRetrieved = Counter.builder("eventsourcing.events.retrieved")
        .description("Total number of events retrieved from the event store")
        .register(meterRegistry);
    
    private final Counter snapshotsSaved = Counter.builder("eventsourcing.snapshots.saved")
        .description("Total number of snapshots saved")
        .register(meterRegistry);
    
    private final Counter snapshotsLoaded = Counter.builder("eventsourcing.snapshots.loaded")
        .description("Total number of snapshots loaded")
        .register(meterRegistry);
    
    private final Counter concurrencyConflicts = Counter.builder("eventsourcing.concurrency.conflicts")
        .description("Total number of concurrency conflicts")
        .register(meterRegistry);
    
    private final Counter subscriptions = Counter.builder("eventsourcing.subscriptions.created")
        .description("Total number of event subscriptions created")
        .register(meterRegistry);
    
    private final Counter subscriptionEvents = Counter.builder("eventsourcing.subscriptions.events")
        .description("Total number of events delivered to subscriptions")
        .register(meterRegistry);
    
    // Timers
    private final Timer appendTimer = Timer.builder("eventsourcing.append.duration")
        .description("Duration of event append operations")
        .register(meterRegistry);
    
    private final Timer retrieveTimer = Timer.builder("eventsourcing.retrieve.duration")
        .description("Duration of event retrieval operations")
        .register(meterRegistry);
    
    private final Timer snapshotSaveTimer = Timer.builder("eventsourcing.snapshot.save.duration")
        .description("Duration of snapshot save operations")
        .register(meterRegistry);
    
    private final Timer snapshotLoadTimer = Timer.builder("eventsourcing.snapshot.load.duration")
        .description("Duration of snapshot load operations")
        .register(meterRegistry);
    
    // Gauges
    private final AtomicLong activeSubscriptions = new AtomicLong(0);
    private final AtomicLong eventStoreSize = new AtomicLong(0);
    private final AtomicLong snapshotStoreSize = new AtomicLong(0);
    
    public EventSourcingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register gauges
        Gauge.builder("eventsourcing.subscriptions.active")
            .description("Number of active event subscriptions")
            .register(meterRegistry, activeSubscriptions, AtomicLong::get);
        
        Gauge.builder("eventsourcing.eventstore.size")
            .description("Total number of events in the event store")
            .register(meterRegistry, eventStoreSize, AtomicLong::get);
        
        Gauge.builder("eventsourcing.snapshotstore.size")
            .description("Total number of snapshots in the snapshot store")
            .register(meterRegistry, snapshotStoreSize, AtomicLong::get);
    }
    
    /**
     * Record event append operation
     */
    public Timer.Sample startAppendTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordEventAppended(Timer.Sample sample, int eventCount, String aggregateType) {
        sample.stop(appendTimer.withTags("aggregateType", aggregateType));
        eventsAppended.increment(eventCount);
        eventStoreSize.addAndGet(eventCount);
        log.debug("Recorded {} events appended for aggregate type {}", eventCount, aggregateType);
    }
    
    /**
     * Record event retrieval operation
     */
    public Timer.Sample startRetrieveTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordEventsRetrieved(Timer.Sample sample, int eventCount, String aggregateType) {
        sample.stop(retrieveTimer.withTags("aggregateType", aggregateType));
        eventsRetrieved.increment(eventCount);
        log.debug("Recorded {} events retrieved for aggregate type {}", eventCount, aggregateType);
    }
    
    /**
     * Record snapshot save operation
     */
    public Timer.Sample startSnapshotSaveTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordSnapshotSaved(Timer.Sample sample, String aggregateType) {
        sample.stop(snapshotSaveTimer.withTags("aggregateType", aggregateType));
        snapshotsSaved.increment();
        snapshotStoreSize.incrementAndGet();
        log.debug("Recorded snapshot saved for aggregate type {}", aggregateType);
    }
    
    /**
     * Record snapshot load operation
     */
    public Timer.Sample startSnapshotLoadTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordSnapshotLoaded(Timer.Sample sample, String aggregateType, boolean found) {
        sample.stop(snapshotLoadTimer.withTags("aggregateType", aggregateType, "found", String.valueOf(found)));
        if (found) {
            snapshotsLoaded.increment();
        }
        log.debug("Recorded snapshot load for aggregate type {} (found: {})", aggregateType, found);
    }
    
    /**
     * Record concurrency conflict
     */
    public void recordConcurrencyConflict(String aggregateType) {
        concurrencyConflicts.increment(Tags.of("aggregateType", aggregateType));
        log.debug("Recorded concurrency conflict for aggregate type {}", aggregateType);
    }
    
    /**
     * Record subscription creation
     */
    public void recordSubscriptionCreated(String subscriptionId) {
        subscriptions.increment(Tags.of("subscriptionId", subscriptionId));
        activeSubscriptions.incrementAndGet();
        log.debug("Recorded subscription created: {}", subscriptionId);
    }
    
    /**
     * Record subscription removal
     */
    public void recordSubscriptionRemoved(String subscriptionId) {
        activeSubscriptions.decrementAndGet();
        log.debug("Recorded subscription removed: {}", subscriptionId);
    }
    
    /**
     * Record event delivered to subscription
     */
    public void recordSubscriptionEvent(String subscriptionId, String eventType) {
        subscriptionEvents.increment(Tags.of("subscriptionId", subscriptionId, "eventType", eventType));
        log.debug("Recorded event {} delivered to subscription {}", eventType, subscriptionId);
    }
    
    /**
     * Update event store size gauge
     */
    public void updateEventStoreSize(long size) {
        eventStoreSize.set(size);
    }
    
    /**
     * Update snapshot store size gauge
     */
    public void updateSnapshotStoreSize(long size) {
        snapshotStoreSize.set(size);
    }
    
    /**
     * Get current metrics summary
     */
    public EventSourcingMetricsSummary getMetricsSummary() {
        return EventSourcingMetricsSummary.builder()
            .totalEventsAppended((long) eventsAppended.count())
            .totalEventsRetrieved((long) eventsRetrieved.count())
            .totalSnapshotsSaved((long) snapshotsSaved.count())
            .totalSnapshotsLoaded((long) snapshotsLoaded.count())
            .totalConcurrencyConflicts((long) concurrencyConflicts.count())
            .totalSubscriptions((long) subscriptions.count())
            .activeSubscriptions(activeSubscriptions.get())
            .eventStoreSize(eventStoreSize.get())
            .snapshotStoreSize(snapshotStoreSize.get())
            .averageAppendDuration(appendTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .averageRetrieveDuration(retrieveTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .averageSnapshotSaveDuration(snapshotSaveTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .averageSnapshotLoadDuration(snapshotLoadTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .build();
    }
    
    /**
     * Helper class for Tags
     */
    private static class Tags {
        public static io.micrometer.core.instrument.Tags of(String key, String value) {
            return io.micrometer.core.instrument.Tags.of(key, value);
        }
    }
    
    /**
     * Metrics summary data class
     */
    @lombok.Builder
    @lombok.Data
    public static class EventSourcingMetricsSummary {
        private long totalEventsAppended;
        private long totalEventsRetrieved;
        private long totalSnapshotsSaved;
        private long totalSnapshotsLoaded;
        private long totalConcurrencyConflicts;
        private long totalSubscriptions;
        private long activeSubscriptions;
        private long eventStoreSize;
        private long snapshotStoreSize;
        private double averageAppendDuration;
        private double averageRetrieveDuration;
        private double averageSnapshotSaveDuration;
        private double averageSnapshotLoadDuration;
    }
}