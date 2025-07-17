package com.zamaz.mcp.pattern.detector.design;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.zamaz.mcp.pattern.core.PatternCategory;
import com.zamaz.mcp.pattern.core.PatternDetector;
import com.zamaz.mcp.pattern.core.PatternSeverity;
import com.zamaz.mcp.pattern.core.PatternType;
import com.zamaz.mcp.pattern.model.CodeAnalysisContext;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import com.zamaz.mcp.pattern.model.LocationContext;
import com.zamaz.mcp.pattern.model.PatternImpact;
import com.zamaz.mcp.pattern.model.PatternPriority;
import com.zamaz.mcp.pattern.model.RefactoringEffort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detector for Singleton design pattern.
 * 
 * Identifies classic Singleton implementations including:
 * - Eager initialization
 * - Lazy initialization
 * - Thread-safe singleton
 * - Bill Pugh singleton
 * - Enum singleton
 */
@Component
public class SingletonPatternDetector implements PatternDetector {
    
    @Override
    public PatternType getPatternType() {
        return PatternType.SINGLETON;
    }
    
    @Override
    public PatternCategory getPatternCategory() {
        return PatternCategory.DESIGN_PATTERN;
    }
    
    @Override
    public String getDescription() {
        return "Detects Singleton design pattern implementations";
    }
    
    @Override
    public List<PatternDetectionResult> detectPatterns(CodeAnalysisContext context) {
        List<PatternDetectionResult> results = new ArrayList<>();
        
        SingletonVisitor visitor = new SingletonVisitor(context, results);
        context.getCompilationUnit().accept(visitor, null);
        
        return results;
    }
    
    private static class SingletonVisitor extends VoidVisitorAdapter<Void> {
        private final CodeAnalysisContext context;
        private final List<PatternDetectionResult> results;
        
        SingletonVisitor(CodeAnalysisContext context, List<PatternDetectionResult> results) {
            this.context = context;
            this.results = results;
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration clazz, Void arg) {
            if (clazz.isInterface()) {
                super.visit(clazz, arg);
                return;
            }
            
            SingletonAnalysis analysis = analyzeSingleton(clazz);
            if (analysis.isSingleton()) {
                createSingletonResult(clazz, analysis);
            }
            
            super.visit(clazz, arg);
        }
        
        private SingletonAnalysis analyzeSingleton(ClassOrInterfaceDeclaration clazz) {
            SingletonAnalysis analysis = new SingletonAnalysis();
            
            // Check for private constructors
            AtomicBoolean hasPrivateConstructor = new AtomicBoolean(false);
            AtomicBoolean hasPublicConstructor = new AtomicBoolean(false);
            
            clazz.getConstructors().forEach(constructor -> {
                if (constructor.isPrivate()) {
                    hasPrivateConstructor.set(true);
                } else if (constructor.isPublic()) {
                    hasPublicConstructor.set(true);
                }
            });
            
            // If no constructors, check for default constructor suppression
            if (clazz.getConstructors().isEmpty()) {
                hasPublicConstructor.set(true); // Default constructor is public
            }
            
            analysis.setHasPrivateConstructor(hasPrivateConstructor.get());
            analysis.setHasPublicConstructor(hasPublicConstructor.get());
            
            // Check for static instance field
            AtomicBoolean hasStaticInstanceField = new AtomicBoolean(false);
            AtomicBoolean hasGetInstanceMethod = new AtomicBoolean(false);
            
            clazz.getFields().forEach(field -> {
                if (field.isStatic() && field.getVariables().stream()
                        .anyMatch(var -> var.getType().asString().equals(clazz.getNameAsString()))) {
                    hasStaticInstanceField.set(true);
                    
                    // Check if it's final (eager initialization)
                    if (field.isFinal()) {
                        analysis.setEagerInitialization(true);
                    }
                    
                    // Check if it's volatile (thread-safe)
                    if (field.getModifiers().stream()
                            .anyMatch(mod -> mod.getKeyword().name().equals("VOLATILE"))) {
                        analysis.setThreadSafe(true);
                    }
                }
            });
            
            // Check for getInstance method
            clazz.getMethods().forEach(method -> {
                if (method.isStatic() && method.isPublic() && 
                    (method.getNameAsString().equals("getInstance") || 
                     method.getNameAsString().equals("instance"))) {
                    hasGetInstanceMethod.set(true);
                    
                    // Check for synchronized method (thread-safe)
                    if (method.isSynchronized()) {
                        analysis.setThreadSafe(true);
                    }
                    
                    // Check for double-checked locking
                    String methodBody = method.toString();
                    if (methodBody.contains("synchronized") && 
                        methodBody.contains("if") && 
                        methodBody.contains("null")) {
                        analysis.setDoubleCheckedLocking(true);
                        analysis.setThreadSafe(true);
                    }
                    
                    // Check for Bill Pugh singleton (inner class)
                    if (methodBody.contains("Holder") || methodBody.contains("holder")) {
                        analysis.setBillPughSingleton(true);
                    }
                }
            });
            
            analysis.setHasStaticInstanceField(hasStaticInstanceField.get());
            analysis.setHasGetInstanceMethod(hasGetInstanceMethod.get());
            
            // Check for enum singleton
            if (clazz.isEnumDeclaration()) {
                analysis.setEnumSingleton(true);
            }
            
            return analysis;
        }
        
