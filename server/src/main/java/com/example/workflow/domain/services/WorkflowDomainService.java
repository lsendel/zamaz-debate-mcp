package com.example.workflow.domain.services;

import com.example.workflow.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain service for workflow business logic
 * Contains core workflow operations and validation
 * Implements requirements 1.1, 1.3, 3.5, 3.6 for workflow creation, validation, and execution
 */
@Service
public class WorkflowDomainService {
    
    private final ConditionEvaluationService conditionEvaluationService;
    private final WorkflowValidator workflowValidator;
    
    public WorkflowDomainService(ConditionEvaluationService conditionEvaluationService) {
        this.conditionEvaluationService = Objects.requireNonNull(conditionEvaluationService);
        this.workflowValidator = new WorkflowValidator();
    }
    
    /**
     * Create a new workflow with validation
     * Implements requirement 1.1: Support for workflow creation with drag-and-drop capabilities
     */
    public Workflow createWorkflow(String name, String organizationId, 
                                 List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Objects.requireNonNull(name, "Workflow name cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(nodes, "Nodes cannot be null");
        Objects.requireNonNull(edges, "Edges cannot be null");
        
        WorkflowId workflowId = WorkflowId.generate();
        Workflow workflow = new Workflow(workflowId, name, organizationId, nodes, edges);
        
        // Validate workflow structure and connections (requirement 1.3)
        validateWorkflowStructure(workflow);
        return workflow;
    }
    
    /**
     * Validate node connection compatibility
     * Implements requirement 1.3: Validate connections and provide visual feedback
     */
    public ConnectionValidationResult validateConnection(WorkflowNode sourceNode, WorkflowNode targetNode, EdgeType edgeType) {
        Objects.requireNonNull(sourceNode, "Source node cannot be null");
        Objects.requireNonNull(targetNode, "Target node cannot be null");
        Objects.requireNonNull(edgeType, "Edge type cannot be null");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Basic connection rules
        if (sourceNode.equals(targetNode)) {
            errors.add("Cannot connect node to itself");
        }
        
        // Start nodes should not have incoming connections
        if (targetNode.getType() == NodeType.START) {
            errors.add("Start nodes cannot have incoming connections");
        }
        
        // End nodes should not have outgoing connections
        if (sourceNode.getType() == NodeType.END) {
            errors.add("End nodes cannot have outgoing connections");
        }
        
        // Decision nodes should use typed edges
        if (sourceNode.getType() == NodeType.DECISION && edgeType == EdgeType.DEFAULT) {
            warnings.add("Decision nodes should use CONDITIONAL_TRUE or CONDITIONAL_FALSE edge types");
        }
        
        // Condition nodes should use typed edges
        if (sourceNode.getType() == NodeType.CONDITION && edgeType == EdgeType.DEFAULT) {
            warnings.add("Condition nodes should use CONDITIONAL_TRUE or CONDITIONAL_FALSE edge types");
        }
        
        boolean isValid = errors.isEmpty();
        return new ConnectionValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Execute workflow with telemetry data
     */
    public WorkflowExecution executeWorkflow(Workflow workflow, TelemetryData telemetryData) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Cannot execute workflow that is not active: " + workflow.getStatus());
        }
        
        ExecutionId executionId = ExecutionId.generate();
        WorkflowExecution execution = new WorkflowExecution(
            executionId, 
            workflow.getId(), 
            workflow.getOrganizationId(), 
            telemetryData
        );
        
        // Start execution from start nodes
        List<WorkflowNode> startNodes = workflow.getStartNodes();
        if (startNodes.isEmpty()) {
            execution.fail("No start nodes found in workflow");
            return execution;
        }
        
        // Execute first start node
        WorkflowNode firstStartNode = startNodes.get(0);
        execution.moveToNode(firstStartNode.getId());
        
        return processNode(workflow, execution, firstStartNode, telemetryData);
    }
    
