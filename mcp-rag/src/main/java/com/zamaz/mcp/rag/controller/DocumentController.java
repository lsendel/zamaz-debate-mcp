package com.zamaz.mcp.rag.controller;

import com.zamaz.mcp.common.dto.ApiResponse;
import com.zamaz.mcp.rag.dto.DocumentRequest;
import com.zamaz.mcp.rag.dto.DocumentResponse;
import com.zamaz.mcp.rag.dto.SearchRequest;
import com.zamaz.mcp.rag.dto.SearchResponse;
import com.zamaz.mcp.rag.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for document management.
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {
    
    private final DocumentService documentService;
    
    @PostMapping
    @Operation(summary = "Store a document")
    public ResponseEntity<ApiResponse<DocumentResponse>> storeDocument(@Valid @RequestBody DocumentRequest request) {
        log.info("Storing document: {} for organization: {}", request.getDocumentId(), request.getOrganizationId());
        
        DocumentResponse response = documentService.storeDocument(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document stored successfully"));
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and store a document file")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestParam("organizationId") String organizationId,
            @RequestParam("documentId") String documentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        log.info("Uploading document file: {} for organization: {}", file.getOriginalFilename(), organizationId);
        
        DocumentResponse response = documentService.storeDocumentFromFile(organizationId, documentId, file);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }
    
    @GetMapping("/{organizationId}/{documentId}")
    @Operation(summary = "Get a document by ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable String organizationId,
            @PathVariable String documentId) {
        
        DocumentResponse response = documentService.getDocument(organizationId, documentId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{organizationId}")
    @Operation(summary = "List documents for an organization")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> listDocuments(
            @PathVariable String organizationId,
            Pageable pageable) {
        
        Page<DocumentResponse> documents = documentService.listDocuments(organizationId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(documents));
    }
    
    @PostMapping("/search")
    @Operation(summary = "Search documents")
    public ResponseEntity<ApiResponse<SearchResponse>> searchDocuments(@Valid @RequestBody SearchRequest request) {
        log.info("Searching documents with query: {} for organization: {}", 
                request.getQuery(), request.getOrganizationId());
        
        SearchResponse response = documentService.searchDocuments(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{organizationId}/{documentId}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String organizationId,
            @PathVariable String documentId) {
        
        log.info("Deleting document: {} from organization: {}", documentId, organizationId);
        
        documentService.deleteDocument(organizationId, documentId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }
}