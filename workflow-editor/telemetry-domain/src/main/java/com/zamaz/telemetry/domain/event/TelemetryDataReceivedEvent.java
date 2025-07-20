package com.zamaz.telemetry.domain.event;

import com.zamaz.telemetry.domain.entity.TelemetryData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class TelemetryDataReceivedEvent implements DomainEvent {
    private final TelemetryData telemetryData;
    private final Instant occurredAt = Instant.now();
    
    @Override
    public String getEventType() {
        return "telemetry.data.received";
    }
}