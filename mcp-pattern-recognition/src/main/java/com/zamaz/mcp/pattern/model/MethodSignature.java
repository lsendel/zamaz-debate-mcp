package com.zamaz.mcp.pattern.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Set;

/**
 * Information about a method signature for pattern analysis.
 */
@Data
@Builder
public class MethodSignature {
    
    /**
     * Method name
     */
    private final String name;
    
    /**
     * Return type
     */
    private final String returnType;
    
    /**
     * Parameter types
     */
    @Singular
    private final List<String> parameterTypes;
    
    /**
     * Parameter names
     */
    @Singular
    private final List<String> parameterNames;
    
    /**
     * Method modifiers (public, private, static, etc.)
     */
    @Singular
    private final Set<String> modifiers;
    
    /**
     * Method annotations
     */
    @Singular
    private final Set<String> annotations;
    
    /**
     * Number of lines in the method
     */
    private final int lineCount;
    
    /**
     * Cyclomatic complexity of the method
     */
    private final int cyclomaticComplexity;
    
    /**
     * Number of parameters
     */
    private final int parameterCount;
    
    /**
     * Whether this is a constructor
     */
    private final boolean isConstructor;
    
    /**
     * Whether this is a getter method
     */
    private final boolean isGetter;
    
    /**
     * Whether this is a setter method
     */
    private final boolean isSetter;
    
    /**
     * Whether this is a test method
     */
    private final boolean isTestMethod;
    
    /**
     * Class name that contains this method
     */
    private final String containingClass;
    
    /**
     * Line number where method starts
     */
    private final int startLine;
    
    /**
     * Line number where method ends
     */
    private final int endLine;
    
    /**
     * Check if method is public
     */
    public boolean isPublic() {
        return modifiers.contains("public");
    }
    
    /**
     * Check if method is private
     */
    public boolean isPrivate() {
        return modifiers.contains("private");
    }
    
    /**
     * Check if method is static
     */
    public boolean isStatic() {
        return modifiers.contains("static");
    }
    
    /**
     * Check if method is abstract
     */
    public boolean isAbstract() {
        return modifiers.contains("abstract");
    }
    
    /**
     * Check if method is synchronized
     */
    public boolean isSynchronized() {
        return modifiers.contains("synchronized");
    }
    
    /**
     * Check if method has a specific annotation
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations.contains(annotationName) || 
               annotations.stream().anyMatch(ann -> ann.contains(annotationName));
    }
    
    /**
     * Check if this is a long method (code smell)
     */
    public boolean isLongMethod() {
        return lineCount > 20; // Configurable threshold
    }
    
    /**
     * Check if this method has too many parameters (code smell)
     */
    public boolean hasTooManyParameters() {
        return parameterCount > 5; // Configurable threshold
    }
    
    /**
     * Check if this method is too complex (code smell)
     */
    public boolean isTooComplex() {
        return cyclomaticComplexity > 10; // Configurable threshold
    }
    
    /**
     * Get the method signature string
     */
    public String getSignatureString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType).append(" ").append(name).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes.get(i));
            if (i < parameterNames.size()) {
                sb.append(" ").append(parameterNames.get(i));
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Check if this is a utility method (static, public, no state change)
     */
    public boolean isUtilityMethod() {
        return isStatic() && isPublic() && !returnType.equals("void");
    }
    
    /**
     * Check if this is a Spring-specific method
     */
    public boolean isSpringMethod() {
        return hasAnnotation("RequestMapping") || 
               hasAnnotation("GetMapping") || 
               hasAnnotation("PostMapping") || 
               hasAnnotation("PutMapping") || 
               hasAnnotation("DeleteMapping") || 
               hasAnnotation("Bean") || 
               hasAnnotation("EventListener") || 
               hasAnnotation("Scheduled") || 
               hasAnnotation("Transactional");
    }
}