    /**
     * Process a single workflow node
     */
    public WorkflowExecution processNode(Workflow workflow, WorkflowExecution execution, 
                                       WorkflowNode node, TelemetryData telemetryData) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        Objects.requireNonNull(execution, "Execution cannot be null");
        Objects.requireNonNull(node, "Node cannot be null");
        
        try {
            switch (node.getType()) {
                case START -> processStartNode(workflow, execution, node, telemetryData);
                case DECISION -> processDecisionNode(workflow, execution, node, telemetryData);
                case TASK -> processTaskNode(workflow, execution, node, telemetryData);
                case ACTION -> processActionNode(workflow, execution, node, telemetryData);
                case END -> processEndNode(workflow, execution, node, telemetryData);
                default -> throw new IllegalArgumentException("Unsupported node type: " + node.getType());
            }
        } catch (Exception e) {
            execution.fail("Error processing node " + node.getId() + ": " + e.getMessage());
        }
        
        return execution;
    }
    
    /**
     * Process start node
     */
    private void processStartNode(Workflow workflow, WorkflowExecution execution, 
                                WorkflowNode node, TelemetryData telemetryData) {
        // Start nodes just pass through to next nodes
        List<WorkflowNode> nextNodes = workflow.getNextNodes(node.getId());
        if (!nextNodes.isEmpty()) {
            WorkflowNode nextNode = nextNodes.get(0);
            execution.moveToNode(nextNode.getId());
            processNode(workflow, execution, nextNode, telemetryData);
        } else {
            execution.complete();
        }
    }
    
    /**
     * Process decision node with condition evaluation
     * Implements requirements 3.5 and 3.6: Evaluate conditions against real-time data and route execution
     */
    private void processDecisionNode(Workflow workflow, WorkflowExecution execution, 
                                   WorkflowNode node, TelemetryData telemetryData) {
        // Get conditions from node configuration
        Object conditionsConfig = node.getConfigurationValue("conditions");
        if (conditionsConfig == null) {
            execution.fail("Decision node " + node.getId() + " has no conditions configured");
            return;
        }
        
        try {
            // Requirement 3.5: Evaluate conditions against real-time telemetry data
            boolean conditionResult = conditionEvaluationService.evaluateConditions(conditionsConfig, telemetryData);
            
            // Store condition evaluation result in execution context
            execution.setContextData("condition_result_" + node.getId(), conditionResult);
            execution.setContextData("condition_evaluation_time", System.currentTimeMillis());
            
            // Requirement 3.6: Route workflow execution based on condition result
            WorkflowNode nextNode = findNextNodeForCondition(workflow, node, conditionResult);
            
            if (nextNode != null) {
                execution.setContextData("routing_decision", 
                    "Condition " + (conditionResult ? "TRUE" : "FALSE") + " -> Node " + nextNode.getId());
                execution.moveToNode(nextNode.getId());
                processNode(workflow, execution, nextNode, telemetryData);
            } else {
                execution.setContextData("routing_decision", "No next node found for condition result: " + conditionResult);
                execution.complete();
            }
        } catch (Exception e) {
            execution.fail("Error evaluating conditions in decision node " + node.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Process task node
     */
    private void processTaskNode(Workflow workflow, WorkflowExecution execution, 
                               WorkflowNode node, TelemetryData telemetryData) {
        // Execute task logic based on node configuration
        Object taskConfig = node.getConfigurationValue("task");
        if (taskConfig != null) {
            execution.setContextData("task_result", executeTask(taskConfig, telemetryData));
        }
        
        // Move to next node
        List<WorkflowNode> nextNodes = workflow.getNextNodes(node.getId());
        if (!nextNodes.isEmpty()) {
            WorkflowNode nextNode = nextNodes.get(0);
            execution.moveToNode(nextNode.getId());
            processNode(workflow, execution, nextNode, telemetryData);
        } else {
            execution.complete();
        }
    }
    
    /**
     * Process action node
     */
    private void processActionNode(Workflow workflow, WorkflowExecution execution, 
                                 WorkflowNode node, TelemetryData telemetryData) {
        // Execute action based on node configuration
        Object actionConfig = node.getConfigurationValue("action");
        if (actionConfig != null) {
            execution.setContextData("action_result", executeAction(actionConfig, telemetryData));
        }
        
        // Move to next node
        List<WorkflowNode> nextNodes = workflow.getNextNodes(node.getId());
        if (!nextNodes.isEmpty()) {
            WorkflowNode nextNode = nextNodes.get(0);
            execution.moveToNode(nextNode.getId());
            processNode(workflow, execution, nextNode, telemetryData);
        } else {
            execution.complete();
        }
    }
    
    /**
     * Process end node
     */
    private void processEndNode(Workflow workflow, WorkflowExecution execution, 
                              WorkflowNode node, TelemetryData telemetryData) {
        // End nodes complete the execution
        execution.complete();
    }
    
    /**
     * Find next node based on condition result
     */
    private WorkflowNode findNextNodeForCondition(Workflow workflow, WorkflowNode decisionNode, boolean conditionResult) {
        List<WorkflowEdge> outgoingEdges = workflow.getEdges().stream()
            .filter(edge -> edge.getSource().equals(decisionNode.getId()))
            .toList();
        
        // Look for edges with appropriate types
        EdgeType targetEdgeType = conditionResult ? EdgeType.CONDITIONAL_TRUE : EdgeType.CONDITIONAL_FALSE;
        
        for (WorkflowEdge edge : outgoingEdges) {
            if (edge.getType() == targetEdgeType) {
                return workflow.findNodeById(edge.getTarget());
            }
        }
        
        // Fallback to first available edge if no typed edges found
        if (!outgoingEdges.isEmpty()) {
            return workflow.findNodeById(outgoingEdges.get(0).getTarget());
        }
        
        return null;
    }
    
    /**
     * Execute task logic
     */
    private Object executeTask(Object taskConfig, TelemetryData telemetryData) {
        // Placeholder for task execution logic
        // In real implementation, this would delegate to specific task handlers
        return "Task executed with config: " + taskConfig;
    }
    
    /**
     * Execute action logic
     */
    private Object executeAction(Object actionConfig, TelemetryData telemetryData) {
        // Placeholder for action execution logic
        // In real implementation, this would delegate to specific action handlers
        return "Action executed with config: " + actionConfig;
    }
    

    
    /**
     * Validate workflow structure (internal)
     * Implements requirement 1.3: Validate connections and provide feedback
     */
    private void validateWorkflowStructure(Workflow workflow) {
        WorkflowValidationResult result = validateWorkflow(workflow);
        if (!result.isValid()) {
            throw new WorkflowValidationException("Invalid workflow structure: " + String.join(", ", result.getErrors()), result);
        }
    }
    
    /**
     * Validate workflow structure and return detailed result
     * Implements requirement 1.3: Provide validation feedback for connections
     */
    public WorkflowValidationResult validateWorkflow(Workflow workflow) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        return workflowValidator.validate(workflow);
    }
    
    /**
     * Validate workflow for real-time execution readiness
     * Ensures workflow can handle telemetry data processing
     */
    public ExecutionReadinessResult validateExecutionReadiness(Workflow workflow) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check workflow status
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            issues.add("Workflow must be in ACTIVE status for execution");
        }
        
        // Check for decision nodes with proper conditions
        for (WorkflowNode node : workflow.getNodes()) {
            if (node.getType() == NodeType.DECISION || node.getType() == NodeType.CONDITION) {
                Object conditions = node.getConfigurationValue("conditions");
                if (conditions == null) {
                    issues.add("Decision/Condition node " + node.getId() + " has no conditions configured");
                }
            }
        }
        
        // Check for proper edge routing from decision nodes
        for (WorkflowNode node : workflow.getNodes()) {
            if (node.getType() == NodeType.DECISION) {
                List<WorkflowEdge> outgoingEdges = workflow.getEdges().stream()
                    .filter(edge -> edge.getSource().equals(node.getId()))
                    .toList();
                
                boolean hasTrueEdge = outgoingEdges.stream()
                    .anyMatch(edge -> edge.getType() == EdgeType.CONDITIONAL_TRUE);
                boolean hasFalseEdge = outgoingEdges.stream()
                    .anyMatch(edge -> edge.getType() == EdgeType.CONDITIONAL_FALSE);
                
                if (!hasTrueEdge && !hasFalseEdge) {
                    warnings.add("Decision node " + node.getId() + " should have typed conditional edges");
                }
            }
        }
        
        boolean isReady = issues.isEmpty();
        return new ExecutionReadinessResult(isReady, issues, warnings);
    }
    
    /**
     * Check if workflow can be executed with given telemetry data
     */
    public boolean canExecuteWithTelemetry(Workflow workflow, TelemetryData telemetryData) {
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            return false;
        }
        
        if (telemetryData == null) {
            return true; // Manual execution
        }
        
        // Check if workflow organization matches telemetry organization
        return workflow.getOrganizationId().equals(telemetryData.getOrganizationId());
    }
    
    /**
     * Get workflow execution statistics
     */
    public WorkflowExecutionStats getExecutionStats(Workflow workflow, List<WorkflowExecution> executions) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        Objects.requireNonNull(executions, "Executions cannot be null");
        
        long totalExecutions = executions.size();
        long completedExecutions = executions.stream()
            .mapToLong(exec -> exec.getStatus() == ExecutionStatus.COMPLETED ? 1 : 0)
            .sum();
        long failedExecutions = executions.stream()
            .mapToLong(exec -> exec.getStatus() == ExecutionStatus.FAILED ? 1 : 0)
            .sum();
        
        double averageDuration = executions.stream()
            .filter(exec -> exec.isFinished())
            .mapToLong(WorkflowExecution::getDurationSeconds)
            .average()
            .orElse(0.0);
        
        return new WorkflowExecutionStats(
            totalExecutions,
            completedExecutions,
            failedExecutions,
            averageDuration
        );
    }
    
    /**
     * Execute workflow step-by-step for debugging and monitoring
     * Supports real-time telemetry processing requirements
     */
    public WorkflowExecution executeWorkflowStep(Workflow workflow, WorkflowExecution execution, TelemetryData telemetryData) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        Objects.requireNonNull(execution, "Execution cannot be null");
        
        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot execute step when execution is not running: " + execution.getStatus());
        }
        
        NodeId currentNodeId = execution.getCurrentNodeId();
        if (currentNodeId == null) {
            // Start execution from first start node
            List<WorkflowNode> startNodes = workflow.getStartNodes();
            if (startNodes.isEmpty()) {
                execution.fail("No start nodes found in workflow");
                return execution;
            }
            currentNodeId = startNodes.get(0).getId();
            execution.moveToNode(currentNodeId);
        }
        
        WorkflowNode currentNode = workflow.findNodeById(currentNodeId);
        if (currentNode == null) {
            execution.fail("Current node not found: " + currentNodeId);
            return execution;
        }
        
        return processNode(workflow, execution, currentNode, telemetryData);
    }
    
    /**
     * Check if workflow execution can continue with current telemetry data
     * Supports real-time processing validation
     */
    public boolean canContinueExecution(Workflow workflow, WorkflowExecution execution, TelemetryData telemetryData) {
        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            return false;
        }
        
        if (execution.getCurrentNodeId() == null) {
            return true; // Can start execution
        }
        
        WorkflowNode currentNode = workflow.findNodeById(execution.getCurrentNodeId());
        if (currentNode == null) {
            return false;
        }
        
        // Check if current node can process the telemetry data
        if (currentNode.getType() == NodeType.DECISION || currentNode.getType() == NodeType.CONDITION) {
            Object conditions = currentNode.getConfigurationValue("conditions");
            if (conditions == null) {
                return false; // Cannot evaluate without conditions
            }
        }
        
        return true;
    }
    
    /**
     * Get next possible nodes for execution planning
     * Useful for workflow visualization and execution prediction
     */
    public List<WorkflowNode> getPossibleNextNodes(Workflow workflow, NodeId currentNodeId, TelemetryData telemetryData) {
        Objects.requireNonNull(workflow, "Workflow cannot be null");
        Objects.requireNonNull(currentNodeId, "Current node ID cannot be null");
        
        WorkflowNode currentNode = workflow.findNodeById(currentNodeId);
        if (currentNode == null) {
            return List.of();
        }
        
        List<WorkflowNode> nextNodes = workflow.getNextNodes(currentNodeId);
        
        // For decision nodes, try to predict which path will be taken
        if ((currentNode.getType() == NodeType.DECISION || currentNode.getType() == NodeType.CONDITION) && telemetryData != null) {
            Object conditions = currentNode.getConfigurationValue("conditions");
            if (conditions != null) {
                try {
                    boolean conditionResult = conditionEvaluationService.evaluateConditions(conditions, telemetryData);
                    WorkflowNode predictedNext = findNextNodeForCondition(workflow, currentNode, conditionResult);
                    if (predictedNext != null) {
                        return List.of(predictedNext);
                    }
                } catch (Exception e) {
                    // If condition evaluation fails, return all possible next nodes
                }
            }
        }
        
        return nextNodes;
    }
}

