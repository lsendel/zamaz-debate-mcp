package com.zamaz.mcp.github.analyzer.model;

/**
 * Enumeration of supported project types
 */
public enum ProjectTypeEnum {
    
    // Build Systems
    MAVEN("Maven"),
    GRADLE("Gradle"),
    NODE_JS("Node.js"),
    PYTHON("Python"),
    
    // Frameworks
    SPRING_BOOT("Spring Boot"),
    REACT("React"),
    ANGULAR("Angular"),
    VUE("Vue.js"),
    
    // Infrastructure
    DOCKER("Docker"),
    KUBERNETES("Kubernetes"),
    
    // Architecture Types
    MICROSERVICE("Microservice"),
    MONOLITH("Monolith"),
    LIBRARY("Library"),
    
    // Language-specific
    KOTLIN("Kotlin"),
    SCALA("Scala"),
    GO("Go"),
    RUST("Rust"),
    
    // Unknown
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    ProjectTypeEnum(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get project type from string
     */
    public static ProjectTypeEnum fromString(String type) {
        for (ProjectTypeEnum projectType : values()) {
            if (projectType.name().equalsIgnoreCase(type) || 
                projectType.displayName.equalsIgnoreCase(type)) {
                return projectType;
            }
        }
        return UNKNOWN;
    }
    
    /**
     * Check if this is a build system type
     */
    public boolean isBuildSystem() {
        return this == MAVEN || this == GRADLE || this == NODE_JS || this == PYTHON;
    }
    
    /**
     * Check if this is a framework type
     */
    public boolean isFramework() {
        return this == SPRING_BOOT || this == REACT || this == ANGULAR || this == VUE;
    }
    
    /**
     * Check if this is an infrastructure type
     */
    public boolean isInfrastructure() {
        return this == DOCKER || this == KUBERNETES;
    }
    
    /**
     * Check if this is an architecture type
     */
    public boolean isArchitecture() {
        return this == MICROSERVICE || this == MONOLITH || this == LIBRARY;
    }
    
    /**
     * Get primary language for this project type
     */
    public String getPrimaryLanguage() {
        return switch (this) {
            case MAVEN, GRADLE, SPRING_BOOT -> "java";
            case NODE_JS, REACT, ANGULAR, VUE -> "javascript";
            case PYTHON -> "python";
            case KOTLIN -> "kotlin";
            case SCALA -> "scala";
            case GO -> "go";
            case RUST -> "rust";
            default -> "unknown";
        };
    }
    
    /**
     * Get typical config files for this project type
     */
    public String[] getConfigFiles() {
        return switch (this) {
            case MAVEN -> new String[]{"pom.xml"};
            case GRADLE -> new String[]{"build.gradle", "build.gradle.kts"};
            case NODE_JS -> new String[]{"package.json", "package-lock.json", "yarn.lock"};
            case PYTHON -> new String[]{"setup.py", "pyproject.toml", "requirements.txt", "Pipfile"};
            case SPRING_BOOT -> new String[]{"application.yml", "application.properties"};
            case REACT -> new String[]{"package.json", "public/index.html"};
            case ANGULAR -> new String[]{"angular.json", "package.json"};
            case VUE -> new String[]{"vue.config.js", "package.json"};
            case DOCKER -> new String[]{"Dockerfile", "docker-compose.yml"};
            case KUBERNETES -> new String[]{"deployment.yaml", "service.yaml"};
            default -> new String[]{};
        };
    }
}