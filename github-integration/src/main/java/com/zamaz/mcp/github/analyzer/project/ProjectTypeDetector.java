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
    
    // Detection confidence thresholds
    private static final double REQUIRED_FILE_CONFIDENCE = 0.4;
    private static final double OPTIONAL_FILE_CONFIDENCE = 0.1;
    private static final double DIRECTORY_PATTERN_CONFIDENCE = 0.2;
    private static final double FILE_PATTERN_CONFIDENCE = 0.1;
    private static final double MAX_CONFIDENCE = 1.0;
    
    // Minimum confidence thresholds for project types
    private static final double STANDARD_MIN_CONFIDENCE = 0.4;
    private static final double FRAMEWORK_MIN_CONFIDENCE = 0.5;
    
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
                confidence += REQUIRED_FILE_CONFIDENCE;
                foundConfigFiles.add(requiredFile);
            } else {
                // If required file is missing, this project type is unlikely
                return null;
            }
        }
        
        // Check for optional files
        for (String optionalFile : rule.getOptionalFiles()) {
            if (structure.hasFile(optionalFile)) {
                confidence += OPTIONAL_FILE_CONFIDENCE;
                foundConfigFiles.add(optionalFile);
            }
        }
        
        // Check for directory patterns
        for (String directoryPattern : rule.getDirectoryPatterns()) {
            if (structure.getDirectories().stream()
                    .anyMatch(dir -> dir.getName().matches(directoryPattern))) {
                confidence += DIRECTORY_PATTERN_CONFIDENCE;
            }
        }
        
        // Check for file patterns
        for (String filePattern : rule.getFilePatterns()) {
            Pattern pattern = Pattern.compile(filePattern);
            if (structure.getFiles().stream()
                    .anyMatch(file -> pattern.matcher(file.getName()).matches())) {
                confidence += FILE_PATTERN_CONFIDENCE;
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
        
        // Cap confidence at maximum
        confidence = Math.min(confidence, MAX_CONFIDENCE);
        
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
        
        // Use project type specific enhancer
        getProjectEnhancer(type).enhance(builder, structure);
        
        return builder.build();
    }
    
    private ProjectEnhancer getProjectEnhancer(ProjectTypeEnum type) {
        return switch (type) {
            case MAVEN -> this::enhanceMavenProject;
            case GRADLE -> this::enhanceGradleProject;
            case NODE_JS -> this::enhanceNodeJsProject;
            case PYTHON -> this::enhancePythonProject;
            case SPRING_BOOT -> this::enhanceSpringBootProject;
            case REACT -> this::enhanceReactProject;
            case ANGULAR -> this::enhanceAngularProject;
            case VUE -> this::enhanceVueProject;
            case DOCKER -> this::enhanceDockerProject;
            case KUBERNETES -> this::enhanceKubernetesProject;
        };
    }
    
    @FunctionalInterface
    private interface ProjectEnhancer {
        void enhance(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure);
    }
    
    /**
     * Initialize detection rules for all project types
     */
    private Map<ProjectTypeEnum, ProjectTypeDetectionRule> initializeDetectionRules() {
        Map<ProjectTypeEnum, ProjectTypeDetectionRule> rules = new HashMap<>();
        
        rules.put(ProjectTypeEnum.MAVEN, createMavenRule());
        rules.put(ProjectTypeEnum.GRADLE, createGradleRule());
        rules.put(ProjectTypeEnum.NODE_JS, createNodeJsRule());
        rules.put(ProjectTypeEnum.PYTHON, createPythonRule());
        rules.put(ProjectTypeEnum.SPRING_BOOT, createSpringBootRule());
        rules.put(ProjectTypeEnum.REACT, createReactRule());
        rules.put(ProjectTypeEnum.ANGULAR, createAngularRule());
        rules.put(ProjectTypeEnum.VUE, createVueRule());
        rules.put(ProjectTypeEnum.DOCKER, createDockerRule());
        rules.put(ProjectTypeEnum.KUBERNETES, createKubernetesRule());
        
        return rules;
    }
    
    private ProjectTypeDetectionRule createMavenRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("pom.xml"))
                .optionalFiles(Arrays.asList("mvnw", "mvnw.cmd", ".mvn/wrapper/maven-wrapper.properties"))
                .directoryPatterns(Arrays.asList("src/main/java", "src/test/java"))
                .filePatterns(Arrays.asList(".*\\.java$"))
                .minConfidence(STANDARD_MIN_CONFIDENCE)
                .build();
    }
    
    private ProjectTypeDetectionRule createGradleRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("build.gradle", "build.gradle.kts"))
                .optionalFiles(Arrays.asList("gradlew", "gradlew.bat", "gradle.properties", "settings.gradle"))
                .directoryPatterns(Arrays.asList("src/main/java", "src/test/java"))
                .filePatterns(Arrays.asList(".*\\.java$", ".*\\.kt$"))
                .minConfidence(STANDARD_MIN_CONFIDENCE)
                .build();
    }
    
    private ProjectTypeDetectionRule createNodeJsRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("package.json"))
                .optionalFiles(Arrays.asList("package-lock.json", "yarn.lock", "node_modules"))
                .directoryPatterns(Arrays.asList("src", "lib", "dist"))
                .filePatterns(Arrays.asList(".*\\.js$", ".*\\.ts$", ".*\\.jsx$", ".*\\.tsx$"))
                .minConfidence(STANDARD_MIN_CONFIDENCE)
                .build();
    }
    
    private ProjectTypeDetectionRule createPythonRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("setup.py", "pyproject.toml", "requirements.txt"))
                .optionalFiles(Arrays.asList("Pipfile", "Pipfile.lock", "poetry.lock", "conda.yaml"))
                .directoryPatterns(Arrays.asList("src", "lib", "tests"))
                .filePatterns(Arrays.asList(".*\\.py$"))
                .minConfidence(STANDARD_MIN_CONFIDENCE)
                .customLogic(this::detectPythonProject)
                .build();
    }
    
    private ProjectTypeDetectionRule createSpringBootRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("pom.xml", "build.gradle"))
                .optionalFiles(Arrays.asList("application.yml", "application.properties"))
                .directoryPatterns(Arrays.asList("src/main/java", "src/main/resources"))
                .filePatterns(Arrays.asList(".*Application\\.java$"))
                .minConfidence(FRAMEWORK_MIN_CONFIDENCE)
                .customLogic(this::detectSpringBootProject)
                .build();
    }
    
    private ProjectTypeDetectionRule createReactRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("package.json"))
                .optionalFiles(Arrays.asList("public/index.html", "src/index.js", "src/App.js"))
                .directoryPatterns(Arrays.asList("src", "public"))
                .filePatterns(Arrays.asList(".*\\.jsx?$"))
                .minConfidence(FRAMEWORK_MIN_CONFIDENCE)
                .customLogic(this::detectReactProject)
                .build();
    }
    
    private ProjectTypeDetectionRule createAngularRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("angular.json", "package.json"))
                .optionalFiles(Arrays.asList("tsconfig.json", "src/main.ts"))
                .directoryPatterns(Arrays.asList("src/app"))
                .filePatterns(Arrays.asList(".*\\.ts$", ".*\\.html$"))
                .minConfidence(FRAMEWORK_MIN_CONFIDENCE)
                .build();
    }
    
    private ProjectTypeDetectionRule createVueRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("package.json"))
                .optionalFiles(Arrays.asList("vue.config.js", "src/main.js"))
                .directoryPatterns(Arrays.asList("src"))
                .filePatterns(Arrays.asList(".*\\.vue$"))
                .minConfidence(FRAMEWORK_MIN_CONFIDENCE)
                .customLogic(this::detectVueProject)
                .build();
    }
    
    private ProjectTypeDetectionRule createDockerRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("Dockerfile"))
                .optionalFiles(Arrays.asList("docker-compose.yml", "docker-compose.yaml", ".dockerignore"))
                .minConfidence(STANDARD_MIN_CONFIDENCE)
                .build();
    }
    
    private ProjectTypeDetectionRule createKubernetesRule() {
        return ProjectTypeDetectionRule.builder()
                .requiredFiles(Arrays.asList("deployment.yaml", "service.yaml"))
                .optionalFiles(Arrays.asList("configmap.yaml", "ingress.yaml", "namespace.yaml"))
                .directoryPatterns(Arrays.asList("k8s", "kubernetes"))
                .filePatterns(Arrays.asList(".*\\.yaml$", ".*\\.yml$"))
                .minConfidence(STANDARD_MIN_CONFIDENCE)
                .build();
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
        
        // Parse pom.xml to extract dependencies, version, etc.
        parsePomXml(builder, structure);
    }
    
    /**
     * Parse pom.xml file to extract Maven project information
     */
    private void parsePomXml(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        FileInfo pomXml = findPomXmlFile(structure);
        if (pomXml == null || pomXml.getContent() == null) {
            return;
        }
        
        try {
            String content = pomXml.getContent();
            Map<String, String> metadata = extractPomMetadata(content);
            builder.metadata(metadata);
        } catch (Exception e) {
            log.warn("Failed to parse pom.xml: {}", e.getMessage());
        }
    }
    
    private FileInfo findPomXmlFile(RepositoryStructure structure) {
        return structure.getFiles().stream()
                .filter(file -> file.getName().equals("pom.xml"))
                .findFirst()
                .orElse(null);
    }
    
    private Map<String, String> extractPomMetadata(String content) {
        Map<String, String> metadata = new HashMap<>();
        
        extractBasicPomInfo(content, metadata);
        extractJavaVersion(content, metadata);
        extractSpringBootInfo(content, metadata);
        extractDependencyInfo(content, metadata);
        extractPluginInfo(content, metadata);
        extractModuleInfo(content, metadata);
        
        return metadata;
    }
    
    private void extractBasicPomInfo(String content, Map<String, String> metadata) {
        addIfNotNull(metadata, "groupId", extractXmlValue(content, "groupId"));
        addIfNotNull(metadata, "artifactId", extractXmlValue(content, "artifactId"));
        addIfNotNull(metadata, "version", extractXmlValue(content, "version"));
        addIfNotNull(metadata, "name", extractXmlValue(content, "name"));
        addIfNotNull(metadata, "description", extractXmlValue(content, "description"));
        addIfNotNull(metadata, "packaging", extractXmlValue(content, "packaging"));
    }
    
    private void extractJavaVersion(String content, Map<String, String> metadata) {
        String javaVersion = extractXmlValue(content, "maven.compiler.source");
        if (javaVersion == null) {
            javaVersion = extractXmlValue(content, "java.version");
        }
        addIfNotNull(metadata, "javaVersion", javaVersion);
    }
    
    private void extractSpringBootInfo(String content, Map<String, String> metadata) {
        String springBootVersion = extractXmlValue(content, "spring-boot.version");
        if (springBootVersion != null) {
            metadata.put("springBootVersion", springBootVersion);
            metadata.put("framework", "Spring Boot");
        }
        
        if (content.contains("<artifactId>spring-boot-starter-parent</artifactId>")) {
            metadata.put("framework", "Spring Boot");
            String parentVersion = extractParentVersion(content, "spring-boot-starter-parent");
            addIfNotNull(metadata, "springBootVersion", parentVersion);
        }
    }
    
    private void extractDependencyInfo(String content, Map<String, String> metadata) {
        int dependencyCount = countXmlOccurrences(content, "<dependency>");
        metadata.put("dependencyCount", String.valueOf(dependencyCount));
        
        checkDependency(content, metadata, "spring-boot-starter", "hasSpringBoot");
        checkDependency(content, metadata, "junit", "hasJUnit");
        checkDependency(content, metadata, "mockito", "hasMockito");
        checkDependency(content, metadata, "testcontainers", "hasTestcontainers");
        checkDependency(content, metadata, "lombok", "hasLombok");
        checkDependency(content, metadata, "mapstruct", "hasMapStruct");
        checkDependency(content, metadata, "redis", "hasRedis");
        checkDependency(content, metadata, "docker", "hasDocker");
        checkDependency(content, metadata, "kubernetes", "hasKubernetes");
        
        if (content.contains("postgresql") || content.contains("mysql") || content.contains("h2")) {
            metadata.put("hasDatabase", "true");
        }
    }
    
    private void extractPluginInfo(String content, Map<String, String> metadata) {
        checkDependency(content, metadata, "spring-boot-maven-plugin", "hasSpringBootPlugin");
        checkDependency(content, metadata, "maven-compiler-plugin", "hasCompilerPlugin");
        checkDependency(content, metadata, "maven-surefire-plugin", "hasSurefirePlugin");
        checkDependency(content, metadata, "jacoco-maven-plugin", "hasJacocoPlugin");
        checkDependency(content, metadata, "spotbugs-maven-plugin", "hasSpotBugsPlugin");
        checkDependency(content, metadata, "checkstyle", "hasCheckstylePlugin");
    }
    
    private void extractModuleInfo(String content, Map<String, String> metadata) {
        if (content.contains("<modules>")) {
            metadata.put("isMultiModule", "true");
            int moduleCount = countXmlOccurrences(content, "<module>");
            metadata.put("moduleCount", String.valueOf(moduleCount));
        }
    }
    
    private void addIfNotNull(Map<String, String> metadata, String key, String value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
    
    private void checkDependency(String content, Map<String, String> metadata, String dependency, String key) {
        if (content.contains(dependency)) {
            metadata.put(key, "true");
        }
    }
    
    /**
     * Extract XML element value
     */
    private String extractXmlValue(String xml, String elementName) {
        String pattern = "<" + elementName + ">([^<]+)</" + elementName + ">";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }
    
    /**
     * Extract parent version for specific artifact
     */
    private String extractParentVersion(String xml, String artifactId) {
        String pattern = "<parent>.*?<artifactId>" + artifactId + "</artifactId>.*?<version>([^<]+)</version>.*?</parent>";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }
    
    /**
     * Count occurrences of XML element
     */
    private int countXmlOccurrences(String xml, String element) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(element);
        java.util.regex.Matcher m = p.matcher(xml);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
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
        
        // Parse package.json to extract dependencies, version, etc.
        parsePackageJson(builder, structure);
    }
    
    /**
     * Parse package.json file to extract project information
     */
    private void parsePackageJson(ProjectType.ProjectTypeBuilder builder, RepositoryStructure structure) {
        FileInfo packageJson = findPackageJsonFile(structure);
        if (packageJson == null || packageJson.getContent() == null) {
            return;
        }
        
        try {
            String content = packageJson.getContent();
            Map<String, String> metadata = extractPackageJsonMetadata(content);
            builder.metadata(metadata);
        } catch (Exception e) {
            log.warn("Failed to parse package.json: {}", e.getMessage());
        }
    }
    
    private FileInfo findPackageJsonFile(RepositoryStructure structure) {
        return structure.getFiles().stream()
                .filter(file -> file.getName().equals("package.json"))
                .findFirst()
                .orElse(null);
    }
    
    private Map<String, String> extractPackageJsonMetadata(String content) {
        Map<String, String> metadata = new HashMap<>();
        
        extractBasicPackageInfo(content, metadata);
        extractScriptsInfo(content, metadata);
        extractDependenciesInfo(content, metadata);
        extractProjectInfo(content, metadata);
        
        return metadata;
    }
    
    private void extractBasicPackageInfo(String content, Map<String, String> metadata) {
        addIfNotNull(metadata, "packageName", extractJsonField(content, "name"));
        addIfNotNull(metadata, "version", extractJsonField(content, "version"));
        addIfNotNull(metadata, "description", extractJsonField(content, "description"));
        addIfNotNull(metadata, "homepage", extractJsonField(content, "homepage"));
        addIfNotNull(metadata, "license", extractJsonField(content, "license"));
    }
    
    private void extractScriptsInfo(String content, Map<String, String> metadata) {
        String scripts = extractJsonObject(content, "scripts");
        if (scripts != null) {
            metadata.put("scripts", scripts);
            detectScriptPatterns(scripts, metadata);
        }
    }
    
    private void detectScriptPatterns(String scripts, Map<String, String> metadata) {
        checkJsonField(scripts, metadata, "\"test\"", "hasTests");
        checkJsonField(scripts, metadata, "\"build\"", "hasBuild");
        checkJsonField(scripts, metadata, "\"start\"", "hasStart");
        checkJsonField(scripts, metadata, "\"dev\"", "hasDev");
    }
    
    private void extractDependenciesInfo(String content, Map<String, String> metadata) {
        String dependencies = extractJsonObject(content, "dependencies");
        String devDependencies = extractJsonObject(content, "devDependencies");
        
        if (dependencies != null) {
            metadata.put("dependencies", dependencies);
            detectFrameworkDependencies(dependencies, metadata);
        }
        
        if (devDependencies != null) {
            metadata.put("devDependencies", devDependencies);
            detectDevDependencies(devDependencies, metadata);
        }
    }
    
    private void detectFrameworkDependencies(String dependencies, Map<String, String> metadata) {
        if (dependencies.contains("\"react\"")) metadata.put("framework", "React");
        if (dependencies.contains("\"vue\"")) metadata.put("framework", "Vue.js");
        if (dependencies.contains("\"angular\"") || dependencies.contains("\"@angular/core\"")) {
            metadata.put("framework", "Angular");
        }
        if (dependencies.contains("\"express\"")) metadata.put("serverFramework", "Express");
        if (dependencies.contains("\"next\"")) metadata.put("framework", "Next.js");
        if (dependencies.contains("\"nuxt\"")) metadata.put("framework", "Nuxt.js");
        if (dependencies.contains("\"svelte\"")) metadata.put("framework", "Svelte");
    }
    
    private void detectDevDependencies(String devDependencies, Map<String, String> metadata) {
        // Testing frameworks
        if (devDependencies.contains("\"jest\"")) metadata.put("testFramework", "Jest");
        if (devDependencies.contains("\"mocha\"")) metadata.put("testFramework", "Mocha");
        if (devDependencies.contains("\"cypress\"")) metadata.put("e2eFramework", "Cypress");
        if (devDependencies.contains("\"playwright\"")) metadata.put("e2eFramework", "Playwright");
        
        // Build tools
        if (devDependencies.contains("\"webpack\"")) metadata.put("bundler", "Webpack");
        if (devDependencies.contains("\"vite\"")) metadata.put("bundler", "Vite");
        if (devDependencies.contains("\"rollup\"")) metadata.put("bundler", "Rollup");
        
        // Code quality tools
        if (devDependencies.contains("\"typescript\"")) metadata.put("hasTypeScript", "true");
        if (devDependencies.contains("\"eslint\"")) metadata.put("hasLinting", "true");
        if (devDependencies.contains("\"prettier\"")) metadata.put("hasFormatting", "true");
    }
    
    private void extractProjectInfo(String content, Map<String, String> metadata) {
        addIfNotNull(metadata, "repository", extractJsonField(content, "repository"));
        addIfNotNull(metadata, "author", extractJsonField(content, "author"));
        addIfNotNull(metadata, "keywords", extractJsonArray(content, "keywords"));
    }
    
    private void checkJsonField(String content, Map<String, String> metadata, String field, String key) {
        if (content.contains(field)) {
            metadata.put(key, "true");
        }
    }
    
    /**
     * Extract a simple field value from JSON content
     */
    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    /**
     * Extract a JSON object as string
     */
    private String extractJsonObject(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\\{([^}]+)\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? "{" + m.group(1) + "}" : null;
    }
    
    /**
     * Extract a JSON array as string
     */
    private String extractJsonArray(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? "[" + m.group(1) + "]" : null;
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