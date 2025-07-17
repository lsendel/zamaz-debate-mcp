package com.zamaz.mcp.common.domain.model;

import com.zamaz.mcp.common.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots in Domain-Driven Design.
 * Aggregate roots are the entry points to aggregates and manage domain events.
 * This is a pure domain class with no framework dependencies.
 * 
 * @param <ID> The type of the aggregate root identifier
 */
public abstract class AggregateRoot<ID> extends DomainEntity<ID> {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot(ID id) {
        super(id);
    }
    
    /**
     * Registers a domain event to be raised.
     * Events are collected and can be dispatched after the aggregate is persisted.
     * 
     * @param event the domain event to register
     */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    /**
     * Gets all domain events that have been registered.
     * 
     * @return an unmodifiable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    /**
     * Clears all domain events.
     * Should be called after events have been dispatched.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    /**
     * Validates the aggregate's invariants.
     * Subclasses should override to enforce business rules.
     * 
     * @throws IllegalStateException if any invariant is violated
     */
    public abstract void validateInvariants();
}