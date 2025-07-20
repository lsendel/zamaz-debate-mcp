package com.zamaz.telemetry.domain.service;

import com.zamaz.telemetry.domain.entity.TelemetryAnalysis;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.query.TelemetryQuery;
import com.zamaz.telemetry.domain.repository.TelemetryRepository;
import com.zamaz.telemetry.domain.event.TelemetryThresholdExceededEvent;
import com.zamaz.telemetry.domain.event.EventPublisher;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TelemetryDomainServiceImpl implements TelemetryDomainService {
    private final TelemetryRepository telemetryRepository;
    private final EventPublisher eventPublisher;
    private final Map<String, ThresholdConfiguration> thresholdConfigurations = new ConcurrentHashMap<>();
    
    @Override
    public void processTelemetryStream(Stream<TelemetryData> dataStream) {
        dataStream.forEach(data -> {
            // Validate data
            if (validateTelemetryData(data)) {
                // Enrich data
                TelemetryData enrichedData = enrichTelemetryData(data);
                
                // Save to repository
                telemetryRepository.saveTimeSeries(enrichedData);
                if (enrichedData.hasLocation()) {
                    telemetryRepository.saveSpatialData(enrichedData);
                }
                
                // Check thresholds and trigger events
                checkThresholds(enrichedData);
            }
        });
    }
    
    @Override
    public TelemetryAnalysis analyzeTelemetry(TelemetryQuery query) {
        Stream<TelemetryData> dataStream = telemetryRepository.queryTimeSeries(
            new TimeRange(query.getStartTime(), query.getEndTime())
        );
        
        // Filter by devices if specified
        if (query.getDeviceIds() != null && !query.getDeviceIds().isEmpty()) {
            dataStream = dataStream.filter(data -> 
                query.getDeviceIds().contains(data.getDeviceId())
            );
        }
        
        // Filter by location if specified
        if (query.getGeoQuery() != null) {
            dataStream = dataStream.filter(data -> 
                matchesGeoQuery(data, query.getGeoQuery())
            );
        }
        
        // Collect data for analysis
        List<TelemetryData> dataList = dataStream.toList();
        
        if (dataList.isEmpty()) {
            return createEmptyAnalysis(query);
        }
        
        // Calculate statistics
        Map<String, Double> averages = new HashMap<>();
        Map<String, Double> minimums = new HashMap<>();
        Map<String, Double> maximums = new HashMap<>();
        Map<String, Double> stdDevs = new HashMap<>();
        
        Set<String> metricNames = extractMetricNames(dataList);
        
        for (String metricName : metricNames) {
            List<Double> values = extractMetricValues(dataList, metricName);
            
            if (!values.isEmpty()) {
                averages.put(metricName, calculateAverage(values));
                minimums.put(metricName, Collections.min(values));
                maximums.put(metricName, Collections.max(values));
                stdDevs.put(metricName, calculateStandardDeviation(values));
            }
        }
        
        double qualityScore = calculateQualityScore(dataList);
        
        return TelemetryAnalysis.builder()
                .analysisId(UUID.randomUUID().toString())
                .startTime(query.getStartTime())
                .endTime(query.getEndTime())
                .dataPointCount(dataList.size())
                .averageMetrics(averages)
                .minMetrics(minimums)
                .maxMetrics(maximums)
                .standardDeviationMetrics(stdDevs)
                .dataQualityScore(qualityScore)
                .build();
    }
    
    @Override
    public void triggerWorkflowConditions(TelemetryData data) {
        // Check if any workflow conditions should be triggered
        for (Map.Entry<String, ThresholdConfiguration> entry : thresholdConfigurations.entrySet()) {
            ThresholdConfiguration config = entry.getValue();
            
            Object metricValue = data.getMetric(config.getMetricName());
            if (metricValue instanceof Number) {
                double value = ((Number) metricValue).doubleValue();
                
                if (config.isExceeded(value)) {
                    TelemetryThresholdExceededEvent event = TelemetryThresholdExceededEvent.builder()
                            .telemetryId(data.getId())
                            .deviceId(data.getDeviceId())
                            .metricName(config.getMetricName())
                            .thresholdValue(config.getThresholdValue())
                            .actualValue(value)
                            .workflowId(config.getWorkflowId())
                            .build();
                    
                    eventPublisher.publish(event);
                }
            }
        }
    }
    
    @Override
    public boolean validateTelemetryData(TelemetryData data) {
        // Basic validation
        if (data.getId() == null || data.getDeviceId() == null || data.getTimestamp() == null) {
            return false;
        }
        
        // Check timestamp is not in the future
        if (data.getTimestamp().isAfter(Instant.now().plusSeconds(60))) {
            return false;
        }
        
        // Check timestamp is not too old (e.g., more than 7 days)
        if (data.getTimestamp().isBefore(Instant.now().minusSeconds(7 * 24 * 60 * 60))) {
            return false;
        }
        
        // Validate metrics
        if (data.getMetrics().isEmpty()) {
            return false;
        }
        
        // Validate location if present
        if (data.hasLocation()) {
            GeoLocation location = data.getLocation();
            if (location.getLatitude() < -90 || location.getLatitude() > 90 ||
                location.getLongitude() < -180 || location.getLongitude() > 180) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public TelemetryData enrichTelemetryData(TelemetryData data) {
        Map<String, Object> enrichedMetrics = new HashMap<>(data.getMetrics());
        
        // Add derived metrics
        if (enrichedMetrics.containsKey("temperature_celsius")) {
            double celsius = ((Number) enrichedMetrics.get("temperature_celsius")).doubleValue();
            enrichedMetrics.put("temperature_fahrenheit", celsius * 9/5 + 32);
        }
        
        // Add metadata
        enrichedMetrics.put("_processed_at", Instant.now().toString());
        enrichedMetrics.put("_quality_score", data.getQualityScore());
        
        return TelemetryData.builder()
                .id(data.getId())
                .deviceId(data.getDeviceId())
                .timestamp(data.getTimestamp())
                .metrics(enrichedMetrics)
                .location(data.getLocation())
                .dataSource(data.getDataSource())
                .qualityScore(data.getQualityScore())
                .build();
    }
    
    private void checkThresholds(TelemetryData data) {
        triggerWorkflowConditions(data);
    }
    
    private boolean matchesGeoQuery(TelemetryData data, TelemetryQuery.GeoQuery geoQuery) {
        if (!data.hasLocation()) {
            return false;
        }
        
        GeoLocation location = data.getLocation();
        
        if (geoQuery.getBoundingBox() != null) {
            return geoQuery.getBoundingBox().contains(location);
        }
        
        if (geoQuery.getCenter() != null && geoQuery.getRadiusKm() > 0) {
            double distance = location.distanceTo(geoQuery.getCenter());
            return distance <= geoQuery.getRadiusKm();
        }
        
        return true;
    }
    
    private Set<String> extractMetricNames(List<TelemetryData> dataList) {
        Set<String> metricNames = new HashSet<>();
        for (TelemetryData data : dataList) {
            metricNames.addAll(data.getMetrics().keySet());
        }
        metricNames.removeIf(name -> name.startsWith("_")); // Remove metadata fields
        return metricNames;
    }
    
    private List<Double> extractMetricValues(List<TelemetryData> dataList, String metricName) {
        List<Double> values = new ArrayList<>();
        for (TelemetryData data : dataList) {
            Object value = data.getMetric(metricName);
            if (value instanceof Number) {
                values.add(((Number) value).doubleValue());
            }
        }
        return values;
    }
    
    private double calculateAverage(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    private double calculateStandardDeviation(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        double mean = calculateAverage(values);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private double calculateQualityScore(List<TelemetryData> dataList) {
        return dataList.stream()
                .mapToInt(TelemetryData::getQualityScore)
                .average()
                .orElse(0.0);
    }
    
    private TelemetryAnalysis createEmptyAnalysis(TelemetryQuery query) {
        return TelemetryAnalysis.builder()
                .analysisId(UUID.randomUUID().toString())
                .startTime(query.getStartTime())
                .endTime(query.getEndTime())
                .dataPointCount(0)
                .averageMetrics(new HashMap<>())
                .minMetrics(new HashMap<>())
                .maxMetrics(new HashMap<>())
                .standardDeviationMetrics(new HashMap<>())
                .dataQualityScore(0.0)
                .build();
    }
    
    public void registerThreshold(String id, ThresholdConfiguration configuration) {
        thresholdConfigurations.put(id, configuration);
    }
    
    @lombok.Getter
    @lombok.Builder
    public static class ThresholdConfiguration {
        private final String metricName;
        private final double thresholdValue;
        private final ThresholdType type;
        private final String workflowId;
        
        public boolean isExceeded(double value) {
            switch (type) {
                case GREATER_THAN:
                    return value > thresholdValue;
                case LESS_THAN:
                    return value < thresholdValue;
                case EQUALS:
                    return Math.abs(value - thresholdValue) < 0.0001;
                default:
                    return false;
            }
        }
    }
    
    public enum ThresholdType {
        GREATER_THAN,
        LESS_THAN,
        EQUALS
    }
    
    public static class TimeRange {
        private final Instant start;
        private final Instant end;
        
        public TimeRange(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }
        
        public Instant getStart() {
            return start;
        }
        
        public Instant getEnd() {
            return end;
        }
    }
}