package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.query.ListDocumentsQuery;
import com.zamaz.mcp.rag.domain.model.Document;
import java.util.List;

/**
 * Use case for listing documents with filtering.
 */
public interface ListDocumentsUseCase {
    
    /**
     * List documents based on query criteria.
     * 
     * @param query the list documents query
     * @return list of documents matching the criteria
     */
    List<Document> execute(ListDocumentsQuery query);
}