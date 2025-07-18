package com.zamaz.mcp.common.testing.builders;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.testing.TestDataBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for creating test aggregate roots.
 */
public class AggregateRootBuilder<ID> implements TestDataBuilder<TestAggregate<ID>> {
    
    private ID id;
    private long version = 0L;
    private Instant createdAt = Instant.now();
    private Instant lastModifiedAt = Instant.now();
    private String createdBy = "test-user";
    private String lastModifiedBy = "test-user";
    private List<DomainEvent> pendingEvents = new ArrayList<>();
    private String name = "Test Aggregate";
    private String description = "Test Description";

    @SuppressWarnings("unchecked")
    public static <ID> AggregateRootBuilder<ID> anAggregate() {
        AggregateRootBuilder<ID> builder = new AggregateRootBuilder<>();
        builder.id = (ID) UUID.randomUUID().toString();
        return builder;
    }

    public static AggregateRootBuilder<String> aStringAggregate() {
        return new AggregateRootBuilder<String>().withId(UUID.randomUUID().toString());
    }

    public static AggregateRootBuilder<Long> aLongAggregate() {
        return new AggregateRootBuilder<Long>().withId(System.currentTimeMillis());
    }

    public AggregateRootBuilder<ID> withId(ID id) {
        this.id = id;
        return this;
    }

    public AggregateRootBuilder<ID> withVersion(long version) {
        this.version = version;
        return this;
    }

    public AggregateRootBuilder<ID> withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public AggregateRootBuilder<ID> withLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    public AggregateRootBuilder<ID> withCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public AggregateRootBuilder<ID> withLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        return this;
    }

    public AggregateRootBuilder<ID> withPendingEvent(DomainEvent event) {
        this.pendingEvents.add(event);
        return this;
    }

    public AggregateRootBuilder<ID> withPendingEvents(List<DomainEvent> events) {
        this.pendingEvents.addAll(events);
        return this;
    }

    public AggregateRootBuilder<ID> withName(String name) {
        this.name = name;
        return this;
    }

    public AggregateRootBuilder<ID> withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public TestAggregate<ID> build() {
        TestAggregate<ID> aggregate = new TestAggregate<>(
            id, version, createdAt, lastModifiedAt, 
            createdBy, lastModifiedBy, name, description
        );
        
        // Add pending events if any
        pendingEvents.forEach(aggregate::raiseEvent);
        
        return aggregate;
    }

    /**
     * Test implementation of AggregateRoot for testing purposes.
     */
    public static class TestAggregate<ID> extends AggregateRoot<ID> {
        private String name;
        private String description;

        public TestAggregate(ID id, long version, Instant createdAt, Instant lastModifiedAt,
                           String createdBy, String lastModifiedBy, String name, String description) {
            super(id);
            this.name = name;
            this.description = description;
            
            // Set audit fields if the aggregate supports them
            setAuditFields(createdAt, lastModifiedAt, createdBy, lastModifiedBy);
            setVersion(version);
        }

        private void setAuditFields(Instant createdAt, Instant lastModifiedAt, 
                                   String createdBy, String lastModifiedBy) {
            // This would need to be implemented based on the actual AggregateRoot implementation
        }

        private void setVersion(long version) {
            // This would need to be implemented based on the actual AggregateRoot implementation
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            raiseEvent(DomainEventBuilder.anEvent()
                .withAggregateId(getId().toString())
                .withEventType("TestAggregateNameChanged")
                .withPayload(name)
                .build());
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
            raiseEvent(DomainEventBuilder.anEvent()
                .withAggregateId(getId().toString())
                .withEventType("TestAggregateDescriptionChanged")
                .withPayload(description)
                .build());
        }

        public void doBusinessOperation(String parameter) {
            // Simulate a business operation that raises an event
            raiseEvent(DomainEventBuilder.anEvent()
                .withAggregateId(getId().toString())
                .withEventType("BusinessOperationPerformed")
                .withPayload(parameter)
                .build());
        }
    }

    /**
     * Creates a simple aggregate with default values.
     */
    public static <ID> TestAggregate<ID> simpleAggregate(ID id) {
        return AggregateRootBuilder.<ID>anAggregate()
            .withId(id)
            .build();
    }

    /**
     * Creates an aggregate with pending events.
     */
    public static <ID> TestAggregate<ID> aggregateWithEvents(ID id, DomainEvent... events) {
        return AggregateRootBuilder.<ID>anAggregate()
            .withId(id)
            .withPendingEvents(List.of(events))
            .build();
    }

    /**
     * Creates an aggregate with a specific version.
     */
    public static <ID> TestAggregate<ID> aggregateWithVersion(ID id, long version) {
        return AggregateRootBuilder.<ID>anAggregate()
            .withId(id)
            .withVersion(version)
            .build();
    }
}