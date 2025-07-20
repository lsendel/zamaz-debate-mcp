package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing file information for a document.
 */
public record FileInfo(
    String fileName,
    String fileType,
    long fileSize
) implements ValueObject {
    
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int MAX_FILENAME_LENGTH = 255;
    
    public FileInfo {
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(fileType, "File type cannot be null");
        
        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        
        if (fileName.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException(
                "File name cannot exceed " + MAX_FILENAME_LENGTH + " characters"
            );
        }
        
        if (fileType.trim().isEmpty()) {
            throw new IllegalArgumentException("File type cannot be empty");
        }
        
        if (fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                "File size cannot exceed " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB"
            );
        }
    }
    
    public static FileInfo of(String fileName, String fileType, long fileSize) {
        return new FileInfo(fileName.trim(), fileType.trim().toLowerCase(), fileSize);
    }
    
    public String getFileExtension() {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }
    
    public String getFileNameWithoutExtension() {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDot);
    }
    
    public boolean isPdf() {
        return "pdf".equalsIgnoreCase(getFileExtension()) || 
               "application/pdf".equalsIgnoreCase(fileType);
    }
    
    public boolean isWord() {
        String ext = getFileExtension();
        return "doc".equalsIgnoreCase(ext) || 
               "docx".equalsIgnoreCase(ext) ||
               fileType.contains("msword") ||
               fileType.contains("wordprocessingml");
    }
    
    public boolean isText() {
        return "txt".equalsIgnoreCase(getFileExtension()) || 
               "text/plain".equalsIgnoreCase(fileType);
    }
    
    public boolean isMarkdown() {
        String ext = getFileExtension();
        return "md".equalsIgnoreCase(ext) || 
               "markdown".equalsIgnoreCase(ext) ||
               "text/markdown".equalsIgnoreCase(fileType);
    }
    
    public boolean isSupported() {
        return isPdf() || isWord() || isText() || isMarkdown();
    }
    
    public String getHumanReadableSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    public boolean isLarge() {
        return fileSize > 10 * 1024 * 1024; // 10MB
    }
    
    public boolean isSmall() {
        return fileSize < 1024; // 1KB
    }
    
    @Override
    public String toString() {
        return String.format("FileInfo{name='%s', type='%s', size=%s}", 
            fileName, fileType, getHumanReadableSize());
    }
}