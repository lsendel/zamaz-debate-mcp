package com.example.workflow.infrastructure;

import com.example.workflow.domain.*;
import com.example.workflow.domain.ports.TelemetryRepository;
import com.influxdb.client.*;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * InfluxDB implementation of TelemetryRepository
 * Provides high-performance time-series storage with batch operations and retention policies
 */
@Repository
public class InfluxDbTelemetryRepository implements TelemetryRepository {
    
    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final WriteApi writeApiAsync;
    private final QueryApi queryApi;
    
    @Value("${influxdb.bucket:telemetry}")
    private String bucket;
    
    @Value("${influxdb.organization:workflow-org}")
    private String organization;
    
    @Value("${influxdb.batch.size:1000}")
    private int batchSize;
    
    @Value("${influxdb.flush.interval:1000}")
    private int flushIntervalMs;
    
    // Batch processing
    private final Map<String, List<TelemetryData>> batchBuffer = new ConcurrentHashMap<>();
    private final Object batchLock = new Object();
    
    @Autowired
    public InfluxDbTelemetryRepository(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
        this.writeApi = influxDBClient.getWriteApiBlocking();
        this.writeApiAsync = influxDBClient.makeWriteApi();
        this.queryApi = influxDBClient.getQueryApi();
        
        // Configure batch writing
        configureAsyncWriteApi();
    }
    
    private void configureAsyncWriteApi() {
        writeApiAsync.listenEvents(WriteSuccessEvent.class, event -> {
            // Handle successful writes
        });
        
        writeApiAsync.listenEvents(WriteErrorEvent.class, event -> {
            // Handle write errors
            System.err.println("InfluxDB write error: " + event.getThrowable().getMessage());
        });
    }
    
    @Override
    public void saveTimeSeries(TelemetryData data) {
        Point point = createTimeSeriesPoint(data);
        writeApi.writePoint(bucket, organization, point);
    }
    
    @Override
    public void saveSpatialData(TelemetryData data) {
        if (data.hasSpatialData()) {
            Point point = createSpatialPoint(data);
            writeApi.writePoint(bucket, organization, point);
        }
    }
    
    @Override
    public void saveBatch(List<TelemetryData> dataList) {
        List<Point> points = dataList.stream()
            .map(this::createTimeSeriesPoint)
            .collect(Collectors.toList());
        
        writeApi.writePoints(bucket, organization, points);
        
        // Also save spatial data for items that have location
        List<Point> spatialPoints = dataList.stream()
            .filter(TelemetryData::hasSpatialData)
            .map(this::createSpatialPoint)
            .collect(Collectors.toList());
        
        if (!spatialPoints.isEmpty()) {
            writeApi.writePoints(bucket, organization, spatialPoints);
        }
    }
    