        private void createSingletonResult(ClassOrInterfaceDeclaration clazz, SingletonAnalysis analysis) {
            String description = buildDescription(analysis);
            String explanation = buildExplanation(analysis);
            List<String> suggestions = buildSuggestions(analysis);
            
            PatternDetectionResult result = PatternDetectionResult.builder()
                    .patternType(PatternType.SINGLETON)
                    .patternCategory(PatternCategory.DESIGN_PATTERN)
                    .severity(PatternSeverity.LOW)
                    .confidence(analysis.getConfidence())
                    .filePath(context.getFilePath())
                    .startLine(clazz.getBegin().map(pos -> pos.line).orElse(0))
                    .endLine(clazz.getEnd().map(pos -> pos.line).orElse(0))
                    .startColumn(clazz.getBegin().map(pos -> pos.column).orElse(0))
                    .endColumn(clazz.getEnd().map(pos -> pos.column).orElse(0))
                    .description(description)
                    .explanation(explanation)
                    .suggestions(suggestions)
                    .detectorName("SingletonPatternDetector")
                    .detectorVersion("1.0")
                    .impact(analysis.isThreadSafe() ? PatternImpact.LOW : PatternImpact.MEDIUM)
                    .refactoringEffort(RefactoringEffort.LOW)
                    .priority(PatternPriority.LOW)
                    .codeSnippet(clazz.toString())
                    .locationContext(LocationContext.builder()
                            .className(clazz.getNameAsString())
                            .methodName(null)
                            .build())
                    .metadata("singletonType", analysis.getSingletonType())
                    .metadata("threadSafe", analysis.isThreadSafe())
                    .metadata("eagerInitialization", analysis.isEagerInitialization())
                    .build();
            
            results.add(result);
        }
        
        private String buildDescription(SingletonAnalysis analysis) {
            return "Singleton pattern implementation detected (" + analysis.getSingletonType() + ")";
        }
        
        private String buildExplanation(SingletonAnalysis analysis) {
            StringBuilder explanation = new StringBuilder();
            explanation.append("This class implements the Singleton pattern using ");
            explanation.append(analysis.getSingletonType().toLowerCase());
            explanation.append(" initialization. ");
            
            if (analysis.isThreadSafe()) {
                explanation.append("The implementation is thread-safe. ");
            } else {
                explanation.append("The implementation is NOT thread-safe. ");
            }
            
            if (analysis.isDoubleCheckedLocking()) {
                explanation.append("Uses double-checked locking for performance. ");
            }
            
            if (analysis.isBillPughSingleton()) {
                explanation.append("Uses Bill Pugh singleton pattern with inner class. ");
            }
            
            return explanation.toString();
        }
        
