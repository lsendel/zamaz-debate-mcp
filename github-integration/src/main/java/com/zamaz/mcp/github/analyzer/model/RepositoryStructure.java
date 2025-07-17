package com.zamaz.mcp.github.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents the structure of a repository
 */
@Data
@Builder
public class RepositoryStructure {
    
    /**
     * Repository owner
     */
    private String owner;
    
    /**
     * Repository name
     */
    private String repository;
    
    /**
     * Branch being analyzed
     */
    private String branch;
    
    /**
     * Root path of the analysis
     */
    private String rootPath;
    
    /**
     * All files in the repository
     */
    private List<FileInfo> files;
    
    /**
     * All directories in the repository
     */
    private List<DirectoryInfo> directories;
    
    /**
     * Repository metadata
     */
    private RepositoryMetadata metadata;
    
    /**
     * Get files by extension
     */
    public List<FileInfo> getFilesByExtension(String extension) {
        return files.stream()
                .filter(file -> file.getName().toLowerCase().endsWith(extension.toLowerCase()))
                .toList();
    }
    
    /**
     * Get files by path pattern
     */
    public List<FileInfo> getFilesByPathPattern(String pattern) {
        return files.stream()
                .filter(file -> file.getPath().matches(pattern))
                .toList();
    }
    
    /**
     * Get directories by name
     */
    public List<DirectoryInfo> getDirectoriesByName(String name) {
        return directories.stream()
                .filter(dir -> dir.getName().equals(name))
                .toList();
    }
    
    /**
     * Get total lines of code
     */
    public int getTotalLinesOfCode() {
        return files.stream()
                .mapToInt(FileInfo::getLineCount)
                .sum();
    }
    
    /**
     * Get total file size in bytes
     */
    public long getTotalFileSize() {
        return files.stream()
                .mapToLong(FileInfo::getSize)
                .sum();
    }
    
    /**
     * Get maximum directory depth
     */
    public int getMaxDirectoryDepth() {
        return directories.stream()
                .mapToInt(dir -> dir.getPath().split("/").length)
                .max()
                .orElse(0);
    }
    
    /**
     * Check if repository has specific file
     */
    public boolean hasFile(String fileName) {
        return files.stream()
                .anyMatch(file -> file.getName().equals(fileName));
    }
    
    /**
     * Check if repository has specific directory
     */
    public boolean hasDirectory(String directoryName) {
        return directories.stream()
                .anyMatch(dir -> dir.getName().equals(directoryName));
    }
    
    /**
     * Get files in specific directory
     */
    public List<FileInfo> getFilesInDirectory(String directoryPath) {
        return files.stream()
                .filter(file -> file.getPath().startsWith(directoryPath))
                .toList();
    }
    
    /**
     * Get subdirectories of specific directory
     */
    public List<DirectoryInfo> getSubdirectories(String directoryPath) {
        return directories.stream()
                .filter(dir -> dir.getPath().startsWith(directoryPath) && !dir.getPath().equals(directoryPath))
                .toList();
    }
}