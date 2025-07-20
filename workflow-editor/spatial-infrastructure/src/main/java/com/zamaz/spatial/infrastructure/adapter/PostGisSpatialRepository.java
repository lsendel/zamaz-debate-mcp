package com.zamaz.spatial.infrastructure.adapter;

import com.zamaz.spatial.infrastructure.entity.SpatialTelemetryEntity;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.query.TelemetryQuery;
import com.zamaz.telemetry.domain.repository.TelemetryRepository;
import com.zamaz.telemetry.domain.service.TelemetryDomainServiceImpl.TimeRange;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import com.zamaz.telemetry.domain.valueobject.TelemetryId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Repository("postGisSpatialRepository")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostGisSpatialRepository implements TelemetryRepository {
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    
    @Override
    public void saveTimeSeries(TelemetryData data) {
        // PostGIS implementation focuses on spatial data
        // Time series data would be handled by InfluxDB
        log.debug("PostGIS repository handles spatial data only");
    }
    
    @Override
    public void saveSpatialData(TelemetryData data) {
        if (!data.hasLocation()) {
            log.warn("Attempting to save telemetry data without location to spatial repository");
            return;
        }
        
        GeoLocation location = data.getLocation();
        Point point = geometryFactory.createPoint(
            new Coordinate(location.getLongitude(), location.getLatitude())
        );
        
        SpatialTelemetryEntity entity = SpatialTelemetryEntity.builder()
                .telemetryId(data.getId().getValue())
                .deviceId(data.getDeviceId().getValue())
                .location(point)
                .timestamp(data.getTimestamp())
                .data(data.getMetrics())
                .qualityScore(data.getQualityScore())
                .dataSource(data.getDataSource())
                .build();
        
        entityManager.persist(entity);
        log.debug("Saved spatial telemetry for device: {} at location: {}", 
                data.getDeviceId(), location);
    }
    
    @Override
    public Stream<TelemetryData> queryTimeSeries(TimeRange range) {
        // PostGIS implementation would delegate time series queries to InfluxDB
        throw new UnsupportedOperationException("Time series queries should use InfluxDB repository");
    }
    
    @Override
    public List<TelemetryData> querySpatial(TelemetryQuery.GeoQuery query) {
        String jpql;
        Map<String, Object> params = new HashMap<>();
        
        if (query.getBoundingBox() != null) {
            TelemetryQuery.BoundingBox box = query.getBoundingBox();
            jpql = """
                SELECT s FROM SpatialTelemetryEntity s
                WHERE within(s.location, :polygon) = true
                ORDER BY s.timestamp DESC
                """;
            
            // Create bounding box polygon
            Coordinate[] coords = new Coordinate[] {
                new Coordinate(box.getWest(), box.getSouth()),
                new Coordinate(box.getEast(), box.getSouth()),
                new Coordinate(box.getEast(), box.getNorth()),
                new Coordinate(box.getWest(), box.getNorth()),
                new Coordinate(box.getWest(), box.getSouth())
            };
            params.put("polygon", geometryFactory.createPolygon(coords));
            
        } else if (query.getCenter() != null && query.getRadiusKm() > 0) {
            jpql = """
                SELECT s FROM SpatialTelemetryEntity s
                WHERE distance(s.location, :center) <= :radius
                ORDER BY s.timestamp DESC
                """;
            
            GeoLocation center = query.getCenter();
            Point centerPoint = geometryFactory.createPoint(
                new Coordinate(center.getLongitude(), center.getLatitude())
            );
            params.put("center", centerPoint);
            params.put("radius", query.getRadiusKm() * 1000); // Convert to meters
            
        } else {
            jpql = "SELECT s FROM SpatialTelemetryEntity s ORDER BY s.timestamp DESC";
        }
        
        TypedQuery<SpatialTelemetryEntity> typedQuery = entityManager.createQuery(jpql, SpatialTelemetryEntity.class);
        params.forEach(typedQuery::setParameter);
        typedQuery.setMaxResults(1000); // Limit results
        
        return typedQuery.getResultList().stream()
                .map(this::entityToTelemetryData)
                .toList();
    }
    
    @Override
    public void deleteOldData(TimeRange range) {
        String sql = """
            DELETE FROM spatial_telemetry
            WHERE timestamp >= ? AND timestamp <= ?
            """;
        
        int deleted = jdbcTemplate.update(sql, range.getStart(), range.getEnd());
        log.info("Deleted {} spatial telemetry records from {} to {}", 
                deleted, range.getStart(), range.getEnd());
    }
    
    @Override
    public long count(TimeRange range) {
        String jpql = """
            SELECT COUNT(s) FROM SpatialTelemetryEntity s
            WHERE s.timestamp >= :start AND s.timestamp <= :end
            """;
        
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("start", range.getStart());
        query.setParameter("end", range.getEnd());
        
        return query.getSingleResult();
    }
    
    // Additional spatial-specific queries
    
    public List<TelemetryData> findNearestNeighbors(GeoLocation location, int count) {
        String sql = """
            SELECT *, ST_Distance(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)) as distance
            FROM spatial_telemetry
            ORDER BY location <-> ST_SetSRID(ST_MakePoint(?, ?), 4326)
            LIMIT ?
            """;
        
        return jdbcTemplate.query(sql, 
            new Object[]{location.getLongitude(), location.getLatitude(), 
                        location.getLongitude(), location.getLatitude(), count},
            (rs, rowNum) -> {
                // Map ResultSet to TelemetryData
                // Implementation would go here
                return null;
            });
    }
    
    public Map<String, Long> getDeviceCountByRegion(List<TelemetryQuery.BoundingBox> regions) {
        Map<String, Long> counts = new HashMap<>();
        
        for (int i = 0; i < regions.size(); i++) {
            TelemetryQuery.BoundingBox region = regions.get(i);
            String sql = """
                SELECT COUNT(DISTINCT device_id)
                FROM spatial_telemetry
                WHERE ST_Within(location, ST_MakeEnvelope(?, ?, ?, ?, 4326))
                """;
            
            Long count = jdbcTemplate.queryForObject(sql, Long.class,
                region.getWest(), region.getSouth(), region.getEast(), region.getNorth());
            
            counts.put("Region_" + i, count);
        }
        
        return counts;
    }
    
    private TelemetryData entityToTelemetryData(SpatialTelemetryEntity entity) {
        Point point = entity.getLocation();
        GeoLocation location = GeoLocation.of(point.getY(), point.getX());
        
        return TelemetryData.builder()
                .id(TelemetryId.of(entity.getTelemetryId()))
                .deviceId(DeviceId.of(entity.getDeviceId()))
                .timestamp(entity.getTimestamp())
                .metrics(entity.getData())
                .location(location)
                .qualityScore(entity.getQualityScore() != null ? entity.getQualityScore() : 100)
                .dataSource(entity.getDataSource())
                .build();
    }
}