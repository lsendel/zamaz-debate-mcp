package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.command.UploadDocumentCommand;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentContent;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.FileInfo;
import com.zamaz.mcp.rag.domain.port.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * Implementation of UploadDocumentUseCase.
 */
@Service
@Transactional
public class UploadDocumentUseCaseImpl implements UploadDocumentUseCase {
    
    private final DocumentRepository documentRepository;
    
    public UploadDocumentUseCaseImpl(DocumentRepository documentRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository cannot be null");
    }
    
    @Override
    public DocumentId execute(UploadDocumentCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        // Generate new document ID
        DocumentId documentId = DocumentId.generate();
        
        // Create file info
        FileInfo fileInfo = FileInfo.of(
            command.fileName(),
            command.fileType(),
            command.fileSize()
        );
        
        // Validate file type is supported
        if (!fileInfo.isSupported()) {
            throw new IllegalArgumentException(
                "Unsupported file type: " + fileInfo.fileType() + 
                ". Supported types: PDF, Word, TXT, Markdown"
            );
        }
        
        // Create document content
        DocumentContent content = DocumentContent.of(command.getContentAsString());
        
        // Create the document
        Document document = Document.create(
            documentId,
            command.organizationId(),
            command.title(),
            fileInfo,
            content,
            command.metadata()
        );
        
        // Save the document
        documentRepository.save(document);
        
        return documentId;
    }
}