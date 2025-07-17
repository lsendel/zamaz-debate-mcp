package com.zamaz.mcp.pattern.detector.smell;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
import com.zamaz.mcp.pattern.model.CodeExample;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detector for God Class anti-pattern.
 * 
 * Identifies classes that are doing too much and violate the Single Responsibility Principle.
 * Uses multiple heuristics to detect god classes:
 * - High number of methods
 * - High number of fields
 * - High cyclomatic complexity
 * - High lines of code
 * - High coupling
 * - Low cohesion
 */
@Component
public class GodClassDetector implements PatternDetector {
    
    // Configurable thresholds
    private static final int HIGH_METHOD_COUNT = 20;
    private static final int HIGH_FIELD_COUNT = 15;
    private static final int HIGH_LOC = 500;
    private static final int HIGH_COMPLEXITY = 50;
    private static final double LOW_COHESION = 0.3;
    
    @Override
    public PatternType getPatternType() {
        return PatternType.GOD_CLASS;
    }
    
    @Override
    public PatternCategory getPatternCategory() {
        return PatternCategory.ANTI_PATTERN;
    }
    
    @Override
    public String getDescription() {
        return "Detects God Class anti-pattern - classes that do too much";
    }
    
    @Override
    public PerformanceImpact getPerformanceImpact() {
        return PerformanceImpact.MEDIUM;
    }
    
    @Override
    public List<PatternDetectionResult> detectPatterns(CodeAnalysisContext context) {
        List<PatternDetectionResult> results = new ArrayList<>();
        
        GodClassVisitor visitor = new GodClassVisitor(context, results);
        context.getCompilationUnit().accept(visitor, null);
        
        return results;
    }
    
    private static class GodClassVisitor extends VoidVisitorAdapter<Void> {
        private final CodeAnalysisContext context;
        private final List<PatternDetectionResult> results;
        
        GodClassVisitor(CodeAnalysisContext context, List<PatternDetectionResult> results) {
            this.context = context;
            this.results = results;
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration clazz, Void arg) {
            if (clazz.isInterface()) {
                super.visit(clazz, arg);
                return;
            }
            
            GodClassAnalysis analysis = analyzeClass(clazz);
            if (analysis.isGodClass()) {
                createGodClassResult(clazz, analysis);
            }
            
            super.visit(clazz, arg);
        }
        
        private GodClassAnalysis analyzeClass(ClassOrInterfaceDeclaration clazz) {
            GodClassAnalysis analysis = new GodClassAnalysis();
            
            // Count methods and calculate complexity
            AtomicInteger methodCount = new AtomicInteger(0);
            AtomicInteger totalComplexity = new AtomicInteger(0);
            AtomicInteger totalLoc = new AtomicInteger(0);
            
            clazz.getMethods().forEach(method -> {
                methodCount.incrementAndGet();
                int methodComplexity = calculateMethodComplexity(method);
                totalComplexity.addAndGet(methodComplexity);
                totalLoc.addAndGet(calculateMethodLoc(method));
            });
            
            analysis.setMethodCount(methodCount.get());
            analysis.setTotalComplexity(totalComplexity.get());
            analysis.setTotalLoc(totalLoc.get());
            
            // Count fields
            AtomicInteger fieldCount = new AtomicInteger(0);
            clazz.getFields().forEach(field -> {
                fieldCount.addAndGet(field.getVariables().size());
            });
            analysis.setFieldCount(fieldCount.get());
            
            // Calculate coupling (simplified - count imports and dependencies)
            analysis.setCouplingScore(calculateCoupling(clazz));
            
            // Calculate cohesion (simplified - method-field relationships)
            analysis.setCohesionScore(calculateCohesion(clazz));
            
            // Additional metrics
            analysis.setConstructorCount(clazz.getConstructors().size());
            analysis.setInnerClassCount(clazz.getMembers().stream()
                    .mapToInt(member -> member.isClassOrInterfaceDeclaration() ? 1 : 0)
                    .sum());
            
            return analysis;
        }
        
