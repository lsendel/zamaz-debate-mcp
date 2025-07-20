package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.rag.dto.DocumentRequest;
import com.zamaz.mcp.rag.dto.DocumentResponse;
import com.zamaz.mcp.rag.dto.SearchRequest;
import com.zamaz.mcp.rag.dto.SearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service interface for document management.
 */
public interface DocumentService {
    
    /**
     * Store a document in the RAG system.
     */
    DocumentResponse storeDocument(DocumentRequest request);
    
    /**
     * Store a document from file upload.
     */
    DocumentResponse storeDocumentFromFile(String organizationId, String documentId, MultipartFile file) throws IOException;
    
    /**
     * Get a document by organization and document ID.
     */
    DocumentResponse getDocument(String organizationId, String documentId);
    
    /**
     * List documents for an organization.
     */
    Page<DocumentResponse> listDocuments(String organizationId, Pageable pageable);
    
    /**
     * Search documents using semantic search.
     */
    SearchResponse searchDocuments(SearchRequest request);
    
    /**
     * Delete a document.
     */
    void deleteDocument(String organizationId, String documentId);
    
    /**
     * Process pending documents.
     */
    void processPendingDocuments();
}