package com.zamaz.mcp.github.analyzer.visualization;

import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.VisualizationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates visualizations for repository structure analysis
 */
@Component
@Slf4j
public class StructureVisualizer {
    
    /**
     * Create comprehensive visualization for repository structure
     */
    public StructureVisualization createVisualization(RepositoryStructure structure, 
                                                    DependencyGraph dependencyGraph, 
                                                    CodeOrganizationResult organization) {
        log.info("Creating structure visualization for repository {}/{}", 
                structure.getOwner(), structure.getRepository());
        
        // Create different visualization types
        VisualizationNode treeView = createTreeVisualization(structure, organization);
        VisualizationNode graphView = createGraphVisualization(dependencyGraph);
        VisualizationNode sunburstView = createSunburstVisualization(structure, organization);
        VisualizationNode treemapView = createTreemapVisualization(structure, organization);
        VisualizationNode networkView = createNetworkVisualization(dependencyGraph);
        
        // Create visualization metadata
        VisualizationMetadata metadata = VisualizationMetadata.builder()
                .totalNodes(calculateTotalNodes(structure))
                .totalEdges(calculateTotalEdges(dependencyGraph))
                .maxDepth(calculateMaxDepth(structure))
                .complexity(calculateVisualizationComplexity(structure, dependencyGraph))
                .recommendations(generateVisualizationRecommendations(structure, dependencyGraph))
                .build();
        
        StructureVisualization visualization = StructureVisualization.builder()
                .treeView(treeView)
                .graphView(graphView)
                .sunburstView(sunburstView)
                .treemapView(treemapView)
                .networkView(networkView)
                .metadata(metadata)
                .timestamp(System.currentTimeMillis())
                .build();
        
        log.info("Structure visualization created with {} nodes and {} edges", 
                metadata.getTotalNodes(), metadata.getTotalEdges());
        
        return visualization;
    }
    
    /**
     * Create tree visualization of repository structure
     */
    private VisualizationNode createTreeVisualization(RepositoryStructure structure, 
                                                     CodeOrganizationResult organization) {
        log.info("Creating tree visualization");
        
        VisualizationNode root = VisualizationNode.builder()
                .id("root")
                .name(structure.getRepository())
                .type(VisualizationType.TREE)
                .nodeType("repository")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        root.getProperties().put("owner", structure.getOwner());
        root.getProperties().put("branch", structure.getBranch());
        root.getProperties().put("totalFiles", String.valueOf(structure.getFiles().size()));
        root.getProperties().put("totalDirectories", String.valueOf(structure.getDirectories().size()));
        
        // Add directory structure
        Map<String, VisualizationNode> nodeMap = new HashMap<>();
        nodeMap.put("root", root);
        
        // Sort directories by depth to ensure parents are created first
        List<DirectoryInfo> sortedDirectories = structure.getDirectories().stream()
                .sorted(Comparator.comparingInt(DirectoryInfo::getDepth))
                .toList();
        
        for (DirectoryInfo directory : sortedDirectories) {
            VisualizationNode dirNode = createDirectoryNode(directory, structure);
            nodeMap.put(directory.getPath(), dirNode);
            
            // Find parent node
            String parentPath = getParentPath(directory.getPath());
            VisualizationNode parentNode = nodeMap.get(parentPath);
            
            if (parentNode != null) {
                parentNode.getChildren().add(dirNode);
            } else {
                root.getChildren().add(dirNode);
            }
        }
        
        // Add files to their respective directories
        for (FileInfo file : structure.getFiles()) {
            VisualizationNode fileNode = createFileNode(file);
            String parentPath = getParentPath(file.getPath());
            VisualizationNode parentNode = nodeMap.get(parentPath);
            
            if (parentNode != null) {
                parentNode.getChildren().add(fileNode);
            } else {
                root.getChildren().add(fileNode);
            }
        }
        
        return root;
    }
    
