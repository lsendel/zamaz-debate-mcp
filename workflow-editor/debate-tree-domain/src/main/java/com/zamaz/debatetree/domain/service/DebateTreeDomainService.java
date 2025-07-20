package com.zamaz.debatetree.domain.service;

import com.zamaz.debatetree.domain.entity.DebateTreeNode;
import com.zamaz.debatetree.domain.entity.DebateTreeStructure;
import com.zamaz.debatetree.domain.entity.TreeMapVisualization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DebateTreeDomainService {
    
    public DebateTreeStructure buildTreeFromDebates(List<Debate> debates) {
        List<DebateTreeNode> nodes = new ArrayList<>();
        
        for (Debate debate : debates) {
            DebateTreeNode node = DebateTreeNode.builder()
                    .debateId(debate.getId())
                    .title(debate.getTitle())
                    .description(debate.getDescription())
                    .status(mapDebateStatus(debate.getStatus()))
                    .parentDebateId(debate.getParentId())
                    .participantCount(debate.getParticipantCount())
                    .responseCount(debate.getResponseCount())
                    .createdAt(debate.getCreatedAt())
                    .lastActivityAt(debate.getLastActivityAt())
                    .depth(calculateDepth(debate, debates))
                    .relevanceScore(calculateRelevanceScore(debate))
                    .build();
            
            nodes.add(node);
        }
        
        return DebateTreeStructure.buildFromNodes(nodes);
    }
    
    public TreeMapVisualization generateTreeMap(DebateTreeStructure tree) {
        List<TreeMapVisualization.TreeMapNode> visualNodes = new ArrayList<>();
        Map<String, TreeMapVisualization.TreeMapNode> nodeMap = new ConcurrentHashMap<>();
        
        // Convert tree structure to visualization format
        convertToVisualization(tree.getRoot(), null, visualNodes, nodeMap);
        
        return TreeMapVisualization.builder()
                .nodes(visualNodes)
                .totalNodes(tree.getTotalNodes())
                .maxDepth(tree.getMaxDepth())
                .build();
    }
    
    private void convertToVisualization(DebateTreeNode node, String parentId,
                                      List<TreeMapVisualization.TreeMapNode> visualNodes,
                                      Map<String, TreeMapVisualization.TreeMapNode> nodeMap) {
        // Calculate node size based on activity
        double size = calculateNodeSize(node);
        
        TreeMapVisualization.TreeMapNode visualNode = TreeMapVisualization.TreeMapNode.builder()
                .id(node.getDebateId())
                .parentId(parentId)
                .name(node.getTitle())
                .value(size)
                .color(node.getStatus().getColor())
                .metadata(buildNodeMetadata(node))
                .build();
        
        visualNodes.add(visualNode);
        nodeMap.put(visualNode.getId(), visualNode);
        
        // Process children
        for (DebateTreeNode child : node.getChildren()) {
            convertToVisualization(child, node.getDebateId(), visualNodes, nodeMap);
        }
    }
    
    private double calculateNodeSize(DebateTreeNode node) {
        // Size based on activity metrics
        double participantWeight = 0.4;
        double responseWeight = 0.4;
        double descendantWeight = 0.2;
        
        double size = (node.getParticipantCount() * participantWeight) +
                     (node.getResponseCount() * responseWeight) +
                     (node.getTotalDescendants() * descendantWeight);
        
        // Ensure minimum size for visibility
        return Math.max(size, 10.0);
    }
    
    private Map<String, Object> buildNodeMetadata(DebateTreeNode node) {
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("status", node.getStatus().getDisplayName());
        metadata.put("participants", node.getParticipantCount());
        metadata.put("responses", node.getResponseCount());
        metadata.put("depth", node.getDepth());
        metadata.put("hasChildren", node.hasChildren());
        metadata.put("childCount", node.getChildren().size());
        metadata.put("relevanceScore", node.getRelevanceScore());
        
        if (node.getDescription() != null) {
            metadata.put("description", node.getDescription());
        }
        
        return metadata;
    }
    
    private DebateTreeNode.DebateStatus mapDebateStatus(String status) {
        return switch (status.toUpperCase()) {
            case "ACTIVE", "OPEN" -> DebateTreeNode.DebateStatus.ACTIVE;
            case "CLOSED", "COMPLETED" -> DebateTreeNode.DebateStatus.CLOSED;
            case "PENDING", "DRAFT" -> DebateTreeNode.DebateStatus.PENDING;
            case "ARCHIVED" -> DebateTreeNode.DebateStatus.ARCHIVED;
            default -> DebateTreeNode.DebateStatus.PENDING;
        };
    }
    
    private int calculateDepth(Debate debate, List<Debate> allDebates) {
        if (debate.getParentId() == null) {
            return 0;
        }
        
        Debate parent = allDebates.stream()
                .filter(d -> d.getId().equals(debate.getParentId()))
                .findFirst()
                .orElse(null);
        
        if (parent == null) {
            return 0;
        }
        
        return 1 + calculateDepth(parent, allDebates);
    }
    
    private double calculateRelevanceScore(Debate debate) {
        // Simple relevance calculation based on activity
        double recencyFactor = calculateRecencyFactor(debate);
        double activityFactor = (debate.getParticipantCount() + debate.getResponseCount()) / 100.0;
        
        return Math.min(1.0, recencyFactor * 0.6 + activityFactor * 0.4);
    }
    
    private double calculateRecencyFactor(Debate debate) {
        if (debate.getLastActivityAt() == null) {
            return 0.5;
        }
        
        long daysSinceActivity = java.time.Duration.between(
                debate.getLastActivityAt(), 
                java.time.Instant.now()
        ).toDays();
        
        if (daysSinceActivity <= 1) return 1.0;
        if (daysSinceActivity <= 7) return 0.8;
        if (daysSinceActivity <= 30) return 0.6;
        if (daysSinceActivity <= 90) return 0.4;
        return 0.2;
    }
    
    // Simple Debate interface to avoid circular dependencies
    public interface Debate {
        String getId();
        String getTitle();
        String getDescription();
        String getStatus();
        String getParentId();
        int getParticipantCount();
        int getResponseCount();
        java.time.Instant getCreatedAt();
        java.time.Instant getLastActivityAt();
    }
}