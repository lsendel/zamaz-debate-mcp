package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents a detected project type in the repository
 */
@Data
@Builder
public class ProjectType {
    
    /**
     * Type of project (MAVEN, GRADLE, NODE_JS, PYTHON, etc.)
     */
    private ProjectTypeEnum type;
    
    /**
     * Primary programming language
     */
    private String language;
    
    /**
     * Project name
     */
    private String name;
    
    /**
     * Project version
     */
    private String version;
    
    /**
     * Root directory of this project
     */
    private String rootDirectory;
    
    /**
     * Configuration files that identified this project type
     */
    private List<String> configFiles;
    
    /**
     * Confidence score (0.0 to 1.0)
     */
    private double confidence;
    
    /**
     * Project dependencies
     */
    private List<Dependency> dependencies;
    
    /**
     * Build tools and their versions
     */
    private Map<String, String> buildTools;
    
    /**
     * Framework information
     */
    private List<Framework> frameworks;
    
    /**
     * Source directories
     */
    private List<String> sourceDirectories;
    
    /**
     * Test directories
     */
    private List<String> testDirectories;
    
    /**
     * Resource directories
     */
    private List<String> resourceDirectories;
    
    /**
     * Output/build directories
     */
    private List<String> outputDirectories;
    
    /**
     * Project metadata
     */
    private Map<String, String> metadata;
    
    /**
     * Check if this is a multi-module project
     */
    public boolean isMultiModule() {
        return metadata != null && 
               ("true".equals(metadata.get("multiModule")) || 
                metadata.containsKey("modules"));
    }
    
    /**
     * Check if this project uses specific framework
     */
    public boolean usesFramework(String frameworkName) {
        return frameworks != null && 
               frameworks.stream()
                   .anyMatch(f -> f.getName().equalsIgnoreCase(frameworkName));
    }
    
    /**
     * Check if this project has specific dependency
     */
    public boolean hasDependency(String groupId, String artifactId) {
        return dependencies != null && 
               dependencies.stream()
                   .anyMatch(d -> d.getGroupId().equals(groupId) && 
                                 d.getArtifactId().equals(artifactId));
    }
    
    /**
     * Get framework by name
     */
    public Framework getFramework(String frameworkName) {
        return frameworks != null ? 
               frameworks.stream()
                   .filter(f -> f.getName().equalsIgnoreCase(frameworkName))
                   .findFirst()
                   .orElse(null) : null;
    }
    
    /**
     * Get primary build tool
     */
    public String getPrimaryBuildTool() {
        if (buildTools == null || buildTools.isEmpty()) {
            return null;
        }
        
        // Common build tools in order of priority
        String[] commonTools = {"maven", "gradle", "npm", "yarn", "pip", "cargo", "go"};
        
        for (String tool : commonTools) {
            if (buildTools.containsKey(tool)) {
                return tool;
            }
        }
        
        // Return first available tool
        return buildTools.keySet().iterator().next();
    }
    
    /**
     * Get description of this project type
     */
    public String getDescription() {
        return switch (type) {
            case MAVEN -> "Maven-based Java project";
            case GRADLE -> "Gradle-based Java/Kotlin project";
            case NODE_JS -> "Node.js JavaScript/TypeScript project";
            case PYTHON -> "Python project";
            case SPRING_BOOT -> "Spring Boot application";
            case REACT -> "React application";
            case ANGULAR -> "Angular application";
            case VUE -> "Vue.js application";
            case DOCKER -> "Dockerized application";
            case KUBERNETES -> "Kubernetes application";
            case MICROSERVICE -> "Microservice architecture";
            case MONOLITH -> "Monolithic application";
            case LIBRARY -> "Library/SDK project";
            case UNKNOWN -> "Unknown project type";
        };
    }
    
    /**
     * Check if this is a web application project
     */
    public boolean isWebApplication() {
        return type == ProjectTypeEnum.SPRING_BOOT || 
               type == ProjectTypeEnum.REACT || 
               type == ProjectTypeEnum.ANGULAR || 
               type == ProjectTypeEnum.VUE ||
               usesFramework("express") ||
               usesFramework("django") ||
               usesFramework("flask");
    }
    
    /**
     * Check if this is a Java project
     */
    public boolean isJavaProject() {
        return "java".equalsIgnoreCase(language) || 
               type == ProjectTypeEnum.MAVEN || 
               type == ProjectTypeEnum.GRADLE ||
               type == ProjectTypeEnum.SPRING_BOOT;
    }
}