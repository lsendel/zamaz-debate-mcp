package com.zamaz.workflow.api.graphql;

import com.zamaz.telemetry.application.dto.TelemetryAnalysisResponse;
import com.zamaz.telemetry.application.dto.TelemetryDataDto;
import com.zamaz.telemetry.application.dto.TelemetryQueryRequest;
import com.zamaz.telemetry.application.service.TelemetryApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TelemetryGraphQLController {
    private final TelemetryApplicationService telemetryService;
    
    @QueryMapping
    @PreAuthorize("hasRole('TELEMETRY_USER')")
    public TelemetryAnalysisResponse telemetryData(@Argument TelemetryQuery query) {
        log.debug("Querying telemetry data with: {}", query);
        
        TelemetryQueryRequest request = mapToQueryRequest(query);
        return telemetryService.analyzeTelemetry(request);
    }
    
    @SubscriptionMapping
    @PreAuthorize("hasRole('TELEMETRY_USER')")
    public Flux<TelemetryDataDto> telemetryStream(@Argument List<String> deviceIds) {
        log.info("Starting telemetry stream for devices: {}", deviceIds);
        
        return telemetryService.startTelemetryStream(deviceIds)
                .doOnSubscribe(sub -> log.debug("Client subscribed to telemetry stream"))
                .doOnCancel(() -> {
                    log.debug("Client unsubscribed from telemetry stream");
                    telemetryService.stopTelemetryStream(deviceIds);
                })
                .doOnError(error -> log.error("Error in telemetry stream", error));
    }
    
    @SubscriptionMapping
    @PreAuthorize("hasRole('TELEMETRY_USER')")
    public Flux<NodeStatusEvent> nodeStatusChanged(@Argument String workflowId) {
        log.debug("Subscribing to node status changes for workflow: {}", workflowId);
        
        // This would connect to the node status event stream
        return Flux.interval(Duration.ofSeconds(2))
                .map(tick -> NodeStatusEvent.builder()
                        .nodeId("node-" + tick)
                        .status(tick % 3 == 0 ? "SUCCESS" : "RUNNING")
                        .timestamp(java.time.Instant.now().toString())
                        .build())
                .take(20); // Limit for demo
    }
    
    private TelemetryQueryRequest mapToQueryRequest(TelemetryQuery query) {
        TelemetryQueryRequest.TelemetryQueryRequestBuilder builder = TelemetryQueryRequest.builder()
                .startTime(java.time.Instant.parse(query.getStartTime()))
                .endTime(java.time.Instant.parse(query.getEndTime()));
        
        if (query.getDeviceIds() != null) {
            builder.deviceIds(query.getDeviceIds());
        }
        
        if (query.getLocation() != null && query.getLocation().getCenter() != null) {
            TelemetryQueryRequest.GeoQueryDto geoQuery = TelemetryQueryRequest.GeoQueryDto.builder()
                    .center(TelemetryQueryRequest.GeoQueryDto.GeoLocationDto.builder()
                            .latitude(query.getLocation().getCenter().getLatitude())
                            .longitude(query.getLocation().getCenter().getLongitude())
                            .build())
                    .radiusKm(query.getLocation().getRadiusKm())
                    .build();
            builder.geoQuery(geoQuery);
        }
        
        return builder.build();
    }
    
    @lombok.Data
    public static class TelemetryQuery {
        private List<String> deviceIds;
        private String startTime;
        private String endTime;
        private GeoQueryInput location;
    }
    
    @lombok.Data
    public static class GeoQueryInput {
        private GeoLocationInput center;
        private Double radiusKm;
    }
    
    @lombok.Data
    public static class GeoLocationInput {
        private double latitude;
        private double longitude;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class NodeStatusEvent {
        private String nodeId;
        private String status;
        private String timestamp;
    }
}