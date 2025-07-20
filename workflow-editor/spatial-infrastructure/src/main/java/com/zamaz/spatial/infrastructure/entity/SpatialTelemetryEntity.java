package com.zamaz.spatial.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "spatial_telemetry", 
       indexes = {
           @Index(name = "idx_spatial_telemetry_device", columnList = "device_id"),
           @Index(name = "idx_spatial_telemetry_timestamp", columnList = "timestamp"),
           @Index(name = "idx_spatial_telemetry_location", columnList = "location")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpatialTelemetryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "telemetry_id", nullable = false, unique = true)
    private String telemetryId;
    
    @Column(name = "device_id", nullable = false)
    private String deviceId;
    
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    
    @Column(name = "quality_score")
    private Integer qualityScore;
    
    @Column(name = "data_source")
    private String dataSource;
}