package com.zamaz.telemetry.domain.query;

import com.zamaz.telemetry.domain.valueobject.DeviceId;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class TelemetryQuery {
    private final List<DeviceId> deviceIds;
    private final Instant startTime;
    private final Instant endTime;
    private final GeoQuery geoQuery;
    private final List<String> metricNames;
    private final Integer limit;
    
    @Getter
    @Builder
    public static class GeoQuery {
        private final GeoLocation center;
        private final double radiusKm;
        private final BoundingBox boundingBox;
    }
    
    @Getter
    @Builder
    public static class BoundingBox {
        private final double north;
        private final double south;
        private final double east;
        private final double west;
        
        public boolean contains(GeoLocation location) {
            return location.getLatitude() >= south && 
                   location.getLatitude() <= north &&
                   location.getLongitude() >= west && 
                   location.getLongitude() <= east;
        }
    }
}