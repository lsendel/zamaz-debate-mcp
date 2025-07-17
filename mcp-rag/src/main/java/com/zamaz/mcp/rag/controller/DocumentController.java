package com.zamaz.mcp.rag.controller;

import com.zamaz.mcp.rag.model.Document;
import com.zamaz.mcp.rag.model.DocumentIngestionRequest;
import com.zamaz.mcp.rag.model.DocumentIngestionResponse;
import com.zamaz.mcp.rag.service.AsyncDocumentIngestionService;
import com.zamaz.mcp.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for document operations.
 */
@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final AsyncDocumentIngestionService asyncDocumentIngestionService;

    /**
     * Ingest documents into a knowledge base.
     *
     * @param organizationId the organization ID
     * @param knowledgeBaseId the knowledge base ID
     * @param files the files to ingest
     * @param request the ingestion request
     * @return the ingestion response
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentIngestionResponse> ingestDocuments(
            @RequestHeader("X-Organization-ID") String organizationId,
            @PathVariable String knowledgeBaseId,
            @RequestParam("files") List<MultipartFile> files,
            @ModelAttribute DocumentIngestionRequest request) {
        
        log.info("Ingesting {} documents into knowledge base: {}", files.size(), knowledgeBaseId);
        
        DocumentIngestionResponse response = asyncDocumentIngestionService.ingestDocumentsAsync(
                organizationId, knowledgeBaseId, request, files);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get all documents in a knowledge base.
     *
     * @param organizationId the organization ID
     * @param knowledgeBaseId the knowledge base ID
     * @return the list of documents
     */
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(
            @RequestHeader("X-Organization-ID") String organizationId,
            @PathVariable String knowledgeBaseId) {
        
        List<Document> documents = documentService.getDocuments(organizationId, knowledgeBaseId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get a document by ID.
     *
     * @param organizationId the organization ID
     * @param knowledgeBaseId the knowledge base ID
     * @param documentId the document ID
     * @return the document
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocument(
            @RequestHeader("X-Organization-ID") String organizationId,
            @PathVariable String knowledgeBaseId,
            @PathVariable String documentId) {
        
        Document document = documentService.getDocument(organizationId, knowledgeBaseId, documentId);
        return ResponseEntity.ok(document);
    }

    /**
     * Delete a document.
     *
     * @param organizationId the organization ID
     * @param knowledgeBaseId the knowledge base ID
     * @param documentId the document ID
     * @return no content
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @RequestHeader("X-Organization-ID") String organizationId,
            @PathVariable String knowledgeBaseId,
            @PathVariable String documentId) {
        
        documentService.deleteDocument(organizationId, knowledgeBaseId, documentId);
        return ResponseEntity.noContent().build();
    }
}
