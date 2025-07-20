package com.zamaz.mcp.rag.adapter.external;

import com.zamaz.mcp.rag.domain.model.ChunkContent;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentContent;
import com.zamaz.mcp.rag.domain.port.DocumentProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * External adapter for document processing and chunking.
 */
@Service
public class DocumentProcessingServiceAdapter implements DocumentProcessingService {
    
    @Value("${rag.chunking.strategy:sliding-window}")
    private String chunkingStrategy;
    
    @Value("${rag.chunking.size:512}")
    private int chunkSize;
    
    @Value("${rag.chunking.overlap:128}")
    private int chunkOverlap;
    
    @Value("${rag.chunking.min-size:50}")
    private int minChunkSize;
    
    @Override
    public List<ChunkContent> chunkDocument(Document document) {
        DocumentContent content = document.getContent();
        String text = content.text();
        
        return switch (chunkingStrategy) {
            case "sliding-window" -> slidingWindowChunking(text);
            case "sentence" -> sentenceBasedChunking(text);
            case "paragraph" -> paragraphBasedChunking(text);
            default -> slidingWindowChunking(text);
        };
    }
    
    /**
     * Sliding window chunking strategy.
     */
    private List<ChunkContent> slidingWindowChunking(String text) {
        List<ChunkContent> chunks = new ArrayList<>();
        
        if (text == null || text.isBlank()) {
            return chunks;
        }
        
        // Clean and normalize text
        String cleanText = text.trim().replaceAll("\\s+", " ");
        
        int start = 0;
        while (start < cleanText.length()) {
            // Calculate end position
            int end = Math.min(start + chunkSize, cleanText.length());
            
            // Try to break at word boundary
            if (end < cleanText.length() && !Character.isWhitespace(cleanText.charAt(end))) {
                int wordBoundary = cleanText.lastIndexOf(' ', end);
                if (wordBoundary > start) {
                    end = wordBoundary;
                }
            }
            
            // Extract chunk
            String chunkText = cleanText.substring(start, end).trim();
            
            // Only add chunks that meet minimum size
            if (chunkText.length() >= minChunkSize) {
                chunks.add(ChunkContent.of(chunkText, start, end));
            }
            
            // Move to next chunk with overlap
            start = end - chunkOverlap;
            if (start <= 0) {
                start = end;
            }
        }
        
        return chunks;
    }
    
    /**
     * Sentence-based chunking strategy.
     */
    private List<ChunkContent> sentenceBasedChunking(String text) {
        List<ChunkContent> chunks = new ArrayList<>();
        
        if (text == null || text.isBlank()) {
            return chunks;
        }
        
        // Split by sentence endings
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkStart = 0;
        int currentPos = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                // Save current chunk
                chunks.add(ChunkContent.of(
                    currentChunk.toString().trim(),
                    chunkStart,
                    currentPos
                ));
                
                // Start new chunk
                currentChunk = new StringBuilder();
                chunkStart = currentPos;
            }
            
            currentChunk.append(sentence).append(" ");
            currentPos += sentence.length() + 1;
        }
        
        // Add final chunk
        if (currentChunk.length() >= minChunkSize) {
            chunks.add(ChunkContent.of(
                currentChunk.toString().trim(),
                chunkStart,
                currentPos
            ));
        }
        
        return chunks;
    }
    
    /**
     * Paragraph-based chunking strategy.
     */
    private List<ChunkContent> paragraphBasedChunking(String text) {
        List<ChunkContent> chunks = new ArrayList<>();
        
        if (text == null || text.isBlank()) {
            return chunks;
        }
        
        // Split by double newlines (paragraphs)
        String[] paragraphs = text.split("\\n\\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkStart = 0;
        int currentPos = 0;
        
        for (String paragraph : paragraphs) {
            String trimmedPara = paragraph.trim();
            
            if (currentChunk.length() + trimmedPara.length() > chunkSize && currentChunk.length() > 0) {
                // Save current chunk
                chunks.add(ChunkContent.of(
                    currentChunk.toString().trim(),
                    chunkStart,
                    currentPos
                ));
                
                // Start new chunk
                currentChunk = new StringBuilder();
                chunkStart = currentPos;
            }
            
            currentChunk.append(trimmedPara).append("\n\n");
            currentPos += paragraph.length() + 2;
        }
        
        // Add final chunk
        if (currentChunk.length() >= minChunkSize) {
            chunks.add(ChunkContent.of(
                currentChunk.toString().trim(),
                chunkStart,
                currentPos
            ));
        }
        
        return chunks;
    }
    
    @Override
    public String extractText(byte[] fileContent, String mimeType) {
        // This is a simplified implementation
        // In production, would use Apache Tika or similar for proper text extraction
        
        if (mimeType == null) {
            return new String(fileContent);
        }
        
        return switch (mimeType) {
            case "text/plain", "text/markdown", "text/csv" -> new String(fileContent);
            case "application/json", "application/xml" -> new String(fileContent);
            default -> {
                // For now, just convert to string
                // In production, would handle PDFs, DOCX, etc.
                yield new String(fileContent);
            }
        };
    }
}