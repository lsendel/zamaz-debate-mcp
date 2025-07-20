package com.zamaz.workflow.domain.event;

import java.time.Instant;

public interface DomainEvent {
    String getEventType();
    Instant getOccurredAt();
}