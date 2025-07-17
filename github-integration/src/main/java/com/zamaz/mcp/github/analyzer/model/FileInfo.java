package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

/**
 * Information about a file in the repository
 */
@Data
@Builder
public class FileInfo {
    
    /**
     * File name
     */
    private String name;
    
    /**
     * Full path from repository root
     */
    private String path;
    
    /**
     * File extension
     */
    private String extension;
    
    /**
     * File size in bytes
     */
    private long size;
    
    /**
     * Number of lines in the file
     */
    private int lineCount;
    
    /**
     * File content (may be null for binary files or large files)
     */
    private String content;
    
    /**
     * SHA hash of the file
     */
    private String sha;
    
    /**
     * MIME type of the file
     */
    private String mimeType;
    
    /**
     * Whether the file is binary
     */
    private boolean binary;
    
    /**
     * Last modified timestamp
     */
    private long lastModified;
    
    /**
     * File permissions
     */
    private String permissions;
    
    /**
     * Language detected for this file
     */
    private String language;
    
    /**
     * Whether this file is a test file
     */
    private boolean testFile;
    
    /**
     * Whether this file is a configuration file
     */
    private boolean configFile;
    
    /**
     * Whether this file is documentation
     */
    private boolean documentationFile;
    
    /**
     * Complexity metrics for code files
     */
    private FileComplexity complexity;
    
    /**
     * Get file name without extension
     */
    public String getBaseName() {
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }
    
    /**
     * Get parent directory path
     */
    public String getParentPath() {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "/";
    }
    
    /**
     * Check if file is in specific directory
     */
    public boolean isInDirectory(String directoryPath) {
        return path.startsWith(directoryPath);
    }
    
    /**
     * Check if file is a source code file
     */
    public boolean isSourceFile() {
        return !binary && !testFile && !configFile && !documentationFile && 
               (language != null && !language.equals("unknown"));
    }
    
    /**
     * Check if file is large (> 1000 lines)
     */
    public boolean isLargeFile() {
        return lineCount > 1000;
    }
    
    /**
     * Check if file is empty
     */
    public boolean isEmpty() {
        return size == 0 || lineCount == 0;
    }
}