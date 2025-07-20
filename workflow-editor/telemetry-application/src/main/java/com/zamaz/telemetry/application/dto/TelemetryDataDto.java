package com.zamaz.telemetry.application.dto;

import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import com.zamaz.telemetry.domain.valueobject.TelemetryId;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class TelemetryDataDto {
    private String id;
    private String deviceId;
    private Instant timestamp;
    private Map<String, Object> metrics;
    private GeoLocationDto location;
    private String dataSource;
    private int qualityScore;
    
    public static TelemetryDataDto from(TelemetryData data) {
        TelemetryDataDto.TelemetryDataDtoBuilder builder = TelemetryDataDto.builder()
                .id(data.getId().getValue())
                .deviceId(data.getDeviceId().getValue())
                .timestamp(data.getTimestamp())
                .metrics(data.getMetrics())
                .dataSource(data.getDataSource())
                .qualityScore(data.getQualityScore());
        
        if (data.hasLocation()) {
            builder.location(GeoLocationDto.from(data.getLocation()));
        }
        
        return builder.build();
    }
    
    public TelemetryData toDomain() {
        TelemetryData.TelemetryDataBuilder builder = TelemetryData.builder()
                .id(TelemetryId.of(id))
                .deviceId(DeviceId.of(deviceId))
                .timestamp(timestamp)
                .metrics(metrics)
                .dataSource(dataSource)
                .qualityScore(qualityScore);
        
        if (location != null) {
            builder.location(location.toGeoLocation());
        }
        
        return builder.build();
    }
    
    @Data
    @Builder
    public static class GeoLocationDto {
        private double latitude;
        private double longitude;
        private Double altitude;
        
        public static GeoLocationDto from(GeoLocation location) {
            return GeoLocationDto.builder()
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .altitude(location.getAltitude())
                    .build();
        }
        
        public GeoLocation toGeoLocation() {
            if (altitude != null) {
                return GeoLocation.of(latitude, longitude, altitude);
            } else {
                return GeoLocation.of(latitude, longitude);
            }
        }
    }
}