package com.zamaz.mcp.github.analyzer;

import com.zamaz.mcp.github.analyzer.ast.ASTAnalyzer;
import com.zamaz.mcp.github.analyzer.ast.JavaASTAnalyzer;
import com.zamaz.mcp.github.analyzer.ast.PythonASTAnalyzer;
import com.zamaz.mcp.github.analyzer.ast.JavaScriptASTAnalyzer;
import com.zamaz.mcp.github.analyzer.ast.TypeScriptASTAnalyzer;
import com.zamaz.mcp.github.analyzer.dependency.DependencyAnalyzer;
import com.zamaz.mcp.github.analyzer.pattern.ArchitecturePatternDetector;
import com.zamaz.mcp.github.analyzer.project.ProjectTypeDetector;
import com.zamaz.mcp.github.analyzer.structure.CodeOrganizationAnalyzer;
import com.zamaz.mcp.github.analyzer.visualization.StructureVisualizer;
import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.service.GitHubApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for analyzing repository structure and providing comprehensive insights
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryStructureAnalyzer {

    private final GitHubApiClient apiClient;
    private final ProjectTypeDetector projectTypeDetector;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final CodeOrganizationAnalyzer codeOrganizationAnalyzer;
    private final ArchitecturePatternDetector architecturePatternDetector;
    private final StructureVisualizer structureVisualizer;
    private final Map<String, ASTAnalyzer> astAnalyzers;

    public RepositoryStructureAnalyzer(
            GitHubApiClient apiClient,
            ProjectTypeDetector projectTypeDetector,
            DependencyAnalyzer dependencyAnalyzer,
            CodeOrganizationAnalyzer codeOrganizationAnalyzer,
            ArchitecturePatternDetector architecturePatternDetector,
            StructureVisualizer structureVisualizer) {
        
        this.apiClient = apiClient;
        this.projectTypeDetector = projectTypeDetector;
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.codeOrganizationAnalyzer = codeOrganizationAnalyzer;
        this.architecturePatternDetector = architecturePatternDetector;
        this.structureVisualizer = structureVisualizer;
        
        // Initialize AST analyzers
        this.astAnalyzers = new HashMap<>();
        this.astAnalyzers.put("java", new JavaASTAnalyzer());
        this.astAnalyzers.put("python", new PythonASTAnalyzer());
        this.astAnalyzers.put("javascript", new JavaScriptASTAnalyzer());
        this.astAnalyzers.put("typescript", new TypeScriptASTAnalyzer());
    }

    /**
     * Perform comprehensive repository structure analysis
     */
    public RepositoryAnalysisResult analyzeRepository(String accessToken, String owner, String repo, String branch) {
        log.info("Starting comprehensive repository analysis for {}/{} on branch {}", owner, repo, branch);
        
        try {
            // 1. Get repository structure
            RepositoryStructure structure = getRepositoryStructure(accessToken, owner, repo, branch);
            
            // 2. Detect project types
            List<ProjectType> projectTypes = projectTypeDetector.detectProjectTypes(structure);
            
            // 3. Analyze dependencies
            DependencyGraph dependencyGraph = dependencyAnalyzer.analyzeDependencies(structure, projectTypes);
            
            // 4. Perform AST analysis for each supported language
            Map<String, ASTAnalysisResult> astResults = performASTAnalysis(structure);
            
            // 5. Analyze code organization
            CodeOrganizationResult organizationResult = codeOrganizationAnalyzer.analyzeOrganization(structure, astResults);
            
            // 6. Detect architecture patterns
            List<ArchitecturePattern> architecturePatterns = architecturePatternDetector.detectPatterns(structure, organizationResult, dependencyGraph);
            
            // 7. Generate insights and recommendations
            List<StructureInsight> insights = generateInsights(structure, projectTypes, dependencyGraph, organizationResult, architecturePatterns);
            
            // 8. Create visualizations
            StructureVisualization visualization = structureVisualizer.createVisualization(structure, dependencyGraph, organizationResult);
            
            // 9. Build final result
            RepositoryAnalysisResult result = RepositoryAnalysisResult.builder()
                    .repositoryStructure(structure)
                    .projectTypes(projectTypes)
                    .dependencyGraph(dependencyGraph)
                    .astResults(astResults)
                    .codeOrganization(organizationResult)
                    .architecturePatterns(architecturePatterns)
                    .insights(insights)
                    .visualization(visualization)
                    .analysisTimestamp(System.currentTimeMillis())
                    .build();
            
            log.info("Completed repository analysis for {}/{}", owner, repo);
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing repository {}/{}: {}", owner, repo, e.getMessage(), e);
            throw new RuntimeException("Failed to analyze repository structure", e);
        }
    }

    /**
     * Get repository structure from GitHub API
     */
    private RepositoryStructure getRepositoryStructure(String accessToken, String owner, String repo, String branch) {
        log.info("Fetching repository structure for {}/{} on branch {}", owner, repo, branch);
        
        // This will be implemented to recursively fetch the entire repository structure
        // For now, we'll create a placeholder structure
        return RepositoryStructure.builder()
                .owner(owner)
                .repository(repo)
                .branch(branch)
                .rootPath("/")
                .files(new ArrayList<>())
                .directories(new ArrayList<>())
                .build();
    }

    /**
     * Perform AST analysis for all supported languages
     */
    private Map<String, ASTAnalysisResult> performASTAnalysis(RepositoryStructure structure) {
        log.info("Performing AST analysis for repository");
        
        Map<String, ASTAnalysisResult> results = new HashMap<>();
        
        // Group files by language
        Map<String, List<FileInfo>> filesByLanguage = structure.getFiles().stream()
                .collect(Collectors.groupingBy(this::detectLanguage))
                .entrySet().stream()
                .filter(entry -> astAnalyzers.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // Analyze each language
        for (Map.Entry<String, List<FileInfo>> entry : filesByLanguage.entrySet()) {
            String language = entry.getKey();
            List<FileInfo> files = entry.getValue();
            
            ASTAnalyzer analyzer = astAnalyzers.get(language);
            if (analyzer != null) {
                try {
                    ASTAnalysisResult result = analyzer.analyzeFiles(files);
                    results.put(language, result);
                    log.info("Completed AST analysis for {} files in {}", files.size(), language);
                } catch (Exception e) {
                    log.error("Error analyzing {} files: {}", language, e.getMessage(), e);
                }
            }
        }
        
        return results;
    }

    /**
     * Detect programming language from file extension
     */
    private String detectLanguage(FileInfo file) {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.endsWith(".java")) return "java";
        if (fileName.endsWith(".py")) return "python";
        if (fileName.endsWith(".js") || fileName.endsWith(".jsx")) return "javascript";
        if (fileName.endsWith(".ts") || fileName.endsWith(".tsx")) return "typescript";
        if (fileName.endsWith(".kt") || fileName.endsWith(".kts")) return "kotlin";
        if (fileName.endsWith(".scala")) return "scala";
        if (fileName.endsWith(".go")) return "go";
        if (fileName.endsWith(".rs")) return "rust";
        if (fileName.endsWith(".rb")) return "ruby";
        if (fileName.endsWith(".php")) return "php";
        if (fileName.endsWith(".cs")) return "csharp";
        if (fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx")) return "cpp";
        if (fileName.endsWith(".c")) return "c";
        if (fileName.endsWith(".h") || fileName.endsWith(".hpp")) return "header";
        
        return "unknown";
    }

    /**
     * Generate insights and recommendations based on analysis results
     */
    private List<StructureInsight> generateInsights(
            RepositoryStructure structure,
            List<ProjectType> projectTypes,
            DependencyGraph dependencyGraph,
            CodeOrganizationResult organizationResult,
            List<ArchitecturePattern> architecturePatterns) {
        
        List<StructureInsight> insights = new ArrayList<>();
        
        // Project structure insights
        insights.addAll(generateProjectStructureInsights(structure, projectTypes));
        
        // Dependency insights
        insights.addAll(generateDependencyInsights(dependencyGraph));
        
        // Code organization insights
        insights.addAll(generateOrganizationInsights(organizationResult));
        
        // Architecture pattern insights
        insights.addAll(generateArchitectureInsights(architecturePatterns));
        
        return insights;
    }

    private List<StructureInsight> generateProjectStructureInsights(RepositoryStructure structure, List<ProjectType> projectTypes) {
        List<StructureInsight> insights = new ArrayList<>();
        
        // Multi-language project detection
        if (projectTypes.size() > 1) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.MULTI_LANGUAGE)
                    .severity(InsightSeverity.INFO)
                    .title("Multi-language Project")
                    .description("This repository contains multiple programming languages: " + 
                               projectTypes.stream().map(ProjectType::getLanguage).collect(Collectors.joining(", ")))
                    .recommendation("Consider organizing different language components into separate directories")
                    .build());
        }
        
        // Large repository warning
        if (structure.getFiles().size() > 1000) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.LARGE_REPOSITORY)
                    .severity(InsightSeverity.WARNING)
                    .title("Large Repository")
                    .description("Repository contains over 1000 files")
                    .recommendation("Consider breaking down into smaller, more focused repositories or using a monorepo management tool")
                    .build());
        }
        
        return insights;
    }

    private List<StructureInsight> generateDependencyInsights(DependencyGraph dependencyGraph) {
        List<StructureInsight> insights = new ArrayList<>();
        
        // Circular dependency detection
        if (dependencyGraph.hasCircularDependencies()) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.CIRCULAR_DEPENDENCY)
                    .severity(InsightSeverity.ERROR)
                    .title("Circular Dependencies Detected")
                    .description("Found circular dependencies in the codebase")
                    .recommendation("Refactor to eliminate circular dependencies by introducing interfaces or reorganizing modules")
                    .build());
        }
        
        // High coupling warning
        if (dependencyGraph.getAverageCoupling() > 0.7) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.HIGH_COUPLING)
                    .severity(InsightSeverity.WARNING)
                    .title("High Coupling Detected")
                    .description("Components show high coupling with average coupling score: " + 
                               String.format("%.2f", dependencyGraph.getAverageCoupling()))
                    .recommendation("Consider reducing coupling by using dependency injection, interfaces, or event-driven patterns")
                    .build());
        }
        
        return insights;
    }

    private List<StructureInsight> generateOrganizationInsights(CodeOrganizationResult organizationResult) {
        List<StructureInsight> insights = new ArrayList<>();
        
        // Package/module organization
        if (organizationResult.getPackageDepth() > 5) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.DEEP_PACKAGE_STRUCTURE)
                    .severity(InsightSeverity.WARNING)
                    .title("Deep Package Structure")
                    .description("Package structure is very deep with " + organizationResult.getPackageDepth() + " levels")
                    .recommendation("Consider flattening the package structure for better maintainability")
                    .build());
        }
        
        // Large files warning
        if (organizationResult.hasLargeFiles()) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.LARGE_FILES)
                    .severity(InsightSeverity.WARNING)
                    .title("Large Files Detected")
                    .description("Some files are very large and may be difficult to maintain")
                    .recommendation("Consider breaking large files into smaller, more focused components")
                    .build());
        }
        
        return insights;
    }

    private List<StructureInsight> generateArchitectureInsights(List<ArchitecturePattern> architecturePatterns) {
        List<StructureInsight> insights = new ArrayList<>();
        
        // Architecture pattern detection
        if (!architecturePatterns.isEmpty()) {
            insights.add(StructureInsight.builder()
                    .type(InsightType.ARCHITECTURE_PATTERN)
                    .severity(InsightSeverity.INFO)
                    .title("Architecture Patterns Detected")
                    .description("Found architecture patterns: " + 
                               architecturePatterns.stream().map(ArchitecturePattern::getName).collect(Collectors.joining(", ")))
                    .recommendation("Ensure consistency in applying the detected patterns throughout the codebase")
                    .build());
        } else {
            insights.add(StructureInsight.builder()
                    .type(InsightType.NO_ARCHITECTURE_PATTERN)
                    .severity(InsightSeverity.WARNING)
                    .title("No Clear Architecture Pattern")
                    .description("No clear architecture pattern detected in the codebase")
                    .recommendation("Consider adopting a well-known architecture pattern (MVC, MVP, Clean Architecture, etc.)")
                    .build());
        }
        
        return insights;
    }
}