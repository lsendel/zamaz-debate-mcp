package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.Entity;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing a chunk of a document with its embedding.
 */
public class DocumentChunk implements Entity<ChunkId> {
    
    private final ChunkId id;
    private final DocumentId documentId;
    private final ChunkContent content;
    private final int chunkIndex;
    private final int startPosition;
    private final int endPosition;
    private final Instant createdAt;
    private Embedding embedding;
    private double relevanceScore;
    
    private DocumentChunk(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Chunk ID cannot be null");
        this.documentId = Objects.requireNonNull(builder.documentId, "Document ID cannot be null");
        this.content = Objects.requireNonNull(builder.content, "Content cannot be null");
        this.chunkIndex = builder.chunkIndex;
        this.startPosition = builder.startPosition;
        this.endPosition = builder.endPosition;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created timestamp cannot be null");
        this.embedding = builder.embedding;
        this.relevanceScore = builder.relevanceScore;
        
        validateInvariants();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static DocumentChunk create(ChunkId id, DocumentId documentId, ChunkContent content, 
                                     int chunkIndex, int startPosition, int endPosition) {
        return builder()
            .id(id)
            .documentId(documentId)
            .content(content)
            .chunkIndex(chunkIndex)
            .startPosition(startPosition)
            .endPosition(endPosition)
            .createdAt(Instant.now())
            .build();
    }
    
    @Override
    public ChunkId getId() {
        return id;
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public ChunkContent getContent() {
        return content;
    }
    
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public int getStartPosition() {
        return startPosition;
    }
    
    public int getEndPosition() {
        return endPosition;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Optional<Embedding> getEmbedding() {
        return Optional.ofNullable(embedding);
    }
    
    public double getRelevanceScore() {
        return relevanceScore;
    }
    
    public boolean hasEmbedding() {
        return embedding != null;
    }
    
    public int getContentLength() {
        return content.length();
    }
    
    public int getWordCount() {
        return content.wordCount();
    }
    
    public void setEmbedding(Embedding embedding) {
        this.embedding = Objects.requireNonNull(embedding, "Embedding cannot be null");
    }
    
    public void updateRelevanceScore(double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
        }
        this.relevanceScore = score;
    }
    
    public double calculateSimilarity(DocumentChunk other) {
        Objects.requireNonNull(other, "Other chunk cannot be null");
        
        if (!this.hasEmbedding() || !other.hasEmbedding()) {
            // Fallback to content similarity if embeddings are not available
            return this.content.calculateSimilarity(other.content);
        }
        
        return this.embedding.cosineSimilarity(other.embedding);
    }
    
    public boolean isRelevant(double threshold) {
        return relevanceScore >= threshold;
    }
    
    public boolean isHighlyRelevant() {
        return relevanceScore >= 0.8;
    }
    
    public boolean isLowRelevance() {
        return relevanceScore < 0.3;
    }
    
    public String getContentPreview(int maxChars) {
        return content.getPreview(maxChars);
    }
    
    public boolean containsText(String searchText) {
        Objects.requireNonNull(searchText, "Search text cannot be null");
        return content.contains(searchText);
    }
    
    public DocumentChunk withRelevanceScore(double score) {
        return builder()
            .id(this.id)
            .documentId(this.documentId)
            .content(this.content)
            .chunkIndex(this.chunkIndex)
            .startPosition(this.startPosition)
            .endPosition(this.endPosition)
            .createdAt(this.createdAt)
            .embedding(this.embedding)
            .relevanceScore(score)
            .build();
    }
    
    private void validateInvariants() {
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative");
        }
        
        if (startPosition < 0) {
            throw new IllegalArgumentException("Start position cannot be negative");
        }
        
        if (endPosition <= startPosition) {
            throw new IllegalArgumentException("End position must be greater than start position");
        }
        
        if (relevanceScore < 0.0 || relevanceScore > 1.0) {
            throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DocumentChunk that = (DocumentChunk) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("DocumentChunk{id=%s, documentId=%s, index=%d, length=%d, hasEmbedding=%s, relevance=%.3f}",
            id, documentId, chunkIndex, getContentLength(), hasEmbedding(), relevanceScore);
    }
    
    public static class Builder {
        private ChunkId id;
        private DocumentId documentId;
        private ChunkContent content;
        private int chunkIndex;
        private int startPosition;
        private int endPosition;
        private Instant createdAt;
        private Embedding embedding;
        private double relevanceScore = 0.0;
        
        public Builder id(ChunkId id) {
            this.id = id;
            return this;
        }
        
        public Builder documentId(DocumentId documentId) {
            this.documentId = documentId;
            return this;
        }
        
        public Builder content(ChunkContent content) {
            this.content = content;
            return this;
        }
        
        public Builder chunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }
        
        public Builder startPosition(int startPosition) {
            this.startPosition = startPosition;
            return this;
        }
        
        public Builder endPosition(int endPosition) {
            this.endPosition = endPosition;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder embedding(Embedding embedding) {
            this.embedding = embedding;
            return this;
        }
        
        public Builder relevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
            return this;
        }
        
        public DocumentChunk build() {
            return new DocumentChunk(this);
        }
    }
}