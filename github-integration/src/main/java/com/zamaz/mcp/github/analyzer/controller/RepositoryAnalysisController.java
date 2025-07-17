package com.zamaz.mcp.github.analyzer.controller;

import com.zamaz.mcp.github.analyzer.RepositoryStructureAnalyzer;
import com.zamaz.mcp.github.analyzer.model.RepositoryAnalysisResult;
import com.zamaz.mcp.github.analyzer.model.StructureInsight;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.InsightSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for repository structure analysis
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Repository Analysis", description = "Repository structure analysis endpoints")
public class RepositoryAnalysisController {
    
    private final RepositoryStructureAnalyzer repositoryAnalyzer;
    
    /**
     * Analyze repository structure
     */
    @PostMapping("/repository/{owner}/{repo}")
    @Operation(summary = "Analyze repository structure", 
               description = "Perform comprehensive analysis of repository structure including dependencies, patterns, and insights")
    @ApiResponse(responseCode = "200", description = "Analysis completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Repository not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<RepositoryAnalysisResult> analyzeRepository(
            @Parameter(description = "Repository owner", required = true)
            @PathVariable String owner,
            
            @Parameter(description = "Repository name", required = true)
            @PathVariable String repo,
            
            @Parameter(description = "Branch to analyze", required = false)
            @RequestParam(defaultValue = "main") String branch,
            
            @Parameter(description = "GitHub access token", required = true)
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Starting repository analysis for {}/{} on branch {}", owner, repo, branch);
        
        try {
            // Extract token from Authorization header
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            // Perform analysis
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            log.info("Repository analysis completed for {}/{} - Found {} insights", 
                    owner, repo, result.getInsights().size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error analyzing repository {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get analysis summary
     */
    @GetMapping("/repository/{owner}/{repo}/summary")
    @Operation(summary = "Get repository analysis summary", 
               description = "Get a summary of repository analysis results")
    public ResponseEntity<AnalysisSummary> getAnalysisSummary(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting analysis summary for {}/{}", owner, repo);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            return ResponseEntity.ok(result.getSummary());
            
        } catch (Exception e) {
            log.error("Error getting analysis summary for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get insights by severity
     */
    @GetMapping("/repository/{owner}/{repo}/insights")
    @Operation(summary = "Get repository insights", 
               description = "Get repository insights filtered by severity")
    public ResponseEntity<List<StructureInsight>> getInsights(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(required = false) InsightSeverity severity,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting insights for {}/{} with severity filter: {}", owner, repo, severity);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            List<StructureInsight> insights = severity != null ? 
                    result.getInsightsBySeverity(severity) : 
                    result.getInsights();
            
            return ResponseEntity.ok(insights);
            
        } catch (Exception e) {
            log.error("Error getting insights for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get dependency graph
     */
    @GetMapping("/repository/{owner}/{repo}/dependencies")
    @Operation(summary = "Get dependency graph", 
               description = "Get repository dependency graph analysis")
    public ResponseEntity<DependencyGraph> getDependencyGraph(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting dependency graph for {}/{}", owner, repo);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            return ResponseEntity.ok(result.getDependencyGraph());
            
        } catch (Exception e) {
            log.error("Error getting dependency graph for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get architecture patterns
     */
    @GetMapping("/repository/{owner}/{repo}/patterns")
    @Operation(summary = "Get architecture patterns", 
               description = "Get detected architecture patterns in repository")
    public ResponseEntity<List<ArchitecturePattern>> getArchitecturePatterns(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting architecture patterns for {}/{}", owner, repo);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            return ResponseEntity.ok(result.getArchitecturePatterns());
            
        } catch (Exception e) {
            log.error("Error getting architecture patterns for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get structure visualization
     */
    @GetMapping("/repository/{owner}/{repo}/visualization")
    @Operation(summary = "Get structure visualization", 
               description = "Get repository structure visualization data")
    public ResponseEntity<StructureVisualization> getVisualization(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting structure visualization for {}/{}", owner, repo);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            return ResponseEntity.ok(result.getVisualization());
            
        } catch (Exception e) {
            log.error("Error getting structure visualization for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get project types
     */
    @GetMapping("/repository/{owner}/{repo}/project-types")
    @Operation(summary = "Get project types", 
               description = "Get detected project types in repository")
    public ResponseEntity<List<ProjectType>> getProjectTypes(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting project types for {}/{}", owner, repo);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            return ResponseEntity.ok(result.getProjectTypes());
            
        } catch (Exception e) {
            log.error("Error getting project types for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get code organization
     */
    @GetMapping("/repository/{owner}/{repo}/organization")
    @Operation(summary = "Get code organization", 
               description = "Get repository code organization analysis")
    public ResponseEntity<CodeOrganizationResult> getCodeOrganization(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "main") String branch,
            @RequestHeader("Authorization") String authToken) {
        
        log.info("Getting code organization for {}/{}", owner, repo);
        
        try {
            String accessToken = authToken.startsWith("Bearer ") ? 
                    authToken.substring(7) : authToken;
            
            RepositoryAnalysisResult result = repositoryAnalyzer.analyzeRepository(
                    accessToken, owner, repo, branch);
            
            return ResponseEntity.ok(result.getCodeOrganization());
            
        } catch (Exception e) {
            log.error("Error getting code organization for {}/{}: {}", owner, repo, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get health check for analysis service
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", 
               description = "Check if analysis service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = Map.of(
                "status", "UP",
                "service", "Repository Structure Analyzer",
                "timestamp", String.valueOf(System.currentTimeMillis())
        );
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get supported project types
     */
    @GetMapping("/project-types")
    @Operation(summary = "Get supported project types", 
               description = "Get list of supported project types for analysis")
    public ResponseEntity<List<String>> getSupportedProjectTypes() {
        List<String> projectTypes = List.of(
                "MAVEN", "GRADLE", "NODE_JS", "PYTHON", "SPRING_BOOT",
                "REACT", "ANGULAR", "VUE", "DOCKER", "KUBERNETES",
                "MICROSERVICE", "MONOLITH", "LIBRARY"
        );
        
        return ResponseEntity.ok(projectTypes);
    }
    
    /**
     * Get supported architecture patterns
     */
    @GetMapping("/architecture-patterns")
    @Operation(summary = "Get supported architecture patterns", 
               description = "Get list of supported architecture patterns for detection")
    public ResponseEntity<List<String>> getSupportedArchitecturePatterns() {
        List<String> patterns = List.of(
                "MVC", "MVP", "MVVM", "CLEAN_ARCHITECTURE", "HEXAGONAL",
                "LAYERED", "MICROSERVICES", "MONOLITH", "EVENT_DRIVEN",
                "REPOSITORY_PATTERN", "FACTORY_PATTERN", "SINGLETON_PATTERN",
                "OBSERVER_PATTERN", "STRATEGY_PATTERN"
        );
        
        return ResponseEntity.ok(patterns);
    }
}