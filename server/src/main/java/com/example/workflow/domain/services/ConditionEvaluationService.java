package com.example.workflow.domain.services;

import com.example.workflow.domain.TelemetryData;
import com.example.workflow.domain.MetricValue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for evaluating workflow conditions against telemetry data
 * Supports complex conditional logic with AND, OR, NOT operators
 * Implements requirements 3.5 and 3.6 for real-time condition evaluation and routing
 */
@Service
public class ConditionEvaluationService {
    
    /**
     * Evaluate conditions against telemetry data
     * Implements requirement 3.5: Evaluate conditions against real-time data
     */
    public boolean evaluateConditions(Object conditionsConfig, TelemetryData telemetryData) {
        Objects.requireNonNull(conditionsConfig, "Conditions config cannot be null");
        
        try {
            if (conditionsConfig instanceof Map<?, ?> conditionMap) {
                return evaluateConditionMap(conditionMap, telemetryData);
            } else if (conditionsConfig instanceof List<?> conditionList) {
                return evaluateConditionList(conditionList, telemetryData);
            } else if (conditionsConfig instanceof String conditionString) {
                return evaluateConditionString(conditionString, telemetryData);
            }
            
            throw new IllegalArgumentException("Unsupported condition configuration type: " + conditionsConfig.getClass());
        } catch (Exception e) {
            throw new ConditionEvaluationException("Failed to evaluate conditions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate condition configuration without executing it
     * Supports requirement 1.3: Validate conditions and provide feedback
     */
    public ConditionValidationResult validateConditions(Object conditionsConfig) {
        Objects.requireNonNull(conditionsConfig, "Conditions config cannot be null");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            validateConditionStructure(conditionsConfig, errors, warnings);
        } catch (Exception e) {
            errors.add("Invalid condition structure: " + e.getMessage());
        }
        
        boolean isValid = errors.isEmpty();
        return new ConditionValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Evaluate condition map (complex conditions with operators)
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateConditionMap(Map<?, ?> conditionMap, TelemetryData telemetryData) {
        String operator = (String) conditionMap.get("operator");
        if (operator == null) {
            operator = "AND"; // Default operator
        }
        
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionMap.get("conditions");
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means always true
        }
        
        return switch (operator.toUpperCase()) {
            case "AND" -> evaluateAndConditions(conditions, telemetryData);
            case "OR" -> evaluateOrConditions(conditions, telemetryData);
            case "NOT" -> evaluateNotConditions(conditions, telemetryData);
            default -> throw new IllegalArgumentException("Unsupported logical operator: " + operator);
        };
    }
    
    /**
     * Evaluate condition list (implicit AND)
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateConditionList(List<?> conditionList, TelemetryData telemetryData) {
        for (Object condition : conditionList) {
            if (condition instanceof Map<?, ?> conditionMap) {
                if (!evaluateSingleCondition((Map<String, Object>) conditionMap, telemetryData)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Evaluate simple condition string
     */
    private boolean evaluateConditionString(String conditionString, TelemetryData telemetryData) {
        // Parse simple condition strings like "temperature > 25"
        return parseAndEvaluateSimpleCondition(conditionString, telemetryData);
    }
    
    /**
     * Evaluate AND conditions (all must be true)
     */
    private boolean evaluateAndConditions(List<Map<String, Object>> conditions, TelemetryData telemetryData) {
        for (Map<String, Object> condition : conditions) {
            if (!evaluateSingleCondition(condition, telemetryData)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Evaluate OR conditions (at least one must be true)
     */
    private boolean evaluateOrConditions(List<Map<String, Object>> conditions, TelemetryData telemetryData) {
        for (Map<String, Object> condition : conditions) {
            if (evaluateSingleCondition(condition, telemetryData)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Evaluate NOT conditions (none must be true)
     */
    private boolean evaluateNotConditions(List<Map<String, Object>> conditions, TelemetryData telemetryData) {
        return !evaluateAndConditions(conditions, telemetryData);
    }
    
    /**
     * Evaluate a single condition
     */
    private boolean evaluateSingleCondition(Map<String, Object> condition, TelemetryData telemetryData) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");
        
        if (field == null || operator == null || expectedValue == null) {
            throw new IllegalArgumentException("Condition must have field, operator, and value");
        }
        
        // Handle special fields
        if ("timestamp".equals(field)) {
            return evaluateTimestampCondition(operator, expectedValue, telemetryData);
        } else if ("deviceId".equals(field)) {
            return evaluateDeviceIdCondition(operator, expectedValue, telemetryData);
        } else if ("location".equals(field)) {
            return evaluateLocationCondition(operator, expectedValue, telemetryData);
        }
        
        // Handle metric fields
        MetricValue actualValue = telemetryData.getMetric(field);
        if (actualValue == null) {
            return false; // Metric not found
        }
        
        return evaluateMetricCondition(actualValue, operator, expectedValue);
    }
    
    /**
     * Evaluate metric condition
     */
    private boolean evaluateMetricCondition(MetricValue actualValue, String operator, Object expectedValue) {
        return switch (operator.toLowerCase()) {
            case "eq", "equals", "==" -> evaluateEquals(actualValue, expectedValue);
            case "ne", "not_equals", "!=" -> !evaluateEquals(actualValue, expectedValue);
            case "gt", "greater_than", ">" -> evaluateGreaterThan(actualValue, expectedValue);
            case "gte", "greater_than_equals", ">=" -> evaluateGreaterThanEquals(actualValue, expectedValue);
            case "lt", "less_than", "<" -> evaluateLessThan(actualValue, expectedValue);
            case "lte", "less_than_equals", "<=" -> evaluateLessThanEquals(actualValue, expectedValue);
            case "contains" -> evaluateContains(actualValue, expectedValue);
            case "in" -> evaluateIn(actualValue, expectedValue);
            case "between" -> evaluateBetween(actualValue, expectedValue);
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }
    
    /**
     * Evaluate equals condition
     */
    private boolean evaluateEquals(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isNumeric() && expectedValue instanceof Number) {
            return Double.compare(actualValue.getNumericValue(), ((Number) expectedValue).doubleValue()) == 0;
        } else if (actualValue.isString() && expectedValue instanceof String) {
            return actualValue.getStringValue().equals(expectedValue);
        } else if (actualValue.isBoolean() && expectedValue instanceof Boolean) {
            return actualValue.getBooleanValue().equals(expectedValue);
        }
        return false;
    }
    
    /**
     * Evaluate greater than condition
     */
    private boolean evaluateGreaterThan(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isNumeric() && expectedValue instanceof Number) {
            return actualValue.getNumericValue() > ((Number) expectedValue).doubleValue();
        }
        return false;
    }
    
    /**
     * Evaluate greater than or equals condition
     */
    private boolean evaluateGreaterThanEquals(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isNumeric() && expectedValue instanceof Number) {
            return actualValue.getNumericValue() >= ((Number) expectedValue).doubleValue();
        }
        return false;
    }
    
    /**
     * Evaluate less than condition
     */
    private boolean evaluateLessThan(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isNumeric() && expectedValue instanceof Number) {
            return actualValue.getNumericValue() < ((Number) expectedValue).doubleValue();
        }
        return false;
    }
    
    /**
     * Evaluate less than or equals condition
     */
    private boolean evaluateLessThanEquals(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isNumeric() && expectedValue instanceof Number) {
            return actualValue.getNumericValue() <= ((Number) expectedValue).doubleValue();
        }
        return false;
    }
    
    /**
     * Evaluate contains condition
     */
    private boolean evaluateContains(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isString() && expectedValue instanceof String) {
            return actualValue.getStringValue().contains((String) expectedValue);
        }
        return false;
    }
    
    /**
     * Evaluate in condition (value in list)
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateIn(MetricValue actualValue, Object expectedValue) {
        if (expectedValue instanceof List<?> expectedList) {
            for (Object item : expectedList) {
                if (evaluateEquals(actualValue, item)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Evaluate between condition (value between min and max)
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateBetween(MetricValue actualValue, Object expectedValue) {
        if (actualValue.isNumeric() && expectedValue instanceof Map<?, ?> rangeMap) {
            Map<String, Object> range = (Map<String, Object>) rangeMap;
            Object minValue = range.get("min");
            Object maxValue = range.get("max");
            
            if (minValue instanceof Number && maxValue instanceof Number) {
                double actual = actualValue.getNumericValue();
                double min = ((Number) minValue).doubleValue();
                double max = ((Number) maxValue).doubleValue();
                return actual >= min && actual <= max;
            }
        }
        return false;
    }
    
    /**
     * Evaluate timestamp condition
     */
    private boolean evaluateTimestampCondition(String operator, Object expectedValue, TelemetryData telemetryData) {
        // Implementation for timestamp-based conditions
        // This would compare telemetryData.getTimestamp() with expectedValue
        return true; // Placeholder
    }
    
    /**
     * Evaluate device ID condition
     */
    private boolean evaluateDeviceIdCondition(String operator, Object expectedValue, TelemetryData telemetryData) {
        String actualDeviceId = telemetryData.getDeviceId().value();
        if ("eq".equals(operator) || "equals".equals(operator)) {
            return actualDeviceId.equals(expectedValue);
        } else if ("contains".equals(operator)) {
            return actualDeviceId.contains((String) expectedValue);
        }
        return false;
    }
    
    /**
     * Evaluate location condition
     */
    private boolean evaluateLocationCondition(String operator, Object expectedValue, TelemetryData telemetryData) {
        if (!telemetryData.hasSpatialData()) {
            return false;
        }
        
        // Implementation for location-based conditions
        // This would handle proximity, bounding box, etc.
        return true; // Placeholder
    }
    
    /**
     * Parse and evaluate simple condition string like "temperature > 25"
     */
    private boolean parseAndEvaluateSimpleCondition(String conditionString, TelemetryData telemetryData) {
        // Simple parser for conditions like "temperature > 25"
        String[] parts = conditionString.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid condition format: " + conditionString);
        }
        
        String field = parts[0];
        String operator = parts[1];
        String valueStr = parts[2];
        
        // Try to parse value as number, boolean, or string
        Object value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            if ("true".equalsIgnoreCase(valueStr) || "false".equalsIgnoreCase(valueStr)) {
                value = Boolean.parseBoolean(valueStr);
            } else {
                value = valueStr.replace("\"", "").replace("'", "");
            }
        }
        
        MetricValue actualValue = telemetryData.getMetric(field);
        if (actualValue == null) {
            return false;
        }
        
        return evaluateMetricCondition(actualValue, operator, value);
    }
    
