package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.query.SearchDocumentsQuery;
import com.zamaz.mcp.rag.domain.model.ChunkContent;
import com.zamaz.mcp.rag.domain.model.DocumentChunk;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.domain.port.EmbeddingService;
import com.zamaz.mcp.rag.domain.port.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of SearchDocumentsUseCase.
 */
@Service
@Transactional(readOnly = true)
public class SearchDocumentsUseCaseImpl implements SearchDocumentsUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchDocumentsUseCaseImpl.class);
    
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    
    public SearchDocumentsUseCaseImpl(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = Objects.requireNonNull(embeddingService, "Embedding service cannot be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "Vector store cannot be null");
    }
    
    @Override
    public List<DocumentChunk> execute(SearchDocumentsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        logger.info("Searching documents for organization {} with query: '{}'", 
            query.organizationId(), query.searchText());
        
        try {
            // Generate embedding for the search query
            ChunkContent queryContent = ChunkContent.of(query.searchText());
            Embedding queryEmbedding = embeddingService.generateEmbedding(queryContent);
            
            // Search in vector store
            List<DocumentChunk> results = vectorStore.search(
                query.toDomainSearchQuery(),
                queryEmbedding
            );
            
            logger.info("Found {} relevant chunks for query", results.size());
            
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to search documents", e);
            throw new RuntimeException("Document search failed", e);
        }
    }
}