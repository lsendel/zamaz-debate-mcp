package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.query.GetDocumentQuery;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.port.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of GetDocumentUseCase.
 */
@Service
@Transactional(readOnly = true)
public class GetDocumentUseCaseImpl implements GetDocumentUseCase {
    
    private final DocumentRepository documentRepository;
    
    public GetDocumentUseCaseImpl(DocumentRepository documentRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository cannot be null");
    }
    
    @Override
    public Optional<Document> execute(GetDocumentQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        
        return documentRepository.findByIdAndOrganization(
            query.documentId(),
            query.organizationId()
        );
    }
}