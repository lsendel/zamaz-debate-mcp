package com.zamaz.mcp.common.domain.rag;

import java.util.List;

/**
 * Port interface for RAG (Retrieval-Augmented Generation) services.
 */
public interface RagServicePort {
    
    /**
     * Retrieves documents relevant to the given query.
     *
     * @param query The search query
     * @param limit Maximum number of documents to retrieve
     * @return List of relevant documents
     */
    List<Document> retrieveDocuments(String query, int limit);
    
    /**
     * Indexes a new document in the RAG system.
     *
     * @param document The document to index
     * @return The indexed document with ID
     */
    Document indexDocument(Document document);
    
    /**
     * Deletes a document from the RAG system.
     *
     * @param documentId The ID of the document to delete
     * @return True if deleted successfully, false otherwise
     */
    boolean deleteDocument(String documentId);
    
    /**
     * Updates an existing document in the RAG system.
     *
     * @param document The document with updated content
     * @return The updated document
     */
    Document updateDocument(Document document);
    
    /**
     * Performs a similarity search for documents.
     *
     * @param embedding The embedding vector to search for
     * @param limit Maximum number of documents to retrieve
     * @return List of similar documents
     */
    List<Document> searchByEmbedding(float[] embedding, int limit);
}