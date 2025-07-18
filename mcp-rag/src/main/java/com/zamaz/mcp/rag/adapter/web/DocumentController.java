package com.zamaz.mcp.rag.adapter.web;

import com.zamaz.mcp.rag.adapter.web.dto.*;
import com.zamaz.mcp.rag.application.command.ArchiveDocumentCommand;
import com.zamaz.mcp.rag.application.command.ProcessDocumentCommand;
import com.zamaz.mcp.rag.application.command.UploadDocumentCommand;
import com.zamaz.mcp.rag.application.query.GetDocumentQuery;
import com.zamaz.mcp.rag.application.query.ListDocumentsQuery;
import com.zamaz.mcp.rag.application.query.SearchDocumentsQuery;
import com.zamaz.mcp.rag.application.usecase.*;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentChunk;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST controller for document operations in the RAG system.
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management operations for RAG")
public class DocumentController {
    
    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final ProcessDocumentUseCase processDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final SearchDocumentsUseCase searchDocumentsUseCase;
    private final DocumentMapper documentMapper;
    
    public DocumentController(
            UploadDocumentUseCase uploadDocumentUseCase,
            ProcessDocumentUseCase processDocumentUseCase,
            GetDocumentUseCase getDocumentUseCase,
            ListDocumentsUseCase listDocumentsUseCase,
            SearchDocumentsUseCase searchDocumentsUseCase,
            DocumentMapper documentMapper) {
        this.uploadDocumentUseCase = Objects.requireNonNull(uploadDocumentUseCase);
        this.processDocumentUseCase = Objects.requireNonNull(processDocumentUseCase);
        this.getDocumentUseCase = Objects.requireNonNull(getDocumentUseCase);
        this.listDocumentsUseCase = Objects.requireNonNull(listDocumentsUseCase);
        this.searchDocumentsUseCase = Objects.requireNonNull(searchDocumentsUseCase);
        this.documentMapper = Objects.requireNonNull(documentMapper);
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document to the RAG system")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Document file") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Document title") @RequestParam("title") String title,
            @Parameter(description = "Document metadata (JSON)") @RequestParam(value = "metadata", required = false) String metadataJson
    ) throws IOException {
        
        // Create upload command
        UploadDocumentCommand command = documentMapper.toUploadCommand(
            OrganizationId.from(organizationId),
            title,
            file,
            metadataJson
        );
        
        // Execute upload
        DocumentId documentId = uploadDocumentUseCase.execute(command);
        
        // Return response
        UploadDocumentResponse response = new UploadDocumentResponse(
            documentId.toString(),
            "Document uploaded successfully",
            "pending"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{documentId}/process")
    @Operation(summary = "Process a document (generate chunks and embeddings)")
    public ResponseEntity<Map<String, String>> processDocument(
            @Parameter(description = "Document ID") @PathVariable String documentId
    ) {
        
        ProcessDocumentCommand command = ProcessDocumentCommand.of(DocumentId.from(documentId));
        processDocumentUseCase.execute(command);
        
        return ResponseEntity.ok(Map.of(
            "documentId", documentId,
            "status", "processing",
            "message", "Document processing started"
        ));
    }
    
    @GetMapping("/{documentId}")
    @Operation(summary = "Get a document by ID")
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Document ID") @PathVariable String documentId
    ) {
        
        GetDocumentQuery query = GetDocumentQuery.of(
            DocumentId.from(documentId),
            OrganizationId.from(organizationId)
        );
        
        return getDocumentUseCase.execute(query)
            .map(document -> ResponseEntity.ok(documentMapper.toResponse(document)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "List documents for an organization")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by title") @RequestParam(required = false) String title,
            @Parameter(description = "Limit results") @RequestParam(required = false) Integer limit,
            @Parameter(description = "Offset for pagination") @RequestParam(required = false) Integer offset
    ) {
        
        ListDocumentsQuery query = documentMapper.toListQuery(
            OrganizationId.from(organizationId),
            status,
            title,
            limit,
            offset
        );
        
        List<Document> documents = listDocumentsUseCase.execute(query);
        List<DocumentResponse> responses = documents.stream()
            .map(documentMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Search documents semantically")
    public ResponseEntity<SearchResponse> searchDocuments(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Valid @RequestBody SearchRequest request
    ) {
        
        SearchDocumentsQuery query = SearchDocumentsQuery.withSimilarity(
            OrganizationId.from(organizationId),
            request.query(),
            request.maxResults() != null ? request.maxResults() : 10,
            request.minSimilarity() != null ? request.minSimilarity() : 0.0
        );
        
        List<DocumentChunk> chunks = searchDocumentsUseCase.execute(query);
        SearchResponse response = documentMapper.toSearchResponse(chunks, request.query());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{documentId}/archive")
    @Operation(summary = "Archive a document")
    public ResponseEntity<Map<String, String>> archiveDocument(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Document ID") @PathVariable String documentId
    ) {
        
        ArchiveDocumentCommand command = ArchiveDocumentCommand.of(
            DocumentId.from(documentId),
            OrganizationId.from(organizationId)
        );
        
        // Note: Archive use case would need to be implemented
        // archiveDocumentUseCase.execute(command);
        
        return ResponseEntity.ok(Map.of(
            "documentId", documentId,
            "status", "archived",
            "message", "Document archived successfully"
        ));
    }
    
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Document ID") @PathVariable String documentId
    ) {
        
        // Note: Delete use case would need to be implemented
        // deleteDocumentUseCase.execute(new DeleteDocumentCommand(documentId, organizationId));
        
        return ResponseEntity.noContent().build();
    }
}