package com.example.workflow.domain.services;

import com.example.workflow.domain.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for workflow structure and business rules
 */
public class WorkflowValidator {
    
    /**
     * Validate workflow structure and return validation result
     */
    public WorkflowValidationResult validate(Workflow workflow) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Basic validation
        validateBasicStructure(workflow, errors);
        
        // Node validation
        validateNodes(workflow, errors, warnings);
        
        // Edge validation
        validateEdges(workflow, errors, warnings);
        
        // Flow validation
        validateWorkflowFlow(workflow, errors, warnings);
        
        // Business rule validation
        validateBusinessRules(workflow, errors, warnings);
        
        boolean isValid = errors.isEmpty();
        return new WorkflowValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Validate basic workflow structure
     */
    private void validateBasicStructure(Workflow workflow, List<String> errors) {
        if (workflow.getName() == null || workflow.getName().trim().isEmpty()) {
            errors.add("Workflow name cannot be empty");
        }
        
        if (workflow.getOrganizationId() == null || workflow.getOrganizationId().trim().isEmpty()) {
            errors.add("Organization ID cannot be empty");
        }
        
        if (workflow.getNodes().isEmpty()) {
            errors.add("Workflow must have at least one node");
        }
    }
    
    /**
     * Validate workflow nodes
     */
    private void validateNodes(Workflow workflow, List<String> errors, List<String> warnings) {
        Set<NodeId> nodeIds = new HashSet<>();
        boolean hasStartNode = false;
        boolean hasEndNode = false;
        
        for (WorkflowNode node : workflow.getNodes()) {
            // Check for duplicate node IDs
            if (nodeIds.contains(node.getId())) {
                errors.add("Duplicate node ID: " + node.getId());
            }
            nodeIds.add(node.getId());
            
            // Validate node properties
            validateNode(node, errors, warnings);
            
            // Check for start and end nodes
            if (node.getType() == NodeType.START) {
                hasStartNode = true;
            }
            if (node.getType() == NodeType.END) {
                hasEndNode = true;
            }
        }
        
        if (!hasStartNode) {
            errors.add("Workflow must have at least one start node");
        }
        
        if (!hasEndNode) {
            warnings.add("Workflow should have at least one end node");
        }
    }
    
    /**
     * Validate individual node
     */
    private void validateNode(WorkflowNode node, List<String> errors, List<String> warnings) {
        if (node.getLabel() == null || node.getLabel().trim().isEmpty()) {
            warnings.add("Node " + node.getId() + " has no label");
        }
        
        if (node.getPosition() == null) {
            errors.add("Node " + node.getId() + " has no position");
        }
        
        // Validate node-specific configuration
        switch (node.getType()) {
            case DECISION -> validateDecisionNode(node, errors, warnings);
            case TASK -> validateTaskNode(node, errors, warnings);
            case ACTION -> validateActionNode(node, errors, warnings);
        }
    }
    
    /**
     * Validate decision node configuration
     */
    private void validateDecisionNode(WorkflowNode node, List<String> errors, List<String> warnings) {
        Object conditions = node.getConfigurationValue("conditions");
        if (conditions == null) {
            errors.add("Decision node " + node.getId() + " must have conditions configured");
        }
    }
    
    /**
     * Validate task node configuration
     */
    private void validateTaskNode(WorkflowNode node, List<String> errors, List<String> warnings) {
        Object task = node.getConfigurationValue("task");
        if (task == null) {
            warnings.add("Task node " + node.getId() + " has no task configuration");
        }
    }
    
    /**
     * Validate action node configuration
     */
    private void validateActionNode(WorkflowNode node, List<String> errors, List<String> warnings) {
        Object action = node.getConfigurationValue("action");
        if (action == null) {
            warnings.add("Action node " + node.getId() + " has no action configuration");
        }
    }
    
    /**
     * Validate workflow edges
     */
    private void validateEdges(Workflow workflow, List<String> errors, List<String> warnings) {
        Set<EdgeId> edgeIds = new HashSet<>();
        Set<NodeId> nodeIds = workflow.getNodes().stream()
            .map(WorkflowNode::getId)
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
        
        for (WorkflowEdge edge : workflow.getEdges()) {
            // Check for duplicate edge IDs
            if (edgeIds.contains(edge.getId())) {
                errors.add("Duplicate edge ID: " + edge.getId());
            }
            edgeIds.add(edge.getId());
            
            // Validate edge references
            if (!nodeIds.contains(edge.getSource())) {
                errors.add("Edge " + edge.getId() + " references non-existent source node: " + edge.getSource());
            }
            
            if (!nodeIds.contains(edge.getTarget())) {
                errors.add("Edge " + edge.getId() + " references non-existent target node: " + edge.getTarget());
            }
            
            // Validate edge type consistency
            validateEdgeType(workflow, edge, errors, warnings);
        }
    }
    
