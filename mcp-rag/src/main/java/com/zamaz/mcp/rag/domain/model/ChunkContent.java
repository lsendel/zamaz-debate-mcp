package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing the content of a document chunk.
 */
public record ChunkContent(String value) implements ValueObject {
    
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 8192; // 8KB per chunk
    
    public ChunkContent {
        Objects.requireNonNull(value, "Chunk content cannot be null");
        
        if (value.trim().length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Chunk content must be at least " + MIN_LENGTH + " characters"
            );
        }
        
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Chunk content cannot exceed " + MAX_LENGTH + " characters"
            );
        }
    }
    
    public static ChunkContent of(String value) {
        return new ChunkContent(value.trim());
    }
    
    public int length() {
        return value.length();
    }
    
    public int wordCount() {
        return value.trim().split("\\s+").length;
    }
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
    
    public boolean isNotEmpty() {
        return !isEmpty();
    }
    
    public String getPreview(int maxChars) {
        if (maxChars <= 0) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }
    
    public boolean contains(String substring) {
        Objects.requireNonNull(substring, "Substring cannot be null");
        return value.toLowerCase().contains(substring.toLowerCase());
    }
    
    /**
     * Calculate similarity to another chunk content (simple Jaccard similarity).
     */
    public double calculateSimilarity(ChunkContent other) {
        Objects.requireNonNull(other, "Other chunk content cannot be null");
        
        String[] words1 = this.value.toLowerCase().split("\\s+");
        String[] words2 = other.value.toLowerCase().split("\\s+");
        
        java.util.Set<String> set1 = java.util.Set.of(words1);
        java.util.Set<String> set2 = java.util.Set.of(words2);
        
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    @Override
    public String toString() {
        return getPreview(100);
    }
}