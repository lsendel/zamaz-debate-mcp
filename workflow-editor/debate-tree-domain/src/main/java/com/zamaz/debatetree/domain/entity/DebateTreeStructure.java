package com.zamaz.debatetree.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DebateTreeStructure {
    @NonNull
    private final DebateTreeNode root;
    
    @Builder.Default
    private final Map<String, DebateTreeNode> nodeMap = new HashMap<>();
    
    private final int totalNodes;
    
    private final int maxDepth;
    
    @Builder.Default
    private final Map<String, TreeMetrics> levelMetrics = new HashMap<>();
    
    public static DebateTreeStructure buildFromNodes(List<DebateTreeNode> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot build tree from empty node list");
        }
        
        // Find root node(s) - nodes without parents
        List<DebateTreeNode> roots = nodes.stream()
                .filter(node -> node.getParentDebateId() == null)
                .toList();
        
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("No root node found");
        }
        
        // For simplicity, use the first root if multiple exist
        DebateTreeNode root = roots.get(0);
        
        // Build node map for quick lookup
        Map<String, DebateTreeNode> nodeMap = new HashMap<>();
        nodes.forEach(node -> nodeMap.put(node.getDebateId(), node));
        
        // Build tree structure
        buildTreeRecursive(root, nodeMap);
        
        // Calculate metrics
        int maxDepth = calculateMaxDepth(root);
        Map<String, TreeMetrics> levelMetrics = calculateLevelMetrics(root);
        
        return DebateTreeStructure.builder()
                .root(root)
                .nodeMap(nodeMap)
                .totalNodes(nodes.size())
                .maxDepth(maxDepth)
                .levelMetrics(levelMetrics)
                .build();
    }
    
    private static void buildTreeRecursive(DebateTreeNode parent, Map<String, DebateTreeNode> nodeMap) {
        nodeMap.values().stream()
                .filter(node -> parent.getDebateId().equals(node.getParentDebateId()))
                .forEach(child -> {
                    parent.addChild(child);
                    buildTreeRecursive(child, nodeMap);
                });
    }
    
    private static int calculateMaxDepth(DebateTreeNode node) {
        if (node.getChildren().isEmpty()) {
            return node.getDepth();
        }
        
        return node.getChildren().stream()
                .mapToInt(DebateTreeStructure::calculateMaxDepth)
                .max()
                .orElse(node.getDepth());
    }
    
    private static Map<String, TreeMetrics> calculateLevelMetrics(DebateTreeNode root) {
        Map<String, TreeMetrics> metrics = new HashMap<>();
        calculateLevelMetricsRecursive(root, metrics);
        return metrics;
    }
    
    private static void calculateLevelMetricsRecursive(DebateTreeNode node, Map<String, TreeMetrics> metrics) {
        String level = "Level_" + node.getDepth();
        
        metrics.compute(level, (k, v) -> {
            if (v == null) {
                v = new TreeMetrics();
            }
            v.nodeCount++;
            v.totalParticipants += node.getParticipantCount();
            v.totalResponses += node.getResponseCount();
            return v;
        });
        
        node.getChildren().forEach(child -> calculateLevelMetricsRecursive(child, metrics));
    }
    
    @Getter
    public static class TreeMetrics {
        private int nodeCount = 0;
        private int totalParticipants = 0;
        private int totalResponses = 0;
        
        public double getAverageParticipants() {
            return nodeCount > 0 ? (double) totalParticipants / nodeCount : 0;
        }
        
        public double getAverageResponses() {
            return nodeCount > 0 ? (double) totalResponses / nodeCount : 0;
        }
    }
}