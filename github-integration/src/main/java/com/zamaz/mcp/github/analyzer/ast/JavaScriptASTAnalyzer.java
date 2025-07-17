package com.zamaz.mcp.github.analyzer.ast;

import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.ASTNodeType;
import lombok.extern.slf4j.Slf4j;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AST analyzer for JavaScript source code
 * This is a simplified implementation using regex parsing
 * In a production environment, you would use a proper JavaScript parser like Esprima
 */
@Slf4j
public class JavaScriptASTAnalyzer implements ASTAnalyzer {
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?:function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(|([A-Za-z_$][A-Za-z0-9_$]*)\\s*[:=]\\s*function\\s*\\(|([A-Za-z_$][A-Za-z0-9_$]*)\\s*[:=]\\s*\\([^)]*\\)\\s*=>)");
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([A-Za-z_$][A-Za-z0-9_$]*)(?:\\s+extends\\s+([A-Za-z_$][A-Za-z0-9_$]*))?");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+(?:(?:\\{[^}]*\\}|[A-Za-z_$][A-Za-z0-9_$]*|\\*)\\s+from\\s+)?['\"]([^'\"]+)['\"]");
    private static final Pattern EXPORT_PATTERN = Pattern.compile("export\\s+(?:default\\s+)?(?:class|function|const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    
    @Override
    public ASTAnalysisResult analyzeFiles(List<FileInfo> files) {
        log.info("Analyzing {} JavaScript files", files.size());
        
        ASTAnalysisResult.ASTAnalysisResultBuilder resultBuilder = ASTAnalysisResult.builder()
                .language("javascript")
                .fileCount(files.size())
                .nodes(new ArrayList<>())
                .relationships(new ArrayList<>())
                .metrics(new HashMap<>())
                .complexity(new HashMap<>())
                .issues(new ArrayList<>());
        
        int totalClasses = 0;
        int totalFunctions = 0;
        int totalVariables = 0;
        int totalImports = 0;
        int totalExports = 0;
        int totalLines = 0;
        
        for (FileInfo file : files) {
            if (file.getContent() != null && isJavaScriptFile(file)) {
                try {
                    FileASTResult fileResult = analyzeJavaScriptFile(file);
                    
                    // Add nodes from this file
                    resultBuilder.nodes.addAll(fileResult.getNodes());
                    
                    // Add relationships from this file
                    resultBuilder.relationships.addAll(fileResult.getRelationships());
                    
                    // Accumulate metrics
                    totalClasses += fileResult.getClassCount();
                    totalFunctions += fileResult.getMethodCount();
                    totalVariables += fileResult.getFieldCount();
                    totalImports += (int) fileResult.getNodes().stream()
                            .filter(node -> node.getType() == ASTNodeType.IMPORT)
                            .count();
                    totalExports += (int) fileResult.getNodes().stream()
                            .filter(node -> node.getProperties().containsKey("export"))
                            .count();
                    totalLines += file.getLineCount();
                    
                } catch (Exception e) {
                    log.error("Error analyzing JavaScript file {}: {}", file.getName(), e.getMessage());
                    
                    // Add issue for parsing error
                    resultBuilder.issues.add(StructureInsight.builder()
                            .type(ModelEnums.InsightType.CODE_SMELL)
                            .severity(ModelEnums.InsightSeverity.WARNING)
                            .title("Parse Error")
                            .description("Failed to parse JavaScript file: " + file.getName())
                            .recommendation("Check file syntax")
                            .affectedFiles(Arrays.asList(file.getName()))
                            .build());
                }
            }
        }
        
        // Calculate aggregate metrics
        resultBuilder.metrics.put("totalClasses", (double) totalClasses);
        resultBuilder.metrics.put("totalFunctions", (double) totalFunctions);
        resultBuilder.metrics.put("totalVariables", (double) totalVariables);
        resultBuilder.metrics.put("totalImports", (double) totalImports);
        resultBuilder.metrics.put("totalExports", (double) totalExports);
        resultBuilder.metrics.put("totalLines", (double) totalLines);
        resultBuilder.metrics.put("averageFunctionsPerClass", totalClasses > 0 ? (double) totalFunctions / totalClasses : 0.0);
        
        // Calculate complexity metrics
        resultBuilder.complexity.put("averageComplexity", calculateAverageComplexity(resultBuilder.nodes));
        resultBuilder.complexity.put("maxComplexity", calculateMaxComplexity(resultBuilder.nodes));
        
        ASTAnalysisResult result = resultBuilder.build();
        
        log.info("JavaScript AST analysis completed: {} classes, {} functions, {} variables", 
                totalClasses, totalFunctions, totalVariables);
        
        return result;
    }
    
    /**
     * Check if file is a JavaScript file
     */
    private boolean isJavaScriptFile(FileInfo file) {
        return file.getName().endsWith(".js") || file.getName().endsWith(".jsx") || 
               file.getName().endsWith(".mjs") || file.getName().endsWith(".es6");
    }
    
    /**
     * Analyze a single JavaScript file
     */
    private FileASTResult analyzeJavaScriptFile(FileInfo file) {
        List<ASTNode> nodes = new ArrayList<>();
        List<ASTRelationship> relationships = new ArrayList<>();
        
        String[] lines = file.getContent().split("\n");
        int lineNumber = 0;
        
        int classCount = 0;
        int functionCount = 0;
        int variableCount = 0;
        
        for (String line : lines) {
            lineNumber++;
            
            // Match class definitions
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                classCount++;
                String className = classMatcher.group(1);
                String extendsClass = classMatcher.group(2);
                
                ASTNode classNode = ASTNode.builder()
                        .id(file.getName() + ":" + className)
                        .name(className)
                        .type(ASTNodeType.CLASS)
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .complexity(new HashMap<>())
                        .build();
                
                // Calculate complexity
                int complexity = calculateComplexity(lines, lineNumber);
                classNode.getComplexity().put("cyclomaticComplexity", (double) complexity);
                
                nodes.add(classNode);
                
                // Add inheritance relationship
                if (extendsClass != null) {
                    ASTRelationship relationship = ASTRelationship.builder()
                            .sourceId(file.getName() + ":" + className)
                            .targetId(extendsClass)
                            .type("extends")
                            .fileName(file.getName())
                            .build();
                    relationships.add(relationship);
                }
            }
            
            // Match function definitions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                functionCount++;
                String functionName = functionMatcher.group(1);
                if (functionName == null) functionName = functionMatcher.group(2);
                if (functionName == null) functionName = functionMatcher.group(3);
                
                if (functionName != null) {
                    ASTNode functionNode = ASTNode.builder()
                            .id(file.getName() + ":" + functionName)
                            .name(functionName)
                            .type(ASTNodeType.METHOD)
                            .fileName(file.getName())
                            .startLine(lineNumber)
                            .endLine(lineNumber)
                            .properties(new HashMap<>())
                            .complexity(new HashMap<>())
                            .build();
                    
                    // Detect function type
                    if (line.contains("=>")) {
                        functionNode.getProperties().put("type", "arrow");
                    } else if (line.contains("function")) {
                        functionNode.getProperties().put("type", "function");
                    } else {
                        functionNode.getProperties().put("type", "method");
                    }
                    
                    // Calculate complexity
                    int complexity = calculateComplexity(lines, lineNumber);
                    functionNode.getComplexity().put("cyclomaticComplexity", (double) complexity);
                    
                    nodes.add(functionNode);
                }
            }
            
            // Match import statements
            Matcher importMatcher = IMPORT_PATTERN.matcher(line);
            if (importMatcher.find()) {
                String importPath = importMatcher.group(1);
                
                ASTNode importNode = ASTNode.builder()
                        .id(file.getName() + ":import:" + importPath)
                        .name(importPath)
                        .type(ASTNodeType.IMPORT)
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .build();
                
                // Determine import type
                if (importPath.startsWith("./") || importPath.startsWith("../")) {
                    importNode.getProperties().put("type", "relative");
                } else if (importPath.startsWith("/")) {
                    importNode.getProperties().put("type", "absolute");
                } else {
                    importNode.getProperties().put("type", "module");
                }
                
                nodes.add(importNode);
            }
            
            // Match export statements
            Matcher exportMatcher = EXPORT_PATTERN.matcher(line);
            if (exportMatcher.find()) {
                String exportName = exportMatcher.group(1);
                
                ASTNode exportNode = ASTNode.builder()
                        .id(file.getName() + ":export:" + exportName)
                        .name(exportName)
                        .type(ASTNodeType.METHOD) // Could be class, function, or variable
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .build();
                
                exportNode.getProperties().put("export", "true");
                exportNode.getProperties().put("default", line.contains("default") ? "true" : "false");
                
                nodes.add(exportNode);
            }
            
            // Match variable declarations
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(line);
            if (variableMatcher.find()) {
                variableCount++;
                String variableName = variableMatcher.group(1);
                
                ASTNode variableNode = ASTNode.builder()
                        .id(file.getName() + ":" + variableName)
                        .name(variableName)
                        .type(ASTNodeType.VARIABLE)
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .properties(new HashMap<>())
                        .build();
                
                // Determine variable type
                if (line.contains("const")) {
                    variableNode.getProperties().put("type", "const");
                } else if (line.contains("let")) {
                    variableNode.getProperties().put("type", "let");
                } else if (line.contains("var")) {
                    variableNode.getProperties().put("type", "var");
                }
                
                nodes.add(variableNode);
            }
        }
        
        return FileASTResult.builder()
                .fileName(file.getName())
                .nodes(nodes)
                .relationships(relationships)
                .classCount(classCount)
                .methodCount(functionCount)
                .fieldCount(variableCount)
                .build();
    }
    
    /**
     * Calculate cyclomatic complexity for a block of code
     */
    private int calculateComplexity(String[] lines, int startLine) {
        int complexity = 1; // Base complexity
        
        // Simple complexity calculation - count decision points
        for (int i = startLine; i < Math.min(startLine + 50, lines.length); i++) {
            String line = lines[i];
            
            // Count decision points
            if (line.contains("if") || line.contains("else if")) complexity++;
            if (line.contains("switch")) complexity++;
            if (line.contains("case")) complexity++;
            if (line.contains("for") || line.contains("while")) complexity++;
            if (line.contains("try") || line.contains("catch")) complexity++;
            if (line.contains("&&") || line.contains("||")) complexity++;
            if (line.contains("?")) complexity++; // Ternary operator
        }
        
        return complexity;
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
        return "javascript";
    }
    
    @Override
    public boolean supportsFile(FileInfo file) {
        return isJavaScriptFile(file);
    }
}