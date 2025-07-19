package com.example.workflow.application;

import com.example.workflow.domain.*;
import com.example.workflow.domain.ports.*;
import com.example.workflow.domain.services.*;
import org.springframework.stereotype.Service;

@Service
public class TelemetryApplicationService {
    
    private final TelemetryDomainService telemetryDomainService;
    private final TelemetryRepository telemetryRepository;
    
    public TelemetryApplicationService(TelemetryDomainService telemetryDomainService,
                                    TelemetryRepository telemetryRepository) {
        this.telemetryDomainService = telemetryDomainService;
        this.telemetryRepository = telemetryRepository;
    }
    
    public void ingestTelemetryData(TelemetryData data) {
        telemetryRepository.saveTimeSeries(data);
        telemetryDomainService.triggerWorkflowConditions(data);
    }
}