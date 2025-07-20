package com.zamaz.workflow.domain.service;

import com.zamaz.workflow.domain.command.CreateWorkflowCommand;
import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.entity.WorkflowConnection;
import com.zamaz.workflow.domain.entity.WorkflowNode;
import com.zamaz.workflow.domain.entity.WorkflowNodeType;
import com.zamaz.workflow.domain.event.WorkflowExecutionResult;
import com.zamaz.workflow.domain.exception.WorkflowValidationException;
import com.zamaz.workflow.domain.repository.WorkflowRepository;
import com.zamaz.workflow.domain.valueobject.WorkflowId;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@RequiredArgsConstructor
public class WorkflowDomainServiceImpl implements WorkflowDomainService {
    private final WorkflowRepository workflowRepository;
    private final ConditionEvaluator conditionEvaluator;
    
    @Override
    public Workflow createWorkflow(CreateWorkflowCommand command) {
        Workflow workflow = Workflow.create(command.getName(), command.getOrganizationId());
        
        // Add default start and end nodes
        WorkflowNode startNode = WorkflowNode.createStartNode("Start");
        WorkflowNode endNode = WorkflowNode.createEndNode("End");
        
        workflow.addNode(startNode);
        workflow.addNode(endNode);
        
        return workflow;
    }
    
