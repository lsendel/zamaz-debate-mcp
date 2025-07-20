package com.zamaz.mcp.rag.adapter.persistence;

import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentChunkEntity;
import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity;
import com.zamaz.mcp.rag.domain.model.*;
import com.zamaz.mcp.rag.domain.port.ChunkRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation of ChunkRepository using JPA.
 */
@Repository
@Transactional
public class ChunkRepositoryAdapter implements ChunkRepository {
    
    private final DocumentChunkJpaRepository chunkRepository;
    private final DocumentJpaRepository documentRepository;
    private final DocumentEntityMapper mapper;
    
    public ChunkRepositoryAdapter(
            DocumentChunkJpaRepository chunkRepository,
            DocumentJpaRepository documentRepository,
            DocumentEntityMapper mapper) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.mapper = mapper;
    }
    
    @Override
    public DocumentChunk save(DocumentChunk chunk) {
        // Find the document entity
        Optional<DocumentEntity> documentOpt = documentRepository.findById(
            UUID.fromString(chunk.getDocumentId().toString())
        );
        
        if (documentOpt.isEmpty()) {
            throw new IllegalStateException("Document not found: " + chunk.getDocumentId());
        }
        
        DocumentChunkEntity entity = mapper.toChunkEntity(chunk, documentOpt.get());
        DocumentChunkEntity savedEntity = chunkRepository.save(entity);
        return mapper.toDomainChunk(savedEntity);
    }
    
    @Override
    public List<DocumentChunk> saveAll(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // All chunks should belong to the same document
        DocumentId documentId = chunks.get(0).getDocumentId();
        Optional<DocumentEntity> documentOpt = documentRepository.findById(
            UUID.fromString(documentId.toString())
        );
        
        if (documentOpt.isEmpty()) {
            throw new IllegalStateException("Document not found: " + documentId);
        }
        
        DocumentEntity documentEntity = documentOpt.get();
        
        List<DocumentChunkEntity> entities = chunks.stream()
            .map(chunk -> mapper.toChunkEntity(chunk, documentEntity))
            .collect(Collectors.toList());
        
        List<DocumentChunkEntity> savedEntities = chunkRepository.saveAll(entities);
        
        return savedEntities.stream()
            .map(mapper::toDomainChunk)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<DocumentChunk> findById(ChunkId id) {
        return chunkRepository.findById(UUID.fromString(id.toString()))
            .map(mapper::toDomainChunk);
    }
    
    @Override
    public List<DocumentChunk> findByDocumentId(DocumentId documentId) {
        return chunkRepository.findAllByDocumentIdOrderByChunkNumber(
                UUID.fromString(documentId.toString())
            )
            .stream()
            .map(mapper::toDomainChunk)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<DocumentChunk> findSimilar(
            OrganizationId organizationId,
            Embedding queryEmbedding,
            double minSimilarity,
            int maxResults) {
        
        // Serialize embedding for query
        String embeddingStr = serializeEmbedding(queryEmbedding);
        
        // Execute native query
        List<Object[]> results = chunkRepository.findSimilarChunks(
            UUID.fromString(organizationId.toString()),
            embeddingStr,
            minSimilarity,
            maxResults
        );
        
        // Map results to domain chunks with similarity scores
        return results.stream()
            .map(row -> {
                // The native query returns chunk entity fields and similarity score
                DocumentChunkEntity entity = mapRowToEntity(row);
                Double similarity = (Double) row[row.length - 1];
                
                DocumentChunk chunk = mapper.toDomainChunk(entity);
                chunk.setSimilarityScore(similarity);
                return chunk;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteByDocumentId(DocumentId documentId) {
        chunkRepository.deleteAllByDocumentId(UUID.fromString(documentId.toString()));
    }
    
    /**
     * Serialize embedding to PostgreSQL vector format.
     */
    private String serializeEmbedding(Embedding embedding) {
        return "[" + embedding.vector().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")) + "]";
    }
    
    /**
     * Map native query row to chunk entity.
     * This is a simplified implementation - in production would use proper mapping.
     */
    private DocumentChunkEntity mapRowToEntity(Object[] row) {
        DocumentChunkEntity entity = new DocumentChunkEntity();
        
        // Assuming standard column order from SELECT c.*
        int i = 0;
        entity.setId((UUID) row[i++]);
        // entity.setDocument() - would need to fetch separately
        entity.setChunkNumber((Integer) row[i++]);
        entity.setContent((String) row[i++]);
        entity.setStartOffset((Integer) row[i++]);
        entity.setEndOffset((Integer) row[i++]);
        entity.setEmbedding((String) row[i++]);
        entity.setCreatedAt((java.sql.Timestamp) row[i++] != null ? 
            ((java.sql.Timestamp) row[i-1]).toLocalDateTime() : null);
        entity.setUpdatedAt((java.sql.Timestamp) row[i++] != null ? 
            ((java.sql.Timestamp) row[i-1]).toLocalDateTime() : null);
        
        return entity;
    }
}