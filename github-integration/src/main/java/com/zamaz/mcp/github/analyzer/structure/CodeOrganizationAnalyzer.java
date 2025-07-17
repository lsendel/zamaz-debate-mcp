package com.zamaz.mcp.github.analyzer.structure;

import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.DirectoryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes code organization patterns and structure
 */
@Component
@Slf4j
public class CodeOrganizationAnalyzer {
    
    /**
     * Analyze code organization structure
     */
    public CodeOrganizationResult analyzeOrganization(RepositoryStructure structure, 
                                                     Map<String, ASTAnalysisResult> astResults) {
        log.info("Analyzing code organization for repository {}/{}", 
                structure.getOwner(), structure.getRepository());
        
        // Analyze package structure
        PackageStructure packageStructure = analyzePackageStructure(structure);
        
        // Analyze module organization
        ModuleStructure moduleStructure = analyzeModuleStructure(structure, astResults);
        
        // Analyze layer organization
        LayerStructure layerStructure = analyzeLayerStructure(structure, astResults);
        
        // Analyze feature organization
        FeatureStructure featureStructure = analyzeFeatureStructure(structure, astResults);
        
        // Calculate organization metrics
        OrganizationMetrics metrics = calculateOrganizationMetrics(structure, packageStructure, moduleStructure);
        
        // Generate organization insights
        List<StructureInsight> insights = generateOrganizationInsights(structure, packageStructure, moduleStructure, metrics);
        
        CodeOrganizationResult result = CodeOrganizationResult.builder()
                .packageStructure(packageStructure)
                .moduleStructure(moduleStructure)
                .layerStructure(layerStructure)
                .featureStructure(featureStructure)
                .metrics(metrics)
                .insights(insights)
                .packageDepth(packageStructure.getMaxDepth())
                .hasLargeFiles(hasLargeFiles(structure))
                .build();
        
        log.info("Code organization analysis completed. Package depth: {}, Modules: {}, Large files: {}", 
                result.getPackageDepth(), moduleStructure.getModules().size(), result.isHasLargeFiles());
        
        return result;
    }
    
    /**
     * Analyze package structure
     */
    private PackageStructure analyzePackageStructure(RepositoryStructure structure) {
        log.info("Analyzing package structure");
        
        Map<String, PackageInfo> packages = new HashMap<>();
        
        // Analyze directories to identify packages
        for (DirectoryInfo directory : structure.getDirectories()) {
            PackageInfo packageInfo = analyzePackage(directory, structure);
            if (packageInfo != null) {
                packages.put(packageInfo.getName(), packageInfo);
            }
        }
        
        // Build package hierarchy
        Map<String, List<String>> hierarchy = buildPackageHierarchy(packages);
        
        // Calculate package metrics
        int maxDepth = calculateMaxPackageDepth(hierarchy);
        int avgFilesPerPackage = calculateAverageFilesPerPackage(packages);
        
        return PackageStructure.builder()
                .packages(packages)
                .hierarchy(hierarchy)
                .maxDepth(maxDepth)
                .averageFilesPerPackage(avgFilesPerPackage)
                .totalPackages(packages.size())
                .build();
    }
    
    /**
     * Analyze individual package
     */
    private PackageInfo analyzePackage(DirectoryInfo directory, RepositoryStructure structure) {
        // Skip non-source directories
        if (!isSourceDirectory(directory)) {
            return null;
        }
        
        List<FileInfo> packageFiles = structure.getFiles().stream()
                .filter(file -> file.getPath().startsWith(directory.getPath()))
                .toList();
        
        // Determine package type
        DirectoryType packageType = determinePackageType(directory, packageFiles);
        
        // Calculate package metrics
        int fileCount = packageFiles.size();
        int lineCount = packageFiles.stream().mapToInt(FileInfo::getLineCount).sum();
        
        return PackageInfo.builder()
                .name(directory.getName())
                .path(directory.getPath())
                .type(packageType)
                .files(packageFiles.stream().map(FileInfo::getName).toList())
                .fileCount(fileCount)
                .lineCount(lineCount)
                .depth(directory.getDepth())
                .build();
    }
    
