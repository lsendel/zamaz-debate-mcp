package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.common.async.DocumentIngestionMessage;
import com.zamaz.mcp.common.async.RedisStreamMessageProducer;
import com.zamaz.mcp.rag.model.Document;
import com.zamaz.mcp.rag.model.DocumentIngestionRequest;
import com.zamaz.mcp.rag.model.DocumentIngestionResponse;
import com.zamaz.mcp.rag.model.KnowledgeBase;
import com.zamaz.mcp.rag.repository.DocumentRepository;
import com.zamaz.mcp.rag.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for asynchronous document ingestion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncDocumentIngestionService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final RedisStreamMessageProducer messageProducer;
    private final String documentStoragePath = "/app/data/documents";

    /**
     * Ingest documents asynchronously.
     *
     * @param organizationId the organization ID
     * @param knowledgeBaseId the knowledge base ID
     * @param request the ingestion request
     * @param files the files to ingest
     * @return the ingestion response
     */
    @Transactional
    public DocumentIngestionResponse ingestDocumentsAsync(
            String organizationId,
            String knowledgeBaseId,
            DocumentIngestionRequest request,
            List<MultipartFile> files) {
        
        // Validate knowledge base exists and belongs to organization
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndOrganizationId(knowledgeBaseId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found or access denied"));
        
        List<Document> savedDocuments = new ArrayList<>();
        List<String> failedDocuments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // Save the file to disk
                String documentId = UUID.randomUUID().toString();
                String fileName = file.getOriginalFilename();
                String contentType = file.getContentType();
                
                // Create directory if it doesn't exist
                Path directoryPath = Paths.get(documentStoragePath, organizationId, knowledgeBaseId);
                Files.createDirectories(directoryPath);
                
                // Save the file
                Path filePath = directoryPath.resolve(documentId + "_" + fileName);
                file.transferTo(filePath.toFile());
                
                // Create and save document metadata
                Document document = new Document();
                document.setId(documentId);
                document.setKnowledgeBaseId(knowledgeBaseId);
                document.setName(fileName);
                document.setContentType(contentType);
                document.setFilePath(filePath.toString());
                document.setStatus("PENDING");
                document.setCreatedAt(Instant.now());
                document.setUpdatedAt(Instant.now());
                document.setMetadata(request.getMetadata());
                
                Document savedDocument = documentRepository.save(document);
                savedDocuments.add(savedDocument);
                
                // Send message to Redis Stream for asynchronous processing
                DocumentIngestionMessage.ProcessingOptions processingOptions = DocumentIngestionMessage.ProcessingOptions.builder()
                        .chunkSize(request.getChunkSize())
                        .chunkOverlap(request.getChunkOverlap())
                        .skipSections(request.getSkipSections())
                        .embeddingModel(knowledgeBase.getEmbeddingModel())
                        .build();
                
                DocumentIngestionMessage message = DocumentIngestionMessage.builder()
                        .documentId(documentId)
                        .knowledgeBaseId(knowledgeBaseId)
                        .organizationId(organizationId)
                        .contentType(contentType)
                        .filePath(filePath.toString())
                        .metadata(request.getMetadata() != null ? request.getMetadata().toString() : null)
                        .processingOptions(processingOptions)
                        .build();
                
                RecordId recordId = messageProducer.sendDocumentIngestionMessage(message);
                
                if (recordId == null) {
                    log.error("Failed to send document ingestion message for document: {}", documentId);
                    failedDocuments.add(fileName);
                }
                
            } catch (IOException e) {
                log.error("Error saving document file", e);
                failedDocuments.add(file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Error processing document", e);
                failedDocuments.add(file.getOriginalFilename());
            }
        }
        
        return DocumentIngestionResponse.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .documentsAccepted(savedDocuments.size())
                .documentsFailed(failedDocuments.size())
                .failedDocuments(failedDocuments)
                .message("Documents accepted for processing")
                .build();
    }
}