        private int calculateMethodComplexity(MethodDeclaration method) {
            // Simplified complexity calculation - count decision points
            String methodBody = method.toString();
            int complexity = 1; // Base complexity
            
            // Count decision points
            complexity += countOccurrences(methodBody, "if");
            complexity += countOccurrences(methodBody, "else");
            complexity += countOccurrences(methodBody, "while");
            complexity += countOccurrences(methodBody, "for");
            complexity += countOccurrences(methodBody, "do");
            complexity += countOccurrences(methodBody, "switch");
            complexity += countOccurrences(methodBody, "case");
            complexity += countOccurrences(methodBody, "catch");
            complexity += countOccurrences(methodBody, "&&");
            complexity += countOccurrences(methodBody, "||");
            complexity += countOccurrences(methodBody, "?");
            
            return complexity;
        }
        
        private int calculateMethodLoc(MethodDeclaration method) {
            return method.getEnd().map(pos -> pos.line).orElse(0) - 
                   method.getBegin().map(pos -> pos.line).orElse(0) + 1;
        }
        
        private int calculateCoupling(ClassOrInterfaceDeclaration clazz) {
            // Simplified coupling calculation
            int coupling = 0;
            
            // Count dependencies in fields
            clazz.getFields().forEach(field -> {
                String fieldType = field.getCommonType().toString();
                if (!isPrimitive(fieldType) && !fieldType.startsWith("java.lang")) {
                    coupling++;
                }
            });
            
            // Count dependencies in method parameters and return types
            clazz.getMethods().forEach(method -> {
                String returnType = method.getType().toString();
                if (!isPrimitive(returnType) && !returnType.startsWith("java.lang")) {
                    coupling++;
                }
                
                method.getParameters().forEach(param -> {
                    String paramType = param.getType().toString();
                    if (!isPrimitive(paramType) && !paramType.startsWith("java.lang")) {
                        coupling++;
                    }
                });
            });
            
            return coupling;
        }
        
        private double calculateCohesion(ClassOrInterfaceDeclaration clazz) {
            // Simplified cohesion calculation based on method-field relationships
            List<String> fieldNames = new ArrayList<>();
            clazz.getFields().forEach(field -> {
                field.getVariables().forEach(var -> {
                    fieldNames.add(var.getNameAsString());
                });
            });
            
            if (fieldNames.isEmpty()) return 1.0;
            
            int totalMethodFieldRelations = 0;
            int methodCount = clazz.getMethods().size();
            
            for (MethodDeclaration method : clazz.getMethods()) {
                String methodBody = method.toString();
                for (String fieldName : fieldNames) {
                    if (methodBody.contains(fieldName)) {
                        totalMethodFieldRelations++;
                    }
                }
            }
            
            int maxPossibleRelations = methodCount * fieldNames.size();
            return maxPossibleRelations > 0 ? (double) totalMethodFieldRelations / maxPossibleRelations : 1.0;
        }
        
        private boolean isPrimitive(String type) {
            return type.matches("(boolean|byte|short|int|long|float|double|char|void)");
        }
        
        private int countOccurrences(String text, String pattern) {
            return text.split(pattern, -1).length - 1;
        }
        
        private void createGodClassResult(ClassOrInterfaceDeclaration clazz, GodClassAnalysis analysis) {
            PatternSeverity severity = calculateSeverity(analysis);
            String description = buildDescription(analysis);
            String explanation = buildExplanation(analysis);
            List<String> suggestions = buildSuggestions(analysis);
            List<CodeExample> examples = buildCodeExamples();
            
            PatternDetectionResult result = PatternDetectionResult.builder()
                    .patternType(PatternType.GOD_CLASS)
                    .patternCategory(PatternCategory.ANTI_PATTERN)
                    .severity(severity)
                    .confidence(analysis.getConfidence())
                    .filePath(context.getFilePath())
                    .startLine(clazz.getBegin().map(pos -> pos.line).orElse(0))
                    .endLine(clazz.getEnd().map(pos -> pos.line).orElse(0))
                    .startColumn(clazz.getBegin().map(pos -> pos.column).orElse(0))
                    .endColumn(clazz.getEnd().map(pos -> pos.column).orElse(0))
                    .description(description)
                    .explanation(explanation)
                    .suggestions(suggestions)
                    .codeExamples(examples)
                    .detectorName("GodClassDetector")
                    .detectorVersion("1.0")
                    .impact(PatternImpact.HIGH)
                    .refactoringEffort(RefactoringEffort.HIGH)
                    .priority(PatternPriority.HIGH)
                    .codeSnippet(clazz.toString())
                    .locationContext(LocationContext.builder()
                            .className(clazz.getNameAsString())
                            .build())
                    .metadata("methodCount", analysis.getMethodCount())
                    .metadata("fieldCount", analysis.getFieldCount())
                    .metadata("totalComplexity", analysis.getTotalComplexity())
                    .metadata("totalLoc", analysis.getTotalLoc())
                    .metadata("couplingScore", analysis.getCouplingScore())
                    .metadata("cohesionScore", analysis.getCohesionScore())
                    .build();
            
            results.add(result);
        }
        
