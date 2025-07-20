package com.zamaz.mcp.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory vector store implementation.
 * This is a placeholder for development/testing until a proper vector database is integrated.
 */
@Slf4j
@Service
public class InMemoryVectorStore {
    
    private final Map<String, VectorDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, List<VectorDocument>> organizationIndex = new ConcurrentHashMap<>();
    
    public static class VectorDocument {
        private final String id;
        private final String organizationId;
        private final String documentId;
        private final String chunkId;
        private final String content;
        private final float[] embedding;
        private final Map<String, Object> metadata;
        
        public VectorDocument(String id, String organizationId, String documentId, 
                            String chunkId, String content, float[] embedding, 
                            Map<String, Object> metadata) {
            this.id = id;
            this.organizationId = organizationId;
            this.documentId = documentId;
            this.chunkId = chunkId;
            this.content = content;
            this.embedding = embedding;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters
        public String getId() { return id; }
        public String getOrganizationId() { return organizationId; }
        public String getDocumentId() { return documentId; }
        public String getChunkId() { return chunkId; }
        public String getContent() { return content; }
        public float[] getEmbedding() { return embedding; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    public void upsert(VectorDocument document) {
        documents.put(document.getId(), document);
        organizationIndex.computeIfAbsent(document.getOrganizationId(), k -> new ArrayList<>())
                        .add(document);
        log.debug("Stored vector document: {} for organization: {}", 
                 document.getId(), document.getOrganizationId());
    }
    
    public List<SearchResult> search(String organizationId, float[] queryEmbedding, int limit) {
        List<VectorDocument> orgDocuments = organizationIndex.getOrDefault(organizationId, new ArrayList<>());
        
        return orgDocuments.stream()
            .map(doc -> new SearchResult(doc, cosineSimilarity(queryEmbedding, doc.getEmbedding())))
            .sorted((a, b) -> Float.compare(b.score, a.score))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public void deleteByDocumentId(String organizationId, String documentId) {
        List<VectorDocument> orgDocuments = organizationIndex.getOrDefault(organizationId, new ArrayList<>());
        List<VectorDocument> toRemove = orgDocuments.stream()
            .filter(doc -> doc.getDocumentId().equals(documentId))
            .collect(Collectors.toList());
        
        toRemove.forEach(doc -> {
            documents.remove(doc.getId());
            orgDocuments.remove(doc);
        });
        
        log.debug("Deleted {} chunks for document: {} in organization: {}", 
                 toRemove.size(), documentId, organizationId);
    }
    
    public void deleteAll(String organizationId) {
        List<VectorDocument> orgDocuments = organizationIndex.remove(organizationId);
        if (orgDocuments != null) {
            orgDocuments.forEach(doc -> documents.remove(doc.getId()));
            log.debug("Deleted all {} documents for organization: {}", 
                     orgDocuments.size(), organizationId);
        }
    }
    
    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (float)(Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    public static class SearchResult {
        public final VectorDocument document;
        public final float score;
        
        public SearchResult(VectorDocument document, float score) {
            this.document = document;
            this.score = score;
        }
    }
}