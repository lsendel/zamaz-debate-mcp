package com.zamaz.mcp.github.analyzer.project;

import com.zamaz.mcp.github.analyzer.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects project types from repository structure
 */
@Component
@Slf4j
public class ProjectTypeDetector {
    
    private final Map<ProjectTypeEnum, ProjectTypeDetectionRule> detectionRules;
    
    public ProjectTypeDetector() {
        this.detectionRules = initializeDetectionRules();
    }
    
    /**
     * Detect all project types in the repository
     */
    public List<ProjectType> detectProjectTypes(RepositoryStructure structure) {
        log.info("Detecting project types for repository {}/{}", structure.getOwner(), structure.getRepository());
        
        List<ProjectType> detectedTypes = new ArrayList<>();
        
        // Check each detection rule
        for (Map.Entry<ProjectTypeEnum, ProjectTypeDetectionRule> entry : detectionRules.entrySet()) {
            ProjectTypeEnum type = entry.getKey();
            ProjectTypeDetectionRule rule = entry.getValue();
            
            ProjectType detectedType = applyDetectionRule(structure, type, rule);
            if (detectedType != null) {
                detectedTypes.add(detectedType);
                log.info("Detected project type: {} with confidence: {}", 
                        type.getDisplayName(), detectedType.getConfidence());
            }
        }
        
        // Sort by confidence (highest first)
        detectedTypes.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        // Post-process to enhance detection results
        enhanceDetectionResults(structure, detectedTypes);
        
        log.info("Detected {} project types", detectedTypes.size());
        return detectedTypes;
    }
    
    /**
     * Apply a specific detection rule
     */
    private ProjectType applyDetectionRule(RepositoryStructure structure, ProjectTypeEnum type, ProjectTypeDetectionRule rule) {
        double confidence = 0.0;
        List<String> foundConfigFiles = new ArrayList<>();
        
        // Check for required files
        for (String requiredFile : rule.getRequiredFiles()) {
            if (structure.hasFile(requiredFile)) {
                confidence += 0.4; // Required files give high confidence
                foundConfigFiles.add(requiredFile);
            } else {
                // If required file is missing, this project type is unlikely
                return null;
            }
        }
        
        // Check for optional files
        for (String optionalFile : rule.getOptionalFiles()) {
            if (structure.hasFile(optionalFile)) {
                confidence += 0.1; // Optional files give some confidence
                foundConfigFiles.add(optionalFile);
            }
        }
        
        // Check for directory patterns
        for (String directoryPattern : rule.getDirectoryPatterns()) {
            if (structure.getDirectories().stream()
                    .anyMatch(dir -> dir.getName().matches(directoryPattern))) {
                confidence += 0.2;
            }
        }
        
        // Check for file patterns
        for (String filePattern : rule.getFilePatterns()) {
            Pattern pattern = Pattern.compile(filePattern);
            if (structure.getFiles().stream()
                    .anyMatch(file -> pattern.matcher(file.getName()).matches())) {
                confidence += 0.1;
            }
        }
        
        // Apply custom logic if available
        if (rule.getCustomLogic() != null) {
            confidence += rule.getCustomLogic().apply(structure);
        }
        
        // If confidence is too low, don't report this type
        if (confidence < rule.getMinConfidence()) {
            return null;
        }
        
        // Cap confidence at 1.0
        confidence = Math.min(confidence, 1.0);
        
        // Build project type result
        return buildProjectType(structure, type, confidence, foundConfigFiles);
    }
    
    /**
     * Build ProjectType object from detection results
     */
    private ProjectType buildProjectType(RepositoryStructure structure, ProjectTypeEnum type, 
                                       double confidence, List<String> configFiles) {
        
        ProjectType.ProjectTypeBuilder builder = ProjectType.builder()
                .type(type)
                .language(type.getPrimaryLanguage())
                .confidence(confidence)
                .configFiles(configFiles)
                .rootDirectory("/");
        
        // Extract specific information based on project type
        switch (type) {
            case MAVEN -> enhanceMavenProject(builder, structure);
            case GRADLE -> enhanceGradleProject(builder, structure);
            case NODE_JS -> enhanceNodeJsProject(builder, structure);
            case PYTHON -> enhancePythonProject(builder, structure);
            case SPRING_BOOT -> enhanceSpringBootProject(builder, structure);
            case REACT -> enhanceReactProject(builder, structure);
            case ANGULAR -> enhanceAngularProject(builder, structure);
            case VUE -> enhanceVueProject(builder, structure);
            case DOCKER -> enhanceDockerProject(builder, structure);
            case KUBERNETES -> enhanceKubernetesProject(builder, structure);
        }
        
        return builder.build();
    }
    
