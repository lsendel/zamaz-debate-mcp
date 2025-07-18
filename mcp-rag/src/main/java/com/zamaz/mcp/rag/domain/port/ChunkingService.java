package com.zamaz.mcp.rag.domain.port;

import com.zamaz.mcp.rag.domain.model.ChunkContent;
import com.zamaz.mcp.rag.domain.model.DocumentContent;
import java.util.List;

/**
 * Port for document chunking service.
 */
public interface ChunkingService {
    
    /**
     * Chunk a document content into smaller pieces.
     * 
     * @param content the document content to chunk
     * @param chunkSize the target size for each chunk (in characters)
     * @param chunkOverlap the overlap between chunks (in characters)
     * @return list of chunked content
     */
    List<ChunkContent> chunkDocument(DocumentContent content, int chunkSize, int chunkOverlap);
    
    /**
     * Chunk a document with default settings.
     * 
     * @param content the document content to chunk
     * @return list of chunked content
     */
    default List<ChunkContent> chunkDocument(DocumentContent content) {
        return chunkDocument(content, 1000, 200); // Default: 1000 chars with 200 overlap
    }
}