package com.example.workflow.graphql.ports;

import com.example.workflow.domain.Workflow;
import com.example.workflow.domain.WorkflowExecution;
import com.example.workflow.domain.WorkflowNode;
import com.example.workflow.domain.TelemetryData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Inbound port for workflow operations
 * Defines the contract for workflow business logic
 */
public interface WorkflowService {
    
    /**
     * Create a new workflow
     */
    Mono<Workflow> createWorkflow(CreateWorkflowCommand command);
    
    /**
     * Update an existing workflow
     */
    Mono<Workflow> updateWorkflow(String workflowId, UpdateWorkflowCommand command);
    
    /**
     * Get workflow by ID
     */
    Mono<Workflow> getWorkflow(String workflowId);
    
    /**
     * Get workflows by organization
     */
    Flux<Workflow> getWorkflowsByOrganization(String organizationId);
    
    /**
     * Get workflows within viewport (for large-scale visualization)
     */
    Flux<WorkflowNode> getNodesInViewport(String workflowId, ViewportInput viewport);
    
    /**
     * Execute workflow with telemetry data
     */
    Mono<WorkflowExecution> executeWorkflow(String workflowId, TelemetryData telemetryData);
    
    /**
     * Trigger specific workflow action
     */
    Mono<WorkflowExecution> triggerAction(String workflowId, String nodeId, TriggerActionInput input);
    
    /**
     * Subscribe to workflow execution events
     */
    Flux<WorkflowExecutionEvent> subscribeToWorkflowExecution(String workflowId);
    
    /**
     * Delete workflow
     */
    Mono<Boolean> deleteWorkflow(String workflowId);
    
    /**
     * Validate workflow structure
     */
    Mono<WorkflowValidationResult> validateWorkflow(String workflowId);
}

/**
 * Command objects for workflow operations
 */
record CreateWorkflowCommand(
    String name,
    String organizationId,
    List<WorkflowNodeInput> nodes,
    List<WorkflowEdgeInput> edges
) {}

record UpdateWorkflowCommand(
    String name,
    List<WorkflowNodeInput> nodes,
    List<WorkflowEdgeInput> edges
) {}

record WorkflowNodeInput(
    String id,
    String type,
    String label,
    PositionInput position,
    Object config
) {}

record WorkflowEdgeInput(
    String id,
    String source,
    String target,
    String label
) {}

record PositionInput(
    double x,
    double y
) {}

record ViewportInput(
    double x,
    double y,
    double width,
    double height,
    double zoom
) {}

record TriggerActionInput(
    String action,
    Object parameters
) {}

record WorkflowExecutionEvent(
    String workflowId,
    String executionId,
    String nodeId,
    String status,
    Object data,
    long timestamp
) {}

record WorkflowValidationResult(
    boolean valid,
    List<String> errors,
    List<String> warnings
) {}