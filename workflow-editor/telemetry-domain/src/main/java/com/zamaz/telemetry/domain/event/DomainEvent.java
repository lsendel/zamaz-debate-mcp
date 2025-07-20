package com.zamaz.telemetry.domain.event;

import java.time.Instant;

public interface DomainEvent {
    String getEventType();
    Instant getOccurredAt();
}