    /**
     * Initialize detection rules for all project types
     */
    private Map<ProjectTypeEnum, ProjectTypeDetectionRule> initializeDetectionRules() {
        Map<ProjectTypeEnum, ProjectTypeDetectionRule> rules = new HashMap<>();
        
        // Maven
        rules.put(ProjectTypeEnum.MAVEN, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("pom.xml"))
                .optionalFiles(Arrays.asList("mvnw", "mvnw.cmd", ".mvn/wrapper/maven-wrapper.properties"))
                .directoryPatterns(Arrays.asList("src/main/java", "src/test/java"))
                .filePatterns(Arrays.asList(".*\\.java$"))
                .minConfidence(0.4)
                .build());
        
        // Gradle
        rules.put(ProjectTypeEnum.GRADLE, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("build.gradle", "build.gradle.kts"))
                .optionalFiles(Arrays.asList("gradlew", "gradlew.bat", "gradle.properties", "settings.gradle"))
                .directoryPatterns(Arrays.asList("src/main/java", "src/test/java"))
                .filePatterns(Arrays.asList(".*\\.java$", ".*\\.kt$"))
                .minConfidence(0.4)
                .build());
        
        // Node.js
        rules.put(ProjectTypeEnum.NODE_JS, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("package.json"))
                .optionalFiles(Arrays.asList("package-lock.json", "yarn.lock", "node_modules"))
                .directoryPatterns(Arrays.asList("src", "lib", "dist"))
                .filePatterns(Arrays.asList(".*\\.js$", ".*\\.ts$", ".*\\.jsx$", ".*\\.tsx$"))
                .minConfidence(0.4)
                .build());
        
        // Python
        rules.put(ProjectTypeEnum.PYTHON, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("setup.py", "pyproject.toml", "requirements.txt"))
                .optionalFiles(Arrays.asList("Pipfile", "Pipfile.lock", "poetry.lock", "conda.yaml"))
                .directoryPatterns(Arrays.asList("src", "lib", "tests"))
                .filePatterns(Arrays.asList(".*\\.py$"))
                .minConfidence(0.4)
                .customLogic(this::detectPythonProject)
                .build());
        
        // Spring Boot
        rules.put(ProjectTypeEnum.SPRING_BOOT, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("pom.xml", "build.gradle"))
                .optionalFiles(Arrays.asList("application.yml", "application.properties"))
                .directoryPatterns(Arrays.asList("src/main/java", "src/main/resources"))
                .filePatterns(Arrays.asList(".*Application\\.java$"))
                .minConfidence(0.5)
                .customLogic(this::detectSpringBootProject)
                .build());
        
        // React
        rules.put(ProjectTypeEnum.REACT, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("package.json"))
                .optionalFiles(Arrays.asList("public/index.html", "src/index.js", "src/App.js"))
                .directoryPatterns(Arrays.asList("src", "public"))
                .filePatterns(Arrays.asList(".*\\.jsx?$"))
                .minConfidence(0.5)
                .customLogic(this::detectReactProject)
                .build());
        
        // Angular
        rules.put(ProjectTypeEnum.ANGULAR, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("angular.json", "package.json"))
                .optionalFiles(Arrays.asList("tsconfig.json", "src/main.ts"))
                .directoryPatterns(Arrays.asList("src/app"))
                .filePatterns(Arrays.asList(".*\\.ts$", ".*\\.html$"))
                .minConfidence(0.5)
                .build());
        
        // Vue.js
        rules.put(ProjectTypeEnum.VUE, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("package.json"))
                .optionalFiles(Arrays.asList("vue.config.js", "src/main.js"))
                .directoryPatterns(Arrays.asList("src"))
                .filePatterns(Arrays.asList(".*\\.vue$"))
                .minConfidence(0.5)
                .customLogic(this::detectVueProject)
                .build());
        
        // Docker
        rules.put(ProjectTypeEnum.DOCKER, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("Dockerfile"))
                .optionalFiles(Arrays.asList("docker-compose.yml", "docker-compose.yaml", ".dockerignore"))
                .minConfidence(0.4)
                .build());
        
        // Kubernetes
        rules.put(ProjectTypeEnum.KUBERNETES, ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("deployment.yaml", "service.yaml"))
                .optionalFiles(Arrays.asList("configmap.yaml", "ingress.yaml", "namespace.yaml"))
                .directoryPatterns(Arrays.asList("k8s", "kubernetes"))
                .filePatterns(Arrays.asList(".*\\.yaml$", ".*\\.yml$"))
                .minConfidence(0.4)
                .build());
        
        return rules;
    }
    
    /**
     * Custom logic for detecting Python projects
     */
    private double detectPythonProject(RepositoryStructure structure) {
        double confidence = 0.0;
        
        // Check for Python files
        long pythonFiles = structure.getFiles().stream()
                .filter(file -> file.getName().endsWith(".py"))
                .count();
        
        if (pythonFiles > 0) {
            confidence += 0.3;
        }
        
        // Check for common Python directories
        if (structure.hasDirectory("src") || structure.hasDirectory("lib")) {
            confidence += 0.1;
        }
        
        return confidence;
    }
    
    /**
     * Custom logic for detecting Spring Boot projects
     */
    private double detectSpringBootProject(RepositoryStructure structure) {
        double confidence = 0.0;
        
        // Check for Spring Boot application class
        boolean hasSpringBootApp = structure.getFiles().stream()
                .anyMatch(file -> file.getName().endsWith("Application.java") && 
                         file.getContent() != null && 
                         file.getContent().contains("@SpringBootApplication"));
        
        if (hasSpringBootApp) {
            confidence += 0.5;
        }
        
        // Check for Spring Boot configuration files
        if (structure.hasFile("application.yml") || structure.hasFile("application.properties")) {
            confidence += 0.2;
        }
        
        return confidence;
    }
    
    /**
     * Custom logic for detecting React projects
     */
    private double detectReactProject(RepositoryStructure structure) {
        double confidence = 0.0;
        
        // Check package.json for React dependency
        FileInfo packageJson = structure.getFiles().stream()
                .filter(file -> file.getName().equals("package.json"))
                .findFirst()
                .orElse(null);
        
        if (packageJson != null && packageJson.getContent() != null) {
            if (packageJson.getContent().contains("\"react\"")) {
                confidence += 0.5;
            }
        }
        
        // Check for React-specific files
        if (structure.hasFile("src/App.js") || structure.hasFile("src/App.jsx")) {
            confidence += 0.2;
        }
        
        return confidence;
    }
    
    /**
     * Custom logic for detecting Vue.js projects
     */
    private double detectVueProject(RepositoryStructure structure) {
        double confidence = 0.0;
        
        // Check for Vue files
        long vueFiles = structure.getFiles().stream()
                .filter(file -> file.getName().endsWith(".vue"))
                .count();
        
        if (vueFiles > 0) {
            confidence += 0.4;
        }
        
        // Check package.json for Vue dependency
        FileInfo packageJson = structure.getFiles().stream()
                .filter(file -> file.getName().equals("package.json"))
                .findFirst()
                .orElse(null);
        
        if (packageJson != null && packageJson.getContent() != null) {
            if (packageJson.getContent().contains("\"vue\"")) {
                confidence += 0.3;
            }
        }
        
        return confidence;
    }
    
    /**
     * Enhance Maven project information
     */
    private void enhanceMavenProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src/main/java", "src/main/resources"))
               .testDirectories(Arrays.asList("src/test/java", "src/test/resources"))
               .outputDirectories(Arrays.asList("target"));
        
        // TODO: Parse pom.xml to extract dependencies, version, etc.
    }
    
    /**
     * Enhance Gradle project information
     */
    private void enhanceGradleProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src/main/java", "src/main/kotlin", "src/main/resources"))
               .testDirectories(Arrays.asList("src/test/java", "src/test/kotlin", "src/test/resources"))
               .outputDirectories(Arrays.asList("build"));
        
        // TODO: Parse build.gradle to extract dependencies, version, etc.
    }
    
    /**
     * Enhance Node.js project information
     */
    private void enhanceNodeJsProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src", "lib"))
               .testDirectories(Arrays.asList("test", "tests", "__tests__"))
               .outputDirectories(Arrays.asList("dist", "build", "node_modules"));
        
        // TODO: Parse package.json to extract dependencies, version, etc.
    }
    
    /**
     * Enhance Python project information
     */
    private void enhancePythonProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src", "lib"))
               .testDirectories(Arrays.asList("test", "tests"))
               .outputDirectories(Arrays.asList("build", "dist", "__pycache__"));
        
        // TODO: Parse setup.py or pyproject.toml to extract dependencies, version, etc.
    }
    
    /**
     * Enhance Spring Boot project information
     */
    private void enhanceSpringBootProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src/main/java", "src/main/resources"))
               .testDirectories(Arrays.asList("src/test/java"))
               .outputDirectories(Arrays.asList("target", "build"));
        
        // TODO: Parse application.properties/yml for configuration
    }
    
    /**
     * Enhance React project information
     */
    private void enhanceReactProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src"))
               .testDirectories(Arrays.asList("src/__tests__", "tests"))
               .outputDirectories(Arrays.asList("build", "dist"));
        
        // TODO: Parse package.json for React-specific configuration
    }
    
    /**
     * Enhance Angular project information
     */
    private void enhanceAngularProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src/app"))
               .testDirectories(Arrays.asList("src/app/**/*.spec.ts"))
               .outputDirectories(Arrays.asList("dist", "node_modules"));
        
        // TODO: Parse angular.json for Angular-specific configuration
    }
    
    /**
     * Enhance Vue.js project information
     */
    private void enhanceVueProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        builder.sourceDirectories(Arrays.asList("src"))
               .testDirectories(Arrays.asList("tests"))
               .outputDirectories(Arrays.asList("dist", "build"));
        
        // TODO: Parse vue.config.js for Vue-specific configuration
    }
    
    /**
     * Enhance Docker project information
     */
    private void enhanceDockerProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        // TODO: Parse Dockerfile and docker-compose.yml
    }
    
    /**
     * Enhance Kubernetes project information
     */
    private void enhanceKubernetesProject(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        // TODO: Parse Kubernetes manifests
    }
    
    /**
     * Post-process detection results to enhance accuracy
     */
    private void enhanceDetectionResults(RepositoryStructure structure, List<ProjectType> detectedTypes) {
        // Resolve conflicts between similar project types
        resolveConflicts(detectedTypes);
        
        // Add composite project types
        addCompositeTypes(structure, detectedTypes);
        
        // Enhance with additional metadata
        enhanceWithMetadata(structure, detectedTypes);
    }
    
    /**
     * Resolve conflicts between similar project types
     */
    private void resolveConflicts(List<ProjectType> detectedTypes) {
        // If both Maven and Gradle are detected, keep the one with higher confidence
        ProjectType maven = detectedTypes.stream()
                .filter(t -> t.getType() == ProjectTypeEnum.MAVEN)
                .findFirst().orElse(null);
        
        ProjectType gradle = detectedTypes.stream()
                .filter(t -> t.getType() == ProjectTypeEnum.GRADLE)
                .findFirst().orElse(null);
        
        if (maven != null && gradle != null) {
            if (maven.getConfidence() < gradle.getConfidence()) {
                detectedTypes.remove(maven);
            } else {
                detectedTypes.remove(gradle);
            }
        }
    }
    
    /**
     * Add composite project types based on detected types
     */
    private void addCompositeTypes(RepositoryStructure structure, List<ProjectType> detectedTypes) {
        // Detect microservice architecture
        if (detectedTypes.stream().anyMatch(t -> t.getType() == ProjectTypeEnum.SPRING_BOOT) &&
            detectedTypes.stream().anyMatch(t -> t.getType() == ProjectTypeEnum.DOCKER)) {
            
            ProjectType microservice = ProjectType.builder()
                    .type(ProjectTypeEnum.MICROSERVICE)
                    .language("java")
                    .confidence(0.8)
                    .rootDirectory("/")
                    .build();
            
            detectedTypes.add(microservice);
        }
    }
    
    /**
     * Enhance detection results with additional metadata
     */
    private void enhanceWithMetadata(RepositoryStructure structure, List<ProjectType> detectedTypes) {
        for (ProjectType type : detectedTypes) {
            Map<String, String> metadata = new HashMap<>();
            
            // Add basic metadata
            metadata.put("detectedAt", String.valueOf(System.currentTimeMillis()));
            metadata.put("fileCount", String.valueOf(structure.getFiles().size()));
            metadata.put("directoryCount", String.valueOf(structure.getDirectories().size()));
            
            type.setMetadata(metadata);
        }
    }
}