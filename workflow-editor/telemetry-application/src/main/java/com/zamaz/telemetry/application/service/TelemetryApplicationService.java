package com.zamaz.telemetry.application.service;

import com.zamaz.telemetry.application.dto.TelemetryDataDto;
import com.zamaz.telemetry.application.dto.TelemetryAnalysisResponse;
import com.zamaz.telemetry.application.dto.TelemetryQueryRequest;
import com.zamaz.telemetry.application.port.TelemetryStreamPort;
import com.zamaz.telemetry.application.port.WorkflowNotificationPort;
import com.zamaz.telemetry.domain.entity.TelemetryAnalysis;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.event.EventPublisher;
import com.zamaz.telemetry.domain.event.TelemetryDataReceivedEvent;
import com.zamaz.telemetry.domain.query.TelemetryQuery;
import com.zamaz.telemetry.domain.repository.TelemetryRepository;
import com.zamaz.telemetry.domain.service.TelemetryDomainService;
import com.zamaz.telemetry.domain.service.TelemetryDomainServiceImpl;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryApplicationService {
    private final TelemetryDomainService telemetryDomainService;
    private final TelemetryRepository telemetryRepository;
    private final WorkflowNotificationPort workflowNotificationPort;
    private final TelemetryStreamPort telemetryStreamPort;
    private final EventPublisher eventPublisher;
    
    // Track active telemetry streams
    private final Map<String, Flux<TelemetryData>> activeStreams = new ConcurrentHashMap<>();
    
    /**
     * Process incoming telemetry data at 10Hz frequency
     */
    @Async
    @EventListener
    public void handleTelemetryData(TelemetryDataReceivedEvent event) {
        TelemetryData data = event.getTelemetryData();
        
        // Process single telemetry data point
        processTelemetryData(data);
    }
    
    /**
     * Start a telemetry stream for specific devices
     */
    public Flux<TelemetryDataDto> startTelemetryStream(List<String> deviceIds) {
        String streamKey = String.join(",", deviceIds);
        
        Flux<TelemetryData> stream = Flux.interval(Duration.ofMillis(100)) // 10Hz = 100ms interval
                .flatMap(tick -> {
                    // Get telemetry data from devices
                    return Flux.fromIterable(deviceIds)
                            .flatMap(deviceId -> telemetryStreamPort.getTelemetryData(deviceId))
                            .filter(data -> telemetryDomainService.validateTelemetryData(data));
                })
                .doOnNext(this::processTelemetryData)
                .share(); // Share the stream among multiple subscribers
        
        activeStreams.put(streamKey, stream);
        
        return stream.map(TelemetryDataDto::from);
    }
    
    /**
     * Stop a telemetry stream
     */
    public void stopTelemetryStream(List<String> deviceIds) {
        String streamKey = String.join(",", deviceIds);
        activeStreams.remove(streamKey);
    }
    
    /**
     * Process batch of telemetry data
     */
    public Mono<Void> processTelemetryBatch(List<TelemetryDataDto> batch) {
        return Flux.fromIterable(batch)
                .map(dto -> dto.toDomain())
                .buffer(100) // Process in chunks of 100
                .flatMap(chunk -> 
                    Mono.fromRunnable(() -> {
                        Stream<TelemetryData> stream = chunk.stream();
                        telemetryDomainService.processTelemetryStream(stream);
                    })
                    .subscribeOn(Schedulers.parallel())
                )
                .then();
    }
    
    /**
     * Analyze telemetry data based on query
     */
    public TelemetryAnalysisResponse analyzeTelemetry(TelemetryQueryRequest request) {
        TelemetryQuery query = buildTelemetryQuery(request);
        TelemetryAnalysis analysis = telemetryDomainService.analyzeTelemetry(query);
        return TelemetryAnalysisResponse.from(analysis);
    }
    
    /**
     * Get real-time telemetry statistics
     */
    public Map<String, Object> getRealtimeStatistics(List<String> deviceIds) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // Calculate data rate
        long dataPointsLastMinute = countDataPointsInLastMinute(deviceIds);
        double dataRate = dataPointsLastMinute / 60.0; // Points per second
        
        stats.put("dataRate", dataRate);
        stats.put("activeDevices", deviceIds.size());
        stats.put("dataPointsLastMinute", dataPointsLastMinute);
        stats.put("expectedDataRate", deviceIds.size() * 10); // 10Hz per device
        stats.put("dataQuality", calculateDataQuality(dataRate, deviceIds.size() * 10));
        
        return stats;
    }
    
    /**
     * Register telemetry threshold for workflow triggering
     */
    public void registerTelemetryThreshold(String thresholdId, String metricName, 
                                         double thresholdValue, String comparisonType, 
                                         String workflowId) {
        TelemetryDomainServiceImpl.ThresholdConfiguration config = 
            TelemetryDomainServiceImpl.ThresholdConfiguration.builder()
                .metricName(metricName)
                .thresholdValue(thresholdValue)
                .type(TelemetryDomainServiceImpl.ThresholdType.valueOf(comparisonType))
                .workflowId(workflowId)
                .build();
        
        if (telemetryDomainService instanceof TelemetryDomainServiceImpl) {
            ((TelemetryDomainServiceImpl) telemetryDomainService).registerThreshold(thresholdId, config);
        }
        
        log.info("Registered telemetry threshold: {} for workflow: {}", thresholdId, workflowId);
    }
    
    private void processTelemetryData(TelemetryData data) {
        try {
            // Enrich data
            TelemetryData enrichedData = telemetryDomainService.enrichTelemetryData(data);
            
            // Save to repositories
            telemetryRepository.saveTimeSeries(enrichedData);
            if (enrichedData.hasLocation()) {
                telemetryRepository.saveSpatialData(enrichedData);
            }
            
            // Check workflow conditions
            telemetryDomainService.triggerWorkflowConditions(enrichedData);
            
            // Notify interested parties
            workflowNotificationPort.notifyTelemetryReceived(enrichedData);
            
        } catch (Exception e) {
            log.error("Error processing telemetry data for device: {}", data.getDeviceId(), e);
        }
    }
    
    private TelemetryQuery buildTelemetryQuery(TelemetryQueryRequest request) {
        TelemetryQuery.TelemetryQueryBuilder builder = TelemetryQuery.builder()
                .startTime(request.getStartTime())
                .endTime(request.getEndTime());
        
        if (request.getDeviceIds() != null && !request.getDeviceIds().isEmpty()) {
            builder.deviceIds(request.getDeviceIds().stream()
                    .map(DeviceId::of)
                    .collect(Collectors.toList()));
        }
        
        if (request.getGeoQuery() != null) {
            TelemetryQueryRequest.GeoQueryDto geoDto = request.getGeoQuery();
            TelemetryQuery.GeoQuery.GeoQueryBuilder geoBuilder = TelemetryQuery.GeoQuery.builder();
            
            if (geoDto.getCenter() != null) {
                geoBuilder.center(geoDto.getCenter().toGeoLocation());
                geoBuilder.radiusKm(geoDto.getRadiusKm());
            }
            
            if (geoDto.getBoundingBox() != null) {
                geoBuilder.boundingBox(geoDto.getBoundingBox().toBoundingBox());
            }
            
            builder.geoQuery(geoBuilder.build());
        }
        
        if (request.getMetricNames() != null) {
            builder.metricNames(request.getMetricNames());
        }
        
        if (request.getLimit() != null) {
            builder.limit(request.getLimit());
        }
        
        return builder.build();
    }
    
    private long countDataPointsInLastMinute(List<String> deviceIds) {
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        return telemetryRepository.count(
            new TelemetryDomainServiceImpl.TimeRange(oneMinuteAgo, Instant.now())
        );
    }
    
    private double calculateDataQuality(double actualRate, double expectedRate) {
        if (expectedRate == 0) return 100.0;
        return Math.min(100.0, (actualRate / expectedRate) * 100);
    }
}