package com.zamaz.mcp.debateengine.adapter.event;

import com.zamaz.mcp.common.domain.DomainEvent;
import com.zamaz.mcp.debateengine.domain.port.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Spring implementation of domain event publisher.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
    }
    
    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
    
    @Override
    public void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}