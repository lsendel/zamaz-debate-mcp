package com.zamaz.mcp.github.analyzer.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.ASTNodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * AST analyzer for Java source code
 */
@Slf4j
public class JavaASTAnalyzer implements ASTAnalyzer {
    
    private final JavaParser javaParser;
    
    public JavaASTAnalyzer() {
        this.javaParser = new JavaParser();
    }
    
    @Override
    public ASTAnalysisResult analyzeFiles(List<FileInfo> files) {
        log.info("Analyzing {} Java files", files.size());
        
        ASTAnalysisResult.ASTAnalysisResultBuilder resultBuilder = ASTAnalysisResult.builder()
                .language("java")
                .fileCount(files.size())
                .nodes(new ArrayList<>())
                .relationships(new ArrayList<>())
                .metrics(new HashMap<>())
                .complexity(new HashMap<>())
                .issues(new ArrayList<>());
        
        int totalClasses = 0;
        int totalMethods = 0;
        int totalFields = 0;
        int totalLines = 0;
        
        for (FileInfo file : files) {
            if (file.getContent() != null && file.getName().endsWith(".java")) {
                try {
                    FileASTResult fileResult = analyzeJavaFile(file);
                    
                    // Add nodes from this file
                    resultBuilder.nodes.addAll(fileResult.getNodes());
                    
                    // Add relationships from this file
                    resultBuilder.relationships.addAll(fileResult.getRelationships());
                    
                    // Accumulate metrics
                    totalClasses += fileResult.getClassCount();
                    totalMethods += fileResult.getMethodCount();
                    totalFields += fileResult.getFieldCount();
                    totalLines += file.getLineCount();
                    
                } catch (Exception e) {
                    log.error("Error analyzing Java file {}: {}", file.getName(), e.getMessage());
                    
                    // Add issue for parsing error
                    resultBuilder.issues.add(StructureInsight.builder()
                            .type(ModelEnums.InsightType.CODE_SMELL)
                            .severity(ModelEnums.InsightSeverity.WARNING)
                            .title("Parse Error")
                            .description("Failed to parse Java file: " + file.getName())
                            .recommendation("Check file syntax")
                            .affectedFiles(Arrays.asList(file.getName()))
                            .build());
                }
            }
        }
        
        // Calculate aggregate metrics
        resultBuilder.metrics.put("totalClasses", (double) totalClasses);
        resultBuilder.metrics.put("totalMethods", (double) totalMethods);
        resultBuilder.metrics.put("totalFields", (double) totalFields);
        resultBuilder.metrics.put("totalLines", (double) totalLines);
        resultBuilder.metrics.put("averageMethodsPerClass", totalClasses > 0 ? (double) totalMethods / totalClasses : 0.0);
        resultBuilder.metrics.put("averageFieldsPerClass", totalClasses > 0 ? (double) totalFields / totalClasses : 0.0);
        
        // Calculate complexity metrics
        resultBuilder.complexity.put("averageComplexity", calculateAverageComplexity(resultBuilder.nodes));
        resultBuilder.complexity.put("maxComplexity", calculateMaxComplexity(resultBuilder.nodes));
        
        ASTAnalysisResult result = resultBuilder.build();
        
        log.info("Java AST analysis completed: {} classes, {} methods, {} fields", 
                totalClasses, totalMethods, totalFields);
        
        return result;
    }
    
    /**
     * Analyze a single Java file
     */
    private FileASTResult analyzeJavaFile(FileInfo file) {
        List<ASTNode> nodes = new ArrayList<>();
        List<ASTRelationship> relationships = new ArrayList<>();
        
        try {
            CompilationUnit cu = javaParser.parse(file.getContent()).getResult().orElse(null);
            if (cu == null) {
                return FileASTResult.builder()
                        .fileName(file.getName())
                        .nodes(nodes)
                        .relationships(relationships)
                        .classCount(0)
                        .methodCount(0)
                        .fieldCount(0)
                        .build();
            }
            
            JavaASTVisitor visitor = new JavaASTVisitor(file.getName(), nodes, relationships);
            visitor.visit(cu, null);
            
            return FileASTResult.builder()
                    .fileName(file.getName())
                    .nodes(nodes)
                    .relationships(relationships)
                    .classCount(visitor.getClassCount())
                    .methodCount(visitor.getMethodCount())
                    .fieldCount(visitor.getFieldCount())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error parsing Java file {}: {}", file.getName(), e.getMessage());
            throw new RuntimeException("Failed to parse Java file: " + file.getName(), e);
        }
    }
    
