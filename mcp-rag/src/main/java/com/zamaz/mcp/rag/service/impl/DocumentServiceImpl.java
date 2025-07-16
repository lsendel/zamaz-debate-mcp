package com.zamaz.mcp.rag.service.impl;

import com.zamaz.mcp.common.exception.BusinessException;
import com.zamaz.mcp.rag.dto.DocumentRequest;
import com.zamaz.mcp.rag.dto.DocumentResponse;
import com.zamaz.mcp.rag.dto.SearchRequest;
import com.zamaz.mcp.rag.dto.SearchResponse;
import com.zamaz.mcp.rag.dto.SearchResult;
import com.zamaz.mcp.rag.entity.Document;
import com.zamaz.mcp.rag.entity.DocumentChunk;
import com.zamaz.mcp.rag.entity.DocumentStatus;
import com.zamaz.mcp.rag.repository.DocumentChunkRepository;
import com.zamaz.mcp.rag.repository.DocumentRepository;
import com.zamaz.mcp.rag.service.ChunkingService;
import com.zamaz.mcp.rag.service.DocumentService;
import com.zamaz.mcp.rag.service.EmbeddingService;
import com.zamaz.mcp.rag.service.VectorStoreService;
import com.zamaz.mcp.rag.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentParser documentParser;
    
    @Override
    @Transactional
    public DocumentResponse storeDocument(DocumentRequest request) {
        log.info("Storing document: {} for organization: {}", request.getDocumentId(), request.getOrganizationId());
        
        // Check if document already exists
        documentRepository.findByOrganizationIdAndDocumentId(request.getOrganizationId(), request.getDocumentId())
                .ifPresent(doc -> {
                    throw BusinessException.alreadyExists("Document", "documentId", request.getDocumentId());
                });
        
        // Create new document
        Document document = Document.builder()
                .organizationId(request.getOrganizationId())
                .documentId(request.getDocumentId())
                .title(request.getTitle())
                .content(request.getContent())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
                .status(DocumentStatus.PENDING)
                .build();
        
        document = documentRepository.save(document);
        
        // Process document asynchronously
        processDocumentAsync(document.getId());
        
        return mapToResponse(document);
    }
    
    @Override
    @Transactional
    public DocumentResponse storeDocumentFromFile(String organizationId, String documentId, MultipartFile file) throws IOException {
        log.info("Storing document from file: {} for organization: {}", file.getOriginalFilename(), organizationId);
        
        // Parse document content
        String content = documentParser.parseDocument(file);
        
        DocumentRequest request = DocumentRequest.builder()
                .organizationId(organizationId)
                .documentId(documentId)
                .title(file.getOriginalFilename())
                .content(content)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();
        
        return storeDocument(request);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String organizationId, String documentId) {
        Document document = documentRepository.findByOrganizationIdAndDocumentId(organizationId, documentId)
                .orElseThrow(() -> BusinessException.notFound("Document", documentId));
        
        return mapToResponse(document);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String organizationId, Pageable pageable) {
        Page<Document> documents = documentRepository.findByOrganizationId(organizationId, pageable);
        return documents.map(this::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchDocuments(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Searching documents for query: {} in organization: {}", request.getQuery(), request.getOrganizationId());
        
        // Ensure collection exists
        vectorStoreService.ensureCollection(request.getOrganizationId());
        
        // Generate embedding for query
        List<Float> queryEmbedding = embeddingService.generateEmbedding(request.getQuery());
        
        // Search in vector store
        List<SearchResult> results = vectorStoreService.searchSimilar(
                queryEmbedding,
                request.getOrganizationId(),
                request.getLimit(),
                request.getSimilarityThreshold()
        );
        
        // Enrich results with document content if requested
        if (Boolean.TRUE.equals(request.getIncludeContent()) || Boolean.TRUE.equals(request.getIncludeMetadata())) {
            enrichSearchResults(results, request);
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return SearchResponse.builder()
                .query(request.getQuery())
                .totalResults(results.size())
                .results(results)
                .searchTimeMs(searchTime)
                .build();
    }
    
    @Override
    @Transactional
    public void deleteDocument(String organizationId, String documentId) {
        log.info("Deleting document: {} from organization: {}", documentId, organizationId);
        
        Document document = documentRepository.findByOrganizationIdAndDocumentId(organizationId, documentId)
                .orElseThrow(() -> BusinessException.notFound("Document", documentId));
        
        // Delete chunks from vector store
        List<DocumentChunk> chunks = chunkRepository.findChunksByDocumentId(document.getId());
        for (DocumentChunk chunk : chunks) {
            if (chunk.getVectorId() != null) {
                vectorStoreService.deleteVector(chunk.getVectorId());
            }
        }
        
        // Delete chunks from database
        chunkRepository.deleteByDocumentId(document.getId());
        
        // Delete document
        documentRepository.delete(document);
    }
    
    @Override
    @Transactional
    public void processPendingDocuments() {
        List<Document> pendingDocs = documentRepository.findByStatusAndProcessedAtIsNull(DocumentStatus.PENDING);
        log.info("Processing {} pending documents", pendingDocs.size());
        
        for (Document doc : pendingDocs) {
            processDocumentAsync(doc.getId());
        }
    }
    
    @Async
    protected void processDocumentAsync(String documentId) {
        try {
            processDocument(documentId);
        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            markDocumentFailed(documentId, e.getMessage());
        }
    }
    
    @Transactional
    protected void processDocument(String documentId) {
        log.info("Processing document: {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.notFound("Document", documentId));
        
        updateDocumentStatus(document, DocumentStatus.PROCESSING);
        
        try {
            // Ensure vector collection exists
            vectorStoreService.ensureCollection(document.getOrganizationId());
            
            // Process chunks and generate embeddings
            List<DocumentChunk> chunks = processChunks(document);
            List<List<Float>> embeddings = generateAndStoreEmbeddings(chunks, document);
            
            // Update document completion status
            updateDocumentStatus(document, DocumentStatus.COMPLETED, chunks.size());
            
            log.info("Successfully processed document: {} with {} chunks", documentId, chunks.size());
            
        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            markDocumentFailed(documentId, e.getMessage());
            throw new BusinessException("Failed to process document: " + e.getMessage(), "DOCUMENT_PROCESSING_FAILED")
                    .withDetail("documentId", documentId)
                    .withDetail("error", e.getMessage());
        }
    }
    
    private void updateDocumentStatus(Document document, DocumentStatus status) {
        document.setStatus(status);
        documentRepository.save(document);
    }
    
    private void updateDocumentStatus(Document document, DocumentStatus status, int chunkCount) {
        document.setStatus(status);
        document.setChunkCount(chunkCount);
        document.setProcessedAt(LocalDateTime.now());
        documentRepository.save(document);
    }
    
    private List<DocumentChunk> processChunks(Document document) {
        // Chunk the document
        List<DocumentChunk> chunks = chunkingService.chunkDocument(
                document.getContent(),
                document.getId(),
                document.getOrganizationId()
        );
        
        // Save chunks
        return chunkRepository.saveAll(chunks);
    }
    
    private List<List<Float>> generateAndStoreEmbeddings(List<DocumentChunk> chunks, Document document) {
        // Extract chunk texts
        List<String> chunkTexts = chunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
        
        // Generate embeddings
        List<List<Float>> embeddings = embeddingService.generateEmbeddings(chunkTexts);
        
        // Prepare vectors and metadata
        Map<String, List<Float>> vectorMap = new HashMap<>();
        Map<String, Map<String, Object>> metadataMap = new HashMap<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            processChunkVector(chunks.get(i), embeddings.get(i), document, vectorMap, metadataMap);
        }
        
        // Store vectors in batch
        vectorStoreService.storeVectors(vectorMap, metadataMap);
        
        // Update chunks with vector IDs
        chunkRepository.saveAll(chunks);
        
        return embeddings;
    }
    
    private void processChunkVector(DocumentChunk chunk, List<Float> embedding, Document document,
                                   Map<String, List<Float>> vectorMap, 
                                   Map<String, Map<String, Object>> metadataMap) {
        String vectorId = UUID.randomUUID().toString();
        
        // Update chunk with vector info
        chunk.setVectorId(vectorId);
        chunk.setEmbedding(embedding);
        
        // Add to vector map
        vectorMap.put(vectorId, embedding);
        
        // Create and add metadata
        Map<String, Object> metadata = createVectorMetadata(chunk, document);
        metadataMap.put(vectorId, metadata);
    }
    
    private Map<String, Object> createVectorMetadata(DocumentChunk chunk, Document document) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("organizationId", document.getOrganizationId());
        metadata.put("documentId", document.getDocumentId());
        metadata.put("chunkId", chunk.getId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("title", document.getTitle());
        metadata.put("content", chunk.getContent());
        return metadata;
    }
    
    private void markDocumentFailed(String documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(errorMessage);
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);
        });
    }
    
    private void enrichSearchResults(List<SearchResult> results, SearchRequest request) {
        // No enrichment needed as VectorStoreService already provides content
        // The search results from VectorStoreService already include content and metadata
    }
    
    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .organizationId(document.getOrganizationId())
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .metadata(document.getMetadata())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .processedAt(document.getProcessedAt())
                .build();
    }
}