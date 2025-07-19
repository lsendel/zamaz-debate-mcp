package com.example.workflow.domain;

/**
 * Enumeration of workflow execution statuses
 */
public enum ExecutionStatus {
    /**
     * Execution is currently running
     */
    RUNNING,
    
    /**
     * Execution is paused
     */
    PAUSED,
    
    /**
     * Execution completed successfully
     */
    COMPLETED,
    
    /**
     * Execution failed with error
     */
    FAILED,
    
    /**
     * Execution was cancelled
     */
    CANCELLED,
    
    /**
     * Execution is waiting for external input
     */
    WAITING
}