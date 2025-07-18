package com.zamaz.mcp.common.testing;

import com.zamaz.mcp.common.application.port.inbound.UseCase;
import com.zamaz.mcp.common.application.port.inbound.VoidUseCase;
import com.zamaz.mcp.common.application.port.outbound.Repository;
import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.common.domain.event.DomainEventPublisher;
import com.zamaz.mcp.common.domain.event.DomainEventStore;
import com.zamaz.mcp.common.domain.model.AggregateRoot;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Factory for creating pre-configured mock objects for common interfaces.
 * Provides both simple mocks and behavior-configured mocks for testing.
 */
public class MockFactory {

    /**
     * Creates a mock DomainEventPublisher that captures published events.
     */
    public static MockEventPublisher mockEventPublisher() {
        return new MockEventPublisher();
    }

    /**
     * Creates a mock Repository with in-memory storage behavior.
     */
    public static <T extends AggregateRoot<ID>, ID> MockRepository<T, ID> mockRepository(Class<T> entityClass) {
        return new MockRepository<>(entityClass);
    }

    /**
     * Creates a mock TransactionManager with configurable behavior.
     */
    public static MockTransactionManager mockTransactionManager() {
        return new MockTransactionManager();
    }

    /**
     * Creates a mock UseCase with configurable response.
     */
    public static <I, O> UseCase<I, O> mockUseCase(Function<I, O> executor) {
        UseCase<I, O> mock = mock(UseCase.class);
        when(mock.execute(any())).thenAnswer(invocation -> executor.apply(invocation.getArgument(0)));
        return mock;
    }

    /**
     * Creates a mock VoidUseCase with configurable behavior.
     */
    public static <I> VoidUseCase<I> mockVoidUseCase(java.util.function.Consumer<I> executor) {
        VoidUseCase<I> mock = mock(VoidUseCase.class);
        doAnswer(invocation -> {
            executor.accept(invocation.getArgument(0));
            return null;
        }).when(mock).execute(any());
        return mock;
    }

    /**
     * Creates a mock DomainEventStore with in-memory storage.
     */
    public static MockEventStore mockEventStore() {
        return new MockEventStore();
    }

    /**
     * Mock implementation of DomainEventPublisher that captures events.
     */
    public static class MockEventPublisher implements DomainEventPublisher {
        private final List<DomainEvent> publishedEvents = new CopyOnWriteArrayList<>();
        private final Map<Class<?>, List<java.util.function.Consumer<DomainEvent>>> listeners = new ConcurrentHashMap<>();

        @Override
        public void publish(DomainEvent event) {
            publishedEvents.add(event);
            notifyListeners(event);
        }

        @Override
        public void publishAll(List<DomainEvent> events) {
            publishedEvents.addAll(events);
            events.forEach(this::notifyListeners);
        }

        private void notifyListeners(DomainEvent event) {
            listeners.getOrDefault(event.getClass(), Collections.emptyList())
                    .forEach(listener -> listener.accept(event));
        }

        public List<DomainEvent> getPublishedEvents() {
            return new ArrayList<>(publishedEvents);
        }

        public <T extends DomainEvent> List<T> getPublishedEvents(Class<T> eventType) {
            return publishedEvents.stream()
                    .filter(eventType::isInstance)
                    .map(eventType::cast)
                    .toList();
        }

        public void clear() {
            publishedEvents.clear();
        }

        public <T extends DomainEvent> void onEvent(Class<T> eventType, java.util.function.Consumer<T> listener) {
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                    .add(event -> listener.accept(eventType.cast(event)));
        }

        public boolean hasPublished(Class<? extends DomainEvent> eventType) {
            return publishedEvents.stream().anyMatch(eventType::isInstance);
        }

        public int getEventCount() {
            return publishedEvents.size();
        }

        public int getEventCount(Class<? extends DomainEvent> eventType) {
            return (int) publishedEvents.stream().filter(eventType::isInstance).count();
        }
    }

