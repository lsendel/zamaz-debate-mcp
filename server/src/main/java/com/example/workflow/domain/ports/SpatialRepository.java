package com.example.workflow.domain.ports;

import com.example.workflow.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Repository port for spatial data operations
 * Supports geographic queries, spatial analysis, and coordinate system transformations
 */
public interface SpatialRepository {
    
    // Basic spatial persistence
    void saveSpatialData(TelemetryData data);
    void saveSpatialDataBatch(List<TelemetryData> dataList);
    
    // Basic spatial queries
    List<TelemetryData> queryByBoundingBox(BoundingBox boundingBox);
    List<TelemetryData> queryByRadius(GeoLocation center, double radiusKm);
    List<TelemetryData> queryByProximity(GeoLocation point, double distanceKm);
    List<TelemetryData> queryByRegion(String region);
    
    // Advanced spatial queries
    List<TelemetryData> queryByPolygon(List<GeoLocation> polygonVertices);
    List<TelemetryData> queryByMultipleRegions(List<BoundingBox> regions);
    List<TelemetryData> queryNearestNeighbors(GeoLocation point, int count);
    List<TelemetryData> queryWithinDistance(GeoLocation point, double minDistanceKm, double maxDistanceKm);
    
    // Spatial queries with time constraints
    List<TelemetryData> queryByBoundingBoxWithTime(BoundingBox boundingBox, Instant start, Instant end);
    List<TelemetryData> queryByRadiusWithTime(GeoLocation center, double radiusKm, Instant start, Instant end);
    List<TelemetryData> queryByRegionWithTime(String region, Instant start, Instant end);
    
    // Spatial analysis functions
    List<SpatialCluster> findClusters(String organizationId, double clusterRadiusKm, int minPoints);
    List<SpatialHotspot> findHotspots(String organizationId, String metricName, double threshold);
    SpatialDensityMap getDensityMap(BoundingBox area, int gridSize);
    List<SpatialCorrelation> findSpatialCorrelations(String organizationId, List<String> metrics, double radiusKm);
    
    // Geographic feature queries
    List<GeographicFeature> queryFeatures(BoundingBox boundingBox, FeatureType featureType);
    List<GeographicFeature> queryFeaturesNear(GeoLocation point, double radiusKm, FeatureType featureType);
    List<String> getRegionNames(BoundingBox boundingBox);
    
    // Proximity analysis
    List<ProximityResult> findNearbyDevices(DeviceId deviceId, double radiusKm);
    Map<DeviceId, List<DeviceId>> findDeviceNeighbors(String organizationId, double radiusKm);
    List<TelemetryData> findDataPointsNearPath(List<GeoLocation> path, double corridorWidthKm);
    
    // Coordinate system support
    List<TelemetryData> queryWithProjection(BoundingBox boundingBox, CoordinateSystem targetSystem);
    GeoLocation transformCoordinates(GeoLocation location, CoordinateSystem from, CoordinateSystem to);
    BoundingBox transformBoundingBox(BoundingBox bbox, CoordinateSystem from, CoordinateSystem to);
    
    // Spatial statistics
    SpatialStatistics getSpatialStatistics(String organizationId, BoundingBox area);
    Map<String, Double> calculateDistanceMatrix(List<GeoLocation> locations);
    double calculateArea(List<GeoLocation> polygonVertices);
    GeoLocation calculateCentroid(List<GeoLocation> locations);
    
    // Data management
    void deleteSpatialData(BoundingBox area);
    void deleteSpatialDataByDevice(DeviceId deviceId);
    long countSpatialData(BoundingBox area);
    
    /**
     * Spatial cluster result
     */
    record SpatialCluster(
        int clusterId,
        GeoLocation center,
        double radiusKm,
        List<TelemetryData> dataPoints,
        int pointCount,
        Map<String, Double> averageMetrics
    ) {}
    
    /**
     * Spatial hotspot result
     */
    record SpatialHotspot(
        GeoLocation center,
        double radiusKm,
        String metricName,
        double averageValue,
        double maxValue,
        int dataPointCount,
        double significance
    ) {}
    
    /**
     * Spatial density map
     */
    record SpatialDensityMap(
        BoundingBox area,
        int gridSize,
        double[][] densityGrid,
        double maxDensity,
        double minDensity
    ) {}
    
    /**
     * Spatial correlation result
     */
    record SpatialCorrelation(
        String metric1,
        String metric2,
        double correlation,
        double significance,
        int sampleSize,
        GeoLocation center,
        double radiusKm
    ) {}
    
    /**
     * Geographic feature
     */
    record GeographicFeature(
        String id,
        String name,
        FeatureType type,
        GeoLocation location,
        BoundingBox bounds,
        Map<String, Object> properties
    ) {}
    
    /**
     * Proximity analysis result
     */
    record ProximityResult(
        DeviceId deviceId,
        GeoLocation location,
        double distanceKm,
        Instant lastSeen,
        Map<String, MetricValue> lastMetrics
    ) {}
    
    /**
     * Spatial statistics
     */
    record SpatialStatistics(
        long totalDataPoints,
        double averageLatitude,
        double averageLongitude,
        GeoLocation centroid,
        BoundingBox bounds,
        double averageDistanceFromCentroid,
        double spatialSpread,
        Map<String, Integer> regionCounts
    ) {}
    
    /**
     * Geographic feature types
     */
    enum FeatureType {
        CITY, REGION, LANDMARK, ROAD, WATER_BODY, ADMINISTRATIVE_BOUNDARY
    }
    
    /**
     * Coordinate system types
     */
    enum CoordinateSystem {
        WGS84(4326),
        WEB_MERCATOR(3857),
        UTM_ZONE_18N(32618),
        STATE_PLANE_CT(2775);
        
        private final int epsgCode;
        
        CoordinateSystem(int epsgCode) {
            this.epsgCode = epsgCode;
        }
        
        public int getEpsgCode() {
            return epsgCode;
        }
    }
}