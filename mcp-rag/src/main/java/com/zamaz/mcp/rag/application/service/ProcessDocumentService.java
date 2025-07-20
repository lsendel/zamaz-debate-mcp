package com.zamaz.mcp.rag.application.service;

import com.zamaz.mcp.rag.application.port.in.ProcessDocumentUseCase;
import com.zamaz.mcp.rag.application.port.out.DocumentRepository;
import com.zamaz.mcp.rag.application.port.out.EmbeddingService;
import com.zamaz.mcp.rag.application.port.out.EventPublisher;
import com.zamaz.mcp.rag.application.port.out.VectorStore;
import com.zamaz.mcp.rag.domain.exception.DocumentNotFoundException;
import com.zamaz.mcp.rag.domain.model.document.*;
import com.zamaz.mcp.rag.domain.model.embedding.EmbeddingVector;
import com.zamaz.mcp.rag.domain.service.ChunkingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Application service implementing the process document use case.
 * Orchestrates document chunking and embedding generation.
 */
@Service
public class ProcessDocumentService implements ProcessDocumentUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(ProcessDocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final EventPublisher eventPublisher;
    private final ChunkingStrategyFactory chunkingStrategyFactory;
    
    public ProcessDocumentService(
            DocumentRepository documentRepository,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            EventPublisher eventPublisher,
            ChunkingStrategyFactory chunkingStrategyFactory) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.embeddingService = Objects.requireNonNull(embeddingService);
        this.vectorStore = Objects.requireNonNull(vectorStore);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.chunkingStrategyFactory = Objects.requireNonNull(chunkingStrategyFactory);
    }
    
    @Override
    @Async("documentProcessingExecutor")
    @Transactional
    public void processDocument(ProcessDocumentCommand command) {
        log.info("Processing document: {}", command.documentId());
        
        try {
            // Load the document
            Document document = documentRepository.findById(command.documentId())
                    .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));
            
            // Step 1: Chunk the document
            ChunkingStrategy strategy = chunkingStrategyFactory.createStrategy(
                command.chunkingConfig().strategy(),
                command.chunkingConfig().maxChunkSize(),
                command.chunkingConfig().overlapSize(),
                command.chunkingConfig().preserveSentences()
            );
            
            Document.ProcessingResult chunkingResult = document.processIntoChunks(strategy);
            
            if (!chunkingResult.success()) {
                log.error("Failed to chunk document: {}", chunkingResult.errorMessage());
                publishEventsAndSave(document);
                return;
            }
            
            // Save after chunking
            document = documentRepository.save(document);
            
            // Step 2: Generate embeddings for chunks
            List<DocumentChunk> chunks = document.getChunks();
            Map<ChunkId, EmbeddingVector> embeddings = generateEmbeddings(
                chunks,
                command.embeddingConfig()
            );
            
            // Step 3: Update document with embeddings
            document.updateChunkEmbeddings(embeddings);
            
            // Step 4: Store embeddings in vector store
            storeEmbeddingsInVectorStore(document, embeddings);
            
            // Save final state and publish events
            publishEventsAndSave(document);
            
            log.info("Document processing completed successfully: {}", document.getId());
            
        } catch (Exception e) {
            log.error("Error processing document: {}", command.documentId(), e);
            throw new DocumentProcessingException("Failed to process document", e);
        }
    }
    
    private Map<ChunkId, EmbeddingVector> generateEmbeddings(
            List<DocumentChunk> chunks,
            EmbeddingConfiguration config) {
        
        log.info("Generating embeddings for {} chunks", chunks.size());
        
        Map<ChunkId, EmbeddingVector> embeddings = new HashMap<>();
        
        // Process in batches
        List<List<DocumentChunk>> batches = createBatches(chunks, config.batchSize());
        
        for (List<DocumentChunk> batch : batches) {
            List<String> texts = batch.stream()
                    .map(chunk -> chunk.getContent().text())
                    .collect(Collectors.toList());
            
            List<EmbeddingVector> batchEmbeddings = embeddingService.generateEmbeddings(
                texts,
                config.model()
            );
            
            // Map embeddings to chunk IDs
            for (int i = 0; i < batch.size(); i++) {
                embeddings.put(batch.get(i).getId(), batchEmbeddings.get(i));
            }
        }
        
        return embeddings;
    }
    
    private void storeEmbeddingsInVectorStore(Document document, Map<ChunkId, EmbeddingVector> embeddings) {
        log.info("Storing {} embeddings in vector store", embeddings.size());
        
        List<VectorStore.EmbeddingEntry> entries = new ArrayList<>();
        
        for (DocumentChunk chunk : document.getChunks()) {
            EmbeddingVector vector = embeddings.get(chunk.getId());
            if (vector != null) {
                Map<String, String> metadata = Map.of(
                    "documentName", document.getName().value(),
                    "chunkIndex", String.valueOf(chunk.getSequenceNumber()),
                    "documentStatus", document.getStatus().name()
                );
                
                VectorStore.EmbeddingEntry entry = new VectorStore.EmbeddingEntry(
                    chunk.getId(),
                    document.getId(),
                    document.getOrganizationId(),
                    vector,
                    metadata
                );
                
                entries.add(entry);
            }
        }
        
        vectorStore.storeEmbeddings(entries);
    }
    
    private void publishEventsAndSave(Document document) {
        Document savedDocument = documentRepository.save(document);
        eventPublisher.publishAll(savedDocument.getDomainEvents());
        savedDocument.clearDomainEvents();
    }
    
    private <T> List<List<T>> createBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            batches.add(items.subList(i, end));
        }
        return batches;
    }
    
    /**
     * Factory for creating chunking strategies
     */
    public interface ChunkingStrategyFactory {
        ChunkingStrategy createStrategy(String name, int maxSize, int overlap, boolean preserveSentences);
    }
    
    /**
     * Exception for document processing failures
     */
    public static class DocumentProcessingException extends RuntimeException {
        public DocumentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}