    /**
     * Validate condition structure recursively
     */
    @SuppressWarnings("unchecked")
    private void validateConditionStructure(Object conditionsConfig, List<String> errors, List<String> warnings) {
        if (conditionsConfig instanceof Map<?, ?> conditionMap) {
            Map<String, Object> map = (Map<String, Object>) conditionMap;
            
            // Check for operator-based conditions
            if (map.containsKey("operator")) {
                String operator = (String) map.get("operator");
                if (!List.of("AND", "OR", "NOT").contains(operator.toUpperCase())) {
                    errors.add("Invalid logical operator: " + operator);
                }
                
                List<Map<String, Object>> conditions = (List<Map<String, Object>>) map.get("conditions");
                if (conditions == null || conditions.isEmpty()) {
                    errors.add("Operator-based condition must have conditions array");
                } else {
                    for (Map<String, Object> condition : conditions) {
                        validateConditionStructure(condition, errors, warnings);
                    }
                }
            } else {
                // Check for single condition
                validateSingleConditionStructure(map, errors, warnings);
            }
        } else if (conditionsConfig instanceof List<?> conditionList) {
            for (Object condition : conditionList) {
                validateConditionStructure(condition, errors, warnings);
            }
        } else if (conditionsConfig instanceof String conditionString) {
            validateSimpleConditionString(conditionString, errors, warnings);
        }
    }
    
