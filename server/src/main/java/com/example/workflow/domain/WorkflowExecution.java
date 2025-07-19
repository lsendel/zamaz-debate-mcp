package com.example.workflow.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Workflow execution domain entity
 * Represents a single execution instance of a workflow
 */
public class WorkflowExecution {
    
    private final ExecutionId id;
    private final WorkflowId workflowId;
    private final String organizationId;
    private ExecutionStatus status;
    private NodeId currentNodeId;
    private final Instant startedAt;
    private Instant completedAt;
    private final TelemetryData triggerData;
    private String errorMessage;
    private ExecutionContext context;
    
    public WorkflowExecution(ExecutionId id, WorkflowId workflowId, String organizationId, 
                           TelemetryData triggerData) {
        this.id = Objects.requireNonNull(id, "Execution ID cannot be null");
        this.workflowId = Objects.requireNonNull(workflowId, "Workflow ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.triggerData = triggerData; // Can be null for manual executions
        this.status = ExecutionStatus.RUNNING;
        this.startedAt = Instant.now();
        this.context = new ExecutionContext();
    }
    
    /**
     * Move execution to next node
     */
    public void moveToNode(NodeId nodeId) {
        if (status != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot move to node when execution is not running: " + status);
        }
        this.currentNodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
    }
    
    /**
     * Complete execution successfully
     */
    public void complete() {
        if (status != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete execution that is not running: " + status);
        }
        this.status = ExecutionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }
    
    /**
     * Fail execution with error message
     */
    public void fail(String errorMessage) {
        if (status == ExecutionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail execution that is already completed");
        }
        this.status = ExecutionStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
    }
    
    /**
     * Pause execution
     */
    public void pause() {
        if (status != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot pause execution that is not running: " + status);
        }
        this.status = ExecutionStatus.PAUSED;
    }
    
    /**
     * Resume execution
     */
    public void resume() {
        if (status != ExecutionStatus.PAUSED) {
            throw new IllegalStateException("Cannot resume execution that is not paused: " + status);
        }
        this.status = ExecutionStatus.RUNNING;
    }
    
    /**
     * Cancel execution
     */
    public void cancel() {
        if (status == ExecutionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel execution that is already completed");
        }
        this.status = ExecutionStatus.CANCELLED;
        this.completedAt = Instant.now();
    }
    
    /**
     * Get execution duration in seconds
     */
    public long getDurationSeconds() {
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return endTime.getEpochSecond() - startedAt.getEpochSecond();
    }
    
    /**
     * Check if execution is active (running or paused)
     */
    public boolean isActive() {
        return status == ExecutionStatus.RUNNING || status == ExecutionStatus.PAUSED;
    }
    
    /**
     * Check if execution is finished (completed, failed, or cancelled)
     */
    public boolean isFinished() {
        return status == ExecutionStatus.COMPLETED || 
               status == ExecutionStatus.FAILED || 
               status == ExecutionStatus.CANCELLED;
    }
    
    /**
     * Check if execution was triggered by telemetry data
     */
    public boolean isTriggeredByTelemetry() {
        return triggerData != null;
    }
    
    /**
     * Set execution context data
     */
    public void setContextData(String key, Object value) {
        this.context.setData(key, value);
    }
    
    /**
     * Get execution context data
     */
    public Object getContextData(String key) {
        return this.context.getData(key);
    }
    
    // Getters
    public ExecutionId getId() { return id; }
    public WorkflowId getWorkflowId() { return workflowId; }
    public String getOrganizationId() { return organizationId; }
    public ExecutionStatus getStatus() { return status; }
    public NodeId getCurrentNodeId() { return currentNodeId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public TelemetryData getTriggerData() { return triggerData; }
    public String getErrorMessage() { return errorMessage; }
    public ExecutionContext getContext() { return context; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowExecution that = (WorkflowExecution) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowExecution{" +
                "id=" + id +
                ", workflowId=" + workflowId +
                ", status=" + status +
                ", currentNodeId=" + currentNodeId +
                ", startedAt=" + startedAt +
                ", duration=" + getDurationSeconds() + "s" +
                '}';
    }
}