package com.zamaz.mcp.rag.service.impl;

import com.zamaz.mcp.rag.entity.DocumentChunk;
import com.zamaz.mcp.rag.service.ChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ChunkingService for document chunking.
 */
@Slf4j
@Service
public class ChunkingServiceImpl implements ChunkingService {
    
    private final int defaultChunkSize;
    private final int chunkOverlap;
    private final int maxChunkSize;
    
    public ChunkingServiceImpl(@Value("${rag.chunking.default-size:1000}") int defaultChunkSize,
                              @Value("${rag.chunking.overlap:200}") int chunkOverlap,
                              @Value("${rag.chunking.max-size:2000}") int maxChunkSize) {
        this.defaultChunkSize = defaultChunkSize;
        this.chunkOverlap = chunkOverlap;
        this.maxChunkSize = maxChunkSize;
    }
    
    @Override
    public List<DocumentChunk> chunkDocument(String content, String documentId, String organizationId) {
        log.info("Chunking document {} with content length: {}", documentId, content.length());
        
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }
        
        ChunkingContext context = new ChunkingContext(organizationId, content.trim());
        String[] paragraphs = content.split("\n\n+");
        
        for (String paragraph : paragraphs) {
            processParagraph(paragraph.trim(), context);
        }
        
        // Add any remaining content
        finalizeChunking(context);
        
        log.info("Document {} chunked into {} chunks", documentId, context.chunks.size());
        return context.chunks;
    }
    
    private void processParagraph(String paragraph, ChunkingContext context) {
        if (paragraph.isEmpty()) {
            return;
        }
        
        if (shouldStartNewChunk(context.currentChunk, paragraph)) {
            saveCurrentChunk(context);
        }
        
        appendParagraphToChunk(context.currentChunk, paragraph);
        
        if (context.currentChunk.length() > maxChunkSize) {
            handleOversizedChunk(context);
        }
    }
    
    private boolean shouldStartNewChunk(StringBuilder currentChunk, String paragraph) {
        return currentChunk.length() > 0 && 
               currentChunk.length() + paragraph.length() + 2 > defaultChunkSize;
    }
    
    private void appendParagraphToChunk(StringBuilder currentChunk, String paragraph) {
        if (currentChunk.length() > 0) {
            currentChunk.append("\n\n");
        }
        currentChunk.append(paragraph);
    }
    
    private void saveCurrentChunk(ChunkingContext context) {
        String chunkContent = context.currentChunk.toString().trim();
        if (chunkContent.isEmpty()) {
            context.currentChunk = new StringBuilder();
            return;
        }
        
        DocumentChunk chunk = createChunk(
            context.organizationId,
            context.chunkIndex++,
            chunkContent,
            context.currentPosition,
            context.currentPosition + chunkContent.length()
        );
        
        context.chunks.add(chunk);
        
        // Prepare for next chunk with overlap
        String overlap = getOverlapText(chunkContent, chunkOverlap);
        context.currentChunk = new StringBuilder(overlap);
        context.currentPosition += chunkContent.length() - overlap.length();
    }
    
    private void handleOversizedChunk(ChunkingContext context) {
        String longText = context.currentChunk.toString();
        List<String> splitChunks = splitLongText(longText, defaultChunkSize, chunkOverlap);
        
        for (String splitChunk : splitChunks) {
            DocumentChunk chunk = createChunk(
                context.organizationId,
                context.chunkIndex++,
                splitChunk,
                context.currentPosition,
                context.currentPosition + splitChunk.length()
            );
            
            context.chunks.add(chunk);
            context.currentPosition += splitChunk.length() - chunkOverlap;
        }
        
        context.currentChunk = new StringBuilder();
    }
    
    private void finalizeChunking(ChunkingContext context) {
        if (context.currentChunk.length() > 0) {
            String chunkContent = context.currentChunk.toString().trim();
            if (!chunkContent.isEmpty()) {
                DocumentChunk chunk = createChunk(
                    context.organizationId,
                    context.chunkIndex,
                    chunkContent,
                    context.currentPosition,
                    context.currentPosition + chunkContent.length()
                );
                context.chunks.add(chunk);
            }
        }
    }
    
    private DocumentChunk createChunk(String organizationId, int chunkIndex, 
                                     String content, int startPos, int endPos) {
        return DocumentChunk.builder()
                .organizationId(organizationId)
                .chunkIndex(chunkIndex)
                .content(content)
                .startPosition(startPos)
                .endPosition(endPos)
                .tokenCount(estimateTokenCount(content))
                .build();
    }
    
    @Override
    public int getOptimalChunkSize(String contentType) {
        if (contentType == null) {
            return defaultChunkSize;
        }
        
        switch (contentType.toLowerCase()) {
            case "code":
            case "application/x-code":
                return 800; // Smaller chunks for code
            case "markdown":
            case "text/markdown":
                return 1200; // Slightly larger for markdown
            case "technical":
            case "documentation":
                return 1500; // Larger chunks for technical docs
            default:
                return defaultChunkSize;
        }
    }
    
    @Override
    public int getChunkOverlap() {
        return chunkOverlap;
    }
    
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        
        // Try to find a sentence boundary for overlap
        int startPos = Math.max(0, text.length() - overlapSize);
        int sentenceEnd = text.lastIndexOf(". ", startPos);
        
        if (sentenceEnd > startPos / 2) {
            return text.substring(sentenceEnd + 2);
        }
        
        return text.substring(startPos);
    }
    
    private List<String> splitLongText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            
            // Try to find a sentence boundary
            if (end < text.length()) {
                int sentenceEnd = text.lastIndexOf(". ", end);
                if (sentenceEnd > start + chunkSize / 2) {
                    end = sentenceEnd + 2;
                }
            }
            
            chunks.add(text.substring(start, end));
            
            // Move start position with overlap
            start = end - overlap;
            if (start >= text.length() - overlap) {
                break;
            }
        }
        
        return chunks;
    }
    
    private int estimateTokenCount(String text) {
        // Rough estimation: 1 token ≈ 4 characters for English text
        return text.length() / 4;
    }
    
    /**
     * Internal class to hold chunking context/state.
     */
    private static class ChunkingContext {
        final String organizationId;
        final List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int currentPosition = 0;
        
        ChunkingContext(String organizationId, String content) {
            this.organizationId = organizationId;
        }
    }
}