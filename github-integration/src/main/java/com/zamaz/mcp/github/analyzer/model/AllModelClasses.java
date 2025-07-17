package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * This file contains all the remaining model classes needed for the repository structure analyzer
 */

/**
 * AST Analysis Result
 */
@Data
@Builder
public class ASTAnalysisResult {
    private String language;
    private int fileCount;
    private List<ASTNode> nodes;
    private List<ASTRelationship> relationships;
    private Map<String, Double> metrics;
    private Map<String, Double> complexity;
    private List<StructureInsight> issues;
}

/**
 * AST Node
 */
@Data
@Builder
public class ASTNode {
    private String id;
    private String name;
    private ModelEnums.ASTNodeType type;
    private String fileName;
    private int startLine;
    private int endLine;
    private Map<String, String> properties;
    private Map<String, Double> complexity;
}

/**
 * AST Relationship
 */
@Data
@Builder
public class ASTRelationship {
    private String sourceId;
    private String targetId;
    private String type;
    private String fileName;
}

/**
 * File AST Result
 */
@Data
@Builder
public class FileASTResult {
    private String fileName;
    private List<ASTNode> nodes;
    private List<ASTRelationship> relationships;
    private int classCount;
    private int methodCount;
    private int fieldCount;
}

/**
 * Code Organization Result
 */
@Data
@Builder
public class CodeOrganizationResult {
    private PackageStructure packageStructure;
    private ModuleStructure moduleStructure;
    private LayerStructure layerStructure;
    private FeatureStructure featureStructure;
    private OrganizationMetrics metrics;
    private List<StructureInsight> insights;
    private int packageDepth;
    private boolean hasLargeFiles;
}

/**
 * Package Structure
 */
@Data
@Builder
public class PackageStructure {
    private Map<String, PackageInfo> packages;
    private Map<String, List<String>> hierarchy;
    private int maxDepth;
    private int averageFilesPerPackage;
    private int totalPackages;
}

/**
 * Package Info
 */
@Data
@Builder
public class PackageInfo {
    private String name;
    private String path;
    private ModelEnums.DirectoryType type;
    private List<String> files;
    private int fileCount;
    private int lineCount;
    private int depth;
}

/**
 * Module Structure
 */
@Data
@Builder
public class ModuleStructure {
    private List<ModuleInfo> modules;
    private Map<String, List<String>> dependencies;
    private int totalModules;
}

/**
 * Module Info
 */
@Data
@Builder
public class ModuleInfo {
    private String name;
    private String path;
    private String type;
    private int totalFiles;
    private int sourceFiles;
    private int testFiles;
    private List<String> publicInterfaces;
}

/**
 * Layer Structure
 */
@Data
@Builder
public class LayerStructure {
    private Map<String, LayerInfo> layers;
    private Map<String, List<String>> dependencies;
    private int totalLayers;
}

/**
 * Layer Info
 */
@Data
@Builder
public class LayerInfo {
    private String name;
    private List<String> files;
    private int fileCount;
}

/**
 * Feature Structure
 */
@Data
@Builder
public class FeatureStructure {
    private List<FeatureInfo> features;
    private int totalFeatures;
}

/**
 * Feature Info
 */
@Data
@Builder
public class FeatureInfo {
    private String name;
    private String path;
    private List<String> files;
    private int fileCount;
}

/**
 * Organization Metrics
 */
@Data
@Builder
public class OrganizationMetrics {
    private double packageCohesion;
    private double moduleCohesion;
    private double packageCoupling;
    private double moduleCoupling;
    private double organizationComplexity;
}

/**
 * Architecture Pattern
 */
@Data
@Builder
public class ArchitecturePattern {
    private ModelEnums.ArchitecturePatternType type;
    private String name;
    private double confidence;
    private List<String> evidence;
    private String description;
    private List<String> benefits;
    private List<String> drawbacks;
    private List<String> relatedPatterns;
}

/**
 * Dependency Node
 */