    /**
     * Analyze module structure
     */
    private ModuleStructure analyzeModuleStructure(RepositoryStructure structure, Map<String, ASTAnalysisResult> astResults) {
        log.info("Analyzing module structure");
        
        List<ModuleInfo> modules = new ArrayList<>();
        
        // Look for module indicators
        for (DirectoryInfo directory : structure.getDirectories()) {
            ModuleInfo moduleInfo = analyzeModule(directory, structure, astResults);
            if (moduleInfo != null) {
                modules.add(moduleInfo);
            }
        }
        
        // Analyze module dependencies
        Map<String, List<String>> moduleDependencies = analyzeModuleDependencies(modules, astResults);
        
        return ModuleStructure.builder()
                .modules(modules)
                .dependencies(moduleDependencies)
                .totalModules(modules.size())
                .build();
    }
    
    /**
     * Analyze individual module
     */
    private ModuleInfo analyzeModule(DirectoryInfo directory, RepositoryStructure structure, Map<String, ASTAnalysisResult> astResults) {
        // Check for module indicators
        if (!isModuleDirectory(directory, structure)) {
            return null;
        }
        
        List<FileInfo> moduleFiles = structure.getFiles().stream()
                .filter(file -> file.getPath().startsWith(directory.getPath()))
                .toList();
        
        // Determine module type
        String moduleType = determineModuleType(directory, moduleFiles);
        
        // Get module interfaces (public API)
        List<String> publicInterfaces = extractPublicInterfaces(moduleFiles, astResults);
        
        // Calculate module metrics
        int totalFiles = moduleFiles.size();
        int sourceFiles = (int) moduleFiles.stream().filter(FileInfo::isSourceFile).count();
        int testFiles = (int) moduleFiles.stream().filter(FileInfo::isTestFile).count();
        
        return ModuleInfo.builder()
                .name(directory.getName())
                .path(directory.getPath())
                .type(moduleType)
                .totalFiles(totalFiles)
                .sourceFiles(sourceFiles)
                .testFiles(testFiles)
                .publicInterfaces(publicInterfaces)
                .build();
    }
    
    /**
     * Analyze layer structure (presentation, business, data)
     */
    private LayerStructure analyzeLayerStructure(RepositoryStructure structure, Map<String, ASTAnalysisResult> astResults) {
        log.info("Analyzing layer structure");
        
        Map<String, LayerInfo> layers = new HashMap<>();
        
        // Common layer patterns
        String[] layerPatterns = {
            "controller", "presentation", "ui", "web",
            "service", "business", "domain", "logic",
            "repository", "dao", "data", "persistence",
            "model", "entity", "dto", "vo"
        };
        
        for (String pattern : layerPatterns) {
            LayerInfo layer = analyzeLayer(pattern, structure, astResults);
            if (layer != null) {
                layers.put(pattern, layer);
            }
        }
        
        // Analyze layer dependencies
        Map<String, List<String>> layerDependencies = analyzeLayerDependencies(layers, astResults);
        
        return LayerStructure.builder()
                .layers(layers)
                .dependencies(layerDependencies)
                .totalLayers(layers.size())
                .build();
    }
    
    /**
     * Analyze feature structure (feature-based organization)
     */
    private FeatureStructure analyzeFeatureStructure(RepositoryStructure structure, Map<String, ASTAnalysisResult> astResults) {
        log.info("Analyzing feature structure");
        
        List<FeatureInfo> features = new ArrayList<>();
        
        // Look for feature directories
        for (DirectoryInfo directory : structure.getDirectories()) {
            FeatureInfo featureInfo = analyzeFeature(directory, structure, astResults);
            if (featureInfo != null) {
                features.add(featureInfo);
            }
        }
        
        return FeatureStructure.builder()
                .features(features)
                .totalFeatures(features.size())
                .build();
    }
    
