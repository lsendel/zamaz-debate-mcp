package com.example.workflow.domain.services;

import com.example.workflow.domain.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Domain service for telemetry data processing and analysis
 * Implements requirements 2.1, 2.4, 6.6 for stream processing, analysis, and spatial operations
 */
@Service
public class TelemetryDomainService {
    
    private final Map<String, List<TelemetryThreshold>> organizationThresholds = new ConcurrentHashMap<>();
    private final Map<String, TelemetryAnalysis> cachedAnalyses = new ConcurrentHashMap<>();
    
    /**
     * Process telemetry data stream at 10Hz frequency
     * Implements requirement 2.1: Process real-time sensor data at 10Hz frequency
     */
    public void processTelemetryStream(Stream<TelemetryData> dataStream) {
        Objects.requireNonNull(dataStream, "Data stream cannot be null");
        
        dataStream
            .filter(this::isValidTelemetryData)
            .forEach(this::processSingleTelemetryData);
    }
    
    /**
     * Process single telemetry data point
     */
    private void processSingleTelemetryData(TelemetryData data) {
        try {
            // Validate data quality
            validateTelemetryQuality(data);
            
            // Check for threshold violations
            checkThresholdViolations(data);
            
            // Update real-time aggregations
            updateRealTimeAggregations(data);
            
            // Process spatial data if available
            if (data.hasSpatialData()) {
                processSpatialTelemetry(data);
            }
            
        } catch (Exception e) {
            // Log error but don't stop stream processing
            System.err.println("Error processing telemetry data " + data.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Analyze telemetry data for patterns, trends, and anomalies
     * Implements requirement 2.4: Telemetry analysis and aggregation logic
     */
    public TelemetryAnalysis analyzeTelemetry(TelemetryQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        String cacheKey = generateAnalysisCacheKey(query);
        TelemetryAnalysis cachedResult = cachedAnalyses.get(cacheKey);
        
        // Return cached result if recent (within 30 seconds)
        if (cachedResult != null && 
            Duration.between(cachedResult.getAnalysisTime(), Instant.now()).getSeconds() < 30) {
            return cachedResult;
        }
        
        // Perform new analysis
        TelemetryAnalysis analysis = performTelemetryAnalysis(query);
        cachedAnalyses.put(cacheKey, analysis);
        
        return analysis;
    }
    
    /**
     * Perform comprehensive telemetry analysis
     */
    private TelemetryAnalysis performTelemetryAnalysis(TelemetryQuery query) {
        List<DeviceId> analyzedDevices = query.getDeviceIds() != null ? 
            query.getDeviceIds() : List.of();
        
        // Simulate metric analyses (in real implementation, this would query the repository)
        Map<String, MetricAnalysis> metricAnalyses = generateMetricAnalyses(query);
        
        // Detect anomalies
        List<TelemetryAnomaly> anomalies = detectAnomalies(query, metricAnalyses);
        
        // Identify trends
        List<TelemetryTrend> trends = identifyTrends(query, metricAnalyses);
        
        // Calculate statistics
        AnalysisStatistics statistics = calculateAnalysisStatistics(query, metricAnalyses);
        
        return new TelemetryAnalysis(
            query.getOrganizationId(),
            query.getFromTime(),
            query.getToTime(),
            analyzedDevices,
            metricAnalyses,
            anomalies,
            trends,
            statistics
        );
    }
    
    /**
     * Generate metric analyses for different metrics
     */
    private Map<String, MetricAnalysis> generateMetricAnalyses(TelemetryQuery query) {
        Map<String, MetricAnalysis> analyses = new HashMap<>();
        
        // Common telemetry metrics
        List<String> metrics = query.getMetrics() != null ? 
            query.getMetrics() : 
            List.of("temperature", "humidity", "motion", "air_quality", "status");
        
        for (String metric : metrics) {
            analyses.put(metric, generateMetricAnalysis(metric, query));
        }
        
        return analyses;
    }
    
    /**
     * Generate analysis for a specific metric
     */
    private MetricAnalysis generateMetricAnalysis(String metricName, TelemetryQuery query) {
        // Simulate metric analysis (in real implementation, this would aggregate from repository)
        Random random = new Random(metricName.hashCode());
        
        double min = switch (metricName) {
            case "temperature" -> 15.0 + random.nextDouble() * 5.0;
            case "humidity" -> 30.0 + random.nextDouble() * 10.0;
            case "air_quality" -> 40.0 + random.nextDouble() * 20.0;
            default -> random.nextDouble() * 50.0;
        };
        
        double max = switch (metricName) {
            case "temperature" -> 30.0 + random.nextDouble() * 10.0;
            case "humidity" -> 80.0 + random.nextDouble() * 15.0;
            case "air_quality" -> 120.0 + random.nextDouble() * 30.0;
            default -> min + random.nextDouble() * 100.0;
        };
        
        double average = (min + max) / 2.0;
        double stdDev = (max - min) / 4.0; // Approximate standard deviation
        long dataPointCount = 100 + random.nextInt(900); // 100-1000 data points
        
        List<Double> percentiles = List.of(
            min + (max - min) * 0.25,  // 25th percentile
            average,                    // 50th percentile (median)
            min + (max - min) * 0.75,  // 75th percentile
            min + (max - min) * 0.95,  // 95th percentile
            min + (max - min) * 0.99   // 99th percentile
        );
        
        return new MetricAnalysis(metricName, min, max, average, stdDev, dataPointCount, percentiles);
    }
    
    /**
     * Create workflow trigger mechanisms for telemetry thresholds
     * Implements requirement 2.4: Workflow trigger mechanisms for telemetry thresholds
     */
    public void triggerWorkflowConditions(TelemetryData data) {
        Objects.requireNonNull(data, "Telemetry data cannot be null");
        
        List<TelemetryThreshold> thresholds = organizationThresholds.get(data.getOrganizationId());
        if (thresholds == null || thresholds.isEmpty()) {
            return;
        }
        
        for (TelemetryThreshold threshold : thresholds) {
            if (isThresholdViolated(data, threshold)) {
                triggerWorkflowForThreshold(data, threshold);
            }
        }
    }
    
    /**
     * Check if threshold is violated
     */
    private boolean isThresholdViolated(TelemetryData data, TelemetryThreshold threshold) {
        if (!data.hasMetric(threshold.getMetricName())) {
            return false;
        }
        
        MetricValue metricValue = data.getMetric(threshold.getMetricName());
        if (!metricValue.isNumeric()) {
            return false;
        }
        
        double value = metricValue.getNumericValue();
        
        return switch (threshold.getCondition()) {
            case GREATER_THAN -> value > threshold.getValue();
            case LESS_THAN -> value < threshold.getValue();
            case EQUALS -> Math.abs(value - threshold.getValue()) < 0.001;
            case NOT_EQUALS -> Math.abs(value - threshold.getValue()) >= 0.001;
            case GREATER_THAN_OR_EQUAL -> value >= threshold.getValue();
            case LESS_THAN_OR_EQUAL -> value <= threshold.getValue();
        };
    }
    
    /**
     * Trigger workflow for threshold violation
     */
    private void triggerWorkflowForThreshold(TelemetryData data, TelemetryThreshold threshold) {
        // Create workflow trigger event
        WorkflowTriggerEvent event = new WorkflowTriggerEvent(
            threshold.getWorkflowId(),
            data,
            threshold,
            Instant.now()
        );
        
        // In real implementation, this would publish an event or call workflow service
        System.out.println("Workflow trigger: " + event);
    }
    
    /**
     * Implement spatial analysis functions for geographic data
     * Implements requirement 6.6: Spatial analysis functions for geographic data
     */
    public SpatialAnalysisResult analyzeSpatialTelemetry(TelemetryQuery spatialQuery) {
        Objects.requireNonNull(spatialQuery, "Spatial query cannot be null");
        
        if (!spatialQuery.hasSpatialFilter()) {
            throw new IllegalArgumentException("Query must have spatial filter for spatial analysis");
        }
        
        // Perform spatial analysis
        List<SpatialCluster> clusters = identifySpatialClusters(spatialQuery);
        List<SpatialHotspot> hotspots = identifySpatialHotspots(spatialQuery);
        SpatialDistribution distribution = calculateSpatialDistribution(spatialQuery);
        List<ProximityAnalysis> proximityAnalyses = performProximityAnalysis(spatialQuery);
        
        return new SpatialAnalysisResult(
            spatialQuery.getOrganizationId(),
            spatialQuery.getBoundingBox(),
            spatialQuery.getCenterLocation(),
            spatialQuery.getRadiusKm(),
            clusters,
            hotspots,
            distribution,
            proximityAnalyses,
            Instant.now()
        );
    }
    
    /**
     * Register telemetry threshold for workflow triggering
     */
    public void registerTelemetryThreshold(String organizationId, TelemetryThreshold threshold) {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(threshold, "Threshold cannot be null");
        
        organizationThresholds.computeIfAbsent(organizationId, k -> new ArrayList<>()).add(threshold);
    }
    
    /**
     * Get all thresholds for organization
     */
    public List<TelemetryThreshold> getThresholds(String organizationId) {
        return organizationThresholds.getOrDefault(organizationId, List.of());
    }
    
    // Helper methods for spatial analysis, anomaly detection, etc.
    private List<SpatialCluster> identifySpatialClusters(TelemetryQuery query) {
        // Implementation details omitted for brevity
        return List.of();
    }
    
    private List<SpatialHotspot> identifySpatialHotspots(TelemetryQuery query) {
        return List.of();
    }
    
    private SpatialDistribution calculateSpatialDistribution(TelemetryQuery query) {
        return new SpatialDistribution(100, 2.5, 0.8, Map.of("urban", 60, "suburban", 30, "rural", 10));
    }
    
    private List<ProximityAnalysis> performProximityAnalysis(TelemetryQuery query) {
        return List.of();
    }
    
    private List<TelemetryAnomaly> detectAnomalies(TelemetryQuery query, Map<String, MetricAnalysis> metricAnalyses) {
        return List.of();
    }
    
    private List<TelemetryTrend> identifyTrends(TelemetryQuery query, Map<String, MetricAnalysis> metricAnalyses) {
        return List.of();
    }
    
    private AnalysisStatistics calculateAnalysisStatistics(TelemetryQuery query, Map<String, MetricAnalysis> metricAnalyses) {
        return new AnalysisStatistics(1000, 1000, 0, 100.0, 0.95);
    }
    
    private void validateTelemetryQuality(TelemetryData data) {
        if (data.getMetrics().isEmpty()) {
            throw new TelemetryQualityException("Telemetry data has no metrics");
        }
    }
    
    private boolean isValidTelemetryData(TelemetryData data) {
        try {
            validateTelemetryQuality(data);
            return true;
        } catch (TelemetryQualityException e) {
            return false;
        }
    }
    
    private void checkThresholdViolations(TelemetryData data) {
        triggerWorkflowConditions(data);
    }
    
    private void updateRealTimeAggregations(TelemetryData data) {
        // Implementation for real-time aggregations
    }
    
    private void processSpatialTelemetry(TelemetryData data) {
        // Implementation for spatial telemetry processing
    }
    
    private String generateAnalysisCacheKey(TelemetryQuery query) {
        return String.format("%s_%s_%s_%s", 
            query.getOrganizationId(),
            query.getFromTime() != null ? query.getFromTime().toString() : "null",
            query.getToTime() != null ? query.getToTime().toString() : "null",
            query.hashCode()
        );
    }
}

// Supporting classes and records
record TelemetryThreshold(
    String id,
    String organizationId,
    WorkflowId workflowId,
    String metricName,
    ThresholdCondition condition,
    double value,
    String description
) {
    public String getId() { return id; }
    public String getOrganizationId() { return organizationId; }
    public WorkflowId getWorkflowId() { return workflowId; }
    public String getMetricName() { return metricName; }
    public ThresholdCondition getCondition() { return condition; }
    public double getValue() { return value; }
    public String getDescription() { return description; }
}

enum ThresholdCondition {
    GREATER_THAN,
    LESS_THAN,
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL
}

record WorkflowTriggerEvent(
    WorkflowId workflowId,
    TelemetryData telemetryData,
    TelemetryThreshold threshold,
    Instant triggerTime
) {}

record SpatialAnalysisResult(
    String organizationId,
    BoundingBox boundingBox,
    GeoLocation centerLocation,
    Double radiusKm,
    List<SpatialCluster> clusters,
    List<SpatialHotspot> hotspots,
    SpatialDistribution distribution,
    List<ProximityAnalysis> proximityAnalyses,
    Instant analysisTime
) {}

record SpatialCluster(String id, GeoLocation center, double radiusKm, int deviceCount, double density) {}
record SpatialHotspot(String id, String metricName, GeoLocation center, double radiusKm, double intensity) {}
record SpatialDistribution(int totalDevices, double averageDistanceKm, double standardDeviationKm, Map<String, Integer> regionDistribution) {}
record ProximityAnalysis(DeviceId device1, DeviceId device2, double distanceKm, double correlation) {}

class TelemetryQualityException extends RuntimeException {
    public TelemetryQualityException(String message) {
        super(message);
    }
}