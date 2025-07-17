package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents a dependency graph with nodes, edges, and analysis results
 */
@Data
@Builder
public class DependencyGraph {
    
    /**
     * All dependencies in the graph
     */
    private List<Dependency> dependencies;
    
    /**
     * Nodes in the dependency graph
     */
    private List<DependencyNode> nodes;
    
    /**
     * Edges connecting the nodes
     */
    private List<DependencyEdge> edges;
    
    /**
     * Dependency clusters (groups of related dependencies)
     */
    private List<DependencyCluster> clusters;
    
    /**
     * Circular dependencies detected
     */
    private List<List<String>> circularDependencies;
    
    /**
     * Whether circular dependencies were found
     */
    private boolean hasCircularDependencies;
    
    /**
     * Total number of nodes
     */
    private int totalNodes;
    
    /**
     * Total number of edges
     */
    private int totalEdges;
    
    /**
     * Average coupling score
     */
    private double averageCoupling;
    
    /**
     * Maximum dependencies for a single node
     */
    private int maxDependencies;
    
    /**
     * Maximum dependents for a single node
     */
    private int maxDependents;
    
    /**
     * Graph density (0.0 to 1.0)
     */
    private double density;
    
    /**
     * Maximum depth of dependency chain
     */
    private int maxDepth;
    
    /**
     * Average depth of dependency chains
     */
    private double averageDepth;
    
    /**
     * Get node by ID
     */
    public DependencyNode getNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get dependencies by scope
     */
    public List<Dependency> getDependenciesByScope(String scope) {
        return dependencies.stream()
                .filter(dep -> scope.equals(dep.getScope()))
                .toList();
    }
    
    /**
     * Get dependencies by type
     */
    public List<Dependency> getDependenciesByType(String type) {
        return dependencies.stream()
                .filter(dep -> type.equals(dep.getType()))
                .toList();
    }
    
    /**
     * Get root nodes (nodes with no dependents)
     */
    public List<DependencyNode> getRootNodes() {
        return nodes.stream()
                .filter(node -> node.getDependents().isEmpty())
                .toList();
    }
    
    /**
     * Get leaf nodes (nodes with no dependencies)
     */
    public List<DependencyNode> getLeafNodes() {
        return nodes.stream()
                .filter(node -> node.getDependencies().isEmpty())
                .toList();
    }
    
    /**
     * Get nodes with high coupling (above threshold)
     */
    public List<DependencyNode> getHighCouplingNodes(double threshold) {
        return nodes.stream()
                .filter(node -> (node.getDependencies().size() + node.getDependents().size()) > threshold)
                .toList();
    }
    
    /**
     * Calculate complexity score based on graph structure
     */
    public double getComplexityScore() {
        double nodeComplexity = Math.min(totalNodes / 10.0, 30.0);
        double edgeComplexity = Math.min(totalEdges / 20.0, 25.0);
        double couplingComplexity = Math.min(averageCoupling * 10.0, 25.0);
        double depthComplexity = Math.min(maxDepth * 2.0, 20.0);
        
        return Math.min(nodeComplexity + edgeComplexity + couplingComplexity + depthComplexity, 100.0);
    }
    
    /**
     * Calculate maintainability score
     */
    public double getMaintainabilityScore() {
        double baseScore = 100.0;
        
        // Reduce score for circular dependencies
        if (hasCircularDependencies) {
            baseScore -= circularDependencies.size() * 10.0;
        }
        
        // Reduce score for high coupling
        if (averageCoupling > 5.0) {
            baseScore -= (averageCoupling - 5.0) * 5.0;
        }
        
        // Reduce score for high complexity
        baseScore -= getComplexityScore() * 0.3;
        
        return Math.max(Math.min(baseScore, 100.0), 0.0);
    }
    
    /**
     * Check if graph has critical dependency issues
     */
    public boolean hasCriticalIssues() {
        return hasCircularDependencies || 
               averageCoupling > 10.0 || 
               maxDepth > 20 ||
               density > 0.8;
    }
    
    /**
     * Get summary statistics
     */
    public DependencyGraphSummary getSummary() {
        return DependencyGraphSummary.builder()
                .totalDependencies(dependencies.size())
                .totalNodes(totalNodes)
                .totalEdges(totalEdges)
                .circularDependencyCount(circularDependencies.size())
                .averageCoupling(averageCoupling)
                .maxDepth(maxDepth)
                .complexityScore(getComplexityScore())
                .maintainabilityScore(getMaintainabilityScore())
                .hasCriticalIssues(hasCriticalIssues())
                .build();
    }
}