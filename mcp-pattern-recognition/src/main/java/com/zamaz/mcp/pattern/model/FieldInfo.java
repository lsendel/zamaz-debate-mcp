package com.zamaz.mcp.pattern.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Set;

/**
 * Information about a field for pattern analysis.
 */
@Data
@Builder
public class FieldInfo {
    
    /**
     * Field name
     */
    private final String name;
    
    /**
     * Field type
     */
    private final String type;
    
    /**
     * Field modifiers (public, private, static, final, etc.)
     */
    @Singular
    private final Set<String> modifiers;
    
    /**
     * Field annotations
     */
    @Singular
    private final Set<String> annotations;
    
    /**
     * Whether the field is initialized
     */
    private final boolean isInitialized;
    
    /**
     * Initialization value if primitive or string
     */
    private final String initializationValue;
    
    /**
     * Line number where field is declared
     */
    private final int lineNumber;
    
    /**
     * Class name that contains this field
     */
    private final String containingClass;
    
    /**
     * Whether this field is used in the class
     */
    private final boolean isUsed;
    
    /**
     * Number of times this field is accessed
     */
    private final int accessCount;
    
    /**
     * Check if field is public
     */
    public boolean isPublic() {
        return modifiers.contains("public");
    }
    
    /**
     * Check if field is private
     */
    public boolean isPrivate() {
        return modifiers.contains("private");
    }
    
    /**
     * Check if field is static
     */
    public boolean isStatic() {
        return modifiers.contains("static");
    }
    
    /**
     * Check if field is final
     */
    public boolean isFinal() {
        return modifiers.contains("final");
    }
    
    /**
     * Check if field is transient
     */
    public boolean isTransient() {
        return modifiers.contains("transient");
    }
    
    /**
     * Check if field is volatile
     */
    public boolean isVolatile() {
        return modifiers.contains("volatile");
    }
    
    /**
     * Check if field has a specific annotation
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations.contains(annotationName) || 
               annotations.stream().anyMatch(ann -> ann.contains(annotationName));
    }
    
    /**
     * Check if this is a constant field
     */
    public boolean isConstant() {
        return isStatic() && isFinal() && isPublic();
    }
    
    /**
     * Check if this is a dependency injection field
     */
    public boolean isDependencyInjection() {
        return hasAnnotation("Autowired") || 
               hasAnnotation("Inject") || 
               hasAnnotation("Resource") || 
               hasAnnotation("Value");
    }
    
    /**
     * Check if this is a JPA field
     */
    public boolean isJpaField() {
        return hasAnnotation("Column") || 
               hasAnnotation("Id") || 
               hasAnnotation("GeneratedValue") || 
               hasAnnotation("JoinColumn") || 
               hasAnnotation("OneToMany") || 
               hasAnnotation("ManyToOne") || 
               hasAnnotation("OneToOne") || 
               hasAnnotation("ManyToMany");
    }
    
    /**
     * Check if this is a configuration property
     */
    public boolean isConfigurationProperty() {
        return hasAnnotation("ConfigurationProperties") || 
               hasAnnotation("Value");
    }
    
    /**
     * Check if this is an unused field (potential dead code)
     */
    public boolean isUnused() {
        return !isUsed && accessCount == 0;
    }
    
    /**
     * Check if this field violates encapsulation (public non-final)
     */
    public boolean violatesEncapsulation() {
        return isPublic() && !isFinal() && !isStatic();
    }
    
    /**
     * Check if this is a magic number/string
     */
    public boolean isMagicValue() {
        return isInitialized && initializationValue != null && 
               !initializationValue.equals("null") && 
               !initializationValue.equals("true") && 
               !initializationValue.equals("false") && 
               !initializationValue.equals("0") && 
               !initializationValue.equals("1") && 
               !initializationValue.equals("\"\"");
    }
    
    /**
     * Get the field declaration string
     */
    public String getDeclarationString() {
        StringBuilder sb = new StringBuilder();
        modifiers.forEach(modifier -> sb.append(modifier).append(" "));
        sb.append(type).append(" ").append(name);
        if (isInitialized && initializationValue != null) {
            sb.append(" = ").append(initializationValue);
        }
        return sb.toString().trim();
    }
}