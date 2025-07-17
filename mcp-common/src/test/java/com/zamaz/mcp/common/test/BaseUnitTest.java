package com.zamaz.mcp.common.test;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.common.domain.model.AggregateRoot;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for unit tests providing common testing utilities.
 */
public abstract class BaseUnitTest {
    
    /**
     * Asserts that an aggregate has raised a specific event.
     * 
     * @param aggregate the aggregate to check
     * @param eventClass the expected event class
     */
    protected void assertEventRaised(AggregateRoot<?> aggregate, Class<? extends DomainEvent> eventClass) {
        List<DomainEvent> events = aggregate.getUncommittedEvents();
        assertTrue(
            events.stream().anyMatch(eventClass::isInstance),
            "Expected event " + eventClass.getSimpleName() + " was not raised"
        );
    }
    
    /**
     * Asserts that an aggregate has raised a specific number of events.
     * 
     * @param aggregate the aggregate to check
     * @param expectedCount the expected number of events
     */
    protected void assertEventCount(AggregateRoot<?> aggregate, int expectedCount) {
        List<DomainEvent> events = aggregate.getUncommittedEvents();
        assertEquals(
            expectedCount,
            events.size(),
            "Expected " + expectedCount + " events but found " + events.size()
        );
    }
    
    /**
     * Gets the first event of a specific type from an aggregate.
     * 
     * @param aggregate the aggregate to check
     * @param eventClass the event class to find
     * @return the event
     */
    @SuppressWarnings("unchecked")
    protected <T extends DomainEvent> T getEvent(AggregateRoot<?> aggregate, Class<T> eventClass) {
        return (T) aggregate.getUncommittedEvents().stream()
            .filter(eventClass::isInstance)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Event " + eventClass.getSimpleName() + " not found"));
    }
    
    /**
     * Asserts that a code block throws a specific exception.
     * 
     * @param expectedType the expected exception type
     * @param executable the code to execute
     * @return the thrown exception for further assertions
     */
    protected <T extends Throwable> T assertThrowsWithMessage(
            Class<T> expectedType,
            Executable executable,
            String expectedMessagePart) {
        
        T exception = assertThrows(expectedType, executable);
        assertTrue(
            exception.getMessage().contains(expectedMessagePart),
            "Expected message to contain '" + expectedMessagePart + "' but was: " + exception.getMessage()
        );
        return exception;
    }
    
    /**
     * Functional interface for executable code blocks.
     */
    @FunctionalInterface
    protected interface Executable {
        void execute() throws Throwable;
    }
}