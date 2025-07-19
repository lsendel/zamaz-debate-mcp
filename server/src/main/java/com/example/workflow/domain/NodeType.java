package com.example.workflow.domain;

/**
 * Enumeration of workflow node types
 */
public enum NodeType {
    /**
     * Start node - entry point of workflow
     */
    START,
    
    /**
     * Decision node - conditional branching
     */
    DECISION,
    
    /**
     * Task node - action or processing step
     */
    TASK,
    
    /**
     * End node - termination point of workflow
     */
    END,
    
    /**
     * Input node - data input point
     */
    INPUT,
    
    /**
     * Output node - data output point
     */
    OUTPUT,
    
    /**
     * Condition node - condition evaluation
     */
    CONDITION,
    
    /**
     * Action node - specific action execution
     */
    ACTION
}