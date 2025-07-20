package com.zamaz.mcp.rag.application.port.in;

import com.zamaz.mcp.rag.domain.model.document.DocumentId;
import com.zamaz.mcp.rag.domain.model.document.OrganizationId;

/**
 * Inbound port for retrieving document details.
 * This interface defines the contract for document retrieval use case.
 */
public interface GetDocumentUseCase {
    
    /**
     * Get document details by ID
     * 
     * @param query The query containing document ID and organization context
     * @return Document details
     */
    DocumentDto getDocument(GetDocumentQuery query);
    
    /**
     * Query object for document retrieval
     */
    record GetDocumentQuery(
        DocumentId documentId,
        OrganizationId organizationId,
        boolean includeChunks,
        boolean includeProcessingHistory
    ) {
        public GetDocumentQuery {
            if (documentId == null) {
                throw new IllegalArgumentException("Document ID cannot be null");
            }
            if (organizationId == null) {
                throw new IllegalArgumentException("Organization ID cannot be null");
            }
        }
        
        /**
         * Create a basic query without additional details
         */
        public static GetDocumentQuery basic(DocumentId documentId, OrganizationId organizationId) {
            return new GetDocumentQuery(documentId, organizationId, false, false);
        }
        
        /**
         * Create a detailed query with all information
         */
        public static GetDocumentQuery detailed(DocumentId documentId, OrganizationId organizationId) {
            return new GetDocumentQuery(documentId, organizationId, true, true);
        }
    }
    
    /**
     * DTO for document information
     */
    record DocumentDto(
        DocumentId id,
        String name,
        String status,
        String embeddingStatus,
        int chunkCount,
        long fileSize,
        String fileType,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        java.util.List<ChunkDto> chunks,
        java.util.List<ProcessingStepDto> processingHistory
    ) {
        public DocumentDto {
            if (chunks == null) {
                chunks = java.util.List.of();
            }
            if (processingHistory == null) {
                processingHistory = java.util.List.of();
            }
        }
    }
    
    /**
     * DTO for chunk information
     */
    record ChunkDto(
        String chunkId,
        int index,
        String content,
        boolean hasEmbedding
    ) {}
    
    /**
     * DTO for processing step
     */
    record ProcessingStepDto(
        String type,
        String description,
        java.time.Instant timestamp,
        java.util.Map<String, String> metadata
    ) {}
}