package com.zamaz.mcp.github.analyzer.ast;

import com.zamaz.mcp.github.analyzer.model.*;
import com.zamaz.mcp.github.analyzer.model.ModelEnums.ASTNodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AST analyzer for Python source code
 * This is a simplified implementation using regex parsing
 * In a production environment, you would use a proper Python parser
 */
@Slf4j
public class PythonASTAnalyzer implements ASTAnalyzer {
    
    private static final Pattern CLASS_PATTERN = Pattern.compile("^\\s*class\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:\\([^)]*\\))?\\s*:");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*def\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\([^)]*\\)\\s*:");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*(?:from\\s+([A-Za-z_][A-Za-z0-9_.]*)\\s+)?import\\s+([A-Za-z_][A-Za-z0-9_.,\\s*]*)");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*=");
    
    @Override
    public ASTAnalysisResult analyzeFiles(List<FileInfo> files) {
        log.info("Analyzing {} Python files", files.size());
        
        ASTAnalysisResult.ASTAnalysisResultBuilder resultBuilder = ASTAnalysisResult.builder()
                .language("python")
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
        int totalLines = 0;
        
        for (FileInfo file : files) {
            if (file.getContent() != null && file.getName().endsWith(".py")) {
                try {
                    FileASTResult fileResult = analyzePythonFile(file);
                    
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
                    totalLines += file.getLineCount();
                    
                } catch (Exception e) {
                    log.error("Error analyzing Python file {}: {}", file.getName(), e.getMessage());
                    
                    // Add issue for parsing error
                    resultBuilder.issues.add(StructureInsight.builder()
                            .type(ModelEnums.InsightType.CODE_SMELL)
                            .severity(ModelEnums.InsightSeverity.WARNING)
                            .title("Parse Error")
                            .description("Failed to parse Python file: " + file.getName())
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
        resultBuilder.metrics.put("totalLines", (double) totalLines);
        resultBuilder.metrics.put("averageFunctionsPerClass", totalClasses > 0 ? (double) totalFunctions / totalClasses : 0.0);
        
        // Calculate complexity metrics
        resultBuilder.complexity.put("averageComplexity", calculateAverageComplexity(resultBuilder.nodes));
        resultBuilder.complexity.put("maxComplexity", calculateMaxComplexity(resultBuilder.nodes));
        
        ASTAnalysisResult result = resultBuilder.build();
        
        log.info("Python AST analysis completed: {} classes, {} functions, {} variables", 
                totalClasses, totalFunctions, totalVariables);
        
        return result;
    }
    
    /**
     * Analyze a single Python file
     */
    private FileASTResult analyzePythonFile(FileInfo file) {
        List<ASTNode> nodes = new ArrayList<>();
        List<ASTRelationship> relationships = new ArrayList<>();
        
        String[] lines = file.getContent().split("\n");
        int lineNumber = 0;
        
        int classCount = 0;
        int functionCount = 0;
        int variableCount = 0;
        
        String currentClass = null;
        
        for (String line : lines) {
            lineNumber++;
            
            // Match class definitions
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                classCount++;
                String className = classMatcher.group(1);
                currentClass = className;
                
                ASTNode classNode = ASTNode.builder()
                        .id(file.getName() + ":" + className)
                        .name(className)
                        .type(ASTNodeType.CLASS)
                        .fileName(file.getName())
                        .startLine(lineNumber)
                        .endLine(lineNumber) // Will be updated when we find the end
                        .properties(new HashMap<>())
                        .complexity(new HashMap<>())
                        .build();
                
                // Calculate approximate complexity
                int complexity = calculateClassComplexity(lines, lineNumber);
                classNode.getComplexity().put("cyclomaticComplexity", (double) complexity);
                
                nodes.add(classNode);
                
                // Check for inheritance
                if (line.contains("(") && line.contains(")")) {
                    String inheritance = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
                    if (!inheritance.isEmpty()) {
                        String[] parentClasses = inheritance.split(",");
                        for (String parentClass : parentClasses) {
                            parentClass = parentClass.trim();
                            if (!parentClass.isEmpty()) {
                                ASTRelationship relationship = ASTRelationship.builder()
                                        .sourceId(file.getName() + ":" + className)
                                        .targetId(parentClass)
                                        .type("inherits")
                                        .fileName(file.getName())
                                        .build();
                                relationships.add(relationship);
                            }
                        }
                    }
                }
            }
            
            // Match function definitions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                functionCount++;
                String functionName = functionMatcher.group(1);
                
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
                
                // Add properties
                functionNode.getProperties().put("class", currentClass != null ? currentClass : "module");
                functionNode.getProperties().put("isMethod", String.valueOf(currentClass != null));
                
                // Calculate complexity
                int complexity = calculateFunctionComplexity(lines, lineNumber);
                functionNode.getComplexity().put("cyclomaticComplexity", (double) complexity);
                
                nodes.add(functionNode);
            }
            
            // Match import statements
            Matcher importMatcher = IMPORT_PATTERN.matcher(line);
            if (importMatcher.find()) {
                String fromModule = importMatcher.group(1);
                String importList = importMatcher.group(2);
                
                if (fromModule != null) {
                    // from module import items
                    ASTNode importNode = ASTNode.builder()
                            .id(file.getName() + ":import:" + fromModule)
                            .name(fromModule)
                            .type(ASTNodeType.IMPORT)
                            .fileName(file.getName())
                            .startLine(lineNumber)
                            .endLine(lineNumber)
                            .properties(new HashMap<>())
                            .build();
                    
                    importNode.getProperties().put("type", "from");
                    importNode.getProperties().put("items", importList);
                    
                    nodes.add(importNode);
                } else {
                    // direct import
                    String[] imports = importList.split(",");
                    for (String imp : imports) {
                        imp = imp.trim();
                        if (!imp.isEmpty()) {
                            ASTNode importNode = ASTNode.builder()
                                    .id(file.getName() + ":import:" + imp)
                                    .name(imp)
                                    .type(ASTNodeType.IMPORT)
                                    .fileName(file.getName())
                                    .startLine(lineNumber)
                                    .endLine(lineNumber)
                                    .properties(new HashMap<>())
                                    .build();
                            
                            importNode.getProperties().put("type", "direct");
                            
                            nodes.add(importNode);
                        }
                    }
                }
            }
            
            // Match variable assignments (simple case)
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(line);
            if (variableMatcher.find() && !line.trim().startsWith("def") && !line.trim().startsWith("class")) {
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
                
                variableNode.getProperties().put("class", currentClass != null ? currentClass : "module");
                
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
     * Calculate approximate complexity for a Python class
     */
    private int calculateClassComplexity(String[] lines, int startLine) {
        int complexity = 1; // Base complexity
        int indentLevel = getIndentLevel(lines[startLine - 1]);
        
        // Count decision points in the class
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i];
            int lineIndent = getIndentLevel(line);
            
            // If we've gone back to the same or lesser indent, we're out of the class
            if (lineIndent <= indentLevel && line.trim().length() > 0) {
                break;
            }
            
            // Count decision points
            if (line.contains("if ") || line.contains("elif ")) complexity++;
            if (line.contains("for ") || line.contains("while ")) complexity++;
            if (line.contains("try:") || line.contains("except")) complexity++;
            if (line.contains("with ")) complexity++;
        }
        
        return complexity;
    }
    
    /**
     * Calculate approximate complexity for a Python function
     */
    private int calculateFunctionComplexity(String[] lines, int startLine) {
        int complexity = 1; // Base complexity
        int indentLevel = getIndentLevel(lines[startLine - 1]);
        
        // Count decision points in the function
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i];
            int lineIndent = getIndentLevel(line);
            
            // If we've gone back to the same or lesser indent, we're out of the function
            if (lineIndent <= indentLevel && line.trim().length() > 0) {
                break;
            }
            
            // Count decision points
            if (line.contains("if ") || line.contains("elif ")) complexity++;
            if (line.contains("for ") || line.contains("while ")) complexity++;
            if (line.contains("try:") || line.contains("except")) complexity++;
            if (line.contains("with ")) complexity++;
        }
        
        return complexity;
    }
    
    /**
     * Get indentation level of a line
     */
    private int getIndentLevel(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 4; // Assume tab = 4 spaces
            } else {
                break;
            }
        }
        return indent;
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
        return "python";
    }
    
    @Override
    public boolean supportsFile(FileInfo file) {
        return file.getName().endsWith(".py");
    }
}