    /**
     * Validate edge type consistency
     */
    private void validateEdgeType(Workflow workflow, WorkflowEdge edge, List<String> errors, List<String> warnings) {
        WorkflowNode sourceNode = workflow.findNodeById(edge.getSource());
        if (sourceNode == null) {
            return; // Already handled in edge validation
        }
        
        // Decision nodes should have typed edges
        if (sourceNode.getType() == NodeType.DECISION) {
            if (edge.getType() == EdgeType.DEFAULT) {
                warnings.add("Decision node " + sourceNode.getId() + " should have typed edges (CONDITIONAL_TRUE/CONDITIONAL_FALSE)");
            }
        }
    }
    
    /**
     * Validate workflow flow and connectivity
     */
    private void validateWorkflowFlow(Workflow workflow, List<String> errors, List<String> warnings) {
        // Check for unreachable nodes
        Set<NodeId> reachableNodes = findReachableNodes(workflow);
        for (WorkflowNode node : workflow.getNodes()) {
            if (!reachableNodes.contains(node.getId()) && node.getType() != NodeType.START) {
                warnings.add("Node " + node.getId() + " is unreachable from start nodes");
            }
        }
        
        // Check for cycles
        if (hasCycles(workflow)) {
            warnings.add("Workflow contains cycles - this may cause infinite loops");
        }
        
        // Check for orphaned nodes
        validateOrphanedNodes(workflow, warnings);
    }
    
    /**
     * Find all nodes reachable from start nodes
     */
    private Set<NodeId> findReachableNodes(Workflow workflow) {
        Set<NodeId> reachable = new HashSet<>();
        Set<NodeId> visited = new HashSet<>();
        
        // Start DFS from all start nodes
        for (WorkflowNode startNode : workflow.getStartNodes()) {
            dfsReachable(workflow, startNode.getId(), reachable, visited);
        }
        
        return reachable;
    }
    
    /**
     * DFS to find reachable nodes
     */
    private void dfsReachable(Workflow workflow, NodeId nodeId, Set<NodeId> reachable, Set<NodeId> visited) {
        if (visited.contains(nodeId)) {
            return;
        }
        
        visited.add(nodeId);
        reachable.add(nodeId);
        
        // Visit all connected nodes
        for (WorkflowNode nextNode : workflow.getNextNodes(nodeId)) {
            dfsReachable(workflow, nextNode.getId(), reachable, visited);
        }
    }
    
    /**
     * Check for cycles in workflow
     */
    private boolean hasCycles(Workflow workflow) {
        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> recursionStack = new HashSet<>();
        
        for (WorkflowNode node : workflow.getNodes()) {
            if (!visited.contains(node.getId())) {
                if (dfsCycleDetection(workflow, node.getId(), visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * DFS for cycle detection
     */
    private boolean dfsCycleDetection(Workflow workflow, NodeId nodeId, Set<NodeId> visited, Set<NodeId> recursionStack) {
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        for (WorkflowNode nextNode : workflow.getNextNodes(nodeId)) {
            if (!visited.contains(nextNode.getId())) {
                if (dfsCycleDetection(workflow, nextNode.getId(), visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(nextNode.getId())) {
                return true; // Back edge found - cycle detected
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }
    
    /**
     * Validate orphaned nodes (nodes with no connections)
     */
    private void validateOrphanedNodes(Workflow workflow, List<String> warnings) {
        for (WorkflowNode node : workflow.getNodes()) {
            boolean hasIncoming = workflow.getEdges().stream()
                .anyMatch(edge -> edge.getTarget().equals(node.getId()));
            boolean hasOutgoing = workflow.getEdges().stream()
                .anyMatch(edge -> edge.getSource().equals(node.getId()));
            
            if (!hasIncoming && !hasOutgoing && node.getType() != NodeType.START) {
                warnings.add("Node " + node.getId() + " has no connections");
            }
        }
    }
    
    /**
     * Validate business rules
     */
    private void validateBusinessRules(Workflow workflow, List<String> errors, List<String> warnings) {
        // Rule: Start nodes should not have incoming edges
        for (WorkflowNode node : workflow.getNodes()) {
            if (node.getType() == NodeType.START) {
                boolean hasIncoming = workflow.getEdges().stream()
                    .anyMatch(edge -> edge.getTarget().equals(node.getId()));
                if (hasIncoming) {
                    warnings.add("Start node " + node.getId() + " should not have incoming edges");
                }
            }
        }
        
        // Rule: End nodes should not have outgoing edges
        for (WorkflowNode node : workflow.getNodes()) {
            if (node.getType() == NodeType.END) {
                boolean hasOutgoing = workflow.getEdges().stream()
                    .anyMatch(edge -> edge.getSource().equals(node.getId()));
                if (hasOutgoing) {
                    warnings.add("End node " + node.getId() + " should not have outgoing edges");
                }
            }
        }
        
        // Rule: Decision nodes should have at least two outgoing edges
        for (WorkflowNode node : workflow.getNodes()) {
            if (node.getType() == NodeType.DECISION) {
                long outgoingCount = workflow.getEdges().stream()
                    .filter(edge -> edge.getSource().equals(node.getId()))
                    .count();
                if (outgoingCount < 2) {
                    warnings.add("Decision node " + node.getId() + " should have at least two outgoing edges");
                }
            }
        }
    }
}