package com.zamaz.mcp.common.testing.assertions;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.model.ValueObject;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Custom assertions for domain objects.
 */
public class DomainAssertions {

    /**
     * Entry point for AggregateRoot assertions.
     */
    public static <T extends AggregateRoot<?>> AggregateRootAssert<T> assertThat(T actual) {
        return new AggregateRootAssert<>(actual);
    }

    /**
     * Entry point for DomainEvent assertions.
     */
    public static DomainEventAssert assertThat(DomainEvent actual) {
        return new DomainEventAssert(actual);
    }

    /**
     * Entry point for ValueObject assertions.
     */
    public static <T extends ValueObject> ValueObjectAssert<T> assertThat(T actual) {
        return new ValueObjectAssert<>(actual);
    }

    /**
     * Assertions for AggregateRoot objects.
     */
    public static class AggregateRootAssert<T extends AggregateRoot<?>> 
            extends AbstractAssert<AggregateRootAssert<T>, T> {

        public AggregateRootAssert(T actual) {
            super(actual, AggregateRootAssert.class);
        }

        public AggregateRootAssert<T> hasId(Object expectedId) {
            isNotNull();
            Assertions.assertThat(actual.getId()).isEqualTo(expectedId);
            return this;
        }

        public AggregateRootAssert<T> hasVersion(long expectedVersion) {
            isNotNull();
            Assertions.assertThat(actual.getVersion()).isEqualTo(expectedVersion);
            return this;
        }

        public AggregateRootAssert<T> hasRaisedEvent(Class<? extends DomainEvent> eventType) {
            isNotNull();
            boolean hasEvent = actual.getDomainEvents().stream()
                .anyMatch(eventType::isInstance);
            if (!hasEvent) {
                failWithMessage("Expected aggregate to have raised event of type <%s> but it didn't", 
                    eventType.getSimpleName());
            }
            return this;
        }

        public AggregateRootAssert<T> hasRaisedEvents(int count) {
            isNotNull();
            Assertions.assertThat(actual.getDomainEvents()).hasSize(count);
            return this;
        }

        public AggregateRootAssert<T> hasRaisedNoEvents() {
            isNotNull();
            Assertions.assertThat(actual.getDomainEvents()).isEmpty();
            return this;
        }

        public AggregateRootAssert<T> hasRaisedEventMatching(Predicate<DomainEvent> predicate) {
            isNotNull();
            boolean hasMatchingEvent = actual.getDomainEvents().stream()
                .anyMatch(predicate);
            if (!hasMatchingEvent) {
                failWithMessage("Expected aggregate to have raised event matching predicate but none found");
            }
            return this;
        }

        public AggregateRootAssert<T> satisfies(Consumer<T> requirements) {
            isNotNull();
            requirements.accept(actual);
            return this;
        }

        public DomainEventAssert lastRaisedEvent() {
            isNotNull();
            List<DomainEvent> events = actual.getDomainEvents();
            if (events.isEmpty()) {
                failWithMessage("Expected aggregate to have raised events but found none");
            }
            return new DomainEventAssert(events.get(events.size() - 1));
        }
    }

    /**
     * Assertions for DomainEvent objects.
     */
    public static class DomainEventAssert extends AbstractAssert<DomainEventAssert, DomainEvent> {

        public DomainEventAssert(DomainEvent actual) {
            super(actual, DomainEventAssert.class);
        }

        public DomainEventAssert hasAggregateId(String expectedId) {
            isNotNull();
            Assertions.assertThat(actual.getAggregateId()).isEqualTo(expectedId);
            return this;
        }

        public DomainEventAssert hasEventType(String expectedType) {
            isNotNull();
            Assertions.assertThat(actual.getEventType()).isEqualTo(expectedType);
            return this;
        }

        public DomainEventAssert hasVersion(long expectedVersion) {
            isNotNull();
            Assertions.assertThat(actual.getVersion()).isEqualTo(expectedVersion);
            return this;
        }

        public DomainEventAssert occurredAfter(DomainEvent other) {
            isNotNull();
            Assertions.assertThat(actual.getOccurredAt()).isAfter(other.getOccurredAt());
            return this;
        }

        public DomainEventAssert occurredBefore(DomainEvent other) {
            isNotNull();
            Assertions.assertThat(actual.getOccurredAt()).isBefore(other.getOccurredAt());
            return this;
        }

        public DomainEventAssert hasPayload() {
            isNotNull();
            if (actual instanceof PayloadCarrier) {
                Object payload = ((PayloadCarrier) actual).getPayload();
                Assertions.assertThat(payload).isNotNull();
            }
            return this;
        }

        public <P> DomainEventAssert hasPayloadMatching(Class<P> payloadType, Predicate<P> predicate) {
            isNotNull();
            if (actual instanceof PayloadCarrier) {
                Object payload = ((PayloadCarrier) actual).getPayload();
                if (!payloadType.isInstance(payload)) {
                    failWithMessage("Expected event payload of type <%s> but was <%s>",
                        payloadType.getSimpleName(),
                        payload != null ? payload.getClass().getSimpleName() : "null");
                }
                P typedPayload = payloadType.cast(payload);
                if (!predicate.test(typedPayload)) {
                    failWithMessage("Event payload did not match the expected predicate");
                }
            }
            return this;
        }
    }

    /**
     * Assertions for ValueObject objects.
     */
    public static class ValueObjectAssert<T extends ValueObject> 
            extends AbstractAssert<ValueObjectAssert<T>, T> {

        public ValueObjectAssert(T actual) {
            super(actual, ValueObjectAssert.class);
        }

        public ValueObjectAssert<T> isEqualByValueTo(T other) {
            isNotNull();
            Assertions.assertThat(actual).isEqualTo(other);
            return this;
        }

        public ValueObjectAssert<T> isNotEqualByValueTo(T other) {
            isNotNull();
            Assertions.assertThat(actual).isNotEqualTo(other);
            return this;
        }

        public ValueObjectAssert<T> hasSameHashCodeAs(T other) {
            isNotNull();
            Assertions.assertThat(actual.hashCode()).isEqualTo(other.hashCode());
            return this;
        }

        public ValueObjectAssert<T> satisfies(Consumer<T> requirements) {
            isNotNull();
            requirements.accept(actual);
            return this;
        }
    }

    /**
     * Interface for events that carry payload.
     * This should be implemented by domain events that have payload.
     */
    public interface PayloadCarrier {
        Object getPayload();
    }

    /**
     * Utility methods for common domain assertions.
     */
    public static class DomainAssertionUtils {

        /**
         * Asserts that a domain operation throws a specific domain exception.
         */
        public static void assertDomainException(Runnable operation, 
                                               Class<? extends Exception> exceptionType,
                                               String expectedMessage) {
            Assertions.assertThatThrownBy(operation::run)
                .isInstanceOf(exceptionType)
                .hasMessageContaining(expectedMessage);
        }

        /**
         * Asserts that events were raised in a specific order.
         */
        public static void assertEventsInOrder(List<DomainEvent> events, 
                                             Class<? extends DomainEvent>... expectedTypes) {
            Assertions.assertThat(events).hasSize(expectedTypes.length);
            for (int i = 0; i < expectedTypes.length; i++) {
                Assertions.assertThat(events.get(i)).isInstanceOf(expectedTypes[i]);
            }
        }

        /**
         * Asserts that an aggregate has transitioned to a specific state.
         */
        public static <T extends AggregateRoot<?>> void assertStateTransition(
                T aggregate,
                Consumer<T> stateAssertion) {
            Assertions.assertThat(aggregate).isNotNull();
            stateAssertion.accept(aggregate);
        }
    }
}