    /**
     * Create graph visualization of dependency relationships
     */
    private VisualizationNode createGraphVisualization(DependencyGraph dependencyGraph) {
        log.info("Creating graph visualization");
        
        VisualizationNode root = VisualizationNode.builder()
                .id("dependency-graph")
                .name("Dependency Graph")
                .type(VisualizationType.GRAPH)
                .nodeType("graph")
                .children(new ArrayList<>())
                .edges(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        root.getProperties().put("totalDependencies", String.valueOf(dependencyGraph.getDependencies().size()));
        root.getProperties().put("totalNodes", String.valueOf(dependencyGraph.getTotalNodes()));
        root.getProperties().put("totalEdges", String.valueOf(dependencyGraph.getTotalEdges()));
        root.getProperties().put("averageCoupling", String.valueOf(dependencyGraph.getAverageCoupling()));
        
        // Add dependency nodes
        for (DependencyNode depNode : dependencyGraph.getNodes()) {
            VisualizationNode vizNode = createDependencyVisualizationNode(depNode);
            root.getChildren().add(vizNode);
        }
        
        // Add dependency edges
        for (DependencyEdge edge : dependencyGraph.getEdges()) {
            VisualizationEdge vizEdge = createDependencyVisualizationEdge(edge);
            root.getEdges().add(vizEdge);
        }
        
        return root;
    }
    
    /**
     * Create sunburst visualization for hierarchical structure
     */
    private VisualizationNode createSunburstVisualization(RepositoryStructure structure, 
                                                         CodeOrganizationResult organization) {
        log.info("Creating sunburst visualization");
        
        VisualizationNode root = VisualizationNode.builder()
                .id("sunburst-root")
                .name(structure.getRepository())
                .type(VisualizationType.SUNBURST)
                .nodeType("repository")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        // Add package structure as sunburst rings
        if (organization.getPackageStructure() != null) {
            for (PackageInfo pkg : organization.getPackageStructure().getPackages().values()) {
                VisualizationNode packageNode = createPackageVisualizationNode(pkg);
                root.getChildren().add(packageNode);
            }
        }
        
        // Add size information for sunburst segments
        calculateSunburstSizes(root, structure);
        
        return root;
    }
    
    /**
     * Create treemap visualization for code size distribution
     */
    private VisualizationNode createTreemapVisualization(RepositoryStructure structure, 
                                                        CodeOrganizationResult organization) {
        log.info("Creating treemap visualization");
        
        VisualizationNode root = VisualizationNode.builder()
                .id("treemap-root")
                .name(structure.getRepository())
                .type(VisualizationType.TREEMAP)
                .nodeType("repository")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        // Group files by directory and calculate sizes
        Map<String, List<FileInfo>> filesByDirectory = structure.getFiles().stream()
                .collect(Collectors.groupingBy(file -> getParentPath(file.getPath())));
        
        for (Map.Entry<String, List<FileInfo>> entry : filesByDirectory.entrySet()) {
            String directoryPath = entry.getKey();
            List<FileInfo> files = entry.getValue();
            
            VisualizationNode dirNode = VisualizationNode.builder()
                    .id("treemap-" + directoryPath)
                    .name(getDirectoryName(directoryPath))
                    .type(VisualizationType.TREEMAP)
                    .nodeType("directory")
                    .children(new ArrayList<>())
                    .properties(new HashMap<>())
                    .build();
            
            long totalSize = files.stream().mapToLong(FileInfo::getSize).sum();
            int totalLines = files.stream().mapToInt(FileInfo::getLineCount).sum();
            
            dirNode.getProperties().put("size", String.valueOf(totalSize));
            dirNode.getProperties().put("lines", String.valueOf(totalLines));
            dirNode.getProperties().put("files", String.valueOf(files.size()));
            
            // Add individual files
            for (FileInfo file : files) {
                VisualizationNode fileNode = createFileVisualizationNode(file);
                dirNode.getChildren().add(fileNode);
            }
            
            root.getChildren().add(dirNode);
        }
        
        return root;
    }
    
    /**
     * Create network visualization for module/component relationships
     */
    private VisualizationNode createNetworkVisualization(DependencyGraph dependencyGraph) {
        log.info("Creating network visualization");
        
        VisualizationNode root = VisualizationNode.builder()
                .id("network-root")
                .name("Component Network")
                .type(VisualizationType.NETWORK)
                .nodeType("network")
                .children(new ArrayList<>())
                .edges(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        // Create clusters for different dependency types
        Map<String, List<DependencyNode>> nodesByType = dependencyGraph.getNodes().stream()
                .collect(Collectors.groupingBy(node -> node.getType() != null ? node.getType() : "unknown"));
        
        for (Map.Entry<String, List<DependencyNode>> entry : nodesByType.entrySet()) {
            String type = entry.getKey();
            List<DependencyNode> nodes = entry.getValue();
            
            VisualizationNode cluster = VisualizationNode.builder()
                    .id("cluster-" + type)
                    .name(type.toUpperCase() + " Dependencies")
                    .type(VisualizationType.NETWORK)
                    .nodeType("cluster")
                    .children(new ArrayList<>())
                    .properties(new HashMap<>())
                    .build();
            
            cluster.getProperties().put("type", type);
            cluster.getProperties().put("nodeCount", String.valueOf(nodes.size()));
            
            for (DependencyNode node : nodes) {
                VisualizationNode vizNode = createDependencyVisualizationNode(node);
                cluster.getChildren().add(vizNode);
            }
            
            root.getChildren().add(cluster);
        }
        
        // Add network edges
        for (DependencyEdge edge : dependencyGraph.getEdges()) {
            VisualizationEdge vizEdge = createDependencyVisualizationEdge(edge);
            root.getEdges().add(vizEdge);
        }
        
        return root;
    }
    
    /**
     * Create directory visualization node
     */
    private VisualizationNode createDirectoryNode(DirectoryInfo directory, RepositoryStructure structure) {
        VisualizationNode node = VisualizationNode.builder()
                .id("dir-" + directory.getPath())
                .name(directory.getName())
                .type(VisualizationType.TREE)
                .nodeType("directory")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        node.getProperties().put("path", directory.getPath());
        node.getProperties().put("fileCount", String.valueOf(directory.getFileCount()));
        node.getProperties().put("subdirectoryCount", String.valueOf(directory.getSubdirectoryCount()));
        node.getProperties().put("totalSize", String.valueOf(directory.getTotalSize()));
        node.getProperties().put("depth", String.valueOf(directory.getDepth()));
        
        if (directory.getType() != null) {
            node.getProperties().put("directoryType", directory.getType().name());
        }
        
        return node;
    }
    
    /**
     * Create file visualization node
     */
    private VisualizationNode createFileNode(FileInfo file) {
        VisualizationNode node = VisualizationNode.builder()
                .id("file-" + file.getPath())
                .name(file.getName())
                .type(VisualizationType.TREE)
                .nodeType("file")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        node.getProperties().put("path", file.getPath());
        node.getProperties().put("size", String.valueOf(file.getSize()));
        node.getProperties().put("lineCount", String.valueOf(file.getLineCount()));
        node.getProperties().put("extension", file.getExtension());
        node.getProperties().put("language", file.getLanguage());
        node.getProperties().put("binary", String.valueOf(file.isBinary()));
        
        return node;
    }
    
    /**
     * Create file visualization node for treemap
     */
    private VisualizationNode createFileVisualizationNode(FileInfo file) {
        VisualizationNode node = createFileNode(file);
        node.setType(VisualizationType.TREEMAP);
        
        // Add treemap-specific properties
        node.getProperties().put("weight", String.valueOf(file.getLineCount()));
        node.getProperties().put("color", getFileColorByLanguage(file.getLanguage()));
        
        return node;
    }
    
    /**
     * Create dependency visualization node
     */
    private VisualizationNode createDependencyVisualizationNode(DependencyNode depNode) {
        VisualizationNode node = VisualizationNode.builder()
                .id("dep-" + depNode.getId())
                .name(depNode.getArtifactId())
                .type(VisualizationType.GRAPH)
                .nodeType("dependency")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        node.getProperties().put("groupId", depNode.getGroupId());
        node.getProperties().put("artifactId", depNode.getArtifactId());
        node.getProperties().put("version", depNode.getVersion());
        node.getProperties().put("type", depNode.getType());
        node.getProperties().put("scope", depNode.getScope());
        node.getProperties().put("dependencyCount", String.valueOf(depNode.getDependencies().size()));
        node.getProperties().put("dependentCount", String.valueOf(depNode.getDependents().size()));
        
        return node;
    }
    
    /**
     * Create package visualization node
     */
    private VisualizationNode createPackageVisualizationNode(PackageInfo pkg) {
        VisualizationNode node = VisualizationNode.builder()
                .id("pkg-" + pkg.getPath())
                .name(pkg.getName())
                .type(VisualizationType.SUNBURST)
                .nodeType("package")
                .children(new ArrayList<>())
                .properties(new HashMap<>())
                .build();
        
        node.getProperties().put("path", pkg.getPath());
        node.getProperties().put("fileCount", String.valueOf(pkg.getFileCount()));
        node.getProperties().put("lineCount", String.valueOf(pkg.getLineCount()));
        node.getProperties().put("depth", String.valueOf(pkg.getDepth()));
        
        return node;
    }
    
    /**
     * Create dependency visualization edge
     */
    private VisualizationEdge createDependencyVisualizationEdge(DependencyEdge edge) {
        VisualizationEdge vizEdge = VisualizationEdge.builder()
                .id("edge-" + edge.getFrom() + "-" + edge.getTo())
                .source("dep-" + edge.getFrom())
                .target("dep-" + edge.getTo())
                .type(edge.getType().name())
                .properties(new HashMap<>())
                .build();
        
        vizEdge.getProperties().put("scope", edge.getScope());
        vizEdge.getProperties().put("optional", String.valueOf(edge.isOptional()));
        vizEdge.getProperties().put("edgeType", edge.getType().name());
        
        return vizEdge;
    }
    
    /**
     * Calculate sizes for sunburst visualization
     */
    private void calculateSunburstSizes(VisualizationNode root, RepositoryStructure structure) {
        for (VisualizationNode child : root.getChildren()) {
            calculateSunburstSizeRecursive(child, structure);
        }
    }
    
    /**
     * Recursively calculate sunburst sizes
     */
    private int calculateSunburstSizeRecursive(VisualizationNode node, RepositoryStructure structure) {
        if (node.getChildren().isEmpty()) {
            // Leaf node - use line count or file count
            String lineCount = node.getProperties().get("lineCount");
            if (lineCount != null) {
                int size = Integer.parseInt(lineCount);
                node.getProperties().put("size", String.valueOf(size));
                return size;
            }
            return 1;
        }
        
        // Non-leaf node - sum of children
        int totalSize = 0;
        for (VisualizationNode child : node.getChildren()) {
            totalSize += calculateSunburstSizeRecursive(child, structure);
        }
        
        node.getProperties().put("size", String.valueOf(totalSize));
        return totalSize;
    }
    
    /**
     * Get file color based on programming language
     */
    private String getFileColorByLanguage(String language) {
        return switch (language != null ? language.toLowerCase() : "unknown") {
            case "java" -> "#f89820";
            case "javascript" -> "#f1e05a";
            case "typescript" -> "#2b7489";
            case "python" -> "#3572a5";
            case "html" -> "#e34c26";
            case "css" -> "#563d7c";
            case "json" -> "#292929";
            case "xml" -> "#0060ac";
            case "yaml", "yml" -> "#cb171e";
            case "markdown" -> "#083fa1";
            default -> "#586e75";
        };
    }
    
    /**
     * Calculate total nodes for metadata
     */
    private int calculateTotalNodes(RepositoryStructure structure) {
        return structure.getFiles().size() + structure.getDirectories().size();
    }
    
    /**
     * Calculate total edges for metadata
     */
    private int calculateTotalEdges(DependencyGraph dependencyGraph) {
        return dependencyGraph.getTotalEdges();
    }
    
    /**
     * Calculate maximum depth for metadata
     */
    private int calculateMaxDepth(RepositoryStructure structure) {
        return structure.getMaxDirectoryDepth();
    }
    
    /**
     * Calculate visualization complexity
     */
    private double calculateVisualizationComplexity(RepositoryStructure structure, DependencyGraph dependencyGraph) {
        double nodeComplexity = Math.min(calculateTotalNodes(structure) / 100.0, 1.0);
        double edgeComplexity = Math.min(calculateTotalEdges(dependencyGraph) / 200.0, 1.0);
        double depthComplexity = Math.min(calculateMaxDepth(structure) / 10.0, 1.0);
        
        return (nodeComplexity + edgeComplexity + depthComplexity) / 3.0;
    }
    
    /**
     * Generate visualization recommendations
     */
    private List<String> generateVisualizationRecommendations(RepositoryStructure structure, 
                                                             DependencyGraph dependencyGraph) {
        List<String> recommendations = new ArrayList<>();
        
        int totalNodes = calculateTotalNodes(structure);
        int totalEdges = calculateTotalEdges(dependencyGraph);
        
        if (totalNodes > 500) {
            recommendations.add("Consider using filtered views for large repositories");
        }
        
        if (totalEdges > 100) {
            recommendations.add("Use clustering to group related dependencies");
        }
        
        if (structure.getMaxDirectoryDepth() > 8) {
            recommendations.add("Deep directory structure may benefit from hierarchical visualization");
        }
        
        if (dependencyGraph.getAverageCoupling() > 5.0) {
            recommendations.add("High coupling detected - consider dependency visualization");
        }
        
        return recommendations;
    }
    
    /**
     * Get parent path from file/directory path
     */
    private String getParentPath(String path) {
        if (path.equals("/") || path.isEmpty()) {
            return "root";
        }
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "root";
        }
        
        return path.substring(0, lastSlash);
    }
    
    /**
     * Get directory name from path
     */
    private String getDirectoryName(String path) {
        if (path.equals("/") || path.isEmpty()) {
            return "root";
        }
        
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}