    /**
     * Calculate organization metrics
     */
    private OrganizationMetrics calculateOrganizationMetrics(RepositoryStructure structure, 
                                                           PackageStructure packageStructure,
                                                           ModuleStructure moduleStructure) {
        
        // Calculate cohesion metrics
        double packageCohesion = calculatePackageCohesion(packageStructure);
        double moduleCohesion = calculateModuleCohesion(moduleStructure);
        
        // Calculate coupling metrics
        double packageCoupling = calculatePackageCoupling(packageStructure);
        double moduleCoupling = calculateModuleCoupling(moduleStructure);
        
        // Calculate complexity metrics
        double organizationComplexity = calculateOrganizationComplexity(structure, packageStructure);
        
        return OrganizationMetrics.builder()
                .packageCohesion(packageCohesion)
                .moduleCohesion(moduleCohesion)
                .packageCoupling(packageCoupling)
                .moduleCoupling(moduleCoupling)
                .organizationComplexity(organizationComplexity)
                .build();
    }
    
    /**
     * Check if directory is a source directory
     */
    private boolean isSourceDirectory(DirectoryInfo directory) {
        String path = directory.getPath().toLowerCase();
        return path.contains("/src/") || 
               path.contains("/source/") || 
               path.contains("/lib/") ||
               (!path.contains("/test/") && !path.contains("/tests/") && 
                !path.contains("/node_modules/") && !path.contains("/target/") && 
                !path.contains("/build/") && !path.contains("/dist/"));
    }
    
    /**
     * Determine package type based on directory and files
     */
    private DirectoryType determinePackageType(DirectoryInfo directory, List<FileInfo> files) {
        String name = directory.getName().toLowerCase();
        
        if (name.contains("controller") || name.contains("rest") || name.contains("api")) {
            return DirectoryType.LAYER;
        }
        if (name.contains("service") || name.contains("business") || name.contains("logic")) {
            return DirectoryType.LAYER;
        }
        if (name.contains("repository") || name.contains("dao") || name.contains("data")) {
            return DirectoryType.LAYER;
        }
        if (name.contains("model") || name.contains("entity") || name.contains("dto")) {
            return DirectoryType.COMPONENT;
        }
        if (name.contains("util") || name.contains("helper") || name.contains("common")) {
            return DirectoryType.UTILITY;
        }
        if (name.contains("config") || name.contains("configuration")) {
            return DirectoryType.CONFIGURATION;
        }
        if (name.contains("test") || name.contains("spec")) {
            return DirectoryType.TEST;
        }
        
        return DirectoryType.PACKAGE;
    }
    
    /**
     * Check if directory represents a module
     */
    private boolean isModuleDirectory(DirectoryInfo directory, RepositoryStructure structure) {
        // Look for module indicators
        String path = directory.getPath();
        
        // Check for build files that indicate a module
        boolean hasBuildFile = structure.getFiles().stream()
                .anyMatch(file -> file.getPath().startsWith(path) && 
                         (file.getName().equals("pom.xml") || 
                          file.getName().equals("build.gradle") || 
                          file.getName().equals("package.json")));
        
        // Check for source directory structure
        boolean hasSourceStructure = structure.getDirectories().stream()
                .anyMatch(dir -> dir.getPath().startsWith(path) && 
                               (dir.getName().equals("src") || dir.getName().equals("source")));
        
        return hasBuildFile || hasSourceStructure;
    }
    
    /**
     * Determine module type
     */
    private String determineModuleType(DirectoryInfo directory, List<FileInfo> files) {
        String name = directory.getName().toLowerCase();
        
        if (name.contains("api") || name.contains("interface")) {
            return "api";
        }
        if (name.contains("impl") || name.contains("implementation")) {
            return "implementation";
        }
        if (name.contains("web") || name.contains("ui")) {
            return "web";
        }
        if (name.contains("service") || name.contains("business")) {
            return "service";
        }
        if (name.contains("data") || name.contains("persistence")) {
            return "data";
        }
        if (name.contains("common") || name.contains("shared")) {
            return "common";
        }
        if (name.contains("test") || name.contains("testing")) {
            return "test";
        }
        
        return "application";
    }
    
