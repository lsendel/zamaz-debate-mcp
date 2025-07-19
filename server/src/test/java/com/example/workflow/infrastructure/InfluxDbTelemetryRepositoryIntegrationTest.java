package com.example.workflow.infrastructure;

import com.example.workflow.domain.*;
import com.example.workflow.infrastructure.influxdb.InfluxDbBatchProcessor;
import com.example.workflow.infrastructure.influxdb.InfluxDbPerformanceMonitor;
import com.example.workflow.infrastructure.influxdb.InfluxDbSchemaManager;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InfluxDB telemetry repository using TestContainers
 */
@SpringBootTest
@Testcontainers
class InfluxDbTelemetryRepositoryIntegrationTest {
    
    @Container
    static InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
        DockerImageName.parse("influxdb:2.7-alpine"))
        .withDatabase("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withAdminToken("test-token")
        .withOrganization("test-org")
        .withBucket("telemetry");
    
    private InfluxDBClient influxDBClient;
    private InfluxDbTelemetryRepository repository;
    private InfluxDbBatchProcessor batchProcessor;
    private InfluxDbPerformanceMonitor performanceMonitor;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("influxdb.url", influxDBContainer::getUrl);
        registry.add("influxdb.token", () -> "test-token");
        registry.add("influxdb.organization", () -> "test-org");
        registry.add("influxdb.bucket", () -> "telemetry");
        registry.add("influxdb.batch.size", () -> "100");
        registry.add("influxdb.batch.flush-interval", () -> "500");
    }
    
    @BeforeEach
    void setUp() {
        influxDBClient = InfluxDBClientFactory.create(
            influxDBContainer.getUrl(),
            "test-token".toCharArray(),
            "test-org",
            "telemetry"
        );
        
        // Initialize components
        batchProcessor = new InfluxDbBatchProcessor(influxDBClient);
        performanceMonitor = new InfluxDbPerformanceMonitor(
            influxDBClient, batchProcessor, new SimpleMeterRegistry());
        
        repository = new InfluxDbTelemetryRepository(
            influxDBClient, batchProcessor, performanceMonitor);
        
        // Initialize schema
        InfluxDbSchemaManager schemaManager = new InfluxDbSchemaManager(influxDBClient);
        // Note: Schema initialization would normally happen via @PostConstruct
    }
    
    @Test
    void shouldSaveAndQueryTimeSeriesData() throws InterruptedException {
        // Given
        TelemetryData telemetryData = createTestTelemetryData("device-001");
        
        // When
        repository.saveTimeSeries(telemetryData);
        
        // Wait for batch processing
        Thread.sleep(1000);
        
        // Query the data
        Stream<TelemetryData> result = repository.queryTimeSeries(
            "device-001",
            telemetryData.getTimestamp().minusSeconds(10),
            telemetryData.getTimestamp().plusSeconds(10)
        );
        
        // Then
        List<TelemetryData> resultList = result.collect(Collectors.toList());
        assertFalse(resultList.isEmpty());
        
        TelemetryData retrieved = resultList.get(0);
        assertEquals("device-001", retrieved.getDeviceId().getValue());
        assertEquals("test-org", retrieved.getOrganizationId());
        assertTrue(retrieved.hasMetric("temperature"));
        assertTrue(retrieved.hasMetric("humidity"));
    }
    
    @Test
    void shouldSaveAndQuerySpatialData() throws InterruptedException {
        // Given
        TelemetryData spatialData = createTestTelemetryDataWithLocation("device-002");
        
        // When
        repository.saveSpatialData(spatialData);
        
        // Wait for batch processing
        Thread.sleep(1000);
        
        // Query spatial data
        BoundingBox boundingBox = BoundingBox.of(40.0, 42.0, -75.0, -72.0);
        List<TelemetryData> result = repository.querySpatial(boundingBox);
        
        // Then
        assertFalse(result.isEmpty());
        TelemetryData retrieved = result.get(0);
        assertEquals("device-002", retrieved.getDeviceId().getValue());
        assertTrue(retrieved.hasSpatialData());
        assertNotNull(retrieved.getLocation());
    }
    
    @Test
    void shouldHandleBatchOperations() throws InterruptedException {
        // Given
        List<TelemetryData> batchData = List.of(
            createTestTelemetryData("device-003"),
            createTestTelemetryData("device-004"),
            createTestTelemetryDataWithLocation("device-005")
        );
        
        // When
        repository.saveBatch(batchData);
        
        // Wait for batch processing
        Thread.sleep(1500);
        
        // Query recent data
        Stream<TelemetryData> result = repository.queryRecentData("test-org", Duration.ofMinutes(1));
        
        // Then
        List<TelemetryData> resultList = result.collect(Collectors.toList());
        assertTrue(resultList.size() >= 3);
        
        // Verify all devices are present
        List<String> deviceIds = resultList.stream()
            .map(data -> data.getDeviceId().getValue())
            .distinct()
            .collect(Collectors.toList());
        
        assertTrue(deviceIds.contains("device-003"));
        assertTrue(deviceIds.contains("device-004"));
        assertTrue(deviceIds.contains("device-005"));
    }
    
    @Test
    void shouldQueryByMetricThreshold() throws InterruptedException {
        // Given
        TelemetryData highTempData = new TelemetryData(
            TelemetryId.generate(),
            new DeviceId("device-hot"),
            Instant.now(),
            Map.of("temperature", MetricValue.numeric(35.0)),
            null,
            "test-org"
        );
        
        TelemetryData lowTempData = new TelemetryData(
            TelemetryId.generate(),
            new DeviceId("device-cold"),
            Instant.now(),
            Map.of("temperature", MetricValue.numeric(15.0)),
            null,
            "test-org"
        );
        
        // When
        repository.saveTimeSeries(highTempData);
        repository.saveTimeSeries(lowTempData);
        
        // Wait for batch processing
        Thread.sleep(1000);
        
        // Query by threshold
        List<TelemetryData> hotDevices = repository.queryByMetricThreshold(
            "temperature", 30.0, TelemetryRepository.ThresholdComparison.GREATER_THAN);
        
        List<TelemetryData> coldDevices = repository.queryByMetricThreshold(
            "temperature", 20.0, TelemetryRepository.ThresholdComparison.LESS_THAN);
        
        // Then
        assertFalse(hotDevices.isEmpty());
        assertFalse(coldDevices.isEmpty());
        
        assertTrue(hotDevices.stream()
            .anyMatch(data -> "device-hot".equals(data.getDeviceId().getValue())));
        assertTrue(coldDevices.stream()
            .anyMatch(data -> "device-cold".equals(data.getDeviceId().getValue())));
    }
    
    @Test
    void shouldQueryByRadius() throws InterruptedException {
        // Given - Create devices at different locations
        GeoLocation stamfordLocation = new GeoLocation(41.0534, -73.5387);
        GeoLocation nearbyLocation = new GeoLocation(41.0600, -73.5400); // ~1km away
        GeoLocation farLocation = new GeoLocation(42.0000, -74.0000); // ~100km away
        
        TelemetryData nearDevice = createTelemetryDataWithLocation("device-near", nearbyLocation);
        TelemetryData farDevice = createTelemetryDataWithLocation("device-far", farLocation);
        
        // When
        repository.saveSpatialData(nearDevice);
        repository.saveSpatialData(farDevice);
        
        // Wait for batch processing
        Thread.sleep(1000);
        
        // Query by radius (5km from Stamford)
        List<TelemetryData> nearbyDevices = repository.queryByRadius(stamfordLocation, 5.0);
        
        // Then
        assertFalse(nearbyDevices.isEmpty());
        
        // Should find nearby device but not far device
        List<String> deviceIds = nearbyDevices.stream()
            .map(data -> data.getDeviceId().getValue())
            .collect(Collectors.toList());
        
        assertTrue(deviceIds.contains("device-near"));
        assertFalse(deviceIds.contains("device-far"));
    }
    
    @Test
    void shouldProvidePerformanceStatistics() throws InterruptedException {
        // Given - Generate some data
        List<TelemetryData> testData = List.of(
            createTestTelemetryData("perf-device-1"),
            createTestTelemetryData("perf-device-2"),
            createTestTelemetryData("perf-device-3")
        );
        
        // When
        repository.saveBatch(testData);
        
        // Wait for processing
        Thread.sleep(1000);
        
        // Get performance stats
        InfluxDbBatchProcessor.BatchProcessingStats stats = batchProcessor.getStats();
        
        // Then
        assertTrue(stats.totalProcessed() >= 3);
        assertTrue(stats.batchesProcessed() >= 1);
        assertEquals(0, stats.totalErrors()); // Should have no errors
    }
    
    private TelemetryData createTestTelemetryData(String deviceId) {
        return new TelemetryData(
            TelemetryId.generate(),
            new DeviceId(deviceId),
            Instant.now(),
            Map.of(
                "temperature", MetricValue.numeric(20.0 + Math.random() * 10.0),
                "humidity", MetricValue.numeric(50.0 + Math.random() * 30.0),
                "status", MetricValue.string("normal")
            ),
            null,
            "test-org"
        );
    }
    
    private TelemetryData createTestTelemetryDataWithLocation(String deviceId) {
        return createTelemetryDataWithLocation(deviceId, new GeoLocation(41.0534, -73.5387));
    }
    
    private TelemetryData createTelemetryDataWithLocation(String deviceId, GeoLocation location) {
        return new TelemetryData(
            TelemetryId.generate(),
            new DeviceId(deviceId),
            Instant.now(),
            Map.of(
                "temperature", MetricValue.numeric(20.0 + Math.random() * 10.0),
                "humidity", MetricValue.numeric(50.0 + Math.random() * 30.0)
            ),
            location,
            "test-org"
        );
    }
}