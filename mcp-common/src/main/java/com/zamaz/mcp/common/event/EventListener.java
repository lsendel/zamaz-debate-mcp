package com.zamaz.mcp.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event listener for domain events.
 * Methods annotated with @EventListener will be automatically invoked
 * when matching events are published through the event system.
 * 
 * Usage example:
 * <pre>
 * @EventListener(eventType = "DEBATE_CREATED")
 * public void handleDebateCreated(DomainEvent event) {
 *     // Handle the event
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    
    /**
     * The event type(s) to listen for.
     * If empty, the method will receive all events.
     */
    String[] eventType() default {};
    
    /**
     * The aggregate type(s) to filter by.
     * If specified, only events from these aggregate types will be received.
     */
    String[] aggregateType() default {};
    
    /**
     * Organization ID to filter events.
     * If specified, only events from this organization will be received.
     * Use "${organizationId}" to use the current context organization.
     */
    String organizationId() default "";
    
    /**
     * Whether to process events asynchronously.
     * If true, the event handler will be executed in a separate thread.
     */
    boolean async() default true;
    
    /**
     * The order in which this listener should be invoked.
     * Lower values have higher priority.
     */
    int order() default 0;
    
    /**
     * Whether to continue processing if this listener throws an exception.
     * If false, exception will stop the event processing chain.
     */
    boolean ignoreErrors() default false;
    
    /**
     * Condition expression that must evaluate to true for the listener to be invoked.
     * Supports Spring Expression Language (SpEL).
     * Example: "#{event.metadata['priority'] == 'HIGH'}"
     */
    String condition() default "";
}