        private PatternSeverity calculateSeverity(GodClassAnalysis analysis) {
            int severityScore = 0;
            
            if (analysis.getMethodCount() > HIGH_METHOD_COUNT * 2) severityScore += 2;
            else if (analysis.getMethodCount() > HIGH_METHOD_COUNT) severityScore += 1;
            
            if (analysis.getFieldCount() > HIGH_FIELD_COUNT * 2) severityScore += 2;
            else if (analysis.getFieldCount() > HIGH_FIELD_COUNT) severityScore += 1;
            
            if (analysis.getTotalLoc() > HIGH_LOC * 2) severityScore += 2;
            else if (analysis.getTotalLoc() > HIGH_LOC) severityScore += 1;
            
            if (analysis.getTotalComplexity() > HIGH_COMPLEXITY * 2) severityScore += 2;
            else if (analysis.getTotalComplexity() > HIGH_COMPLEXITY) severityScore += 1;
            
            if (analysis.getCohesionScore() < LOW_COHESION / 2) severityScore += 2;
            else if (analysis.getCohesionScore() < LOW_COHESION) severityScore += 1;
            
            return switch (severityScore) {
                case 0, 1, 2 -> PatternSeverity.MEDIUM;
                case 3, 4, 5 -> PatternSeverity.HIGH;
                default -> PatternSeverity.CRITICAL;
            };
        }
        
        private String buildDescription(GodClassAnalysis analysis) {
            return String.format("God Class detected with %d methods, %d fields, %d LOC, complexity %d",
                    analysis.getMethodCount(), analysis.getFieldCount(), 
                    analysis.getTotalLoc(), analysis.getTotalComplexity());
        }
        
        private String buildExplanation(GodClassAnalysis analysis) {
            StringBuilder explanation = new StringBuilder();
            explanation.append("This class appears to be a God Class, violating the Single Responsibility Principle. ");
            explanation.append("It has excessive size and complexity: ");
            
            if (analysis.getMethodCount() > HIGH_METHOD_COUNT) {
                explanation.append(String.format("too many methods (%d), ", analysis.getMethodCount()));
            }
            if (analysis.getFieldCount() > HIGH_FIELD_COUNT) {
                explanation.append(String.format("too many fields (%d), ", analysis.getFieldCount()));
            }
            if (analysis.getTotalLoc() > HIGH_LOC) {
                explanation.append(String.format("too many lines of code (%d), ", analysis.getTotalLoc()));
            }
            if (analysis.getTotalComplexity() > HIGH_COMPLEXITY) {
                explanation.append(String.format("too high complexity (%d), ", analysis.getTotalComplexity()));
            }
            if (analysis.getCohesionScore() < LOW_COHESION) {
                explanation.append(String.format("low cohesion (%.2f), ", analysis.getCohesionScore()));
            }
            
            explanation.append("making it difficult to maintain and test.");
            
            return explanation.toString();
        }
        
        private List<String> buildSuggestions(GodClassAnalysis analysis) {
            List<String> suggestions = new ArrayList<>();
            
            suggestions.add("Break this class into smaller, more focused classes following the Single Responsibility Principle");
            suggestions.add("Identify groups of related methods and fields that can be extracted into separate classes");
            suggestions.add("Use composition instead of inheritance to reduce coupling");
            suggestions.add("Consider using the Strategy pattern for complex conditional logic");
            suggestions.add("Move utility methods to separate utility classes");
            suggestions.add("Extract data structures into separate value objects or DTOs");
            suggestions.add("Use dependency injection to reduce direct dependencies");
            suggestions.add("Consider using the Command pattern for complex operations");
            
            if (analysis.getCohesionScore() < LOW_COHESION) {
                suggestions.add("Improve cohesion by grouping related functionality together");
            }
            
            if (analysis.getCouplingScore() > 10) {
                suggestions.add("Reduce coupling by using interfaces and dependency injection");
            }
            
            return suggestions;
        }
        
