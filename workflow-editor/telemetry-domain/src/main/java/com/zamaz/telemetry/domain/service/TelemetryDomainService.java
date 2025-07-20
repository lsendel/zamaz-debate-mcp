package com.zamaz.telemetry.domain.service;

import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.entity.TelemetryAnalysis;
import com.zamaz.telemetry.domain.query.TelemetryQuery;

import java.util.stream.Stream;

public interface TelemetryDomainService {
    void processTelemetryStream(Stream<TelemetryData> dataStream);
    
    TelemetryAnalysis analyzeTelemetry(TelemetryQuery query);
    
    void triggerWorkflowConditions(TelemetryData data);
    
    boolean validateTelemetryData(TelemetryData data);
    
    TelemetryData enrichTelemetryData(TelemetryData data);
}