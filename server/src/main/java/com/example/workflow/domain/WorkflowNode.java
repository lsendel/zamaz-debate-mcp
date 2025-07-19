package com.example.workflow.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Workflow node domain entity
 * Represents a single node in a workflow
 */
public class WorkflowNode {
    
    private final NodeId id;
    private final NodeType type;
    private String label;
    private Position position;
    private Map<String, Object> configuration;
    
    public WorkflowNode(NodeId id, NodeType type, String label, Position position) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.label = Objects.requireNonNull(label, "Node label cannot be null");
        this.position = Objects.requireNonNull(position, "Node position cannot be null");
        this.configuration = Map.of();
    }
    
    public WorkflowNode(NodeId id, NodeType type, String label, Position position, Map<String, Object> configuration) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.label = Objects.requireNonNull(label, "Node label cannot be null");
        this.position = Objects.requireNonNull(position, "Node position cannot be null");
        this.configuration = Objects.requireNonNull(configuration, "Node configuration cannot be null");
    }
    
    /**
     * Update node label
     */
    public void updateLabel(String newLabel) {
        this.label = Objects.requireNonNull(newLabel, "Node label cannot be null");
    }
    
    /**
     * Update node position
     */
    public void updatePosition(Position newPosition) {
        this.position = Objects.requireNonNull(newPosition, "Node position cannot be null");
    }
    
    /**
     * Update node configuration
     */
    public void updateConfiguration(Map<String, Object> newConfiguration) {
        this.configuration = Objects.requireNonNull(newConfiguration, "Node configuration cannot be null");
    }
    
    /**
     * Get configuration value
     */
    public Object getConfigurationValue(String key) {
        return configuration.get(key);
    }
    
    /**
     * Set configuration value
     */
    public void setConfigurationValue(String key, Object value) {
        this.configuration.put(key, value);
    }
    
    /**
     * Check if node is a start node type
     */
    public boolean isStartNode() {
        return type == NodeType.START;
    }
    
    /**
     * Check if node is an end node type
     */
    public boolean isEndNode() {
        return type == NodeType.END;
    }
    
    /**
     * Check if node is a decision node type
     */
    public boolean isDecisionNode() {
        return type == NodeType.DECISION;
    }
    
    /**
     * Check if node is a task node type
     */
    public boolean isTaskNode() {
        return type == NodeType.TASK;
    }
    
    // Getters
    public NodeId getId() { return id; }
    public NodeType getType() { return type; }
    public String getLabel() { return label; }
    public Position getPosition() { return position; }
    public Map<String, Object> getConfiguration() { return Map.copyOf(configuration); }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowNode that = (WorkflowNode) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowNode{" +
                "id=" + id +
                ", type=" + type +
                ", label='" + label + '\'' +
                ", position=" + position +
                '}';
    }
}