    @Override
    public void executeWorkflow(WorkflowId id, Object telemetryData) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException(id));
        
        if (!workflow.getStatus().canExecute()) {
            throw new WorkflowNotExecutableException(id, workflow.getStatus());
        }
        
        WorkflowNode startNode = findStartNode(workflow);
        executeFromNode(workflow, startNode, telemetryData);
    }
    
    @Override
    public WorkflowExecutionResult processNode(WorkflowNode node, Object telemetryData) {
        Instant startTime = Instant.now();
        
        try {
            Map<String, Object> outputs = new HashMap<>();
            String nextNodeId = null;
            
            switch (node.getType()) {
                case START:
                    nextNodeId = getNextNodeId(node);
                    break;
                    
                case TASK:
                    outputs = executeTask(node, telemetryData);
                    nextNodeId = getNextNodeId(node);
                    break;
                    
                case DECISION:
                    nextNodeId = evaluateDecision(node, telemetryData);
                    break;
                    
                case END:
                    // No next node for end nodes
                    break;
                    
                default:
                    throw new UnsupportedOperationException("Node type not implemented: " + node.getType());
            }
            
            return WorkflowExecutionResult.builder()
                    .nodeId(node.getId())
                    .status(WorkflowExecutionResult.ExecutionStatus.SUCCESS)
                    .outputs(outputs)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .nextNodeId(nextNodeId)
                    .build();
                    
        } catch (Exception e) {
            return WorkflowExecutionResult.builder()
                    .nodeId(node.getId())
                    .status(WorkflowExecutionResult.ExecutionStatus.FAILED)
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public void validateWorkflow(Workflow workflow) {
        List<String> errors = new ArrayList<>();
        
        // Check for start node
        long startNodes = workflow.getNodes().stream()
                .filter(node -> node.getType() == WorkflowNodeType.START)
                .count();
        if (startNodes == 0) {
            errors.add("Workflow must have at least one start node");
        } else if (startNodes > 1) {
            errors.add("Workflow cannot have more than one start node");
        }
        
        // Check for end node
        long endNodes = workflow.getNodes().stream()
                .filter(node -> node.getType() == WorkflowNodeType.END)
                .count();
        if (endNodes == 0) {
            errors.add("Workflow must have at least one end node");
        }
        
        // Check for orphaned nodes
        Set<String> connectedNodes = new HashSet<>();
        for (WorkflowConnection conn : workflow.getConnections()) {
            connectedNodes.add(conn.getSourceNodeId());
            connectedNodes.add(conn.getTargetNodeId());
        }
        
        for (WorkflowNode node : workflow.getNodes()) {
            if (!connectedNodes.contains(node.getId()) && 
                node.getType() != WorkflowNodeType.START && 
                node.getType() != WorkflowNodeType.END) {
                errors.add("Node " + node.getName() + " is not connected to the workflow");
            }
        }
        
        // Check for cycles
        if (hasCycles(workflow)) {
            errors.add("Workflow contains cycles");
        }
        
        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
    
    @Override
    public boolean canNodeConnect(WorkflowNode source, WorkflowNode target) {
        // Start nodes can only be source
        if (source.getType() == WorkflowNodeType.END) {
            return false;
        }
        
        // End nodes can only be target
        if (target.getType() == WorkflowNodeType.START) {
            return false;
        }
        
        // Decision nodes can have multiple outgoing connections
        // Other nodes can only have one outgoing connection
        // This would be enforced elsewhere
        
        return true;
    }
    
    private void executeFromNode(Workflow workflow, WorkflowNode node, Object telemetryData) {
        Queue<WorkflowNode> nodesToProcess = new LinkedList<>();
        Set<String> processedNodes = new HashSet<>();
        
        nodesToProcess.offer(node);
        
        while (!nodesToProcess.isEmpty()) {
            WorkflowNode currentNode = nodesToProcess.poll();
            
            if (processedNodes.contains(currentNode.getId())) {
                continue;
            }
            
            WorkflowExecutionResult result = processNode(currentNode, telemetryData);
            processedNodes.add(currentNode.getId());
            
            if (result.isSuccess() && result.getNextNodeId() != null) {
                workflow.findNodeById(result.getNextNodeId())
                        .ifPresent(nodesToProcess::offer);
            }
        }
    }
    
    private WorkflowNode findStartNode(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> node.getType() == WorkflowNodeType.START)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No start node found"));
    }
    
    private String getNextNodeId(WorkflowNode node) {
        // This would be implemented based on workflow connections
        // For now, returning null
        return null;
    }
    
    private Map<String, Object> executeTask(WorkflowNode node, Object telemetryData) {
        // Task execution logic would go here
        // This would interact with external systems based on node configuration
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("taskCompleted", true);
        outputs.put("timestamp", Instant.now());
        return outputs;
    }
    
    private String evaluateDecision(WorkflowNode node, Object telemetryData) {
        // Get conditions from node configuration
        Object conditions = node.getConfiguration().get("conditions");
        
        // Evaluate conditions using the condition evaluator
        boolean result = conditionEvaluator.evaluate(conditions, telemetryData);
        
        // Return appropriate next node based on condition result
        // This would need to look at the workflow connections
        return null;
    }
    
    private boolean hasCycles(Workflow workflow) {
        // Simple cycle detection using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (WorkflowNode node : workflow.getNodes()) {
            if (hasCyclesUtil(workflow, node.getId(), visited, recursionStack)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasCyclesUtil(Workflow workflow, String nodeId, Set<String> visited, Set<String> recursionStack) {
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        List<String> adjacentNodes = getAdjacentNodes(workflow, nodeId);
        
        for (String adjacent : adjacentNodes) {
            if (!visited.contains(adjacent)) {
                if (hasCyclesUtil(workflow, adjacent, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(adjacent)) {
                return true;
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }
    
    private List<String> getAdjacentNodes(Workflow workflow, String nodeId) {
        List<String> adjacent = new ArrayList<>();
        for (WorkflowConnection conn : workflow.getConnections()) {
            if (conn.getSourceNodeId().equals(nodeId)) {
                adjacent.add(conn.getTargetNodeId());
            }
        }
        return adjacent;
    }
    
    public interface ConditionEvaluator {
        boolean evaluate(Object conditions, Object data);
    }
    
    public static class WorkflowNotFoundException extends RuntimeException {
        public WorkflowNotFoundException(WorkflowId id) {
            super("Workflow not found: " + id.getValue());
        }
    }
    
    public static class WorkflowNotExecutableException extends RuntimeException {
        public WorkflowNotExecutableException(WorkflowId id, Object status) {
            super("Workflow " + id.getValue() + " is not executable in status: " + status);
        }
    }
}