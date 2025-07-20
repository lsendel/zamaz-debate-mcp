package com.zamaz.mcp.rag.application.service;

import com.zamaz.mcp.rag.application.port.in.CreateDocumentUseCase;
import com.zamaz.mcp.rag.application.port.out.DocumentParser;
import com.zamaz.mcp.rag.application.port.out.DocumentRepository;
import com.zamaz.mcp.rag.application.port.out.EventPublisher;
import com.zamaz.mcp.rag.domain.model.FileInfo;
import com.zamaz.mcp.rag.domain.model.document.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Application service implementing the create document use case.
 * Orchestrates document creation, parsing, and persistence.
 */
@Service
@Transactional
public class CreateDocumentService implements CreateDocumentUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(CreateDocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final DocumentParser documentParser;
    private final EventPublisher eventPublisher;
    
    public CreateDocumentService(
            DocumentRepository documentRepository,
            DocumentParser documentParser,
            EventPublisher eventPublisher) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "DocumentRepository cannot be null");
        this.documentParser = Objects.requireNonNull(documentParser, "DocumentParser cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "EventPublisher cannot be null");
    }
    
    @Override
    public DocumentId createDocument(CreateDocumentCommand command) {
        log.info("Creating document: {} for organization: {}", 
                command.fileName(), command.organizationId());
        
        // Validate file type is supported
        if (!documentParser.isSupported(command.contentType())) {
            throw new UnsupportedDocumentTypeException(
                "Unsupported document type: " + command.contentType()
            );
        }
        
        // Parse the document content
        DocumentParser.ParseResult parseResult = documentParser.parse(
            command.content(),
            command.contentType(),
            command.fileName()
        );
        
        if (!parseResult.success()) {
            throw new DocumentParsingException(
                "Failed to parse document: " + parseResult.errorMessage()
            );
        }
        
        // Create document metadata
        DocumentMetadata metadata = DocumentMetadata.builder()
                .property("fileName", command.fileName())
                .property("fileType", command.contentType())
                .property("fileSize", String.valueOf(command.getFileSize()))
                .property("source", command.source())
                .properties(parseResult.metadata())
                .build();
        
        // Create the document aggregate
        Document document = Document.create(
            DocumentName.of(command.fileName()),
            DocumentContent.of(parseResult.text()),
            metadata,
            command.organizationId()
        );
        
        // Save the document
        Document savedDocument = documentRepository.save(document);
        
        // Publish domain events
        eventPublisher.publishAll(savedDocument.getDomainEvents());
        savedDocument.clearDomainEvents();
        
        log.info("Document created successfully with ID: {}", savedDocument.getId());
        
        return savedDocument.getId();
    }
    
    /**
     * Exception for unsupported document types
     */
    public static class UnsupportedDocumentTypeException extends RuntimeException {
        public UnsupportedDocumentTypeException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception for document parsing failures
     */
    public static class DocumentParsingException extends RuntimeException {
        public DocumentParsingException(String message) {
            super(message);
        }
    }
}