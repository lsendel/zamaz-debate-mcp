package com.example.workflow.domain;

/**
 * Enumeration of workflow statuses
 */
public enum WorkflowStatus {
    /**
     * Workflow is being created/edited
     */
    DRAFT,
    
    /**
     * Workflow is active and can execute
     */
    ACTIVE,
    
    /**
     * Workflow execution is paused
     */
    PAUSED,
    
    /**
     * Workflow execution is completed
     */
    COMPLETED,
    
    /**
     * Workflow has failed
     */
    FAILED,
    
    /**
     * Workflow is archived
     */
    ARCHIVED
}