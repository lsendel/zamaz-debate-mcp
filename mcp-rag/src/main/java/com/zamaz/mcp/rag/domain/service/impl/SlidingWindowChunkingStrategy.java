package com.zamaz.mcp.rag.domain.service.impl;

import com.zamaz.mcp.rag.domain.model.document.*;
import com.zamaz.mcp.rag.domain.service.ChunkingStrategy;
import com.zamaz.mcp.rag.domain.service.ChunkingParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of sliding window chunking strategy.
 * Splits text into overlapping chunks of fixed size.
 */
public class SlidingWindowChunkingStrategy implements ChunkingStrategy {
    
    private final ChunkingParameters parameters;
    
    public SlidingWindowChunkingStrategy(ChunkingParameters parameters) {
        this.parameters = parameters;
    }
    
    @Override
    public List<DocumentChunk> chunk(Document document) {
        String content = document.getContent().text();
        List<DocumentChunk> chunks = new ArrayList<>();
        
        int position = 0;
        int chunkIndex = 0;
        
        while (position < content.length()) {
            // Calculate chunk boundaries
            int endPosition = Math.min(position + parameters.maxChunkSize(), content.length());
            
            // Adjust for sentence boundaries if required
            if (parameters.preserveSentences() && endPosition < content.length()) {
                endPosition = findSentenceBoundary(content, position, endPosition);
            }
            
            // Extract chunk text
            String chunkText = content.substring(position, endPosition).trim();
            
            // Skip if chunk is too small
            if (chunkText.length() >= parameters.minChunkSize()) {
                ChunkId chunkId = ChunkId.generate();
                ChunkContent chunkContent = ChunkContent.of(chunkText);
                
                // Create chunk metadata
                ChunkMetadata metadata = new ChunkMetadata(
                    position,
                    endPosition,
                    chunkContent.estimatedTokenCount(),
                    String.valueOf(chunkContent.hashCode())
                );
                
                DocumentChunk chunk = DocumentChunk.create(
                    document.getId(),
                    chunkContent,
                    chunkIndex++,
                    metadata
                );
                
                chunks.add(chunk);
            }
            
            // Move position with overlap
            position = endPosition - parameters.overlapSize();
            if (position <= 0 || position >= content.length() - parameters.minChunkSize()) {
                break;
            }
        }
        
        return chunks;
    }
    
    @Override
    public String getName() {
        return "sliding_window";
    }
    
    @Override
    public ChunkingParameters getParameters() {
        return parameters;
    }
    
    @Override
    public boolean canProcess(Document document) {
        return document != null && 
               document.getContent() != null && 
               document.getContent().length() > 0;
    }
    
    private int findSentenceBoundary(String content, int start, int preferredEnd) {
        // Look for sentence endings
        String[] sentenceEndings = {".", "!", "?", "\n\n"};
        
        int bestBoundary = preferredEnd;
        int minDistance = Integer.MAX_VALUE;
        
        for (String ending : sentenceEndings) {
            int lastIndex = content.lastIndexOf(ending, preferredEnd);
            if (lastIndex > start) {
                int distance = preferredEnd - lastIndex;
                if (distance < minDistance) {
                    minDistance = distance;
                    bestBoundary = lastIndex + ending.length();
                }
            }
        }
        
        // If no sentence boundary found nearby, look forward
        if (bestBoundary == preferredEnd) {
            for (String ending : sentenceEndings) {
                int nextIndex = content.indexOf(ending, preferredEnd);
                if (nextIndex != -1 && nextIndex < preferredEnd + 100) { // Within 100 chars
                    bestBoundary = nextIndex + ending.length();
                    break;
                }
            }
        }
        
        return Math.min(bestBoundary, content.length());
    }
}