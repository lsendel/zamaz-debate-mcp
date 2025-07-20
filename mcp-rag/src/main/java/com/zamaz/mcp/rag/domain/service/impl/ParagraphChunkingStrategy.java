package com.zamaz.mcp.rag.domain.service.impl;

import com.zamaz.mcp.rag.domain.model.document.*;
import com.zamaz.mcp.rag.domain.service.ChunkingStrategy;
import com.zamaz.mcp.rag.domain.service.ChunkingParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of paragraph-based chunking strategy.
 * Splits text into chunks based on paragraph boundaries.
 */
public class ParagraphChunkingStrategy implements ChunkingStrategy {
    
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");
    private final ChunkingParameters parameters;
    
    public ParagraphChunkingStrategy(ChunkingParameters parameters) {
        this.parameters = parameters;
    }
    
    @Override
    public List<DocumentChunk> chunk(Document document) {
        String content = document.getContent().text();
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // Split by paragraphs
        String[] paragraphs = PARAGRAPH_PATTERN.split(content);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            
            // Check if adding this paragraph would exceed max size
            if (currentChunk.length() > 0 && 
                currentChunk.length() + paragraph.length() + 2 > parameters.maxChunkSize()) {
                
                // Save current chunk
                createAndAddChunk(currentChunk.toString(), chunkIndex++, document.getId(), chunks);
                currentChunk = new StringBuilder();
            }
            
            // Add paragraph to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
            
            // If paragraph itself is too large, split it
            if (currentChunk.length() > parameters.maxChunkSize()) {
                String chunkText = currentChunk.toString();
                List<String> splitParagraphs = splitLargeParagraph(chunkText);
                
                for (String split : splitParagraphs) {
                    createAndAddChunk(split, chunkIndex++, document.getId(), chunks);
                }
                currentChunk = new StringBuilder();
            }
        }
        
        // Add any remaining content
        if (currentChunk.length() >= parameters.minChunkSize()) {
            createAndAddChunk(currentChunk.toString(), chunkIndex++, document.getId(), chunks);
        }
        
        return chunks;
    }
    
    @Override
    public String getName() {
        return "paragraph";
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
    
    private void createAndAddChunk(String text, int index, DocumentId documentId, List<DocumentChunk> chunks) {
        text = text.trim();
        if (text.length() >= parameters.minChunkSize()) {
            ChunkId chunkId = ChunkId.generate();
            ChunkContent chunkContent = ChunkContent.of(text);
            
            // Create chunk metadata
            ChunkMetadata metadata = new ChunkMetadata(
                0, // We don't track exact positions in paragraph chunking
                text.length(),
                chunkContent.estimatedTokenCount(),
                String.valueOf(chunkContent.hashCode())
            );
            
            DocumentChunk chunk = DocumentChunk.create(
                documentId,
                chunkContent,
                index,
                metadata
            );
            
            chunks.add(chunk);
        }
    }
    
    private List<String> splitLargeParagraph(String paragraph) {
        List<String> splits = new ArrayList<>();
        
        if (parameters.preserveSentences()) {
            // Split by sentences
            String[] sentences = paragraph.split("(?<=[.!?])\\s+");
            StringBuilder currentSplit = new StringBuilder();
            
            for (String sentence : sentences) {
                if (currentSplit.length() + sentence.length() + 1 > parameters.maxChunkSize()) {
                    if (currentSplit.length() > 0) {
                        splits.add(currentSplit.toString());
                        currentSplit = new StringBuilder();
                    }
                }
                
                if (currentSplit.length() > 0) {
                    currentSplit.append(" ");
                }
                currentSplit.append(sentence);
            }
            
            if (currentSplit.length() > 0) {
                splits.add(currentSplit.toString());
            }
        } else {
            // Hard split at max size
            int pos = 0;
            while (pos < paragraph.length()) {
                int end = Math.min(pos + parameters.maxChunkSize(), paragraph.length());
                splits.add(paragraph.substring(pos, end));
                pos = end;
            }
        }
        
        return splits;
    }
}