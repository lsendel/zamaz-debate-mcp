package com.example.workflow.infrastructure;

import com.example.workflow.domain.*;
import com.example.workflow.infrastructure.influxdb.InfluxDbBatchProcessor;
import com.example.workflow.infrastructure.influxdb.InfluxDbPerformanceMonitor;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InfluxDbTelemetryRepository
 */
@ExtendWith(MockitoExtension.class)
class InfluxDbTelemetryRepositoryTest {
    
    @Mock
    private InfluxDBClient influxDBClient;
    
    @Mock
    private WriteApiBlocking writeApi;
    
    @Mock
    private QueryApi queryApi;
    
    @Mock
    private InfluxDbBatchProcessor batchProcessor;
    
    @Mock
    private InfluxDbPerformanceMonitor performanceMonitor;
    
    private InfluxDbTelemetryRepository repository;
    
    @BeforeEach
    void setUp() {
        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        
        repository = new InfluxDbTelemetryRepository(influxDBClient, batchProcessor, performanceMonitor);
        
        // Set test values using reflection
        ReflectionTestUtils.setField(repository, "bucket", "test-bucket");
        ReflectionTestUtils.setField(repository, "organization", "test-org");
    }
    
    @Test
    void shouldSaveTimeSeriesDataUsingBatchProcessor() {
        // Given
        TelemetryData telemetryData = createTestTelemetryData();
        when(batchProcessor.addTelemetryData(telemetryData)).thenReturn(true);
        
        // When
        repository.saveTimeSeries(telemetryData);
        
        // Then
        verify(batchProcessor).addTelemetryData(telemetryData);
        verify(performanceMonitor).recordWriteOperation(any(), eq(true));
        verifyNoInteractions(writeApi);
    }
    
    @Test
    void shouldFallbackToDirectWriteWhenBatchQueueFull() {
        // Given
        TelemetryData telemetryData = createTestTelemetryData();
        when(batchProcessor.addTelemetryData(telemetryData)).thenReturn(false);
        
        // When
        repository.saveTimeSeries(telemetryData);
        
        // Then
        verify(batchProcessor).addTelemetryData(telemetryData);
        verify(writeApi).writePoint(eq("test-bucket"), eq("test-org"), any());
        verify(performanceMonitor).recordWriteOperation(any(), eq(true));
    }
    
    @Test
    void shouldSaveSpatialDataWhenLocationExists() {
        // Given
        TelemetryData telemetryData = createTestTelemetryDataWithLocation();
        when(batchProcessor.addTelemetryData(telemetryData)).thenReturn(true);
        
        // When
        repository.saveSpatialData(telemetryData);
        
        // Then
        verify(batchProcessor).addTelemetryData(telemetryData);
        verify(performanceMonitor).recordWriteOperation(any(), eq(true));
    }
    
    @Test
    void shouldSkipSpatialDataWhenNoLocation() {
        // Given
        TelemetryData telemetryData = createTestTelemetryData(); // No location
        
        // When
        repository.saveSpatialData(telemetryData);
        
        // Then
        verifyNoInteractions(batchProcessor);
        verifyNoInteractions(performanceMonitor);
    }
    
    @Test
    void shouldSaveBatchUsingBatchProcessor() {
        // Given
        List<TelemetryData> dataList = List.of(
            createTestTelemetryData(),
            createTestTelemetryDataWithLocation()
        );
        when(batchProcessor.addTelemetryDataBatch(dataList)).thenReturn(2);
        
        // When
        repository.saveBatch(dataList);
        
        // Then
        verify(batchProcessor).addTelemetryDataBatch(dataList);
        verify(performanceMonitor).recordWriteOperation(any(), eq(true));
        verifyNoInteractions(writeApi);
    }
    
    @Test
    void shouldProcessRemainingItemsDirectlyWhenBatchPartiallyQueued() {
        // Given
        List<TelemetryData> dataList = List.of(
            createTestTelemetryData(),
            createTestTelemetryDataWithLocation()
        );
        when(batchProcessor.addTelemetryDataBatch(dataList)).thenReturn(1); // Only 1 queued
        
        // When
        repository.saveBatch(dataList);
        
        // Then
        verify(batchProcessor).addTelemetryDataBatch(dataList);
        verify(writeApi, times(2)).writePoints(eq("test-bucket"), eq("test-org"), anyList());
        verify(performanceMonitor).recordWriteOperation(any(), eq(true));
    }
    
    @Test
    void shouldHandleEmptyBatch() {
        // Given
        List<TelemetryData> emptyList = List.of();
        
        // When
        repository.saveBatch(emptyList);
        
        // Then
        verifyNoInteractions(batchProcessor);
        verifyNoInteractions(performanceMonitor);
    }
    
    @Test
    void shouldHandleNullBatch() {
        // When
        repository.saveBatch(null);
        
        // Then
        verifyNoInteractions(batchProcessor);
        verifyNoInteractions(performanceMonitor);
    }
    
    @Test
    void shouldRecordPerformanceMetricsOnWriteError() {
        // Given
        TelemetryData telemetryData = createTestTelemetryData();
        when(batchProcessor.addTelemetryData(telemetryData)).thenReturn(false);
        doThrow(new RuntimeException("Write failed")).when(writeApi).writePoint(any(), any(), any());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> repository.saveTimeSeries(telemetryData));
        verify(performanceMonitor).recordWriteOperation(any(), eq(false));
    }
    
    @Test
    void shouldRecordPerformanceMetricsOnQuerySuccess() {
        // Given
        String deviceId = "device-001";
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        when(queryApi.query(anyString(), eq("test-org"))).thenReturn(List.of());
        
        // When
        Stream<TelemetryData> result = repository.queryTimeSeries(deviceId, start, end);
        
        // Then
        assertNotNull(result);
        verify(performanceMonitor).recordReadOperation(any(), eq(true));
    }
    
    @Test
    void shouldRecordPerformanceMetricsOnQueryError() {
        // Given
        String deviceId = "device-001";
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        when(queryApi.query(anyString(), eq("test-org"))).thenThrow(new RuntimeException("Query failed"));
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            repository.queryTimeSeries(deviceId, start, end));
        verify(performanceMonitor).recordReadOperation(any(), eq(false));
    }
    
    private TelemetryData createTestTelemetryData() {
        return new TelemetryData(
            TelemetryId.generate(),
            new DeviceId("test-device-001"),
            Instant.now(),
            Map.of(
                "temperature", MetricValue.numeric(23.5),
                "humidity", MetricValue.numeric(65.0),
                "status", MetricValue.string("normal")
            ),
            null, // No location
            "test-org"
        );
    }
    
    private TelemetryData createTestTelemetryDataWithLocation() {
        return new TelemetryData(
            TelemetryId.generate(),
            new DeviceId("test-device-002"),
            Instant.now(),
            Map.of(
                "temperature", MetricValue.numeric(24.0),
                "humidity", MetricValue.numeric(70.0)
            ),
            new GeoLocation(41.0534, -73.5387), // Stamford, CT
            "test-org"
        );
    }
}