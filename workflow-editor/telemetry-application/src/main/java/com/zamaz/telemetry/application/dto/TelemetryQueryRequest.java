package com.zamaz.telemetry.application.dto;

import com.zamaz.telemetry.domain.query.TelemetryQuery;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TelemetryQueryRequest {
    private List<String> deviceIds;
    
    @NotNull
    private Instant startTime;
    
    @NotNull
    private Instant endTime;
    
    private GeoQueryDto geoQuery;
    private List<String> metricNames;
    private Integer limit;
    
    @Data
    @Builder
    public static class GeoQueryDto {
        private GeoLocationDto center;
        private Double radiusKm;
        private BoundingBoxDto boundingBox;
        
        @Data
        @Builder
        public static class GeoLocationDto {
            private double latitude;
            private double longitude;
            
            public GeoLocation toGeoLocation() {
                return GeoLocation.of(latitude, longitude);
            }
        }
        
        @Data
        @Builder
        public static class BoundingBoxDto {
            private double north;
            private double south;
            private double east;
            private double west;
            
            public TelemetryQuery.BoundingBox toBoundingBox() {
                return TelemetryQuery.BoundingBox.builder()
                        .north(north)
                        .south(south)
                        .east(east)
                        .west(west)
                        .build();
            }
        }
    }
}