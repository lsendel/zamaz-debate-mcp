package com.zamaz.mcp.github.analyzer.dependency;

import com.zamaz.mcp.github.analyzer.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes dependencies and creates dependency graphs
 */
@Component
@Slf4j
public class DependencyAnalyzer {
    
    private final Map<ProjectTypeEnum, DependencyExtractor> extractors;
    
    public DependencyAnalyzer() {
        this.extractors = initializeExtractors();
    }
    
    /**
     * Analyze dependencies for all detected project types
     */
    public DependencyGraph analyzeDependencies(RepositoryStructure structure, List<ProjectType> projectTypes) {
        log.info("Analyzing dependencies for {} project types", projectTypes.size());
        
        DependencyGraph.DependencyGraphBuilder graphBuilder = DependencyGraph.builder()
                .dependencies(new ArrayList<>())
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .clusters(new ArrayList<>());
        
        // Extract dependencies for each project type
        for (ProjectType projectType : projectTypes) {
            DependencyExtractor extractor = extractors.get(projectType.getType());
            if (extractor != null) {
                try {
                    List<Dependency> dependencies = extractor.extractDependencies(structure, projectType);
                    graphBuilder.dependencies.addAll(dependencies);
                    
                    log.info("Extracted {} dependencies for project type {}", 
                            dependencies.size(), projectType.getType().getDisplayName());
                } catch (Exception e) {
                    log.error("Error extracting dependencies for project type {}: {}", 
                            projectType.getType(), e.getMessage(), e);
                }
            }
        }
        
        DependencyGraph graph = graphBuilder.build();
        
        // Build graph structure
        buildGraphStructure(graph);
        
        // Analyze dependency relationships
        analyzeDependencyRelationships(graph);
        
        // Detect circular dependencies
        detectCircularDependencies(graph);
        
        // Calculate metrics
        calculateMetrics(graph);
        
        log.info("Dependency analysis completed. Found {} dependencies, {} nodes, {} edges", 
                graph.getDependencies().size(), graph.getNodes().size(), graph.getEdges().size());
        
        return graph;
    }
    
    /**
     * Build graph structure from dependencies
     */
    private void buildGraphStructure(DependencyGraph graph) {
        Set<String> nodeIds = new HashSet<>();
        
        // Create nodes for all dependencies
        for (Dependency dependency : graph.getDependencies()) {
            String nodeId = dependency.getGroupId() + ":" + dependency.getArtifactId();
            
            if (!nodeIds.contains(nodeId)) {
                DependencyNode node = DependencyNode.builder()
                        .id(nodeId)
                        .groupId(dependency.getGroupId())
                        .artifactId(dependency.getArtifactId())
                        .version(dependency.getVersion())
                        .type(dependency.getType())
                        .scope(dependency.getScope())
                        .dependencies(new ArrayList<>())
                        .dependents(new ArrayList<>())
                        .build();
                
                graph.getNodes().add(node);
                nodeIds.add(nodeId);
            }
        }
        
        // Create edges between dependencies
        for (Dependency dependency : graph.getDependencies()) {
            if (dependency.getDependencies() != null) {
                for (Dependency childDep : dependency.getDependencies()) {
                    String fromId = dependency.getGroupId() + ":" + dependency.getArtifactId();
                    String toId = childDep.getGroupId() + ":" + childDep.getArtifactId();
                    
                    DependencyEdge edge = DependencyEdge.builder()
                            .from(fromId)
                            .to(toId)
                            .type(DependencyEdgeType.DEPENDS_ON)
                            .scope(childDep.getScope())
                            .optional(childDep.isOptional())
                            .build();
                    
                    graph.getEdges().add(edge);
                }
            }
        }
    }
    
    /**
     * Analyze dependency relationships
     */
    private void analyzeDependencyRelationships(DependencyGraph graph) {
        Map<String, DependencyNode> nodeMap = graph.getNodes().stream()
                .collect(Collectors.toMap(DependencyNode::getId, node -> node));
        
        // Build dependency relationships
        for (DependencyEdge edge : graph.getEdges()) {
            DependencyNode fromNode = nodeMap.get(edge.getFrom());
            DependencyNode toNode = nodeMap.get(edge.getTo());
            
            if (fromNode != null && toNode != null) {
                fromNode.getDependencies().add(toNode.getId());
                toNode.getDependents().add(fromNode.getId());
            }
        }
    }
    
    /**
     * Detect circular dependencies
     */
    private void detectCircularDependencies(DependencyGraph graph) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<List<String>> cycles = new ArrayList<>();
        
        for (DependencyNode node : graph.getNodes()) {
            if (!visited.contains(node.getId())) {
                detectCyclesRecursive(node, graph, visited, recursionStack, cycles, new ArrayList<>());
            }
        }
        
        graph.setCircularDependencies(cycles);
        graph.setHasCircularDependencies(!cycles.isEmpty());
        
