package com.zamaz.telemetry.infrastructure.adapter;

import com.influxdb.client.*;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.query.TelemetryQuery;
import com.zamaz.telemetry.domain.repository.TelemetryRepository;
import com.zamaz.telemetry.domain.service.TelemetryDomainServiceImpl.TimeRange;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import com.zamaz.telemetry.domain.valueobject.TelemetryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InfluxDbTelemetryRepository implements TelemetryRepository {
    private final InfluxDBClient influxDBClient;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.org:workflow-editor}")
    private String organization;
    
    @Value("${telemetry.batch.size:100}")
    private int batchSize;
    
    @Value("${telemetry.batch.interval.ms:100}")
    private long batchIntervalMs;
    
    private final Queue<Point> writeBuffer = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    
    {
        // Start batch writer
        executorService.scheduleWithFixedDelay(this::flushBatch, batchIntervalMs, batchIntervalMs, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void saveTimeSeries(TelemetryData data) {
        Point point = Point.measurement("telemetry")
                .addTag("device_id", data.getDeviceId().getValue())
                .addTag("data_source", data.getDataSource() != null ? data.getDataSource() : "unknown")
                .addField("telemetry_id", data.getId().getValue())
                .addField("quality_score", data.getQualityScore())
                .time(data.getTimestamp(), WritePrecision.MS);
        
        // Add location if present
        if (data.hasLocation()) {
            GeoLocation location = data.getLocation();
            point.addField("latitude", location.getLatitude())
                 .addField("longitude", location.getLongitude());
            if (location.getAltitude() != null) {
                point.addField("altitude", location.getAltitude());
            }
        }
        
        // Add all metrics
        data.getMetrics().forEach((key, value) -> {
            if (value instanceof Number) {
                point.addField(key, ((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                point.addField(key, (Boolean) value);
            } else if (value != null) {
                point.addField(key, value.toString());
            }
        });
        
        writeBuffer.offer(point);
        
        // Flush if buffer exceeds size
        if (writeBuffer.size() >= batchSize) {
            flushBatch();
        }
    }
    
    @Override
    public void saveSpatialData(TelemetryData data) {
        // Spatial data is included in the time series data
        // Additional spatial indexing could be implemented here if needed
        log.debug("Spatial data saved as part of time series for device: {}", data.getDeviceId());
    }
    
    @Override
    public Stream<TelemetryData> queryTimeSeries(TimeRange range) {
        String flux = String.format("""
            from(bucket: "%s")
                |> range(start: %s, stop: %s)
                |> filter(fn: (r) => r._measurement == "telemetry")
                |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                |> sort(columns: ["_time"])
            """, bucket, range.getStart().toString(), range.getEnd().toString());
        
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, organization);
        
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::recordToTelemetryData)
                .filter(Objects::nonNull);
    }
    
    @Override
    public List<TelemetryData> querySpatial(TelemetryQuery.GeoQuery query) {
        String flux;
        
        if (query.getBoundingBox() != null) {
            TelemetryQuery.BoundingBox box = query.getBoundingBox();
            flux = String.format("""
                from(bucket: "%s")
                    |> range(start: -1h)
                    |> filter(fn: (r) => r._measurement == "telemetry")
                    |> filter(fn: (r) => r._field == "latitude" or r._field == "longitude")
                    |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                    |> filter(fn: (r) => r.latitude >= %f and r.latitude <= %f and r.longitude >= %f and r.longitude <= %f)
                    |> limit(n: 1000)
                """, bucket, box.getSouth(), box.getNorth(), box.getWest(), box.getEast());
        } else if (query.getCenter() != null && query.getRadiusKm() > 0) {
            // For radius queries, we'd need to implement haversine distance in Flux
            // For now, using a simple bounding box approximation
            double lat = query.getCenter().getLatitude();
            double lon = query.getCenter().getLongitude();
            double latDelta = query.getRadiusKm() / 111.0; // Rough approximation
            double lonDelta = query.getRadiusKm() / (111.0 * Math.cos(Math.toRadians(lat)));
            
            flux = String.format("""
                from(bucket: "%s")
                    |> range(start: -1h)
                    |> filter(fn: (r) => r._measurement == "telemetry")
                    |> filter(fn: (r) => r._field == "latitude" or r._field == "longitude")
                    |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                    |> filter(fn: (r) => r.latitude >= %f and r.latitude <= %f and r.longitude >= %f and r.longitude <= %f)
                    |> limit(n: 1000)
                """, bucket, lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta);
        } else {
            return new ArrayList<>();
        }
        
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, organization);
        
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::recordToTelemetryData)
                .filter(Objects::nonNull)
                .filter(data -> {
                    // Fine-grained filtering for radius queries
                    if (query.getCenter() != null && query.getRadiusKm() > 0 && data.hasLocation()) {
                        double distance = data.getLocation().distanceTo(query.getCenter());
                        return distance <= query.getRadiusKm();
                    }
                    return true;
                })
                .toList();
    }
    
    @Override
    public void deleteOldData(TimeRange range) {
        DeleteApi deleteApi = influxDBClient.getDeleteApi();
        deleteApi.delete(range.getStart(), range.getEnd(), "", bucket, organization);
        log.info("Deleted telemetry data from {} to {}", range.getStart(), range.getEnd());
    }
    
    @Override
    public long count(TimeRange range) {
        String flux = String.format("""
            from(bucket: "%s")
                |> range(start: %s, stop: %s)
                |> filter(fn: (r) => r._measurement == "telemetry")
                |> group()
                |> count()
            """, bucket, range.getStart().toString(), range.getEnd().toString());
        
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, organization);
        
        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            Object value = tables.get(0).getRecords().get(0).getValue();
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        }
        
        return 0L;
    }
    
    private void flushBatch() {
        if (writeBuffer.isEmpty()) {
            return;
        }
        
        List<Point> points = new ArrayList<>();
        Point point;
        while ((point = writeBuffer.poll()) != null && points.size() < batchSize) {
            points.add(point);
        }
        
        if (!points.isEmpty()) {
            try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
                writeApi.writePoints(bucket, organization, points);
                log.debug("Flushed {} telemetry points to InfluxDB", points.size());
            } catch (Exception e) {
                log.error("Failed to write telemetry batch to InfluxDB", e);
                // Re-queue failed points
                points.forEach(writeBuffer::offer);
            }
        }
    }
    
    private TelemetryData recordToTelemetryData(FluxRecord record) {
        try {
            String deviceId = (String) record.getValueByKey("device_id");
            if (deviceId == null) {
                return null;
            }
            
            Map<String, Object> metrics = new HashMap<>();
            record.getValues().forEach((key, value) -> {
                if (!key.startsWith("_") && !key.equals("device_id") && 
                    !key.equals("telemetry_id") && !key.equals("data_source") &&
                    !key.equals("latitude") && !key.equals("longitude") && 
                    !key.equals("altitude") && !key.equals("quality_score")) {
                    metrics.put(key, value);
                }
            });
            
            TelemetryData.TelemetryDataBuilder builder = TelemetryData.builder()
                    .id(TelemetryId.of((String) record.getValueByKey("telemetry_id")))
                    .deviceId(DeviceId.of(deviceId))
                    .timestamp(record.getTime())
                    .metrics(metrics)
                    .dataSource((String) record.getValueByKey("data_source"));
            
            // Add quality score if present
            Object qualityScore = record.getValueByKey("quality_score");
            if (qualityScore instanceof Number) {
                builder.qualityScore(((Number) qualityScore).intValue());
            }
            
            // Add location if present
            Object latitude = record.getValueByKey("latitude");
            Object longitude = record.getValueByKey("longitude");
            if (latitude instanceof Number && longitude instanceof Number) {
                Object altitude = record.getValueByKey("altitude");
                if (altitude instanceof Number) {
                    builder.location(GeoLocation.of(
                            ((Number) latitude).doubleValue(),
                            ((Number) longitude).doubleValue(),
                            ((Number) altitude).doubleValue()
                    ));
                } else {
                    builder.location(GeoLocation.of(
                            ((Number) latitude).doubleValue(),
                            ((Number) longitude).doubleValue()
                    ));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to convert record to TelemetryData", e);
            return null;
        }
    }
    
    public void shutdown() {
        flushBatch();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}