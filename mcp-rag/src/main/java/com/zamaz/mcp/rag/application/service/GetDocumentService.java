package com.zamaz.mcp.rag.application.service;

import com.zamaz.mcp.rag.application.port.in.GetDocumentUseCase;
import com.zamaz.mcp.rag.application.port.out.DocumentRepository;
import com.zamaz.mcp.rag.domain.exception.DocumentNotFoundException;
import com.zamaz.mcp.rag.domain.model.document.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Application service implementing the get document use case.
 * Retrieves document details and transforms them to DTOs.
 */
@Service
@Transactional(readOnly = true)
public class GetDocumentService implements GetDocumentUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(GetDocumentService.class);
    
    private final DocumentRepository documentRepository;
    
    public GetDocumentService(DocumentRepository documentRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
    }
    
    @Override
    public DocumentDto getDocument(GetDocumentQuery query) {
        log.info("Getting document: {} for organization: {}", 
                query.documentId(), query.organizationId());
        
        // Find the document
        Document document = documentRepository
                .findByIdAndOrganization(query.documentId(), query.organizationId())
                .orElseThrow(() -> new DocumentNotFoundException(query.documentId()));
        
        // Build the DTO
        DocumentDto.DocumentDtoBuilder builder = DocumentDto.builder()
                .id(document.getId())
                .name(document.getName().value())
                .status(document.getStatus().name())
                .embeddingStatus(document.getEmbeddingStatus().name())
                .chunkCount(document.getChunks().size())
                .fileSize(document.getMetadata().getFileInfo().fileSize())
                .fileType(document.getMetadata().getFileInfo().fileType())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt());
        
        // Add chunks if requested
        if (query.includeChunks()) {
            List<ChunkDto> chunks = document.getChunks().stream()
                    .map(this::toChunkDto)
                    .collect(Collectors.toList());
            builder.chunks(chunks);
        }
        
        // Add processing history if requested
        if (query.includeProcessingHistory()) {
            List<ProcessingStepDto> history = document.getProcessingHistory()
                    .getSteps().stream()
                    .map(this::toProcessingStepDto)
                    .collect(Collectors.toList());
            builder.processingHistory(history);
        }
        
        return builder.build();
    }
    
    private ChunkDto toChunkDto(DocumentChunk chunk) {
        return new ChunkDto(
            chunk.getId().value(),
            chunk.getSequenceNumber(),
            chunk.getContent().text(),
            chunk.hasEmbedding()
        );
    }
    
    private ProcessingStepDto toProcessingStepDto(ProcessingStep step) {
        return new ProcessingStepDto(
            step.type().name(),
            step.description(),
            step.timestamp(),
            step.metadata()
        );
    }
    
    /**
     * Builder for DocumentDto to handle optional fields
     */
    private static class DocumentDtoBuilder {
        private DocumentId id;
        private String name;
        private String status;
        private String embeddingStatus;
        private int chunkCount;
        private long fileSize;
        private String fileType;
        private java.time.Instant createdAt;
        private java.time.Instant updatedAt;
        private List<ChunkDto> chunks;
        private List<ProcessingStepDto> processingHistory;
        
        public static DocumentDtoBuilder builder() {
            return new DocumentDtoBuilder();
        }
        
        public DocumentDtoBuilder id(DocumentId id) {
            this.id = id;
            return this;
        }
        
        public DocumentDtoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public DocumentDtoBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public DocumentDtoBuilder embeddingStatus(String embeddingStatus) {
            this.embeddingStatus = embeddingStatus;
            return this;
        }
        
        public DocumentDtoBuilder chunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
            return this;
        }
        
        public DocumentDtoBuilder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }
        
        public DocumentDtoBuilder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }
        
        public DocumentDtoBuilder createdAt(java.time.Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public DocumentDtoBuilder updatedAt(java.time.Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public DocumentDtoBuilder chunks(List<ChunkDto> chunks) {
            this.chunks = chunks;
            return this;
        }
        
        public DocumentDtoBuilder processingHistory(List<ProcessingStepDto> processingHistory) {
            this.processingHistory = processingHistory;
            return this;
        }
        
        public DocumentDto build() {
            return new DocumentDto(
                id, name, status, embeddingStatus, chunkCount,
                fileSize, fileType, createdAt, updatedAt,
                chunks, processingHistory
            );
        }
    }
}