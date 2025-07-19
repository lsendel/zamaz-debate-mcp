package com.example.workflow.domain;

import java.util.Objects;

/**
 * Workflow edge domain entity
 * Represents a connection between two workflow nodes
 */
public class WorkflowEdge {
    
    private final EdgeId id;
    private final NodeId source;
    private final NodeId target;
    private String label;
    private EdgeType type;
    
    public WorkflowEdge(EdgeId id, NodeId source, NodeId target) {
        this.id = Objects.requireNonNull(id, "Edge ID cannot be null");
        this.source = Objects.requireNonNull(source, "Source node ID cannot be null");
        this.target = Objects.requireNonNull(target, "Target node ID cannot be null");
        this.label = "";
        this.type = EdgeType.DEFAULT;
        
        if (source.equals(target)) {
            throw new IllegalArgumentException("Edge cannot connect a node to itself");
        }
    }
    
    public WorkflowEdge(EdgeId id, NodeId source, NodeId target, String label, EdgeType type) {
        this.id = Objects.requireNonNull(id, "Edge ID cannot be null");
        this.source = Objects.requireNonNull(source, "Source node ID cannot be null");
        this.target = Objects.requireNonNull(target, "Target node ID cannot be null");
        this.label = Objects.requireNonNull(label, "Edge label cannot be null");
        this.type = Objects.requireNonNull(type, "Edge type cannot be null");
        
        if (source.equals(target)) {
            throw new IllegalArgumentException("Edge cannot connect a node to itself");
        }
    }
    
    /**
     * Update edge label
     */
    public void updateLabel(String newLabel) {
        this.label = Objects.requireNonNull(newLabel, "Edge label cannot be null");
    }
    
    /**
     * Update edge type
     */
    public void updateType(EdgeType newType) {
        this.type = Objects.requireNonNull(newType, "Edge type cannot be null");
    }
    
    /**
     * Check if this edge connects the specified nodes
     */
    public boolean connects(NodeId sourceId, NodeId targetId) {
        return this.source.equals(sourceId) && this.target.equals(targetId);
    }
    
    /**
     * Check if this edge is connected to the specified node
     */
    public boolean isConnectedTo(NodeId nodeId) {
        return this.source.equals(nodeId) || this.target.equals(nodeId);
    }
    
    // Getters
    public EdgeId getId() { return id; }
    public NodeId getSource() { return source; }
    public NodeId getTarget() { return target; }
    public String getLabel() { return label; }
    public EdgeType getType() { return type; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowEdge that = (WorkflowEdge) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowEdge{" +
                "id=" + id +
                ", source=" + source +
                ", target=" + target +
                ", label='" + label + '\'' +
                ", type=" + type +
                '}';
    }
}