    /**
     * Validate single condition structure
     */
    private void validateSingleConditionStructure(Map<String, Object> condition, List<String> errors, List<String> warnings) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object value = condition.get("value");
        
        if (field == null || field.trim().isEmpty()) {
            errors.add("Condition field cannot be empty");
        }
        
        if (operator == null || operator.trim().isEmpty()) {
            errors.add("Condition operator cannot be empty");
        } else {
            List<String> validOperators = List.of("eq", "ne", "gt", "gte", "lt", "lte", "contains", "in", "between");
            if (!validOperators.contains(operator.toLowerCase())) {
                warnings.add("Unknown operator: " + operator);
            }
        }
        
        if (value == null) {
            warnings.add("Condition value is null for field: " + field);
        }
    }
    
    /**
     * Validate simple condition string format
     */
    private void validateSimpleConditionString(String conditionString, List<String> errors, List<String> warnings) {
        if (conditionString == null || conditionString.trim().isEmpty()) {
            errors.add("Condition string cannot be empty");
            return;
        }
        
        String[] parts = conditionString.trim().split("\\s+");
        if (parts.length != 3) {
            errors.add("Invalid condition format. Expected: 'field operator value'");
        } else {
            String field = parts[0];
            String operator = parts[1];
            String value = parts[2];
            
            if (field.isEmpty()) {
                errors.add("Field name cannot be empty");
            }
            
            List<String> validOperators = List.of(">", "<", ">=", "<=", "==", "!=");
            if (!validOperators.contains(operator)) {
                warnings.add("Unknown operator in simple condition: " + operator);
            }
            
            if (value.isEmpty()) {
                errors.add("Value cannot be empty");
            }
        }
    }
}

/**
 * Result of condition validation
 */
record ConditionValidationResult(
    boolean isValid,
    List<String> errors,
    List<String> warnings
) {
    public static ConditionValidationResult valid() {
        return new ConditionValidationResult(true, List.of(), List.of());
    }
    
    public static ConditionValidationResult invalid(List<String> errors) {
        return new ConditionValidationResult(false, errors, List.of());
    }
    
    public static ConditionValidationResult withWarnings(List<String> warnings) {
        return new ConditionValidationResult(true, List.of(), warnings);
    }
}

/**
 * Exception thrown when condition evaluation fails
 */
class ConditionEvaluationException extends RuntimeException {
    public ConditionEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}