@Data
@Builder
public class DependencyNode {
    private String id;
    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String scope;
    private List<String> dependencies;
    private List<String> dependents;
}

/**
 * Dependency Edge
 */
@Data
@Builder
public class DependencyEdge {
    private String from;
    private String to;
    private ModelEnums.DependencyEdgeType type;
    private String scope;
    private boolean optional;
}

/**
 * Dependency Cluster
 */
@Data
@Builder
public class DependencyCluster {
    private String id;
    private String name;
    private List<String> nodes;
    private String type;
}

/**
 * Dependency Exclusion
 */
@Data
@Builder
public class DependencyExclusion {
    private String groupId;
    private String artifactId;
}

/**
 * Security Vulnerability
 */
@Data
@Builder
public class SecurityVulnerability {
    private String id;
    private String title;
    private String description;
    private ModelEnums.VulnerabilitySeverity severity;
    private String cvssScore;
    private List<String> affectedVersions;
    private String fixedVersion;
    private List<String> references;
}

/**
 * Framework
 */
@Data
@Builder
public class Framework {
    private String name;
    private String version;
    private ModelEnums.FrameworkType type;
    private String description;
    private List<String> features;
}

/**
 * Repository Metadata
 */
@Data
@Builder
public class RepositoryMetadata {
    private String description;
    private String language;
    private int stars;
    private int forks;
    private int watchers;
    private String license;
    private String homepage;
    private List<String> topics;
    private long createdAt;
    private long updatedAt;
    private long size;
}

/**
 * File Complexity
 */
@Data
@Builder
public class FileComplexity {
    private int cyclomaticComplexity;
    private int cognitiveComplexity;
    private int linesOfCode;
    private int maintainabilityIndex;
    private ModelEnums.FileComplexity level;
}

/**
 * Analysis Summary
 */
@Data
@Builder
public class AnalysisSummary {
    private int totalFiles;
    private int totalDirectories;
    private int languageCount;
    private int projectTypeCount;
    private int dependencyCount;
    private int architecturePatternCount;
    private int insightCount;
    private int criticalInsightCount;
    private int warningInsightCount;
}

/**
 * Insight Summary
 */
@Data
@Builder
public class InsightSummary {
    private ModelEnums.InsightType type;
    private ModelEnums.InsightSeverity severity;
    private String title;
    private double confidence;
    private double impact;
    private double effort;
    private double priorityScore;
    private double estimatedTimeToFix;
    private int affectedFileCount;
    private boolean autoFixable;
}

/**
 * Dependency Graph Summary
 */
@Data
@Builder
public class DependencyGraphSummary {
    private int totalDependencies;
    private int totalNodes;
    private int totalEdges;
    private int circularDependencyCount;
    private double averageCoupling;
    private int maxDepth;
    private double complexityScore;
    private double maintainabilityScore;
    private boolean hasCriticalIssues;
}

/**
 * Structure Visualization
 */
@Data
@Builder
public class StructureVisualization {
    private VisualizationNode treeView;
    private VisualizationNode graphView;
    private VisualizationNode sunburstView;
    private VisualizationNode treemapView;
    private VisualizationNode networkView;
    private VisualizationMetadata metadata;
    private long timestamp;
}

/**
 * Visualization Node
 */
@Data
@Builder
public class VisualizationNode {
    private String id;
    private String name;
    private ModelEnums.VisualizationType type;
    private String nodeType;
    private List<VisualizationNode> children;
    private List<VisualizationEdge> edges;
    private Map<String, String> properties;
}

/**
 * Visualization Edge
 */
@Data
@Builder
public class VisualizationEdge {
    private String id;
    private String source;
    private String target;
    private String type;
    private Map<String, String> properties;
}

/**
 * Visualization Metadata
 */
@Data
@Builder
public class VisualizationMetadata {
    private int totalNodes;
    private int totalEdges;
    private int maxDepth;
    private double complexity;
    private List<String> recommendations;
}