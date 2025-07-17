package com.zamaz.mcp.pattern.model;

import com.github.javaparser.ast.CompilationUnit;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context object containing all information needed for pattern analysis.
 * This includes source code, parsed AST, file metadata, and project context.
 */
@Data
@Builder
public class CodeAnalysisContext {
    
    /**
     * The source file being analyzed
     */
    private final Path filePath;
    
    /**
     * Raw source code content
     */
    private final String sourceCode;
    
    /**
     * Parsed AST representation
     */
    private final CompilationUnit compilationUnit;
    
    /**
     * File metadata (size, last modified, etc.)
     */
    private final FileMetadata fileMetadata;
    
    /**
     * Project-wide context information
     */
    private final ProjectContext projectContext;
    
    /**
     * Dependencies and imports used in this file
     */
    @Singular
    private final Set<String> imports;
    
    /**
     * Annotations present in the file
     */
    @Singular
    private final Set<String> annotations;
    
    /**
     * Class and interface names defined in this file
     */
    @Singular
    private final List<String> classNames;
    
    /**
     * Method signatures in this file
     */
    @Singular
    private final List<MethodSignature> methodSignatures;
    
    /**
     * Field information
     */
    @Singular
    private final List<FieldInfo> fields;
    
    /**
     * Metrics calculated for this file
     */
    private final CodeMetrics metrics;
    
    /**
     * Additional metadata that can be added by analyzers
     */
    @Singular
    private final Map<String, Object> additionalMetadata;
    
    /**
     * Lines of code in the file
     */
    private final int lineCount;
    
    /**
     * Cyclomatic complexity
     */
    private final int cyclomaticComplexity;
    
    /**
     * Number of classes in the file
     */
    private final int classCount;
    
    /**
     * Number of methods in the file
     */
    private final int methodCount;
    
    /**
     * Whether this file is a test file
     */
    private final boolean isTestFile;
    
    /**
     * Package name of the file
     */
    private final String packageName;
    
    /**
     * File extension
     */
    private final String fileExtension;
    
    /**
     * Check if the file contains a specific annotation
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations.contains(annotationName) || 
               annotations.stream().anyMatch(ann -> ann.contains(annotationName));
    }
    
    /**
     * Check if the file imports a specific class or package
     */
    public boolean hasImport(String importName) {
        return imports.contains(importName) || 
               imports.stream().anyMatch(imp -> imp.contains(importName));
    }
    
    /**
     * Get a specific piece of additional metadata
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalMetadata(String key, Class<T> type) {
        Object value = additionalMetadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Check if the file contains a specific class name
     */
    public boolean hasClass(String className) {
        return classNames.contains(className) || 
               classNames.stream().anyMatch(name -> name.endsWith("." + className));
    }
    
    /**
     * Get the primary class name (first class in the file)
     */
    public String getPrimaryClassName() {
        return classNames.isEmpty() ? null : classNames.get(0);
    }
    
    /**
     * Check if this is a Spring component
     */
    public boolean isSpringComponent() {
        return hasAnnotation("Component") || 
               hasAnnotation("Service") || 
               hasAnnotation("Repository") || 
               hasAnnotation("Controller") || 
               hasAnnotation("RestController") || 
               hasAnnotation("Configuration");
    }
    
    /**
     * Check if this is a JPA entity
     */
    public boolean isJpaEntity() {
        return hasAnnotation("Entity") || 
               hasAnnotation("Table") || 
               hasAnnotation("Embeddable");
    }
}