    /**
     * Extract public interfaces from module files
     */
    private List<String> extractPublicInterfaces(List<FileInfo> files, Map<String, ASTAnalysisResult> astResults) {
        List<String> publicInterfaces = new ArrayList<>();
        
        for (FileInfo file : files) {
            if (file.isSourceFile()) {
                String language = file.getLanguage();
                ASTAnalysisResult astResult = astResults.get(language);
                
                if (astResult != null) {
                    // TODO: Extract public classes, interfaces, methods from AST
                    // For now, use simple heuristics
                    if (file.getName().toLowerCase().contains("interface") || 
                        file.getName().toLowerCase().contains("api")) {
                        publicInterfaces.add(file.getName());
                    }
                }
            }
        }
        
        return publicInterfaces;
    }
    
    /**
     * Build package hierarchy
     */
    private Map<String, List<String>> buildPackageHierarchy(Map<String, PackageInfo> packages) {
        Map<String, List<String>> hierarchy = new HashMap<>();
        
        for (PackageInfo pkg : packages.values()) {
            String parentPath = getParentPath(pkg.getPath());
            
            if (packages.containsKey(parentPath)) {
                hierarchy.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(pkg.getName());
            }
        }
        
        return hierarchy;
    }
    
    /**
     * Calculate maximum package depth
     */
    private int calculateMaxPackageDepth(Map<String, List<String>> hierarchy) {
        return hierarchy.keySet().stream()
                .mapToInt(path -> path.split("/").length)
                .max()
                .orElse(0);
    }
    
    /**
     * Calculate average files per package
     */
    private int calculateAverageFilesPerPackage(Map<String, PackageInfo> packages) {
        return packages.values().stream()
                .mapToInt(PackageInfo::getFileCount)
                .sum() / Math.max(packages.size(), 1);
    }
    
    /**
     * Analyze module dependencies
     */
    private Map<String, List<String>> analyzeModuleDependencies(List<ModuleInfo> modules, Map<String, ASTAnalysisResult> astResults) {
        Map<String, List<String>> dependencies = new HashMap<>();
        
        // TODO: Implement actual dependency analysis using AST results
        // For now, return empty dependencies
        for (ModuleInfo module : modules) {
            dependencies.put(module.getName(), new ArrayList<>());
        }
        
        return dependencies;
    }
    
    /**
     * Analyze layer for specific pattern
     */
    private LayerInfo analyzeLayer(String pattern, RepositoryStructure structure, Map<String, ASTAnalysisResult> astResults) {
        List<FileInfo> layerFiles = structure.getFiles().stream()
                .filter(file -> file.getPath().toLowerCase().contains(pattern))
                .toList();
        
        if (layerFiles.isEmpty()) {
            return null;
        }
        
        return LayerInfo.builder()
                .name(pattern)
                .files(layerFiles.stream().map(FileInfo::getName).toList())
                .fileCount(layerFiles.size())
                .build();
    }
    
    /**
     * Analyze layer dependencies
     */
    private Map<String, List<String>> analyzeLayerDependencies(Map<String, LayerInfo> layers, Map<String, ASTAnalysisResult> astResults) {
        Map<String, List<String>> dependencies = new HashMap<>();
        
        // TODO: Implement actual layer dependency analysis
        // For now, return empty dependencies
        for (String layerName : layers.keySet()) {
            dependencies.put(layerName, new ArrayList<>());
        }
        
        return dependencies;
    }
    
