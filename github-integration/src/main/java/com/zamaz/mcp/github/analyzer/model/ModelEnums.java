package com.zamaz.mcp.github.analyzer.model;

/**
 * Common enums used across the model classes
 */
public class ModelEnums {
    
    /**
     * Directory types
     */
    public enum DirectoryType {
        PACKAGE,
        MODULE,
        COMPONENT,
        LAYER,
        FEATURE,
        UTILITY,
        CONFIGURATION,
        RESOURCE,
        TEST,
        DOCUMENTATION,
        BUILD,
        UNKNOWN
    }
    
    /**
     * File complexity levels
     */
    public enum FileComplexity {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }
    
    /**
     * Insight types
     */
    public enum InsightType {
        MULTI_LANGUAGE,
        LARGE_REPOSITORY,
        CIRCULAR_DEPENDENCY,
        HIGH_COUPLING,
        DEEP_PACKAGE_STRUCTURE,
        LARGE_FILES,
        ARCHITECTURE_PATTERN,
        NO_ARCHITECTURE_PATTERN,
        SECURITY_ISSUE,
        PERFORMANCE_ISSUE,
        MAINTAINABILITY_ISSUE,
        CODE_SMELL,
        BEST_PRACTICE,
        OPTIMIZATION_OPPORTUNITY
    }
    
    /**
     * Insight severity levels
     */
    public enum InsightSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    /**
     * Architecture pattern types
     */
    public enum ArchitecturePatternType {
        MVC,
        MVP,
        MVVM,
        CLEAN_ARCHITECTURE,
        HEXAGONAL,
        LAYERED,
        MICROSERVICES,
        MONOLITH,
        EVENT_DRIVEN,
        PIPE_AND_FILTER,
        CLIENT_SERVER,
        REPOSITORY_PATTERN,
        FACTORY_PATTERN,
        SINGLETON_PATTERN,
        OBSERVER_PATTERN,
        STRATEGY_PATTERN,
        UNKNOWN
    }
    
    /**
     * Dependency edge types
     */
    public enum DependencyEdgeType {
        DEPENDS_ON,
        INHERITS_FROM,
        IMPLEMENTS,
        USES,
        CONTAINS,
        AGGREGATES,
        COMPOSES
    }
    
    /**
     * Dependency categories
     */
    public enum DependencyCategory {
        FRAMEWORK,
        LIBRARY,
        UTILITY,
        TESTING,
        LOGGING,
        DATABASE,
        WEB,
        SECURITY,
        CONFIGURATION,
        BUILD_TOOL,
        DEVELOPMENT,
        RUNTIME,
        UNKNOWN
    }
    
    /**
     * Vulnerability severity levels
     */
    public enum VulnerabilitySeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * AST node types
     */
    public enum ASTNodeType {
        CLASS,
        INTERFACE,
        ENUM,
        ANNOTATION,
        METHOD,
        CONSTRUCTOR,
        FIELD,
        VARIABLE,
        PARAMETER,
        PACKAGE,
        IMPORT,
        COMMENT,
        UNKNOWN
    }
    
    /**
     * Code complexity metrics
     */
    public enum ComplexityMetric {
        CYCLOMATIC_COMPLEXITY,
        COGNITIVE_COMPLEXITY,
        NPATH_COMPLEXITY,
        HALSTEAD_COMPLEXITY,
        MAINTAINABILITY_INDEX,
        TECHNICAL_DEBT
    }
    
    /**
     * Visualization types
     */
    public enum VisualizationType {
        TREE,
        GRAPH,
        SUNBURST,
        TREEMAP,
        NETWORK,
        HIERARCHY,
        CIRCULAR,
        MATRIX
    }
    
    /**
     * Framework types
     */
    public enum FrameworkType {
        WEB,
        TESTING,
        ORM,
        DEPENDENCY_INJECTION,
        LOGGING,
        SERIALIZATION,
        CACHING,
        MESSAGING,
        SECURITY,
        MONITORING,
        UNKNOWN
    }
}