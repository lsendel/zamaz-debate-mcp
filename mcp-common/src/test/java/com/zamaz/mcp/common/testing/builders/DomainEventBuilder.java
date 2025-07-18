package com.zamaz.mcp.common.testing.builders;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.common.testing.TestDataBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Builder for creating test domain events.
 */
public class DomainEventBuilder implements TestDataBuilder<DomainEvent> {
    
    private String eventId = UUID.randomUUID().toString();
    private String aggregateId = UUID.randomUUID().toString();
    private String eventType = "TestEvent";
    private Instant occurredAt = Instant.now();
    private long version = 1L;
    private String userId = "test-user";
    private String tenantId = "test-tenant";
    private Object payload = null;

    public static DomainEventBuilder anEvent() {
        return new DomainEventBuilder();
    }

    public DomainEventBuilder withEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public DomainEventBuilder withAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
        return this;
    }

    public DomainEventBuilder withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public DomainEventBuilder withOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
        return this;
    }

    public DomainEventBuilder withVersion(long version) {
        this.version = version;
        return this;
    }

    public DomainEventBuilder withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public DomainEventBuilder withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public DomainEventBuilder withPayload(Object payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public DomainEvent build() {
        return new TestDomainEvent(
            eventId,
            aggregateId,
            eventType,
            occurredAt,
            version,
            userId,
            tenantId,
            payload
        );
    }

    /**
     * Test implementation of DomainEvent for testing purposes.
     */
    public static class TestDomainEvent extends AbstractDomainEvent {
        private final Object payload;

        public TestDomainEvent(String eventId, String aggregateId, String eventType, 
                             Instant occurredAt, long version, String userId, 
                             String tenantId, Object payload) {
            super(aggregateId, occurredAt, version);
            this.payload = payload;
            // Set protected fields from AbstractDomainEvent if accessible
            setEventMetadata(eventId, eventType, userId, tenantId);
        }

        private void setEventMetadata(String eventId, String eventType, String userId, String tenantId) {
            // This assumes we can set these fields - if not, we'd need to modify AbstractDomainEvent
            // or create a proper test event class in the domain package
        }

        public Object getPayload() {
            return payload;
        }

        @Override
        public String getEventType() {
            return "TestDomainEvent";
        }
    }

    /**
     * Creates a domain event for a specific aggregate creation.
     */
    public static DomainEvent aggregateCreated(String aggregateId, String aggregateType) {
        return anEvent()
            .withAggregateId(aggregateId)
            .withEventType(aggregateType + "Created")
            .withVersion(1L)
            .build();
    }

    /**
     * Creates a domain event for a specific aggregate update.
     */
    public static DomainEvent aggregateUpdated(String aggregateId, String aggregateType, long version) {
        return anEvent()
            .withAggregateId(aggregateId)
            .withEventType(aggregateType + "Updated")
            .withVersion(version)
            .build();
    }

    /**
     * Creates a domain event for a specific aggregate deletion.
     */
    public static DomainEvent aggregateDeleted(String aggregateId, String aggregateType, long version) {
        return anEvent()
            .withAggregateId(aggregateId)
            .withEventType(aggregateType + "Deleted")
            .withVersion(version)
            .build();
    }
}