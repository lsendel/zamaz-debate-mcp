package com.example.workflow.infrastructure.neo4j;

import org.springframework.data.neo4j.core.schema.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Neo4j entity for Workflow
 * Maps workflow domain object to Neo4j graph structure
 */
@Node("Workflow")
public class WorkflowEntity {
    
    @Id
    private String id;
    
    @Property("name")
    private String name;
    
    @Property("organizationId")
    private String organizationId;
    
    @Property("status")
    private String status;
    
    @Property("createdAt")
    private Instant createdAt;
    
    @Property("updatedAt")
    private Instant updatedAt;
    
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<WorkflowNodeEntity> nodes;
    
    @Relationship(type = "HAS_EDGE", direction = Relationship.Direction.OUTGOING)
    private List<WorkflowEdgeEntity> edges;
    
    // Default constructor for Neo4j
    public WorkflowEntity() {}
    
    public WorkflowEntity(String id, String name, String organizationId, String status,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.organizationId = organizationId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public List<WorkflowNodeEntity> getNodes() { return nodes; }
    public void setNodes(List<WorkflowNodeEntity> nodes) { this.nodes = nodes; }
    
    public List<WorkflowEdgeEntity> getEdges() { return edges; }
    public void setEdges(List<WorkflowEdgeEntity> edges) { this.edges = edges; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowEntity that = (WorkflowEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WorkflowEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}