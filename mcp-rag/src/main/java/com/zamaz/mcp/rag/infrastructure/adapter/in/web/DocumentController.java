package com.zamaz.mcp.rag.infrastructure.adapter.in.web;

import com.zamaz.mcp.rag.application.port.in.CreateDocumentUseCase;
import com.zamaz.mcp.rag.application.port.in.GetDocumentUseCase;
import com.zamaz.mcp.rag.application.port.in.ProcessDocumentUseCase;
import com.zamaz.mcp.rag.domain.model.document.DocumentId;
import com.zamaz.mcp.rag.domain.model.document.OrganizationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

/**
 * REST controller for document management operations.
 * Inbound adapter implementing the web interface for document use cases.
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    
    private final CreateDocumentUseCase createDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final ProcessDocumentUseCase processDocumentUseCase;
    
    public DocumentController(
            CreateDocumentUseCase createDocumentUseCase,
            GetDocumentUseCase getDocumentUseCase,
            ProcessDocumentUseCase processDocumentUseCase) {
        this.createDocumentUseCase = Objects.requireNonNull(createDocumentUseCase);
        this.getDocumentUseCase = Objects.requireNonNull(getDocumentUseCase);
        this.processDocumentUseCase = Objects.requireNonNull(processDocumentUseCase);
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new document")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("organizationId") String organizationId,
            @RequestParam(value = "source", defaultValue = "api_upload") String source) {
        
        log.info("Uploading document: {} for organization: {}", file.getOriginalFilename(), organizationId);
        
        try {
            // Create command
            CreateDocumentUseCase.CreateDocumentCommand command = new CreateDocumentUseCase.CreateDocumentCommand(
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType(),
                OrganizationId.of(organizationId),
                source
            );
            
            // Execute use case
            DocumentId documentId = createDocumentUseCase.createDocument(command);
            
            // Create response
            DocumentUploadResponse response = new DocumentUploadResponse(
                documentId.value(),
                file.getOriginalFilename(),
                file.getSize(),
                "Document uploaded successfully"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IOException e) {
            log.error("Failed to read file content", e);
            throw new DocumentUploadException("Failed to read file content: " + e.getMessage());
        }
    }
    
    @GetMapping("/{documentId}")
    @Operation(summary = "Get document details")
    public ResponseEntity<GetDocumentUseCase.DocumentDto> getDocument(
            @PathVariable String documentId,
            @RequestParam("organizationId") String organizationId,
            @RequestParam(value = "includeChunks", defaultValue = "false") boolean includeChunks,
            @RequestParam(value = "includeHistory", defaultValue = "false") boolean includeHistory) {
        
        log.info("Getting document: {} for organization: {}", documentId, organizationId);
        
        GetDocumentUseCase.GetDocumentQuery query = new GetDocumentUseCase.GetDocumentQuery(
            DocumentId.of(documentId),
            OrganizationId.of(organizationId),
            includeChunks,
            includeHistory
        );
        
        GetDocumentUseCase.DocumentDto document = getDocumentUseCase.getDocument(query);
        
        return ResponseEntity.ok(document);
    }
    
    @PostMapping("/{documentId}/process")
    @Operation(summary = "Process a document (chunk and generate embeddings)")
    public ResponseEntity<ProcessingResponse> processDocument(
            @PathVariable String documentId,
            @RequestBody ProcessingRequest request) {
        
        log.info("Processing document: {} with strategy: {}", documentId, request.chunkingStrategy());
        
        // Create chunking configuration
        ProcessDocumentUseCase.ChunkingConfiguration chunkingConfig = new ProcessDocumentUseCase.ChunkingConfiguration(
            request.chunkingStrategy(),
            request.maxChunkSize(),
            request.overlapSize(),
            request.preserveSentences()
        );
        
        // Create embedding configuration
        ProcessDocumentUseCase.EmbeddingConfiguration embeddingConfig = new ProcessDocumentUseCase.EmbeddingConfiguration(
            request.embeddingModel(),
            request.batchSize(),
            request.useCache()
        );
        
        // Create command
        ProcessDocumentUseCase.ProcessDocumentCommand command = new ProcessDocumentUseCase.ProcessDocumentCommand(
            DocumentId.of(documentId),
            chunkingConfig,
            embeddingConfig
        );
        
        // Execute use case (async)
        processDocumentUseCase.processDocument(command);
        
        // Return accepted response
        ProcessingResponse response = new ProcessingResponse(
            documentId,
            "Document processing started",
            "PROCESSING"
        );
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Request DTO for document processing
     */
    record ProcessingRequest(
        String chunkingStrategy,
        int maxChunkSize,
        int overlapSize,
        boolean preserveSentences,
        String embeddingModel,
        int batchSize,
        boolean useCache
    ) {
        public ProcessingRequest {
            // Provide defaults
            if (chunkingStrategy == null) chunkingStrategy = "sliding_window";
            if (maxChunkSize <= 0) maxChunkSize = 512;
            if (overlapSize < 0) overlapSize = 128;
            if (embeddingModel == null) embeddingModel = "text-embedding-ada-002";
            if (batchSize <= 0) batchSize = 100;
        }
    }
    
    /**
     * Response DTO for document upload
     */
    record DocumentUploadResponse(
        String documentId,
        String fileName,
        long fileSize,
        String message
    ) {}
    
    /**
     * Response DTO for processing request
     */
    record ProcessingResponse(
        String documentId,
        String message,
        String status
    ) {}
    
    /**
     * Exception for upload failures
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class DocumentUploadException extends RuntimeException {
        public DocumentUploadException(String message) {
            super(message);
        }
    }
}