package com.example.workflow.infrastructure.neo4j;

import org.springframework.data.neo4j.core.schema.*;
import java.util.Map;
import java.util.Objects;

/**
 * Neo4j entity for WorkflowEdge
 * Represents connections between workflow nodes
 */
@RelationshipProperties
public class WorkflowEdgeEntity {
    
    @Id
    private String id;
    
    @Property("type")
    private String type;
    
    @Property("label")
    private String label;
    
    @Property("condition")
    private String condition;
    
    @Property("properties")
    private Map<String, Object> properties;
    
    @Property("workflowId")
    private String workflowId;
    
    @TargetNode
    private WorkflowNodeEntity targetNode;
    
    // Default constructor for Neo4j
    public WorkflowEdgeEntity() {}
    
    public WorkflowEdgeEntity(String id, String type, String label, String condition,
                             Map<String, Object> properties, String workflowId,
                             WorkflowNodeEntity targetNode) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.condition = condition;
        this.properties = properties;
        this.workflowId = workflowId;
        this.targetNode = targetNode;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    
    public WorkflowNodeEntity getTargetNode() { return targetNode; }
    public void setTargetNode(WorkflowNodeEntity targetNode) { this.targetNode = targetNode; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowEdgeEntity that = (WorkflowEdgeEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowEdgeEntity{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", label='" + label + '\'' +
                ", workflowId='" + workflowId + '\'' +
                '}';
    }
}