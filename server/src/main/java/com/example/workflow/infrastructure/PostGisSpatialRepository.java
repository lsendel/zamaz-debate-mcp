package com.example.workflow.infrastructure;

import com.example.workflow.domain.*;
import com.example.workflow.domain.ports.SpatialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostGIS implementation of SpatialRepository
 * Provides geographic queries, spatial analysis, and coordinate system transformations
 */
@Repository
@Transactional
public class PostGisSpatialRepository implements SpatialRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public PostGisSpatialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeSpatialTables();
    }
    
    private void initializeSpatialTables() {
        // Create spatial telemetry table if it doesn't exist
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS spatial_telemetry (
                id SERIAL PRIMARY KEY,
                telemetry_id VARCHAR(255) NOT NULL,
                device_id VARCHAR(255) NOT NULL,
                organization_id VARCHAR(255) NOT NULL,
                location GEOMETRY(POINT, 4326) NOT NULL,
                timestamp TIMESTAMPTZ NOT NULL,
                metrics JSONB NOT NULL,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )
            """);
        
        // Create spatial indexes
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_location ON spatial_telemetry USING GIST(location)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_timestamp ON spatial_telemetry(timestamp)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_device ON spatial_telemetry(device_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_org ON spatial_telemetry(organization_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_spatial_telemetry_metrics ON spatial_telemetry USING GIN(metrics)");
        
        // Create Stamford addresses table for geospatial sample
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS stamford_addresses (
                id SERIAL PRIMARY KEY,
                address TEXT NOT NULL,
                location GEOMETRY(POINT, 4326) NOT NULL,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )
            """);
        
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_stamford_addresses_location ON stamford_addresses USING GIST(location)");
        
        // Create geographic features table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS geographic_features (
                id SERIAL PRIMARY KEY,
                feature_id VARCHAR(255) UNIQUE NOT NULL,
                name VARCHAR(255) NOT NULL,
                feature_type VARCHAR(50) NOT NULL,
                location GEOMETRY(POINT, 4326),
                bounds GEOMETRY(POLYGON, 4326),
                properties JSONB,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )
            """);
        
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_geographic_features_location ON geographic_features USING GIST(location)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_geographic_features_bounds ON geographic_features USING GIST(bounds)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_geographic_features_type ON geographic_features(feature_type)");
    }
    
    @Override
    public void saveSpatialData(TelemetryData data) {
        if (!data.hasSpatialData()) {
            return;
        }
        
        String sql = """
            INSERT INTO spatial_telemetry (telemetry_id, device_id, organization_id, location, timestamp, metrics)
            VALUES (?, ?, ?, ST_GeomFromText(?, 4326), ?, ?::jsonb)
            """;
        
        jdbcTemplate.update(sql,
            data.getId().getValue(),
            data.getDeviceId().getValue(),
            data.getOrganizationId(),
            data.getLocation().toWKT(),
            data.getTimestamp(),
            convertMetricsToJson(data.getMetrics())
        );
    }
    
    @Override
    public void saveSpatialDataBatch(List<TelemetryData> dataList) {
        List<TelemetryData> spatialData = dataList.stream()
            .filter(TelemetryData::hasSpatialData)
            .collect(Collectors.toList());
        
        if (spatialData.isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO spatial_telemetry (telemetry_id, device_id, organization_id, location, timestamp, metrics)
            VALUES (?, ?, ?, ST_GeomFromText(?, 4326), ?, ?::jsonb)
            """;
        
        jdbcTemplate.batchUpdate(sql, spatialData, spatialData.size(),
            (ps, data) -> {
                ps.setString(1, data.getId().getValue());
                ps.setString(2, data.getDeviceId().getValue());
                ps.setString(3, data.getOrganizationId());
                ps.setString(4, data.getLocation().toWKT());
                ps.setTimestamp(5, java.sql.Timestamp.from(data.getTimestamp()));
                ps.setString(6, convertMetricsToJson(data.getMetrics()));
            });
    }
    
    @Override
    public List<TelemetryData> queryByBoundingBox(BoundingBox boundingBox) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id, 
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            ORDER BY timestamp DESC
            """;
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(),
            boundingBox.minLongitude(), boundingBox.minLatitude(),
            boundingBox.maxLongitude(), boundingBox.maxLatitude());
    }
    
    @Override
    public List<TelemetryData> queryByRadius(GeoLocation center, double radiusKm) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_DWithin(location, ST_GeomFromText(?, 4326), ?)
            ORDER BY ST_Distance(location, ST_GeomFromText(?, 4326))
            """;
        
        double radiusMeters = radiusKm * 1000;
        String centerWKT = center.toWKT();
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(),
            centerWKT, radiusMeters, centerWKT);
    }
    
    @Override
    public List<TelemetryData> queryByProximity(GeoLocation point, double distanceKm) {
        return queryByRadius(point, distanceKm);
    }
    
    @Override
    public List<TelemetryData> queryByRegion(String region) {
        // Query by predefined regions (could be enhanced with actual region geometries)
        BoundingBox regionBounds = switch (region.toLowerCase()) {
            case "stamford" -> BoundingBox.stamfordCT();
            case "north_america" -> BoundingBox.northAmerica();
            case "europe" -> BoundingBox.europe();
            default -> throw new IllegalArgumentException("Unknown region: " + region);
        };
        
        return queryByBoundingBox(regionBounds);
    }
    
    @Override
    public List<TelemetryData> queryByPolygon(List<GeoLocation> polygonVertices) {
        if (polygonVertices.size() < 3) {
            throw new IllegalArgumentException("Polygon must have at least 3 vertices");
        }
        
        String polygonWKT = createPolygonWKT(polygonVertices);
        
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_Within(location, ST_GeomFromText(?, 4326))
            ORDER BY timestamp DESC
            """;
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(), polygonWKT);
    }
    
    @Override
    public List<TelemetryData> queryByMultipleRegions(List<BoundingBox> regions) {
        if (regions.isEmpty()) {
            return List.of();
        }
        
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE 
            """);
        
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(" OR ");
            }
            sqlBuilder.append("ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))");
            BoundingBox bbox = regions.get(i);
            params.add(bbox.minLongitude());
            params.add(bbox.minLatitude());
            params.add(bbox.maxLongitude());
            params.add(bbox.maxLatitude());
        }
        
        sqlBuilder.append(" ORDER BY timestamp DESC");
        
        return jdbcTemplate.query(sqlBuilder.toString(), new TelemetryDataRowMapper(), params.toArray());
    }
    
    @Override
    public List<TelemetryData> queryNearestNeighbors(GeoLocation point, int count) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            ORDER BY ST_Distance(location, ST_GeomFromText(?, 4326))
            LIMIT ?
            """;
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(), point.toWKT(), count);
    }
    
    @Override
    public List<TelemetryData> queryWithinDistance(GeoLocation point, double minDistanceKm, double maxDistanceKm) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_DWithin(location, ST_GeomFromText(?, 4326), ?)
              AND NOT ST_DWithin(location, ST_GeomFromText(?, 4326), ?)
            ORDER BY ST_Distance(location, ST_GeomFromText(?, 4326))
            """;
        
        double minDistanceMeters = minDistanceKm * 1000;
        double maxDistanceMeters = maxDistanceKm * 1000;
        String pointWKT = point.toWKT();
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(),
            pointWKT, maxDistanceMeters, pointWKT, minDistanceMeters, pointWKT);
    }
    
    @Override
    public List<TelemetryData> queryByBoundingBoxWithTime(BoundingBox boundingBox, Instant start, Instant end) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
              AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC
            """;
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(),
            boundingBox.minLongitude(), boundingBox.minLatitude(),
            boundingBox.maxLongitude(), boundingBox.maxLatitude(),
            start, end);
    }
    
    @Override
    public List<TelemetryData> queryByRadiusWithTime(GeoLocation center, double radiusKm, Instant start, Instant end) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_DWithin(location, ST_GeomFromText(?, 4326), ?)
              AND timestamp >= ? AND timestamp <= ?
            ORDER BY ST_Distance(location, ST_GeomFromText(?, 4326))
            """;
        
        double radiusMeters = radiusKm * 1000;
        String centerWKT = center.toWKT();
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(),
            centerWKT, radiusMeters, start, end, centerWKT);
    }
    
    @Override
    public List<TelemetryData> queryByRegionWithTime(String region, Instant start, Instant end) {
        BoundingBox regionBounds = switch (region.toLowerCase()) {
            case "stamford" -> BoundingBox.stamfordCT();
            case "north_america" -> BoundingBox.northAmerica();
            case "europe" -> BoundingBox.europe();
            default -> throw new IllegalArgumentException("Unknown region: " + region);
        };
        
        return queryByBoundingBoxWithTime(regionBounds, start, end);
    }
    
    @Override
    public List<SpatialCluster> findClusters(String organizationId, double clusterRadiusKm, int minPoints) {
        String sql = """
            WITH clustered_points AS (
                SELECT device_id, location, metrics,
                       ST_ClusterDBSCAN(location, ?, ?) OVER() AS cluster_id
                FROM spatial_telemetry
                WHERE organization_id = ?
            ),
            cluster_stats AS (
                SELECT cluster_id,
                       COUNT(*) as point_count,
                       ST_Centroid(ST_Collect(location)) as center,
                       ST_ConvexHull(ST_Collect(location)) as hull
                FROM clustered_points
                WHERE cluster_id IS NOT NULL
                GROUP BY cluster_id
                HAVING COUNT(*) >= ?
            )
            SELECT cluster_id, point_count,
                   ST_X(center) as center_lng, ST_Y(center) as center_lat,
                   ST_Area(ST_Transform(hull, 3857)) / 1000000 as area_km2
            FROM cluster_stats
            ORDER BY point_count DESC
            """;
        
        double radiusMeters = clusterRadiusKm * 1000;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            int clusterId = rs.getInt("cluster_id");
            int pointCount = rs.getInt("point_count");
            double centerLng = rs.getDouble("center_lng");
            double centerLat = rs.getDouble("center_lat");
            double areaKm2 = rs.getDouble("area_km2");
            
            GeoLocation center = new GeoLocation(centerLat, centerLng);
            double radiusKm = Math.sqrt(areaKm2 / Math.PI);
            
            return new SpatialCluster(clusterId, center, radiusKm, List.of(), pointCount, Map.of());
        }, radiusMeters, minPoints, organizationId, minPoints);
    }
    
    @Override
    public List<SpatialHotspot> findHotspots(String organizationId, String metricName, double threshold) {
        String sql = """
            WITH metric_points AS (
                SELECT location, (metrics->>?)::numeric as metric_value
                FROM spatial_telemetry
                WHERE organization_id = ?
                  AND metrics->>? IS NOT NULL
                  AND (metrics->>?)::numeric >= ?
            ),
            hotspot_areas AS (
                SELECT ST_ClusterDBSCAN(location, 1000, 3) OVER() AS cluster_id,
                       location, metric_value
                FROM metric_points
            ),
            hotspot_stats AS (
                SELECT cluster_id,
                       COUNT(*) as point_count,
                       AVG(metric_value) as avg_value,
                       MAX(metric_value) as max_value,
                       ST_Centroid(ST_Collect(location)) as center
                FROM hotspot_areas
                WHERE cluster_id IS NOT NULL
                GROUP BY cluster_id
                HAVING AVG(metric_value) >= ?
            )
            SELECT cluster_id, point_count, avg_value, max_value,
                   ST_X(center) as center_lng, ST_Y(center) as center_lat
            FROM hotspot_stats
            ORDER BY avg_value DESC
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            double avgValue = rs.getDouble("avg_value");
            double maxValue = rs.getDouble("max_value");
            int pointCount = rs.getInt("point_count");
            double centerLng = rs.getDouble("center_lng");
            double centerLat = rs.getDouble("center_lat");
            
            GeoLocation center = new GeoLocation(centerLat, centerLng);
            double significance = (avgValue - threshold) / threshold;
            
            return new SpatialHotspot(center, 1.0, metricName, avgValue, maxValue, pointCount, significance);
        }, metricName, organizationId, metricName, metricName, threshold, threshold);
    }
    
    @Override
    public SpatialDensityMap getDensityMap(BoundingBox area, int gridSize) {
        String sql = """
            WITH grid AS (
                SELECT i, j,
                       ST_MakeEnvelope(
                           ? + (? - ?) * i / ?,
                           ? + (? - ?) * j / ?,
                           ? + (? - ?) * (i + 1) / ?,
                           ? + (? - ?) * (j + 1) / ?,
                           4326
                       ) as cell
                FROM generate_series(0, ? - 1) i,
                     generate_series(0, ? - 1) j
            ),
            density AS (
                SELECT g.i, g.j, COUNT(st.location) as point_count
                FROM grid g
                LEFT JOIN spatial_telemetry st ON ST_Within(st.location, g.cell)
                GROUP BY g.i, g.j
            )
            SELECT i, j, point_count
            FROM density
            ORDER BY i, j
            """;
        
        double minLng = area.minLongitude();
        double maxLng = area.maxLongitude();
        double minLat = area.minLatitude();
        double maxLat = area.maxLatitude();
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql,
            minLng, maxLng, minLng, gridSize,
            minLat, maxLat, minLat, gridSize,
            minLng, maxLng, minLng, gridSize,
            minLat, maxLat, minLat, gridSize,
            gridSize, gridSize);
        
        double[][] densityGrid = new double[gridSize][gridSize];
        double maxDensity = 0.0;
        double minDensity = Double.MAX_VALUE;
        
        for (Map<String, Object> row : results) {
            int i = (Integer) row.get("i");
            int j = (Integer) row.get("j");
            long count = (Long) row.get("point_count");
            
            densityGrid[i][j] = count;
            maxDensity = Math.max(maxDensity, count);
            minDensity = Math.min(minDensity, count);
        }
        
        return new SpatialDensityMap(area, gridSize, densityGrid, maxDensity, minDensity);
    }
    
    @Override
    public List<SpatialCorrelation> findSpatialCorrelations(String organizationId, List<String> metrics, double radiusKm) {
        // This is a complex spatial correlation analysis
        // For brevity, returning a placeholder implementation
        return List.of();
    }
    
    @Override
    public List<GeographicFeature> queryFeatures(BoundingBox boundingBox, FeatureType featureType) {
        String sql = """
            SELECT feature_id, name, feature_type,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   ST_XMin(bounds) as min_lng, ST_YMin(bounds) as min_lat,
                   ST_XMax(bounds) as max_lng, ST_YMax(bounds) as max_lat,
                   properties
            FROM geographic_features
            WHERE feature_type = ?
              AND (location IS NULL OR ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326)))
              AND (bounds IS NULL OR ST_Intersects(bounds, ST_MakeEnvelope(?, ?, ?, ?, 4326)))
            """;
        
        return jdbcTemplate.query(sql, new GeographicFeatureRowMapper(),
            featureType.name(), 
            boundingBox.minLongitude(), boundingBox.minLatitude(),
            boundingBox.maxLongitude(), boundingBox.maxLatitude(),
            boundingBox.minLongitude(), boundingBox.minLatitude(),
            boundingBox.maxLongitude(), boundingBox.maxLatitude());
    }
    
    @Override
    public List<GeographicFeature> queryFeaturesNear(GeoLocation point, double radiusKm, FeatureType featureType) {
        String sql = """
            SELECT feature_id, name, feature_type,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   ST_XMin(bounds) as min_lng, ST_YMin(bounds) as min_lat,
                   ST_XMax(bounds) as max_lng, ST_YMax(bounds) as max_lat,
                   properties
            FROM geographic_features
            WHERE feature_type = ?
              AND (location IS NULL OR ST_DWithin(location, ST_GeomFromText(?, 4326), ?))
            ORDER BY ST_Distance(location, ST_GeomFromText(?, 4326))
            """;
        
        double radiusMeters = radiusKm * 1000;
        String pointWKT = point.toWKT();
        
        return jdbcTemplate.query(sql, new GeographicFeatureRowMapper(),
            featureType.name(), pointWKT, radiusMeters, pointWKT);
    }
    
    @Override
    public List<String> getRegionNames(BoundingBox boundingBox) {
        String sql = """
            SELECT DISTINCT name
            FROM geographic_features
            WHERE feature_type IN ('REGION', 'ADMINISTRATIVE_BOUNDARY')
              AND ST_Intersects(bounds, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            ORDER BY name
            """;
        
        return jdbcTemplate.queryForList(sql, String.class,
            boundingBox.minLongitude(), boundingBox.minLatitude(),
            boundingBox.maxLongitude(), boundingBox.maxLatitude());
    }
    
    @Override
    public List<ProximityResult> findNearbyDevices(DeviceId deviceId, double radiusKm) {
        String sql = """
            WITH device_location AS (
                SELECT location
                FROM spatial_telemetry
                WHERE device_id = ?
                ORDER BY timestamp DESC
                LIMIT 1
            )
            SELECT DISTINCT st.device_id,
                   ST_X(st.location) as longitude, ST_Y(st.location) as latitude,
                   ST_Distance(st.location, dl.location) / 1000 as distance_km,
                   MAX(st.timestamp) as last_seen
            FROM spatial_telemetry st, device_location dl
            WHERE st.device_id != ?
              AND ST_DWithin(st.location, dl.location, ?)
            GROUP BY st.device_id, st.location, dl.location
            ORDER BY distance_km
            """;
        
        double radiusMeters = radiusKm * 1000;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String nearbyDeviceId = rs.getString("device_id");
            double lng = rs.getDouble("longitude");
            double lat = rs.getDouble("latitude");
            double distance = rs.getDouble("distance_km");
            Instant lastSeen = rs.getTimestamp("last_seen").toInstant();
            
            return new ProximityResult(
                new DeviceId(nearbyDeviceId),
                new GeoLocation(lat, lng),
                distance,
                lastSeen,
                Map.of()
            );
        }, deviceId.getValue(), deviceId.getValue(), radiusMeters);
    }
    
    @Override
    public Map<DeviceId, List<DeviceId>> findDeviceNeighbors(String organizationId, double radiusKm) {
        // This would require a complex self-join query
        // For brevity, returning a placeholder implementation
        return Map.of();
    }
    
    @Override
    public List<TelemetryData> findDataPointsNearPath(List<GeoLocation> path, double corridorWidthKm) {
        if (path.size() < 2) {
            return List.of();
        }
        
        String pathWKT = createLineStringWKT(path);
        double corridorWidthMeters = corridorWidthKm * 1000;
        
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(location) as longitude, ST_Y(location) as latitude,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_DWithin(location, ST_GeomFromText(?, 4326), ?)
            ORDER BY ST_Distance(location, ST_GeomFromText(?, 4326))
            """;
        
        return jdbcTemplate.query(sql, new TelemetryDataRowMapper(),
            pathWKT, corridorWidthMeters, pathWKT);
    }
    
    @Override
    public List<TelemetryData> queryWithProjection(BoundingBox boundingBox, CoordinateSystem targetSystem) {
        String sql = """
            SELECT telemetry_id, device_id, organization_id,
                   ST_X(ST_Transform(location, ?)) as x, ST_Y(ST_Transform(location, ?)) as y,
                   timestamp, metrics
            FROM spatial_telemetry
            WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            ORDER BY timestamp DESC
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            // This would need custom handling for projected coordinates
            // For now, returning standard lat/lng
            return mapRowToTelemetryData(rs);
        }, targetSystem.getEpsgCode(), targetSystem.getEpsgCode(),
           boundingBox.minLongitude(), boundingBox.minLatitude(),
           boundingBox.maxLongitude(), boundingBox.maxLatitude());
    }
    
    @Override
    public GeoLocation transformCoordinates(GeoLocation location, CoordinateSystem from, CoordinateSystem to) {
        String sql = """
            SELECT ST_X(ST_Transform(ST_GeomFromText(?, ?), ?)) as x,
                   ST_Y(ST_Transform(ST_GeomFromText(?, ?), ?)) as y
            """;
        
        String wkt = location.toWKT();
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> 
            new GeoLocation(rs.getDouble("y"), rs.getDouble("x")),
            wkt, from.getEpsgCode(), to.getEpsgCode(),
            wkt, from.getEpsgCode(), to.getEpsgCode());
    }
    
    @Override
    public BoundingBox transformBoundingBox(BoundingBox bbox, CoordinateSystem from, CoordinateSystem to) {
        GeoLocation sw = transformCoordinates(
            new GeoLocation(bbox.minLatitude(), bbox.minLongitude()), from, to);
        GeoLocation ne = transformCoordinates(
            new GeoLocation(bbox.maxLatitude(), bbox.maxLongitude()), from, to);
        
        return BoundingBox.of(sw.latitude(), ne.latitude(), sw.longitude(), ne.longitude());
    }
    
    @Override
    public SpatialStatistics getSpatialStatistics(String organizationId, BoundingBox area) {
        String sql = """
            SELECT COUNT(*) as total_points,
                   AVG(ST_Y(location)) as avg_lat,
                   AVG(ST_X(location)) as avg_lng,
                   ST_X(ST_Centroid(ST_Collect(location))) as centroid_lng,
                   ST_Y(ST_Centroid(ST_Collect(location))) as centroid_lat,
                   ST_XMin(ST_Extent(location)) as min_lng,
                   ST_YMin(ST_Extent(location)) as min_lat,
                   ST_XMax(ST_Extent(location)) as max_lng,
                   ST_YMax(ST_Extent(location)) as max_lat
            FROM spatial_telemetry
            WHERE organization_id = ?
              AND ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalPoints = rs.getLong("total_points");
            double avgLat = rs.getDouble("avg_lat");
            double avgLng = rs.getDouble("avg_lng");
            double centroidLng = rs.getDouble("centroid_lng");
            double centroidLat = rs.getDouble("centroid_lat");
            double minLng = rs.getDouble("min_lng");
            double minLat = rs.getDouble("min_lat");
            double maxLng = rs.getDouble("max_lng");
            double maxLat = rs.getDouble("max_lat");
            
            GeoLocation centroid = new GeoLocation(centroidLat, centroidLng);
            BoundingBox bounds = BoundingBox.of(minLat, maxLat, minLng, maxLng);
            
            return new SpatialStatistics(
                totalPoints, avgLat, avgLng, centroid, bounds,
                0.0, 0.0, Map.of()
            );
        }, organizationId, area.minLongitude(), area.minLatitude(),
           area.maxLongitude(), area.maxLatitude());
    }
    
    @Override
    public Map<String, Double> calculateDistanceMatrix(List<GeoLocation> locations) {
        Map<String, Double> distanceMatrix = new HashMap<>();
        
        for (int i = 0; i < locations.size(); i++) {
            for (int j = i + 1; j < locations.size(); j++) {
                GeoLocation loc1 = locations.get(i);
                GeoLocation loc2 = locations.get(j);
                double distance = loc1.distanceToKm(loc2);
                
                String key = i + "-" + j;
                distanceMatrix.put(key, distance);
            }
        }
        
        return distanceMatrix;
    }
    
    @Override
    public double calculateArea(List<GeoLocation> polygonVertices) {
        if (polygonVertices.size() < 3) {
            return 0.0;
        }
        
        String polygonWKT = createPolygonWKT(polygonVertices);
        
        String sql = "SELECT ST_Area(ST_Transform(ST_GeomFromText(?, 4326), 3857)) / 1000000 as area_km2";
        
        return jdbcTemplate.queryForObject(sql, Double.class, polygonWKT);
    }
    
    @Override
    public GeoLocation calculateCentroid(List<GeoLocation> locations) {
        if (locations.isEmpty()) {
            return new GeoLocation(0.0, 0.0);
        }
        
        String pointsWKT = locations.stream()
            .map(GeoLocation::toWKT)
            .collect(Collectors.joining(",", "MULTIPOINT(", ")"));
        
        String sql = """
            SELECT ST_X(ST_Centroid(ST_GeomFromText(?, 4326))) as lng,
                   ST_Y(ST_Centroid(ST_GeomFromText(?, 4326))) as lat
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
            new GeoLocation(rs.getDouble("lat"), rs.getDouble("lng")),
            pointsWKT, pointsWKT);
    }
    
    @Override
    public void deleteSpatialData(BoundingBox area) {
        String sql = """
            DELETE FROM spatial_telemetry
            WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            """;
        
        jdbcTemplate.update(sql,
            area.minLongitude(), area.minLatitude(),
            area.maxLongitude(), area.maxLatitude());
    }
    
    @Override
    public void deleteSpatialDataByDevice(DeviceId deviceId) {
        String sql = "DELETE FROM spatial_telemetry WHERE device_id = ?";
        jdbcTemplate.update(sql, deviceId.getValue());
    }
    
    @Override
    public long countSpatialData(BoundingBox area) {
        String sql = """
            SELECT COUNT(*)
            FROM spatial_telemetry
            WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            """;
        
        return jdbcTemplate.queryForObject(sql, Long.class,
            area.minLongitude(), area.minLatitude(),
            area.maxLongitude(), area.maxLatitude());
    }
    
    // Helper methods
    private String convertMetricsToJson(Map<String, MetricValue> metrics) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, MetricValue> entry : metrics.entrySet()) {
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            MetricValue value = entry.getValue();
            
            if (value.isNumeric()) {
                json.append(value.getNumericValue());
            } else if (value.isString()) {
                json.append("\"").append(value.getStringValue()).append("\"");
            } else if (value.isBoolean()) {
                json.append(value.getBooleanValue());
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    private String createPolygonWKT(List<GeoLocation> vertices) {
        StringBuilder wkt = new StringBuilder("POLYGON((");
        
        for (int i = 0; i < vertices.size(); i++) {
            if (i > 0) wkt.append(",");
            GeoLocation vertex = vertices.get(i);
            wkt.append(vertex.longitude()).append(" ").append(vertex.latitude());
        }
        
        // Close the polygon by repeating the first vertex
        GeoLocation first = vertices.get(0);
        wkt.append(",").append(first.longitude()).append(" ").append(first.latitude());
        wkt.append("))");
        
        return wkt.toString();
    }
    
    private String createLineStringWKT(List<GeoLocation> points) {
        StringBuilder wkt = new StringBuilder("LINESTRING(");
        
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) wkt.append(",");
            GeoLocation point = points.get(i);
            wkt.append(point.longitude()).append(" ").append(point.latitude());
        }
        
        wkt.append(")");
        return wkt.toString();
    }
    
    private TelemetryData mapRowToTelemetryData(ResultSet rs) throws SQLException {
        String telemetryId = rs.getString("telemetry_id");
        String deviceId = rs.getString("device_id");
        String organizationId = rs.getString("organization_id");
        double longitude = rs.getDouble("longitude");
        double latitude = rs.getDouble("latitude");
        Instant timestamp = rs.getTimestamp("timestamp").toInstant();
        String metricsJson = rs.getString("metrics");
        
        GeoLocation location = new GeoLocation(latitude, longitude);
        Map<String, MetricValue> metrics = parseMetricsFromJson(metricsJson);
        
        return new TelemetryData(
            new TelemetryId(telemetryId),
            new DeviceId(deviceId),
            timestamp,
            metrics,
            location,
            organizationId
        );
    }
    
    private Map<String, MetricValue> parseMetricsFromJson(String json) {
        // Simple JSON parsing - in production, use a proper JSON library
        Map<String, MetricValue> metrics = new HashMap<>();
        
        if (json != null && !json.isEmpty()) {
            // This is a simplified parser - use Jackson or similar in production
            json = json.substring(1, json.length() - 1); // Remove { }
            String[] pairs = json.split(",");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim();
                    
                    if (value.startsWith("\"")) {
                        metrics.put(key, MetricValue.string(value.replaceAll("\"", "")));
                    } else if ("true".equals(value) || "false".equals(value)) {
                        metrics.put(key, MetricValue.bool(Boolean.parseBoolean(value)));
                    } else {
                        try {
                            metrics.put(key, MetricValue.numeric(Double.parseDouble(value)));
                        } catch (NumberFormatException e) {
                            metrics.put(key, MetricValue.string(value));
                        }
                    }
                }
            }
        }
        
        return metrics;
    }
    
    // Row mappers
    private static class TelemetryDataRowMapper implements RowMapper<TelemetryData> {
        @Override
        public TelemetryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            String telemetryId = rs.getString("telemetry_id");
            String deviceId = rs.getString("device_id");
            String organizationId = rs.getString("organization_id");
            double longitude = rs.getDouble("longitude");
            double latitude = rs.getDouble("latitude");
            Instant timestamp = rs.getTimestamp("timestamp").toInstant();
            String metricsJson = rs.getString("metrics");
            
            GeoLocation location = new GeoLocation(latitude, longitude);
            Map<String, MetricValue> metrics = parseMetricsFromJsonStatic(metricsJson);
            
            return new TelemetryData(
                new TelemetryId(telemetryId),
                new DeviceId(deviceId),
                timestamp,
                metrics,
                location,
                organizationId
            );
        }
        
        private static Map<String, MetricValue> parseMetricsFromJsonStatic(String json) {
            Map<String, MetricValue> metrics = new HashMap<>();
            
            if (json != null && !json.isEmpty()) {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("\"", "");
                        String value = keyValue[1].trim();
                        
                        if (value.startsWith("\"")) {
                            metrics.put(key, MetricValue.string(value.replaceAll("\"", "")));
                        } else if ("true".equals(value) || "false".equals(value)) {
                            metrics.put(key, MetricValue.bool(Boolean.parseBoolean(value)));
                        } else {
                            try {
                                metrics.put(key, MetricValue.numeric(Double.parseDouble(value)));
                            } catch (NumberFormatException e) {
                                metrics.put(key, MetricValue.string(value));
                            }
                        }
                    }
                }
            }
            
            return metrics;
        }
    }
    
    private static class GeographicFeatureRowMapper implements RowMapper<GeographicFeature> {
        @Override
        public GeographicFeature mapRow(ResultSet rs, int rowNum) throws SQLException {
            String featureId = rs.getString("feature_id");
            String name = rs.getString("name");
            String featureType = rs.getString("feature_type");
            
            GeoLocation location = null;
            double lng = rs.getDouble("longitude");
            double lat = rs.getDouble("latitude");
            if (!rs.wasNull()) {
                location = new GeoLocation(lat, lng);
            }
            
            BoundingBox bounds = null;
            double minLng = rs.getDouble("min_lng");
            double minLat = rs.getDouble("min_lat");
            double maxLng = rs.getDouble("max_lng");
            double maxLat = rs.getDouble("max_lat");
            if (!rs.wasNull()) {
                bounds = BoundingBox.of(minLat, maxLat, minLng, maxLng);
            }
            
            String propertiesJson = rs.getString("properties");
            Map<String, Object> properties = parsePropertiesFromJson(propertiesJson);
            
            return new GeographicFeature(
                featureId, name, FeatureType.valueOf(featureType),
                location, bounds, properties
            );
        }
        
        private Map<String, Object> parsePropertiesFromJson(String json) {
            // Simplified JSON parsing
            return json != null ? Map.of() : Map.of();
        }
    }
}