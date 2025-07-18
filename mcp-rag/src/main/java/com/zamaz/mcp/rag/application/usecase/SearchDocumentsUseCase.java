package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.query.SearchDocumentsQuery;
import com.zamaz.mcp.rag.domain.model.DocumentChunk;
import java.util.List;

/**
 * Use case for searching documents semantically.
 */
public interface SearchDocumentsUseCase {
    
    /**
     * Search for relevant document chunks based on semantic similarity.
     * 
     * @param query the search query
     * @return list of relevant chunks ordered by similarity
     */
    List<DocumentChunk> execute(SearchDocumentsQuery query);
}