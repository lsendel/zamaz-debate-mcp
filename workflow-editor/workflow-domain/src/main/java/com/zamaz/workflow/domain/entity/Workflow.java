package com.zamaz.workflow.domain.entity;

import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.workflow.domain.valueobject.WorkflowStatus;
import com.zamaz.workflow.domain.valueobject.OrganizationId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Builder
public class Workflow {
    @NonNull
    private final WorkflowId id;
    
    @NonNull
    private final String name;
    
    private final String description;
    
    @NonNull
    @Builder.Default
    private final List<WorkflowNode> nodes = new ArrayList<>();
    
    @NonNull
    @Builder.Default
    private final List<WorkflowConnection> connections = new ArrayList<>();
    
    @NonNull
    @Builder.Default
    private final WorkflowStatus status = WorkflowStatus.DRAFT;
    
    @NonNull
    private final OrganizationId organizationId;
    
    @NonNull
    private final Instant createdAt;
    
    private final Instant updatedAt;
    
    public static Workflow create(String name, OrganizationId organizationId) {
        return Workflow.builder()
                .id(WorkflowId.generate())
                .name(name)
                .organizationId(organizationId)
                .createdAt(Instant.now())
                .build();
    }
    
    public void addNode(WorkflowNode node) {
        if (findNodeById(node.getId()).isPresent()) {
            throw new IllegalArgumentException("Node with ID " + node.getId() + " already exists");
        }
        nodes.add(node);
    }
    
    public void removeNode(String nodeId) {
        nodes.removeIf(node -> node.getId().equals(nodeId));
        connections.removeIf(conn -> 
            conn.getSourceNodeId().equals(nodeId) || conn.getTargetNodeId().equals(nodeId));
    }
    
    public void addConnection(WorkflowConnection connection) {
        validateConnection(connection);
        connections.add(connection);
    }
    
    public void removeConnection(String connectionId) {
        connections.removeIf(conn -> conn.getId().equals(connectionId));
    }
    
    public Optional<WorkflowNode> findNodeById(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst();
    }
    
    public List<WorkflowNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
    public List<WorkflowConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    }
    
    public void activate() {
        if (status != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Only draft workflows can be activated");
        }
        validateForActivation();
    }
    
    public void deactivate() {
        if (status != WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Only active workflows can be deactivated");
        }
    }
    
    private void validateConnection(WorkflowConnection connection) {
        if (!findNodeById(connection.getSourceNodeId()).isPresent()) {
            throw new IllegalArgumentException("Source node not found: " + connection.getSourceNodeId());
        }
        if (!findNodeById(connection.getTargetNodeId()).isPresent()) {
            throw new IllegalArgumentException("Target node not found: " + connection.getTargetNodeId());
        }
    }
    
    private void validateForActivation() {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one node");
        }
        
        long startNodes = nodes.stream()
                .filter(node -> node.getType() == WorkflowNodeType.START)
                .count();
        if (startNodes != 1) {
            throw new IllegalStateException("Workflow must have exactly one start node");
        }
        
        long endNodes = nodes.stream()
                .filter(node -> node.getType() == WorkflowNodeType.END)
                .count();
        if (endNodes == 0) {
            throw new IllegalStateException("Workflow must have at least one end node");
        }
    }
    
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
}