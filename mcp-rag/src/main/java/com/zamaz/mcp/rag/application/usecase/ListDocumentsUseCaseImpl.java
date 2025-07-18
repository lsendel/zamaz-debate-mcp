package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.query.ListDocumentsQuery;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.port.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of ListDocumentsUseCase.
 */
@Service
@Transactional(readOnly = true)
public class ListDocumentsUseCaseImpl implements ListDocumentsUseCase {
    
    private final DocumentRepository documentRepository;
    
    public ListDocumentsUseCaseImpl(DocumentRepository documentRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository cannot be null");
    }
    
    @Override
    public List<Document> execute(ListDocumentsQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        return documentRepository.findByFilters(
            query.organizationId(),
            query.statuses(),
            query.getTitleFilter().orElse(null),
            query.getLimit().orElse(null),
            query.getOffset().orElse(null)
        );
    }
}