    /**
     * Calculate average complexity across all nodes
     */
    private double calculateAverageComplexity(List<ASTNode> nodes) {
        return nodes.stream()
                .filter(node -> node.getComplexity() != null)
                .mapToDouble(node -> node.getComplexity().get("cyclomaticComplexity"))
                .average()
                .orElse(0.0);
    }
    
    /**
     * Calculate maximum complexity across all nodes
     */
    private double calculateMaxComplexity(List<ASTNode> nodes) {
        return nodes.stream()
                .filter(node -> node.getComplexity() != null)
                .mapToDouble(node -> node.getComplexity().get("cyclomaticComplexity"))
                .max()
                .orElse(0.0);
    }
    
    /**
     * Visitor for Java AST traversal
     */
    private static class JavaASTVisitor extends VoidVisitorAdapter<Void> {
        private final String fileName;
        private final List<ASTNode> nodes;
        private final List<ASTRelationship> relationships;
        
        private int classCount = 0;
        private int methodCount = 0;
        private int fieldCount = 0;
        
        public JavaASTVisitor(String fileName, List<ASTNode> nodes, List<ASTRelationship> relationships) {
            this.fileName = fileName;
            this.nodes = nodes;
            this.relationships = relationships;
        }
        
        @Override
        public void visit(PackageDeclaration n, Void arg) {
            String packageName = n.getNameAsString();
            
            ASTNode packageNode = ASTNode.builder()
                    .id(packageName)
                    .name(packageName)
                    .type(ASTNodeType.PACKAGE)
                    .fileName(fileName)
                    .startLine(n.getBegin().map(pos -> pos.line).orElse(0))
                    .endLine(n.getEnd().map(pos -> pos.line).orElse(0))
                    .properties(new HashMap<>())
                    .build();
            
            nodes.add(packageNode);
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ImportDeclaration n, Void arg) {
            String importName = n.getNameAsString();
            
            ASTNode importNode = ASTNode.builder()
                    .id(importName)
                    .name(importName)
                    .type(ASTNodeType.IMPORT)
                    .fileName(fileName)
                    .startLine(n.getBegin().map(pos -> pos.line).orElse(0))
                    .endLine(n.getEnd().map(pos -> pos.line).orElse(0))
                    .properties(new HashMap<>())
                    .build();
            
            importNode.getProperties().put("static", String.valueOf(n.isStatic()));
            importNode.getProperties().put("wildcard", String.valueOf(n.isAsterisk()));
            
            nodes.add(importNode);
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            classCount++;
            
            String className = n.getNameAsString();
            String fullyQualifiedName = n.getFullyQualifiedName().orElse(className);
            
            ASTNode classNode = ASTNode.builder()
                    .id(fullyQualifiedName)
                    .name(className)
                    .type(n.isInterface() ? ASTNodeType.INTERFACE : ASTNodeType.CLASS)
                    .fileName(fileName)
                    .startLine(n.getBegin().map(pos -> pos.line).orElse(0))
                    .endLine(n.getEnd().map(pos -> pos.line).orElse(0))
                    .properties(new HashMap<>())
                    .complexity(new HashMap<>())
                    .build();
            
            // Add properties
            classNode.getProperties().put("abstract", String.valueOf(n.isAbstract()));
            classNode.getProperties().put("final", String.valueOf(n.isFinal()));
            classNode.getProperties().put("static", String.valueOf(n.isStatic()));
            classNode.getProperties().put("public", String.valueOf(n.isPublic()));
            classNode.getProperties().put("private", String.valueOf(n.isPrivate()));
            classNode.getProperties().put("protected", String.valueOf(n.isProtected()));
            
            // Calculate complexity
            int cyclomaticComplexity = calculateClassComplexity(n);
            classNode.getComplexity().put("cyclomaticComplexity", (double) cyclomaticComplexity);
            
            nodes.add(classNode);
            
            // Add inheritance relationships
            n.getExtendedTypes().forEach(extendedType -> {
                ASTRelationship relationship = ASTRelationship.builder()
                        .sourceId(fullyQualifiedName)
                        .targetId(extendedType.getNameAsString())
                        .type("extends")
                        .fileName(fileName)
                        .build();
                relationships.add(relationship);
            });
            
            // Add implementation relationships
            n.getImplementedTypes().forEach(implementedType -> {
                ASTRelationship relationship = ASTRelationship.builder()
                        .sourceId(fullyQualifiedName)
                        .targetId(implementedType.getNameAsString())
                        .type("implements")
                        .fileName(fileName)
                        .build();
                relationships.add(relationship);
            });
            
            super.visit(n, arg);
        }
        
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            methodCount++;
            
