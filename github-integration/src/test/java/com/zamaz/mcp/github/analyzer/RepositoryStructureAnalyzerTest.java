package com.zamaz.mcp.github.analyzer;

import com.zamaz.mcp.github.analyzer.dependency.DependencyAnalyzer;
import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.pattern.ArchitecturePatternDetector;
import com.zamaz.mcp.github.analyzer.project.ProjectTypeDetector;
import com.zamaz.mcp.github.analyzer.structure.CodeOrganizationAnalyzer;
import com.zamaz.mcp.github.analyzer.visualization.StructureVisualizer;
import com.zamaz.mcp.github.service.GitHubApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Repository Structure Analyzer
 */
@ExtendWith(MockitoExtension.class)
class RepositoryStructureAnalyzerTest {
    
    @Mock
    private GitHubApiClient mockApiClient;
    
    @Mock
    private ProjectTypeDetector mockProjectTypeDetector;
    
    @Mock
    private DependencyAnalyzer mockDependencyAnalyzer;
    
    @Mock
    private CodeOrganizationAnalyzer mockCodeOrganizationAnalyzer;
    
    @Mock
    private ArchitecturePatternDetector mockArchitecturePatternDetector;
    
    @Mock
    private StructureVisualizer mockStructureVisualizer;
    
    private RepositoryStructureAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new RepositoryStructureAnalyzer(
                mockApiClient,
                mockProjectTypeDetector,
                mockDependencyAnalyzer,
                mockCodeOrganizationAnalyzer,
                mockArchitecturePatternDetector,
                mockStructureVisualizer
        );
    }
    
    @Test
    void testAnalyzeRepository_SpringBootProject() {
        // Given
        String owner = "test-owner";
        String repo = "test-repo";
        String branch = "main";
        String accessToken = "test-token";
        
        RepositoryStructure mockStructure = createMockSpringBootStructure();
        List<ProjectType> mockProjectTypes = createMockSpringBootProjectTypes();
        DependencyGraph mockDependencyGraph = createMockDependencyGraph();
        Map<String, ASTAnalysisResult> mockAstResults = createMockASTResults();
        CodeOrganizationResult mockOrganizationResult = createMockOrganizationResult();
        List<ArchitecturePattern> mockPatterns = createMockArchitecturePatterns();
        StructureVisualization mockVisualization = createMockVisualization();
        
        // Mock dependencies
        when(mockProjectTypeDetector.detectProjectTypes(any())).thenReturn(mockProjectTypes);
        when(mockDependencyAnalyzer.analyzeDependencies(any(), any())).thenReturn(mockDependencyGraph);
        when(mockCodeOrganizationAnalyzer.analyzeOrganization(any(), any())).thenReturn(mockOrganizationResult);
        when(mockArchitecturePatternDetector.detectPatterns(any(), any(), any())).thenReturn(mockPatterns);
        when(mockStructureVisualizer.createVisualization(any(), any(), any())).thenReturn(mockVisualization);
        
        // When
        RepositoryAnalysisResult result = analyzer.analyzeRepository(accessToken, owner, repo, branch);
        
        // Then
        assertNotNull(result);
        assertEquals(mockProjectTypes, result.getProjectTypes());
        assertEquals(mockDependencyGraph, result.getDependencyGraph());
        assertEquals(mockOrganizationResult, result.getCodeOrganization());
        assertEquals(mockPatterns, result.getArchitecturePatterns());
        assertEquals(mockVisualization, result.getVisualization());
        
        // Verify method calls
        verify(mockProjectTypeDetector).detectProjectTypes(any());
        verify(mockDependencyAnalyzer).analyzeDependencies(any(), any());
        verify(mockCodeOrganizationAnalyzer).analyzeOrganization(any(), any());
        verify(mockArchitecturePatternDetector).detectPatterns(any(), any(), any());
        verify(mockStructureVisualizer).createVisualization(any(), any(), any());
    }
    
    @Test
    void testAnalyzeRepository_NodeJsProject() {
        // Given
        String owner = "test-owner";
        String repo = "node-repo";
        String branch = "main";
        String accessToken = "test-token";
        
        RepositoryStructure mockStructure = createMockNodeJsStructure();
        List<ProjectType> mockProjectTypes = createMockNodeJsProjectTypes();
        DependencyGraph mockDependencyGraph = createMockNodeJsDependencyGraph();
        Map<String, ASTAnalysisResult> mockAstResults = createMockNodeJsASTResults();
        CodeOrganizationResult mockOrganizationResult = createMockNodeJsOrganizationResult();
        List<ArchitecturePattern> mockPatterns = createMockNodeJsArchitecturePatterns();
        StructureVisualization mockVisualization = createMockVisualization();
        
        // Mock dependencies
        when(mockProjectTypeDetector.detectProjectTypes(any())).thenReturn(mockProjectTypes);
        when(mockDependencyAnalyzer.analyzeDependencies(any(), any())).thenReturn(mockDependencyGraph);
        when(mockCodeOrganizationAnalyzer.analyzeOrganization(any(), any())).thenReturn(mockOrganizationResult);
        when(mockArchitecturePatternDetector.detectPatterns(any(), any(), any())).thenReturn(mockPatterns);
        when(mockStructureVisualizer.createVisualization(any(), any(), any())).thenReturn(mockVisualization);
        
        // When
        RepositoryAnalysisResult result = analyzer.analyzeRepository(accessToken, owner, repo, branch);
        
        // Then
        assertNotNull(result);
        assertEquals("javascript", result.getPrimaryLanguage());
        assertEquals(mockProjectTypes, result.getProjectTypes());
        assertFalse(result.hasCriticalIssues());
    }
    
    @Test
    void testAnalyzeRepository_PythonProject() {
        // Given
        String owner = "test-owner";
        String repo = "python-repo";
        String branch = "main";
        String accessToken = "test-token";
        
        RepositoryStructure mockStructure = createMockPythonStructure();
        List<ProjectType> mockProjectTypes = createMockPythonProjectTypes();
        DependencyGraph mockDependencyGraph = createMockPythonDependencyGraph();
        Map<String, ASTAnalysisResult> mockAstResults = createMockPythonASTResults();
        CodeOrganizationResult mockOrganizationResult = createMockPythonOrganizationResult();
        List<ArchitecturePattern> mockPatterns = createMockPythonArchitecturePatterns();
        StructureVisualization mockVisualization = createMockVisualization();
        
        // Mock dependencies
        when(mockProjectTypeDetector.detectProjectTypes(any())).thenReturn(mockProjectTypes);
        when(mockDependencyAnalyzer.analyzeDependencies(any(), any())).thenReturn(mockDependencyGraph);
        when(mockCodeOrganizationAnalyzer.analyzeOrganization(any(), any())).thenReturn(mockOrganizationResult);
        when(mockArchitecturePatternDetector.detectPatterns(any(), any(), any())).thenReturn(mockPatterns);
        when(mockStructureVisualizer.createVisualization(any(), any(), any())).thenReturn(mockVisualization);
        
        // When
        RepositoryAnalysisResult result = analyzer.analyzeRepository(accessToken, owner, repo, branch);
        
        // Then
        assertNotNull(result);
        assertEquals("python", result.getPrimaryLanguage());
        assertEquals(mockProjectTypes, result.getProjectTypes());
    }
    
    @Test
    void testAnalyzeRepository_WithCircularDependencies() {
        // Given
        String owner = "test-owner";
        String repo = "circular-repo";
        String branch = "main";
        String accessToken = "test-token";
        
        RepositoryStructure mockStructure = createMockStructureWithCircularDeps();
        List<ProjectType> mockProjectTypes = createMockSpringBootProjectTypes();
        DependencyGraph mockDependencyGraph = createMockDependencyGraphWithCircularDeps();
        Map<String, ASTAnalysisResult> mockAstResults = createMockASTResults();
        CodeOrganizationResult mockOrganizationResult = createMockOrganizationResult();
        List<ArchitecturePattern> mockPatterns = createMockArchitecturePatterns();
        StructureVisualization mockVisualization = createMockVisualization();
        
        // Mock dependencies
        when(mockProjectTypeDetector.detectProjectTypes(any())).thenReturn(mockProjectTypes);
        when(mockDependencyAnalyzer.analyzeDependencies(any(), any())).thenReturn(mockDependencyGraph);
        when(mockCodeOrganizationAnalyzer.analyzeOrganization(any(), any())).thenReturn(mockOrganizationResult);
        when(mockArchitecturePatternDetector.detectPatterns(any(), any(), any())).thenReturn(mockPatterns);
        when(mockStructureVisualizer.createVisualization(any(), any(), any())).thenReturn(mockVisualization);
        
        // When
        RepositoryAnalysisResult result = analyzer.analyzeRepository(accessToken, owner, repo, branch);
        
        // Then
        assertNotNull(result);
        assertTrue(result.hasCriticalIssues());
        assertTrue(result.getDependencyGraph().isHasCircularDependencies());
        
        // Check for circular dependency insights
        boolean hasCircularDependencyInsight = result.getInsights().stream()
                .anyMatch(insight -> insight.getType() == ModelEnums.InsightType.CIRCULAR_DEPENDENCY);
        assertTrue(hasCircularDependencyInsight);
    }
    
    @Test
    void testGetComplexityScore() {
        // Given
        RepositoryAnalysisResult result = createMockAnalysisResult();
        
        // When
        double complexityScore = result.getComplexityScore();
        
        // Then
        assertTrue(complexityScore >= 0.0);
        assertTrue(complexityScore <= 100.0);
    }
    
    @Test
    void testGetMaintainabilityScore() {
        // Given
        RepositoryAnalysisResult result = createMockAnalysisResult();
        
        // When
        double maintainabilityScore = result.getMaintainabilityScore();
        
        // Then
        assertTrue(maintainabilityScore >= 0.0);
        assertTrue(maintainabilityScore <= 100.0);
    }
    
    @Test
    void testGetAnalysisSummary() {
        // Given
        RepositoryAnalysisResult result = createMockAnalysisResult();
        
        // When
        AnalysisSummary summary = result.getSummary();
        
        // Then
        assertNotNull(summary);
        assertTrue(summary.getTotalFiles() >= 0);
        assertTrue(summary.getTotalDirectories() >= 0);
        assertTrue(summary.getLanguageCount() >= 0);
        assertTrue(summary.getProjectTypeCount() >= 0);
        assertTrue(summary.getDependencyCount() >= 0);
        assertTrue(summary.getInsightCount() >= 0);
    }
    
    // Helper methods to create mock objects
    
    private RepositoryStructure createMockSpringBootStructure() {
        return RepositoryStructure.builder()
                .owner("test-owner")
                .repository("test-repo")
                .branch("main")
                .rootPath("/")
                .files(createMockSpringBootFiles())
                .directories(createMockSpringBootDirectories())
                .build();
    }
    
    private List<FileInfo> createMockSpringBootFiles() {
        List<FileInfo> files = new ArrayList<>();
        
        files.add(FileInfo.builder()
                .name("pom.xml")
                .path("/pom.xml")
                .extension("xml")
                .size(2000)
                .lineCount(50)
                .language("xml")
                .configFile(true)
                .build());
        
        files.add(FileInfo.builder()
                .name("Application.java")
                .path("/src/main/java/com/example/Application.java")
                .extension("java")
                .size(500)
                .lineCount(20)
                .language("java")
                .content("@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}")
                .build());
        
        files.add(FileInfo.builder()
                .name("UserController.java")
                .path("/src/main/java/com/example/controller/UserController.java")
                .extension("java")
                .size(1500)
                .lineCount(60)
                .language("java")
                .content("@RestController\npublic class UserController {\n    @GetMapping(\"/users\")\n    public List<User> getUsers() {\n        return userService.findAll();\n    }\n}")
                .build());
        
        return files;
    }
    
    private List<DirectoryInfo> createMockSpringBootDirectories() {
        List<DirectoryInfo> directories = new ArrayList<>();
        
        directories.add(DirectoryInfo.builder()
                .name("src")
                .path("/src")
                .fileCount(0)
                .subdirectoryCount(2)
                .depth(1)
                .sourceDirectory(true)
                .build());
        
        directories.add(DirectoryInfo.builder()
                .name("controller")
                .path("/src/main/java/com/example/controller")
                .fileCount(2)
                .subdirectoryCount(0)
                .depth(5)
                .sourceDirectory(true)
                .build());
        
        return directories;
    }
    
    private RepositoryStructure createMockNodeJsStructure() {
        return RepositoryStructure.builder()
                .owner("test-owner")
                .repository("node-repo")
                .branch("main")
                .rootPath("/")
                .files(createMockNodeJsFiles())
                .directories(createMockNodeJsDirectories())
                .build();
    }
    
    private List<FileInfo> createMockNodeJsFiles() {
        List<FileInfo> files = new ArrayList<>();
        
        files.add(FileInfo.builder()
                .name("package.json")
                .path("/package.json")
                .extension("json")
                .size(1000)
                .lineCount(30)
                .language("json")
                .configFile(true)
                .content("{\n  \"name\": \"test-app\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n    \"express\": \"^4.18.0\"\n  }\n}")
                .build());
        
        files.add(FileInfo.builder()
                .name("index.js")
                .path("/src/index.js")
                .extension("js")
                .size(800)
                .lineCount(40)
                .language("javascript")
                .content("const express = require('express');\nconst app = express();\n\napp.get('/', (req, res) => {\n  res.send('Hello World!');\n});\n\napp.listen(3000);")
                .build());
        
        return files;
    }
    
    private List<DirectoryInfo> createMockNodeJsDirectories() {
        return List.of(
                DirectoryInfo.builder()
                        .name("src")
                        .path("/src")
                        .fileCount(3)
                        .subdirectoryCount(0)
                        .depth(1)
                        .sourceDirectory(true)
                        .build()
        );
    }
    
    private RepositoryStructure createMockPythonStructure() {
        return RepositoryStructure.builder()
                .owner("test-owner")
                .repository("python-repo")
                .branch("main")
                .rootPath("/")
                .files(createMockPythonFiles())
                .directories(createMockPythonDirectories())
                .build();
    }
    
    private List<FileInfo> createMockPythonFiles() {
        List<FileInfo> files = new ArrayList<>();
        
        files.add(FileInfo.builder()
                .name("requirements.txt")
                .path("/requirements.txt")
                .extension("txt")
                .size(500)
                .lineCount(15)
                .language("text")
                .configFile(true)
                .content("django==4.1.0\nrequests==2.28.1\npytest==7.1.2")
                .build());
        
        files.add(FileInfo.builder()
                .name("main.py")
                .path("/src/main.py")
                .extension("py")
                .size(1200)
                .lineCount(50)
                .language("python")
                .content("import django\nfrom django.http import HttpResponse\n\ndef hello(request):\n    return HttpResponse('Hello, World!')")
                .build());
        
        return files;
    }
    
    private List<DirectoryInfo> createMockPythonDirectories() {
        return List.of(
                DirectoryInfo.builder()
                        .name("src")
                        .path("/src")
                        .fileCount(5)
                        .subdirectoryCount(0)
                        .depth(1)
                        .sourceDirectory(true)
                        .build()
        );
    }
    
    private RepositoryStructure createMockStructureWithCircularDeps() {
        return RepositoryStructure.builder()
                .owner("test-owner")
                .repository("circular-repo")
                .branch("main")
                .rootPath("/")
                .files(createMockSpringBootFiles())
                .directories(createMockSpringBootDirectories())
                .build();
    }
    
    private List<ProjectType> createMockSpringBootProjectTypes() {
        return List.of(
                ProjectType.builder()
                        .type(ProjectTypeEnum.SPRING_BOOT)
                        .language("java")
                        .name("Spring Boot Application")
                        .version("2.7.0")
                        .rootDirectory("/")
                        .configFiles(Arrays.asList("pom.xml", "application.yml"))
                        .confidence(0.9)
                        .build(),
                ProjectType.builder()
                        .type(ProjectTypeEnum.MAVEN)
                        .language("java")
                        .name("Maven Project")
                        .version("3.8.1")
                        .rootDirectory("/")
                        .configFiles(Arrays.asList("pom.xml"))
                        .confidence(0.8)
                        .build()
        );
    }
    
    private List<ProjectType> createMockNodeJsProjectTypes() {
        return List.of(
                ProjectType.builder()
                        .type(ProjectTypeEnum.NODE_JS)
                        .language("javascript")
                        .name("Node.js Application")
                        .version("16.14.0")
                        .rootDirectory("/")
                        .configFiles(Arrays.asList("package.json"))
                        .confidence(0.9)
                        .build()
        );
    }
    
    private List<ProjectType> createMockPythonProjectTypes() {
        return List.of(
                ProjectType.builder()
                        .type(ProjectTypeEnum.PYTHON)
                        .language("python")
                        .name("Python Application")
                        .version("3.9.0")
                        .rootDirectory("/")
                        .configFiles(Arrays.asList("requirements.txt"))
                        .confidence(0.8)
                        .build()
        );
    }
    
    private DependencyGraph createMockDependencyGraph() {
        return DependencyGraph.builder()
                .dependencies(List.of(
                        Dependency.builder()
                                .groupId("org.springframework.boot")
                                .artifactId("spring-boot-starter-web")
                                .version("2.7.0")
                                .type("jar")
                                .scope("compile")
                                .build()
                ))
                .nodes(List.of(
                        DependencyNode.builder()
                                .id("org.springframework.boot:spring-boot-starter-web")
                                .groupId("org.springframework.boot")
                                .artifactId("spring-boot-starter-web")
                                .version("2.7.0")
                                .type("jar")
                                .scope("compile")
                                .dependencies(new ArrayList<>())
                                .dependents(new ArrayList<>())
                                .build()
                ))
                .edges(new ArrayList<>())
                .clusters(new ArrayList<>())
                .circularDependencies(new ArrayList<>())
                .hasCircularDependencies(false)
                .totalNodes(1)
                .totalEdges(0)
                .averageCoupling(0.0)
                .maxDependencies(0)
                .maxDependents(0)
                .density(0.0)
                .maxDepth(1)
                .averageDepth(1.0)
                .build();
    }
    
    private DependencyGraph createMockNodeJsDependencyGraph() {
        return DependencyGraph.builder()
                .dependencies(List.of(
                        Dependency.builder()
                                .groupId("npm")
                                .artifactId("express")
                                .version("4.18.0")
                                .type("npm")
                                .scope("dependencies")
                                .build()
                ))
                .nodes(List.of(
                        DependencyNode.builder()
                                .id("npm:express")
                                .groupId("npm")
                                .artifactId("express")
                                .version("4.18.0")
                                .type("npm")
                                .scope("dependencies")
                                .dependencies(new ArrayList<>())
                                .dependents(new ArrayList<>())
                                .build()
                ))
                .edges(new ArrayList<>())
                .clusters(new ArrayList<>())
                .circularDependencies(new ArrayList<>())
                .hasCircularDependencies(false)
                .totalNodes(1)
                .totalEdges(0)
                .averageCoupling(0.0)
                .maxDependencies(0)
                .maxDependents(0)
                .density(0.0)
                .maxDepth(1)
                .averageDepth(1.0)
                .build();
    }
    
    private DependencyGraph createMockPythonDependencyGraph() {
        return DependencyGraph.builder()
                .dependencies(List.of(
                        Dependency.builder()
                                .groupId("pypi")
                                .artifactId("django")
                                .version("4.1.0")
                                .type("wheel")
                                .scope("install")
                                .build()
                ))
                .nodes(List.of(
                        DependencyNode.builder()
                                .id("pypi:django")
                                .groupId("pypi")
                                .artifactId("django")
                                .version("4.1.0")
                                .type("wheel")
                                .scope("install")
                                .dependencies(new ArrayList<>())
                                .dependents(new ArrayList<>())
                                .build()
                ))
                .edges(new ArrayList<>())
                .clusters(new ArrayList<>())
                .circularDependencies(new ArrayList<>())
                .hasCircularDependencies(false)
                .totalNodes(1)
                .totalEdges(0)
                .averageCoupling(0.0)
                .maxDependencies(0)
                .maxDependents(0)
                .density(0.0)
                .maxDepth(1)
                .averageDepth(1.0)
                .build();
    }
    
    private DependencyGraph createMockDependencyGraphWithCircularDeps() {
        return DependencyGraph.builder()
                .dependencies(List.of())
                .nodes(List.of())
                .edges(List.of())
                .clusters(new ArrayList<>())
                .circularDependencies(List.of(
                        Arrays.asList("A", "B", "C", "A")
                ))
                .hasCircularDependencies(true)
                .totalNodes(3)
                .totalEdges(3)
                .averageCoupling(2.0)
                .maxDependencies(1)
                .maxDependents(1)
                .density(1.0)
                .maxDepth(3)
                .averageDepth(3.0)
                .build();
    }
    
    private Map<String, ASTAnalysisResult> createMockASTResults() {
        Map<String, ASTAnalysisResult> results = new HashMap<>();
        
        results.put("java", ASTAnalysisResult.builder()
                .language("java")
                .fileCount(3)
                .nodes(List.of(
                        ASTNode.builder()
                                .id("Application")
                                .name("Application")
                                .type(ModelEnums.ASTNodeType.CLASS)
                                .fileName("Application.java")
                                .startLine(1)
                                .endLine(10)
                                .properties(new HashMap<>())
                                .complexity(Map.of("cyclomaticComplexity", 2.0))
                                .build()
                ))
                .relationships(new ArrayList<>())
                .metrics(Map.of("totalClasses", 1.0, "totalMethods", 1.0))
                .complexity(Map.of("averageComplexity", 2.0))
                .issues(new ArrayList<>())
                .build());
        
        return results;
    }
    
    private Map<String, ASTAnalysisResult> createMockNodeJsASTResults() {
        Map<String, ASTAnalysisResult> results = new HashMap<>();
        
        results.put("javascript", ASTAnalysisResult.builder()
                .language("javascript")
                .fileCount(2)
                .nodes(List.of(
                        ASTNode.builder()
                                .id("index.js:main")
                                .name("main")
                                .type(ModelEnums.ASTNodeType.METHOD)
                                .fileName("index.js")
                                .startLine(1)
                                .endLine(10)
                                .properties(new HashMap<>())
                                .complexity(Map.of("cyclomaticComplexity", 1.0))
                                .build()
                ))
                .relationships(new ArrayList<>())
                .metrics(Map.of("totalFunctions", 1.0, "totalVariables", 2.0))
                .complexity(Map.of("averageComplexity", 1.0))
                .issues(new ArrayList<>())
                .build());
        
        return results;
    }
    
    private Map<String, ASTAnalysisResult> createMockPythonASTResults() {
        Map<String, ASTAnalysisResult> results = new HashMap<>();
        
        results.put("python", ASTAnalysisResult.builder()
                .language("python")
                .fileCount(2)
                .nodes(List.of(
                        ASTNode.builder()
                                .id("main.py:hello")
                                .name("hello")
                                .type(ModelEnums.ASTNodeType.METHOD)
                                .fileName("main.py")
                                .startLine(4)
                                .endLine(5)
                                .properties(new HashMap<>())
                                .complexity(Map.of("cyclomaticComplexity", 1.0))
                                .build()
                ))
                .relationships(new ArrayList<>())
                .metrics(Map.of("totalFunctions", 1.0, "totalVariables", 0.0))
                .complexity(Map.of("averageComplexity", 1.0))
                .issues(new ArrayList<>())
                .build());
        
        return results;
    }
    
    private CodeOrganizationResult createMockOrganizationResult() {
        return CodeOrganizationResult.builder()
                .packageStructure(PackageStructure.builder()
                        .packages(new HashMap<>())
                        .hierarchy(new HashMap<>())
                        .maxDepth(5)
                        .averageFilesPerPackage(3)
                        .totalPackages(5)
                        .build())
                .moduleStructure(ModuleStructure.builder()
                        .modules(new ArrayList<>())
                        .dependencies(new HashMap<>())
                        .totalModules(1)
                        .build())
                .layerStructure(LayerStructure.builder()
                        .layers(new HashMap<>())
                        .dependencies(new HashMap<>())
                        .totalLayers(3)
                        .build())
                .featureStructure(FeatureStructure.builder()
                        .features(new ArrayList<>())
                        .totalFeatures(2)
                        .build())
                .metrics(OrganizationMetrics.builder()
                        .packageCohesion(0.7)
                        .moduleCohesion(0.8)
                        .packageCoupling(0.3)
                        .moduleCoupling(0.4)
                        .organizationComplexity(0.5)
                        .build())
                .insights(new ArrayList<>())
                .packageDepth(5)
                .hasLargeFiles(false)
                .build();
    }
    
    private CodeOrganizationResult createMockNodeJsOrganizationResult() {
        return CodeOrganizationResult.builder()
                .packageStructure(PackageStructure.builder()
                        .packages(new HashMap<>())
                        .hierarchy(new HashMap<>())
                        .maxDepth(2)
                        .averageFilesPerPackage(2)
                        .totalPackages(2)
                        .build())
                .moduleStructure(ModuleStructure.builder()
                        .modules(new ArrayList<>())
                        .dependencies(new HashMap<>())
                        .totalModules(1)
                        .build())
                .layerStructure(LayerStructure.builder()
                        .layers(new HashMap<>())
                        .dependencies(new HashMap<>())
                        .totalLayers(1)
                        .build())
                .featureStructure(FeatureStructure.builder()
                        .features(new ArrayList<>())
                        .totalFeatures(1)
                        .build())
                .metrics(OrganizationMetrics.builder()
                        .packageCohesion(0.8)
                        .moduleCohesion(0.9)
                        .packageCoupling(0.2)
                        .moduleCoupling(0.1)
                        .organizationComplexity(0.3)
                        .build())
                .insights(new ArrayList<>())
                .packageDepth(2)
                .hasLargeFiles(false)
                .build();
    }
    
    private CodeOrganizationResult createMockPythonOrganizationResult() {
        return CodeOrganizationResult.builder()
                .packageStructure(PackageStructure.builder()
                        .packages(new HashMap<>())
                        .hierarchy(new HashMap<>())
                        .maxDepth(2)
                        .averageFilesPerPackage(3)
                        .totalPackages(2)
                        .build())
                .moduleStructure(ModuleStructure.builder()
                        .modules(new ArrayList<>())
                        .dependencies(new HashMap<>())
                        .totalModules(1)
                        .build())
                .layerStructure(LayerStructure.builder()
                        .layers(new HashMap<>())
                        .dependencies(new HashMap<>())
                        .totalLayers(1)
                        .build())
                .featureStructure(FeatureStructure.builder()
                        .features(new ArrayList<>())
                        .totalFeatures(1)
                        .build())
                .metrics(OrganizationMetrics.builder()
                        .packageCohesion(0.6)
                        .moduleCohesion(0.7)
                        .packageCoupling(0.4)
                        .moduleCoupling(0.3)
                        .organizationComplexity(0.4)
                        .build())
                .insights(new ArrayList<>())
                .packageDepth(2)
                .hasLargeFiles(false)
                .build();
    }
    
    private List<ArchitecturePattern> createMockArchitecturePatterns() {
        return List.of(
                ArchitecturePattern.builder()
                        .type(ModelEnums.ArchitecturePatternType.MVC)
                        .name("Model-View-Controller")
                        .confidence(0.8)
                        .evidence(Arrays.asList("Found controller directory", "Found model classes"))
                        .description("Web application following MVC pattern")
                        .benefits(Arrays.asList("Separation of concerns", "Testability"))
                        .drawbacks(Arrays.asList("Can become complex"))
                        .build(),
                ArchitecturePattern.builder()
                        .type(ModelEnums.ArchitecturePatternType.LAYERED)
                        .name("Layered Architecture")
                        .confidence(0.7)
                        .evidence(Arrays.asList("Found service layer", "Found repository layer"))
                        .description("Application organized in layers")
                        .benefits(Arrays.asList("Clear separation", "Maintainability"))
                        .drawbacks(Arrays.asList("Performance overhead"))
                        .build()
        );
    }
    
    private List<ArchitecturePattern> createMockNodeJsArchitecturePatterns() {
        return List.of(
                ArchitecturePattern.builder()
                        .type(ModelEnums.ArchitecturePatternType.MONOLITH)
                        .name("Monolithic Architecture")
                        .confidence(0.9)
                        .evidence(Arrays.asList("Single application file", "No module separation"))
                        .description("Single application deployed as one unit")
                        .benefits(Arrays.asList("Simplicity", "Easy deployment"))
                        .drawbacks(Arrays.asList("Limited scalability"))
                        .build()
        );
    }
    
    private List<ArchitecturePattern> createMockPythonArchitecturePatterns() {
        return List.of(
                ArchitecturePattern.builder()
                        .type(ModelEnums.ArchitecturePatternType.MONOLITH)
                        .name("Monolithic Architecture")
                        .confidence(0.8)
                        .evidence(Arrays.asList("Single main module", "Django application"))
                        .description("Django application as monolith")
                        .benefits(Arrays.asList("Simplicity", "Django framework"))
                        .drawbacks(Arrays.asList("Scaling challenges"))
                        .build()
        );
    }
    
    private StructureVisualization createMockVisualization() {
        return StructureVisualization.builder()
                .treeView(VisualizationNode.builder()
                        .id("root")
                        .name("Repository")
                        .type(ModelEnums.VisualizationType.TREE)
                        .nodeType("repository")
                        .children(new ArrayList<>())
                        .properties(new HashMap<>())
                        .build())
                .graphView(VisualizationNode.builder()
                        .id("dependency-graph")
                        .name("Dependency Graph")
                        .type(ModelEnums.VisualizationType.GRAPH)
                        .nodeType("graph")
                        .children(new ArrayList<>())
                        .edges(new ArrayList<>())
                        .properties(new HashMap<>())
                        .build())
                .sunburstView(VisualizationNode.builder()
                        .id("sunburst-root")
                        .name("Structure Sunburst")
                        .type(ModelEnums.VisualizationType.SUNBURST)
                        .nodeType("repository")
                        .children(new ArrayList<>())
                        .properties(new HashMap<>())
                        .build())
                .treemapView(VisualizationNode.builder()
                        .id("treemap-root")
                        .name("Code Size Distribution")
                        .type(ModelEnums.VisualizationType.TREEMAP)
                        .nodeType("repository")
                        .children(new ArrayList<>())
                        .properties(new HashMap<>())
                        .build())
                .networkView(VisualizationNode.builder()
                        .id("network-root")
                        .name("Component Network")
                        .type(ModelEnums.VisualizationType.NETWORK)
                        .nodeType("network")
                        .children(new ArrayList<>())
                        .edges(new ArrayList<>())
                        .properties(new HashMap<>())
                        .build())
                .metadata(VisualizationMetadata.builder()
                        .totalNodes(10)
                        .totalEdges(5)
                        .maxDepth(3)
                        .complexity(0.5)
                        .recommendations(Arrays.asList("Use filtered views for large repositories"))
                        .build())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    private RepositoryAnalysisResult createMockAnalysisResult() {
        return RepositoryAnalysisResult.builder()
                .repositoryStructure(createMockSpringBootStructure())
                .projectTypes(createMockSpringBootProjectTypes())
                .dependencyGraph(createMockDependencyGraph())
                .astResults(createMockASTResults())
                .codeOrganization(createMockOrganizationResult())
                .architecturePatterns(createMockArchitecturePatterns())
                .insights(List.of(
                        StructureInsight.builder()
                                .type(ModelEnums.InsightType.MULTI_LANGUAGE)
                                .severity(ModelEnums.InsightSeverity.INFO)
                                .title("Multi-language Project")
                                .description("Repository contains multiple languages")
                                .recommendation("Consider organizing by language")
                                .confidence(0.9)
                                .impact(0.5)
                                .effort(0.3)
                                .build()
                ))
                .visualization(createMockVisualization())
                .analysisTimestamp(System.currentTimeMillis())
                .build();
    }
}