        private List<CodeExample> buildCodeExamples() {
            List<CodeExample> examples = new ArrayList<>();
            
            examples.add(CodeExample.builder()
                    .title("Before: God Class")
                    .description("Example of a God Class that does too much")
                    .code("""
                            public class OrderProcessor {
                                private CustomerService customerService;
                                private PaymentService paymentService;
                                private InventoryService inventoryService;
                                private EmailService emailService;
                                private Logger logger;
                                // ... many more fields
                                
                                public void processOrder(Order order) {
                                    // Validate customer
                                    // Process payment
                                    // Update inventory
                                    // Send email
                                    // Log everything
                                    // ... hundreds of lines
                                }
                                
                                // ... many more methods
                            }
                            """)
                    .build());
            
            examples.add(CodeExample.builder()
                    .title("After: Refactored into focused classes")
                    .description("Breaking the God Class into smaller, focused classes")
                    .code("""
                            public class OrderProcessor {
                                private final OrderValidator validator;
                                private final PaymentProcessor paymentProcessor;
                                private final InventoryManager inventoryManager;
                                private final NotificationService notificationService;
                                
                                public void processOrder(Order order) {
                                    validator.validate(order);
                                    paymentProcessor.processPayment(order);
                                    inventoryManager.updateInventory(order);
                                    notificationService.sendConfirmation(order);
                                }
                            }
                            
                            public class OrderValidator {
                                public void validate(Order order) {
                                    // Focused validation logic
                                }
                            }
                            
                            public class PaymentProcessor {
                                public void processPayment(Order order) {
                                    // Focused payment logic
                                }
                            }
                            """)
                    .build());
            
            return examples;
        }
    }
    
    private static class GodClassAnalysis {
        private int methodCount;
        private int fieldCount;
        private int totalComplexity;
        private int totalLoc;
        private int couplingScore;
        private double cohesionScore;
        private int constructorCount;
        private int innerClassCount;
        
        public boolean isGodClass() {
            int godClassIndicators = 0;
            
            if (methodCount > HIGH_METHOD_COUNT) godClassIndicators++;
            if (fieldCount > HIGH_FIELD_COUNT) godClassIndicators++;
            if (totalLoc > HIGH_LOC) godClassIndicators++;
            if (totalComplexity > HIGH_COMPLEXITY) godClassIndicators++;
            if (cohesionScore < LOW_COHESION) godClassIndicators++;
            
            return godClassIndicators >= 3; // At least 3 indicators
        }
        
        public double getConfidence() {
            double confidence = 0.0;
            
            if (methodCount > HIGH_METHOD_COUNT) confidence += 0.2;
            if (fieldCount > HIGH_FIELD_COUNT) confidence += 0.2;
            if (totalLoc > HIGH_LOC) confidence += 0.2;
            if (totalComplexity > HIGH_COMPLEXITY) confidence += 0.2;
            if (cohesionScore < LOW_COHESION) confidence += 0.2;
            
            return Math.min(confidence, 1.0);
        }
        
        // Getters and setters
        public int getMethodCount() { return methodCount; }
        public void setMethodCount(int methodCount) { this.methodCount = methodCount; }
        
        public int getFieldCount() { return fieldCount; }
        public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
        
        public int getTotalComplexity() { return totalComplexity; }
        public void setTotalComplexity(int totalComplexity) { this.totalComplexity = totalComplexity; }
        
        public int getTotalLoc() { return totalLoc; }
        public void setTotalLoc(int totalLoc) { this.totalLoc = totalLoc; }
        
        public int getCouplingScore() { return couplingScore; }
        public void setCouplingScore(int couplingScore) { this.couplingScore = couplingScore; }
        
        public double getCohesionScore() { return cohesionScore; }
        public void setCohesionScore(double cohesionScore) { this.cohesionScore = cohesionScore; }
        
        public int getConstructorCount() { return constructorCount; }
        public void setConstructorCount(int constructorCount) { this.constructorCount = constructorCount; }
        
        public int getInnerClassCount() { return innerClassCount; }
        public void setInnerClassCount(int innerClassCount) { this.innerClassCount = innerClassCount; }
    }
}