    /**
     * Analyze feature
     */
    private FeatureInfo analyzeFeature(DirectoryInfo directory, RepositoryStructure structure, Map<String, ASTAnalysisResult> astResults) {
        // Simple heuristic: directories with multiple file types might be features
        List<FileInfo> featureFiles = structure.getFiles().stream()
                .filter(file -> file.getPath().startsWith(directory.getPath()))
                .toList();
        
        if (featureFiles.size() < 3) {
            return null; // Too small to be a feature
        }
        
        Set<String> fileTypes = featureFiles.stream()
                .map(FileInfo::getExtension)
                .collect(Collectors.toSet());
        
        if (fileTypes.size() < 2) {
            return null; // Not diverse enough
        }
        
        return FeatureInfo.builder()
                .name(directory.getName())
                .path(directory.getPath())
                .files(featureFiles.stream().map(FileInfo::getName).toList())
                .fileCount(featureFiles.size())
                .build();
    }
    
    /**
     * Calculate package cohesion
     */
    private double calculatePackageCohesion(PackageStructure packageStructure) {
        // TODO: Implement actual cohesion calculation
        // For now, return a mock value
        return 0.7;
    }
    
    /**
     * Calculate module cohesion
     */
    private double calculateModuleCohesion(ModuleStructure moduleStructure) {
        // TODO: Implement actual cohesion calculation
        return 0.8;
    }
    
    /**
     * Calculate package coupling
     */
    private double calculatePackageCoupling(PackageStructure packageStructure) {
        // TODO: Implement actual coupling calculation
        return 0.3;
    }
    
    /**
     * Calculate module coupling
     */
    private double calculateModuleCoupling(ModuleStructure moduleStructure) {
        // TODO: Implement actual coupling calculation
        return 0.4;
    }
    
    /**
     * Calculate organization complexity
     */
    private double calculateOrganizationComplexity(RepositoryStructure structure, PackageStructure packageStructure) {
        double fileComplexity = Math.min(structure.getFiles().size() / 100.0, 1.0);
        double packageComplexity = Math.min(packageStructure.getTotalPackages() / 50.0, 1.0);
        double depthComplexity = Math.min(packageStructure.getMaxDepth() / 10.0, 1.0);
        
        return (fileComplexity + packageComplexity + depthComplexity) / 3.0;
    }
    
    /**
     * Check if repository has large files
     */
    private boolean hasLargeFiles(RepositoryStructure structure) {
        return structure.getFiles().stream()
                .anyMatch(file -> file.getLineCount() > 1000);
    }
    
    /**
     * Generate organization insights
     */
    private List<StructureInsight> generateOrganizationInsights(RepositoryStructure structure, 
                                                               PackageStructure packageStructure,
                                                               ModuleStructure moduleStructure,
                                                               OrganizationMetrics metrics) {
        List<StructureInsight> insights = new ArrayList<>();
        
        // Package organization insights
        if (packageStructure.getMaxDepth() > 6) {
            insights.add(StructureInsight.builder()
                    .type(ModelEnums.InsightType.DEEP_PACKAGE_STRUCTURE)
                    .severity(ModelEnums.InsightSeverity.WARNING)
                    .title("Deep Package Structure")
                    .description("Package structure is very deep with " + packageStructure.getMaxDepth() + " levels")
                    .recommendation("Consider flattening the package structure")
                    .confidence(0.8)
                    .impact(0.6)
                    .effort(0.7)
                    .build());
        }
        
        // Module organization insights
        if (moduleStructure.getTotalModules() > 20) {
            insights.add(StructureInsight.builder()
                    .type(ModelEnums.InsightType.LARGE_REPOSITORY)
                    .severity(ModelEnums.InsightSeverity.INFO)
                    .title("Many Modules")
                    .description("Repository has " + moduleStructure.getTotalModules() + " modules")
                    .recommendation("Consider if all modules are necessary or if some can be combined")
                    .confidence(0.7)
                    .impact(0.5)
                    .effort(0.8)
                    .build());
        }
        
        return insights;
    }
    
    /**
     * Get parent path from a given path
     */
    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "/";
    }
}