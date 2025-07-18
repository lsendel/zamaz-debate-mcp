package com.zamaz.mcp.rag.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentChunkEntity;
import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity;
import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity.DocumentStatusEnum;
import com.zamaz.mcp.rag.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain models and JPA entities.
 */
@Component
public class DocumentEntityMapper {
    
    private final ObjectMapper objectMapper;
    
    public DocumentEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Convert domain document to JPA entity.
     */
    public DocumentEntity toEntity(Document document) {
        DocumentEntity entity = new DocumentEntity();
        
        entity.setId(UUID.fromString(document.getId().toString()));
        entity.setOrganizationId(UUID.fromString(document.getOrganizationId().toString()));
        entity.setTitle(document.getMetadata().title());
        entity.setStatus(mapStatus(document.getStatus()));
        entity.setContent(document.getContent().text());
        entity.setContentType(document.getContent().type());
        
        FileInfo fileInfo = document.getFileInfo();
        entity.setFileName(fileInfo.fileName());
        entity.setFileType(fileInfo.fileType());
        entity.setFileSize(fileInfo.fileSize());
        
        // Serialize metadata to JSON
        try {
            entity.setMetadata(objectMapper.writeValueAsString(document.getMetadata().properties()));
        } catch (JsonProcessingException e) {
            entity.setMetadata("{}");
        }
        
        entity.setChunkCount(document.getChunkCount());
        entity.setCreatedAt(document.getCreatedAt());
        entity.setUpdatedAt(document.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain document.
     */
    public Document toDomain(DocumentEntity entity) {
        DocumentId documentId = DocumentId.from(entity.getId().toString());
        OrganizationId organizationId = OrganizationId.from(entity.getOrganizationId().toString());
        
        DocumentContent content = DocumentContent.of(
            entity.getContent(),
            entity.getContentType()
        );
        
        // Deserialize metadata from JSON
        Map<String, String> properties = new HashMap<>();
        if (entity.getMetadata() != null) {
            try {
                properties = objectMapper.readValue(entity.getMetadata(), Map.class);
            } catch (JsonProcessingException e) {
                // Ignore, use empty properties
            }
        }
        
        DocumentMetadata metadata = DocumentMetadata.of(entity.getTitle(), properties);
        
        FileInfo fileInfo = FileInfo.of(
            entity.getFileName(),
            entity.getFileType(),
            entity.getFileSize()
        );
        
        Document document = Document.create(
            documentId,
            organizationId,
            content,
            metadata,
            fileInfo
        );
        
        // Set status
        document.updateStatus(mapStatus(entity.getStatus()));
        
        // Add chunks if loaded
        if (entity.getChunks() != null) {
            entity.getChunks().forEach(chunkEntity -> {
                DocumentChunk chunk = toDomainChunk(chunkEntity);
                document.addChunk(chunk);
            });
        }
        
        return document;
    }
    
    /**
     * Convert domain chunk to JPA entity.
     */
    public DocumentChunkEntity toChunkEntity(DocumentChunk chunk, DocumentEntity documentEntity) {
        DocumentChunkEntity entity = new DocumentChunkEntity();
        
        entity.setId(UUID.fromString(chunk.getId().toString()));
        entity.setDocument(documentEntity);
        entity.setChunkNumber(chunk.getChunkNumber());
        entity.setContent(chunk.getContent().text());
        entity.setStartOffset(chunk.getContent().startOffset());
        entity.setEndOffset(chunk.getContent().endOffset());
        
        // Convert embedding to string representation
        if (chunk.getEmbedding() != null) {
            entity.setEmbedding(serializeEmbedding(chunk.getEmbedding()));
        }
        
        entity.setCreatedAt(chunk.getCreatedAt());
        entity.setUpdatedAt(chunk.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain chunk.
     */
    public DocumentChunk toDomainChunk(DocumentChunkEntity entity) {
        ChunkId chunkId = ChunkId.from(entity.getId().toString());
        DocumentId documentId = DocumentId.from(entity.getDocument().getId().toString());
        
        ChunkContent content = ChunkContent.of(
            entity.getContent(),
            entity.getStartOffset() != null ? entity.getStartOffset() : 0,
            entity.getEndOffset() != null ? entity.getEndOffset() : entity.getContent().length()
        );
        
        DocumentChunk chunk = DocumentChunk.create(
            chunkId,
            documentId,
            entity.getDocument().getTitle(),
            content,
            entity.getChunkNumber()
        );
        
        // Set embedding if present
        if (entity.getEmbedding() != null) {
            Embedding embedding = deserializeEmbedding(entity.getEmbedding());
            if (embedding != null) {
                chunk.setEmbedding(embedding);
            }
        }
        
        return chunk;
    }
    
    /**
     * Map domain status to JPA enum.
     */
    private DocumentStatusEnum mapStatus(DocumentStatus status) {
        return switch (status) {
            case UPLOADED -> DocumentStatusEnum.UPLOADED;
            case PROCESSING -> DocumentStatusEnum.PROCESSING;
            case PROCESSED -> DocumentStatusEnum.PROCESSED;
            case FAILED -> DocumentStatusEnum.FAILED;
            case ARCHIVED -> DocumentStatusEnum.ARCHIVED;
        };
    }
    
    /**
     * Map JPA enum to domain status.
     */
    private DocumentStatus mapStatus(DocumentStatusEnum status) {
        return switch (status) {
            case UPLOADED -> DocumentStatus.UPLOADED;
            case PROCESSING -> DocumentStatus.PROCESSING;
            case PROCESSED -> DocumentStatus.PROCESSED;
            case FAILED -> DocumentStatus.FAILED;
            case ARCHIVED -> DocumentStatus.ARCHIVED;
        };
    }
    
    /**
     * Serialize embedding to string.
     */
    private String serializeEmbedding(Embedding embedding) {
        // Convert to PostgreSQL vector format: [0.1,0.2,0.3]
        return "[" + embedding.vector().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")) + "]";
    }
    
    /**
     * Deserialize embedding from string.
     */
    private Embedding deserializeEmbedding(String embeddingStr) {
        if (embeddingStr == null || embeddingStr.isBlank()) {
            return null;
        }
        
        try {
            // Remove brackets and split
            String vectorStr = embeddingStr.trim();
            if (vectorStr.startsWith("[")) {
                vectorStr = vectorStr.substring(1);
            }
            if (vectorStr.endsWith("]")) {
                vectorStr = vectorStr.substring(0, vectorStr.length() - 1);
            }
            
            List<Double> vector = Arrays.stream(vectorStr.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
            
            return new Embedding(vector);
        } catch (Exception e) {
            return null;
        }
    }
}