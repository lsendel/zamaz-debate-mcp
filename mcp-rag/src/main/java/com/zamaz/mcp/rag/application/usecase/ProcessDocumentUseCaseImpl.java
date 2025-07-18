package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.command.ProcessDocumentCommand;
import com.zamaz.mcp.rag.application.exception.DocumentNotFoundException;
import com.zamaz.mcp.rag.domain.model.ChunkContent;
import com.zamaz.mcp.rag.domain.model.ChunkId;
import com.zamaz.mcp.rag.domain.model.Document;
import com.zamaz.mcp.rag.domain.model.DocumentChunk;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.domain.port.ChunkingService;
import com.zamaz.mcp.rag.domain.port.DocumentRepository;
import com.zamaz.mcp.rag.domain.port.EmbeddingService;
import com.zamaz.mcp.rag.domain.port.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of ProcessDocumentUseCase.
 */
@Service
@Transactional
public class ProcessDocumentUseCaseImpl implements ProcessDocumentUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessDocumentUseCaseImpl.class);
    
    private final DocumentRepository documentRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    
    public ProcessDocumentUseCaseImpl(
            DocumentRepository documentRepository,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            VectorStore vectorStore) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository cannot be null");
        this.chunkingService = Objects.requireNonNull(chunkingService, "Chunking service cannot be null");
        this.embeddingService = Objects.requireNonNull(embeddingService, "Embedding service cannot be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "Vector store cannot be null");
    }
    
    @Override
    public void execute(ProcessDocumentCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        logger.info("Processing document: {}", command.documentId());
        
        try {
            // Find the document
            Document document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + command.documentId()));
            
            // Check if document can be processed
            if (!document.canBeProcessed()) {
                throw new IllegalStateException("Document cannot be processed in status: " + document.getStatus());
            }
            
            // Start processing
            document.startProcessing();
            documentRepository.save(document);
            
            // Chunk the document
            List<ChunkContent> chunkContents = chunkingService.chunkDocument(document.getContent());
            logger.info("Document {} chunked into {} pieces", document.getId(), chunkContents.size());
            
            // Create document chunks and generate embeddings
            List<DocumentChunk> chunks = new ArrayList<>();
            int startPosition = 0;
            
            for (int i = 0; i < chunkContents.size(); i++) {
                ChunkContent content = chunkContents.get(i);
                int endPosition = startPosition + content.length();
                
                // Create chunk
                DocumentChunk chunk = DocumentChunk.create(
                    ChunkId.generate(),
                    document.getId(),
                    content,
                    i,
                    startPosition,
                    endPosition
                );
                
                // Generate embedding
                try {
                    Embedding embedding = embeddingService.generateEmbedding(content);
                    chunk.setEmbedding(embedding);
                } catch (Exception e) {
                    logger.error("Failed to generate embedding for chunk {} of document {}", i, document.getId(), e);
                    // Continue processing other chunks
                }
                
                chunks.add(chunk);
                startPosition = endPosition;
            }
            
            // Store embeddings in vector store
            List<DocumentChunk> chunksWithEmbeddings = chunks.stream()
                .filter(DocumentChunk::hasEmbedding)
                .toList();
            
            if (!chunksWithEmbeddings.isEmpty()) {
                vectorStore.storeEmbeddings(chunksWithEmbeddings);
                logger.info("Stored {} embeddings for document {}", chunksWithEmbeddings.size(), document.getId());
            }
            
            // Complete processing
            document.completeProcessing(chunks);
            documentRepository.save(document);
            
            logger.info("Successfully processed document {} with {} chunks", document.getId(), chunks.size());
            
        } catch (Exception e) {
            logger.error("Failed to process document: {}", command.documentId(), e);
            
            // Try to mark document as failed
            try {
                Document document = documentRepository.findById(command.documentId()).orElse(null);
                if (document != null) {
                    document.failProcessing(e.getMessage());
                    documentRepository.save(document);
                }
            } catch (Exception saveError) {
                logger.error("Failed to save error status for document: {}", command.documentId(), saveError);
            }
            
            throw new RuntimeException("Document processing failed", e);
        }
    }
}