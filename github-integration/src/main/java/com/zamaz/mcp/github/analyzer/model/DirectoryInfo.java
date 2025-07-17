package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Information about a directory in the repository
 */
@Data
@Builder
public class DirectoryInfo {
    
    /**
     * Directory name
     */
    private String name;
    
    /**
     * Full path from repository root
     */
    private String path;
    
    /**
     * Number of files in this directory (direct children only)
     */
    private int fileCount;
    
    /**
     * Number of subdirectories in this directory (direct children only)
     */
    private int subdirectoryCount;
    
    /**
     * Total size of all files in this directory and subdirectories
     */
    private long totalSize;
    
    /**
     * Total lines of code in this directory and subdirectories
     */
    private int totalLinesOfCode;
    
    /**
     * Depth level from repository root
     */
    private int depth;
    
    /**
     * Whether this directory contains source code
     */
    private boolean sourceDirectory;
    
    /**
     * Whether this directory contains tests
     */
    private boolean testDirectory;
    
    /**
     * Whether this directory contains configuration files
     */
    private boolean configDirectory;
    
    /**
     * Whether this directory contains documentation
     */
    private boolean documentationDirectory;
    
    /**
     * Primary language in this directory
     */
    private String primaryLanguage;
    
    /**
     * Directory type (package, module, component, etc.)
     */
    private DirectoryType type;
    
    /**
     * Files directly in this directory
     */
    private List<String> files;
    
    /**
     * Subdirectories directly in this directory
     */
    private List<String> subdirectories;
    
    /**
     * Get parent directory path
     */
    public String getParentPath() {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "/";
    }
    
    /**
     * Check if directory is in specific parent directory
     */
    public boolean isInDirectory(String parentPath) {
        return path.startsWith(parentPath);
    }
    
    /**
     * Check if directory is a leaf directory (no subdirectories)
     */
    public boolean isLeafDirectory() {
        return subdirectoryCount == 0;
    }
    
    /**
     * Check if directory is empty
     */
    public boolean isEmpty() {
        return fileCount == 0 && subdirectoryCount == 0;
    }
    
    /**
     * Get relative path from another directory
     */
    public String getRelativePathFrom(String basePath) {
        if (path.startsWith(basePath)) {
            return path.substring(basePath.length());
        }
        return path;
    }
}