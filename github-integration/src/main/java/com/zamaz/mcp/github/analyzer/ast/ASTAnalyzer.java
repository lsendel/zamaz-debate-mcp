package com.zamaz.mcp.github.analyzer.ast;

import com.zamaz.mcp.github.analyzer.model.ASTAnalysisResult;
import com.zamaz.mcp.github.analyzer.model.FileInfo;

import java.util.List;

/**
 * Interface for AST analyzers for different programming languages
 */
public interface ASTAnalyzer {
    
    /**
     * Analyze a list of files and return AST analysis results
     * 
     * @param files List of files to analyze
     * @return AST analysis results
     */
    ASTAnalysisResult analyzeFiles(List<FileInfo> files);
    
    /**
     * Get the supported language for this analyzer
     * 
     * @return Language name (e.g., "java", "python", "javascript")
     */
    default String getSupportedLanguage() {
        return "unknown";
    }
    
    /**
     * Check if this analyzer supports the given file
     * 
     * @param file File to check
     * @return true if supported, false otherwise
     */
    default boolean supportsFile(FileInfo file) {
        return false;
    }
}