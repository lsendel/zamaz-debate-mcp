package com.zamaz.mcp.github.analyzer.ast;

import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.ASTNodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AST analyzer for TypeScript source code
 * This extends the JavaScript analyzer with TypeScript-specific features
 */
@Slf4j
public class TypeScriptASTAnalyzer extends JavaScriptASTAnalyzer {
    
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("interface\\s+([A-Za-z_$][A-Za-z0-9_$]*)(?:\\s+extends\\s+([A-Za-z_$][A-Za-z0-9_$]*))?");
    private static final Pattern TYPE_PATTERN = Pattern.compile("type\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=");
    private static final Pattern ENUM_PATTERN = Pattern.compile("enum\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern TYPED_FUNCTION_PATTERN = Pattern.compile("(?:function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*:\\s*[A-Za-z_$][A-Za-z0-9_$]*|([A-Za-z_$][A-Za-z0-9_$]*)\\s*:\\s*\\([^)]*\\)\\s*=>\\s*[A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern GENERIC_PATTERN = Pattern.compile("<([A-Za-z_$][A-Za-z0-9_$,\\s<>]*)>");
    
    @Override
    public ASTAnalysisResult analyzeFiles(List<FileInfo> files) {
        log.info("Analyzing {} TypeScript files", files.size());
        
        ASTAnalysisResult.ASTAnalysisResultBuilder resultBuilder = ASTAnalysisResult.builder()
                .language("typescript")
                .fileCount(files.size())
                .nodes(new ArrayList<>())
                .relationships(new ArrayList<>())
                .metrics(new HashMap<>())
                .complexity(new HashMap<>())
                .issues(new ArrayList<>());
        
        int totalClasses = 0;
        int totalInterfaces = 0;
        int totalTypes = 0;
        int totalEnums = 0;
        int totalFunctions = 0;
        int totalVariables = 0;
        int totalImports = 0;
        int totalLines = 0;
        
        for (FileInfo file : files) {
            if (file.getContent() != null && isTypeScriptFile(file)) {
                try {
                    FileASTResult fileResult = analyzeTypeScriptFile(file);
                    
                    // Add nodes from this file
                    resultBuilder.nodes.addAll(fileResult.getNodes());
                    
                    // Add relationships from this file
                    resultBuilder.relationships.addAll(fileResult.getRelationships());
                    
                    // Accumulate metrics
                    totalClasses += fileResult.getClassCount();
                    totalInterfaces += (int) fileResult.getNodes().stream()
                            .filter(node -> node.getType() == ASTNodeType.INTERFACE)
                            .count();
                    totalTypes += (int) fileResult.getNodes().stream()
                            .filter(node -> node.getProperties().containsKey("typeAlias"))
                            .count();
                    totalEnums += (int) fileResult.getNodes().stream()
                            .filter(node -> node.getType() == ASTNodeType.ENUM)
                            .count();
                    totalFunctions += fileResult.getMethodCount();
                    totalVariables += fileResult.getFieldCount();
                    totalImports += (int) fileResult.getNodes().stream()
                            .filter(node -> node.getType() == ASTNodeType.IMPORT)
                            .count();
                    totalLines += file.getLineCount();
                    
                } catch (Exception e) {
                    log.error("Error analyzing TypeScript file {}: {}", file.getName(), e.getMessage());
                    
                    // Add issue for parsing error
                    resultBuilder.issues.add(StructureInsight.builder()
                            .type(ModelEnums.InsightType.CODE_SMELL)
                            .severity(ModelEnums.InsightSeverity.WARNING)
                            .title("Parse Error")
                            .description("Failed to parse TypeScript file: " + file.getName())
                            .recommendation("Check file syntax")
                            .affectedFiles(Arrays.asList(file.getName()))
                            .build());
                }
            }
        }
        
        // Calculate aggregate metrics
        resultBuilder.metrics.put("totalClasses", (double) totalClasses);
        resultBuilder.metrics.put("totalInterfaces", (double) totalInterfaces);
        resultBuilder.metrics.put("totalTypes", (double) totalTypes);
        resultBuilder.metrics.put("totalEnums", (double) totalEnums);
        resultBuilder.metrics.put("totalFunctions", (double) totalFunctions);
        resultBuilder.metrics.put("totalVariables", (double) totalVariables);
        resultBuilder.metrics.put("totalImports", (double) totalImports);
        resultBuilder.metrics.put("totalLines", (double) totalLines);
        resultBuilder.metrics.put("typeRatio", totalClasses > 0 ? (double) totalInterfaces / totalClasses : 0.0);
        
        // Calculate complexity metrics
        resultBuilder.complexity.put("averageComplexity", calculateAverageComplexity(resultBuilder.nodes));
        resultBuilder.complexity.put("maxComplexity", calculateMaxComplexity(resultBuilder.nodes));
        resultBuilder.complexity.put("typeComplexity", calculateTypeComplexity(resultBuilder.nodes));
        
        ASTAnalysisResult result = resultBuilder.build();
        
        log.info("TypeScript AST analysis completed: {} classes, {} interfaces, {} types, {} enums", 
                totalClasses, totalInterfaces, totalTypes, totalEnums);
        
        return result;
    }
    
    /**
     * Check if file is a TypeScript file
     */
    private boolean isTypeScriptFile(FileInfo file) {
        return file.getName().endsWith(".ts") || file.getName().endsWith(".tsx");
    }
    
    /**
     * Analyze a single TypeScript file
     */
    private FileASTResult analyzeTypeScriptFile(FileInfo file) {
        // First run the JavaScript analyzer to get basic structure
        FileASTResult jsResult = super.analyzeJavaScriptFile(file);
        
        List<ASTNode> nodes = new ArrayList<>(jsResult.getNodes());
        List<ASTRelationship> relationships = new ArrayList<>(jsResult.getRelationships());
        
        String[] lines = file.getContent().split("\n");
        int lineNumber = 0;
        
        int interfaceCount = 0;
        int typeCount = 0;
        int enumCount = 0;
        
        for (String line : lines) {
            lineNumber++;
            
            // Match interface definitions
            Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(line);
            if (interfaceMatcher.find()) {
                interfaceCount++;
                String interfaceName = interfaceMatcher.group(1);
                String extendsInterface = interfaceMatcher.group(2);
                
                ASTNode interfaceNode = ASTNode.builder()
                        .id(file.getName() + ":" + interfaceName)
                        .name(interfaceName)
                        .type(ASTNodeType.INTERFACE)
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .build();
                
                // Check for generics
                Matcher genericMatcher = GENERIC_PATTERN.matcher(line);
                if (genericMatcher.find()) {
                    interfaceNode.getProperties().put("generics", genericMatcher.group(1));
                }
                
                nodes.add(interfaceNode);
                
                // Add inheritance relationship
                if (extendsInterface != null) {
                    ASTRelationship relationship = ASTRelationship.builder()
                            .sourceId(file.getName() + ":" + interfaceName)
                            .targetId(extendsInterface)
                            .type("extends")
                            .fileName(file.getName())
                            .build();
                    relationships.add(relationship);
                }
            }
            
            // Match type aliases
            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                typeCount++;
                String typeName = typeMatcher.group(1);
                
                ASTNode typeNode = ASTNode.builder()
                        .id(file.getName() + ":" + typeName)
                        .name(typeName)
                        .type(ASTNodeType.CLASS) // Use CLASS type for type aliases
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .build();
                
                typeNode.getProperties().put("typeAlias", "true");
                
                nodes.add(typeNode);
            }
            
            // Match enum definitions
            Matcher enumMatcher = ENUM_PATTERN.matcher(line);
            if (enumMatcher.find()) {
                enumCount++;
                String enumName = enumMatcher.group(1);
                
                ASTNode enumNode = ASTNode.builder()
                        .id(file.getName() + ":" + enumName)
                        .name(enumName)
                        .type(ASTNodeType.ENUM)
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .build();
                
                nodes.add(enumNode);
            }
            
            // Enhance function nodes with type information
            enhanceFunctionWithTypes(line, nodes, lineNumber);
        }
        
        return FileASTResult.builder()
                .fileName(file.getName())
                .nodes(nodes)
                .relationships(relationships)
                .classCount(jsResult.getClassCount())
                .methodCount(jsResult.getMethodCount())
                .fieldCount(jsResult.getFieldCount())
                .build();
    }
    
    /**
     * Enhance function nodes with TypeScript type information
     */
    private void enhanceFunctionWithTypes(String line, List<ASTNode> nodes, int lineNumber) {
        Matcher typedFunctionMatcher = TYPED_FUNCTION_PATTERN.matcher(line);
        if (typedFunctionMatcher.find()) {
            String functionName = typedFunctionMatcher.group(1);
            if (functionName == null) functionName = typedFunctionMatcher.group(2);
            
            if (functionName != null) {
                // Find the corresponding function node and enhance it
                nodes.stream()
                        .filter(node -> node.getName().equals(functionName) && 
                                       node.getStartLine() == lineNumber)
                        .findFirst()
                        .ifPresent(node -> {
                            node.getProperties().put("typed", "true");
                            
                            // Extract return type
                            String returnType = extractReturnType(line);
                            if (returnType != null) {
                                node.getProperties().put("returnType", returnType);
                            }
                            
                            // Extract parameter types
                            String paramTypes = extractParameterTypes(line);
                            if (paramTypes != null) {
                                node.getProperties().put("parameterTypes", paramTypes);
                            }
                        });
            }
        }
    }
    
    /**
     * Extract return type from function signature
     */
    private String extractReturnType(String line) {
        Pattern returnTypePattern = Pattern.compile("\\)\\s*:\\s*([A-Za-z_$][A-Za-z0-9_$<>\\[\\]|&\\s]*)");
        Matcher matcher = returnTypePattern.matcher(line);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
    
    /**
     * Extract parameter types from function signature
     */
    private String extractParameterTypes(String line) {
        Pattern paramPattern = Pattern.compile("\\(([^)]*)\\)");
        Matcher matcher = paramPattern.matcher(line);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
    
    /**
     * Calculate type complexity based on generic usage and type definitions
     */
    private double calculateTypeComplexity(List<ASTNode> nodes) {
        int genericCount = 0;
        int typeCount = 0;
        
        for (ASTNode node : nodes) {
            if (node.getProperties().containsKey("generics")) {
                genericCount++;
            }
            if (node.getProperties().containsKey("typeAlias")) {
                typeCount++;
            }
        }
        
        return (double) (genericCount + typeCount) / Math.max(nodes.size(), 1);
    }
    
    /**
     * Calculate average complexity across all nodes
     */
    private double calculateAverageComplexity(List<ASTNode> nodes) {
        return nodes.stream()
                .filter(node -> node.getComplexity() != null && node.getComplexity().containsKey("cyclomaticComplexity"))
                .mapToDouble(node -> node.getComplexity().get("cyclomaticComplexity"))
                .average()
                .orElse(0.0);
    }
    
    /**
     * Calculate maximum complexity across all nodes
     */
    private double calculateMaxComplexity(List<ASTNode> nodes) {
        return nodes.stream()
                .filter(node -> node.getComplexity() != null && node.getComplexity().containsKey("cyclomaticComplexity"))
                .mapToDouble(node -> node.getComplexity().get("cyclomaticComplexity"))
                .max()
                .orElse(0.0);
    }
    
    @Override
    public String getSupportedLanguage() {
        return "typescript";
    }
    
    @Override
    public boolean supportsFile(FileInfo file) {
        return isTypeScriptFile(file);
    }
}