package com.example.workflow.domain;

import java.util.Objects;

/**
 * Value object representing a telemetry metric value
 * Supports numeric, string, and boolean values
 */
public class MetricValue {
    
    private final Object value;
    private final MetricType type;
    
    private MetricValue(Object value, MetricType type) {
        this.value = Objects.requireNonNull(value, "Metric value cannot be null");
        this.type = Objects.requireNonNull(type, "Metric type cannot be null");
    }
    
    /**
     * Create numeric metric value
     */
    public static MetricValue numeric(Number value) {
        return new MetricValue(value.doubleValue(), MetricType.NUMERIC);
    }
    
    /**
     * Create string metric value
     */
    public static MetricValue string(String value) {
        return new MetricValue(value, MetricType.STRING);
    }
    
    /**
     * Create boolean metric value
     */
    public static MetricValue bool(Boolean value) {
        return new MetricValue(value, MetricType.BOOLEAN);
    }
    
    /**
     * Get numeric value (throws if not numeric)
     */
    public Double getNumericValue() {
        if (type != MetricType.NUMERIC) {
            throw new IllegalStateException("Metric value is not numeric: " + type);
        }
        return (Double) value;
    }
    
    /**
     * Get string value (throws if not string)
     */
    public String getStringValue() {
        if (type != MetricType.STRING) {
            throw new IllegalStateException("Metric value is not string: " + type);
        }
        return (String) value;
    }
    
    /**
     * Get boolean value (throws if not boolean)
     */
    public Boolean getBooleanValue() {
        if (type != MetricType.BOOLEAN) {
            throw new IllegalStateException("Metric value is not boolean: " + type);
        }
        return (Boolean) value;
    }
    
    /**
     * Check if value is numeric
     */
    public boolean isNumeric() {
        return type == MetricType.NUMERIC;
    }
    
    /**
     * Check if value is string
     */
    public boolean isString() {
        return type == MetricType.STRING;
    }
    
    /**
     * Check if value is boolean
     */
    public boolean isBoolean() {
        return type == MetricType.BOOLEAN;
    }
    
    /**
     * Get raw value
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Get metric type
     */
    public MetricType getType() {
        return type;
    }
    
    /**
     * Compare numeric values (only for numeric metrics)
     */
    public int compareNumeric(MetricValue other) {
        if (!this.isNumeric() || !other.isNumeric()) {
            throw new IllegalArgumentException("Both values must be numeric for comparison");
        }
        return Double.compare(this.getNumericValue(), other.getNumericValue());
    }
    
    /**
     * Check if numeric value is greater than threshold
     */
    public boolean isGreaterThan(double threshold) {
        if (!isNumeric()) {
            throw new IllegalStateException("Cannot compare non-numeric value to threshold");
        }
        return getNumericValue() > threshold;
    }
    
    /**
     * Check if numeric value is less than threshold
     */
    public boolean isLessThan(double threshold) {
        if (!isNumeric()) {
            throw new IllegalStateException("Cannot compare non-numeric value to threshold");
        }
        return getNumericValue() < threshold;
    }
    
    /**
     * Check if numeric value is within range
     */
    public boolean isInRange(double min, double max) {
        if (!isNumeric()) {
            throw new IllegalStateException("Cannot check range for non-numeric value");
        }
        double val = getNumericValue();
        return val >= min && val <= max;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricValue that = (MetricValue) o;
        return Objects.equals(value, that.value) && type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }
    
    @Override
    public String toString() {
        return "MetricValue{" +
                "value=" + value +
                ", type=" + type +
                '}';
    }
}

/**
 * Enumeration of metric value types
 */
enum MetricType {
    NUMERIC,
    STRING,
    BOOLEAN
}