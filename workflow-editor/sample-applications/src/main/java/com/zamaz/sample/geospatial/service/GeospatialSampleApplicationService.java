package com.zamaz.sample.geospatial.service;

import com.zamaz.geospatial.domain.entity.StamfordAddress;
import com.zamaz.geospatial.domain.service.GeospatialSampleDomainService;
import com.zamaz.sample.geospatial.dto.GeospatialSampleResponse;
import com.zamaz.sample.geospatial.dto.StamfordAddressDto;
import com.zamaz.telemetry.application.service.TelemetryApplicationService;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.workflow.application.service.WorkflowApplicationService;
import com.zamaz.workflow.application.dto.CreateWorkflowRequest;
import com.zamaz.workflow.application.dto.WorkflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeospatialSampleApplicationService {
    private final GeospatialSampleDomainService geospatialDomainService;
    private final TelemetryApplicationService telemetryApplicationService;
    private final WorkflowApplicationService workflowApplicationService;
    
    private List<StamfordAddress> addresses;
    private final AtomicBoolean telemetryStreamActive = new AtomicBoolean(false);
    private final Map<String, TelemetryData> latestTelemetryData = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing Geospatial Sample Application");
        addresses = geospatialDomainService.generateRandomAddresses();
        createSampleWorkflow();
    }
    
    public GeospatialSampleResponse initializeSample() {
        if (addresses == null || addresses.isEmpty()) {
            addresses = geospatialDomainService.generateRandomAddresses();
        }
        
        // Start telemetry stream if not already active
        if (!telemetryStreamActive.get()) {
            startTelemetryGeneration();
        }
        
        Map<String, Object> statistics = geospatialDomainService.generateAddressStatistics(addresses);
        
        return GeospatialSampleResponse.builder()
                .addresses(addresses.stream()
                        .map(StamfordAddressDto::from)
                        .collect(Collectors.toList()))
                .statistics(statistics)
                .telemetryActive(telemetryStreamActive.get())
                .build();
    }
    
    public void startTelemetryGeneration() {
        telemetryStreamActive.set(true);
        log.info("Started telemetry generation for {} addresses", addresses.size());
        
        // Register telemetry thresholds
        registerTemperatureThreshold();
        registerAirQualityThreshold();
    }
    
    public void stopTelemetryGeneration() {
        telemetryStreamActive.set(false);
        log.info("Stopped telemetry generation");
    }
    
    @Scheduled(fixedRate = 100) // 10Hz = 100ms
    public void generateTelemetryData() {
        if (!telemetryStreamActive.get() || addresses == null) {
            return;
        }
        
        Stream<TelemetryData> telemetryStream = geospatialDomainService.simulateSensorData(addresses);
        
        telemetryStream.forEach(data -> {
            // Store latest data
            latestTelemetryData.put(data.getDeviceId().getValue(), data);
            
            // Process through telemetry application service
            telemetryApplicationService.handleTelemetryData(
                new com.zamaz.telemetry.domain.event.TelemetryDataReceivedEvent(data)
            );
        });
    }
    
    public Map<String, TelemetryData> getLatestTelemetryData() {
        return new ConcurrentHashMap<>(latestTelemetryData);
    }
    
    public List<StamfordAddressDto> getAddressesWithTelemetry() {
        return addresses.stream()
                .map(address -> {
                    StamfordAddressDto dto = StamfordAddressDto.from(address);
                    String deviceId = "sensor-" + address.getId();
                    TelemetryData telemetry = latestTelemetryData.get(deviceId);
                    if (telemetry != null) {
                        dto.setLatestTelemetry(telemetry.getMetrics());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    private void createSampleWorkflow() {
        try {
            // Create a sample workflow for temperature monitoring
            CreateWorkflowRequest request = CreateWorkflowRequest.builder()
                    .name("Stamford Temperature Monitoring")
                    .description("Monitor temperature sensors across Stamford addresses")
                    .organizationId("sample-org")
                    .createdBy("geospatial-sample")
                    .build();
            
            WorkflowResponse workflow = workflowApplicationService.createWorkflow(request);
            log.info("Created sample workflow: {}", workflow.getId());
            
        } catch (Exception e) {
            log.error("Failed to create sample workflow", e);
        }
    }
    
    private void registerTemperatureThreshold() {
        telemetryApplicationService.registerTelemetryThreshold(
                "stamford-temp-high",
                "temperature_celsius",
                30.0,
                "GREATER_THAN",
                "temp-alert-workflow"
        );
        
        telemetryApplicationService.registerTelemetryThreshold(
                "stamford-temp-low",
                "temperature_celsius",
                5.0,
                "LESS_THAN",
                "temp-alert-workflow"
        );
    }
    
    private void registerAirQualityThreshold() {
        telemetryApplicationService.registerTelemetryThreshold(
                "stamford-air-quality",
                "air_quality_index",
                100.0,
                "GREATER_THAN",
                "air-quality-workflow"
        );
    }
}