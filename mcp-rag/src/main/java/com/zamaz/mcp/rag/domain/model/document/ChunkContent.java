package com.zamaz.mcp.rag.domain.model.document;

import java.util.Objects;

/**
 * Value Object representing the content of a document chunk.
 * Enforces size constraints for vector embedding compatibility.
 */
public record ChunkContent(String text) {
    
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 2000; // Typical limit for embedding models
    private static final int MAX_TOKEN_ESTIMATE = 500; // Rough estimate: ~4 chars per token
    
    public ChunkContent {
        Objects.requireNonNull(text, "Chunk content text cannot be null");
        
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Chunk content cannot be empty");
        }
        
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Chunk content must be at least %d characters", MIN_LENGTH)
            );
        }
        
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Chunk content cannot exceed %d characters", MAX_LENGTH)
            );
        }
        
        text = trimmed;
    }
    
    /**
     * Factory method to create ChunkContent
     */
    public static ChunkContent of(String text) {
        return new ChunkContent(text);
    }
    
    /**
     * Get the length of the content
     */
    public int length() {
        return text.length();
    }
    
    /**
     * Get estimated token count (rough approximation)
     */
    public int estimatedTokenCount() {
        // Very rough estimate: average 4 characters per token
        return Math.max(1, text.length() / 4);
    }
    
    /**
     * Check if content is within token limits
     */
    public boolean isWithinTokenLimit() {
        return estimatedTokenCount() <= MAX_TOKEN_ESTIMATE;
    }
    
    /**
     * Get a preview of the content
     */
    public String preview(int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * Check if content contains search term (case-insensitive)
     */
    public boolean containsIgnoreCase(String searchTerm) {
        return text.toLowerCase().contains(searchTerm.toLowerCase());
    }
    
    /**
     * Get content with search term highlighted
     */
    public String highlightTerm(String searchTerm, String highlightPrefix, String highlightSuffix) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return text;
        }
        
        String lowerText = text.toLowerCase();
        String lowerSearch = searchTerm.toLowerCase();
        
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        int index = lowerText.indexOf(lowerSearch);
        
        while (index != -1) {
            result.append(text, lastIndex, index);
            result.append(highlightPrefix);
            result.append(text, index, index + searchTerm.length());
            result.append(highlightSuffix);
            lastIndex = index + searchTerm.length();
            index = lowerText.indexOf(lowerSearch, lastIndex);
        }
        
        result.append(text.substring(lastIndex));
        return result.toString();
    }
    
    @Override
    public String toString() {
        return text;
    }
}