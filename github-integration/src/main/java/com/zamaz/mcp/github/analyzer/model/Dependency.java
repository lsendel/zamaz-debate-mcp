package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents a dependency in the project
 */
@Data
@Builder
public class Dependency {
    
    /**
     * Group ID (e.g., org.springframework.boot)
     */
    private String groupId;
    
    /**
     * Artifact ID (e.g., spring-boot-starter-web)
     */
    private String artifactId;
    
    /**
     * Version (e.g., 2.7.0)
     */
    private String version;
    
    /**
     * Dependency type (jar, war, pom, npm, wheel, etc.)
     */
    private String type;
    
    /**
     * Scope (compile, runtime, test, provided, etc.)
     */
    private String scope;
    
    /**
     * Whether this dependency is optional
     */
    private boolean optional;
    
    /**
     * Classifier (e.g., sources, javadoc)
     */
    private String classifier;
    
    /**
     * System path (for system scope dependencies)
     */
    private String systemPath;
    
    /**
     * Transitive dependencies
     */
    private List<Dependency> dependencies;
    
    /**
     * Exclusions
     */
    private List<DependencyExclusion> exclusions;
    
    /**
     * License information
     */
    private String license;
    
    /**
     * Description
     */
    private String description;
    
    /**
     * Homepage URL
     */
    private String homepage;
    
    /**
     * Source repository URL
     */
    private String repository;
    
    /**
     * File size in bytes
     */
    private long size;
    
    /**
     * SHA checksum
     */
    private String sha;
    
    /**
     * Security vulnerabilities
     */
    private List<SecurityVulnerability> vulnerabilities;
    
    /**
     * Dependency metadata
     */
    private Map<String, String> metadata;
    
    /**
     * Whether this dependency is outdated
     */
    private boolean outdated;
    
    /**
     * Latest available version
     */
    private String latestVersion;
    
    /**
     * Dependency category (framework, utility, testing, etc.)
     */
    private DependencyCategory category;
    
    /**
     * Get full dependency identifier
     */
    public String getFullId() {
        return groupId + ":" + artifactId + ":" + version;
    }
    
    /**
     * Get short identifier (without version)
     */
    public String getShortId() {
        return groupId + ":" + artifactId;
    }
    
    /**
     * Check if this is a direct dependency (not transitive)
     */
    public boolean isDirect() {
        return "compile".equals(scope) || "runtime".equals(scope) || "provided".equals(scope);
    }
    
    /**
     * Check if this is a test dependency
     */
    public boolean isTestDependency() {
        return "test".equals(scope);
    }
    
    /**
     * Check if this dependency has security vulnerabilities
     */
    public boolean hasSecurityVulnerabilities() {
        return vulnerabilities != null && !vulnerabilities.isEmpty();
    }
    
    /**
     * Get high severity vulnerabilities
     */
    public List<SecurityVulnerability> getHighSeverityVulnerabilities() {
        return vulnerabilities != null ? 
               vulnerabilities.stream()
                   .filter(v -> v.getSeverity() == VulnerabilitySeverity.HIGH || 
                               v.getSeverity() == VulnerabilitySeverity.CRITICAL)
                   .toList() : 
               List.of();
    }
    
    /**
     * Check if dependency is transitive
     */
    public boolean isTransitive() {
        return dependencies != null && !dependencies.isEmpty();
    }
    
    /**
     * Get dependency depth (number of transitive levels)
     */
    public int getDepth() {
        if (dependencies == null || dependencies.isEmpty()) {
            return 0;
        }
        
        int maxDepth = 0;
        for (Dependency dep : dependencies) {
            maxDepth = Math.max(maxDepth, dep.getDepth());
        }
        
        return maxDepth + 1;
    }
    
    /**
     * Get total transitive dependency count
     */
    public int getTransitiveDependencyCount() {
        if (dependencies == null || dependencies.isEmpty()) {
            return 0;
        }
        
        int count = dependencies.size();
        for (Dependency dep : dependencies) {
            count += dep.getTransitiveDependencyCount();
        }
        
        return count;
    }
    
    /**
     * Check if version is a snapshot
     */
    public boolean isSnapshot() {
        return version != null && version.contains("SNAPSHOT");
    }
    
    /**
     * Check if version is a release candidate
     */
    public boolean isReleaseCandidate() {
        return version != null && (version.contains("RC") || version.contains("rc"));
    }
    
    /**
     * Check if version is a milestone
     */
    public boolean isMilestone() {
        return version != null && (version.contains("M") || version.contains("milestone"));
    }
    
    /**
     * Check if this is a stable version
     */
    public boolean isStableVersion() {
        return !isSnapshot() && !isReleaseCandidate() && !isMilestone();
    }
}