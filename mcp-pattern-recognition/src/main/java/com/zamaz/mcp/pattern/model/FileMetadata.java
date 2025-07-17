package com.zamaz.mcp.pattern.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Metadata about a source file being analyzed.
 */
@Data
@Builder
public class FileMetadata {
    
    /**
     * File name
     */
    private final String fileName;
    
    /**
     * File size in bytes
     */
    private final long fileSize;
    
    /**
     * Last modified timestamp
     */
    private final LocalDateTime lastModified;
    
    /**
     * File extension
     */
    private final String extension;
    
    /**
     * Relative path from project root
     */
    private final String relativePath;
    
    /**
     * Whether this is a test file
     */
    private final boolean isTestFile;
    
    /**
     * Whether this is a generated file
     */
    private final boolean isGenerated;
    
    /**
     * Encoding of the file
     */
    private final String encoding;
    
    /**
     * Number of lines in the file
     */
    private final int lineCount;
    
    /**
     * Number of non-empty lines
     */
    private final int nonEmptyLineCount;
    
    /**
     * Number of comment lines
     */
    private final int commentLineCount;
    
    /**
     * Hash of the file content for change detection
     */
    private final String contentHash;
    
    /**
     * Git information if available
     */
    private final GitInfo gitInfo;
    
    /**
     * Calculate the comment ratio
     */
    public double getCommentRatio() {
        return nonEmptyLineCount > 0 ? (double) commentLineCount / nonEmptyLineCount : 0.0;
    }
    
    /**
     * Check if this is a small file (configurable threshold)
     */
    public boolean isSmallFile() {
        return lineCount < 50;
    }
    
    /**
     * Check if this is a large file (configurable threshold)
     */
    public boolean isLargeFile() {
        return lineCount > 500;
    }
    
    /**
     * Check if this file has good documentation
     */
    public boolean hasGoodDocumentation() {
        return getCommentRatio() >= 0.2; // At least 20% comments
    }
}