    @Override
    public Stream<TelemetryData> queryTimeSeries(String deviceId, Instant start, Instant end) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, start, end, deviceId
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables);
    }
    
    @Override
    public Stream<TelemetryData> queryTimeSeries(List<DeviceId> deviceIds, Instant start, Instant end) {
        String deviceFilter = deviceIds.stream()
            .map(id -> "\"" + id.getValue() + "\"")
            .collect(Collectors.joining(", "));
        
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => contains(value: r.device_id, set: [%s])) " +
            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, start, end, deviceFilter
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables);
    }
    
    @Override
    public Stream<TelemetryData> queryTimeSeriesWithMetrics(String deviceId, Instant start, Instant end, List<String> metrics) {
        String metricFilter = metrics.stream()
            .map(metric -> "\"" + metric + "\"")
            .collect(Collectors.joining(", "));
        
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> filter(fn: (r) => contains(value: r._field, set: [%s])) " +
            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, start, end, deviceId, metricFilter
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables);
    }
    
    @Override
    public Stream<TelemetryData> queryRecentData(String organizationId, Duration duration) {
        Instant start = Instant.now().minus(duration);
        
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.organization_id == \"%s\") " +
            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, start, organizationId
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables);
    }
    
    @Override
    public Stream<TelemetryData> queryRealTimeData(String organizationId) {
        return queryRecentData(organizationId, Duration.ofSeconds(10));
    }
    
    @Override
    public List<TelemetryData> querySpatial(BoundingBox boundingBox) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r._measurement == \"spatial_telemetry\") " +
            "|> filter(fn: (r) => r.latitude >= %f and r.latitude <= %f) " +
            "|> filter(fn: (r) => r.longitude >= %f and r.longitude <= %f) " +
            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, boundingBox.minLatitude(), boundingBox.maxLatitude(),
            boundingBox.minLongitude(), boundingBox.maxLongitude()
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables).collect(Collectors.toList());
    }
    
    @Override
    public List<TelemetryData> querySpatialWithTime(BoundingBox boundingBox, Instant start, Instant end) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"spatial_telemetry\") " +
            "|> filter(fn: (r) => r.latitude >= %f and r.latitude <= %f) " +
            "|> filter(fn: (r) => r.longitude >= %f and r.longitude <= %f) " +
            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, start, end, boundingBox.minLatitude(), boundingBox.maxLatitude(),
            boundingBox.minLongitude(), boundingBox.maxLongitude()
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables).collect(Collectors.toList());
    }
    
    @Override
    public List<TelemetryData> queryByRadius(GeoLocation center, double radiusKm) {
        // Using bounding box approximation for InfluxDB
        double latDelta = radiusKm / 111.0; // Approximate degrees per km
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(center.latitude())));
        
        BoundingBox bbox = BoundingBox.of(
            center.latitude() - latDelta,
            center.latitude() + latDelta,
            center.longitude() - lngDelta,
            center.longitude() + lngDelta
        );
        
        return querySpatial(bbox).stream()
            .filter(data -> data.getLocation() != null && 
                           data.getLocation().distanceToKm(center) <= radiusKm)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TelemetryData> queryByRadiusWithTime(GeoLocation center, double radiusKm, Instant start, Instant end) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(center.latitude())));
        
        BoundingBox bbox = BoundingBox.of(
            center.latitude() - latDelta,
            center.latitude() + latDelta,
            center.longitude() - lngDelta,
            center.longitude() + lngDelta
        );
        
        return querySpatialWithTime(bbox, start, end).stream()
            .filter(data -> data.getLocation() != null && 
                           data.getLocation().distanceToKm(center) <= radiusKm)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TelemetryData> queryByMetric(String metricName, Object value) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r._field == \"%s\") " +
            "|> filter(fn: (r) => r._value == %s)",
            bucket, metricName, formatValue(value)
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables).collect(Collectors.toList());
    }
    
    @Override
    public List<TelemetryData> queryByMetricRange(String metricName, double minValue, double maxValue) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r._field == \"%s\") " +
            "|> filter(fn: (r) => r._value >= %f and r._value <= %f)",
            bucket, metricName, minValue, maxValue
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables).collect(Collectors.toList());
    }
    
    @Override
    public List<TelemetryData> queryByMetricThreshold(String metricName, double threshold, ThresholdComparison comparison) {
        String operator = switch (comparison) {
            case GREATER_THAN -> ">";
            case LESS_THAN -> "<";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN_OR_EQUAL -> "<=";
            case EQUAL -> "==";
            case NOT_EQUAL -> "!=";
        };
        
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r._field == \"%s\") " +
            "|> filter(fn: (r) => r._value %s %f)",
            bucket, metricName, operator, threshold
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToTelemetryStream(tables).collect(Collectors.toList());
    }
    
    @Override
    public TelemetryQueryResult query(TelemetryQuery query) {
        // Build Flux query based on TelemetryQuery parameters
        StringBuilder fluxBuilder = new StringBuilder();
        fluxBuilder.append(String.format("from(bucket: \"%s\")", bucket));
        
        if (query.hasTimeRange()) {
            fluxBuilder.append(String.format(" |> range(start: %s", query.getFromTime()));
            if (query.getToTime() != null) {
                fluxBuilder.append(String.format(", stop: %s", query.getToTime()));
            }
            fluxBuilder.append(")");
        } else {
            fluxBuilder.append(" |> range(start: -1h)");
        }
        
        fluxBuilder.append(" |> filter(fn: (r) => r._measurement == \"telemetry\")");
        fluxBuilder.append(String.format(" |> filter(fn: (r) => r.organization_id == \"%s\")", query.getOrganizationId()));
        
        if (query.hasDeviceFilter()) {
            String deviceFilter = query.getDeviceIds().stream()
                .map(id -> "\"" + id.getValue() + "\"")
                .collect(Collectors.joining(", "));
            fluxBuilder.append(String.format(" |> filter(fn: (r) => contains(value: r.device_id, set: [%s]))", deviceFilter));
        }
        
        if (query.hasMetricFilter()) {
            String metricFilter = query.getMetrics().stream()
                .map(metric -> "\"" + metric + "\"")
                .collect(Collectors.joining(", "));
            fluxBuilder.append(String.format(" |> filter(fn: (r) => contains(value: r._field, set: [%s]))", metricFilter));
        }
        
        if (query.getLimit() != null) {
            fluxBuilder.append(String.format(" |> limit(n: %d)", query.getLimit()));
        }
        
        fluxBuilder.append(" |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")");
        
        List<FluxTable> tables = queryApi.query(fluxBuilder.toString(), organization);
        List<TelemetryData> data = convertFluxTablesToTelemetryStream(tables).collect(Collectors.toList());
        
        return new TelemetryQueryResult(data, data.size(), false, null);
    }
    
    @Override
    public Stream<TelemetryData> queryStream(TelemetryQuery query) {
        TelemetryQueryResult result = query(query);
        return result.data().stream();
    }
    
    @Override
    public List<TelemetryAggregation> aggregate(TelemetryQuery query, AggregationType aggregationType, Duration interval) {
        String aggregationFunction = switch (aggregationType) {
            case AVERAGE -> "mean()";
            case SUM -> "sum()";
            case MIN -> "min()";
            case MAX -> "max()";
            case COUNT -> "count()";
            case FIRST -> "first()";
            case LAST -> "last()";
            case STDDEV -> "stddev()";
            case MEDIAN -> "median()";
            default -> "mean()";
        };
        
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.organization_id == \"%s\") " +
            "|> aggregateWindow(every: %s, fn: %s)",
            bucket, query.getFromTime(), query.getToTime(), 
            query.getOrganizationId(), formatDuration(interval), aggregationFunction
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return convertFluxTablesToAggregations(tables, aggregationType);
    }
    
    @Override
    public Map<String, Double> getMetricStatistics(String deviceId, String metricName, Instant start, Instant end) {
        String flux = String.format(
            "data = from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> filter(fn: (r) => r._field == \"%s\") " +
            
            "min_val = data |> min() |> findRecord(fn: (key) => true, idx: 0) " +
            "max_val = data |> max() |> findRecord(fn: (key) => true, idx: 0) " +
            "avg_val = data |> mean() |> findRecord(fn: (key) => true, idx: 0) " +
            "count_val = data |> count() |> findRecord(fn: (key) => true, idx: 0)",
            bucket, start, end, deviceId, metricName
        );
        
        // This is a simplified implementation - in practice, you'd need multiple queries
        // or use InfluxDB's statistical functions more effectively
        Map<String, Double> stats = new HashMap<>();
        stats.put("min", 0.0);
        stats.put("max", 0.0);
        stats.put("avg", 0.0);
        stats.put("count", 0.0);
        
        return stats;
    }
    
    @Override
    public List<DeviceMetricSummary> getDeviceSummaries(String organizationId, Instant start, Instant end) {
        // Implementation would require complex Flux queries
        // This is a placeholder implementation
        return List.of();
    }
    
    @Override
    public void deleteOldData(String organizationId, Instant beforeTime) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: 1970-01-01T00:00:00Z, stop: %s) " +
            "|> filter(fn: (r) => r.organization_id == \"%s\") " +
            "|> drop()",
            bucket, beforeTime, organizationId
        );
        
        queryApi.query(flux, organization);
    }
    
    @Override
    public void deleteByDevice(DeviceId deviceId) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: 1970-01-01T00:00:00Z) " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> drop()",
            bucket, deviceId.getValue()
        );
        
        queryApi.query(flux, organization);
    }
    
    @Override
    public long countByOrganization(String organizationId, Instant start, Instant end) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.organization_id == \"%s\") " +
            "|> count()",
            bucket, start, end, organizationId
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return extractCountFromFluxTables(tables);
    }
    
    @Override
    public long countByDevice(DeviceId deviceId, Instant start, Instant end) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> count()",
            bucket, start, end, deviceId.getValue()
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return extractCountFromFluxTables(tables);
    }
    
    @Override
    public TelemetryRepositoryStats getRepositoryStats(String organizationId) {
        // This would require multiple queries to gather comprehensive stats
        // Placeholder implementation
        return new TelemetryRepositoryStats(
            0L, 0L, 0.0, 0L, Map.of(), 
            Instant.now(), Instant.now()
        );
    }
    
    @Override
    public List<DeviceId> getActiveDevices(String organizationId, Duration recentDuration) {
        Instant start = Instant.now().minus(recentDuration);
        
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.organization_id == \"%s\") " +
            "|> distinct(column: \"device_id\")",
            bucket, start, organizationId
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return extractDeviceIdsFromFluxTables(tables);
    }
    
    @Override
    public Instant getLatestTimestamp(DeviceId deviceId) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -30d) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> last()",
            bucket, deviceId.getValue()
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return extractLatestTimestampFromFluxTables(tables);
    }
    
    @Override
    public Instant getEarliestTimestamp(DeviceId deviceId) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: 1970-01-01T00:00:00Z) " +
            "|> filter(fn: (r) => r._measurement == \"telemetry\") " +
            "|> filter(fn: (r) => r.device_id == \"%s\") " +
            "|> first()",
            bucket, deviceId.getValue()
        );
        
        List<FluxTable> tables = queryApi.query(flux, organization);
        return extractEarliestTimestampFromFluxTables(tables);
    }
    
    // Helper methods
    private Point createTimeSeriesPoint(TelemetryData data) {
        Point point = Point.measurement("telemetry")
            .time(data.getTimestamp(), WritePrecision.MS)
            .addTag("device_id", data.getDeviceId().getValue())
            .addTag("organization_id", data.getOrganizationId());
        
        // Add metrics as fields
        for (Map.Entry<String, MetricValue> entry : data.getMetrics().entrySet()) {
            MetricValue value = entry.getValue();
            if (value.isNumeric()) {
                point.addField(entry.getKey(), value.getNumericValue());
            } else if (value.isString()) {
                point.addField(entry.getKey(), value.getStringValue());
            } else if (value.isBoolean()) {
                point.addField(entry.getKey(), value.getBooleanValue());
            }
        }
        
        return point;
    }
    
    private Point createSpatialPoint(TelemetryData data) {
        Point point = Point.measurement("spatial_telemetry")
            .time(data.getTimestamp(), WritePrecision.MS)
            .addTag("device_id", data.getDeviceId().getValue())
            .addTag("organization_id", data.getOrganizationId())
            .addField("latitude", data.getLocation().latitude())
            .addField("longitude", data.getLocation().longitude());
        
        // Add metrics as fields
        for (Map.Entry<String, MetricValue> entry : data.getMetrics().entrySet()) {
            MetricValue value = entry.getValue();
            if (value.isNumeric()) {
                point.addField(entry.getKey(), value.getNumericValue());
            }
        }
        
        return point;
    }
    
    private Stream<TelemetryData> convertFluxTablesToTelemetryStream(List<FluxTable> tables) {
        return tables.stream()
            .flatMap(table -> table.getRecords().stream())
            .collect(Collectors.groupingBy(record -> 
                record.getTime() + "_" + record.getValueByKey("device_id")))
            .values()
            .stream()
            .map(this::convertRecordsToTelemetryData)
            .filter(Objects::nonNull);
    }
    
    private TelemetryData convertRecordsToTelemetryData(List<FluxRecord> records) {
        if (records.isEmpty()) return null;
        
        FluxRecord firstRecord = records.get(0);
        String deviceId = (String) firstRecord.getValueByKey("device_id");
        String organizationId = (String) firstRecord.getValueByKey("organization_id");
        Instant timestamp = firstRecord.getTime();
        
        Map<String, MetricValue> metrics = new HashMap<>();
        GeoLocation location = null;
        
        for (FluxRecord record : records) {
            String field = (String) record.getValueByKey("_field");
            Object value = record.getValue();
            
            if ("latitude".equals(field) && value instanceof Number) {
                double lat = ((Number) value).doubleValue();
                if (location == null) {
                    location = new GeoLocation(lat, 0.0);
                } else {
                    location = new GeoLocation(lat, location.longitude());
                }
            } else if ("longitude".equals(field) && value instanceof Number) {
                double lng = ((Number) value).doubleValue();
                if (location == null) {
                    location = new GeoLocation(0.0, lng);
                } else {
                    location = new GeoLocation(location.latitude(), lng);
                }
            } else if (field != null && value != null) {
                if (value instanceof Number) {
                    metrics.put(field, MetricValue.numeric(((Number) value).doubleValue()));
                } else if (value instanceof String) {
                    metrics.put(field, MetricValue.string((String) value));
                } else if (value instanceof Boolean) {
                    metrics.put(field, MetricValue.bool((Boolean) value));
                }
            }
        }
        
        return new TelemetryData(
            TelemetryId.generate(),
            new DeviceId(deviceId),
            timestamp,
            metrics,
            location,
            organizationId
        );
    }
    
    private List<TelemetryAggregation> convertFluxTablesToAggregations(List<FluxTable> tables, AggregationType type) {
        return tables.stream()
            .flatMap(table -> table.getRecords().stream())
            .map(record -> new TelemetryAggregation(
                record.getTime(),
                (String) record.getValueByKey("_field"),
                record.getValue() instanceof Number ? ((Number) record.getValue()).doubleValue() : 0.0,
                1L,
                type
            ))
            .collect(Collectors.toList());
    }
    
    private String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return value.toString();
    }
    
    private String formatDuration(Duration duration) {
        return duration.getSeconds() + "s";
    }
    
    private long extractCountFromFluxTables(List<FluxTable> tables) {
        return tables.stream()
            .flatMap(table -> table.getRecords().stream())
            .mapToLong(record -> record.getValue() instanceof Number ? 
                ((Number) record.getValue()).longValue() : 0L)
            .sum();
    }
    
    private List<DeviceId> extractDeviceIdsFromFluxTables(List<FluxTable> tables) {
        return tables.stream()
            .flatMap(table -> table.getRecords().stream())
            .map(record -> (String) record.getValueByKey("device_id"))
            .filter(Objects::nonNull)
            .distinct()
            .map(DeviceId::new)
            .collect(Collectors.toList());
    }
    
    private Instant extractLatestTimestampFromFluxTables(List<FluxTable> tables) {
        return tables.stream()
            .flatMap(table -> table.getRecords().stream())
            .map(FluxRecord::getTime)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(Instant.now());
    }
    
    private Instant extractEarliestTimestampFromFluxTables(List<FluxTable> tables) {
        return tables.stream()
            .flatMap(table -> table.getRecords().stream())
            .map(FluxRecord::getTime)
            .filter(Objects::nonNull)
            .min(Instant::compareTo)
            .orElse(Instant.now());
    }
}