package com.zamaz.mcp.rag.domain.model.embedding;

import com.zamaz.mcp.rag.domain.model.document.ChunkId;
import com.zamaz.mcp.rag.domain.model.document.DocumentId;

import java.time.Instant;
import java.util.Objects;

/**
 * Embedding Aggregate - represents an embedding vector associated with a document chunk.
 * This is a separate aggregate from Document to allow independent scaling and management.
 */
public class Embedding {
    
    private final EmbeddingId id;
    private final ChunkId chunkId;
    private final DocumentId documentId;
    private final EmbeddingVector vector;
    private final EmbeddingMetadata metadata;
    private final Instant createdAt;
    
    private Embedding(
            EmbeddingId id,
            ChunkId chunkId,
            DocumentId documentId,
            EmbeddingVector vector,
            EmbeddingMetadata metadata) {
        this.id = Objects.requireNonNull(id, "Embedding ID cannot be null");
        this.chunkId = Objects.requireNonNull(chunkId, "Chunk ID cannot be null");
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.vector = Objects.requireNonNull(vector, "Embedding vector cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Embedding metadata cannot be null");
        this.createdAt = Instant.now();
    }
    
    /**
     * Factory method for creating new embeddings
     */
    public static Embedding create(
            ChunkId chunkId,
            DocumentId documentId,
            EmbeddingVector vector,
            EmbeddingMetadata metadata) {
        EmbeddingId id = EmbeddingId.generate();
        return new Embedding(id, chunkId, documentId, vector, metadata);
    }
    
    /**
     * Factory method for reconstitution from persistence
     */
    public static Embedding reconstitute(
            EmbeddingId id,
            ChunkId chunkId,
            DocumentId documentId,
            EmbeddingVector vector,
            EmbeddingMetadata metadata,
            Instant createdAt) {
        Embedding embedding = new Embedding(id, chunkId, documentId, vector, metadata);
        // Would need reflection or different approach to set createdAt
        return embedding;
    }
    
    /**
     * Calculate similarity to another embedding
     */
    public double calculateSimilarity(Embedding other) {
        Objects.requireNonNull(other, "Other embedding cannot be null");
        return this.vector.cosineSimilarity(other.vector);
    }
    
    /**
     * Calculate distance to another embedding
     */
    public double calculateDistance(Embedding other) {
        Objects.requireNonNull(other, "Other embedding cannot be null");
        return this.vector.euclideanDistance(other.vector);
    }
    
    /**
     * Check if this embedding matches the given model
     */
    public boolean isFromModel(String modelName) {
        return metadata.modelName().equals(modelName);
    }
    
    // Getters
    public EmbeddingId getId() {
        return id;
    }
    
    public ChunkId getChunkId() {
        return chunkId;
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public EmbeddingVector getVector() {
        return vector;
    }
    
    public EmbeddingMetadata getMetadata() {
        return metadata;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Embedding embedding = (Embedding) o;
        return Objects.equals(id, embedding.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}