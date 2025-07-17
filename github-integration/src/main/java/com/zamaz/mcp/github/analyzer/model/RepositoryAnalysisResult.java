package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive result of repository structure analysis
 */
@Data
@Builder
public class RepositoryAnalysisResult {
    
    /**
     * Basic repository structure information
     */
    private RepositoryStructure repositoryStructure;
    
    /**
     * Detected project types (Maven, Gradle, Node.js, etc.)
     */
    private List<ProjectType> projectTypes;
    
    /**
     * Dependency graph analysis
     */
    private DependencyGraph dependencyGraph;
    
    /**
     * AST analysis results by language
     */
    private Map<String, ASTAnalysisResult> astResults;
    
    /**
     * Code organization analysis
     */
    private CodeOrganizationResult codeOrganization;
    
    /**
     * Detected architecture patterns
     */
    private List<ArchitecturePattern> architecturePatterns;
    
    /**
     * Generated insights and recommendations
     */
    private List<StructureInsight> insights;
    
    /**
     * Visualization data
     */
    private StructureVisualization visualization;
    
    /**
     * Analysis timestamp
     */
    private long analysisTimestamp;
    
    /**
     * Analysis duration in milliseconds
     */
    private long analysisDuration;
    
    /**
     * Analysis version for tracking changes
     */
    private String analysisVersion;
    
    /**
     * Get summary statistics
     */
    public AnalysisSummary getSummary() {
        return AnalysisSummary.builder()
                .totalFiles(repositoryStructure.getFiles().size())
                .totalDirectories(repositoryStructure.getDirectories().size())
                .languageCount(astResults.size())
                .projectTypeCount(projectTypes.size())
                .dependencyCount(dependencyGraph.getDependencies().size())
                .architecturePatternCount(architecturePatterns.size())
                .insightCount(insights.size())
                .criticalInsightCount((int) insights.stream()
                        .filter(insight -> insight.getSeverity() == InsightSeverity.ERROR)
                        .count())
                .warningInsightCount((int) insights.stream()
                        .filter(insight -> insight.getSeverity() == InsightSeverity.WARNING)
                        .count())
                .build();
    }
    
    /**
     * Get insights by severity
     */
    public List<StructureInsight> getInsightsBySeverity(InsightSeverity severity) {
        return insights.stream()
                .filter(insight -> insight.getSeverity() == severity)
                .toList();
    }
    
    /**
     * Get insights by type
     */
    public List<StructureInsight> getInsightsByType(InsightType type) {
        return insights.stream()
                .filter(insight -> insight.getType() == type)
                .toList();
    }
    
    /**
     * Check if repository has critical issues
     */
    public boolean hasCriticalIssues() {
        return insights.stream()
                .anyMatch(insight -> insight.getSeverity() == InsightSeverity.ERROR);
    }
    
    /**
     * Check if repository has warnings
     */
    public boolean hasWarnings() {
        return insights.stream()
                .anyMatch(insight -> insight.getSeverity() == InsightSeverity.WARNING);
    }
    
    /**
     * Get primary programming language
     */
    public String getPrimaryLanguage() {
        return astResults.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> 
                        Integer.compare(a.getFileCount(), b.getFileCount())))
                .map(Map.Entry::getKey)
                .orElse("unknown");
    }
    
    /**
     * Get complexity score (0-100)
     */
    public double getComplexityScore() {
        double fileComplexity = Math.min(repositoryStructure.getFiles().size() / 10.0, 30.0);
        double dependencyComplexity = Math.min(dependencyGraph.getDependencies().size() / 5.0, 20.0);
        double organizationComplexity = Math.min(codeOrganization.getPackageDepth() * 5.0, 25.0);
        double patternComplexity = Math.min(architecturePatterns.size() * 5.0, 25.0);
        
        return Math.min(fileComplexity + dependencyComplexity + organizationComplexity + patternComplexity, 100.0);
    }
    
    /**
     * Get maintainability score (0-100)
     */
    public double getMaintainabilityScore() {
        double baseScore = 100.0;
        
        // Reduce score based on critical issues
        baseScore -= getInsightsBySeverity(InsightSeverity.ERROR).size() * 15.0;
        
        // Reduce score based on warnings
        baseScore -= getInsightsBySeverity(InsightSeverity.WARNING).size() * 5.0;
        
        // Reduce score based on complexity
        baseScore -= getComplexityScore() * 0.3;
        
        // Increase score for good architecture patterns
        baseScore += architecturePatterns.size() * 2.0;
        
        return Math.max(Math.min(baseScore, 100.0), 0.0);
    }
}