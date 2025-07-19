package com.example.workflow.domain.services;

import com.example.workflow.domain.DeviceId;
import com.example.workflow.domain.MetricValue;
import com.example.workflow.domain.TelemetryData;
import com.example.workflow.domain.TelemetryId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConditionEvaluationService
 * Verifies implementation of requirements 3.5 and 3.6 for condition evaluation
 */
class ConditionEvaluationServiceTest {
    
    private ConditionEvaluationService conditionEvaluationService;
    private TelemetryData testTelemetryData;
    
    @BeforeEach
    void setUp() {
        conditionEvaluationService = new ConditionEvaluationService();
        
        // Create test telemetry data
        testTelemetryData = new TelemetryData(
            TelemetryId.generate(),
            DeviceId.of("test-device"),
            "org-123"
        );
        testTelemetryData.addMetric("temperature", MetricValue.of(25.5));
        testTelemetryData.addMetric("humidity", MetricValue.of(60.0));
        testTelemetryData.addMetric("status", MetricValue.of("active"));
        testTelemetryData.addMetric("enabled", MetricValue.of(true));
    }
    
    @Test
    void shouldEvaluateSimpleStringCondition() {
        // Given
        String condition = "temperature > 25";
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldEvaluateSimpleStringConditionFalse() {
        // Given
        String condition = "temperature > 30";
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void shouldEvaluateMapBasedCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "field", "temperature",
            "operator", "gt",
            "value", 25.0
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldEvaluateComplexAndCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "operator", "AND",
            "conditions", List.of(
                Map.of("field", "temperature", "operator", "gt", "value", 20.0),
                Map.of("field", "humidity", "operator", "lt", "value", 70.0)
            )
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldEvaluateComplexOrCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "operator", "OR",
            "conditions", List.of(
                Map.of("field", "temperature", "operator", "gt", "value", 30.0), // false
                Map.of("field", "humidity", "operator", "lt", "value", 70.0)     // true
            )
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldEvaluateStringContainsCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "field", "status",
            "operator", "contains",
            "value", "act"
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldEvaluateBooleanCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "field", "enabled",
            "operator", "eq",
            "value", true
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldValidateValidConditions() {
        // Given
        Map<String, Object> condition = Map.of(
            "field", "temperature",
            "operator", "gt",
            "value", 25.0
        );
        
        // When
        ConditionValidationResult result = conditionEvaluationService.validateConditions(condition);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }
    
    @Test
    void shouldValidateInvalidConditions() {
        // Given - missing field
        Map<String, Object> condition = Map.of(
            "operator", "gt",
            "value", 25.0
        );
        
        // When
        ConditionValidationResult result = conditionEvaluationService.validateConditions(condition);
        
        // Then
        assertFalse(result.isValid());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("field cannot be empty")));
    }
    
    @Test
    void shouldValidateSimpleConditionString() {
        // Given
        String condition = "temperature > 25";
        
        // When
        ConditionValidationResult result = conditionEvaluationService.validateConditions(condition);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }
    
    @Test
    void shouldValidateInvalidSimpleConditionString() {
        // Given - invalid format
        String condition = "temperature";
        
        // When
        ConditionValidationResult result = conditionEvaluationService.validateConditions(condition);
        
        // Then
        assertFalse(result.isValid());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("Invalid condition format")));
    }
    
    @Test
    void shouldThrowExceptionForUnsupportedConditionType() {
        // Given
        Integer invalidCondition = 123;
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            conditionEvaluationService.evaluateConditions(invalidCondition, testTelemetryData);
        });
    }
    
    @Test
    void shouldHandleConditionEvaluationException() {
        // Given - condition that will cause evaluation error
        Map<String, Object> condition = Map.of(
            "field", "nonexistent_field",
            "operator", "gt",
            "value", "invalid_number"
        );
        
        // When & Then
        assertThrows(ConditionEvaluationException.class, () -> {
            conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        });
    }
    
    @Test
    void shouldEvaluateInCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "field", "status",
            "operator", "in",
            "value", List.of("active", "running", "enabled")
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldEvaluateBetweenCondition() {
        // Given
        Map<String, Object> condition = Map.of(
            "field", "temperature",
            "operator", "between",
            "value", Map.of("min", 20.0, "max", 30.0)
        );
        
        // When
        boolean result = conditionEvaluationService.evaluateConditions(condition, testTelemetryData);
        
        // Then
        assertTrue(result);
    }
}