package com.zamaz.mcp.rag.domain.port;

import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.DocumentStatus;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository port for document persistence.
 */
public interface DocumentRepository {
    
    /**
     * Save a document.
     * 
     * @param document the document to save
     * @return the saved document
     */
    Document save(Document document);
    
    /**
     * Find a document by ID.
     * 
     * @param documentId the document ID
     * @return the document if found
     */
    Optional<Document> findById(DocumentId documentId);
    
    /**
     * Find a document by ID and organization.
     * 
     * @param documentId the document ID
     * @param organizationId the organization ID
     * @return the document if found and belongs to organization
     */
    Optional<Document> findByIdAndOrganization(DocumentId documentId, OrganizationId organizationId);
    
    /**
     * Find all documents for an organization.
     * 
     * @param organizationId the organization ID
     * @return all documents for the organization
     */
    List<Document> findByOrganization(OrganizationId organizationId);
    
    /**
     * Find documents by organization and status.
     * 
     * @param organizationId the organization ID
     * @param statuses the statuses to filter by
     * @return documents matching the criteria
     */
    List<Document> findByOrganizationAndStatuses(OrganizationId organizationId, Set<DocumentStatus> statuses);
    
    /**
     * Find documents by organization and title containing text.
     * 
     * @param organizationId the organization ID
     * @param titleFilter the text to search for in titles
     * @return documents with matching titles
     */
    List<Document> findByOrganizationAndTitleContaining(OrganizationId organizationId, String titleFilter);
    
    /**
     * Find documents with pagination.
     * 
     * @param organizationId the organization ID
     * @param statuses the statuses to filter by
     * @param titleFilter the title filter (optional)
     * @param limit maximum number of results
     * @param offset offset for pagination
     * @return documents matching the criteria
     */
    List<Document> findByFilters(OrganizationId organizationId, Set<DocumentStatus> statuses, 
                                String titleFilter, Integer limit, Integer offset);
    
    /**
     * Find pending documents for processing.
     * 
     * @param limit maximum number to retrieve
     * @return pending documents
     */
    List<Document> findPendingDocuments(int limit);
    
    /**
     * Count documents by organization and status.
     * 
     * @param organizationId the organization ID
     * @param statuses the statuses to filter by
     * @return count of documents
     */
    long countByOrganizationAndStatuses(OrganizationId organizationId, Set<DocumentStatus> statuses);
    
    /**
     * Delete a document.
     * 
     * @param documentId the document ID to delete
     * @return true if deleted, false if not found
     */
    boolean deleteById(DocumentId documentId);
    
    /**
     * Check if a document exists.
     * 
     * @param documentId the document ID
     * @return true if exists, false otherwise
     */
    boolean existsById(DocumentId documentId);
    
    /**
     * Check if a document exists for an organization.
     * 
     * @param documentId the document ID
     * @param organizationId the organization ID
     * @return true if exists and belongs to organization, false otherwise
     */
    boolean existsByIdAndOrganization(DocumentId documentId, OrganizationId organizationId);
}