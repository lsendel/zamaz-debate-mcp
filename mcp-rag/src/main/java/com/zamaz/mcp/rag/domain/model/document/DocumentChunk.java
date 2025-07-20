package com.zamaz.mcp.rag.domain.model.document;

import com.zamaz.mcp.rag.domain.model.embedding.EmbeddingVector;

import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing a chunk of a document.
 * Part of the Document aggregate.
 */
public class DocumentChunk {
    
    private final ChunkId id;
    private final DocumentId documentId;
    private final ChunkContent content;
    private final int sequenceNumber;
    private final ChunkMetadata metadata;
    private EmbeddingVector embedding;
    
    private DocumentChunk(
            ChunkId id,
            DocumentId documentId,
            ChunkContent content,
            int sequenceNumber,
            ChunkMetadata metadata) {
        this.id = Objects.requireNonNull(id, "Chunk ID cannot be null");
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.content = Objects.requireNonNull(content, "Chunk content cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Chunk metadata cannot be null");
        
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("Sequence number must be non-negative");
        }
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * Factory method for creating a new chunk
     */
    public static DocumentChunk create(
            DocumentId documentId,
            ChunkContent content,
            int sequenceNumber,
            ChunkMetadata metadata) {
        ChunkId id = ChunkId.generate();
        return new DocumentChunk(id, documentId, content, sequenceNumber, metadata);
    }
    
    /**
     * Factory method for reconstitution from persistence
     */
    public static DocumentChunk reconstitute(
            ChunkId id,
            DocumentId documentId,
            ChunkContent content,
            int sequenceNumber,
            ChunkMetadata metadata,
            EmbeddingVector embedding) {
        DocumentChunk chunk = new DocumentChunk(id, documentId, content, sequenceNumber, metadata);
        chunk.embedding = embedding;
        return chunk;
    }
    
    /**
     * Update the embedding for this chunk
     */
    public void updateEmbedding(EmbeddingVector embedding) {
        this.embedding = Objects.requireNonNull(embedding, "Embedding cannot be null");
    }
    
    /**
     * Check if this chunk has an embedding
     */
    public boolean hasEmbedding() {
        return embedding != null;
    }
    
    /**
     * Get the embedding if present
     */
    public Optional<EmbeddingVector> getEmbedding() {
        return Optional.ofNullable(embedding);
    }
    
    /**
     * Calculate similarity to another chunk (requires embeddings)
     */
    public double calculateSimilarity(DocumentChunk other) {
        if (!this.hasEmbedding() || !other.hasEmbedding()) {
            throw new IllegalStateException("Both chunks must have embeddings to calculate similarity");
        }
        return this.embedding.cosineSimilarity(other.embedding);
    }
    
    // Getters
    public ChunkId getId() {
        return id;
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public ChunkContent getContent() {
        return content;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public ChunkMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentChunk that = (DocumentChunk) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

/**
 * Value object for chunk metadata
 */
record ChunkMetadata(
        int startOffset,
        int endOffset,
        int tokenCount,
        String checksum
) {
    public ChunkMetadata {
        if (startOffset < 0) {
            throw new IllegalArgumentException("Start offset must be non-negative");
        }
        if (endOffset <= startOffset) {
            throw new IllegalArgumentException("End offset must be greater than start offset");
        }
        if (tokenCount <= 0) {
            throw new IllegalArgumentException("Token count must be positive");
        }
        Objects.requireNonNull(checksum, "Checksum cannot be null");
    }
    
    public int getLength() {
        return endOffset - startOffset;
    }
}