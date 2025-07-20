package com.zamaz.workflow.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashMap;
import java.util.Map;

@Node("WorkflowNode")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNodeEntity {
    @Id
    private String id;
    
    @Property
    private String type;
    
    @Property
    private String name;
    
    @Property
    private String description;
    
    @Property("configuration")
    @Builder.Default
    private Map<String, Object> configuration = new HashMap<>();
    
    @Property
    private double positionX;
    
    @Property
    private double positionY;
}