            String methodName = n.getNameAsString();
            String signature = n.getSignature().asString();
            
            ASTNode methodNode = ASTNode.builder()
                    .id(signature)
                    .name(methodName)
                    .type(ASTNodeType.METHOD)
                    .fileName(fileName)
                    .startLine(n.getBegin().map(pos -> pos.line).orElse(0))
                    .endLine(n.getEnd().map(pos -> pos.line).orElse(0))
                    .properties(new HashMap<>())
                    .complexity(new HashMap<>())
                    .build();
            
            // Add properties
            methodNode.getProperties().put("abstract", String.valueOf(n.isAbstract()));
            methodNode.getProperties().put("final", String.valueOf(n.isFinal()));
            methodNode.getProperties().put("static", String.valueOf(n.isStatic()));
            methodNode.getProperties().put("public", String.valueOf(n.isPublic()));
            methodNode.getProperties().put("private", String.valueOf(n.isPrivate()));
            methodNode.getProperties().put("protected", String.valueOf(n.isProtected()));
            methodNode.getProperties().put("returnType", n.getTypeAsString());
            methodNode.getProperties().put("parameterCount", String.valueOf(n.getParameters().size()));
            
            // Calculate complexity
            int cyclomaticComplexity = calculateMethodComplexity(n);
            methodNode.getComplexity().put("cyclomaticComplexity", (double) cyclomaticComplexity);
            
            nodes.add(methodNode);
            super.visit(n, arg);
        }
        
        @Override
        public void visit(FieldDeclaration n, Void arg) {
            fieldCount++;
            
            n.getVariables().forEach(variable -> {
                String fieldName = variable.getNameAsString();
                
                ASTNode fieldNode = ASTNode.builder()
                        .id(fieldName)
                        .name(fieldName)
                        .type(ASTNodeType.FIELD)
                        .fileName(fileName)
                        .startLine(n.getBegin().map(pos -> pos.line).orElse(0))
                        .endLine(n.getEnd().map(pos -> pos.line).orElse(0))
                        .properties(new HashMap<>())
                        .build();
                
                // Add properties
                fieldNode.getProperties().put("final", String.valueOf(n.isFinal()));
                fieldNode.getProperties().put("static", String.valueOf(n.isStatic()));
                fieldNode.getProperties().put("public", String.valueOf(n.isPublic()));
                fieldNode.getProperties().put("private", String.valueOf(n.isPrivate()));
                fieldNode.getProperties().put("protected", String.valueOf(n.isProtected()));
                fieldNode.getProperties().put("type", variable.getTypeAsString());
                
                nodes.add(fieldNode);
            });
            
            super.visit(n, arg);
        }
        
        /**
         * Calculate cyclomatic complexity for a class
         */
        private int calculateClassComplexity(ClassOrInterfaceDeclaration n) {
            // Simple approximation - count decision points
            int complexity = 1; // Base complexity
            
            // This is a simplified version - in a real implementation,
            // you would traverse the AST to count actual decision points
            complexity += n.getMethods().size() * 2; // Rough estimate
            
            return complexity;
        }
        
        /**
         * Calculate cyclomatic complexity for a method
         */
        private int calculateMethodComplexity(MethodDeclaration n) {
            // Simple approximation - count decision points
            int complexity = 1; // Base complexity
            
            // This is a simplified version - in a real implementation,
            // you would traverse the AST to count if statements, loops, etc.
            String body = n.getBody().map(Object::toString).orElse("");
            complexity += countOccurrences(body, "if");
            complexity += countOccurrences(body, "while");
            complexity += countOccurrences(body, "for");
            complexity += countOccurrences(body, "switch");
            complexity += countOccurrences(body, "catch");
            
            return complexity;
        }
        
        /**
         * Count occurrences of a string in text
         */
        private int countOccurrences(String text, String substring) {
            int count = 0;
            int index = 0;
            
            while ((index = text.indexOf(substring, index)) != -1) {
                count++;
                index += substring.length();
            }
            
            return count;
        }
        
        public int getClassCount() { return classCount; }
        public int getMethodCount() { return methodCount; }
        public int getFieldCount() { return fieldCount; }
    }
}