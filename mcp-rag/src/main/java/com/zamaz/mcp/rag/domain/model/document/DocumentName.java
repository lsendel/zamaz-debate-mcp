package com.zamaz.mcp.rag.domain.model.document;

import java.util.Objects;

/**
 * Value Object representing a Document's name.
 * Enforces business rules for valid document names.
 */
public record DocumentName(String value) {
    
    private static final int MAX_LENGTH = 255;
    private static final String INVALID_CHARS_REGEX = "[<>:\"|?*\\x00-\\x1F]";
    
    public DocumentName {
        Objects.requireNonNull(value, "Document name cannot be null");
        
        if (value.isBlank()) {
            throw new IllegalArgumentException("Document name cannot be blank");
        }
        
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Document name cannot exceed %d characters", MAX_LENGTH)
            );
        }
        
        if (value.matches(".*" + INVALID_CHARS_REGEX + ".*")) {
            throw new IllegalArgumentException(
                "Document name contains invalid characters"
            );
        }
        
        // Trim whitespace
        value = value.trim();
    }
    
    /**
     * Factory method to create a DocumentName
     */
    public static DocumentName of(String value) {
        return new DocumentName(value);
    }
    
    /**
     * Get the file extension if present
     */
    public String getExtension() {
        int lastDotIndex = value.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < value.length() - 1) {
            return value.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Get the name without extension
     */
    public String getBaseName() {
        int lastDotIndex = value.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return value.substring(0, lastDotIndex);
        }
        return value;
    }
    
    /**
     * Check if the document is of a specific type
     */
    public boolean hasExtension(String extension) {
        return getExtension().equalsIgnoreCase(extension);
    }
    
    @Override
    public String toString() {
        return value;
    }
}