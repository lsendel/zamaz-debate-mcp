package com.example.workflow.domain.services;

import com.example.workflow.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TelemetryDomainService
 * Tests requirements 2.1, 2.4, 6.6 for stream processing, analysis, and spatial operations
 */
class TelemetryDomainServiceTest {
    
    private TelemetryDomainService telemetryDomainService;
    private String organizationId;
    private DeviceId deviceId;
    private GeoLocation stamfordLocation;
    
    @BeforeEach
    void setUp() {
        telemetryDomainService = new TelemetryDomainService();
        organizationId = "test-org-123";
        deviceId = DeviceId.of("test-device-001");
        stamfordLocation = GeoLocation.of(41.0534, -73.5387); // Stamford, CT
    }
    
    @Test
    @DisplayName("Should process telemetry stream successfully")
    void shouldProcessTelemetryStreamSuccessfully() {
        // Given
        List<TelemetryData> telemetryList = List.of(
            createValidTelemetryData("device-001"),
            createValidTelemetryData("device-002"),
            createValidTelemetryData("device-003")
        );
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            telemetryDomainService.processTelemetryStream(telemetryList.stream());
        });
    }
    
    @Test
    @DisplayName("Should analyze telemetry data and return comprehensive analysis")
    void shouldAnalyzeTelemetryDataAndReturnComprehensiveAnalysis() {
        // Given
        TelemetryQuery query = TelemetryQuery.builder(organizationId)
            .timeRange(Instant.now().minusHours(1), Instant.now())
            .metrics(List.of("temperature", "humidity", "air_quality"))
            .build();
        
        // When
        TelemetryAnalysis analysis = telemetryDomainService.analyzeTelemetry(query);
        
        // Then
        assertNotNull(analysis);
        assertEquals(organizationId, analysis.getOrganizationId());
        assertNotNull(analysis.getAnalysisTime());
        assertNotNull(analysis.getMetricAnalyses());
        assertNotNull(analysis.getAnomalies());
        assertNotNull(analysis.getTrends());
        assertNotNull(analysis.getStatistics());
        
        // Should have analyses for requested metrics
        assertTrue(analysis.getMetricAnalyses().containsKey("temperature"));
        assertTrue(analysis.getMetricAnalyses().containsKey("humidity"));
        assertTrue(analysis.getMetricAnalyses().containsKey("air_quality"));
    }
    
    @Test
    @DisplayName("Should trigger workflow conditions when thresholds are violated")
    void shouldTriggerWorkflowConditionsWhenThresholdsAreViolated() {
        // Given
        WorkflowId workflowId = WorkflowId.generate();
        TelemetryThreshold threshold = new TelemetryThreshold(
            "threshold-001",
            organizationId,
            workflowId,
            "temperature",
            ThresholdCondition.GREATER_THAN,
            30.0,
            "High temperature alert"
        );
        
        telemetryDomainService.registerTelemetryThreshold(organizationId, threshold);
        
        TelemetryData highTempData = new TelemetryData(
            TelemetryId.generate(),
            deviceId,
            Instant.now(),
            Map.of("temperature", MetricValue.numeric(35.0)), // Above threshold
            stamfordLocation,
            organizationId
        );
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            telemetryDomainService.triggerWorkflowConditions(highTempData);
        });
    }
    
    @Test
    @DisplayName("Should perform spatial analysis with clusters and hotspots")
    void shouldPerformSpatialAnalysisWithClustersAndHotspots() {
        // Given
        TelemetryQuery spatialQuery = TelemetryQuery.builder(organizationId)
            .spatialRadius(stamfordLocation, 5.0) // 5km radius around Stamford
            .metrics(List.of("temperature", "air_quality"))
            .build();
        
        // When
        SpatialAnalysisResult result = telemetryDomainService.analyzeSpatialTelemetry(spatialQuery);
        
        // Then
        assertNotNull(result);
        assertEquals(organizationId, result.organizationId());
        assertEquals(stamfordLocation, result.centerLocation());
        assertEquals(5.0, result.radiusKm());
        assertNotNull(result.clusters());
        assertNotNull(result.hotspots());
        assertNotNull(result.distribution());
        assertNotNull(result.proximityAnalyses());
        assertNotNull(result.analysisTime());
        
        // Should have at least one cluster
        assertFalse(result.clusters().isEmpty());
    }
    
    // Helper methods
    private TelemetryData createValidTelemetryData(String deviceIdValue) {
        return new TelemetryData(
            TelemetryId.generate(),
            DeviceId.of(deviceIdValue),
            Instant.now(),
            Map.of(
                "temperature", MetricValue.numeric(20.0 + Math.random() * 15.0),
                "humidity", MetricValue.numeric(40.0 + Math.random() * 40.0),
                "motion", MetricValue.bool(Math.random() > 0.7),
                "air_quality", MetricValue.numeric(50.0 + Math.random() * 100.0)
            ),
            stamfordLocation,
            organizationId
        );
    }
}