        if (!cycles.isEmpty()) {
            log.warn("Detected {} circular dependency cycles", cycles.size());
        }
    }
    
    /**
     * Recursive helper for cycle detection
     */
    private void detectCyclesRecursive(DependencyNode node, DependencyGraph graph, 
                                     Set<String> visited, Set<String> recursionStack, 
                                     List<List<String>> cycles, List<String> currentPath) {
        
        visited.add(node.getId());
        recursionStack.add(node.getId());
        currentPath.add(node.getId());
        
        Map<String, DependencyNode> nodeMap = graph.getNodes().stream()
                .collect(Collectors.toMap(DependencyNode::getId, n -> n));
        
        for (String depId : node.getDependencies()) {
            DependencyNode depNode = nodeMap.get(depId);
            if (depNode != null) {
                if (recursionStack.contains(depId)) {
                    // Found a cycle
                    int cycleStart = currentPath.indexOf(depId);
                    List<String> cycle = new ArrayList<>(currentPath.subList(cycleStart, currentPath.size()));
                    cycle.add(depId); // Complete the cycle
                    cycles.add(cycle);
                } else if (!visited.contains(depId)) {
                    detectCyclesRecursive(depNode, graph, visited, recursionStack, cycles, currentPath);
                }
            }
        }
        
        recursionStack.remove(node.getId());
        currentPath.remove(currentPath.size() - 1);
    }
    
    /**
     * Calculate dependency metrics
     */
    private void calculateMetrics(DependencyGraph graph) {
        int totalNodes = graph.getNodes().size();
        int totalEdges = graph.getEdges().size();
        
        // Calculate coupling metrics
        double totalCoupling = 0.0;
        int maxDependencies = 0;
        int maxDependents = 0;
        
        for (DependencyNode node : graph.getNodes()) {
            int dependencies = node.getDependencies().size();
            int dependents = node.getDependents().size();
            
            totalCoupling += dependencies + dependents;
            maxDependencies = Math.max(maxDependencies, dependencies);
            maxDependents = Math.max(maxDependents, dependents);
        }
        
        double averageCoupling = totalNodes > 0 ? totalCoupling / totalNodes : 0.0;
        double density = totalNodes > 1 ? (double) totalEdges / (totalNodes * (totalNodes - 1)) : 0.0;
        
        // Calculate depth metrics
        int maxDepth = calculateMaxDepth(graph);
        double averageDepth = calculateAverageDepth(graph);
        
        // Set metrics
        graph.setTotalNodes(totalNodes);
        graph.setTotalEdges(totalEdges);
        graph.setAverageCoupling(averageCoupling);
        graph.setMaxDependencies(maxDependencies);
        graph.setMaxDependents(maxDependents);
        graph.setDensity(density);
        graph.setMaxDepth(maxDepth);
        graph.setAverageDepth(averageDepth);
        
        log.info("Dependency metrics - Nodes: {}, Edges: {}, Avg Coupling: {:.2f}, Density: {:.2f}, Max Depth: {}", 
                totalNodes, totalEdges, averageCoupling, density, maxDepth);
    }
    
    /**
     * Calculate maximum dependency depth
     */
    private int calculateMaxDepth(DependencyGraph graph) {
        int maxDepth = 0;
        
        for (DependencyNode node : graph.getNodes()) {
            if (node.getDependents().isEmpty()) { // Root node
                int depth = calculateDepthRecursive(node, graph, new HashSet<>());
                maxDepth = Math.max(maxDepth, depth);
            }
        }
        
        return maxDepth;
    }
    
    /**
     * Calculate average dependency depth
     */
    private double calculateAverageDepth(DependencyGraph graph) {
        int totalDepth = 0;
        int nodeCount = 0;
        
        for (DependencyNode node : graph.getNodes()) {
            if (node.getDependents().isEmpty()) { // Root node
                totalDepth += calculateDepthRecursive(node, graph, new HashSet<>());
                nodeCount++;
            }
        }
        
        return nodeCount > 0 ? (double) totalDepth / nodeCount : 0.0;
    }
    
    /**
     * Recursive helper for depth calculation
     */
    private int calculateDepthRecursive(DependencyNode node, DependencyGraph graph, Set<String> visited) {
        if (visited.contains(node.getId())) {
            return 0; // Avoid infinite recursion
        }
        
        visited.add(node.getId());
        
        int maxChildDepth = 0;
        Map<String, DependencyNode> nodeMap = graph.getNodes().stream()
                .collect(Collectors.toMap(DependencyNode::getId, n -> n));
        
        for (String depId : node.getDependencies()) {
            DependencyNode depNode = nodeMap.get(depId);
            if (depNode != null) {
                int childDepth = calculateDepthRecursive(depNode, graph, new HashSet<>(visited));
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
        }
        
        return maxChildDepth + 1;
    }
    
    /**
     * Initialize dependency extractors for different project types
     */
    private Map<ProjectTypeEnum, DependencyExtractor> initializeExtractors() {
        Map<ProjectTypeEnum, DependencyExtractor> extractors = new HashMap<>();
        
        extractors.put(ProjectTypeEnum.MAVEN, new MavenDependencyExtractor());
        extractors.put(ProjectTypeEnum.GRADLE, new GradleDependencyExtractor());
        extractors.put(ProjectTypeEnum.NODE_JS, new NodeJsDependencyExtractor());
        extractors.put(ProjectTypeEnum.PYTHON, new PythonDependencyExtractor());
        extractors.put(ProjectTypeEnum.SPRING_BOOT, new MavenDependencyExtractor()); // Spring Boot uses Maven/Gradle
        
        return extractors;
    }
    
    /**
     * Extract dependencies from Maven pom.xml
     */
    private static class MavenDependencyExtractor implements DependencyExtractor {
        @Override
        public List<Dependency> extractDependencies(RepositoryStructure structure, ProjectType projectType) {
            List<Dependency> dependencies = new ArrayList<>();
            
            // Find pom.xml file
            FileInfo pomFile = structure.getFiles().stream()
                    .filter(file -> file.getName().equals("pom.xml"))
                    .findFirst()
                    .orElse(null);
            
            if (pomFile != null && pomFile.getContent() != null) {
                // TODO: Parse pom.xml to extract dependencies
                // For now, return mock dependencies
                dependencies.add(createMockDependency("org.springframework.boot", "spring-boot-starter-web", "2.7.0"));
                dependencies.add(createMockDependency("org.springframework.boot", "spring-boot-starter-data-jpa", "2.7.0"));
            }
            
            return dependencies;
        }
        
        private Dependency createMockDependency(String groupId, String artifactId, String version) {
            return Dependency.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .version(version)
                    .type("jar")
                    .scope("compile")
                    .optional(false)
                    .build();
        }
    }
    
    /**
     * Extract dependencies from Gradle build.gradle
     */
    private static class GradleDependencyExtractor implements DependencyExtractor {
        @Override
        public List<Dependency> extractDependencies(RepositoryStructure structure, ProjectType projectType) {
            List<Dependency> dependencies = new ArrayList<>();
            
            // Find build.gradle file
            FileInfo buildFile = structure.getFiles().stream()
                    .filter(file -> file.getName().equals("build.gradle") || file.getName().equals("build.gradle.kts"))
                    .findFirst()
                    .orElse(null);
            
            if (buildFile != null && buildFile.getContent() != null) {
                // TODO: Parse build.gradle to extract dependencies
                // For now, return mock dependencies
                dependencies.add(createMockDependency("org.springframework.boot", "spring-boot-starter-web", "2.7.0"));
            }
            
            return dependencies;
        }
        
        private Dependency createMockDependency(String groupId, String artifactId, String version) {
            return Dependency.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .version(version)
                    .type("jar")
                    .scope("implementation")
                    .optional(false)
                    .build();
        }
    }
    
    /**
     * Extract dependencies from Node.js package.json
     */
    private static class NodeJsDependencyExtractor implements DependencyExtractor {
        @Override
        public List<Dependency> extractDependencies(RepositoryStructure structure, ProjectType projectType) {
            List<Dependency> dependencies = new ArrayList<>();
            
            // Find package.json file
            FileInfo packageFile = structure.getFiles().stream()
                    .filter(file -> file.getName().equals("package.json"))
                    .findFirst()
                    .orElse(null);
            
            if (packageFile != null && packageFile.getContent() != null) {
                // TODO: Parse package.json to extract dependencies
                // For now, return mock dependencies
                dependencies.add(createMockDependency("react", "react", "18.2.0"));
                dependencies.add(createMockDependency("react-dom", "react-dom", "18.2.0"));
            }
            
            return dependencies;
        }
        
        private Dependency createMockDependency(String name, String artifactId, String version) {
            return Dependency.builder()
                    .groupId("npm")
                    .artifactId(artifactId)
                    .version(version)
                    .type("npm")
                    .scope("dependencies")
                    .optional(false)
                    .build();
        }
    }
    
    /**
     * Extract dependencies from Python requirements.txt or setup.py
     */
    private static class PythonDependencyExtractor implements DependencyExtractor {
        @Override
        public List<Dependency> extractDependencies(RepositoryStructure structure, ProjectType projectType) {
            List<Dependency> dependencies = new ArrayList<>();
            
            // Find requirements.txt or setup.py
            FileInfo requirementsFile = structure.getFiles().stream()
                    .filter(file -> file.getName().equals("requirements.txt") || 
                                   file.getName().equals("setup.py") ||
                                   file.getName().equals("pyproject.toml"))
                    .findFirst()
                    .orElse(null);
            
            if (requirementsFile != null && requirementsFile.getContent() != null) {
                // TODO: Parse requirements file to extract dependencies
                // For now, return mock dependencies
                dependencies.add(createMockDependency("django", "django", "4.1.0"));
                dependencies.add(createMockDependency("requests", "requests", "2.28.1"));
            }
            
            return dependencies;
        }
        
        private Dependency createMockDependency(String name, String artifactId, String version) {
            return Dependency.builder()
                    .groupId("pypi")
                    .artifactId(artifactId)
                    .version(version)
                    .type("wheel")
                    .scope("install")
                    .optional(false)
                    .build();
        }
    }
    
    /**
     * Interface for extracting dependencies from different project types
     */
    private interface DependencyExtractor {
        List<Dependency> extractDependencies(RepositoryStructure structure, ProjectType projectType);
    }
}