        private List<String> buildSuggestions(SingletonAnalysis analysis) {
            List<String> suggestions = new ArrayList<>();
            
            if (!analysis.isThreadSafe()) {
                suggestions.add("Make the singleton thread-safe by using synchronized methods or double-checked locking");
            }
            
            if (!analysis.isEnumSingleton() && !analysis.isBillPughSingleton()) {
                suggestions.add("Consider using enum singleton for the simplest and most effective implementation");
                suggestions.add("Consider using Bill Pugh singleton pattern for lazy initialization without synchronization overhead");
            }
            
            if (analysis.isEagerInitialization()) {
                suggestions.add("Consider lazy initialization if the singleton is expensive to create");
            }
            
            suggestions.add("Ensure the singleton is truly needed - consider dependency injection as an alternative");
            suggestions.add("Make the class final to prevent subclassing");
            
            return suggestions;
        }
    }
    
    private static class SingletonAnalysis {
        private boolean hasPrivateConstructor;
        private boolean hasPublicConstructor;
        private boolean hasStaticInstanceField;
        private boolean hasGetInstanceMethod;
        private boolean threadSafe;
        private boolean eagerInitialization;
        private boolean doubleCheckedLocking;
        private boolean billPughSingleton;
        private boolean enumSingleton;
        
        public boolean isSingleton() {
            if (enumSingleton) return true;
            
            // Classic singleton requirements
            return hasPrivateConstructor && 
                   !hasPublicConstructor && 
                   hasStaticInstanceField && 
                   hasGetInstanceMethod;
        }
        
        public double getConfidence() {
            double confidence = 0.0;
            
            if (enumSingleton) return 1.0;
            
            if (hasPrivateConstructor) confidence += 0.3;
            if (!hasPublicConstructor) confidence += 0.2;
            if (hasStaticInstanceField) confidence += 0.3;
            if (hasGetInstanceMethod) confidence += 0.2;
            
            return confidence;
        }
        
        public String getSingletonType() {
            if (enumSingleton) return "Enum";
            if (billPughSingleton) return "Bill Pugh";
            if (doubleCheckedLocking) return "Double-Checked Locking";
            if (threadSafe) return "Thread-Safe";
            if (eagerInitialization) return "Eager";
            return "Lazy";
        }
        
        // Getters and setters
        public boolean isHasPrivateConstructor() { return hasPrivateConstructor; }
        public void setHasPrivateConstructor(boolean hasPrivateConstructor) { this.hasPrivateConstructor = hasPrivateConstructor; }
        
        public boolean isHasPublicConstructor() { return hasPublicConstructor; }
        public void setHasPublicConstructor(boolean hasPublicConstructor) { this.hasPublicConstructor = hasPublicConstructor; }
        
        public boolean isHasStaticInstanceField() { return hasStaticInstanceField; }
        public void setHasStaticInstanceField(boolean hasStaticInstanceField) { this.hasStaticInstanceField = hasStaticInstanceField; }
        
        public boolean isHasGetInstanceMethod() { return hasGetInstanceMethod; }
        public void setHasGetInstanceMethod(boolean hasGetInstanceMethod) { this.hasGetInstanceMethod = hasGetInstanceMethod; }
        
        public boolean isThreadSafe() { return threadSafe; }
        public void setThreadSafe(boolean threadSafe) { this.threadSafe = threadSafe; }
        
        public boolean isEagerInitialization() { return eagerInitialization; }
        public void setEagerInitialization(boolean eagerInitialization) { this.eagerInitialization = eagerInitialization; }
        
        public boolean isDoubleCheckedLocking() { return doubleCheckedLocking; }
        public void setDoubleCheckedLocking(boolean doubleCheckedLocking) { this.doubleCheckedLocking = doubleCheckedLocking; }
        
        public boolean isBillPughSingleton() { return billPughSingleton; }
        public void setBillPughSingleton(boolean billPughSingleton) { this.billPughSingleton = billPughSingleton; }
        
        public boolean isEnumSingleton() { return enumSingleton; }
        public void setEnumSingleton(boolean enumSingleton) { this.enumSingleton = enumSingleton; }
    }
}