package com.example.workflow.domain;

/**
 * Enumeration of workflow edge types
 */
public enum EdgeType {
    /**
     * Default edge type
     */
    DEFAULT,
    
    /**
     * Conditional edge (true branch)
     */
    CONDITIONAL_TRUE,
    
    /**
     * Conditional edge (false branch)
     */
    CONDITIONAL_FALSE,
    
    /**
     * Success path edge
     */
    SUCCESS,
    
    /**
     * Error path edge
     */
    ERROR,
    
    /**
     * Data flow edge
     */
    DATA_FLOW,
    
    /**
     * Control flow edge
     */
    CONTROL_FLOW
}