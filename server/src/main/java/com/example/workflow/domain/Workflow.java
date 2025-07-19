package com.example.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Workflow domain entity
 * Core business object representing a workflow
 */
public class Workflow {
    
    private final WorkflowId id;
    private String name;
    private final String organizationId;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private WorkflowStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    
    public Workflow(WorkflowId id, String name, String organizationId, 
                   List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        this.id = Objects.requireNonNull(id, "Workflow ID cannot be null");
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.nodes = Objects.requireNonNull(nodes, "Nodes cannot be null");
        this.edges = Objects.requireNonNull(edges, "Edges cannot be null");
        this.status = WorkflowStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        
        validateWorkflow();
    }
    
    /**
     * Update workflow structure
     */
    public void updateStructure(String newName, List<WorkflowNode> newNodes, List<WorkflowEdge> newEdges) {
        this.name = Objects.requireNonNull(newName, "Workflow name cannot be null");
        this.nodes = Objects.requireNonNull(newNodes, "Nodes cannot be null");
        this.edges = Objects.requireNonNull(newEdges, "Edges cannot be null");
        this.updatedAt = Instant.now();
        
        validateWorkflow();
    }
    
    /**
     * Activate workflow for execution
     */
    public void activate() {
        if (status == WorkflowStatus.DRAFT) {
            validateWorkflow();
            this.status = WorkflowStatus.ACTIVE;
            this.updatedAt = Instant.now();
        } else {
            throw new IllegalStateException("Cannot activate workflow in status: " + status);
        }
    }
    
    /**
     * Pause workflow execution
     */
    public void pause() {
        if (status == WorkflowStatus.ACTIVE) {
            this.status = WorkflowStatus.PAUSED;
            this.updatedAt = Instant.now();
        } else {
            throw new IllegalStateException("Cannot pause workflow in status: " + status);
        }
    }
    
    /**
     * Resume workflow execution
     */
    public void resume() {
        if (status == WorkflowStatus.PAUSED) {
            this.status = WorkflowStatus.ACTIVE;
            this.updatedAt = Instant.now();
        } else {
            throw new IllegalStateException("Cannot resume workflow in status: " + status);
        }
    }
    
    /**
     * Complete workflow execution
     */
    public void complete() {
        if (status == WorkflowStatus.ACTIVE || status == WorkflowStatus.PAUSED) {
            this.status = WorkflowStatus.COMPLETED;
            this.updatedAt = Instant.now();
        } else {
            throw new IllegalStateException("Cannot complete workflow in status: " + status);
        }
    }
    
    /**
     * Get start nodes (nodes with no incoming edges)
     */
    public List<WorkflowNode> getStartNodes() {
        return nodes.stream()
            .filter(node -> edges.stream().noneMatch(edge -> edge.getTarget().equals(node.getId())))
            .toList();
    }
    
    /**
     * Get end nodes (nodes with no outgoing edges)
     */
    public List<WorkflowNode> getEndNodes() {
        return nodes.stream()
            .filter(node -> edges.stream().noneMatch(edge -> edge.getSource().equals(node.getId())))
            .toList();
    }
    
    /**
     * Get next nodes for a given node
     */
    public List<WorkflowNode> getNextNodes(NodeId nodeId) {
        return edges.stream()
            .filter(edge -> edge.getSource().equals(nodeId))
            .map(edge -> findNodeById(edge.getTarget()))
            .filter(Objects::nonNull)
            .toList();
    }
    
    /**
     * Find node by ID
     */
    public WorkflowNode findNodeById(NodeId nodeId) {
        return nodes.stream()
            .filter(node -> node.getId().equals(nodeId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Validate workflow structure
     */
    private void validateWorkflow() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name cannot be empty");
        }
        
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one node");
        }
        
        // Validate that all edge references point to existing nodes
        for (WorkflowEdge edge : edges) {
            if (findNodeById(edge.getSource()) == null) {
                throw new IllegalArgumentException("Edge references non-existent source node: " + edge.getSource());
            }
            if (findNodeById(edge.getTarget()) == null) {
                throw new IllegalArgumentException("Edge references non-existent target node: " + edge.getTarget());
            }
        }
        
        // Validate that there's at least one start node
        if (getStartNodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one start node");
        }
    }
    
    // Getters
    public WorkflowId getId() { return id; }
    public String getName() { return name; }
    public String getOrganizationId() { return organizationId; }
    public List<WorkflowNode> getNodes() { return List.copyOf(nodes); }
    public List<WorkflowEdge> getEdges() { return List.copyOf(edges); }
    public WorkflowStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workflow workflow = (Workflow) o;
        return Objects.equals(id, workflow.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Workflow{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", status=" + status +
                ", nodeCount=" + nodes.size() +
                ", edgeCount=" + edges.size() +
                '}';
    }
}