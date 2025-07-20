package com.zamaz.debatetree.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TreeMapVisualization {
    @NonNull
    private final List<TreeMapNode> nodes;
    
    private final int totalNodes;
    
    private final int maxDepth;
    
    @Getter
    @Builder
    public static class TreeMapNode {
        @NonNull
        private final String id;
        
        private final String parentId;
        
        @NonNull
        private final String name;
        
        private final double value;
        
        private final String color;
        
        private final Map<String, Object> metadata;
    }
}