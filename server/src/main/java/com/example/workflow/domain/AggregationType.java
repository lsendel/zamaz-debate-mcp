package com.example.workflow.domain;

/**
 * Enumeration of telemetry data aggregation types
 */
public enum AggregationType {
    /**
     * No aggregation - raw data
     */
    NONE,
    
    /**
     * Average values over time window
     */
    AVERAGE,
    
    /**
     * Sum values over time window
     */
    SUM,
    
    /**
     * Minimum value over time window
     */
    MIN,
    
    /**
     * Maximum value over time window
     */
    MAX,
    
    /**
     * Count of data points over time window
     */
    COUNT,
    
    /**
     * First value in time window
     */
    FIRST,
    
    /**
     * Last value in time window
     */
    LAST,
    
    /**
     * Standard deviation over time window
     */
    STDDEV,
    
    /**
     * Median value over time window
     */
    MEDIAN,
    
    /**
     * 95th percentile over time window
     */
    P95,
    
    /**
     * 99th percentile over time window
     */
    P99
}