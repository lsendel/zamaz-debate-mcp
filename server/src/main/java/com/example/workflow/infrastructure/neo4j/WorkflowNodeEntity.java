package com.example.workflow.infrastructure.neo4j;

import org.springframework.data.neo4j.core.schema.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Neo4j entity for WorkflowNode
 * Represents individual nodes within a workflow graph with proper relationship mappings
 */
@Node("WorkflowNode")
public class WorkflowNodeEntity {
    
    @Id
    private String id;
    
    @Property("type")
    private String type;
    
    @Property("name")
    private String name;
    
    @Property("description")
    private String description;
    
    @Property("configuration")
    private Map<String, Object> configuration;
    
    @Property("positionX")
    private Double positionX;
    
    @Property("positionY")
    private Double positionY;
    
    @Property("workflowId")
    private String workflowId;
    
    @Property("label")
    private String label;
    
    @Property("isStartNode")
    private Boolean isStartNode;
    
    @Property("isEndNode")
    private Boolean isEndNode;
    
    // Outgoing relationships to other nodes
    @Relationship(type = "CONNECTS_TO", direction = Relationship.Direction.OUTGOING)
    private List<WorkflowEdgeEntity> outgoingEdges;
    
    // Incoming relationships from other nodes
    @Relationship(type = "CONNECTS_TO", direction = Relationship.Direction.INCOMING)
    private List<WorkflowEdgeEntity> incomingEdges;
    
    // Default constructor for Neo4j
    public WorkflowNodeEntity() {}
    
    public WorkflowNodeEntity(String id, String type, String name, String description,
                             Map<String, Object> configuration, Double positionX, Double positionY,
                             String workflowId, String label, Boolean isStartNode, Boolean isEndNode) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.configuration = configuration;
        this.positionX = positionX;
        this.positionY = positionY;
        this.workflowId = workflowId;
        this.label = label;
        this.isStartNode = isStartNode;
        this.isEndNode = isEndNode;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
    
    public Double getPositionX() { return positionX; }
    public void setPositionX(Double positionX) { this.positionX = positionX; }
    
    public Double getPositionY() { return positionY; }
    public void setPositionY(Double positionY) { this.positionY = positionY; }
    
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public Boolean getIsStartNode() { return isStartNode; }
    public void setIsStartNode(Boolean isStartNode) { this.isStartNode = isStartNode; }
    
    public Boolean getIsEndNode() { return isEndNode; }
    public void setIsEndNode(Boolean isEndNode) { this.isEndNode = isEndNode; }
    
    public List<WorkflowEdgeEntity> getOutgoingEdges() { return outgoingEdges; }
    public void setOutgoingEdges(List<WorkflowEdgeEntity> outgoingEdges) { this.outgoingEdges = outgoingEdges; }
    
    public List<WorkflowEdgeEntity> getIncomingEdges() { return incomingEdges; }
    public void setIncomingEdges(List<WorkflowEdgeEntity> incomingEdges) { this.incomingEdges = incomingEdges; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowNodeEntity that = (WorkflowNodeEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowNodeEntity{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", isStartNode=" + isStartNode +
                ", isEndNode=" + isEndNode +
                '}';
    }
}