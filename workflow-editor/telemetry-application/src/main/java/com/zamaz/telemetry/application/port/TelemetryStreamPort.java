package com.zamaz.telemetry.application.port;

import com.zamaz.telemetry.domain.entity.TelemetryData;
import reactor.core.publisher.Mono;

public interface TelemetryStreamPort {
    Mono<TelemetryData> getTelemetryData(String deviceId);
}