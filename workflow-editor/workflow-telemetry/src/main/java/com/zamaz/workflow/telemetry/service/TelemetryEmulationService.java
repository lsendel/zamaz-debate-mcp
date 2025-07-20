package com.zamaz.workflow.telemetry.service;

import com.zamaz.workflow.telemetry.model.TelemetryData;
import com.zamaz.workflow.telemetry.model.GeoLocation;
import com.zamaz.workflow.telemetry.model.TelemetryMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryEmulationService {
    
    private final Map<String, DeviceEmulator> emulators = new ConcurrentHashMap<>();
    private final Sinks.Many<TelemetryData> telemetrySink = Sinks.many().multicast().onBackpressureBuffer();
    
    // Configuration
    private static final int DEFAULT_DEVICE_COUNT = 100;
    private static final int EMISSION_RATE_HZ = 10; // 10Hz
    private static final long EMISSION_INTERVAL_MS = 1000 / EMISSION_RATE_HZ;
    
    // Geographic bounds
    private static final double NA_MIN_LAT = 25.0;
    private static final double NA_MAX_LAT = 60.0;
    private static final double NA_MIN_LNG = -130.0;
    private static final double NA_MAX_LNG = -60.0;
    
    private static final double EU_MIN_LAT = 35.0;
    private static final double EU_MAX_LAT = 70.0;
    private static final double EU_MIN_LNG = -10.0;
    private static final double EU_MAX_LNG = 40.0;
    
    public void startEmulation(EmulationConfig config) {
        log.info("Starting telemetry emulation with {} devices", config.getDeviceCount());
        
        // Create device emulators
        IntStream.range(0, config.getDeviceCount())
                .forEach(i -> {
                    String deviceId = String.format("device-%s-%d", config.getRegion(), i);
                    DeviceEmulator emulator = createDeviceEmulator(deviceId, config);
                    emulators.put(deviceId, emulator);
                });
        
        log.info("Created {} device emulators", emulators.size());
    }
    
    public void stopEmulation() {
        log.info("Stopping telemetry emulation");
        emulators.clear();
    }
    
    @Scheduled(fixedDelay = EMISSION_INTERVAL_MS)
    public void emitTelemetryData() {
        if (emulators.isEmpty()) return;
        
        emulators.values().parallelStream()
                .map(DeviceEmulator::generateTelemetryData)
                .forEach(telemetrySink::tryEmitNext);
    }
    
    public Flux<TelemetryData> getTelemetryStream() {
        return telemetrySink.asFlux();
    }
    
    private DeviceEmulator createDeviceEmulator(String deviceId, EmulationConfig config) {
        GeoLocation location = generateRandomLocation(config.getRegion());
        
        return DeviceEmulator.builder()
                .deviceId(deviceId)
                .deviceType(config.getDeviceTypes().get(
                    ThreadLocalRandom.current().nextInt(config.getDeviceTypes().size())
                ))
                .location(location)
                .baseValues(generateBaseValues(config))
                .variancePercent(config.getVariancePercent())
                .anomalyProbability(config.getAnomalyProbability())
                .build();
    }
    
    private GeoLocation generateRandomLocation(String region) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double lat, lng;
        
        switch (region.toUpperCase()) {
            case "NA":
            case "NORTH_AMERICA":
                lat = random.nextDouble(NA_MIN_LAT, NA_MAX_LAT);
                lng = random.nextDouble(NA_MIN_LNG, NA_MAX_LNG);
                break;
            case "EU":
            case "EUROPE":
                lat = random.nextDouble(EU_MIN_LAT, EU_MAX_LAT);
                lng = random.nextDouble(EU_MIN_LNG, EU_MAX_LNG);
                break;
            default:
                // Global random
                lat = random.nextDouble(-90, 90);
                lng = random.nextDouble(-180, 180);
        }
        
        return GeoLocation.builder()
                .latitude(lat)
                .longitude(lng)
                .altitude(random.nextDouble(0, 1000))
                .build();
    }
    
    private Map<String, Double> generateBaseValues(EmulationConfig config) {
        Map<String, Double> baseValues = new HashMap<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Standard sensor types
        baseValues.put("temperature", random.nextDouble(15, 30));
        baseValues.put("pressure", random.nextDouble(950, 1050));
        baseValues.put("humidity", random.nextDouble(30, 70));
        baseValues.put("speed", random.nextDouble(0, 100));
        baseValues.put("vibration", random.nextDouble(0, 10));
        baseValues.put("power", random.nextDouble(100, 1000));
        
        // Add custom metrics from config
        config.getCustomMetrics().forEach((key, range) -> {
            baseValues.put(key, random.nextDouble(range[0], range[1]));
        });
        
        return baseValues;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class EmulationConfig {
        @lombok.Builder.Default
        private int deviceCount = DEFAULT_DEVICE_COUNT;
        
        @lombok.Builder.Default
        private String region = "GLOBAL";
        
        @lombok.Builder.Default
        private List<String> deviceTypes = Arrays.asList("sensor", "actuator", "gateway");
        
        @lombok.Builder.Default
        private double variancePercent = 10.0;
        
        @lombok.Builder.Default
        private double anomalyProbability = 0.01;
        
        @lombok.Builder.Default
        private Map<String, double[]> customMetrics = new HashMap<>();
        
        @lombok.Builder.Default
        private boolean includeQualityMetrics = true;
        
        @lombok.Builder.Default
        private boolean simulateMissingData = true;
        
        @lombok.Builder.Default
        private double missingDataProbability = 0.02;
    }
    
    @lombok.Data
    @lombok.Builder
    private static class DeviceEmulator {
        private final String deviceId;
        private final String deviceType;
        private final GeoLocation location;
        private final Map<String, Double> baseValues;
        private final double variancePercent;
        private final double anomalyProbability;
        
        public TelemetryData generateTelemetryData() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Map<String, Object> metrics = new HashMap<>();
            
            // Generate metrics with variance
            baseValues.forEach((metric, baseValue) -> {
                double variance = baseValue * (variancePercent / 100);
                double value = baseValue + random.nextGaussian() * variance;
                
                // Simulate anomalies
                if (random.nextDouble() < anomalyProbability) {
                    value *= random.nextDouble(1.5, 3.0); // Spike
                }
                
                metrics.put(metric, value);
            });
            
            // Add quality metrics
            metrics.put("signalStrength", random.nextDouble(60, 100));
            metrics.put("batteryLevel", random.nextDouble(20, 100));
            
            // Create metadata
            TelemetryMetadata metadata = TelemetryMetadata.builder()
                    .source("emulator")
                    .quality(random.nextDouble() > 0.1 ? "GOOD" : "POOR")
                    .processingTime(random.nextLong(1, 50))
                    .tags(Arrays.asList("emulated", deviceType, "region:" + getRegionFromLocation()))
                    .build();
            
            return TelemetryData.builder()
                    .id(UUID.randomUUID().toString())
                    .deviceId(deviceId)
                    .timestamp(Instant.now())
                    .location(location)
                    .metrics(metrics)
                    .metadata(metadata)
                    .build();
        }
        
        private String getRegionFromLocation() {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            
            if (lat >= NA_MIN_LAT && lat <= NA_MAX_LAT && 
                lng >= NA_MIN_LNG && lng <= NA_MAX_LNG) {
                return "NA";
            } else if (lat >= EU_MIN_LAT && lat <= EU_MAX_LAT && 
                       lng >= EU_MIN_LNG && lng <= EU_MAX_LNG) {
                return "EU";
            }
            return "GLOBAL";
        }
    }
}