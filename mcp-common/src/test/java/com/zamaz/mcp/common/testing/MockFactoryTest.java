package com.zamaz.mcp.common.testing;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.common.testing.annotations.DomainTest;
import com.zamaz.mcp.common.testing.annotations.FastTest;
import com.zamaz.mcp.common.testing.builders.AggregateRootBuilder;
import com.zamaz.mcp.common.testing.builders.DomainEventBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@DomainTest
@FastTest
class MockFactoryTest {

    @Test
    void mockEventPublisher_shouldCapturePublishedEvents() {
        // Given
        MockFactory.MockEventPublisher publisher = MockFactory.mockEventPublisher();
        DomainEvent event1 = DomainEventBuilder.anEvent().withEventType("Event1").build();
        DomainEvent event2 = DomainEventBuilder.anEvent().withEventType("Event2").build();

        // When
        publisher.publish(event1);
        publisher.publish(event2);

        // Then
        assertThat(publisher.getPublishedEvents()).hasSize(2);
        assertThat(publisher.getEventCount()).isEqualTo(2);
        assertThat(publisher.hasPublished(event1.getClass())).isTrue();
    }

    @Test
    void mockEventPublisher_shouldFilterEventsByType() {
        // Given
        MockFactory.MockEventPublisher publisher = MockFactory.mockEventPublisher();
        DomainEvent event1 = DomainEventBuilder.anEvent().withEventType("Type1").build();
        DomainEvent event2 = DomainEventBuilder.anEvent().withEventType("Type2").build();

        // When
        publisher.publish(event1);
        publisher.publish(event2);

        // Then
        List<DomainEventBuilder.TestDomainEvent> testEvents = 
            publisher.getPublishedEvents(DomainEventBuilder.TestDomainEvent.class);
        assertThat(testEvents).hasSize(2);
    }

    @Test
    void mockEventPublisher_shouldNotifyListeners() {
        // Given
        MockFactory.MockEventPublisher publisher = MockFactory.mockEventPublisher();
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        AtomicReference<DomainEvent> receivedEvent = new AtomicReference<>();
        
        publisher.onEvent(DomainEventBuilder.TestDomainEvent.class, event -> {
            listenerCalled.set(true);
            receivedEvent.set(event);
        });

        DomainEvent event = DomainEventBuilder.anEvent().build();

        // When
        publisher.publish(event);

        // Then
        assertThat(listenerCalled.get()).isTrue();
        assertThat(receivedEvent.get()).isEqualTo(event);
    }

    @Test
    void mockRepository_shouldStoreAndRetrieveEntities() {
        // Given
        MockFactory.MockRepository<AggregateRootBuilder.TestAggregate<String>, String> repository = 
            MockFactory.mockRepository(AggregateRootBuilder.TestAggregate.class);
        
        AggregateRootBuilder.TestAggregate<String> aggregate = 
            AggregateRootBuilder.aStringAggregate().withId("123").build();

        // When
        repository.save(aggregate);

        // Then
        assertThat(repository.findById("123")).isPresent().contains(aggregate);
        assertThat(repository.existsById("123")).isTrue();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void mockRepository_shouldSimulateFailures() {
        // Given
        MockFactory.MockRepository<AggregateRootBuilder.TestAggregate<String>, String> repository = 
            MockFactory.mockRepository(AggregateRootBuilder.TestAggregate.class);
        
        AggregateRootBuilder.TestAggregate<String> aggregate = 
            AggregateRootBuilder.aStringAggregate().withId("123").build();

        // When
        repository.simulateSaveFailure();

        // Then
        assertThatThrownBy(() -> repository.save(aggregate))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Save operation failed");
    }

    @Test
    void mockTransactionManager_shouldExecuteTransactions() {
        // Given
        MockFactory.MockTransactionManager txManager = MockFactory.mockTransactionManager();
        AtomicBoolean executed = new AtomicBoolean(false);

        // When
        String result = txManager.executeInTransaction(() -> {
            executed.set(true);
            return "success";
        });

        // Then
        assertThat(result).isEqualTo("success");
        assertThat(executed.get()).isTrue();
        assertThat(txManager.getTransactionCount()).isEqualTo(1);
        assertThat(txManager.getExecutedTransactions()).contains("REGULAR");
    }

    @Test
    void mockTransactionManager_shouldSimulateRollback() {
        // Given
        MockFactory.MockTransactionManager txManager = MockFactory.mockTransactionManager();
        txManager.simulateRollback();

        // Then
        assertThatThrownBy(() -> txManager.executeInTransaction(() -> "test"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Transaction rolled back");
    }

    @Test
    void mockUseCase_shouldExecuteWithConfiguredBehavior() {
        // Given
        UseCase<String, String> useCase = MockFactory.mockUseCase(input -> input.toUpperCase());

        // When
        String result = useCase.execute("hello");

        // Then
        assertThat(result).isEqualTo("HELLO");
    }

    @Test
    void mockVoidUseCase_shouldExecuteWithConfiguredBehavior() {
        // Given
        AtomicReference<String> captured = new AtomicReference<>();
        VoidUseCase<String> useCase = MockFactory.mockVoidUseCase(captured::set);

        // When
        useCase.execute("test");

        // Then
        assertThat(captured.get()).isEqualTo("test");
    }

    @Test
    void mockEventStore_shouldStoreAndRetrieveEvents() {
        // Given
        MockFactory.MockEventStore eventStore = MockFactory.mockEventStore();
        DomainEvent event1 = DomainEventBuilder.anEvent()
            .withAggregateId("agg-1")
            .withVersion(1L)
            .build();
        DomainEvent event2 = DomainEventBuilder.anEvent()
            .withAggregateId("agg-1")
            .withVersion(2L)
            .build();

        // When
        eventStore.store(event1);
        eventStore.store(event2);

        // Then
        assertThat(eventStore.findByAggregateId("agg-1")).hasSize(2);
        assertThat(eventStore.findByAggregateIdAfterVersion("agg-1", 1L)).hasSize(1);
        assertThat(eventStore.hasEvents("agg-1")).isTrue();
        assertThat(eventStore.getEventCount()).isEqualTo(2);
    }

    @Test
    void mockEventStore_shouldHandleBatchStorage() {
        // Given
        MockFactory.MockEventStore eventStore = MockFactory.mockEventStore();
        List<DomainEvent> events = List.of(
            DomainEventBuilder.anEvent().withAggregateId("agg-1").build(),
            DomainEventBuilder.anEvent().withAggregateId("agg-2").build()
        );

        // When
        eventStore.storeAll(events);

        // Then
        assertThat(eventStore.findAll()).hasSize(2);
        assertThat(eventStore.findByAggregateId("agg-1")).hasSize(1);
        assertThat(eventStore.findByAggregateId("agg-2")).hasSize(1);
    }
}