    /**
     * Mock implementation of Repository with in-memory storage.
     */
    public static class MockRepository<T extends AggregateRoot<ID>, ID> implements Repository<T, ID> {
        private final Map<ID, T> storage = new ConcurrentHashMap<>();
        private final Class<T> entityClass;
        private boolean throwOnSave = false;
        private boolean throwOnFind = false;

        public MockRepository(Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public T save(T entity) {
            if (throwOnSave) {
                throw new RuntimeException("Save operation failed");
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<T> findById(ID id) {
            if (throwOnFind) {
                throw new RuntimeException("Find operation failed");
            }
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public boolean existsById(ID id) {
            return storage.containsKey(id);
        }

        @Override
        public void deleteById(ID id) {
            storage.remove(id);
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }

        public void simulateSaveFailure() {
            this.throwOnSave = true;
        }

        public void simulateFindFailure() {
            this.throwOnFind = true;
        }

        public void reset() {
            storage.clear();
            throwOnSave = false;
            throwOnFind = false;
        }

        public int count() {
            return storage.size();
        }

        public boolean contains(ID id) {
            return storage.containsKey(id);
        }
    }

    /**
     * Mock implementation of TransactionManager with configurable behavior.
     */
    public static class MockTransactionManager implements TransactionManager {
        private boolean simulateRollback = false;
        private int transactionCount = 0;
        private final List<String> executedTransactions = new ArrayList<>();

        @Override
        public <T> T executeInTransaction(Supplier<T> action) {
            transactionCount++;
            executedTransactions.add("REGULAR");
            if (simulateRollback) {
                throw new RuntimeException("Transaction rolled back");
            }
            return action.get();
        }

        @Override
        public void executeInTransaction(Runnable action) {
            transactionCount++;
            executedTransactions.add("REGULAR_VOID");
            if (simulateRollback) {
                throw new RuntimeException("Transaction rolled back");
            }
            action.run();
        }

        @Override
        public <T> T executeInNewTransaction(Supplier<T> action) {
            transactionCount++;
            executedTransactions.add("NEW");
            if (simulateRollback) {
                throw new RuntimeException("Transaction rolled back");
            }
            return action.get();
        }

        @Override
        public <T> T executeInReadOnlyTransaction(Supplier<T> action) {
            transactionCount++;
            executedTransactions.add("READONLY");
            return action.get();
        }

        public void simulateRollback() {
            this.simulateRollback = true;
        }

        public void reset() {
            this.simulateRollback = false;
            this.transactionCount = 0;
            this.executedTransactions.clear();
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public List<String> getExecutedTransactions() {
            return new ArrayList<>(executedTransactions);
        }
    }

    /**
     * Mock implementation of DomainEventStore with in-memory storage.
     */
    public static class MockEventStore implements DomainEventStore {
        private final List<DomainEvent> events = new CopyOnWriteArrayList<>();
        private final Map<String, List<DomainEvent>> eventsByAggregate = new ConcurrentHashMap<>();

        @Override
        public void store(DomainEvent event) {
            events.add(event);
            eventsByAggregate.computeIfAbsent(event.getAggregateId(), k -> new CopyOnWriteArrayList<>())
                    .add(event);
        }

        @Override
        public void storeAll(List<DomainEvent> events) {
            events.forEach(this::store);
        }

        @Override
        public List<DomainEvent> findByAggregateId(String aggregateId) {
            return new ArrayList<>(eventsByAggregate.getOrDefault(aggregateId, Collections.emptyList()));
        }

        @Override
        public List<DomainEvent> findByAggregateIdAfterVersion(String aggregateId, long version) {
            return eventsByAggregate.getOrDefault(aggregateId, Collections.emptyList()).stream()
                    .filter(event -> event.getVersion() > version)
                    .toList();
        }

        @Override
        public List<DomainEvent> findAll() {
            return new ArrayList<>(events);
        }

        public void clear() {
            events.clear();
            eventsByAggregate.clear();
        }

        public int getEventCount() {
            return events.size();
        }

        public boolean hasEvents(String aggregateId) {
            return eventsByAggregate.containsKey(aggregateId) && 
                   !eventsByAggregate.get(aggregateId).isEmpty();
        }
    }
}