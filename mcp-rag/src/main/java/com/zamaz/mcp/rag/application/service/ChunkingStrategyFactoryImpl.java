package com.zamaz.mcp.rag.application.service;

import com.zamaz.mcp.rag.domain.service.ChunkingParameters;
import com.zamaz.mcp.rag.domain.service.ChunkingStrategy;
import com.zamaz.mcp.rag.domain.service.impl.ParagraphChunkingStrategy;
import com.zamaz.mcp.rag.domain.service.impl.SlidingWindowChunkingStrategy;
import org.springframework.stereotype.Component;

/**
 * Factory implementation for creating chunking strategies.
 * Maps strategy names to concrete implementations.
 */
@Component
public class ChunkingStrategyFactoryImpl implements ProcessDocumentService.ChunkingStrategyFactory {
    
    @Override
    public ChunkingStrategy createStrategy(String name, int maxSize, int overlap, boolean preserveSentences) {
        ChunkingParameters parameters = new ChunkingParameters(
            maxSize,
            overlap,
            Math.min(50, maxSize / 10), // Min size is 10% of max or 50
            preserveSentences,
            "paragraph".equals(name) // Preserve paragraphs for paragraph strategy
        );
        
        switch (name.toLowerCase()) {
            case "sliding_window":
                return new SlidingWindowChunkingStrategy(parameters);
            
            case "paragraph":
                return new ParagraphChunkingStrategy(parameters);
            
            case "semantic":
                // For now, use sliding window with larger overlap
                ChunkingParameters semanticParams = new ChunkingParameters(
                    maxSize,
                    Math.min(overlap * 2, maxSize / 2), // Double overlap for semantic
                    Math.min(100, maxSize / 8),
                    true,
                    true
                );
                return new SlidingWindowChunkingStrategy(semanticParams);
            
            default:
                throw new IllegalArgumentException("Unknown chunking strategy: " + name);
        }
    }
}