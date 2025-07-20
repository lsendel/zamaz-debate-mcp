package com.zamaz.workflow.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashMap;
import java.util.Map;

@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConnectionEntity {
    @Id
    @GeneratedValue
    private Long id;
    
    @Property
    private String connectionId;
    
    @Property
    private String label;
    
    @Property
    private int order;
    
    @Property("metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @TargetNode
    private WorkflowNodeEntity targetNode;
}