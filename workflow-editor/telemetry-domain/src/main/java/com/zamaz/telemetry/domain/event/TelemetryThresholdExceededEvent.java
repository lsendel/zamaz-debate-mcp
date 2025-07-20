package com.zamaz.telemetry.domain.event;

import com.zamaz.telemetry.domain.valueobject.TelemetryId;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TelemetryThresholdExceededEvent {
    private final TelemetryId telemetryId;
    private final DeviceId deviceId;
    private final String metricName;
    private final double thresholdValue;
    private final double actualValue;
    private final String workflowId;
    @Builder.Default
    private final Instant occurredAt = Instant.now();
}