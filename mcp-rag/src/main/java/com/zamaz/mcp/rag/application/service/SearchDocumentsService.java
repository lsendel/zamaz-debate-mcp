package com.zamaz.mcp.rag.application.service;

import com.zamaz.mcp.rag.application.port.in.SearchDocumentsUseCase;
import com.zamaz.mcp.rag.application.port.out.DocumentRepository;
import com.zamaz.mcp.rag.application.port.out.EmbeddingService;
import com.zamaz.mcp.rag.application.port.out.VectorStore;
import com.zamaz.mcp.rag.domain.model.document.*;
import com.zamaz.mcp.rag.domain.model.embedding.EmbeddingVector;
import com.zamaz.mcp.rag.domain.model.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service implementing the search documents use case.
 * Orchestrates vector similarity search across documents.
 */
@Service
@Transactional(readOnly = true)
public class SearchDocumentsService implements SearchDocumentsUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(SearchDocumentsService.class);
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-ada-002";
    
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    
    public SearchDocumentsService(
            DocumentRepository documentRepository,
            EmbeddingService embeddingService,
            VectorStore vectorStore) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.embeddingService = Objects.requireNonNull(embeddingService);
        this.vectorStore = Objects.requireNonNull(vectorStore);
    }
    
    @Override
    @Cacheable(value = "searchResults", key = "#query.hashCode()")
    public List<SearchResult> search(SearchQuery query) {
        log.info("Searching documents for query: '{}' in organization: {}", 
                query.queryText(), query.organizationId());
        
        // Generate embedding for the query
        EmbeddingVector queryEmbedding = embeddingService.generateEmbedding(
            query.queryText(),
            DEFAULT_EMBEDDING_MODEL
        );
        
        // Create vector search query
        VectorStore.VectorSearchQuery vectorQuery = new VectorStore.VectorSearchQuery(
            queryEmbedding,
            query.organizationId(),
            query.topK(),
            query.minScore(),
            Set.of(), // No specific document filtering
            createMetadataFilters(query.tags())
        );
        
        // Search in vector store
        List<VectorStore.SimilarityResult> similarityResults = vectorStore.search(vectorQuery);
        
        // Convert to search results
        List<SearchResult> searchResults = new ArrayList<>();
        
        // Group by document to fetch efficiently
        Map<DocumentId, List<VectorStore.SimilarityResult>> resultsByDocument = 
            similarityResults.stream()
                .collect(Collectors.groupingBy(VectorStore.SimilarityResult::documentId));
        
        // Fetch documents and build results
        for (Map.Entry<DocumentId, List<VectorStore.SimilarityResult>> entry : resultsByDocument.entrySet()) {
            DocumentId documentId = entry.getKey();
            List<VectorStore.SimilarityResult> docResults = entry.getValue();
            
            documentRepository.findByIdAndOrganization(documentId, query.organizationId())
                .ifPresent(document -> {
                    for (VectorStore.SimilarityResult simResult : docResults) {
                        SearchResult searchResult = createSearchResult(
                            document,
                            simResult,
                            query.includeContent()
                        );
                        
                        if (searchResult != null) {
                            searchResults.add(searchResult);
                        }
                    }
                });
        }
        
        // Sort by relevance score (descending)
        searchResults.sort((a, b) -> Double.compare(b.relevanceScore(), a.relevanceScore()));
        
        // Limit to topK
        if (searchResults.size() > query.topK()) {
            searchResults = searchResults.subList(0, query.topK());
        }
        
        log.info("Found {} search results", searchResults.size());
        
        return searchResults;
    }
    
    private Map<String, String> createMetadataFilters(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        
        // Convert tags to metadata filter
        // Assuming tags are stored as comma-separated in metadata
        return Map.of("tags", String.join(",", tags));
    }
    
    private SearchResult createSearchResult(
            Document document,
            VectorStore.SimilarityResult simResult,
            boolean includeContent) {
        
        // Find the matching chunk
        Optional<DocumentChunk> chunkOpt = document.getChunks().stream()
                .filter(chunk -> chunk.getId().equals(simResult.chunkId()))
                .findFirst();
        
        if (chunkOpt.isEmpty()) {
            log.warn("Chunk not found: {} in document: {}", simResult.chunkId(), document.getId());
            return null;
        }
        
        DocumentChunk chunk = chunkOpt.get();
        
        // Create chunk content based on includeContent flag
        ChunkContent content = includeContent ? 
            chunk.getContent() : 
            ChunkContent.of("[Content hidden]");
        
        return SearchResult.fromSimilarity(
            document.getId(),
            chunk.getId(),
            content,
            simResult.score(),
            document.getName().value(),
            chunk.getSequenceNumber()
        );
    }
}