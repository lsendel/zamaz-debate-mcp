package com.zamaz.geospatial.domain.service;

import com.zamaz.geospatial.domain.entity.StamfordAddress;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GeospatialSampleDomainService {
    private static final double STAMFORD_CENTER_LAT = 41.0534;
    private static final double STAMFORD_CENTER_LON = -73.5387;
    private static final double LAT_RANGE = 0.05; // Approximately 5.5km
    private static final double LON_RANGE = 0.05;
    
    private final Random random = new Random();
    private final Map<String, StamfordAddress> addressCache = new ConcurrentHashMap<>();
    
    public List<StamfordAddress> generateRandomAddresses() {
        List<StamfordAddress> addresses = new ArrayList<>();
        
        // Generate 10 random addresses within Stamford boundaries
        String[] streets = {
            "Main St", "Atlantic St", "Washington Blvd", "Bedford St", 
            "Summer St", "Broad St", "Tresser Blvd", "Grove St", 
            "Franklin St", "Canal St"
        };
        
        String[] neighborhoods = {
            "Downtown", "North Stamford", "Springdale", "Glenbrook",
            "Waterside", "Shippan", "Cove", "Westover"
        };
        
        String[] zipCodes = {"06901", "06902", "06903", "06904", "06905", "06906", "06907"};
        
        for (int i = 0; i < 10; i++) {
            int streetNumber = 100 + random.nextInt(900);
            String street = streets[i % streets.length];
            String neighborhood = neighborhoods[random.nextInt(neighborhoods.length)];
            String zipCode = zipCodes[random.nextInt(zipCodes.length)];
            
            double lat = STAMFORD_CENTER_LAT + (random.nextDouble() - 0.5) * LAT_RANGE;
            double lon = STAMFORD_CENTER_LON + (random.nextDouble() - 0.5) * LON_RANGE;
            
            StamfordAddress address = StamfordAddress.builder()
                    .streetAddress(streetNumber + " " + street)
                    .zipCode(zipCode)
                    .location(GeoLocation.of(lat, lon))
                    .neighborhood(neighborhood)
                    .type(i < 7 ? StamfordAddress.AddressType.RESIDENTIAL : 
                          StamfordAddress.AddressType.COMMERCIAL)
                    .build();
            
            addresses.add(address);
            addressCache.put(address.getId(), address);
        }
        
        return addresses;
    }
    
    public Stream<TelemetryData> simulateSensorData(List<StamfordAddress> addresses) {
        return addresses.stream()
                .flatMap(address -> generateSensorDataForAddress(address));
    }
    
    private Stream<TelemetryData> generateSensorDataForAddress(StamfordAddress address) {
        String deviceId = "sensor-" + address.getId();
        
        // Generate realistic sensor data
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        // Temperature varies by time of day and location
        double baseTemp = 20.0 + random.nextGaussian() * 5;
        metrics.put("temperature_celsius", baseTemp);
        
        // Humidity
        double humidity = 40 + random.nextDouble() * 40;
        metrics.put("humidity_percent", humidity);
        
        // Motion detection (more likely in commercial areas)
        boolean motion = address.getType() == StamfordAddress.AddressType.COMMERCIAL ?
                        random.nextDouble() < 0.3 : random.nextDouble() < 0.1;
        metrics.put("motion_detected", motion);
        
        // Air quality index
        double airQuality = 50 + random.nextDouble() * 50;
        metrics.put("air_quality_index", airQuality);
        
        // Energy consumption (higher for commercial)
        double energyBase = address.getType() == StamfordAddress.AddressType.COMMERCIAL ?
                          500 : 100;
        double energy = energyBase + random.nextDouble() * energyBase;
        metrics.put("energy_consumption_kwh", energy);
        
        // Create telemetry data
        TelemetryData telemetryData = TelemetryData.createWithLocation(
                DeviceId.of(deviceId),
                metrics,
                address.getLocation()
        );
        
        return Stream.of(telemetryData);
    }
    
    public List<StamfordAddress> findAddressesNearLocation(GeoLocation center, double radiusKm) {
        return addressCache.values().stream()
                .filter(address -> address.getLocation().distanceTo(center) <= radiusKm)
                .toList();
    }
    
    public Map<String, Object> generateAddressStatistics(List<StamfordAddress> addresses) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        long residential = addresses.stream()
                .filter(a -> a.getType() == StamfordAddress.AddressType.RESIDENTIAL)
                .count();
        long commercial = addresses.stream()
                .filter(a -> a.getType() == StamfordAddress.AddressType.COMMERCIAL)
                .count();
        
        stats.put("total_addresses", addresses.size());
        stats.put("residential_count", residential);
        stats.put("commercial_count", commercial);
        
        Map<String, Long> byNeighborhood = new ConcurrentHashMap<>();
        addresses.forEach(address -> {
            if (address.getNeighborhood() != null) {
                byNeighborhood.merge(address.getNeighborhood(), 1L, Long::sum);
            }
        });
        stats.put("by_neighborhood", byNeighborhood);
        
        return stats;
    }
}