/**
 * Workflow execution statistics
 */
record WorkflowExecutionStats(
    long totalExecutions,
    long completedExecutions,
    long failedExecutions,
    double averageDurationSeconds
) {
    public double getSuccessRate() {
        return totalExecutions > 0 ? (double) completedExecutions / totalExecutions : 0.0;
    }
    
    public double getFailureRate() {
        return totalExecutions > 0 ? (double) failedExecutions / totalExecutions : 0.0;
    }
}

/**
 * Workflow validation result
 */
record WorkflowValidationResult(
    boolean isValid,
    List<String> errors,
    List<String> warnings
) {
    public static WorkflowValidationResult valid() {
        return new WorkflowValidationResult(true, List.of(), List.of());
    }
    
    public static WorkflowValidationResult invalid(List<String> errors) {
        return new WorkflowValidationResult(false, errors, List.of());
    }
    
    public static WorkflowValidationResult withWarnings(List<String> warnings) {
        return new WorkflowValidationResult(true, List.of(), warnings);
    }
}

/**
 * Connection validation result for node connections
 * Supports requirement 1.3: Validate connections and provide visual feedback
 */
record ConnectionValidationResult(
    boolean isValid,
    List<String> errors,
    List<String> warnings
) {
    public static ConnectionValidationResult valid() {
        return new ConnectionValidationResult(true, List.of(), List.of());
    }
    
    public static ConnectionValidationResult invalid(List<String> errors) {
        return new ConnectionValidationResult(false, errors, List.of());
    }
    
    public static ConnectionValidationResult withWarnings(List<String> warnings) {
        return new ConnectionValidationResult(true, List.of(), warnings);
    }
}

/**
 * Execution readiness result for workflow execution validation
 */
record ExecutionReadinessResult(
    boolean isReady,
    List<String> issues,
    List<String> warnings
) {
    public static ExecutionReadinessResult ready() {
        return new ExecutionReadinessResult(true, List.of(), List.of());
    }
    
    public static ExecutionReadinessResult notReady(List<String> issues) {
        return new ExecutionReadinessResult(false, issues, List.of());
    }
    
    public static ExecutionReadinessResult readyWithWarnings(List<String> warnings) {
        return new ExecutionReadinessResult(true, List.of(), warnings);
    }
}

/**
 * Exception thrown when workflow validation fails
 */
class WorkflowValidationException extends RuntimeException {
    private final WorkflowValidationResult validationResult;
    
    public WorkflowValidationException(String message, WorkflowValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }
    
    public WorkflowValidationResult getValidationResult() {
        return validationResult;
    }
}