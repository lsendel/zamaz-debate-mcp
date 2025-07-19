package com.example.workflow.application;

import com.example.workflow.domain.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Random;

@Service
public class GeospatialSampleApplicationService {
    
    private final Random random = new Random();
    
    public List<StamfordAddress> generateStamfordAddresses() {
        return List.of(
            new StamfordAddress("123 Main St", new GeoLocation(41.0534, -73.5387)),
            new StamfordAddress("456 Oak Ave", new GeoLocation(41.0544, -73.5397)),
            new StamfordAddress("789 Pine Rd", new GeoLocation(41.0524, -73.5377))
        );
    }
    
    public TelemetryData generateSensorData(StamfordAddress address) {
        TelemetryData data = new TelemetryData(
            TelemetryId.generate(),
            DeviceId.of("sensor-" + address.hashCode()),
            "stamford-org"
        );
        
        data.addMetric("temperature", MetricValue.of(20 + random.nextDouble() * 15));
        data.addMetric("humidity", MetricValue.of(40 + random.nextDouble() * 40));
        data.setLocation(address.location());
        
        return data;
    }
}

record StamfordAddress(String address, GeoLocation location) {}