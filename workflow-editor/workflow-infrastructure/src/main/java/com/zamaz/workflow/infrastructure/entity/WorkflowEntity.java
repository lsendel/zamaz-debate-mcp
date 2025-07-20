package com.zamaz.workflow.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Node("Workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEntity {
    @Id
    private String id;
    
    @Property
    private String name;
    
    @Property
    private String description;
    
    @Property
    private String status;
    
    @Property
    private String organizationId;
    
    @Property
    private Instant createdAt;
    
    @Property
    private Instant updatedAt;
    
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<WorkflowNodeEntity> nodes = new ArrayList<>();
}